package com.bearbones.kumalift.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class RoutineExerciseConfig(
    val exercise: Exercise,
    val targetSets: Int = 3,
    val targetReps: Int = 8,
    val targetWeightKg: Double? = null,
    val restTimerSeconds: Int = 90,
    val supersetGroupId: String? = null
)

@Serializable
data class RoutineBlueprint(
    val id: String,
    val programId: String = DEFAULT_PROGRAM_ID,
    val title: String,
    val splitCategory: String, // "PPL Split", "Upper / Lower", "Full Body"
    val muscleFocus: String,   // "Chest · Triceps · Delts"
    val estimatedDurationMinutes: Int = 60,
    val targetRpeTier: String = "Heavy, RPE 8–9",
    val exerciseTemplates: List<Exercise> = emptyList(),
    val exerciseConfigs: List<RoutineExerciseConfig> = emptyList(),
    val plannedSetsPerExercise: Int = 3,
    val lastPerformedDaysAgo: String? = null,
    val scheduledDays: List<Int> = emptyList() // 1 = Monday .. 7 = Sunday
) {
    /**
     * Resolves effective exercise configurations, ensuring backward compatibility with
     * older routines that only have `exerciseTemplates`.
     */
    val effectiveConfigs: List<RoutineExerciseConfig>
        get() {
            if (exerciseConfigs.isNotEmpty()) return exerciseConfigs
            return exerciseTemplates.map {
                RoutineExerciseConfig(
                    exercise = it,
                    targetSets = plannedSetsPerExercise,
                    targetReps = 8,
                    targetWeightKg = null,
                    restTimerSeconds = 90
                )
            }
        }
}
