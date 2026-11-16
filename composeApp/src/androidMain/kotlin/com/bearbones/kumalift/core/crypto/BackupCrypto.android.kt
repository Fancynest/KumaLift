package com.bearbones.kumalift.core.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.bearbones.kumalift.core.backup.BackupErrorCode
import com.bearbones.kumalift.core.backup.BackupException
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

actual object BackupCrypto {
    private val MAGIC = byteArrayOf(0x4B, 0x4C, 0x42, 0x4B) // "KLBK"
    private const val FORMAT_VERSION: Byte = 0x01
    private const val FLAG_ENCRYPTED: Byte = 0x01
    private const val FLAG_KEYSTORE: Byte = 0x02

    private const val PBKDF2_ITERATIONS = 600_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 16
    private const val IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128

    private const val KEYSTORE_ALIAS = "kumalift_auto_backup_key"

    actual fun inspectFile(bytes: ByteArray): BackupInspection {
        if (bytes.size >= 6 &&
            bytes[0] == MAGIC[0] &&
            bytes[1] == MAGIC[1] &&
            bytes[2] == MAGIC[2] &&
            bytes[3] == MAGIC[3]
        ) {
            val version = bytes[4].toInt() and 0xFF
            val flags = bytes[5].toInt() and 0xFF
            val isEncrypted = (flags and FLAG_ENCRYPTED.toInt()) != 0
            val isKeystore = (flags and FLAG_KEYSTORE.toInt()) != 0
            val type = when {
                isEncrypted && isKeystore -> BackupFileType.KEYSTORE_ENCRYPTED
                isEncrypted -> BackupFileType.PASSWORD_ENCRYPTED
                else -> BackupFileType.UNENCRYPTED_CONTAINER
            }
            return BackupInspection(type, isEncrypted, version)
        }

        if (bytes.isNotEmpty() && (bytes[0] == '{'.code.toByte() || bytes[0] == ' '.code.toByte() || bytes[0] == '\n'.code.toByte() || bytes[0] == '\r'.code.toByte())) {
            return BackupInspection(BackupFileType.LEGACY_JSON, false, 1)
        }

        return BackupInspection(BackupFileType.UNKNOWN, false, 0)
    }

    actual fun encryptWithPassword(payloadBytes: ByteArray, password: String): ByteArray {
        require(password.isNotBlank()) { "Password cannot be blank" }

        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH_BYTES)
        random.nextBytes(salt)

        val iv = ByteArray(IV_LENGTH_BYTES)
        random.nextBytes(iv)

        val secretKey = deriveKey(password, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val ciphertext = cipher.doFinal(payloadBytes)

        val header = byteArrayOf(
            MAGIC[0], MAGIC[1], MAGIC[2], MAGIC[3],
            FORMAT_VERSION,
            FLAG_ENCRYPTED
        )

        return header + salt + iv + ciphertext
    }

    actual fun decryptWithPassword(fileBytes: ByteArray, password: String): ByteArray {
        val inspection = inspectFile(fileBytes)
        if (inspection.type != BackupFileType.PASSWORD_ENCRYPTED) {
            if (inspection.type == BackupFileType.UNENCRYPTED_CONTAINER || inspection.type == BackupFileType.LEGACY_JSON) {
                return unpackUnencryptedContainer(fileBytes)
            }
            throw BackupException(BackupErrorCode.UNSUPPORTED_FORMAT, null, "Unsupported or unrecognized encrypted backup format")
        }

        val minExpectedSize = 6 + SALT_LENGTH_BYTES + IV_LENGTH_BYTES + 16 // 16 bytes auth tag minimum
        if (fileBytes.size < minExpectedSize) {
            throw BackupException(BackupErrorCode.WRONG_PASSWORD_OR_CORRUPTED, null, "Incorrect password or corrupted backup file")
        }

        val salt = fileBytes.copyOfRange(6, 6 + SALT_LENGTH_BYTES)
        val ivOffset = 6 + SALT_LENGTH_BYTES
        val iv = fileBytes.copyOfRange(ivOffset, ivOffset + IV_LENGTH_BYTES)
        val ciphertextOffset = ivOffset + IV_LENGTH_BYTES
        val ciphertext = fileBytes.copyOfRange(ciphertextOffset, fileBytes.size)

        return try {
            val secretKey = deriveKey(password, salt)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            cipher.doFinal(ciphertext)
        } catch (e: Exception) {
            throw BackupException(BackupErrorCode.WRONG_PASSWORD_OR_CORRUPTED, e, "Incorrect password or corrupted backup file")
        }
    }

    actual fun createUnencryptedContainer(payloadBytes: ByteArray): ByteArray {
        val header = byteArrayOf(
            MAGIC[0], MAGIC[1], MAGIC[2], MAGIC[3],
            FORMAT_VERSION,
            0x00
        )
        return header + payloadBytes
    }

    actual fun unpackUnencryptedContainer(fileBytes: ByteArray): ByteArray {
        val inspection = inspectFile(fileBytes)
        return if (inspection.type == BackupFileType.UNENCRYPTED_CONTAINER && fileBytes.size >= 6) {
            fileBytes.copyOfRange(6, fileBytes.size)
        } else {
            fileBytes
        }
    }

    actual fun encryptWithKeystore(payloadBytes: ByteArray): ByteArray {
        val secretKey = getOrCreateKeystoreKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv ?: ByteArray(IV_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
        val ciphertext = cipher.doFinal(payloadBytes)

        val header = byteArrayOf(
            MAGIC[0], MAGIC[1], MAGIC[2], MAGIC[3],
            FORMAT_VERSION,
            (FLAG_ENCRYPTED.toInt() or FLAG_KEYSTORE.toInt()).toByte()
        )

        return header + iv + ciphertext
    }

    actual fun decryptWithKeystore(fileBytes: ByteArray): ByteArray {
        val inspection = inspectFile(fileBytes)
        if (inspection.type != BackupFileType.KEYSTORE_ENCRYPTED) {
            throw IllegalArgumentException("Not a keystore-encrypted backup file")
        }

        if (fileBytes.size < 6 + IV_LENGTH_BYTES + 16) {
            throw IllegalArgumentException("Corrupted keystore backup file")
        }

        val iv = fileBytes.copyOfRange(6, 6 + IV_LENGTH_BYTES)
        val ciphertext = fileBytes.copyOfRange(6 + IV_LENGTH_BYTES, fileBytes.size)

        return try {
            val secretKey = getOrCreateKeystoreKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            cipher.doFinal(ciphertext)
        } catch (e: Exception) {
            throw IllegalArgumentException("Cannot decrypt hardware-encrypted backup on this device", e)
        }
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val secretBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(secretBytes, "AES")
    }

    private var jvmFallbackKey: SecretKey? = null

    private fun getOrCreateKeystoreKey(): SecretKey {
        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
                val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
                val keyGenSpec = KeyGenParameterSpec.Builder(
                    KEYSTORE_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(KEY_LENGTH_BITS)
                    .build()
                keyGen.init(keyGenSpec)
                keyGen.generateKey()
            }
            (keyStore.getEntry(KEYSTORE_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
        } catch (_: Exception) {
            // Fallback for JVM unit tests where AndroidKeyStore is not present
            jvmFallbackKey ?: run {
                val keyGen = KeyGenerator.getInstance("AES")
                keyGen.init(256)
                val k = keyGen.generateKey()
                jvmFallbackKey = k
                k
            }
        }
    }
}
