package com.bearbones.kumalift.domain.share

import com.bearbones.kumalift.core.localization.AppStrings
import com.bearbones.kumalift.core.localization.label
import com.bearbones.kumalift.core.util.NumberFormatUtils
import com.bearbones.kumalift.core.util.UnitConverters
import com.bearbones.kumalift.core.util.currentEpochMillis
import com.bearbones.kumalift.domain.model.AppLanguage
import com.bearbones.kumalift.domain.model.MuscleGroup
import com.bearbones.kumalift.domain.model.RoutineBlueprint
import com.bearbones.kumalift.domain.model.RoutineExerciseConfig
import com.bearbones.kumalift.domain.model.SharedExerciseSpec
import com.bearbones.kumalift.domain.model.SharedPlanType
import com.bearbones.kumalift.domain.model.SharedRoutineSpec
import com.bearbones.kumalift.domain.model.SharedSetSpec
import com.bearbones.kumalift.domain.model.SharedWorkoutPlanPackage
import com.bearbones.kumalift.domain.model.TrainingGoal
import com.bearbones.kumalift.domain.model.WeightUnit
import com.bearbones.kumalift.domain.model.localizedTitle

enum class ProgramShareScope {
    WHOLE_PROGRAM,
    SINGLE_ROUTINE,
    CUSTOM_SELECTION
}

data class ProgramShareExerciseRow(
    val itemKey: String,
    val exerciseId: String,
    val exerciseName: String,
    val thumbnailPath: String?,
    val primaryMuscle: MuscleGroup,
    val muscleLabel: String,
    val equipmentLabel: String,
    val setsRepsLabel: String,
    val targetDetailsLabel: String?
)

data class ProgramShareDayColumn(
    val routineId: String,
    val title: String,
    val exercises: List<ProgramShareExerciseRow>
)

data class ProgramShareRenderData(
    val headerTitle: String,
    val subtitle: String,
    val columns: List<ProgramShareDayColumn>,
    val totalExercisesCount: Int
)

object ProgramShareBuilder {

    fun buildExerciseItemKey(routineId: String, index: Int, exerciseId: String): String =
        "${routineId}::${index}::${exerciseId}"

    fun allExerciseKeysForRoutines(routines: List<RoutineBlueprint>): Set<String> {
        val keys = linkedSetOf<String>()
        routines.forEach { routine ->
            routine.effectiveConfigs.forEachIndexed { idx, cfg ->
                keys.add(buildExerciseItemKey(routine.id, idx, cfg.exercise.id))
            }
        }
        return keys
    }

    fun formatSetsRepsLine(config: RoutineExerciseConfig, s: AppStrings): String {
        val sets = config.effectiveSets
        val setCount = sets.size.coerceAtLeast(1)
        val repsList = sets.map { it.targetReps.coerceAtLeast(1) }
        val minReps = repsList.minOrNull() ?: config.targetReps.coerceAtLeast(1)
        val maxReps = repsList.maxOrNull() ?: minReps
        return if (minReps == maxReps) {
            s.shareRoutineSetsRepsLine(setCount, minReps)
        } else {
            "$setCount ${s.setsAbbrev} × $minReps–$maxReps ${s.repsAbbrev}"
        }
    }

