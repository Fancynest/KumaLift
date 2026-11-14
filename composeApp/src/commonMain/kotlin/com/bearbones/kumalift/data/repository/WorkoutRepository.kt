package com.bearbones.kumalift.data.repository

import com.bearbones.kumalift.core.util.IdGenerator
import com.bearbones.kumalift.core.util.OneRepMaxCalculator
import com.bearbones.kumalift.core.util.currentEpochMillis
import com.bearbones.kumalift.data.local.KumaDatabaseRepository
import com.bearbones.kumalift.data.local.SeedData
import com.bearbones.kumalift.domain.model.CalculationSettings
import com.bearbones.kumalift.domain.model.Equipment
import com.bearbones.kumalift.domain.model.Exercise
import com.bearbones.kumalift.domain.model.MovementType
import com.bearbones.kumalift.domain.model.MuscleGroup
import com.bearbones.kumalift.domain.model.PersonalRecord
import com.bearbones.kumalift.domain.model.PrefillSource
import com.bearbones.kumalift.domain.model.RoutineBlueprint
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
            secondaryMuscleDetails = secondaryMuscles.joinToString(", ") { it.displayName },
            equipment = equipment,
            equipmentDetail = equipment.displayName,
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
    initialRoutines: List<RoutineBlueprint> = SeedData.defaultRoutines,
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

    fun deleteRoutine(routineId: String) {
        _routines.update { current ->
            current.filterNot { it.id == routineId }
        }

        // T5: Side effects run strictly outside StateFlow.update
        databaseRepo?.deleteCustomRoutine(routineId)
        onSave?.invoke(_routines.value)
    }
}

class PersonalRecordRepository(
    initialPrs: List<PersonalRecord> = emptyList(),
    private val databaseRepo: KumaDatabaseRepository? = null,
    private val calculationSettingsProvider: () -> CalculationSettings = { CalculationSettings() }
) {
    private val _prs = MutableStateFlow(initialPrs)
    val personalRecords: StateFlow<List<PersonalRecord>> = _prs.asStateFlow()

    fun detectAndRecordPr(
        exercise: Exercise,
        weightKg: Double,
        reps: Int,
        sourceSetId: String? = null,
        setType: SetType = SetType.NORMAL
    ): PersonalRecord? {
        // T7: Exclude warmup sets from PR eligibility per spec
        if (setType == SetType.WARMUP) return null
        if (weightKg <= 0.0 || reps <= 0) return null

        val calcSettings = calculationSettingsProvider()
        val e1rm = OneRepMaxCalculator.calculateEstimated1Rm(
            weightKg,
            reps,
            calcSettings.oneRepMaxFormula
        )

        val currentPr = _prs.value.find { it.exerciseId == exercise.id }
        val currentMaxWeight = maxOf(currentPr?.weightKg ?: 0.0, exercise.currentPrWeightKg ?: 0.0)

        if (weightKg > currentMaxWeight) {
            val delta = if (currentMaxWeight > 0.0) weightKg - currentMaxWeight else weightKg
            val now = currentEpochMillis()
            val newPr = PersonalRecord(
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                weightKg = weightKg,
                reps = reps,
                estimated1RmKg = e1rm,
                dateAchieved = formatDateEpoch(now),
                deltaWeightKg = delta,
                prTier = "${reps}RM PR",
                dateAchievedEpochMs = now,
                sourceSetId = sourceSetId
            )

            _prs.update { list ->
                list.filterNot { it.exerciseId == exercise.id } + newPr
            }

            // T5: Side effects run strictly outside StateFlow.update
            databaseRepo?.savePersonalRecord(newPr)
            return newPr
        }
        return null
    }

    fun revokePrBySetId(sourceSetId: String) {
        val affected = _prs.value.find { it.sourceSetId == sourceSetId }
        if (affected != null) {
            _prs.update { list -> list.filterNot { it.sourceSetId == sourceSetId } }
            databaseRepo?.deletePersonalRecordBySourceSet(sourceSetId)
        }
    }

    private fun formatDateEpoch(epochMs: Long): String {
        if (epochMs <= 0L) return "Today"
        val instant = kotlinx.datetime.Instant.fromEpochMilliseconds(epochMs)
        val dt = instant.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
        val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val month = monthNames.getOrElse(dt.monthNumber - 1) { "Jan" }
        return "$month ${dt.dayOfMonth}"
    }
}

