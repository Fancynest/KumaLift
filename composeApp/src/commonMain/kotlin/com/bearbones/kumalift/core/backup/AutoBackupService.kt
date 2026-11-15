package com.bearbones.kumalift.core.backup

import com.bearbones.kumalift.data.local.KumaDatabaseRepository
import com.bearbones.kumalift.domain.model.UserSettings

data class AutoBackupExecutionResult(
    val success: Boolean,
    val fileName: String? = null,
    val errorMessage: String? = null
)

expect object AutoBackupService {
    fun schedule(settings: UserSettings)
    fun cancel()
    suspend fun runBackupNow(
        databaseRepo: KumaDatabaseRepository,
        settings: UserSettings
    ): AutoBackupExecutionResult
}
