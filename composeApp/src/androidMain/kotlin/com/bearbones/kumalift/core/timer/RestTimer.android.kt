package com.bearbones.kumalift.core.timer

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.bearbones.kumalift.MainActivity
import com.bearbones.kumalift.core.localization.AppStrings
import com.bearbones.kumalift.core.localization.resolveAppStrings
import com.bearbones.kumalift.core.notification.PlatformNotificationChannels
import com.bearbones.kumalift.core.sound.PlatformSoundPlayer
import com.bearbones.kumalift.core.sound.SoundEvent
import com.bearbones.kumalift.core.sound.SoundManager
import com.bearbones.kumalift.core.timer.RestTimerNotificationReceiver.Companion.ACTION_REST_TIMER_ADD_15
import com.bearbones.kumalift.core.timer.RestTimerNotificationReceiver.Companion.ACTION_REST_TIMER_ADD_30
import com.bearbones.kumalift.core.timer.RestTimerNotificationReceiver.Companion.ACTION_REST_TIMER_SKIP
import com.bearbones.kumalift.domain.model.AppLanguage
import com.bearbones.kumalift.domain.model.UserSettings

object AndroidAppProvider {
    @Volatile
    var appContext: Context? = null
}

private const val NOTIF_ID_ONGOING = 2002

private var lastOngoingExerciseName: String? = null

private fun getStrings(context: Context): AppStrings {
    val prefs = context.getSharedPreferences("kumalift_reminders", Context.MODE_PRIVATE)
    val langStr = prefs.getString("language_code", null)
    val appLanguage = try {
        if (langStr != null) AppLanguage.valueOf(langStr) else AppLanguage.SYSTEM
    } catch (_: Exception) {
        AppLanguage.SYSTEM
    }
    return resolveAppStrings(appLanguage)
}

private fun getUserSettings(): UserSettings {
    RestTimerCoordinator.userSettingsProvider?.invoke()?.let { return it }
    val context = AndroidAppProvider.appContext
    if (context != null) {
        try {
            val driver = com.bearbones.kumalift.data.local.DatabaseDriverFactory(context).createDriver()
            val database = com.bearbones.kumalift.database.KumaDatabase(driver)
            val dbRepo = com.bearbones.kumalift.data.local.KumaDatabaseRepository(database)
            val loaded = dbRepo.loadUserSettings()
            if (loaded != null) {
                RestTimerCoordinator.userSettingsProvider = { loaded }
                return loaded
            }
        } catch (_: Throwable) {}
    }
    return UserSettings()
}

actual fun notifyRestTimerStarted(targetEndEpochMs: Long, totalSeconds: Int, exerciseName: String?) {
    val context = AndroidAppProvider.appContext ?: return
    lastOngoingExerciseName = exerciseName
    PlatformSoundPlayer.cancelRestFinishedNotification()

    val settings = getUserSettings()
    if (settings.notifications.ongoingWorkout || settings.notifications.restTimerAlert) {
        updateOngoingNotification(context, targetEndEpochMs, exerciseName)
    } else {
        notifyRestTimerStopped()
    }
}

actual fun notifyRestTimerUpdated(targetEndEpochMs: Long, remainingSeconds: Int) {
    val context = AndroidAppProvider.appContext ?: return
    val settings = getUserSettings()
    if (settings.notifications.ongoingWorkout || settings.notifications.restTimerAlert) {
        updateOngoingNotification(context, targetEndEpochMs, lastOngoingExerciseName)
    } else {
        notifyRestTimerStopped()
    }
}

actual fun notifyRestTimerStopped() {
    val context = AndroidAppProvider.appContext ?: return
    try {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.cancel(NOTIF_ID_ONGOING)
    } catch (_: Throwable) {}
    PlatformSoundPlayer.cancelRestFinishedNotification()
}

