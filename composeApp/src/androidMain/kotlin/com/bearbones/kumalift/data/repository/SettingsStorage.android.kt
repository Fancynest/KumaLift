package com.bearbones.kumalift.data.repository

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberSettingsRepository(): SettingsRepository {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        val store = AndroidSettingsStore(context)
        SettingsRepository(
            initialSettings = store.load(),
            onSave = { updated -> store.save(updated) }
        )
    }
}
