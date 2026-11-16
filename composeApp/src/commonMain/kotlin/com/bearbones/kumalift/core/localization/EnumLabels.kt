package com.bearbones.kumalift.core.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.bearbones.kumalift.domain.model.BackupFrequency
import com.bearbones.kumalift.domain.model.Equipment
import com.bearbones.kumalift.domain.model.Exercise
import com.bearbones.kumalift.domain.model.ExperienceLevel
import com.bearbones.kumalift.domain.model.MovementType
import com.bearbones.kumalift.domain.model.MuscleGroup
import com.bearbones.kumalift.domain.model.SetType
import com.bearbones.kumalift.domain.model.TrainingGoal

fun TrainingGoal.label(s: AppStrings): String = when (this) {
    TrainingGoal.HYPERTROPHY -> s.programHypertrophy
    TrainingGoal.STRENGTH -> s.programStrength
    TrainingGoal.ENDURANCE -> s.programEndurance
    TrainingGoal.FAT_LOSS -> s.programFatLoss
    TrainingGoal.GENERAL -> s.programGeneral
}

val TrainingGoal.label: String
    @Composable
    @ReadOnlyComposable
    get() = label(LocalStrings.current)

fun MuscleGroup.label(s: AppStrings): String = when (this) {
    MuscleGroup.CHEST -> s.muscleChest
    MuscleGroup.BACK -> s.muscleBack
    MuscleGroup.LEGS -> s.muscleLegs
    MuscleGroup.SHOULDERS -> s.muscleShoulders
    MuscleGroup.ARMS -> s.muscleArms
    MuscleGroup.CORE -> s.muscleCore
    MuscleGroup.CARDIO -> s.muscleCardio
    MuscleGroup.NECK -> s.muscleNeck
    MuscleGroup.FULL_BODY -> s.muscleFullBody
}

val MuscleGroup.label: String
    @Composable
    @ReadOnlyComposable
    get() = label(LocalStrings.current)

fun Equipment.label(s: AppStrings): String = when (this) {
    Equipment.BARBELL -> s.equipBarbell
    Equipment.DUMBBELL -> s.equipDumbbell
    Equipment.CABLE -> s.equipCable
    Equipment.BODYWEIGHT -> s.equipBodyweight
    Equipment.MACHINE -> s.equipMachine
    Equipment.SMITH_MACHINE -> s.equipSmithMachine
    Equipment.BAND -> s.equipBand
    Equipment.KETTLEBELL -> s.equipKettlebell
    Equipment.OTHER -> s.equipOther
}

val Equipment.label: String
    @Composable
    @ReadOnlyComposable
    get() = label(LocalStrings.current)

fun MovementType.label(s: AppStrings): String = when (this) {
    MovementType.COMPOUND -> s.movementCompound
    MovementType.ISOLATION -> s.movementIsolation
}

val MovementType.label: String
    @Composable
    @ReadOnlyComposable
    get() = label(LocalStrings.current)

fun ExperienceLevel.label(s: AppStrings): String = when (this) {
    ExperienceLevel.BEGINNER -> s.levelBeginner
    ExperienceLevel.INTERMEDIATE -> s.levelIntermediate
    ExperienceLevel.ADVANCED -> s.levelAdvanced
}

val ExperienceLevel.label: String
    @Composable
    @ReadOnlyComposable
    get() = label(LocalStrings.current)

fun Exercise.localizedMuscleSubtitle(s: AppStrings): String {
    val primary = primaryMuscle.label(s)
    if (subMuscle.isBlank()) return primary
    val sub = s.subMuscleLabel(subMuscle)
    return if (!sub.equals(primary, ignoreCase = true)) "$primary · $sub" else primary
}

val Exercise.localizedMuscleSubtitle: String
    @Composable
    @ReadOnlyComposable
    get() = localizedMuscleSubtitle(LocalStrings.current)

fun SetType.label(s: AppStrings): String = when (this) {
    SetType.WARMUP -> s.setTypeWarmup
    SetType.NORMAL -> s.setTypeNormal
    SetType.DROP -> s.setTypeDrop
    SetType.FAILURE -> s.setTypeFailure
}

val SetType.label: String
    @Composable
    @ReadOnlyComposable
    get() = label(LocalStrings.current)

fun BackupFrequency.label(s: AppStrings): String = when (this) {
    BackupFrequency.DAILY -> s.backupFrequencyDaily
    BackupFrequency.WEEKLY -> s.backupFrequencyWeekly
    BackupFrequency.MONTHLY -> s.backupFrequencyMonthly
}

val BackupFrequency.label: String
    @Composable
    @ReadOnlyComposable
    get() = label(LocalStrings.current)
