package com.bearbones.kumalift.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.bearbones.kumalift.core.backup.AutoBackupScheduler
import com.bearbones.kumalift.data.local.DatabaseDriverFactory
import com.bearbones.kumalift.data.local.KumaDatabaseRepository
import com.bearbones.kumalift.database.KumaDatabase

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            AndroidWorkoutReminderScheduler.rescheduleFromStorage(context)
            try {
                val driver = DatabaseDriverFactory(context).createDriver()
                val db = KumaDatabase(driver)
                val repo = KumaDatabaseRepository(db)
                repo.loadUserSettings()?.let { settings ->
                    AutoBackupScheduler.scheduleOrUpdate(context, settings)
                }
            } catch (_: Exception) {}
        }
    }
}
