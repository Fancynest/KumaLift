package com.bearbones.kumalift.core.notification

import androidx.compose.runtime.Composable
import com.bearbones.kumalift.domain.model.NotificationSettings

expect class WorkoutReminderManager {
    fun syncSettings(settings: NotificationSettings)
    fun isBatteryOptimizationIgnored(): Boolean
    fun requestIgnoreBatteryOptimization()
    fun hasNotificationPermission(): Boolean
    fun openNotificationSettings()
}

@Composable
expect fun rememberWorkoutReminderManager(): WorkoutReminderManager

@Composable
expect fun rememberNotificationPermissionLauncher(onResult: (Boolean) -> Unit): () -> Unit
