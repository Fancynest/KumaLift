package com.bearbones.kumalift.domain.model

import com.bearbones.kumalift.data.repository.PersonalRecordRepository
import com.bearbones.kumalift.data.repository.WorkoutRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SmartExerciseAlternativeAdvisorTests {

    private val barbellBench = Exercise(
        id = "ex_bb_bench",
        name = "Barbell Bench Press",
        primaryMuscle = MuscleGroup.CHEST,
        subMuscle = "mid_chest",
        secondaryMuscles = listOf(MuscleGroup.ARMS, MuscleGroup.SHOULDERS),
        movementType = MovementType.COMPOUND,
        equipment = Equipment.BARBELL
    )

    private val dumbbellBench = Exercise(
        id = "ex_db_bench",
        name = "Dumbbell Bench Press",
        primaryMuscle = MuscleGroup.CHEST,
        subMuscle = "mid_chest",
        secondaryMuscles = listOf(MuscleGroup.ARMS, MuscleGroup.SHOULDERS),
        movementType = MovementType.COMPOUND,
        equipment = Equipment.DUMBBELL
    )

    private val machineChestPress = Exercise(
        id = "ex_machine_chest",
        name = "Machine Chest Press",
        primaryMuscle = MuscleGroup.CHEST,
        subMuscle = "mid_chest",
        secondaryMuscles = listOf(MuscleGroup.ARMS),
        movementType = MovementType.COMPOUND,
        equipment = Equipment.MACHINE
    )

    private val cableChestFly = Exercise(
        id = "ex_cable_fly",
        name = "Cable Chest Fly",
        primaryMuscle = MuscleGroup.CHEST,
        subMuscle = "mid_chest",
        secondaryMuscles = emptyList(),
        movementType = MovementType.ISOLATION,
        equipment = Equipment.CABLE
    )

    private val overheadPress = Exercise(
        id = "ex_ohp",
        name = "Overhead Press",
        primaryMuscle = MuscleGroup.SHOULDERS,
        subMuscle = "front_delts",
        secondaryMuscles = listOf(MuscleGroup.ARMS),
        movementType = MovementType.COMPOUND,
        equipment = Equipment.BARBELL
    )

    private val barbellSquat = Exercise(
        id = "ex_squat",
        name = "Barbell Squat",
        primaryMuscle = MuscleGroup.LEGS,
        movementType = MovementType.COMPOUND,
        equipment = Equipment.BARBELL
    )

    private val dataset = listOf(
        barbellBench,
        dumbbellBench,
        machineChestPress,
        cableChestFly,
        overheadPress,
        barbellSquat
    )

    @Test
    fun testSmartAlternativesRanking() {
        val alternatives = SmartExerciseAlternativeAdvisor.findAlternatives(
            targetExercise = barbellBench,
            allExercises = dataset
        )

        assertTrue(alternatives.isNotEmpty())
        // Top alternatives must be compound chest movements (Dumbbell Bench / Machine Chest Press)
        val topIds = alternatives.take(2).map { it.exercise.id }
        assertTrue(topIds.contains("ex_db_bench"))
        assertTrue(topIds.contains("ex_machine_chest"))

        val dbBenchAlternative = alternatives.first { it.exercise.id == "ex_db_bench" }
        assertEquals("same_pattern", dbBenchAlternative.matchReasonCode)
        assertTrue(dbBenchAlternative.score >= 80)

        // Cable fly should be included as same muscle, but ranked lower due to isolation
        val flyAlternative = alternatives.find { it.exercise.id == "ex_cable_fly" }
        assertNotNull(flyAlternative)
        assertEquals("same_muscle", flyAlternative.matchReasonCode)
        assertTrue(flyAlternative.score < dbBenchAlternative.score)

        // Unrelated squat should not be returned as an alternative
        assertTrue(alternatives.none { it.exercise.id == "ex_squat" })
    }

    @Test
    fun testEquipmentFilter() {
        val dumbellOnly = SmartExerciseAlternativeAdvisor.findAlternatives(
            targetExercise = barbellBench,
            allExercises = dataset,
            equipmentFilter = Equipment.DUMBBELL
        )

        assertEquals(1, dumbellOnly.size)
        assertEquals("ex_db_bench", dumbellOnly.first().exercise.id)

        val cableOnly = SmartExerciseAlternativeAdvisor.findAlternatives(
            targetExercise = barbellBench,
            allExercises = dataset,
            equipmentFilter = Equipment.CABLE
        )

        assertEquals(1, cableOnly.size)
        assertEquals("ex_cable_fly", cableOnly.first().exercise.id)
    }

    @Test
    fun testRepositoryReplaceExerciseInActiveWorkout() {
        val prRepo = PersonalRecordRepository()
        val workoutRepo = WorkoutRepository(prRepository = prRepo)

        workoutRepo.startEmptyWorkout("Morning Lift")
        workoutRepo.addExerciseToActiveWorkout(barbellBench)

        val initialWorkout = workoutRepo.activeWorkout.value
        assertNotNull(initialWorkout)
        val initialExercise = initialWorkout.exercises.first()
        assertEquals("ex_bb_bench", initialExercise.exercise.id)

        // Swap to Dumbbell Bench Press
        workoutRepo.replaceExerciseInActiveWorkout(initialExercise.id, dumbbellBench)

        val updatedWorkout = workoutRepo.activeWorkout.value
        assertNotNull(updatedWorkout)
        assertEquals(1, updatedWorkout.exercises.size)
        assertEquals("ex_db_bench", updatedWorkout.exercises.first().exercise.id)
        assertEquals("Dumbbell Bench Press", updatedWorkout.exercises.first().exercise.name)
    }
}
