package com.bearbones.kumalift.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import com.bearbones.kumalift.R
import com.bearbones.kumalift.core.localization.resolveAppStrings
import com.bearbones.kumalift.core.timer.AndroidAppProvider
import com.bearbones.kumalift.domain.model.AppLanguage

object PlatformNotificationChannels {
    const val CHANNEL_REST_TIMER_ONGOING = "rest_timer_ongoing_v2"
    const val CHANNEL_REST_TIMER_ALERT_KUMA = "rest_timer_alert_kuma_v2"
    const val CHANNEL_REST_TIMER_ALERT_VIBRATE = "rest_timer_alert_vibrate_v2"
    const val CHANNEL_REST_TIMER_ALERT_SILENT = "rest_timer_alert_silent_v2"

    const val CHANNEL_WORKOUT_REMINDER_KUMA = "workout_reminder_kuma_v2"
    const val CHANNEL_WORKOUT_REMINDER_VIBRATE = "workout_reminder_vibrate_v2"
    const val CHANNEL_WORKOUT_REMINDER_SILENT = "workout_reminder_silent_v2"

    const val CHANNEL_BACKUP_ERROR = "backup_error_v2"

    val LEGACY_CHANNELS = listOf(
        "kuma_rest_timer_ongoing",
        "kuma_rest_timer_alert",
        "workout_reminders",
        "kumalift_backup_alerts"
    )

    fun ensureChannels(context: Context, language: AppLanguage) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val strings = resolveAppStrings(language)

        // Delete legacy channels once on upgrade
        for (legacyId in LEGACY_CHANNELS) {
            try {
                manager.deleteNotificationChannel(legacyId)
            } catch (_: Throwable) {}
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val lightVibration = longArrayOf(0, 100)

        val restEndSoundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.kumalift_rest_end}")
        val reminderSoundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.kumalift_reminder}")
        val errorSoundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.kumalift_error}")

        // 1. Ongoing rest timer
        val ongoingChannel = NotificationChannel(
            CHANNEL_REST_TIMER_ONGOING,
            strings.channelRestTimerOngoingName,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = strings.channelRestTimerOngoingDesc
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannel(ongoingChannel)

        // 2. Rest timer alerts (Kuma, Vibrate, Silent)
        val alertKumaChannel = NotificationChannel(
            CHANNEL_REST_TIMER_ALERT_KUMA,
            strings.channelRestTimerAlertName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = strings.channelRestTimerAlertDesc
            setSound(restEndSoundUri, audioAttributes)
            enableVibration(true)
            vibrationPattern = lightVibration
            enableLights(false)
        }
        manager.createNotificationChannel(alertKumaChannel)

        val alertVibrateChannel = NotificationChannel(
            CHANNEL_REST_TIMER_ALERT_VIBRATE,
            strings.channelRestTimerAlertName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = strings.channelRestTimerAlertDesc
            setSound(null, null)
            enableVibration(true)
            vibrationPattern = lightVibration
            enableLights(false)
        }
        manager.createNotificationChannel(alertVibrateChannel)

        val alertSilentChannel = NotificationChannel(
            CHANNEL_REST_TIMER_ALERT_SILENT,
            strings.channelRestTimerAlertName,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = strings.channelRestTimerAlertDesc
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannel(alertSilentChannel)

        // 3. Workout reminders (Kuma, Vibrate, Silent)
        val reminderKumaChannel = NotificationChannel(
            CHANNEL_WORKOUT_REMINDER_KUMA,
            strings.channelWorkoutReminderName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = strings.channelWorkoutReminderDesc
            setSound(reminderSoundUri, audioAttributes)
            enableVibration(true)
            vibrationPattern = lightVibration
            enableLights(false)
        }
        manager.createNotificationChannel(reminderKumaChannel)

        val reminderVibrateChannel = NotificationChannel(
            CHANNEL_WORKOUT_REMINDER_VIBRATE,
            strings.channelWorkoutReminderName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = strings.channelWorkoutReminderDesc
            setSound(null, null)
            enableVibration(true)
            vibrationPattern = lightVibration
            enableLights(false)
        }
        manager.createNotificationChannel(reminderVibrateChannel)

        val reminderSilentChannel = NotificationChannel(
            CHANNEL_WORKOUT_REMINDER_SILENT,
            strings.channelWorkoutReminderName,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = strings.channelWorkoutReminderDesc
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannel(reminderSilentChannel)

        // 4. Backup failure
        val backupErrorChannel = NotificationChannel(
            CHANNEL_BACKUP_ERROR,
            strings.channelBackupErrorName,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = strings.channelBackupErrorDesc
            setSound(errorSoundUri, audioAttributes)
            enableVibration(false)
            enableLights(false)
        }
        manager.createNotificationChannel(backupErrorChannel)
    }
}

actual fun syncPlatformNotificationChannels(language: AppLanguage) {
    val context = AndroidAppProvider.appContext ?: return

    // Sync SharedPreferences for background receivers/services
    try {
        val prefs = context.getSharedPreferences("kumalift_reminders", Context.MODE_PRIVATE)
        prefs.edit().putString("language_code", language.name).apply()
    } catch (_: Throwable) {}

    PlatformNotificationChannels.ensureChannels(context, language)
}
