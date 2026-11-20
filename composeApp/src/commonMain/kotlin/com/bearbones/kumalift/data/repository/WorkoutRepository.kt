package com.bearbones.kumalift.data.repository

import com.bearbones.kumalift.core.logging.KumaLogger
import com.bearbones.kumalift.core.util.IdGenerator
import com.bearbones.kumalift.core.util.OneRepMaxCalculator
import com.bearbones.kumalift.core.util.currentEpochMillis
import com.bearbones.kumalift.data.local.KumaDatabaseRepository
import com.bearbones.kumalift.data.local.SeedData
import com.bearbones.kumalift.domain.model.CalculationSettings
import com.bearbones.kumalift.domain.model.DateFormatPreference
import com.bearbones.kumalift.domain.model.Equipment
import com.bearbones.kumalift.domain.model.Exercise
import com.bearbones.kumalift.domain.model.LoggingSettings
import com.bearbones.kumalift.domain.model.MovementType
import com.bearbones.kumalift.domain.model.MuscleGroup
import com.bearbones.kumalift.domain.model.PersonalRecord
import com.bearbones.kumalift.domain.model.PrMetricType
import com.bearbones.kumalift.domain.model.PrefillSource
import com.bearbones.kumalift.domain.model.RoutineBlueprint
import com.bearbones.kumalift.domain.model.ScheduleStatus
import com.bearbones.kumalift.domain.model.SetLog
import com.bearbones.kumalift.domain.model.SetType
import com.bearbones.kumalift.domain.model.Workout
import com.bearbones.kumalift.domain.model.WorkoutExercise
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ExerciseRepository(
    initialExercises: List<Exercise> = SeedData.defaultExercises,
    private val databaseRepo: KumaDatabaseRepository? = null,
    private val onSaveCustom: ((List<Exercise>) -> Unit)? = null
) {
    private val _exercises = MutableStateFlow(initialExercises)
    val exercises: StateFlow<List<Exercise>> = _exercises.asStateFlow()

    fun getExerciseById(id: String): Exercise? {
        return _exercises.value.find { it.id == id }
    }

    fun searchExercises(
        query: String = "",
        muscleGroup: MuscleGroup? = null,
        equipment: Equipment? = null
    ): List<Exercise> {
        return _exercises.value.filter { exercise ->
            val matchesQuery = query.isBlank() || exercise.name.contains(query, ignoreCase = true)
            val matchesMuscle = muscleGroup == null || muscleGroup == MuscleGroup.FULL_BODY ||
                    exercise.primaryMuscle == muscleGroup || exercise.secondaryMuscles.contains(muscleGroup)
            val matchesEquipment = equipment == null || exercise.equipment == equipment
            matchesQuery && matchesMuscle && matchesEquipment
        }
    }

    fun addCustomExercise(
        name: String,
        primaryMuscle: MuscleGroup,
        equipment: Equipment,
        secondaryMuscles: List<MuscleGroup> = emptyList(),
        movementType: MovementType = MovementType.COMPOUND
    ): Exercise {
        val cleanId = IdGenerator.customExerciseId(name)
        val newEx = Exercise(
            id = cleanId,
            name = name,
            primaryMuscle = primaryMuscle,
            secondaryMuscles = secondaryMuscles,
            secondaryMuscleDetails = "",
            equipment = equipment,
            equipmentDetail = "",
            movementType = movementType,
            defaultIncrementKg = 2.5
        )
        // Prepend to top of list so the user immediately sees the new exercise in the catalog
        _exercises.update { current ->
            listOf(newEx) + current
        }

        // T5: Side effects run strictly outside StateFlow.update
        val customOnly = _exercises.value.filter { it.id.startsWith("custom_") }
        databaseRepo?.saveCustomExercise(newEx)
        onSaveCustom?.invoke(customOnly)
        return newEx
    }

    fun updatePinnedNote(exerciseId: String, note: String) {
        _exercises.update { list ->
            list.map { ex ->
                if (ex.id == exerciseId) ex.copy(pinnedNote = note) else ex
            }
        }
    }
}

class RoutineRepository(
    initialRoutines: List<RoutineBlueprint> = emptyList(),
    private val databaseRepo: KumaDatabaseRepository? = null,
    private val onSave: ((List<RoutineBlueprint>) -> Unit)? = null
) {
    private val _routines = MutableStateFlow(initialRoutines)
    val routines: StateFlow<List<RoutineBlueprint>> = _routines.asStateFlow()

    fun getRoutineById(id: String): RoutineBlueprint? {
        return _routines.value.find { it.id == id }
    }

    fun addRoutine(routine: RoutineBlueprint) {
        val routineWithId = if (routine.id.isBlank()) {
            routine.copy(id = IdGenerator.routineId(routine.title))
        } else routine

        _routines.update { current ->
            listOf(routineWithId) + current
        }

        // T5: Side effects run strictly outside StateFlow.update
        databaseRepo?.saveCustomRoutine(routineWithId)
        onSave?.invoke(_routines.value)
    }

    fun updateRoutine(routine: RoutineBlueprint) {
        _routines.update { current ->
            current.map { if (it.id == routine.id) routine else it }
        }
        databaseRepo?.saveCustomRoutine(routine)
        onSave?.invoke(_routines.value)
    }

    fun deleteRoutine(routineId: String) {
        _routines.update { current ->
            current.filterNot { it.id == routineId }
        }

        // T5: Side effects run strictly outside StateFlow.update
        databaseRepo?.deleteCustomRoutine(routineId)
        onSave?.invoke(_routines.value)
    }

    fun deleteRoutinesForProgram(programId: String) {
        _routines.update { current ->
            current.filterNot { it.programId == programId }
        }
        databaseRepo?.deleteCustomRoutinesForProgram(programId)
        onSave?.invoke(_routines.value)
    }

    fun getRoutinesForProgram(programId: String): List<RoutineBlueprint> {
        return _routines.value.filter { it.programId == programId }
    }
}

