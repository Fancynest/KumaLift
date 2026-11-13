package com.bearbones.kumalift.data.local

import com.bearbones.kumalift.domain.model.Equipment
import com.bearbones.kumalift.domain.model.Exercise
import com.bearbones.kumalift.domain.model.MuscleGroup
import com.bearbones.kumalift.domain.model.PersonalRecord
import com.bearbones.kumalift.domain.model.RoutineBlueprint

object SeedData {

    val defaultExercises: List<Exercise> = listOf(
        Exercise(
            id = "ex_bench_press",
            name = "Barbell Bench Press",
            primaryMuscle = MuscleGroup.CHEST,
            secondaryMuscles = listOf(MuscleGroup.ARMS, MuscleGroup.SHOULDERS),
            equipment = Equipment.BARBELL,
            defaultIncrementKg = 2.5,
            currentPrWeightKg = 118.0,
            currentPrReps = 3,
            lastLoggedDate = "2 days ago"
        ),
        Exercise(
            id = "ex_incline_db_press",
            name = "Incline Dumbbell Press",
            primaryMuscle = MuscleGroup.CHEST,
            secondaryMuscles = listOf(MuscleGroup.SHOULDERS, MuscleGroup.ARMS),
            equipment = Equipment.DUMBBELL,
            defaultIncrementKg = 2.0,
            currentPrWeightKg = 38.0,
            currentPrReps = 8,
            lastLoggedDate = "2 days ago"
        ),
        Exercise(
            id = "ex_cable_fly",
            name = "Cable Chest Flyes",
            primaryMuscle = MuscleGroup.CHEST,
            secondaryMuscles = emptyList(),
            equipment = Equipment.CABLE,
            defaultIncrementKg = 1.25,
            currentPrWeightKg = 18.0,
            currentPrReps = 12,
            lastLoggedDate = "5 days ago"
        ),
        Exercise(
            id = "ex_squat",
            name = "Barbell Back Squat",
            primaryMuscle = MuscleGroup.LEGS,
            secondaryMuscles = listOf(MuscleGroup.CORE),
            equipment = Equipment.BARBELL,
            defaultIncrementKg = 5.0,
            currentPrWeightKg = 150.0,
            currentPrReps = 5,
            lastLoggedDate = "4 days ago"
        ),
        Exercise(
            id = "ex_deadlift",
            name = "Conventional Deadlift",
            primaryMuscle = MuscleGroup.BACK,
            secondaryMuscles = listOf(MuscleGroup.LEGS, MuscleGroup.CORE),
            equipment = Equipment.BARBELL,
            defaultIncrementKg = 5.0,
            currentPrWeightKg = 185.0,
            currentPrReps = 3,
            lastLoggedDate = "1 week ago"
        ),
        Exercise(
            id = "ex_ohp",
            name = "Overhead Barbell Press",
            primaryMuscle = MuscleGroup.SHOULDERS,
            secondaryMuscles = listOf(MuscleGroup.ARMS, MuscleGroup.CORE),
            equipment = Equipment.BARBELL,
            defaultIncrementKg = 2.5,
            currentPrWeightKg = 72.5,
            currentPrReps = 5,
            lastLoggedDate = "3 days ago"
        ),
        Exercise(
            id = "ex_barbell_row",
            name = "Barbell Bent-Over Row",
            primaryMuscle = MuscleGroup.BACK,
            secondaryMuscles = listOf(MuscleGroup.ARMS),
            equipment = Equipment.BARBELL,
            defaultIncrementKg = 2.5,
            currentPrWeightKg = 95.0,
            currentPrReps = 6,
            lastLoggedDate = "Yesterday"
        ),
        Exercise(
            id = "ex_lat_pulldown",
            name = "Lat Pulldown (Wide Grip)",
            primaryMuscle = MuscleGroup.BACK,
            secondaryMuscles = listOf(MuscleGroup.ARMS),
            equipment = Equipment.CABLE,
            defaultIncrementKg = 2.5,
            currentPrWeightKg = 80.0,
            currentPrReps = 10,
            lastLoggedDate = "Yesterday"
        ),
        Exercise(
            id = "ex_tricep_pushdown",
            name = "Tricep Rope Pushdown",
            primaryMuscle = MuscleGroup.ARMS,
            secondaryMuscles = emptyList(),
            equipment = Equipment.CABLE,
            defaultIncrementKg = 1.25,
            currentPrWeightKg = 32.5,
            currentPrReps = 12,
            lastLoggedDate = "2 days ago"
        ),
        Exercise(
            id = "ex_lateral_raise",
            name = "Dumbbell Lateral Raise",
            primaryMuscle = MuscleGroup.SHOULDERS,
            secondaryMuscles = emptyList(),
            equipment = Equipment.DUMBBELL,
            defaultIncrementKg = 1.0,
            currentPrWeightKg = 16.0,
            currentPrReps = 12,
            lastLoggedDate = "3 days ago"
        ),
        Exercise(
            id = "ex_romanian_deadlift",
            name = "Romanian Deadlift",
            primaryMuscle = MuscleGroup.LEGS,
            secondaryMuscles = listOf(MuscleGroup.BACK),
            equipment = Equipment.BARBELL,
            defaultIncrementKg = 5.0,
            currentPrWeightKg = 130.0,
            currentPrReps = 8,
            lastLoggedDate = "4 days ago"
        ),
        Exercise(
            id = "ex_hanging_knee_raise",
            name = "Hanging Knee Raise",
            primaryMuscle = MuscleGroup.CORE,
            secondaryMuscles = emptyList(),
            equipment = Equipment.BODYWEIGHT,
            defaultIncrementKg = 0.0,
            currentPrWeightKg = 0.0,
            currentPrReps = 20,
            lastLoggedDate = "Yesterday"
        )
    )

