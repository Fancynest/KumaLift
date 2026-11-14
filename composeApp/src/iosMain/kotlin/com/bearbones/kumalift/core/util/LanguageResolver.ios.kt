package com.bearbones.kumalift.core.util

import platform.Foundation.NSLocale
import platform.Foundation.preferredLanguages

actual fun getSystemLanguageCode(): String {
    return try {
        val preferred = NSLocale.preferredLanguages.firstOrNull() as? String
        preferred?.take(2)?.lowercase() ?: "en"
    } catch (_: Throwable) {
        "en"
    }
}
