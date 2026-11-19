package com.bearbones.kumalift.core.sound

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.bearbones.kumalift.MainActivity
import com.bearbones.kumalift.core.localization.resolveAppStrings
import com.bearbones.kumalift.core.notification.PlatformNotificationChannels
import com.bearbones.kumalift.core.timer.AndroidAppProvider
import com.bearbones.kumalift.domain.model.AppLanguage
import com.bearbones.kumalift.domain.model.RestTimerSoundSource
import com.bearbones.kumalift.domain.model.SoundVolume

actual object PlatformSoundPlayer {

    private const val NOTIF_ID_ALERT = 2003

    init {
        KumaSoundPlayer.onCustomSoundCompletion = {
            SoundManager.onPlaybackStopped?.invoke()
        }
    }

    actual fun playSound(event: SoundEvent, volumePercent: Int) {
        val context = AndroidAppProvider.appContext ?: return
        KumaSoundPlayer.play(context, event, volumePercent)
    }

    actual fun playRestTimerSound(
        volumePercent: Int,
        source: RestTimerSoundSource,
        customPath: String?
    ) {
        val context = AndroidAppProvider.appContext ?: return
        KumaSoundPlayer.playRestTimerSound(context, volumePercent, source, customPath)
    }

    actual fun playCustomSound(filePath: String, volumePercent: Int) {
        val context = AndroidAppProvider.appContext ?: return
        KumaSoundPlayer.playCustomSound(context, filePath, (volumePercent / 100f).coerceIn(0f, 1f))
    }

    actual fun playSystemDefaultSound(volumePercent: Int) {
        val context = AndroidAppProvider.appContext ?: return
        KumaSoundPlayer.playSystemDefaultSound(context, (volumePercent / 100f).coerceIn(0f, 1f))
    }

    actual fun stopRestTimerSound() {
        val context = AndroidAppProvider.appContext ?: return
        KumaSoundPlayer.stopAllSounds(context)
    }

    actual fun playHaptic() {
        val context = AndroidAppProvider.appContext ?: return
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(25)
                }
            }
        } catch (_: Throwable) {}
    }

    actual fun cancelRestFinishedNotification() {
        val context = AndroidAppProvider.appContext ?: return
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.cancel(NOTIF_ID_ALERT)
        } catch (_: Throwable) {}
        stopRestTimerSound()
    }

    actual fun postRestFinishedNotification(channelVariant: ChannelVariant) {
        val context = AndroidAppProvider.appContext ?: return
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val prefs = context.getSharedPreferences("kumalift_reminders", Context.MODE_PRIVATE)
            val langStr = prefs.getString("language_code", null)
            val appLanguage = try {
                if (langStr != null) AppLanguage.valueOf(langStr) else AppLanguage.SYSTEM
            } catch (_: Exception) {
                AppLanguage.SYSTEM
            }
            val strings = resolveAppStrings(appLanguage)

            PlatformNotificationChannels.ensureChannels(context, appLanguage)

            val channelId = when (channelVariant) {
                ChannelVariant.KUMA -> PlatformNotificationChannels.CHANNEL_REST_TIMER_ALERT_KUMA
                ChannelVariant.VIBRATE -> PlatformNotificationChannels.CHANNEL_REST_TIMER_ALERT_VIBRATE
                ChannelVariant.SILENT -> PlatformNotificationChannels.CHANNEL_REST_TIMER_ALERT_SILENT
            }

            val appIconRes = context.applicationInfo.icon.let { if (it != 0) it else android.R.drawable.ic_dialog_info }

            val openIntent = PendingIntent.getActivity(
                context,
                105,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alertBuilder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(context, channelId)
            } else {
                @Suppress("DEPRECATION")
                Notification.Builder(context)
            }

            val alertNotification = alertBuilder
                .setSmallIcon(appIconRes)
                .setContentTitle(strings.restTimerNotificationTitle)
                .setContentText(strings.restTimerNotificationText)
                .setContentIntent(openIntent)
                .setAutoCancel(true)
                .setShowWhen(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .build()

            nm.notify(NOTIF_ID_ALERT, alertNotification)
        } catch (_: Throwable) {}
    }

    actual fun isAppInForeground(): Boolean = AppForegroundTracker.isForeground

    actual fun isScreenInteractive(): Boolean {
        val context = AndroidAppProvider.appContext ?: return true
        return AppForegroundTracker.isScreenInteractive(context)
    }

    actual fun getRingerMode(): RingerMode {
        val context = AndroidAppProvider.appContext ?: return RingerMode.NORMAL
        return AppForegroundTracker.getRingerMode(context)
    }

    actual fun isDoNotDisturbActive(): Boolean {
        val context = AndroidAppProvider.appContext ?: return false
        return AppForegroundTracker.isDoNotDisturbActive(context)
    }

    actual fun hasNotificationPermission(): Boolean {
        val context = AndroidAppProvider.appContext ?: return true
        return AppForegroundTracker.hasNotificationPermission(context)
    }
}
