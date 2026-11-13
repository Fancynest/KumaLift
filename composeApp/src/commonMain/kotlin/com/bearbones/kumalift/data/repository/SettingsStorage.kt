package com.bearbones.kumalift.data.repository

import androidx.compose.runtime.Composable

/**
 * Returns a SettingsRepository populated with persisted user preferences on platforms that support it.
 */
@Composable
expect fun rememberSettingsRepository(): SettingsRepository
