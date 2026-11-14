package com.bearbones.kumalift.core.util

import java.util.Locale

actual fun getSystemLanguageCode(): String {
    return try {
        Locale.getDefault().language ?: "en"
    } catch (_: Throwable) {
        "en"
    }
}