class PersonalRecordRepository(
    initialPrs: List<PersonalRecord> = emptyList(),
    private val databaseRepo: KumaDatabaseRepository? = null,
    private val calculationSettingsProvider: () -> CalculationSettings = { CalculationSettings() },
    private val dateFormatProvider: () -> DateFormatPreference = { DateFormatPreference.SYSTEM }
) {
    private val _prs = MutableStateFlow(
        sanitizeInitialPrs(initialPrs, calculationSettingsProvider(), databaseRepo)
    )
    val personalRecords: StateFlow<List<PersonalRecord>> = _prs.asStateFlow()

    companion object {
        fun sanitizeInitialPrs(
            prs: List<PersonalRecord>,
            calcSettings: CalculationSettings,
            databaseRepo: KumaDatabaseRepository?
        ): List<PersonalRecord> {
            if (calcSettings.repRangePrs) return prs
            val grouped = prs.groupBy { it.exerciseId }
            val sanitized = mutableListOf<PersonalRecord>()
            for ((exerciseId, list) in grouped) {
                if (list.size > 1) {
                    val best = list.maxWithOrNull(compareBy({ it.weightKg }, { it.reps })) ?: list.first()
                    sanitized.add(best)
                    databaseRepo?.deletePersonalRecordsForExercise(exerciseId)
                    databaseRepo?.savePersonalRecord(best)
                } else {
                    sanitized.addAll(list)
                }
            }
            return sanitized
        }
    }

    fun detectAndRecordPr(
        exercise: Exercise,
        weightKg: Double,
        reps: Int,
        sourceSetId: String? = null,
        setType: SetType = SetType.NORMAL
    ): PersonalRecord? {
        // Exclude WARMUP and DROP sets from PR eligibility per spec
        if (setType == SetType.WARMUP || setType == SetType.DROP) return null
        if (weightKg <= 0.0 || reps <= 0) return null

        val calcSettings = calculationSettingsProvider()
        val e1rm = OneRepMaxCalculator.calculate(
            weightKg = weightKg,
            reps = reps,
            formula = calcSettings.oneRepMaxFormula,
            maxReps = calcSettings.maxRepsForEstimate
        )

        val hasWeightMetric = calcSettings.prMetrics.contains(PrMetricType.WEIGHT)
        val hasE1rmMetric = calcSettings.prMetrics.contains(PrMetricType.E1RM)

        val exercisePrs = _prs.value.filter { it.exerciseId == exercise.id }
        val currentMaxWeight = maxOf(exercisePrs.maxOfOrNull { it.weightKg } ?: 0.0, exercise.currentPrWeightKg ?: 0.0)

        val baselineE1rm = if (exercise.currentPrWeightKg != null && exercise.currentPrWeightKg > 0.0) {
            val baselineReps = exercise.currentPrReps ?: reps
            OneRepMaxCalculator.calculate(
                weightKg = exercise.currentPrWeightKg,
                reps = baselineReps,
                formula = calcSettings.oneRepMaxFormula,
                maxReps = calcSettings.maxRepsForEstimate
            )
        } else 0.0

        val currentMaxE1rm = maxOf(exercisePrs.maxOfOrNull { it.estimated1RmKg } ?: 0.0, baselineE1rm)
        val currentRepMax = maxOf(
            exercisePrs.filter { it.reps >= reps }.maxOfOrNull { it.weightKg } ?: 0.0,
            if ((exercise.currentPrReps ?: 1) >= reps) (exercise.currentPrWeightKg ?: 0.0) else 0.0
        )

        val hasAnyRecord = currentMaxWeight > 0.0 || currentMaxE1rm > 0.0

        // Check each metric independently (no else-if short circuiting e1rm)
        val isWeightPr = hasWeightMetric && (currentMaxWeight > 0.0 && weightKg > currentMaxWeight)
        val isE1rmPr = hasE1rmMetric && (currentMaxE1rm > 0.0 && e1rm > currentMaxE1rm)
        val isRepRangePr = calcSettings.repRangePrs && (weightKg > currentRepMax)

        val isPr = if (!hasAnyRecord) {
            true // Initial completed normal set for an unrecorded exercise establishes the initial PR
        } else {
            isWeightPr || isE1rmPr || isRepRangePr
        }

        if (isPr) {
            val delta = when {
                !hasAnyRecord -> 0.0
                isWeightPr -> weightKg - currentMaxWeight
                isRepRangePr && currentRepMax > 0.0 -> weightKg - currentRepMax
                else -> 0.0
            }
            val now = currentEpochMillis()
            val tier = when {
                calcSettings.repRangePrs && reps > 1 -> "${reps}RM PR"
                isWeightPr && reps == 1 -> "1RM PR"
                isWeightPr -> "1RM PR"
                else -> "e1RM PR"
            }
            val newPr = PersonalRecord(
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                weightKg = weightKg,
                reps = reps,
                estimated1RmKg = e1rm,
                dateAchieved = formatDateEpoch(now, dateFormatProvider()),
                deltaWeightKg = delta,
                prTier = tier,
                dateAchievedEpochMs = now,
                sourceSetId = sourceSetId
            )

            _prs.update { list ->
                if (calcSettings.repRangePrs) {
                    list.filterNot { it.exerciseId == exercise.id && it.reps == reps } + newPr
                } else {
                    list.filterNot { it.exerciseId == exercise.id } + newPr
                }
            }

            // T5: Side effects run strictly outside StateFlow.update
            if (!calcSettings.repRangePrs) {
                databaseRepo?.deletePersonalRecordsForExercise(exercise.id)
            }
            databaseRepo?.savePersonalRecord(newPr)
            KumaLogger.i("PersonalRecord", "New PR detected for ${exercise.name}: ${weightKg}kg x $reps ($tier)")
            return newPr
        }
        return null
    }

    fun recomputePersonalRecordsForExercise(
        exerciseId: String,
        allWorkouts: List<Workout>,
        calcSettings: CalculationSettings
    ) {
        val hasWeightMetric = calcSettings.prMetrics.contains(PrMetricType.WEIGHT)
        val hasE1rmMetric = calcSettings.prMetrics.contains(PrMetricType.E1RM)
        val sortedWorkouts = allWorkouts.sortedBy { it.startTimeEpochMs }

        val repMap = mutableMapOf<Int, PersonalRecord>()
        var overallBest: PersonalRecord? = null
        var maxWeight = 0.0
        var maxE1rm = 0.0

        for (workout in sortedWorkouts) {
            for (we in workout.exercises.filter { it.exercise.id == exerciseId }) {
                for (set in we.sets.filter { it.isCompleted }) {
                    if (set.setType == SetType.WARMUP || set.setType == SetType.DROP) continue
                    if (set.weightKg <= 0.0 || set.reps <= 0) continue

                    val e1rm = OneRepMaxCalculator.calculate(
                        weightKg = set.weightKg,
                        reps = set.reps,
                        formula = calcSettings.oneRepMaxFormula,
                        maxReps = calcSettings.maxRepsForEstimate
                    )

                    val isWeightPr = hasWeightMetric && (set.weightKg > maxWeight)
                    val isE1rmPr = hasE1rmMetric && (e1rm > maxE1rm)
                    val prevRepWeight = repMap[set.reps]?.weightKg ?: 0.0
                    val isRepRangePr = calcSettings.repRangePrs && (set.weightKg > prevRepWeight)

                    val tier = when {
                        calcSettings.repRangePrs && set.reps > 1 -> "${set.reps}RM PR"
                        isWeightPr && set.reps == 1 -> "1RM PR"
                        isWeightPr -> "Max Weight PR"
                        else -> "e1RM PR"
                    }
                    val pr = PersonalRecord(
                        exerciseId = exerciseId,
                        exerciseName = we.exercise.name,
                        weightKg = set.weightKg,
                        reps = set.reps,
                        estimated1RmKg = e1rm,
                        dateAchieved = formatDateEpoch(workout.startTimeEpochMs, dateFormatProvider()),
                        deltaWeightKg = if (maxWeight > 0.0) set.weightKg - maxWeight else 0.0,
                        prTier = tier,
                        dateAchievedEpochMs = workout.startTimeEpochMs,
                        sourceSetId = set.id
                    )

                    if (isWeightPr) maxWeight = set.weightKg
                    if (isE1rmPr) maxE1rm = e1rm
                    if (isWeightPr || isE1rmPr) overallBest = pr
                    if (isRepRangePr || repMap[set.reps] == null) {
                        repMap[set.reps] = pr
                    }
                }
            }
        }

        val restoredPrs = if (calcSettings.repRangePrs) repMap.values.toList() else listOfNotNull(overallBest)

        _prs.update { list ->
            list.filterNot { it.exerciseId == exerciseId } + restoredPrs
        }
        databaseRepo?.let { db ->
            db.deletePersonalRecordsForExercise(exerciseId)
            restoredPrs.forEach { db.savePersonalRecord(it) }
        }
    }

    fun recomputePersonalRecords(
        allWorkouts: List<Workout>,
        calcSettings: CalculationSettings
    ) {
        val allExerciseIds = allWorkouts.flatMap { it.exercises.map { we -> we.exercise.id } }.distinct()
        for (exId in allExerciseIds) {
            recomputePersonalRecordsForExercise(exId, allWorkouts, calcSettings)
        }
    }

    fun revokePrBySetId(
        sourceSetId: String,
        exerciseId: String? = null,
        allWorkouts: List<Workout> = emptyList()
    ) {
        val affected = _prs.value.find { it.sourceSetId == sourceSetId }
        val targetExerciseId = exerciseId ?: affected?.exerciseId
        if (affected != null) {
            _prs.update { list -> list.filterNot { it.sourceSetId == sourceSetId } }
            databaseRepo?.deletePersonalRecordBySourceSet(sourceSetId)
        }
        if (targetExerciseId != null && allWorkouts.isNotEmpty()) {
            recomputePersonalRecordsForExercise(targetExerciseId, allWorkouts, calculationSettingsProvider())
        }
    }

    private fun formatDateEpoch(epochMs: Long, pref: DateFormatPreference = DateFormatPreference.SYSTEM): String {
        if (epochMs <= 0L) return "Today"
        val instant = kotlinx.datetime.Instant.fromEpochMilliseconds(epochMs)
        val dt = instant.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
        return when (pref) {
            DateFormatPreference.DMY -> "${dt.dayOfMonth.toString().padStart(2, '0')}/${dt.monthNumber.toString().padStart(2, '0')}/${dt.year}"
            DateFormatPreference.MDY -> "${dt.monthNumber.toString().padStart(2, '0')}/${dt.dayOfMonth.toString().padStart(2, '0')}/${dt.year}"
            DateFormatPreference.YMD -> "${dt.year}-${dt.monthNumber.toString().padStart(2, '0')}-${dt.dayOfMonth.toString().padStart(2, '0')}"
            DateFormatPreference.SYSTEM -> {
                val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                val month = monthNames.getOrElse(dt.monthNumber - 1) { "Jan" }
                "$month ${dt.dayOfMonth}"
            }
        }
    }
}

