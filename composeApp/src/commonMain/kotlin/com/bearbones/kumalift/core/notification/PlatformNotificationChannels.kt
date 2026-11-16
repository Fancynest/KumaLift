package com.bearbones.kumalift.core.notification

import com.bearbones.kumalift.domain.model.AppLanguage

/**
 * Re-registers platform notification channels with localized names and descriptions
 * based on the user's chosen language, and syncs platform preferences.
 */
expect fun syncPlatformNotificationChannels(language: AppLanguage)
