package com.bearbones.kumalift.domain.model

import kotlinx.serialization.Serializable

const val DEFAULT_PROGRAM_ID = "program_hypertrophy_default"

@Serializable
data class TrainingProgram(
    val id: String,
    val name: String,
    val description: String = "",
    val category: TrainingGoal = TrainingGoal.HYPERTROPHY,
    val targetDaysPerWeek: Int = 3,
    val activeDays: List<Int> = listOf(1, 3, 5),
    val isDefault: Boolean = false,
    val createdAtEpochMs: Long = 0
) {
    companion object {
        val DEFAULT_HYPERTROPHY = TrainingProgram(
            id = DEFAULT_PROGRAM_ID,
            name = "Hypertrophy Program",
            description = "Focused on muscle hypertrophy and progressive overload.",
            category = TrainingGoal.HYPERTROPHY,
            targetDaysPerWeek = 3,
            activeDays = listOf(1, 3, 5),
            isDefault = true,
            createdAtEpochMs = 1700000000000L
        )
    }
}
