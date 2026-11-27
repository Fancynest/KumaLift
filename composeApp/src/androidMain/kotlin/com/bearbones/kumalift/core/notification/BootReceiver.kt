package com.bearbones.kumalift.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.bearbones.kumalift.core.backup.AutoBackupScheduler
import com.bearbones.kumalift.data.local.AndroidDatabaseProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val pendingResult = goAsync()
            val appContext = context.applicationContext ?: context
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    AndroidWorkoutReminderScheduler.rescheduleFromStorage(appContext)
                    val repo = AndroidDatabaseProvider.getRepository(appContext)
                    repo.loadUserSettings()?.let { settings ->
                        AutoBackupScheduler.scheduleOrUpdate(appContext, settings)
                    }
                } catch (_: Exception) {
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