class WorkoutRepository(
    private val prRepository: PersonalRecordRepository,
    private val databaseRepo: KumaDatabaseRepository? = null,
    private val scheduleRepository: WorkoutScheduleRepository? = null,
    private val calculationSettingsProvider: () -> CalculationSettings = { CalculationSettings() },
    private val loggingSettingsProvider: () -> LoggingSettings = { LoggingSettings() },
    staleWorkoutHours: Int = 24
) {
    private val _activeWorkout = MutableStateFlow<Workout?>(null)
    val activeWorkout: StateFlow<Workout?> = _activeWorkout.asStateFlow()

    private val _completedWorkouts = MutableStateFlow<List<Workout>>(emptyList())
    val completedWorkouts: StateFlow<List<Workout>> = _completedWorkouts.asStateFlow()

    fun setCompletedWorkoutsForTesting(workouts: List<Workout>) {
        _completedWorkouts.value = workouts
    }

    init {
        // T1: Restore completed workouts from local database
        val savedCompleted = databaseRepo?.getCompletedWorkouts() ?: emptyList()
        _completedWorkouts.value = savedCompleted

        // T1: Restore active unfinished workout if valid and not stale
        val restoredActive = databaseRepo?.loadActiveWorkout(staleWorkoutHours)
        _activeWorkout.value = restoredActive
    }

    fun startWorkoutFromRoutine(
        routine: RoutineBlueprint,
        prefillSource: PrefillSource = PrefillSource.LAST_SESSION,
        scheduledEventId: String? = null
    ) {
        val now = currentEpochMillis()
        val configs = routine.effectiveConfigs
        val workoutExercises = configs.mapIndexed { index, config ->
            val exercise = config.exercise
            val previousSet = findPreviousSet(exercise.id, prefillSource)
            // Bodyweight default is 0 added weight, barbell/dumbbell default from target, previous, or starting weight
            val isBodyweight = exercise.equipment == Equipment.BODYWEIGHT
            val defaultWeight = config.targetWeightKg
                ?: previousSet?.weightKg
                ?: (if (isBodyweight) 0.0 else (exercise.currentPrWeightKg?.let { it * 0.75 } ?: 20.0))
            val defaultReps = if (config.targetReps > 0) config.targetReps else (previousSet?.reps ?: 8)
            val setsCount = maxOf(1, config.targetSets)

            val sets = config.effectiveSets.mapIndexed { setIndex, setConfig ->
                val targetWeight = setConfig.targetWeightKg ?: defaultWeight
                val targetReps = if (setConfig.targetReps > 0) setConfig.targetReps else defaultReps
                val prevWeight = setConfig.previousWeightKg ?: previousSet?.weightKg
                val prevReps = setConfig.previousReps ?: previousSet?.reps
                SetLog(
                    id = IdGenerator.setId(exercise.id, setIndex + 1),
                    setNumber = setIndex + 1,
                    setType = SetType.NORMAL,
                    weightKg = targetWeight,
                    reps = targetReps,
                    rpe = setConfig.targetRpe,
                    isCompleted = false,
                    targetWeightKg = targetWeight,
                    targetReps = targetReps,
                    previousWeightKg = prevWeight,
                    previousReps = prevReps,
                    restTimerSeconds = setConfig.restTimerSeconds
                )
            }
            WorkoutExercise(
                id = IdGenerator.workoutExerciseId(exercise.id),
                exercise = exercise,
                sets = sets,
                restTimerSeconds = config.restTimerSeconds
            )
        }

        val newWorkout = Workout(
            id = IdGenerator.workoutId(routine.id),
            title = routine.title,
            splitType = routine.splitCategory,
            startTimeEpochMs = now,
            exercises = workoutExercises,
            isFinished = false,
            scheduledEventId = scheduledEventId
        )

        _activeWorkout.value = newWorkout
        databaseRepo?.saveActiveWorkout(newWorkout)
        KumaLogger.i("WorkoutRepository", "Started workout from routine '${routine.title}' with ${workoutExercises.size} exercises")
    }

    fun startEmptyWorkout(title: String = "Freestyle Workout") {
        val now = currentEpochMillis()
        val newWorkout = Workout(
            id = IdGenerator.workoutId("freestyle"),
            title = title,
            splitType = "Freestyle",
            startTimeEpochMs = now,
            exercises = emptyList(),
            isFinished = false
        )
        _activeWorkout.value = newWorkout
        databaseRepo?.saveActiveWorkout(newWorkout)
        KumaLogger.i("WorkoutRepository", "Started freestyle workout: $title")
    }

    fun toggleSetCompletion(
        exerciseId: String,
        setId: String,
        currentHeartRateBpm: Int? = null,
        onCompleted: (Boolean) -> Unit
    ) {
        var completedSet: SetLog? = null
        var exerciseForPr: Exercise? = null
        var isNowCompleted = false

        _activeWorkout.update { currentWorkout ->
            if (currentWorkout == null) return@update null
            val hasDirectMatch = currentWorkout.exercises.any { it.id == exerciseId }
            val updatedExercises = currentWorkout.exercises.map { we ->
                val isMatch = if (hasDirectMatch) we.id == exerciseId else (we.id == exerciseId || we.exercise.id == exerciseId)
                if (isMatch) {
                    val updatedSets = we.sets.map { set ->
                        if (set.id == setId) {
                            val newCompletion = !set.isCompleted
                            isNowCompleted = newCompletion
                            val updatedSet = set.copy(
                                isCompleted = newCompletion,
                                heartRateBpm = if (newCompletion) (currentHeartRateBpm ?: set.heartRateBpm) else null
                            )
                            if (newCompletion) {
                                completedSet = updatedSet
                                exerciseForPr = we.exercise
                            }
                            updatedSet
                        } else set
                    }
                    we.copy(sets = updatedSets)
                } else we
            }
            currentWorkout.copy(exercises = updatedExercises)
        }

        // T5: Side effects run strictly outside update lambda
        val updatedWorkout = _activeWorkout.value
        databaseRepo?.saveActiveWorkout(updatedWorkout)

        if (isNowCompleted && exerciseForPr != null && completedSet != null) {
            val pr = prRepository.detectAndRecordPr(
                exercise = exerciseForPr!!,
                weightKg = completedSet!!.weightKg,
                reps = completedSet!!.reps,
                sourceSetId = completedSet!!.id,
                setType = completedSet!!.setType
            )
            if (pr != null) {
                _activeWorkout.update { w ->
                    w?.copy(
                        exercises = w.exercises.map { we ->
                            we.copy(
                                sets = we.sets.map { s ->
                                    if (s.id == setId) s.copy(isPr = true) else s
                                }
                            )
                        }
                    )
                }
                databaseRepo?.saveActiveWorkout(_activeWorkout.value)
            }
        } else if (!isNowCompleted) {
            // T7: Un-toggling revokes PR generated by this set and restores previous PR from history
            val targetExercise = _activeWorkout.value?.exercises?.find { we -> we.sets.any { it.id == setId } }?.exercise
            val allHistory = _completedWorkouts.value + listOfNotNull(_activeWorkout.value)
            prRepository.revokePrBySetId(
                sourceSetId = setId,
                exerciseId = targetExercise?.id,
                allWorkouts = allHistory
            )
            _activeWorkout.update { w ->
                w?.copy(
                    exercises = w.exercises.map { we ->
                        we.copy(
                            sets = we.sets.map { s ->
                                if (s.id == setId) s.copy(isPr = false) else s
                            }
                        )
                    }
                )
            }
            databaseRepo?.saveActiveWorkout(_activeWorkout.value)
        }

        onCompleted(isNowCompleted)
    }

    fun updateSet(exerciseId: String, setId: String, weightKg: Double, reps: Int) {
        _activeWorkout.update { currentWorkout ->
            if (currentWorkout == null) return@update null
            val hasDirectMatch = currentWorkout.exercises.any { it.id == exerciseId }
            val updatedExercises = currentWorkout.exercises.map { we ->
                val isMatch = if (hasDirectMatch) we.id == exerciseId else (we.id == exerciseId || we.exercise.id == exerciseId)
                if (isMatch) {
                    val updatedSets = we.sets.map { set ->
                        if (set.id == setId) {
                            set.copy(weightKg = weightKg, reps = reps)
                        } else set
                    }
                    we.copy(sets = updatedSets)
                } else we
            }
            currentWorkout.copy(exercises = updatedExercises)
        }
        databaseRepo?.saveActiveWorkout(_activeWorkout.value)
    }

    fun updateSetType(exerciseId: String, setId: String, setType: SetType) {
        _activeWorkout.update { currentWorkout ->
            if (currentWorkout == null) return@update null
            val hasDirectMatch = currentWorkout.exercises.any { it.id == exerciseId }
            val updatedExercises = currentWorkout.exercises.map { we ->
                val isMatch = if (hasDirectMatch) we.id == exerciseId else (we.id == exerciseId || we.exercise.id == exerciseId)
                if (isMatch) {
                    val updatedSets = we.sets.map { set ->
                        if (set.id == setId) set.copy(setType = setType) else set
                    }
                    we.copy(sets = updatedSets)
                } else we
            }
            currentWorkout.copy(exercises = updatedExercises)
        }
        databaseRepo?.saveActiveWorkout(_activeWorkout.value)
    }

    fun updateSetRpe(exerciseId: String, setId: String, rpe: Double?) {
        _activeWorkout.update { currentWorkout ->
            if (currentWorkout == null) return@update null
            val hasDirectMatch = currentWorkout.exercises.any { it.id == exerciseId }
            val updatedExercises = currentWorkout.exercises.map { we ->
                val isMatch = if (hasDirectMatch) we.id == exerciseId else (we.id == exerciseId || we.exercise.id == exerciseId)
                if (isMatch) {
                    val updatedSets = we.sets.map { set ->
                        if (set.id == setId) set.copy(rpe = rpe) else set
                    }
                    we.copy(sets = updatedSets)
                } else we
            }
            currentWorkout.copy(exercises = updatedExercises)
        }
        databaseRepo?.saveActiveWorkout(_activeWorkout.value)
    }

    fun updateSetPrevious(exerciseId: String, setId: String, previousWeightKg: Double?, previousReps: Int?) {
        _activeWorkout.update { currentWorkout ->
            if (currentWorkout == null) return@update null
            val hasDirectMatch = currentWorkout.exercises.any { it.id == exerciseId }
            val updatedExercises = currentWorkout.exercises.map { we ->
                val isMatch = if (hasDirectMatch) we.id == exerciseId else (we.id == exerciseId || we.exercise.id == exerciseId)
                if (isMatch) {
                    val updatedSets = we.sets.map { set ->
                        if (set.id == setId) set.copy(previousWeightKg = previousWeightKg, previousReps = previousReps) else set
                    }
                    we.copy(sets = updatedSets)
                } else we
            }
            currentWorkout.copy(exercises = updatedExercises)
        }
        databaseRepo?.saveActiveWorkout(_activeWorkout.value)
    }

    fun updateExerciseRestTimer(exerciseId: String, restTimerSeconds: Int) {
        _activeWorkout.update { currentWorkout ->
            if (currentWorkout == null) return@update null
            val hasDirectMatch = currentWorkout.exercises.any { it.id == exerciseId }
            val updatedExercises = currentWorkout.exercises.map { we ->
                val isMatch = if (hasDirectMatch) we.id == exerciseId else (we.id == exerciseId || we.exercise.id == exerciseId)
                if (isMatch) {
                    we.copy(restTimerSeconds = restTimerSeconds)
                } else we
            }
            currentWorkout.copy(exercises = updatedExercises)
        }
        databaseRepo?.saveActiveWorkout(_activeWorkout.value)
    }

    fun updateSetRestTimer(exerciseId: String, setId: String, restTimerSeconds: Int?) {
        _activeWorkout.update { currentWorkout ->
            if (currentWorkout == null) return@update null
            val hasDirectMatch = currentWorkout.exercises.any { it.id == exerciseId }
            val updatedExercises = currentWorkout.exercises.map { we ->
                val isMatch = if (hasDirectMatch) we.id == exerciseId else (we.id == exerciseId || we.exercise.id == exerciseId)
                if (isMatch) {
                    val updatedSets = we.sets.map { set ->
                        if (set.id == setId) set.copy(restTimerSeconds = restTimerSeconds) else set
                    }
                    we.copy(sets = updatedSets)
                } else we
            }
            currentWorkout.copy(exercises = updatedExercises)
        }
        databaseRepo?.saveActiveWorkout(_activeWorkout.value)
    }

    fun updateAllSetsRestTimers(exerciseId: String, exerciseRestSeconds: Int, setTimers: Map<String, Int?>) {
        _activeWorkout.update { currentWorkout ->
            if (currentWorkout == null) return@update null
            val hasDirectMatch = currentWorkout.exercises.any { it.id == exerciseId }
            val updatedExercises = currentWorkout.exercises.map { we ->
                val isMatch = if (hasDirectMatch) we.id == exerciseId else (we.id == exerciseId || we.exercise.id == exerciseId)
                if (isMatch) {
                    val updatedSets = we.sets.map { set ->
                        val specific = setTimers[set.id]
                        set.copy(restTimerSeconds = specific)
                    }
                    we.copy(restTimerSeconds = exerciseRestSeconds, sets = updatedSets)
                } else we
            }
            currentWorkout.copy(exercises = updatedExercises)
        }
        databaseRepo?.saveActiveWorkout(_activeWorkout.value)
    }

    fun addSetToExercise(exerciseId: String) {
        _activeWorkout.update { currentWorkout ->
            if (currentWorkout == null) return@update null
            val hasDirectMatch = currentWorkout.exercises.any { it.id == exerciseId }
            val updatedExercises = currentWorkout.exercises.map { we ->
                val isMatch = if (hasDirectMatch) we.id == exerciseId else (we.id == exerciseId || we.exercise.id == exerciseId)
                if (isMatch) {
                    val lastSet = we.sets.lastOrNull()
                    val newSetNum = we.sets.size + 1
                    val newSet = SetLog(
                        id = IdGenerator.setId(we.exercise.id, newSetNum),
                        setNumber = newSetNum,
                        setType = SetType.NORMAL,
                        weightKg = lastSet?.weightKg ?: (if (we.exercise.equipment == Equipment.BODYWEIGHT) 0.0 else 20.0),
                        reps = lastSet?.reps ?: 10,
                        isCompleted = false,
                        previousWeightKg = lastSet?.weightKg,
                        previousReps = lastSet?.reps,
                        restTimerSeconds = lastSet?.restTimerSeconds ?: we.restTimerSeconds
                    )
                    we.copy(sets = we.sets + newSet)
                } else we
            }
            currentWorkout.copy(exercises = updatedExercises)
        }
        databaseRepo?.saveActiveWorkout(_activeWorkout.value)
    }

    fun removeSetFromExercise(exerciseId: String, setId: String) {
        val targetExercise = _activeWorkout.value?.exercises?.find { we -> we.id == exerciseId || we.exercise.id == exerciseId }?.exercise
        _activeWorkout.update { currentWorkout ->
            if (currentWorkout == null) return@update null
            val hasDirectMatch = currentWorkout.exercises.any { it.id == exerciseId }
            val updatedExercises = currentWorkout.exercises.map { we ->
                val isMatch = if (hasDirectMatch) we.id == exerciseId else (we.id == exerciseId || we.exercise.id == exerciseId)
                if (isMatch) {
                    val remainingSets = we.sets.filterNot { it.id == setId }
                        .mapIndexed { idx, s -> s.copy(setNumber = idx + 1) }
                    we.copy(sets = remainingSets)
                } else we
            }
            currentWorkout.copy(exercises = updatedExercises)
        }
        val allHistory = _completedWorkouts.value + listOfNotNull(_activeWorkout.value)
        prRepository.revokePrBySetId(
            sourceSetId = setId,
            exerciseId = targetExercise?.id,
            allWorkouts = allHistory
        )
        databaseRepo?.saveActiveWorkout(_activeWorkout.value)
        KumaLogger.i("WorkoutRepository", "Removed set $setId from exercise $exerciseId")
    }

    fun addExerciseToActiveWorkout(exercise: Exercise) {
        _activeWorkout.update { currentWorkout ->
            if (currentWorkout == null) return@update null
            val isBodyweight = exercise.equipment == Equipment.BODYWEIGHT
            val logSettings = loggingSettingsProvider()
            val previousSet = findPreviousSet(exercise.id, logSettings.prefillSource)
            val defaultWeight = previousSet?.weightKg
                ?: if (isBodyweight) 0.0 else (exercise.currentPrWeightKg?.let { it * 0.7 } ?: 20.0)
            val defaultReps = previousSet?.reps ?: 10

            val count = maxOf(1, minOf(logSettings.defaultSetCount, 10))
            val sets = (1..count).map { setIndex ->
                SetLog(
                    id = IdGenerator.setId(exercise.id, setIndex),
                    setNumber = setIndex,
                    setType = SetType.NORMAL,
                    weightKg = defaultWeight,
                    reps = defaultReps,
                    isCompleted = false,
                    targetWeightKg = defaultWeight,
                    targetReps = defaultReps,
                    previousWeightKg = previousSet?.weightKg,
                    previousReps = previousSet?.reps
                )
            }
            val newWe = WorkoutExercise(
                id = IdGenerator.workoutExerciseId(exercise.id),
                exercise = exercise,
                sets = sets
            )
            currentWorkout.copy(exercises = currentWorkout.exercises + newWe)
        }
        databaseRepo?.saveActiveWorkout(_activeWorkout.value)
    }

    fun recomputeDerivedStats() {
        val calcSettings = calculationSettingsProvider()
        prRepository.recomputePersonalRecords(_completedWorkouts.value, calcSettings)
    }

    fun removeExerciseFromActiveWorkout(workoutExerciseId: String) {
        val exerciseToRemove = _activeWorkout.value?.exercises?.find { it.id == workoutExerciseId || it.exercise.id == workoutExerciseId }
        val targetExerciseId = exerciseToRemove?.exercise?.id
        val setIds = exerciseToRemove?.sets?.map { it.id } ?: emptyList()
        _activeWorkout.update { currentWorkout ->
            if (currentWorkout == null) return@update null
            currentWorkout.copy(exercises = currentWorkout.exercises.filterNot { it.id == workoutExerciseId || it.exercise.id == workoutExerciseId })
        }
        databaseRepo?.saveActiveWorkout(_activeWorkout.value)
        setIds.forEach { setId ->
            prRepository.revokePrBySetId(setId)
        }
        if (targetExerciseId != null) {
            val allHistory = _completedWorkouts.value + listOfNotNull(_activeWorkout.value)
            prRepository.recomputePersonalRecordsForExercise(targetExerciseId, allHistory, calculationSettingsProvider())
        }
        KumaLogger.i("WorkoutRepository", "Removed exercise $workoutExerciseId from active workout")
    }

    fun updateWorkoutNotes(notes: String) {
        _activeWorkout.update { it?.copy(notes = notes) }
        databaseRepo?.saveActiveWorkout(_activeWorkout.value)
    }

    fun updateExerciseNotes(workoutExerciseId: String, notes: String) {
        _activeWorkout.update { currentWorkout ->
            if (currentWorkout == null) return@update null
            currentWorkout.copy(
                exercises = currentWorkout.exercises.map { we ->
                    if (we.id == workoutExerciseId || we.exercise.id == workoutExerciseId) {
                        we.copy(notes = notes)
                    } else we
                }
            )
        }
        databaseRepo?.saveActiveWorkout(_activeWorkout.value)
    }

    fun addWarmupSetsToExercise(exerciseId: String) {
        _activeWorkout.update { currentWorkout ->
            if (currentWorkout == null) return@update null
            val hasDirectMatch = currentWorkout.exercises.any { it.id == exerciseId }
            val updatedExercises = currentWorkout.exercises.map { we ->
                val isMatch = if (hasDirectMatch) we.id == exerciseId else (we.id == exerciseId || we.exercise.id == exerciseId)
                if (isMatch) {
                    val workingWeight = we.sets.firstOrNull { it.weightKg > 0.0 }?.weightKg
                        ?: we.exercise.currentPrWeightKg ?: 60.0
                    val barWeight = if (we.exercise.equipment == Equipment.DUMBBELL) 0.0 else 20.0
                    val warmupSets = com.bearbones.kumalift.domain.model.WarmupSetCalculator.generateWarmupSets(
                        workingWeightKg = workingWeight,
                        barWeightKg = barWeight,
                        weightStepKg = we.exercise.defaultIncrementKg
                    )
                    val existingNonWarmup = we.sets.filterNot { it.setType == SetType.WARMUP }
                    val combined = (warmupSets + existingNonWarmup).mapIndexed { idx, s ->
                        s.copy(setNumber = idx + 1)
                    }
                    we.copy(sets = combined)
                } else we
            }
            currentWorkout.copy(exercises = updatedExercises)
        }
        databaseRepo?.saveActiveWorkout(_activeWorkout.value)
    }

    fun updateExerciseSupersetGroup(exerciseId: String, supersetGroupId: String?) {
        _activeWorkout.update { currentWorkout ->
            if (currentWorkout == null) return@update null
            val hasDirectMatch = currentWorkout.exercises.any { it.id == exerciseId }
            val updatedExercises = currentWorkout.exercises.map { we ->
                val isMatch = if (hasDirectMatch) we.id == exerciseId else (we.id == exerciseId || we.exercise.id == exerciseId)
                if (isMatch) we.copy(supersetGroupId = supersetGroupId) else we
            }
            currentWorkout.copy(exercises = updatedExercises)
        }
        databaseRepo?.saveActiveWorkout(_activeWorkout.value)
    }

    fun applyProgressionToExercise(exerciseId: String, newWeightKg: Double, targetReps: Int? = null) {
        _activeWorkout.update { currentWorkout ->
            if (currentWorkout == null) return@update null
            val hasDirectMatch = currentWorkout.exercises.any { it.id == exerciseId }
            val updatedExercises = currentWorkout.exercises.map { we ->
                val isMatch = if (hasDirectMatch) we.id == exerciseId else (we.id == exerciseId || we.exercise.id == exerciseId)
                if (isMatch) {
                    val updatedSets = we.sets.map { s ->
                        if (s.setType != SetType.WARMUP) {
                            s.copy(
                                weightKg = newWeightKg,
                                targetWeightKg = newWeightKg,
                                reps = if (!s.isCompleted && targetReps != null && targetReps > 0) targetReps else s.reps,
                                targetReps = targetReps ?: s.targetReps
                            )
                        } else s
                    }
                    we.copy(sets = updatedSets)
                } else we
            }
            currentWorkout.copy(exercises = updatedExercises)
        }
        databaseRepo?.saveActiveWorkout(_activeWorkout.value)
    }

    /**
     * Retrieves the most recent completed working sets for an exercise across past workouts,
     * allowing the progressive overload advisor to analyze historical performance.
     */
    fun getPreviousCompletedSetsForExercise(exerciseId: String): List<SetLog> {
        val workouts = _completedWorkouts.value
        val targetWorkout = workouts
            .sortedByDescending { it.endTimeEpochMs ?: it.startTimeEpochMs }
            .firstOrNull { w ->
                w.exercises.any { we ->
                    (we.exercise.id == exerciseId || we.id == exerciseId) &&
                            we.sets.any { s -> s.isCompleted && s.setType != SetType.WARMUP }
                }
            } ?: return emptyList()

        return targetWorkout.exercises
            .firstOrNull { it.exercise.id == exerciseId || it.id == exerciseId }
            ?.sets
            ?.filter { it.isCompleted }
            ?: emptyList()
    }

    fun pauseWorkout() {
        val current = _activeWorkout.value ?: return
        if (current.isPaused) return
        val now = currentEpochMillis()
        val updated = current.copy(isPaused = true, pausedAtEpochMs = now)
        _activeWorkout.value = updated
        databaseRepo?.saveActiveWorkout(updated)
        KumaLogger.i("WorkoutRepository", "Paused workout '${current.title}' (${current.id})")
    }

    fun resumeWorkout() {
        val current = _activeWorkout.value ?: return
        if (!current.isPaused) return
        val now = currentEpochMillis()
        val pausedDuration = if (current.pausedAtEpochMs != null) now - current.pausedAtEpochMs else 0L
        val updated = current.copy(
            isPaused = false,
            pausedAtEpochMs = null,
            totalPausedDurationMs = current.totalPausedDurationMs + maxOf(0L, pausedDuration)
        )
        _activeWorkout.value = updated
        databaseRepo?.saveActiveWorkout(updated)
        KumaLogger.i("WorkoutRepository", "Resumed workout '${current.title}' (${current.id})")
    }

    fun finishWorkout(
        sessionRpe: Double = 8.5,
        avgHeartRateBpm: Int? = null,
        peakHeartRateBpm: Int? = null,
        activeCaloriesKcal: Double? = null
    ): Workout? {
        val current = _activeWorkout.value ?: return null
        val now = currentEpochMillis()
        val durationSec = if (current.startTimeEpochMs > 0L) {
            val finalNow = if (current.isPaused && current.pausedAtEpochMs != null) current.pausedAtEpochMs else now
            maxOf(1L, (finalNow - current.startTimeEpochMs - current.totalPausedDurationMs) / 1000L)
        } else current.durationSeconds

        val finished = current.copy(
            isFinished = true,
            sessionRpe = sessionRpe,
            endTimeEpochMs = now,
            durationSeconds = durationSec,
            avgHeartRateBpm = avgHeartRateBpm,
            peakHeartRateBpm = peakHeartRateBpm,
            activeCaloriesKcal = activeCaloriesKcal
        )

        _completedWorkouts.update { listOf(finished) + it }
        _activeWorkout.value = null

        // T1: Persist to database and clear active state
        databaseRepo?.saveCompletedWorkout(finished)
        databaseRepo?.clearActiveWorkout()

        // Link scheduled event if present
        finished.scheduledEventId?.let { eventId ->
            scheduleRepository?.completeEvent(eventId, finished.id)
            databaseRepo?.updateScheduleEventStatus(
                id = eventId,
                status = ScheduleStatus.COMPLETED,
                linkedWorkoutId = finished.id
            )
        }

        KumaLogger.i("WorkoutRepository", "Finished workout '${finished.title}' (${finished.id}) - duration ${durationSec}s, volume ${finished.totalVolumeKg}kg, sets ${finished.totalCompletedSets}")

        return finished
    }

    fun cancelWorkout() {
        val active = _activeWorkout.value
        if (active != null) {
            val activeSetIds = active.exercises.flatMap { it.sets }.map { it.id }
            for (setId in activeSetIds) {
                prRepository.revokePrBySetId(setId)
            }
        }
        _activeWorkout.value = null
        databaseRepo?.clearActiveWorkout()
    }

    private fun findPreviousSet(exerciseId: String, prefillSource: PrefillSource): SetLog? {
        if (prefillSource == PrefillSource.NONE) return null
        val workouts = _completedWorkouts.value
        val sets = workouts.flatMap { w ->
            w.exercises.filter { it.exercise.id == exerciseId }.flatMap { it.sets.filter { s -> s.isCompleted } }
        }

        return when (prefillSource) {
            PrefillSource.BEST_SET -> sets.maxByOrNull { it.weightKg }
            PrefillSource.LAST_SESSION -> sets.firstOrNull()
            PrefillSource.NONE -> null
        }
    }
}
