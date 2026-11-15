package com.bearbones.kumalift.domain.model

import com.bearbones.kumalift.core.localization.AppStrings

data class GoalRecommendation(
    val goal: TrainingGoal,
    val title: String,
    val phaseLabel: String,
    val defaultSets: Int,
    val defaultReps: Int,
    val setsRangeText: String,
    val repsRangeText: String,
    val restSeconds: Int,
    val restRangeText: String,
    val targetRpe: String,
    val progressionPrinciple: String,
    val workoutGuideline: String
)

object TrainingGoalConfig {

    fun getConfig(goal: TrainingGoal, s: AppStrings): GoalRecommendation {
        val secUnit = if (s.minAbbrev == "mnt") "dtk" else "sec"
        return when (goal) {
            TrainingGoal.HYPERTROPHY -> GoalRecommendation(
                goal = goal,
                title = s.programHypertrophy,
                phaseLabel = s.phaseHypertrophy,
                defaultSets = 3,
                defaultReps = 10,
                setsRangeText = "3–4 ${s.setsAbbrev}",
                repsRangeText = "8–12 ${s.repsAbbrev}",
                restSeconds = 90,
                restRangeText = "60–90 $secUnit",
                targetRpe = "RPE 7–9",
                progressionPrinciple = s.progressionHypertrophy,
                workoutGuideline = s.guidelineHypertrophy
            )
            TrainingGoal.STRENGTH -> GoalRecommendation(
                goal = goal,
                title = s.programStrength,
                phaseLabel = s.phaseStrength,
                defaultSets = 4,
                defaultReps = 5,
                setsRangeText = "3–5 ${s.setsAbbrev}",
                repsRangeText = "3–6 ${s.repsAbbrev}",
                restSeconds = 150,
                restRangeText = "2–3 ${s.minAbbrev}",
                targetRpe = "RPE 8–9.5",
                progressionPrinciple = s.progressionStrength,
                workoutGuideline = s.guidelineStrength
            )
            TrainingGoal.ENDURANCE -> GoalRecommendation(
                goal = goal,
                title = s.programEndurance,
                phaseLabel = s.phaseEndurance,
                defaultSets = 3,
                defaultReps = 15,
                setsRangeText = "3–4 ${s.setsAbbrev}",
                repsRangeText = "15–20+ ${s.repsAbbrev}",
                restSeconds = 45,
                restRangeText = "30–45 $secUnit",
                targetRpe = "RPE 6–8",
                progressionPrinciple = s.progressionEndurance,
                workoutGuideline = s.guidelineEndurance
            )
            TrainingGoal.FAT_LOSS -> GoalRecommendation(
                goal = goal,
                title = s.programFatLoss,
                phaseLabel = s.phaseFatLoss,
                defaultSets = 3,
                defaultReps = 12,
                setsRangeText = "3–4 ${s.setsAbbrev}",
                repsRangeText = "10–15 ${s.repsAbbrev}",
                restSeconds = 60,
                restRangeText = "45–60 $secUnit",
                targetRpe = "RPE 7–8.5",
                progressionPrinciple = s.progressionFatLoss,
                workoutGuideline = s.guidelineFatLoss
            )
            TrainingGoal.GENERAL -> GoalRecommendation(
                goal = goal,
                title = s.programGeneral,
                phaseLabel = s.phaseGeneral,
                defaultSets = 3,
                defaultReps = 10,
                setsRangeText = "2–3 ${s.setsAbbrev}",
                repsRangeText = "8–12 ${s.repsAbbrev}",
                restSeconds = 75,
                restRangeText = "60–90 $secUnit",
                targetRpe = "RPE 7–8",
                progressionPrinciple = s.progressionGeneral,
                workoutGuideline = s.guidelineGeneral
            )
        }
    }

    /**
     * Determines whether an exercise is relevant for a given training goal.
     * Note: Per requirements, exercises can belong to multiple goals.
     */
    fun isExerciseRelevantForGoal(exercise: Exercise, goal: TrainingGoal): Boolean {
        return when (goal) {
            TrainingGoal.STRENGTH -> {
                // Compound movements, barbell, dumbbell, smith machine, and compound bodyweight
                exercise.movementType == MovementType.COMPOUND ||
                        exercise.equipment in listOf(Equipment.BARBELL, Equipment.DUMBBELL, Equipment.SMITH_MACHINE)
            }
            TrainingGoal.HYPERTROPHY -> {
                // Both compound and isolation movements with resistance equipment or focused bodyweight
                exercise.primaryMuscle != MuscleGroup.CARDIO
            }
            TrainingGoal.ENDURANCE -> {
                // Bodyweight, cables, kettlebells, resistance bands, cardio, and high-capacity movements
                exercise.equipment in listOf(Equipment.BODYWEIGHT, Equipment.CABLE, Equipment.BAND, Equipment.KETTLEBELL, Equipment.DUMBBELL) ||
                        exercise.primaryMuscle in listOf(MuscleGroup.CARDIO, MuscleGroup.CORE)
            }
            TrainingGoal.FAT_LOSS -> {
                // Compound movements, kettlebells, bodyweight, cardio, and multi-joint movements
                exercise.movementType == MovementType.COMPOUND ||
                        exercise.equipment in listOf(Equipment.KETTLEBELL, Equipment.BODYWEIGHT, Equipment.DUMBBELL) ||
                        exercise.primaryMuscle in listOf(MuscleGroup.CARDIO, MuscleGroup.LEGS, MuscleGroup.BACK)
            }
            TrainingGoal.GENERAL -> {
                // All exercises are relevant for general fitness
                true
            }
        }
    }
}
