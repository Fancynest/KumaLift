package com.bearbones.kumalift.core.crypto

enum class BackupFileType {
    LEGACY_JSON,
    UNENCRYPTED_CONTAINER,
    PASSWORD_ENCRYPTED,
    KEYSTORE_ENCRYPTED,
    UNKNOWN
}

data class BackupInspection(
    val type: BackupFileType,
    val isEncrypted: Boolean,
    val version: Int
)

expect object BackupCrypto {
    fun inspectFile(bytes: ByteArray): BackupInspection
    fun encryptWithPassword(payloadBytes: ByteArray, password: String): ByteArray
    fun decryptWithPassword(fileBytes: ByteArray, password: String): ByteArray
    fun createUnencryptedContainer(payloadBytes: ByteArray): ByteArray
    fun unpackUnencryptedContainer(fileBytes: ByteArray): ByteArray
    fun encryptWithKeystore(payloadBytes: ByteArray): ByteArray
    fun decryptWithKeystore(fileBytes: ByteArray): ByteArray
}
