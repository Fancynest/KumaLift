package com.bearbones.kumalift.core.notification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.bearbones.kumalift.domain.model.NotificationSettings

actual class WorkoutReminderManager {
    actual fun syncSettings(settings: NotificationSettings) {}
    actual fun hasNotificationPermission(): Boolean = true
    actual fun openNotificationSettings() {}
}

@Composable
actual fun rememberWorkoutReminderManager(): WorkoutReminderManager {
    return remember { WorkoutReminderManager() }
}

@Composable
actual fun rememberNotificationPermissionLauncher(onResult: (Boolean) -> Unit): () -> Unit {
    return { onResult(true) }
}