class WorkoutRepository(
    private val prRepository: PersonalRecordRepository,
    private val databaseRepo: KumaDatabaseRepository? = null,
    staleWorkoutHours: Int = 24
) {
    private val _activeWorkout = MutableStateFlow<Workout?>(null)
    val activeWorkout: StateFlow<Workout?> = _activeWorkout.asStateFlow()

    private val _completedWorkouts = MutableStateFlow<List<Workout>>(emptyList())
    val completedWorkouts: StateFlow<List<Workout>> = _completedWorkouts.asStateFlow()

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
        prefillSource: PrefillSource = PrefillSource.LAST_SESSION
    ) {
        val now = currentEpochMillis()
        val workoutExercises = routine.exerciseTemplates.mapIndexed { index, exercise ->
            val previousSet = findPreviousSet(exercise.id, prefillSource)
            // Bodyweight default is 0 added weight, barbell/dumbbell default from previous or 0
            val isBodyweight = exercise.equipment == Equipment.BODYWEIGHT
            val defaultWeight = previousSet?.weightKg
                ?: (if (isBodyweight) 0.0 else (exercise.currentPrWeightKg?.let { it * 0.75 } ?: 20.0))
            val defaultReps = previousSet?.reps ?: 8

            val sets = (1..routine.plannedSetsPerExercise).map { setIndex ->
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
            WorkoutExercise(
                id = IdGenerator.workoutExerciseId(exercise.id),
                exercise = exercise,
                sets = sets
            )
        }

        val newWorkout = Workout(
            id = IdGenerator.workoutId(routine.id),
            title = routine.title,
            splitType = routine.splitCategory,
            startTimeEpochMs = now,
            exercises = workoutExercises,
            isFinished = false
        )

        _activeWorkout.value = newWorkout
        databaseRepo?.saveActiveWorkout(newWorkout)
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
            val updatedExercises = currentWorkout.exercises.map { we ->
                if (we.id == exerciseId || we.exercise.id == exerciseId) {
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
            // T7: Un-toggling revokes PR generated by this set
            prRepository.revokePrBySetId(setId)
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
            val updatedExercises = currentWorkout.exercises.map { we ->
                if (we.id == exerciseId || we.exercise.id == exerciseId) {
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
            val updatedExercises = currentWorkout.exercises.map { we ->
                if (we.id == exerciseId || we.exercise.id == exerciseId) {
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
            val updatedExercises = currentWorkout.exercises.map { we ->
                if (we.id == exerciseId || we.exercise.id == exerciseId) {
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

    fun addSetToExercise(exerciseId: String) {
        _activeWorkout.update { currentWorkout ->
            if (currentWorkout == null) return@update null
            val updatedExercises = currentWorkout.exercises.map { we ->
                if (we.id == exerciseId || we.exercise.id == exerciseId) {
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
                        previousReps = lastSet?.reps
                    )
                    we.copy(sets = we.sets + newSet)
                } else we
            }
            currentWorkout.copy(exercises = updatedExercises)
        }
        databaseRepo?.saveActiveWorkout(_activeWorkout.value)
    }

    fun removeSetFromExercise(exerciseId: String, setId: String) {
        _activeWorkout.update { currentWorkout ->
            if (currentWorkout == null) return@update null
            val updatedExercises = currentWorkout.exercises.map { we ->
                if (we.id == exerciseId || we.exercise.id == exerciseId) {
                    val remainingSets = we.sets.filterNot { it.id == setId }
                        .mapIndexed { idx, s -> s.copy(setNumber = idx + 1) }
                    we.copy(sets = remainingSets)
                } else we
            }
            currentWorkout.copy(exercises = updatedExercises)
        }
        prRepository.revokePrBySetId(setId)
        databaseRepo?.saveActiveWorkout(_activeWorkout.value)
    }

    fun addExerciseToActiveWorkout(exercise: Exercise) {
        _activeWorkout.update { currentWorkout ->
            if (currentWorkout == null) return@update null
            val isBodyweight = exercise.equipment == Equipment.BODYWEIGHT
            val defaultWeight = if (isBodyweight) 0.0 else (exercise.currentPrWeightKg?.let { it * 0.7 } ?: 20.0)
            val newWe = WorkoutExercise(
                id = IdGenerator.workoutExerciseId(exercise.id),
                exercise = exercise,
                sets = listOf(
                    SetLog(
                        id = IdGenerator.setId(exercise.id, 1),
                        setNumber = 1,
                        weightKg = defaultWeight,
                        reps = 10,
                        isCompleted = false
                    )
                )
            )
            currentWorkout.copy(exercises = currentWorkout.exercises + newWe)
        }
        databaseRepo?.saveActiveWorkout(_activeWorkout.value)
    }

    fun removeExerciseFromActiveWorkout(workoutExerciseId: String) {
        _activeWorkout.update { currentWorkout ->
            if (currentWorkout == null) return@update null
            currentWorkout.copy(exercises = currentWorkout.exercises.filterNot { it.id == workoutExerciseId || it.exercise.id == workoutExerciseId })
        }
        databaseRepo?.saveActiveWorkout(_activeWorkout.value)
    }

    fun updateWorkoutNotes(notes: String) {
        _activeWorkout.update { it?.copy(notes = notes) }
        databaseRepo?.saveActiveWorkout(_activeWorkout.value)
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
            maxOf(1L, (now - current.startTimeEpochMs) / 1000L)
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

        return finished
    }

    fun cancelWorkout() {
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
