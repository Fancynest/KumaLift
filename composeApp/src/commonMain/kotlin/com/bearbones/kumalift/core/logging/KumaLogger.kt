package com.bearbones.kumalift.core.logging

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Structured local diagnostic logger for KumaLift.
 *
 * Operates purely locally with no network calls, telemetry, or remote services.
 * Writes to a thread-safe, bounded memory buffer (up to 1,000 entries) when enabled
 * via the `security.localLogging` setting. Supports exporting formatted log files for
 * offline debugging and support.
 */
object KumaLogger {

    @Volatile
    var isEnabled: Boolean = false

    private const val MAX_LOG_ENTRIES = 1000
    private val buffer = mutableListOf<String>()
    private val lock = Any()

    fun d(tag: String, message: String) {
        log("DEBUG", tag, message, null)
    }

    fun i(tag: String, message: String) {
        log("INFO", tag, message, null)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        log("WARN", tag, message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        log("ERROR", tag, message, throwable)
    }

    private fun log(level: String, tag: String, message: String, throwable: Throwable?) {
        if (!isEnabled) return
        val now = Clock.System.now()
        val dt = now.toLocalDateTime(TimeZone.currentSystemDefault())
        val timeStr = "${dt.year}-${dt.monthNumber.toString().padStart(2, '0')}-${dt.dayOfMonth.toString().padStart(2, '0')} " +
                "${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}:${dt.second.toString().padStart(2, '0')}"
        val stackTrace = throwable?.stackTraceToString()?.let { "\n$it" } ?: ""
        val entry = "[$timeStr] [$level] [$tag]: $message$stackTrace"

        synchronized(lock) {
            if (buffer.size >= MAX_LOG_ENTRIES) {
                buffer.removeAt(0)
            }
            buffer.add(entry)
        }
    }

    fun getLogs(): String {
        return synchronized(lock) {
            buffer.joinToString(separator = "\n")
        }
    }

    fun clear() {
        synchronized(lock) {
            buffer.clear()
        }
    }

    fun logCount(): Int {
        return synchronized(lock) {
            buffer.size
        }
    }
}
