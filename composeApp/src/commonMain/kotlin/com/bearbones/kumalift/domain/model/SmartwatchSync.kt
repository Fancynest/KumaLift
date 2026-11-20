package com.bearbones.kumalift.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class SmartwatchDeviceType {
    WEAR_OS,
    APPLE_WATCH,
    GENERIC_COMPANION
}

@Serializable
enum class SmartwatchConnectionStatus {
    CONNECTED,
    CONNECTING,
    DISCONNECTED,
    UNAVAILABLE
}

@Serializable
data class SmartwatchWorkoutPayload(
    val workoutId: String,
    val workoutTitle: String,
    val currentExerciseName: String,
    val currentSetNumber: Int,
    val totalSetsForExercise: Int,
    val targetWeightKg: Double,
    val targetReps: Int,
    val isRestTimerActive: Boolean,
    val restSecondsRemaining: Int,
    val elapsedWorkoutSeconds: Long
)

@Serializable
data class SmartwatchSyncEvent(
    val action: String, // e.g. "COMPLETE_SET", "SKIP_REST", "ADD_REST_30S"
    val timestampEpochMs: Long,
    val heartRateBpm: Int? = null
) {
    companion object {
        const val ACTION_COMPLETE_SET = "COMPLETE_SET"
        const val ACTION_SKIP_REST = "SKIP_REST"
        const val ACTION_ADD_REST_30S = "ADD_REST_30S"
        const val ACTION_PAUSE_WORKOUT = "PAUSE_WORKOUT"
        const val ACTION_RESUME_WORKOUT = "RESUME_WORKOUT"
    }
}
