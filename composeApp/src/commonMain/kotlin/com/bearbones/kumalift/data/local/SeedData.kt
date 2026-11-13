package com.bearbones.kumalift.data.local

import com.bearbones.kumalift.domain.model.Equipment
import com.bearbones.kumalift.domain.model.Exercise
import com.bearbones.kumalift.domain.model.MuscleGroup
import com.bearbones.kumalift.domain.model.PersonalRecord
import com.bearbones.kumalift.domain.model.RoutineBlueprint

object SeedData {

    val defaultExercises: List<Exercise> = listOf(
        // ==================== DADA (CHEST) ====================
        Exercise(
            id = "ex_bench_press",
            name = "Barbell Bench Press",
            primaryMuscle = MuscleGroup.CHEST,
            secondaryMuscles = listOf(MuscleGroup.ARMS, MuscleGroup.SHOULDERS),
            equipment = Equipment.BARBELL,
            defaultIncrementKg = 2.5,
            currentPrWeightKg = 118.0,
            currentPrReps = 3,
            lastLoggedDate = "2 hari lalu"
        ),
        Exercise(
            id = "ex_incline_bb_press",
            name = "Incline Barbell Bench Press",
            primaryMuscle = MuscleGroup.CHEST,
            secondaryMuscles = listOf(MuscleGroup.SHOULDERS, MuscleGroup.ARMS),
            equipment = Equipment.BARBELL,
            defaultIncrementKg = 2.5,
            currentPrWeightKg = 90.0,
            currentPrReps = 6
        ),
        Exercise(
            id = "ex_decline_bb_press",
            name = "Decline Barbell Bench Press",
            primaryMuscle = MuscleGroup.CHEST,
            secondaryMuscles = listOf(MuscleGroup.ARMS),
            equipment = Equipment.BARBELL,
            defaultIncrementKg = 2.5
        ),
        Exercise(
            id = "ex_flat_db_press",
            name = "Flat Dumbbell Press",
            primaryMuscle = MuscleGroup.CHEST,
            secondaryMuscles = listOf(MuscleGroup.ARMS, MuscleGroup.SHOULDERS),
            equipment = Equipment.DUMBBELL,
            defaultIncrementKg = 2.0,
            currentPrWeightKg = 42.0,
            currentPrReps = 6
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
            lastLoggedDate = "2 hari lalu"
        ),
        Exercise(
            id = "ex_decline_db_press",
            name = "Decline Dumbbell Press",
            primaryMuscle = MuscleGroup.CHEST,
            secondaryMuscles = listOf(MuscleGroup.ARMS),
            equipment = Equipment.DUMBBELL,
            defaultIncrementKg = 2.0
        ),
        Exercise(
            id = "ex_db_fly",
            name = "Dumbbell Chest Flyes",
            primaryMuscle = MuscleGroup.CHEST,
            secondaryMuscles = emptyList(),
            equipment = Equipment.DUMBBELL,
            defaultIncrementKg = 1.0
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
            lastLoggedDate = "5 hari lalu"
        ),
        Exercise(
            id = "ex_incline_cable_fly",
            name = "Incline Cable Flyes (Low-to-High)",
            primaryMuscle = MuscleGroup.CHEST,
            secondaryMuscles = listOf(MuscleGroup.SHOULDERS),
            equipment = Equipment.CABLE,
            defaultIncrementKg = 1.25
        ),
        Exercise(
            id = "ex_high_cable_fly",
            name = "High-to-Low Cable Flyes",
            primaryMuscle = MuscleGroup.CHEST,
            secondaryMuscles = emptyList(),
            equipment = Equipment.CABLE,
            defaultIncrementKg = 1.25
        ),
        Exercise(
            id = "ex_machine_chest_press",
            name = "Machine Chest Press",
            primaryMuscle = MuscleGroup.CHEST,
            secondaryMuscles = listOf(MuscleGroup.ARMS, MuscleGroup.SHOULDERS),
            equipment = Equipment.MACHINE,
            defaultIncrementKg = 5.0
        ),
        Exercise(
            id = "ex_pec_deck_fly",
            name = "Pec Deck Machine Fly",
            primaryMuscle = MuscleGroup.CHEST,
            secondaryMuscles = emptyList(),
            equipment = Equipment.MACHINE,
            defaultIncrementKg = 2.5
        ),
        Exercise(
            id = "ex_dips_chest",
            name = "Chest Dips",
            primaryMuscle = MuscleGroup.CHEST,
            secondaryMuscles = listOf(MuscleGroup.ARMS, MuscleGroup.SHOULDERS),
            equipment = Equipment.BODYWEIGHT,
            defaultIncrementKg = 2.5,
            currentPrWeightKg = 20.0,
            currentPrReps = 8
        ),
        Exercise(
            id = "ex_push_up",
            name = "Push-Up (Standar)",
            primaryMuscle = MuscleGroup.CHEST,
            secondaryMuscles = listOf(MuscleGroup.ARMS, MuscleGroup.CORE),
            equipment = Equipment.BODYWEIGHT,
            defaultIncrementKg = 0.0
        ),
        Exercise(
            id = "ex_diamond_push_up",
            name = "Diamond Push-Up",
            primaryMuscle = MuscleGroup.CHEST,
            secondaryMuscles = listOf(MuscleGroup.ARMS),
            equipment = Equipment.BODYWEIGHT,
            defaultIncrementKg = 0.0
        ),

        // ==================== PUNGGUNG (BACK) ====================
        Exercise(
            id = "ex_deadlift",
            name = "Conventional Deadlift",
            primaryMuscle = MuscleGroup.BACK,
            secondaryMuscles = listOf(MuscleGroup.LEGS, MuscleGroup.CORE),
            equipment = Equipment.BARBELL,
            defaultIncrementKg = 5.0,
            currentPrWeightKg = 185.0,
            currentPrReps = 3,
            lastLoggedDate = "1 minggu lalu"
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
            lastLoggedDate = "Kemarin"
        ),
        Exercise(
            id = "ex_pendlay_row",
            name = "Pendlay Row",
            primaryMuscle = MuscleGroup.BACK,
            secondaryMuscles = listOf(MuscleGroup.ARMS, MuscleGroup.CORE),
            equipment = Equipment.BARBELL,
            defaultIncrementKg = 2.5
        ),
        Exercise(
            id = "ex_tbar_row",
            name = "T-Bar Row",
            primaryMuscle = MuscleGroup.BACK,
            secondaryMuscles = listOf(MuscleGroup.ARMS),
            equipment = Equipment.BARBELL,
            defaultIncrementKg = 2.5,
            currentPrWeightKg = 70.0,
            currentPrReps = 8
        ),
        Exercise(
            id = "ex_one_arm_db_row",
            name = "Single-Arm Dumbbell Row",
            primaryMuscle = MuscleGroup.BACK,
            secondaryMuscles = listOf(MuscleGroup.ARMS),
            equipment = Equipment.DUMBBELL,
            defaultIncrementKg = 2.0,
            currentPrWeightKg = 40.0,
            currentPrReps = 8
        ),
        Exercise(
            id = "ex_chest_supported_db_row",
            name = "Chest-Supported Dumbbell Row",
            primaryMuscle = MuscleGroup.BACK,
            secondaryMuscles = listOf(MuscleGroup.ARMS),
            equipment = Equipment.DUMBBELL,
            defaultIncrementKg = 2.0
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
            lastLoggedDate = "Kemarin"
        ),
        Exercise(
            id = "ex_close_grip_lat_pulldown",
            name = "Lat Pulldown (Close Grip / V-Bar)",
            primaryMuscle = MuscleGroup.BACK,
            secondaryMuscles = listOf(MuscleGroup.ARMS),
            equipment = Equipment.CABLE,
            defaultIncrementKg = 2.5
        ),
        Exercise(
            id = "ex_seated_cable_row",
            name = "Seated Cable Row",
            primaryMuscle = MuscleGroup.BACK,
            secondaryMuscles = listOf(MuscleGroup.ARMS),
            equipment = Equipment.CABLE,
            defaultIncrementKg = 2.5,
            currentPrWeightKg = 75.0,
            currentPrReps = 10
        ),
        Exercise(
            id = "ex_straight_arm_pulldown",
            name = "Straight-Arm Cable Pulldown",
            primaryMuscle = MuscleGroup.BACK,
            secondaryMuscles = emptyList(),
            equipment = Equipment.CABLE,
            defaultIncrementKg = 1.25
        ),
        Exercise(
            id = "ex_pull_up",
            name = "Pull-Up (Wide Overhand)",
            primaryMuscle = MuscleGroup.BACK,
            secondaryMuscles = listOf(MuscleGroup.ARMS),
            equipment = Equipment.BODYWEIGHT,
            defaultIncrementKg = 2.5,
            currentPrWeightKg = 15.0,
            currentPrReps = 6
        ),
        Exercise(
            id = "ex_chin_up",
            name = "Chin-Up (Underhand)",
            primaryMuscle = MuscleGroup.BACK,
            secondaryMuscles = listOf(MuscleGroup.ARMS),
            equipment = Equipment.BODYWEIGHT,
            defaultIncrementKg = 2.5
        ),
        Exercise(
            id = "ex_seated_machine_row",
            name = "Seated Machine Row",
            primaryMuscle = MuscleGroup.BACK,
            secondaryMuscles = listOf(MuscleGroup.ARMS),
            equipment = Equipment.MACHINE,
            defaultIncrementKg = 5.0
        ),
        Exercise(
            id = "ex_back_extension",
            name = "Back Extension / Hyperextension",
            primaryMuscle = MuscleGroup.BACK,
            secondaryMuscles = listOf(MuscleGroup.LEGS),
            equipment = Equipment.BODYWEIGHT,
            defaultIncrementKg = 2.5
        ),
        Exercise(
            id = "ex_face_pull",
            name = "Cable Face Pull",
            primaryMuscle = MuscleGroup.BACK,
            secondaryMuscles = listOf(MuscleGroup.SHOULDERS),
            equipment = Equipment.CABLE,
            defaultIncrementKg = 1.25,
            currentPrWeightKg = 35.0,
            currentPrReps = 15
        ),

        // ==================== KAKI (LEGS) ====================
        Exercise(
            id = "ex_squat",
            name = "Barbell Back Squat",
            primaryMuscle = MuscleGroup.LEGS,
            secondaryMuscles = listOf(MuscleGroup.CORE),
            equipment = Equipment.BARBELL,
            defaultIncrementKg = 5.0,
            currentPrWeightKg = 150.0,
            currentPrReps = 5,
            lastLoggedDate = "4 hari lalu"
        ),
        Exercise(
            id = "ex_front_squat",
            name = "Barbell Front Squat",
            primaryMuscle = MuscleGroup.LEGS,
            secondaryMuscles = listOf(MuscleGroup.CORE),
            equipment = Equipment.BARBELL,
            defaultIncrementKg = 2.5,
            currentPrWeightKg = 110.0,
            currentPrReps = 5
        ),
        Exercise(
            id = "ex_romanian_deadlift",
            name = "Romanian Deadlift (RDL)",
            primaryMuscle = MuscleGroup.LEGS,
            secondaryMuscles = listOf(MuscleGroup.BACK),
            equipment = Equipment.BARBELL,
            defaultIncrementKg = 5.0,
            currentPrWeightKg = 130.0,
            currentPrReps = 8,
            lastLoggedDate = "4 hari lalu"
        ),
        Exercise(
            id = "ex_db_rdl",
            name = "Dumbbell Romanian Deadlift",
            primaryMuscle = MuscleGroup.LEGS,
            secondaryMuscles = listOf(MuscleGroup.BACK),
            equipment = Equipment.DUMBBELL,
            defaultIncrementKg = 2.0
        ),
        Exercise(
            id = "ex_leg_press",
            name = "Leg Press 45 Derajat",
            primaryMuscle = MuscleGroup.LEGS,
            secondaryMuscles = emptyList(),
            equipment = Equipment.MACHINE,
            defaultIncrementKg = 10.0,
            currentPrWeightKg = 260.0,
            currentPrReps = 10
        ),
        Exercise(
            id = "ex_hack_squat",
            name = "Hack Squat Machine",
            primaryMuscle = MuscleGroup.LEGS,
            secondaryMuscles = emptyList(),
            equipment = Equipment.MACHINE,
            defaultIncrementKg = 5.0,
            currentPrWeightKg = 140.0,
            currentPrReps = 8
        ),
        Exercise(
            id = "ex_walking_lunges",
            name = "Walking Dumbbell Lunges",
            primaryMuscle = MuscleGroup.LEGS,
            secondaryMuscles = listOf(MuscleGroup.CORE),
            equipment = Equipment.DUMBBELL,
            defaultIncrementKg = 2.0
        ),
        Exercise(
            id = "ex_bulgarian_split_squat",
            name = "Bulgarian Split Squat",
            primaryMuscle = MuscleGroup.LEGS,
            secondaryMuscles = listOf(MuscleGroup.CORE),
            equipment = Equipment.DUMBBELL,
            defaultIncrementKg = 2.0,
            currentPrWeightKg = 26.0,
            currentPrReps = 10
        ),
        Exercise(
            id = "ex_goblet_squat",
            name = "Goblet Squat",
            primaryMuscle = MuscleGroup.LEGS,
            secondaryMuscles = listOf(MuscleGroup.CORE),
            equipment = Equipment.DUMBBELL,
            defaultIncrementKg = 2.0
        ),
        Exercise(
            id = "ex_leg_extension",
            name = "Leg Extension Machine",
            primaryMuscle = MuscleGroup.LEGS,
            secondaryMuscles = emptyList(),
            equipment = Equipment.MACHINE,
            defaultIncrementKg = 2.5,
            currentPrWeightKg = 85.0,
            currentPrReps = 12
        ),
        Exercise(
            id = "ex_lying_leg_curl",
            name = "Lying Leg Curl (Hamstring)",
            primaryMuscle = MuscleGroup.LEGS,
            secondaryMuscles = emptyList(),
            equipment = Equipment.MACHINE,
            defaultIncrementKg = 2.5,
            currentPrWeightKg = 65.0,
            currentPrReps = 10
        ),
        Exercise(
            id = "ex_seated_leg_curl",
            name = "Seated Leg Curl",
            primaryMuscle = MuscleGroup.LEGS,
            secondaryMuscles = emptyList(),
            equipment = Equipment.MACHINE,
            defaultIncrementKg = 2.5
        ),
        Exercise(
            id = "ex_standing_calf_raise",
            name = "Standing Calf Raise",
            primaryMuscle = MuscleGroup.LEGS,
            secondaryMuscles = emptyList(),
            equipment = Equipment.MACHINE,
            defaultIncrementKg = 5.0
        ),
        Exercise(
            id = "ex_seated_calf_raise",
            name = "Seated Calf Raise",
            primaryMuscle = MuscleGroup.LEGS,
            secondaryMuscles = emptyList(),
            equipment = Equipment.MACHINE,
            defaultIncrementKg = 2.5
        ),
        Exercise(
            id = "ex_barbell_hip_thrust",
            name = "Barbell Hip Thrust",
            primaryMuscle = MuscleGroup.LEGS,
            secondaryMuscles = emptyList(),
            equipment = Equipment.BARBELL,
            defaultIncrementKg = 5.0,
            currentPrWeightKg = 160.0,
            currentPrReps = 8
        ),

        // ==================== BAHU (SHOULDERS) ====================
        Exercise(
            id = "ex_ohp",
            name = "Overhead Barbell Press",
            primaryMuscle = MuscleGroup.SHOULDERS,
            secondaryMuscles = listOf(MuscleGroup.ARMS, MuscleGroup.CORE),
            equipment = Equipment.BARBELL,
            defaultIncrementKg = 2.5,
            currentPrWeightKg = 72.5,
            currentPrReps = 5,
            lastLoggedDate = "3 hari lalu"
        ),
        Exercise(
            id = "ex_db_shoulder_press",
            name = "Seated Dumbbell Shoulder Press",
            primaryMuscle = MuscleGroup.SHOULDERS,
            secondaryMuscles = listOf(MuscleGroup.ARMS),
            equipment = Equipment.DUMBBELL,
            defaultIncrementKg = 2.0,
            currentPrWeightKg = 32.0,
            currentPrReps = 8
        ),
        Exercise(
            id = "ex_arnold_press",
            name = "Arnold Press",
            primaryMuscle = MuscleGroup.SHOULDERS,
            secondaryMuscles = listOf(MuscleGroup.ARMS),
            equipment = Equipment.DUMBBELL,
            defaultIncrementKg = 2.0
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
            lastLoggedDate = "3 hari lalu"
        ),
        Exercise(
            id = "ex_cable_lateral_raise",
            name = "Cable Lateral Raise",
            primaryMuscle = MuscleGroup.SHOULDERS,
            secondaryMuscles = emptyList(),
            equipment = Equipment.CABLE,
            defaultIncrementKg = 1.25
        ),
        Exercise(
            id = "ex_machine_lateral_raise",
            name = "Machine Lateral Raise",
            primaryMuscle = MuscleGroup.SHOULDERS,
            secondaryMuscles = emptyList(),
            equipment = Equipment.MACHINE,
            defaultIncrementKg = 2.5
        ),
        Exercise(
            id = "ex_barbell_upright_row",
            name = "Barbell Upright Row",
            primaryMuscle = MuscleGroup.SHOULDERS,
            secondaryMuscles = listOf(MuscleGroup.ARMS),
            equipment = Equipment.BARBELL,
            defaultIncrementKg = 2.5
        ),
        Exercise(
            id = "ex_db_rear_delt_fly",
            name = "Dumbbell Rear Delt Fly",
            primaryMuscle = MuscleGroup.SHOULDERS,
            secondaryMuscles = listOf(MuscleGroup.BACK),
            equipment = Equipment.DUMBBELL,
            defaultIncrementKg = 1.0
        ),
        Exercise(
            id = "ex_reverse_pec_deck",
            name = "Reverse Pec Deck (Rear Delts)",
            primaryMuscle = MuscleGroup.SHOULDERS,
            secondaryMuscles = listOf(MuscleGroup.BACK),
            equipment = Equipment.MACHINE,
            defaultIncrementKg = 2.5,
            currentPrWeightKg = 45.0,
            currentPrReps = 12
        ),
        Exercise(
            id = "ex_db_front_raise",
            name = "Dumbbell Front Raise",
            primaryMuscle = MuscleGroup.SHOULDERS,
            secondaryMuscles = emptyList(),
            equipment = Equipment.DUMBBELL,
            defaultIncrementKg = 1.0
        ),

        // ==================== LENGAN (ARMS) ====================
        Exercise(
            id = "ex_barbell_curl",
            name = "Barbell Bicep Curl",
            primaryMuscle = MuscleGroup.ARMS,
            secondaryMuscles = emptyList(),
            equipment = Equipment.BARBELL,
            defaultIncrementKg = 2.5,
            currentPrWeightKg = 45.0,
            currentPrReps = 8
        ),
        Exercise(
            id = "ex_ez_preacher_curl",
            name = "EZ-Bar Preacher Curl",
            primaryMuscle = MuscleGroup.ARMS,
            secondaryMuscles = emptyList(),
            equipment = Equipment.BARBELL,
            defaultIncrementKg = 2.5
        ),
        Exercise(
            id = "ex_db_bicep_curl",
            name = "Standing Dumbbell Bicep Curl",
            primaryMuscle = MuscleGroup.ARMS,
            secondaryMuscles = emptyList(),
            equipment = Equipment.DUMBBELL,
            defaultIncrementKg = 2.0,
            currentPrWeightKg = 20.0,
            currentPrReps = 10
        ),
        Exercise(
            id = "ex_incline_db_curl",
            name = "Incline Dumbbell Curl",
            primaryMuscle = MuscleGroup.ARMS,
            secondaryMuscles = emptyList(),
            equipment = Equipment.DUMBBELL,
            defaultIncrementKg = 1.0
        ),
        Exercise(
            id = "ex_hammer_curl",
            name = "Dumbbell Hammer Curl",
            primaryMuscle = MuscleGroup.ARMS,
            secondaryMuscles = emptyList(),
            equipment = Equipment.DUMBBELL,
            defaultIncrementKg = 2.0,
            currentPrWeightKg = 22.0,
            currentPrReps = 10
        ),
        Exercise(
            id = "ex_cable_bicep_curl",
            name = "Cable Bicep Curl",
            primaryMuscle = MuscleGroup.ARMS,
            secondaryMuscles = emptyList(),
            equipment = Equipment.CABLE,
            defaultIncrementKg = 1.25
        ),
        Exercise(
            id = "ex_concentration_curl",
            name = "Concentration Curl",
            primaryMuscle = MuscleGroup.ARMS,
            secondaryMuscles = emptyList(),
            equipment = Equipment.DUMBBELL,
            defaultIncrementKg = 1.0
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
            lastLoggedDate = "2 hari lalu"
        ),
        Exercise(
            id = "ex_straight_bar_pushdown",
            name = "Straight Bar Tricep Pushdown",
            primaryMuscle = MuscleGroup.ARMS,
            secondaryMuscles = emptyList(),
            equipment = Equipment.CABLE,
            defaultIncrementKg = 2.5
        ),
        Exercise(
            id = "ex_skull_crusher",
            name = "Skull Crusher (Lying Tricep Ext)",
            primaryMuscle = MuscleGroup.ARMS,
            secondaryMuscles = emptyList(),
            equipment = Equipment.BARBELL,
            defaultIncrementKg = 2.5,
            currentPrWeightKg = 40.0,
            currentPrReps = 8
        ),
        Exercise(
            id = "ex_overhead_cable_tricep",
            name = "Overhead Cable Tricep Extension",
            primaryMuscle = MuscleGroup.ARMS,
            secondaryMuscles = emptyList(),
            equipment = Equipment.CABLE,
            defaultIncrementKg = 1.25
        ),
        Exercise(
            id = "ex_db_overhead_tricep",
            name = "Single Dumbbell Overhead Extension",
            primaryMuscle = MuscleGroup.ARMS,
            secondaryMuscles = emptyList(),
            equipment = Equipment.DUMBBELL,
            defaultIncrementKg = 2.0
        ),
        Exercise(
            id = "ex_close_grip_bench",
            name = "Close-Grip Barbell Bench Press",
            primaryMuscle = MuscleGroup.ARMS,
            secondaryMuscles = listOf(MuscleGroup.CHEST),
            equipment = Equipment.BARBELL,
            defaultIncrementKg = 2.5
        ),
        Exercise(
            id = "ex_bench_dips",
            name = "Bench Dips",
            primaryMuscle = MuscleGroup.ARMS,
            secondaryMuscles = emptyList(),
            equipment = Equipment.BODYWEIGHT,
            defaultIncrementKg = 0.0
        ),

        // ==================== CORE / PERUT ====================
        Exercise(
            id = "ex_hanging_knee_raise",
            name = "Hanging Knee Raise",
            primaryMuscle = MuscleGroup.CORE,
            secondaryMuscles = emptyList(),
            equipment = Equipment.BODYWEIGHT,
            defaultIncrementKg = 0.0,
            currentPrWeightKg = 0.0,
            currentPrReps = 20,
            lastLoggedDate = "Kemarin"
        ),
        Exercise(
            id = "ex_hanging_leg_raise",
            name = "Hanging Leg Raise (Toes-to-Bar)",
            primaryMuscle = MuscleGroup.CORE,
            secondaryMuscles = emptyList(),
            equipment = Equipment.BODYWEIGHT,
            defaultIncrementKg = 0.0
        ),
        Exercise(
            id = "ex_cable_woodchopper",
            name = "Cable Woodchopper",
            primaryMuscle = MuscleGroup.CORE,
            secondaryMuscles = emptyList(),
            equipment = Equipment.CABLE,
            defaultIncrementKg = 1.25
        ),
        Exercise(
            id = "ex_kneeling_cable_crunch",
            name = "Kneeling Cable Crunch",
            primaryMuscle = MuscleGroup.CORE,
            secondaryMuscles = emptyList(),
            equipment = Equipment.CABLE,
            defaultIncrementKg = 2.5,
            currentPrWeightKg = 45.0,
            currentPrReps = 15
        ),
        Exercise(
            id = "ex_ab_wheel_rollout",
            name = "Ab Wheel Rollout",
            primaryMuscle = MuscleGroup.CORE,
            secondaryMuscles = emptyList(),
            equipment = Equipment.BODYWEIGHT,
            defaultIncrementKg = 0.0
        ),
        Exercise(
            id = "ex_plank",
            name = "Plank Standar",
            primaryMuscle = MuscleGroup.CORE,
            secondaryMuscles = emptyList(),
            equipment = Equipment.BODYWEIGHT,
            defaultIncrementKg = 0.0
        ),
        Exercise(
            id = "ex_russian_twist",
            name = "Russian Twist",
            primaryMuscle = MuscleGroup.CORE,
            secondaryMuscles = emptyList(),
            equipment = Equipment.BODYWEIGHT,
            defaultIncrementKg = 1.0
        ),
        Exercise(
            id = "ex_decline_crunch",
            name = "Decline Sit-Up / Crunch",
            primaryMuscle = MuscleGroup.CORE,
            secondaryMuscles = emptyList(),
            equipment = Equipment.BODYWEIGHT,
            defaultIncrementKg = 0.0
        ),

        // ==================== SELURUH TUBUH (FULL BODY) ====================
        Exercise(
            id = "ex_trap_bar_deadlift",
            name = "Trap Bar Deadlift",
            primaryMuscle = MuscleGroup.FULL_BODY,
            secondaryMuscles = listOf(MuscleGroup.LEGS, MuscleGroup.BACK),
            equipment = Equipment.BARBELL,
            defaultIncrementKg = 5.0,
            currentPrWeightKg = 190.0,
            currentPrReps = 3
        ),
        Exercise(
            id = "ex_kb_swing",
            name = "Kettlebell Swing",
            primaryMuscle = MuscleGroup.FULL_BODY,
            secondaryMuscles = listOf(MuscleGroup.LEGS, MuscleGroup.BACK),
            equipment = Equipment.DUMBBELL,
            defaultIncrementKg = 2.0
        ),
        Exercise(
            id = "ex_clean_and_press",
            name = "Barbell Clean and Press",
            primaryMuscle = MuscleGroup.FULL_BODY,
            secondaryMuscles = listOf(MuscleGroup.SHOULDERS, MuscleGroup.LEGS),
            equipment = Equipment.BARBELL,
            defaultIncrementKg = 2.5
        ),
        Exercise(
            id = "ex_farmers_walk",
            name = "Farmer's Walk",
            primaryMuscle = MuscleGroup.FULL_BODY,
            secondaryMuscles = listOf(MuscleGroup.CORE, MuscleGroup.ARMS),
            equipment = Equipment.DUMBBELL,
            defaultIncrementKg = 2.0
        )
    )

    private fun ex(id: String): Exercise = defaultExercises.first { it.id == id }

    val defaultRoutines: List<RoutineBlueprint> = listOf(
        RoutineBlueprint(
            id = "rt_pull_day_a",
            title = "Pull Day A",
            splitCategory = "PPL Split",
            muscleFocus = "Back · Biceps · Posterior Delts",
            estimatedDurationMinutes = 55,
            targetRpeTier = "Heavy, RPE 8–9",
            exerciseTemplates = listOf(
                ex("ex_deadlift"),
                ex("ex_barbell_row"),
                ex("ex_lat_pulldown")
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
                ex("ex_bench_press"),
                ex("ex_incline_db_press"),
                ex("ex_cable_fly"),
                ex("ex_tricep_pushdown")
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
                ex("ex_squat"),
                ex("ex_romanian_deadlift")
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
                ex("ex_bench_press"),
                ex("ex_ohp"),
                ex("ex_barbell_row")
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
