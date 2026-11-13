package com.bearbones.kumalift.core.notification

import androidx.compose.runtime.Composable
import com.bearbones.kumalift.domain.model.NotificationSettings

expect class WorkoutReminderManager {
    fun syncSettings(settings: NotificationSettings)
    fun isBatteryOptimizationIgnored(): Boolean
    fun requestIgnoreBatteryOptimization()
    fun sendTestNotification(title: String, message: String)
}

@Composable
expect fun rememberWorkoutReminderManager(): WorkoutReminderManager
