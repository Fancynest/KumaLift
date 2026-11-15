package com.bearbones.kumalift.core.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackupCryptoTests {

    @Test
    fun testInspectFileTypes() {
        // 1. Legacy JSON
        val jsonBytes = "{\"schemaVersion\": 1}".encodeToByteArray()
        val jsonInspection = BackupCrypto.inspectFile(jsonBytes)
        assertEquals(BackupFileType.LEGACY_JSON, jsonInspection.type)
        assertFalse(jsonInspection.isEncrypted)

        // 2. Unencrypted Container
        val unencContainer = BackupCrypto.createUnencryptedContainer(jsonBytes)
        val unencInspection = BackupCrypto.inspectFile(unencContainer)
        assertEquals(BackupFileType.UNENCRYPTED_CONTAINER, unencInspection.type)
        assertFalse(unencInspection.isEncrypted)
        assertEquals(1, unencInspection.version)

        // 3. Password Encrypted Container
        val encBytes = BackupCrypto.encryptWithPassword(jsonBytes, "SecretPassword123!")
        val encInspection = BackupCrypto.inspectFile(encBytes)
        assertEquals(BackupFileType.PASSWORD_ENCRYPTED, encInspection.type)
        assertTrue(encInspection.isEncrypted)
        assertEquals(1, encInspection.version)

        // 4. Unknown format
        val unknownInspection = BackupCrypto.inspectFile(byteArrayOf(0x00, 0x01, 0x02))
        assertEquals(BackupFileType.UNKNOWN, unknownInspection.type)
    }

    @Test
    fun testPasswordEncryptionRoundTrip() {
        val originalText = "{\"app\":\"KumaLift\",\"user\":\"Gabriel\",\"workouts\":[{\"id\":\"w1\",\"volume\":1250.0}]}"
        val password = "MySecureWorkoutPassword#2026"

        val encryptedPackage = BackupCrypto.encryptWithPassword(originalText.encodeToByteArray(), password)

        // Verify magic bytes "KLBK"
        assertEquals(0x4B.toByte(), encryptedPackage[0])
        assertEquals(0x4C.toByte(), encryptedPackage[1])
        assertEquals(0x42.toByte(), encryptedPackage[2])
        assertEquals(0x4B.toByte(), encryptedPackage[3])

        // Verify version 1
        assertEquals(0x01.toByte(), encryptedPackage[4])

        // Verify flag byte (bit 0 = 1, bit 1 = 0)
        assertEquals(0x01.toByte(), encryptedPackage[5])

        // Decrypt with correct password
        val decryptedBytes = BackupCrypto.decryptWithPassword(encryptedPackage, password)
        val decryptedText = decryptedBytes.decodeToString()
        assertEquals(originalText, decryptedText)
    }

    @Test
    fun testIncorrectPasswordFailsCleanly() {
        val originalText = "Sensitive workout logs and biometric health data"
        val encrypted = BackupCrypto.encryptWithPassword(originalText.encodeToByteArray(), "CorrectPassword123")

        val exception = assertFailsWith<IllegalArgumentException> {
            BackupCrypto.decryptWithPassword(encrypted, "WrongPassword456")
        }
        assertTrue(
            exception.message?.contains("Incorrect password or corrupted backup file") == true,
            "Expected friendly error message but got: ${exception.message}"
        )
    }

    @Test
    fun testCorruptedCiphertextFailsAuthentication() {
        val originalText = "Integrity protected data with GCM 128-bit authentication tag"
        val encrypted = BackupCrypto.encryptWithPassword(originalText.encodeToByteArray(), "StrongPass#1")

        // Tamper with one byte in the ciphertext payload
        val corrupted = encrypted.copyOf()
        corrupted[corrupted.size - 5] = (corrupted[corrupted.size - 5] + 1).toByte()

        val exception = assertFailsWith<IllegalArgumentException> {
            BackupCrypto.decryptWithPassword(corrupted, "StrongPass#1")
        }
        assertTrue(
            exception.message?.contains("Incorrect password or corrupted backup file") == true,
            "Expected friendly tamper error message but got: ${exception.message}"
        )
    }

    @Test
    fun testUnencryptedContainerUnpack() {
        val originalText = "{\"status\":\"healthy\",\"offline\":true}"
        val container = BackupCrypto.createUnencryptedContainer(originalText.encodeToByteArray())

        val unpacked = BackupCrypto.unpackUnencryptedContainer(container)
        assertEquals(originalText, unpacked.decodeToString())
    }

    @Test
    fun testKeystoreEncryptionRoundTrip() {
        val originalText = "{\"autoBackup\":\"daily\",\"retention\":5}"
        val encrypted = BackupCrypto.encryptWithKeystore(originalText.encodeToByteArray())

        val inspection = BackupCrypto.inspectFile(encrypted)
        assertEquals(BackupFileType.KEYSTORE_ENCRYPTED, inspection.type)
        assertTrue(inspection.isEncrypted)

        val decrypted = BackupCrypto.decryptWithKeystore(encrypted)
        assertEquals(originalText, decrypted.decodeToString())
    }
}