    fun formatOptionalTargetsLine(
        config: RoutineExerciseConfig,
        weightUnit: WeightUnit,
        language: AppLanguage,
        s: AppStrings
    ): String? {
        val unitStr = if (weightUnit == WeightUnit.LB) s.unitLb else s.unitKg
        val sets = config.effectiveSets
        val weights = sets.mapNotNull { it.targetWeightKg?.takeIf { w -> w > 0.0 } }
            .ifEmpty { listOfNotNull(config.targetWeightKg?.takeIf { w -> w > 0.0 }) }
        val weightText = if (weights.isNotEmpty()) {
            val minW = weights.minOrNull() ?: 0.0
            val maxW = weights.maxOrNull() ?: minW
            val minDisp = NumberFormatUtils.formatDecimal(
                UnitConverters.toDisplayWeight(minW, weightUnit),
                language,
                decimals = 1,
                trimTrailingZeros = true
            )
            if ((maxW - minW) > 0.05) {
                val maxDisp = NumberFormatUtils.formatDecimal(
                    UnitConverters.toDisplayWeight(maxW, weightUnit),
                    language,
                    decimals = 1,
                    trimTrailingZeros = true
                )
                "$minDisp–$maxDisp $unitStr"
            } else {
                "$minDisp $unitStr"
            }
        } else {
            null
        }

        val rpes = sets.mapNotNull { it.targetRpe?.takeIf { r -> r > 0.0 } }
        val rpeText = if (rpes.isNotEmpty()) {
            val avgRpe = rpes.average()
            "${s.rpeColumnHeader} ${NumberFormatUtils.formatDecimal(avgRpe, language, decimals = 1, trimTrailingZeros = true)}"
        } else {
            null
        }

        return listOfNotNull(weightText, rpeText).takeIf { it.isNotEmpty() }?.joinToString(" · ")
    }

    fun build(
        programTitle: String,
        routines: List<RoutineBlueprint>,
        scope: ProgramShareScope,
        selectedRoutineId: String?,
        selectedExerciseKeys: Set<String>,
        showTargets: Boolean,
        weightUnit: WeightUnit,
        language: AppLanguage,
        s: AppStrings
    ): ProgramShareRenderData {
        val targetRoutines = when (scope) {
            ProgramShareScope.WHOLE_PROGRAM -> routines
            ProgramShareScope.SINGLE_ROUTINE -> {
                val chosen = routines.firstOrNull { it.id == selectedRoutineId } ?: routines.firstOrNull()
                listOfNotNull(chosen)
            }
            ProgramShareScope.CUSTOM_SELECTION -> routines
        }

        val columns = targetRoutines.mapNotNull { routine ->
            val rows = routine.effectiveConfigs.mapIndexedNotNull { idx, cfg ->
                val itemKey = buildExerciseItemKey(routine.id, idx, cfg.exercise.id)
                val include = when (scope) {
                    ProgramShareScope.CUSTOM_SELECTION -> selectedExerciseKeys.contains(itemKey)
                    else -> true
                }
                if (!include) {
                    null
                } else {
                    ProgramShareExerciseRow(
                        itemKey = itemKey,
                        exerciseId = cfg.exercise.id,
                        exerciseName = cfg.exercise.name,
                        thumbnailPath = cfg.exercise.thumbnailUrl,
                        primaryMuscle = cfg.exercise.primaryMuscle,
                        muscleLabel = cfg.exercise.primaryMuscle.label(s),
                        equipmentLabel = cfg.exercise.equipment.label(s),
                        setsRepsLabel = formatSetsRepsLine(cfg, s),
                        targetDetailsLabel = if (showTargets) {
                            formatOptionalTargetsLine(cfg, weightUnit, language, s)
                        } else {
                            null
                        }
                    )
                }
            }
            if (rows.isEmpty()) {
                null
            } else {
                ProgramShareDayColumn(
                    routineId = routine.id,
                    title = routine.localizedTitle(s),
                    exercises = rows
                )
            }
        }

        val header = when (scope) {
            ProgramShareScope.SINGLE_ROUTINE -> columns.firstOrNull()?.title ?: programTitle
            else -> programTitle
        }
        val totalEx = columns.sumOf { it.exercises.size }
        val subtitle = s.shareExerciseCountSummary(totalEx)

        return ProgramShareRenderData(
            headerTitle = header,
            subtitle = subtitle,
            columns = columns,
            totalExercisesCount = totalEx
        )
    }

