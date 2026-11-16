package com.bearbones.kumalift.domain.model

import com.bearbones.kumalift.core.localization.AppStrings
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
    val createdAtEpochMs: Long = 0,
    val presetKey: String? = null,
    val isUserRenamed: Boolean = false
) {
    companion object {
        val DEFAULT_HYPERTROPHY = TrainingProgram(
            id = DEFAULT_PROGRAM_ID,
            name = "Hypertrophy Program",
            description = "",
            category = TrainingGoal.HYPERTROPHY,
            targetDaysPerWeek = 3,
            activeDays = listOf(1, 3, 5),
            isDefault = true,
            createdAtEpochMs = 1700000000000L,
            presetKey = "program_hypertrophy",
            isUserRenamed = false
        )
    }
}

fun TrainingProgram.localizedTitle(s: AppStrings): String {
    if (isUserRenamed || presetKey == null) {
        return name.ifEmpty { s.programHypertrophy }
    }
    return when (presetKey) {
        "program_hypertrophy" -> s.programHypertrophy
        else -> name.ifEmpty { s.programHypertrophy }
    }
}

fun TrainingProgram.localizedDescription(s: AppStrings): String {
    if (isUserRenamed || presetKey == null) {
        return description
    }
    return when (presetKey) {
        "program_hypertrophy" -> s.programHypertrophyDesc
        else -> description
    }
}

