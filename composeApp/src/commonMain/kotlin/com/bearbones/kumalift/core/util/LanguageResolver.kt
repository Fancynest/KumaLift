package com.bearbones.kumalift.core.util

import com.bearbones.kumalift.domain.model.AppearanceSettings
import com.bearbones.kumalift.domain.model.AppLanguage
import com.bearbones.kumalift.domain.model.UserSettings

/**
 * Returns the 2-letter ISO language code of the device / system locale (e.g. "id", "en").
 */
expect fun getSystemLanguageCode(): String

/**
 * Resolves whether the UI should be rendered in English (true) or Indonesian (false).
 *
 * Follows system language by default:
 * - AppLanguage.EN -> Always English (true)
 * - AppLanguage.ID -> Always Indonesian (false)
 * - AppLanguage.SYSTEM ->
 *     - If system language is Indonesian ("id" or "in") -> false (Bahasa Indonesia)
 *     - If system language is English ("en") or any unsupported language -> true (English fallback)
 */
fun resolveIsEnglish(preference: AppLanguage): Boolean {
    return when (preference) {
        AppLanguage.EN -> true
        AppLanguage.ID -> false
        AppLanguage.SYSTEM -> {
            val code = getSystemLanguageCode().lowercase()
            code != "id" && code != "in"
        }
    }
}

val AppearanceSettings.isEnglish: Boolean
    get() = resolveIsEnglish(this.language)

val UserSettings.isEnglish: Boolean
    get() = this.appearance.isEnglish