    /**
     * Builds a shareable workout plan package reflecting ONLY the currently chosen scope
     * (Whole Program / Single Routine / Custom Exercises) and preserving routine schedules (`scheduledDays`).
     */
    fun buildScopedPlanPackage(
        programTitle: String,
        routines: List<RoutineBlueprint>,
        scope: ProgramShareScope,
        selectedRoutineId: String?,
        selectedExerciseKeys: Set<String>,
        creatorTag: String,
        s: AppStrings
    ): SharedWorkoutPlanPackage {
        val targetRoutines = when (scope) {
            ProgramShareScope.WHOLE_PROGRAM -> routines
            ProgramShareScope.SINGLE_ROUTINE -> {
                val chosen = routines.firstOrNull { it.id == selectedRoutineId } ?: routines.firstOrNull()
                listOfNotNull(chosen)
            }
            ProgramShareScope.CUSTOM_SELECTION -> routines
        }

        val routineSpecs = targetRoutines.mapNotNull { routine ->
            val filteredConfigs = routine.effectiveConfigs.mapIndexedNotNull { idx, cfg ->
                val itemKey = buildExerciseItemKey(routine.id, idx, cfg.exercise.id)
                if (scope != ProgramShareScope.CUSTOM_SELECTION || selectedExerciseKeys.contains(itemKey)) {
                    cfg
                } else {
                    null
                }
            }
            if (filteredConfigs.isEmpty()) {
                null
            } else {
                SharedRoutineSpec(
                    sourceRoutineId = routine.id,
                    title = routine.localizedTitle(s),
                    splitCategory = routine.splitCategory,
                    muscleFocus = routine.muscleFocus,
                    estimatedDurationMinutes = routine.estimatedDurationMinutes,
                    targetRpeTier = routine.targetRpeTier,
                    scheduledDays = routine.scheduledDays,
                    instructions = "",
                    exercises = filteredConfigs.map { cfg ->
                        val setSpecs = cfg.effectiveSets.map { sc ->
                            SharedSetSpec(
                                setNumber = sc.setNumber,
                                targetReps = sc.targetReps,
                                targetWeightKg = sc.targetWeightKg,
                                restTimerSeconds = sc.restTimerSeconds
                            )
                        }
                        SharedExerciseSpec(
                            exerciseId = cfg.exercise.id,
                            name = cfg.exercise.name,
                            primaryMuscle = cfg.exercise.primaryMuscle,
                            equipment = cfg.exercise.equipment,
                            movementType = cfg.exercise.movementType,
                            targetSets = setSpecs.size.coerceAtLeast(1),
                            targetReps = cfg.targetReps,
                            targetWeightKg = cfg.targetWeightKg,
                            restTimerSeconds = cfg.restTimerSeconds,
                            pinnedNote = cfg.exercise.pinnedNote,
                            setConfigs = setSpecs
                        )
                    }
                )
            }
        }

        val activeDays = routineSpecs.flatMap { it.scheduledDays }.distinct().sorted()
        val effectiveTitle = if (scope == ProgramShareScope.SINGLE_ROUTINE) {
            routineSpecs.firstOrNull()?.title ?: programTitle
        } else {
            programTitle
        }
        val cleanHandle = creatorTag.trim().trimStart('@')

        return SharedWorkoutPlanPackage(
            schemaVersion = 1,
            planType = if (routineSpecs.size <= 1 && scope == ProgramShareScope.SINGLE_ROUTINE) {
                SharedPlanType.ROUTINE
            } else {
                SharedPlanType.PROGRAM
            },
            title = effectiveTitle,
            description = "",
            goal = TrainingGoal.HYPERTROPHY,
            targetDaysPerWeek = activeDays.size.coerceAtLeast(routineSpecs.size.coerceAtLeast(1)),
            activeDays = activeDays,
            creatorName = "",
            creatorUsername = cleanHandle,
            exportedAtEpochMs = currentEpochMillis(),
            routines = routineSpecs
        )
    }
}
