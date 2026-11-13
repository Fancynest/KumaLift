package com.bearbones.kumalift.core.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.bearbones.kumalift.domain.model.NotificationSettings
import java.util.Calendar

object AndroidWorkoutReminderScheduler {
    private const val PREFS_NAME = "kumalift_reminders"
    private const val KEY_ENABLED = "reminders_enabled"
    private const val KEY_TIME = "reminder_time"
    private const val KEY_DAYS = "reminder_days"
    const val ACTION_WORKOUT_REMINDER = "com.bearbones.kumalift.ACTION_WORKOUT_REMINDER"
    const val REQUEST_CODE = 4001

    fun saveAndSchedule(context: Context, settings: NotificationSettings) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_ENABLED, settings.workoutReminders)
            .putString(KEY_TIME, settings.reminderTime)
            .putString(KEY_DAYS, settings.reminderDays.joinToString(","))
            .apply()

        if (settings.workoutReminders) {
            scheduleNext(context)
        } else {
            cancelAlarm(context)
        }
    }

    fun rescheduleFromStorage(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_ENABLED, false)
        if (enabled) {
            scheduleNext(context)
        }
    }

    fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, WorkoutReminderReceiver::class.java).apply {
            action = ACTION_WORKOUT_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun scheduleNext(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_ENABLED, false)
        if (!enabled) return

        val timeStr = prefs.getString(KEY_TIME, "17:00") ?: "17:00"
        val daysStr = prefs.getString(KEY_DAYS, "1,2,4,5") ?: "1,2,4,5"
        val days = daysStr.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
        if (days.isEmpty()) return

        val parts = timeStr.split(":")
        val targetHour = parts.getOrNull(0)?.toIntOrNull() ?: 17
        val targetMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val nextTriggerMillis = calculateNextTriggerMillis(targetHour, targetMinute, days) ?: return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, WorkoutReminderReceiver::class.java).apply {
            action = ACTION_WORKOUT_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Using setAndAllowWhileIdle so it triggers even when device is in Doze / Battery Optimization mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextTriggerMillis,
                pendingIntent
            )
        } else {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                nextTriggerMillis,
                pendingIntent
            )
        }
    }

    private fun calculateNextTriggerMillis(hour: Int, minute: Int, days: Set<Int>): Long? {
        val now = Calendar.getInstance()
        for (offset in 0..7) {
            val candidate = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, offset)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // In java.util.Calendar: SUNDAY=1, MONDAY=2, ... SATURDAY=7
            val calDay = candidate.get(Calendar.DAY_OF_WEEK)
            val appDay = when (calDay) {
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY -> 4
                Calendar.FRIDAY -> 5
                Calendar.SATURDAY -> 6
                Calendar.SUNDAY -> 7
                else -> 1
            }

            if (days.contains(appDay) && candidate.timeInMillis > now.timeInMillis + 60_000L) {
                return candidate.timeInMillis
            }
        }
        return null
    }
}
