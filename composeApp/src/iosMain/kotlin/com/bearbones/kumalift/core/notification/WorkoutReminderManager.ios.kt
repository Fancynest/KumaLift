package com.bearbones.kumalift.core.notification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.bearbones.kumalift.domain.model.NotificationSettings

actual class WorkoutReminderManager {
    actual fun syncSettings(settings: NotificationSettings) {}
    actual fun isBatteryOptimizationIgnored(): Boolean = true
    actual fun requestIgnoreBatteryOptimization() {}
    actual fun sendTestNotification(title: String, message: String) {}
}

@Composable
actual fun rememberWorkoutReminderManager(): WorkoutReminderManager {
    return remember { WorkoutReminderManager() }
}
