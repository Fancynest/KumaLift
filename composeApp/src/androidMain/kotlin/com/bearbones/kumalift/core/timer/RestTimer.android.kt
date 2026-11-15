package com.bearbones.kumalift.core.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.bearbones.kumalift.MainActivity
import com.bearbones.kumalift.core.timer.RestTimerNotificationReceiver.Companion.ACTION_REST_TIMER_ADD_30
import com.bearbones.kumalift.core.timer.RestTimerNotificationReceiver.Companion.ACTION_REST_TIMER_SKIP

object AndroidAppProvider {
    @Volatile
    var appContext: Context? = null
}

private const val CHANNEL_ONGOING_ID = "kuma_rest_timer_ongoing"
private const val CHANNEL_ALERT_ID = "kuma_rest_timer_alert"
private const val NOTIF_ID_ONGOING = 2002
private const val NOTIF_ID_ALERT = 2003

private var lastOngoingExerciseName: String? = null

private fun ensureNotificationChannels(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val ongoingChannel = NotificationChannel(
            CHANNEL_ONGOING_ID,
            "Rest Timer Ongoing",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Persistent live countdown on lockscreen and status bar"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setSound(null, null)
            enableVibration(false)
        }

        val alertChannel = NotificationChannel(
            CHANNEL_ALERT_ID,
            "Rest Timer Finished",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts and vibration when rest timer finishes"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            enableVibration(true)
        }

        manager.createNotificationChannel(ongoingChannel)
        manager.createNotificationChannel(alertChannel)
    }
}

actual fun notifyRestTimerStarted(targetEndEpochMs: Long, totalSeconds: Int, exerciseName: String?) {
    val context = AndroidAppProvider.appContext ?: return
    lastOngoingExerciseName = exerciseName
    updateOngoingNotification(context, targetEndEpochMs, exerciseName)
}

actual fun notifyRestTimerUpdated(targetEndEpochMs: Long, remainingSeconds: Int) {
    val context = AndroidAppProvider.appContext ?: return
    updateOngoingNotification(context, targetEndEpochMs, lastOngoingExerciseName)
}

actual fun notifyRestTimerStopped() {
    val context = AndroidAppProvider.appContext ?: return
    try {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.cancel(NOTIF_ID_ONGOING)
    } catch (_: Throwable) {}
}

private fun updateOngoingNotification(context: Context, targetEndEpochMs: Long, exerciseName: String?) {
    try {
        ensureNotificationChannels(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val appIconRes = context.applicationInfo.icon.let { if (it != 0) it else android.R.drawable.ic_dialog_info }

        // Content intent: Opens active workout
        val contentIntent = PendingIntent.getActivity(
            context,
            100,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: +30s
        val add30Intent = PendingIntent.getBroadcast(
            context,
            101,
            Intent(context, RestTimerNotificationReceiver::class.java).apply {
                action = ACTION_REST_TIMER_ADD_30
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: Skip
        val skipIntent = PendingIntent.getBroadcast(
            context,
            102,
            Intent(context, RestTimerNotificationReceiver::class.java).apply {
                action = ACTION_REST_TIMER_SKIP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (!exerciseName.isNullOrBlank()) "Resting · $exerciseName" else "Rest Timer"
        val subtitle = "Resting between sets"

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, CHANNEL_ONGOING_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }

        builder.setSmallIcon(appIconRes)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setUsesChronometer(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setChronometerCountDown(true)
        }
        builder.setWhen(targetEndEpochMs)

        // Add action buttons
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            val addAction = Notification.Action.Builder(
                null,
                "+30s",
                add30Intent
            ).build()

            val skipAction = Notification.Action.Builder(
                null,
                "Skip",
                skipIntent
            ).build()

            builder.addAction(addAction)
            builder.addAction(skipAction)
        }

        manager.notify(NOTIF_ID_ONGOING, builder.build())
    } catch (_: Throwable) {
        // Fallback gracefully
    }
}

actual fun playTimerCompletionFeedback() {
    val context = AndroidAppProvider.appContext ?: return
    try {
        notifyRestTimerStopped()

        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 300), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(500)
            }
        }

        ensureNotificationChannels(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
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
            Notification.Builder(context, CHANNEL_ALERT_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }

        val alertNotification = alertBuilder
            .setSmallIcon(appIconRes)
            .setContentTitle("Rest Finished!")
            .setContentText("Your rest timer is done. Ready for the next set!")
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setShowWhen(true)
            .build()

        manager.notify(NOTIF_ID_ALERT, alertNotification)
    } catch (_: Throwable) {
        // Safe fallback
    }
}
