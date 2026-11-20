package com.bearbones.kumalift.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MuscleVolumeIntelligenceTests {

    private val benchPress = Exercise(
        id = "ex_bench",
        name = "Barbell Bench Press",
        primaryMuscle = MuscleGroup.CHEST,
        secondaryMuscles = listOf(MuscleGroup.ARMS, MuscleGroup.SHOULDERS),
        movementType = MovementType.COMPOUND,
        equipment = Equipment.BARBELL
    )

    private val barbellRow = Exercise(
        id = "ex_row",
        name = "Barbell Row",
        primaryMuscle = MuscleGroup.BACK,
        secondaryMuscles = listOf(MuscleGroup.ARMS),
        movementType = MovementType.COMPOUND,
        equipment = Equipment.BARBELL
    )

    private val squat = Exercise(
        id = "ex_squat",
        name = "Barbell Squat",
        primaryMuscle = MuscleGroup.LEGS,
        movementType = MovementType.COMPOUND,
        equipment = Equipment.BARBELL
    )

    @Test
    fun testVolumeLandmarksDefaultValues() {
        val chestLandmark = DefaultVolumeLandmarks.forMuscle(MuscleGroup.CHEST)
        assertEquals(8, chestLandmark.mev)
        assertEquals(12, chestLandmark.mavMin)
        assertEquals(20, chestLandmark.mavMax)
        assertEquals(22, chestLandmark.mrv)

        val backLandmark = DefaultVolumeLandmarks.forMuscle(MuscleGroup.BACK)
        assertEquals(10, backLandmark.mev)
        assertEquals(25, backLandmark.mrv)
    }

    @Test
    fun testStatusTransitions() {
        val chestLandmark = DefaultVolumeLandmarks.forMuscle(MuscleGroup.CHEST)

        assertEquals(MuscleVolumeStatus.RECOVERING, chestLandmark.evaluateStatus(0))
        assertEquals(MuscleVolumeStatus.MAINTENANCE, chestLandmark.evaluateStatus(1))
        assertEquals(MuscleVolumeStatus.MAINTENANCE, chestLandmark.evaluateStatus(7))
        assertEquals(MuscleVolumeStatus.OPTIMAL, chestLandmark.evaluateStatus(8))   // MEV
        assertEquals(MuscleVolumeStatus.OPTIMAL, chestLandmark.evaluateStatus(16))  // In MAV
        assertEquals(MuscleVolumeStatus.OPTIMAL, chestLandmark.evaluateStatus(20))  // MAV Max
        assertEquals(MuscleVolumeStatus.OVERREACHING, chestLandmark.evaluateStatus(21))
        assertEquals(MuscleVolumeStatus.OVERREACHING, chestLandmark.evaluateStatus(22)) // MRV
        assertEquals(MuscleVolumeStatus.OVERTRAINED, chestLandmark.evaluateStatus(23))  // Exceeding MRV
    }

    @Test
    fun testWeeklyVolumeCalculationFromWorkouts() {
        val now = 1_000_000_000_000L

        // Workout 1: 3 days ago (Chest 4 working sets + 1 warmup, Squat 3 working sets)
        val workout1 = Workout(
            id = "w1",
            title = "Push Day",
            startTimeEpochMs = now - (3 * 86_400_000L),
            endTimeEpochMs = now - (3 * 86_400_000L) + 3600_000L,
            exercises = listOf(
                WorkoutExercise(
                    id = "we1",
                    exercise = benchPress,
                    sets = listOf(
                        SetLog("s0", 1, setType = SetType.WARMUP, weightKg = 40.0, reps = 10, isCompleted = true),
                        SetLog("s1", 2, setType = SetType.NORMAL, weightKg = 80.0, reps = 10, isCompleted = true),
                        SetLog("s2", 3, setType = SetType.NORMAL, weightKg = 80.0, reps = 10, isCompleted = true),
                        SetLog("s3", 4, setType = SetType.NORMAL, weightKg = 80.0, reps = 8, isCompleted = true),
                        SetLog("s4", 5, setType = SetType.NORMAL, weightKg = 80.0, reps = 8, isCompleted = true)
                    )
                ),
                WorkoutExercise(
                    id = "we2",
                    exercise = squat,
                    sets = listOf(
                        SetLog("s5", 1, setType = SetType.NORMAL, weightKg = 100.0, reps = 6, isCompleted = true),
                        SetLog("s6", 2, setType = SetType.NORMAL, weightKg = 100.0, reps = 6, isCompleted = true),
                        SetLog("s7", 3, setType = SetType.NORMAL, weightKg = 100.0, reps = 6, isCompleted = true)
                    )
                )
            )
        )

        // Workout 2: 1 day ago (Chest 4 working sets, Back 5 working sets)
        val workout2 = Workout(
            id = "w2",
            title = "Upper Day",
            startTimeEpochMs = now - (1 * 86_400_000L),
            endTimeEpochMs = now - (1 * 86_400_000L) + 3600_000L,
            exercises = listOf(
                WorkoutExercise(
                    id = "we3",
                    exercise = benchPress,
                    sets = listOf(
                        SetLog("s8", 1, setType = SetType.NORMAL, weightKg = 82.5, reps = 8, isCompleted = true),
                        SetLog("s9", 2, setType = SetType.NORMAL, weightKg = 82.5, reps = 8, isCompleted = true),
                        SetLog("s10", 3, setType = SetType.NORMAL, weightKg = 82.5, reps = 8, isCompleted = true),
                        SetLog("s11", 4, setType = SetType.NORMAL, weightKg = 82.5, reps = 8, isCompleted = true)
                    )
                ),
                WorkoutExercise(
                    id = "we4",
                    exercise = barbellRow,
                    sets = listOf(
                        SetLog("s12", 1, setType = SetType.NORMAL, weightKg = 70.0, reps = 10, isCompleted = true),
                        SetLog("s13", 2, setType = SetType.NORMAL, weightKg = 70.0, reps = 10, isCompleted = true),
                        SetLog("s14", 3, setType = SetType.NORMAL, weightKg = 70.0, reps = 10, isCompleted = true),
                        SetLog("s15", 4, setType = SetType.NORMAL, weightKg = 70.0, reps = 10, isCompleted = true),
                        SetLog("s16", 5, setType = SetType.NORMAL, weightKg = 70.0, reps = 10, isCompleted = true)
                    )
                )
            )
        )

        // Workout 3: 10 days ago (Old workout, outside 7-day window)
        val workout3 = Workout(
            id = "w3",
            title = "Old Workout",
            startTimeEpochMs = now - (10 * 86_400_000L),
            endTimeEpochMs = now - (10 * 86_400_000L) + 3600_000L,
            exercises = listOf(
                WorkoutExercise(
                    id = "we5",
                    exercise = benchPress,
                    sets = listOf(
                        SetLog("s17", 1, setType = SetType.NORMAL, weightKg = 80.0, reps = 10, isCompleted = true)
                    )
                )
            )
        )

        val stats = MuscleVolumeCalculator.calculateWeeklyVolume(
            completedWorkouts = listOf(workout1, workout2, workout3),
            currentEpochMs = now
        )

        // Chest: 4 sets from w1 + 4 sets from w2 = 8 direct working sets (Warmup excluded, w3 excluded)
        val chestStats = stats[MuscleGroup.CHEST]
        assertNotNull(chestStats)
        assertEquals(8, chestStats.directSets)
        assertEquals(8, chestStats.weeklySets)
        assertEquals(MuscleVolumeStatus.OPTIMAL, chestStats.status) // 8 sets = MEV reached!
        assertEquals("Barbell Bench Press", chestStats.topExercises.first().first)

        // Arms: 2 indirect sets from w1 (4 / 2) + 2 from w2 bench (4 / 2) + 2 from w2 row (5 / 2) = 6 indirect sets
        val armsStats = stats[MuscleGroup.ARMS]
        assertNotNull(armsStats)
        assertEquals(6, armsStats.indirectSets)
        assertEquals(6, armsStats.weeklySets)
        assertEquals(MuscleVolumeStatus.OPTIMAL, armsStats.status) // Arms MEV is 6

        // Legs: 3 sets from squat
        val legsStats = stats[MuscleGroup.LEGS]
        assertNotNull(legsStats)
        assertEquals(3, legsStats.weeklySets)
        assertEquals(MuscleVolumeStatus.MAINTENANCE, legsStats.status) // < MEV (8)

        // Core: 0 sets -> RECOVERING
        val coreStats = stats[MuscleGroup.CORE]
        assertNotNull(coreStats)
        assertEquals(0, coreStats.weeklySets)
        assertEquals(MuscleVolumeStatus.RECOVERING, coreStats.status)
    }
}
