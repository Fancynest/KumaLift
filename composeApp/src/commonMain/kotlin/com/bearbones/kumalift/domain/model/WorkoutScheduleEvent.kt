package com.bearbones.kumalift.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
enum class ScheduleStatus {
    PLANNED,
    COMPLETED,
    CANCELLED
}

@Serializable
data class WorkoutScheduleEvent(
    val id: String,
    val routineId: String? = null,
    val scheduledDate: String, // "YYYY-MM-DD" canonical local calendar date
    val status: ScheduleStatus = ScheduleStatus.PLANNED,
    val linkedWorkoutId: String? = null,
    val routineTitleSnapshot: String,
    val splitTypeSnapshot: String? = null,
    val notes: String? = null,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
) {
    val localDate: LocalDate?
        get() = try {
            LocalDate.parse(scheduledDate)
        } catch (_: Exception) {
            null
        }

    val isPlanned: Boolean
        get() = status == ScheduleStatus.PLANNED

    val isCompleted: Boolean
        get() = status == ScheduleStatus.COMPLETED

    val isCancelled: Boolean
        get() = status == ScheduleStatus.CANCELLED
}
