package com.bearbones.kumalift.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class MesocycleStatus {
    ACTIVE,
    COMPLETED,
    CANCELLED
}

@Serializable
enum class MesocyclePhase {
    INTRODUCTORY,   // Week 1: Establish baseline, RIR 3-4
    ACCUMULATION,   // Weeks 2..N-2: Build volume, progressive overload, RIR 2-3
    PEAK_OVERLOAD,  // Week N-1: High intensity/volume, RIR 0-1
    DELOAD          // Week N: Active recovery, -40% sets, RPE 6-7
}

@Serializable
data class MesocycleBlock(
    val id: String,
    val name: String,
    val programId: String? = null,
    val totalWeeks: Int = 5,
    val currentWeek: Int = 1,
    val deloadWeek: Int = 5,
    val isDeloadActive: Boolean = false,
    val startDateEpochMs: Long = 0L,
    val endDateEpochMs: Long? = null,
    val status: MesocycleStatus = MesocycleStatus.ACTIVE,
    val notes: String = "",
    val createdAtEpochMs: Long = 0L
) {
    val phase: MesocyclePhase
        get() = when {
            isDeloadActive || (currentWeek >= deloadWeek && deloadWeek > 0) -> MesocyclePhase.DELOAD
            currentWeek == 1 -> MesocyclePhase.INTRODUCTORY
            currentWeek == totalWeeks - 1 && totalWeeks > 3 -> MesocyclePhase.PEAK_OVERLOAD
            else -> MesocyclePhase.ACCUMULATION
        }

    val progressFraction: Float
        get() = if (totalWeeks > 0) (currentWeek.toFloat() / totalWeeks.toFloat()).coerceIn(0f, 1f) else 0f

    val isCompleted: Boolean
        get() = status == MesocycleStatus.COMPLETED

    val isActive: Boolean
        get() = status == MesocycleStatus.ACTIVE
}