    val defaultRoutines: List<RoutineBlueprint> = listOf(
        RoutineBlueprint(
            id = "rt_pull_day_a",
            title = "Pull Day A",
            splitCategory = "PPL Split",
            muscleFocus = "Back · Biceps · Posterior Delts",
            estimatedDurationMinutes = 55,
            targetRpeTier = "Heavy, RPE 8–9",
            exerciseTemplates = listOf(
                defaultExercises[4], // Deadlift
                defaultExercises[6], // Barbell Row
                defaultExercises[7]  // Lat Pulldown
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
                defaultExercises[0], // Bench Press
                defaultExercises[1], // Incline DB
                defaultExercises[2], // Cable Fly
                defaultExercises[8]  // Tricep Pushdown
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
                defaultExercises[3], // Squat
                defaultExercises[10] // RDL
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
                defaultExercises[0], // Bench Press
                defaultExercises[5], // OHP
                defaultExercises[6]  // Row
            ),
            lastPerformedDaysAgo = "Last week"
        )
    )

    val personalRecords: List<PersonalRecord> = listOf(
        PersonalRecord(
            exerciseId = "ex_bench_press",
            exerciseName = "Barbell Bench Press",
            weightKg = 118.0,
            reps = 3,
            estimated1RmKg = 129.8,
            dateAchieved = "Oct 28",
            deltaWeightKg = 3.0,
            prTier = "3RM PR"
        ),
        PersonalRecord(
            exerciseId = "ex_squat",
            exerciseName = "Barbell Back Squat",
            weightKg = 150.0,
            reps = 5,
            estimated1RmKg = 175.0,
            dateAchieved = "Nov 02",
            deltaWeightKg = 5.0,
            prTier = "5RM PR"
        ),
        PersonalRecord(
            exerciseId = "ex_deadlift",
            exerciseName = "Conventional Deadlift",
            weightKg = 185.0,
            reps = 3,
            estimated1RmKg = 203.5,
            dateAchieved = "Nov 06",
            deltaWeightKg = 5.0,
            prTier = "3RM PR"
        ),
        PersonalRecord(
            exerciseId = "ex_ohp",
            exerciseName = "Overhead Barbell Press",
            weightKg = 72.5,
            reps = 5,
            estimated1RmKg = 84.5,
            dateAchieved = "Nov 10",
            deltaWeightKg = 2.5,
            prTier = "5RM PR"
        )
    )
}