actual fun notifyRestTimerCompleted(exerciseName: String?) {
    val context = AndroidAppProvider.appContext ?: return
    val settings = getUserSettings()
    if (!settings.notifications.ongoingWorkout && !settings.notifications.restTimerAlert) {
        notifyRestTimerStopped()
        return
    }

    try {
        val strings = getStrings(context)
        val prefs = context.getSharedPreferences("kumalift_reminders", Context.MODE_PRIVATE)
        val langStr = prefs.getString("language_code", null)
        val appLanguage = try {
            if (langStr != null) AppLanguage.valueOf(langStr) else AppLanguage.SYSTEM
        } catch (_: Exception) {
            AppLanguage.SYSTEM
        }
        PlatformNotificationChannels.ensureChannels(context, appLanguage)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val appIconRes = context.applicationInfo.icon.let { if (it != 0) it else android.R.drawable.ic_dialog_info }

        val contentIntent = PendingIntent.getActivity(
            context,
            100,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val add30Intent = PendingIntent.getBroadcast(
            context,
            101,
            Intent(context, RestTimerNotificationReceiver::class.java).apply {
                action = ACTION_REST_TIMER_ADD_30
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = PendingIntent.getBroadcast(
            context,
            103,
            Intent(context, RestTimerNotificationReceiver::class.java).apply {
                action = RestTimerNotificationReceiver.ACTION_REST_TIMER_DISMISS
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = strings.restTimerNotificationTitle
        val text = if (!exerciseName.isNullOrBlank()) {
            "$exerciseName · ${strings.restTimerNotificationText}"
        } else {
            strings.restTimerNotificationText
        }

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, PlatformNotificationChannels.CHANNEL_REST_TIMER_ONGOING)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }

        builder.setSmallIcon(appIconRes)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setUsesChronometer(false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            val addAction = Notification.Action.Builder(
                null,
                strings.restTimerActionAdd30,
                add30Intent
            ).build()

            val dismissAction = Notification.Action.Builder(
                null,
                strings.close,
                dismissIntent
            ).build()

            builder.addAction(addAction)
            builder.addAction(dismissAction)
        }

        manager.notify(NOTIF_ID_ONGOING, builder.build())
    } catch (_: Throwable) {
        // Fallback gracefully
    }
}

private fun updateOngoingNotification(context: Context, targetEndEpochMs: Long, exerciseName: String?) {
    try {
        val prefs = context.getSharedPreferences("kumalift_reminders", Context.MODE_PRIVATE)
        val langStr = prefs.getString("language_code", null)
        val appLanguage = try {
            if (langStr != null) AppLanguage.valueOf(langStr) else AppLanguage.SYSTEM
        } catch (_: Exception) {
            AppLanguage.SYSTEM
        }
        PlatformNotificationChannels.ensureChannels(context, appLanguage)

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

        val strings = getStrings(context)
        val title = if (!exerciseName.isNullOrBlank()) "${strings.restTimerRestingSubtitle} · $exerciseName" else strings.restTimerChannelName
        val subtitle = strings.restTimerRestingSubtitle

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, PlatformNotificationChannels.CHANNEL_REST_TIMER_ONGOING)
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
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setUsesChronometer(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setCategory(Notification.CATEGORY_PROGRESS)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setChronometerCountDown(true)
        }
        builder.setWhen(targetEndEpochMs)

        // Add action buttons
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            val addAction = Notification.Action.Builder(
                null,
                strings.restTimerActionAdd30,
                add30Intent
            ).build()

            val skipAction = Notification.Action.Builder(
                null,
                strings.restTimerActionSkip,
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

actual fun playTimerTickFeedback(secondsRemaining: Int) {
    val settings = getUserSettings()
    SoundManager.trigger(SoundEvent.TICK, settings, secondsRemaining)
}

actual fun playTimerCompletionFeedback() {
    val settings = getUserSettings()
    SoundManager.trigger(SoundEvent.REST_END, settings)
}
