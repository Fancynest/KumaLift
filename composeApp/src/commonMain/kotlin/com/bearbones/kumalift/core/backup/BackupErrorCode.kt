package com.bearbones.kumalift.core.backup

import com.bearbones.kumalift.core.localization.AppStrings

enum class BackupErrorCode {
    FOLDER_NOT_CONFIGURED,
    FOLDER_INACCESSIBLE,
    WRITE_FAILED,
    WRONG_PASSWORD_OR_CORRUPTED,
    UNSUPPORTED_FORMAT,
    INVALID_FILE,
    PERMISSION_LOST,
    UNKNOWN;

    fun localizedMessage(s: AppStrings): String = when (this) {
        FOLDER_NOT_CONFIGURED -> s.backupErrorFolderNotConfigured
        FOLDER_INACCESSIBLE -> s.backupErrorFolderInaccessible
        WRITE_FAILED -> s.backupErrorWriteFailed
        WRONG_PASSWORD_OR_CORRUPTED -> s.backupErrorWrongPasswordOrCorrupted
        UNSUPPORTED_FORMAT -> s.backupErrorUnsupportedFormat
        INVALID_FILE -> s.backupErrorInvalidFile
        PERMISSION_LOST -> s.backupErrorPermissionLost
        UNKNOWN -> s.backupErrorUnknown
    }

    companion object {
        fun fromCode(code: String?): BackupErrorCode {
            if (code == null) return UNKNOWN
            return entries.firstOrNull { it.name.equals(code, ignoreCase = true) } ?: UNKNOWN
        }
    }
}

open class BackupException(
    val errorCode: BackupErrorCode,
    override val cause: Throwable? = null,
    technicalMessage: String? = null
) : IllegalArgumentException(technicalMessage ?: errorCode.name, cause)
