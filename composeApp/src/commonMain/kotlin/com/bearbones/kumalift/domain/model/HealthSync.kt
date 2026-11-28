package com.bearbones.kumalift.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class HealthPlatformType {
    HEALTH_CONNECT,
    HEALTH_KIT,
    NONE
}

@Serializable
enum class HealthSyncStatus {
    CONNECTED,
    NOT_CONFIGURED,
    PERMISSION_REQUIRED,
    UNAVAILABLE
}

@Serializable
data class WorkoutHealthRecord(
    val workoutId: String,
    val title: String,
    val startTimeEpochMs: Long,
    val endTimeEpochMs: Long,
    val activeDurationSeconds: Long,
    val estimatedCaloriesBurned: Double,
    val totalTonnageKg: Double,
    val totalSetsCompleted: Int
) {
    companion object {
        fun estimateCalories(
            durationMinutes: Double,
            bodyWeightKg: Double = 70.0,
            intensityMet: Double = 4.5
        ): Double {
            if (durationMinutes <= 0.0) return 0.0
            val hours = durationMinutes / 60.0
            val calories = intensityMet * bodyWeightKg.coerceAtLeast(30.0) * hours
            return (calories * 10.0).toInt() / 10.0 // 1 decimal place
        }
    }
}

@Serializable
data class HealthSyncResult(
    val isSuccess: Boolean,
    val syncDetails: String = ""
)

@Serializable
data class HealthBodyMeasurementSample(
    val recordId: String,
    val value: Double,
    val recordedAtEpochMs: Long,
    val sourceApp: String = ""
)

@Serializable
data class HealthBodyMetricsSyncPayload(
    val status: HealthSyncStatus,
    val weightSample: HealthBodyMeasurementSample? = null,
    val heightSample: HealthBodyMeasurementSample? = null,
    val hasWeightReadPermission: Boolean = false,
    val hasHeightReadPermission: Boolean = false,
    val errorMessage: String? = null
)
