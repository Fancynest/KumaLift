package com.bearbones.kumalift.core.crypto

actual object BackupCrypto {
    actual fun inspectFile(bytes: ByteArray): BackupInspection {
        if (bytes.size >= 4 &&
            bytes[0] == 0x4B.toByte() &&
            bytes[1] == 0x4C.toByte() &&
            bytes[2] == 0x42.toByte() &&
            bytes[3] == 0x4B.toByte()
        ) {
            val flags = if (bytes.size > 5) bytes[5].toInt() and 0xFF else 0
            val isEncrypted = (flags and 0x01) != 0
            val isKeystore = (flags and 0x02) != 0
            val type = when {
                isEncrypted && isKeystore -> BackupFileType.KEYSTORE_ENCRYPTED
                isEncrypted -> BackupFileType.PASSWORD_ENCRYPTED
                else -> BackupFileType.UNENCRYPTED_CONTAINER
            }
            return BackupInspection(type, isEncrypted, if (bytes.size > 4) bytes[4].toInt() and 0xFF else 1)
        }
        if (bytes.isNotEmpty() && (bytes[0] == '{'.code.toByte() || bytes[0] == ' '.code.toByte())) {
            return BackupInspection(BackupFileType.LEGACY_JSON, false, 1)
        }
        return BackupInspection(BackupFileType.UNKNOWN, false, 0)
    }

    actual fun encryptWithPassword(payloadBytes: ByteArray, password: String): ByteArray {
        return createUnencryptedContainer(payloadBytes)
    }

    actual fun decryptWithPassword(fileBytes: ByteArray, password: String): ByteArray {
        return unpackUnencryptedContainer(fileBytes)
    }

    actual fun createUnencryptedContainer(payloadBytes: ByteArray): ByteArray {
        val header = byteArrayOf(0x4B, 0x4C, 0x42, 0x4B, 0x01, 0x00)
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
        return createUnencryptedContainer(payloadBytes)
    }

    actual fun decryptWithKeystore(fileBytes: ByteArray): ByteArray {
        return unpackUnencryptedContainer(fileBytes)
    }
}
