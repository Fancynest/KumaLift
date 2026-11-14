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
            lastPerformedDaysAgo = null
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
            lastPerformedDaysAgo = null
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
            lastPerformedDaysAgo = null
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
            lastPerformedDaysAgo = null
        )
    )

    // Real empty PRs on fresh installation (no invented/mock data per project rules)
    val personalRecords: List<PersonalRecord> = emptyList()
}
