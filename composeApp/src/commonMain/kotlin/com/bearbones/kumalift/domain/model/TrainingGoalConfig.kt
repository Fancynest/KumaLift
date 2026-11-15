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
        return when (goal) {
            TrainingGoal.HYPERTROPHY -> GoalRecommendation(
                goal = goal,
                title = s.programHypertrophy,
                phaseLabel = s.phaseHypertrophy,
                defaultSets = 3,
                defaultReps = 10,
                setsRangeText = "3–4 Set",
                repsRangeText = "8–12 Reps",
                restSeconds = 90,
                restRangeText = "60–90 dtk",
                targetRpe = "RPE 7–9",
                progressionPrinciple = "Beban bertahap (Progressive Overload), fokus pada waktu di bawah tegangan (TUT) dan kontrol fase eksentrik.",
                workoutGuideline = "Volume moderat hingga tinggi. Fokus pada kontraksi otot maksimal dan istirahat cukup untuk pemulihan glikogen otot."
            )
            TrainingGoal.STRENGTH -> GoalRecommendation(
                goal = goal,
                title = s.programStrength,
                phaseLabel = s.phaseStrength,
                defaultSets = 4,
                defaultReps = 5,
                setsRangeText = "3–5 Set",
                repsRangeText = "3–6 Reps",
                restSeconds = 150,
                restRangeText = "2–3 mnt",
                targetRpe = "RPE 8–9.5",
                progressionPrinciple = "Progresi beban linear dengan fokus peningkatan beban angkatan (intensity % 1RM).",
                workoutGuideline = "Prioritaskan gerakan compound multi-sendi (Bench, Squat, Deadlift, OHP). Istirahat lebih panjang untuk pemulihan sistem saraf pusat (CNS)."
            )
            TrainingGoal.ENDURANCE -> GoalRecommendation(
                goal = goal,
                title = s.programEndurance,
                phaseLabel = s.phaseEndurance,
                defaultSets = 3,
                defaultReps = 15,
                setsRangeText = "3–4 Set",
                repsRangeText = "15–20+ Reps",
                restSeconds = 45,
                restRangeText = "30–45 dtk",
                targetRpe = "RPE 6–8",
                progressionPrinciple = "Peningkatan repetisi, efisiensi gerak, dan pemendekan interval istirahat.",
                workoutGuideline = "Gunakan beban ringan hingga sedang dengan repetisi tinggi dan istirahat singkat untuk melatih daya tahan otot dan kapasitas aerobik/anaerobik."
            )
            TrainingGoal.FAT_LOSS -> GoalRecommendation(
                goal = goal,
                title = s.programFatLoss,
                phaseLabel = s.phaseFatLoss,
                defaultSets = 3,
                defaultReps = 12,
                setsRangeText = "3–4 Set",
                repsRangeText = "10–15 Reps",
                restSeconds = 60,
                restRangeText = "45–60 dtk",
                targetRpe = "RPE 7–8.5",
                progressionPrinciple = "Kepadatan latihan (density training) dan resistensi metabolik untuk memaksimalkan pembakaran kalori.",
                workoutGuideline = "Kombinasi compound resistance training dengan tempo dinamis dan istirahat singkat untuk menjaga denyut jantung tinggi dan membakar lemak optimal."
            )
            TrainingGoal.GENERAL -> GoalRecommendation(
                goal = goal,
                title = s.programGeneral,
                phaseLabel = s.phaseGeneral,
                defaultSets = 3,
                defaultReps = 10,
                setsRangeText = "2–3 Set",
                repsRangeText = "8–12 Reps",
                restSeconds = 75,
                restRangeText = "60–90 dtk",
                targetRpe = "RPE 7–8",
                progressionPrinciple = "Kebugaran holistik seimbang antara kekuatan, mobilitas, dan daya tahan tubuh.",
                workoutGuideline = "Latihan seimbang seluruh tubuh (Full Body) untuk menjaga stamina harian, postur sehat, dan mobilitas sendi."
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
