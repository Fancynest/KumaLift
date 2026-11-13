package com.bearbones.kumalift.data.repository

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberSettingsRepository(): SettingsRepository {
    return remember { SettingsRepository() }
}
