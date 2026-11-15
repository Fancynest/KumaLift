package com.bearbones.kumalift.core.backup

import com.bearbones.kumalift.data.local.KumaDatabaseRepository
import com.bearbones.kumalift.domain.model.UserSettings

actual object AutoBackupService {
    actual fun schedule(settings: UserSettings) {}
    actual fun cancel() {}
    actual suspend fun runBackupNow(
        databaseRepo: KumaDatabaseRepository,
        settings: UserSettings
    ): AutoBackupExecutionResult {
        return AutoBackupExecutionResult(false, null, "Not supported on iOS")
    }
}
