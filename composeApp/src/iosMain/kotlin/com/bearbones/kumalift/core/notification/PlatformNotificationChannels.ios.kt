package com.bearbones.kumalift.core.notification

import com.bearbones.kumalift.domain.model.AppLanguage

actual fun syncPlatformNotificationChannels(language: AppLanguage) {
    // iOS does not use Android-style notification channels
}
