package com.bearbones.kumalift.core.notification

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.bearbones.kumalift.domain.model.NotificationSettings

actual class WorkoutReminderManager(private val context: Context) {
    actual fun syncSettings(settings: NotificationSettings) {
        AndroidWorkoutReminderScheduler.saveAndSchedule(context, settings)
    }

    actual fun isBatteryOptimizationIgnored(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true
        }
        return true
    }

    actual fun requestIgnoreBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                try {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {}
            }
        }
    }

    actual fun sendTestNotification(title: String, message: String) {
        WorkoutReminderReceiver.showReminderNotification(context, title, message)
    }
}

@Composable
actual fun rememberWorkoutReminderManager(): WorkoutReminderManager {
    val context = LocalContext.current.applicationContext
    return remember(context) { WorkoutReminderManager(context) }
}
