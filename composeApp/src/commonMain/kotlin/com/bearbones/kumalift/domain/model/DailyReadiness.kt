package com.bearbones.kumalift.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable

@Serializable
enum class ReadinessRecommendation {
    FULL_INTENSITY,    // Score >= 80%: Peak condition, green light for PR attempts
    MODERATE_LOAD,     // Score 55-79%: Stable baseline, stick to prescribed volume
    LIGHT_RECOVERY     // Score < 55%: Elevated fatigue, consider auto-reducing volume or deload
}

@Serializable
data class DailyReadinessCheckIn(
    val id: String,
    val dateString: String, // "YYYY-MM-DD"
    val sleepScore: Int,    // 1 (Poor) - 5 (Excellent)
    val sorenessScore: Int, // 1 (Very sore) - 5 (Fresh)
    val energyScore: Int,   // 1 (Exhausted) - 5 (Energized)
    val stressScore: Int,   // 1 (High stress) - 5 (Calm/Relaxed)
    val overallPercentage: Int, // 0 - 100%
    val recommendation: ReadinessRecommendation,
    val notes: String = "",
    val createdAtEpochMs: Long
) {
    companion object {
        fun todayDateString(tz: TimeZone = TimeZone.currentSystemDefault()): String {
            return Clock.System.now().toLocalDateTime(tz).date.toString()
        }

        fun calculate(
            sleep: Int,
            soreness: Int,
            energy: Int,
            stress: Int
        ): Pair<Int, ReadinessRecommendation> {
            val sum = sleep + soreness + energy + stress
            val pct = (((sum - 4).toDouble() / 16.0) * 100.0).toInt().coerceIn(0, 100)
            val rec = when {
                pct >= 80 -> ReadinessRecommendation.FULL_INTENSITY
                pct >= 55 -> ReadinessRecommendation.MODERATE_LOAD
                else -> ReadinessRecommendation.LIGHT_RECOVERY
            }
            return Pair(pct, rec)
        }
    }
}
