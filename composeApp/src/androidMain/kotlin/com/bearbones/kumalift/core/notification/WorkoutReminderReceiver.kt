package com.bearbones.kumalift.core.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.bearbones.kumalift.MainActivity
import com.bearbones.kumalift.core.localization.resolveAppStrings
import com.bearbones.kumalift.domain.model.AppLanguage

class WorkoutReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            // Restore scheduled reminders after device restart or package update without firing an premature notification
            AndroidWorkoutReminderScheduler.rescheduleFromStorage(context)
            return
        }

        val prefs = context.getSharedPreferences("kumalift_reminders", Context.MODE_PRIVATE)

        // Prevent duplicate delivery for the same trigger window (within 60s)
        val now = System.currentTimeMillis()
        val lastTrigger = prefs.getLong("last_trigger_ms", 0L)
        if (now - lastTrigger < 60_000L) {
            return
        }
        prefs.edit().putLong("last_trigger_ms", now).apply()

        val langStr = prefs.getString("language_code", null)
        val appLanguage = try {
            if (langStr != null) AppLanguage.valueOf(langStr) else AppLanguage.SYSTEM
        } catch (_: Exception) {
            AppLanguage.SYSTEM
        }
        val strings = resolveAppStrings(appLanguage)

        // Select daily message variation (consistent for today, changes daily across 5 variations)
        val variations = strings.notificationVariations
        val dayOfYear = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
        val selectedVariation = if (variations.isNotEmpty()) {
            variations[dayOfYear % variations.size]
        } else {
            strings.notifReminderTitle to strings.notifReminderBody
        }

        showReminderNotification(
            context = context,
            title = selectedVariation.first,
            message = selectedVariation.second,
            channelName = strings.notifChannelName,
            channelDesc = strings.notifChannelDescription
        )
        // Automatically schedule the next reminder
        AndroidWorkoutReminderScheduler.scheduleNext(context)
    }

    companion object {
        const val CHANNEL_ID = "workout_reminders"
        const val NOTIFICATION_ID = 1001

        fun showReminderNotification(
            context: Context,
            title: String,
            message: String,
            channelName: String = "Workout Reminders",
            channelDesc: String = "KumaLift workout session reminder notifications"
        ) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    channelName,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = channelDesc
                    enableVibration(true)
                    enableLights(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(context, CHANNEL_ID)
            } else {
                @Suppress("DEPRECATION")
                Notification.Builder(context)
            }

            val appIconRes = context.applicationInfo.icon.let { if (it != 0) it else android.R.drawable.ic_dialog_info }

            val notification = builder
                .setSmallIcon(appIconRes)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setShowWhen(true)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }
}
