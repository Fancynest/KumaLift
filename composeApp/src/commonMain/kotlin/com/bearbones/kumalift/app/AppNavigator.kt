package com.bearbones.kumalift.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

enum class Tab {
    HOME,
    ROUTINES,
    LIBRARY,
    PROGRESS,
    ME
}

sealed interface Overlay {
    data object ActiveLogger : Overlay
    data object Summary : Overlay
    data object Settings : Overlay
    data object AiChat : Overlay
    data object ProgressPhotos : Overlay
}

/**
 * Pure Kotlin navigator serving as the single source of truth for app navigation.
 *
 * Implements:
 * - Deterministic tab switching without pager gesture conflicts or async state races.
 * - Overlay stack for fullscreen screens (Active Logger, Summary, Settings, AI Chat).
 * - Root-level back handling with fallback to Home and finally system exit.
 */
class AppNavigator(
    initialTab: Tab = Tab.HOME,
    initialOverlays: List<Overlay> = emptyList()
) {
    var selectedTab by mutableStateOf(initialTab)
    var pendingSettingsDestinationName by mutableStateOf<String?>(null)
    val overlays = mutableStateListOf<Overlay>().apply {
        addAll(initialOverlays)
    }

    val currentOverlay: Overlay?
        get() = overlays.lastOrNull()

    fun selectTab(tab: Tab) {
        pendingSettingsDestinationName = null
        overlays.clear()
        selectedTab = tab
    }

    fun push(overlay: Overlay) {
        if (overlays.lastOrNull() != overlay) {
            overlays.add(overlay)
        }
    }

    fun clearOverlays() {
        overlays.clear()
        pendingSettingsDestinationName = null
    }

    fun popOverlay(): Boolean {
        if (overlays.isNotEmpty()) {
            val removed = overlays.removeAt(overlays.lastIndex)
            if (removed == Overlay.Settings) {
                pendingSettingsDestinationName = null
            }
            return true
        }
        return false
    }

    fun back(): Boolean {
        if (overlays.isNotEmpty()) {
            val removed = overlays.removeAt(overlays.lastIndex)
            if (removed == Overlay.Settings) {
                pendingSettingsDestinationName = null
            }
            return true
        }
        if (selectedTab != Tab.HOME) {
            selectedTab = Tab.HOME
            return true
        }
        return false
    }

    companion object {
        val Saver: Saver<AppNavigator, Any> = listSaver(
            save = { nav ->
                listOf(
                    nav.selectedTab.name,
                    nav.overlays.map { overlay ->
                        when (overlay) {
                            Overlay.ActiveLogger -> "ACTIVE_LOGGER"
                            Overlay.Summary -> "SUMMARY"
                            Overlay.Settings -> "SETTINGS"
                            Overlay.AiChat -> "AI_CHAT"
                            Overlay.ProgressPhotos -> "PROGRESS_PHOTOS"
                        }
                    }
                )
            },
            restore = { list ->
                val tabName = list.getOrNull(0) as? String
                val tab = tabName?.let { runCatching { Tab.valueOf(it) }.getOrNull() } ?: Tab.HOME
                @Suppress("UNCHECKED_CAST")
                val overlayNames = list.getOrNull(1) as? List<String> ?: emptyList()
                val restoredOverlays = overlayNames.mapNotNull { name ->
                    when (name) {
                        "ACTIVE_LOGGER" -> Overlay.ActiveLogger
                        "SUMMARY" -> Overlay.Summary
                        "SETTINGS" -> Overlay.Settings
                        "AI_CHAT" -> Overlay.AiChat
                        "PROGRESS_PHOTOS" -> Overlay.ProgressPhotos
                        else -> null
                    }
                }
                AppNavigator(initialTab = tab, initialOverlays = restoredOverlays)
            }
        )
    }
}

@Composable
fun rememberAppNavigator(
    initialTab: Tab = Tab.HOME,
    initialOverlays: List<Overlay> = emptyList()
): AppNavigator {
    return rememberSaveable(saver = AppNavigator.Saver) {
        AppNavigator(initialTab, initialOverlays)
    }
}
