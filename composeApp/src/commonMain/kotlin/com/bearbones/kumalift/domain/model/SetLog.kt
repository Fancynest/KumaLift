package com.bearbones.kumalift.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class SetType(val shortCode: String) {
    WARMUP("W"),
    NORMAL(""),
    DROP("D"),
    FAILURE("F")
}

@Serializable
data class SetLog(
    val id: String,
    val setNumber: Int,
    val setType: SetType = SetType.NORMAL,
    val weightKg: Double = 0.0,
    val reps: Int = 0,
    val rpe: Double? = null,
    val isCompleted: Boolean = false,
    val previousWeightKg: Double? = null,
    val previousReps: Int? = null,
    val targetWeightKg: Double? = null,
    val targetReps: Int? = null,
    val heartRateBpm: Int? = null,
    val isPr: Boolean = false
) {
    val estimated1RmEpley: Double
        get() = if (reps > 0 && weightKg > 0) weightKg * (1.0 + reps / 30.0) else 0.0

    val volume: Double
        get() = if (isCompleted) weightKg * reps else 0.0
}
