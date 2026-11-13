package com.bearbones.kumalift.data.local

import com.bearbones.kumalift.domain.model.Exercise
import com.bearbones.kumalift.domain.model.PersonalRecord
import com.bearbones.kumalift.domain.model.RoutineBlueprint

object SeedData {

    /**
     * Sourced 100% exclusively from exercises-dataset/data/exercises.json (1,324 exercises).
     * No data from database-latihan-fitnes.md is used.
     */
    val defaultExercises: List<Exercise> = ExerciseDataset.allExercises

    private fun ex(idOrQuery: String): Exercise {
        return defaultExercises.firstOrNull { it.id == idOrQuery }
            ?: defaultExercises.firstOrNull {
                val clean = idOrQuery.removePrefix("ex_").replace("_", " ")
                it.name.contains(clean, ignoreCase = true)
            }
            ?: defaultExercises.first()
    }

    val defaultRoutines: List<RoutineBlueprint> = listOf(
        RoutineBlueprint(
            id = "rt_pull_day_a",
            title = "Pull Day A",
            splitCategory = "PPL Split",
            muscleFocus = "Back · Biceps · Posterior Delts",
            estimatedDurationMinutes = 55,
            targetRpeTier = "Heavy, RPE 8–9",
            exerciseTemplates = listOf(
                ex("0032"), // Barbell Deadlift
                ex("0027"), // Barbell Bent Over Row
                ex("2330")  // Cable Lat Pulldown Full Range Of Motion
            ),
            lastPerformedDaysAgo = "Yesterday"
        ),
        RoutineBlueprint(
            id = "rt_push_day_a",
            title = "Push Day A",
            splitCategory = "PPL Split",
            muscleFocus = "Chest · Triceps · Anterior Delts",
            estimatedDurationMinutes = 48,
            targetRpeTier = "Peak Drive, RPE 8.5",
            exerciseTemplates = listOf(
                ex("0025"), // Barbell Bench Press
                ex("0314"), // Dumbbell Incline Bench Press
                ex("0179"), // Cable Middle Fly
                ex("0241")  // Cable Triceps Pushdown
            ),
            lastPerformedDaysAgo = "2 days ago"
        ),
        RoutineBlueprint(
            id = "rt_legs_a",
            title = "Legs & Posterior",
            splitCategory = "PPL Split",
            muscleFocus = "Quadriceps · Hamstrings · Glutes",
            estimatedDurationMinutes = 60,
            targetRpeTier = "Intense, RPE 9",
            exerciseTemplates = listOf(
                ex("0043"), // Barbell Full Squat
                ex("0085")  // Barbell Romanian Deadlift
            ),
            lastPerformedDaysAgo = "4 days ago"
        ),
        RoutineBlueprint(
            id = "rt_upper_body",
            title = "Upper Body Power",
            splitCategory = "Upper / Lower",
            muscleFocus = "Chest · Back · Shoulders",
            estimatedDurationMinutes = 55,
            targetRpeTier = "Strength, RPE 8",
            exerciseTemplates = listOf(
                ex("0025"), // Barbell Bench Press
                ex("0091"), // Barbell Seated Overhead Press
                ex("0027")  // Barbell Bent Over Row
            ),
            lastPerformedDaysAgo = "Last week"
        )
    )

    val personalRecords: List<PersonalRecord> = listOf(
        PersonalRecord(
            exerciseId = "0025",
            exerciseName = "Barbell Bench Press",
            weightKg = 118.0,
            reps = 3,
            estimated1RmKg = 129.8,
            dateAchieved = "Oct 28",
            deltaWeightKg = 3.0,
            prTier = "3RM PR"
        ),
        PersonalRecord(
            exerciseId = "0043",
            exerciseName = "Barbell Full Squat",
            weightKg = 150.0,
            reps = 5,
            estimated1RmKg = 175.0,
            dateAchieved = "Nov 02",
            deltaWeightKg = 5.0,
            prTier = "5RM PR"
        ),
        PersonalRecord(
            exerciseId = "0032",
            exerciseName = "Barbell Deadlift",
            weightKg = 185.0,
            reps = 3,
            estimated1RmKg = 203.5,
            dateAchieved = "Nov 06",
            deltaWeightKg = 5.0,
            prTier = "3RM PR"
        ),
        PersonalRecord(
            exerciseId = "0091",
            exerciseName = "Barbell Seated Overhead Press",
            weightKg = 72.5,
            reps = 5,
            estimated1RmKg = 84.5,
            dateAchieved = "Nov 10",
            deltaWeightKg = 2.5,
            prTier = "5RM PR"
        )
    )
}
