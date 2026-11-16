package com.bearbones.kumalift.core.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.bearbones.kumalift.core.localization.resolveAppStrings
import com.bearbones.kumalift.core.timer.AndroidAppProvider
import com.bearbones.kumalift.domain.model.AppLanguage

actual fun syncPlatformNotificationChannels(language: AppLanguage) {
    val context = AndroidAppProvider.appContext ?: return
    val strings = resolveAppStrings(language)

    // Sync SharedPreferences for background receivers/services
    try {
        val prefs = context.getSharedPreferences("kumalift_reminders", Context.MODE_PRIVATE)
        prefs.edit().putString("language_code", language.name).apply()
    } catch (_: Throwable) {}

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        // 1. Workout Reminders channel
        val reminderChannel = NotificationChannel(
            WorkoutReminderReceiver.CHANNEL_ID,
            strings.notifChannelName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = strings.notifChannelDescription
            enableVibration(true)
            enableLights(true)
        }
        manager.createNotificationChannel(reminderChannel)

        // 2. Rest Timer ongoing channel
        val ongoingChannel = NotificationChannel(
            "kuma_rest_timer_ongoing",
            strings.restTimerChannelName,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = strings.restTimerChannelDescription
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannel(ongoingChannel)

        // 3. Rest Timer alert channel
        val alertChannel = NotificationChannel(
            "kuma_rest_timer_alert",
            strings.restTimerNotificationTitle,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = strings.restTimerChannelDescription
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            enableVibration(true)
        }
        manager.createNotificationChannel(alertChannel)

        // 4. Auto Backup alerts channel
        val backupChannel = NotificationChannel(
            "kumalift_backup_alerts",
            strings.autoBackupChannelName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = strings.autoBackupChannelDescription
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            enableVibration(true)
        }
        manager.createNotificationChannel(backupChannel)
    }
}
