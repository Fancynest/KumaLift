package com.bearbones.kumalift.data.repository

import com.bearbones.kumalift.data.local.SeedData
import com.bearbones.kumalift.domain.model.Equipment
import com.bearbones.kumalift.domain.model.Exercise
import com.bearbones.kumalift.domain.model.MuscleGroup
import com.bearbones.kumalift.domain.model.PersonalRecord
import com.bearbones.kumalift.domain.model.RoutineBlueprint
import com.bearbones.kumalift.domain.model.SetLog
import com.bearbones.kumalift.domain.model.SetType
import com.bearbones.kumalift.domain.model.Workout
import com.bearbones.kumalift.domain.model.WorkoutExercise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ExerciseRepository(
    initialExercises: List<Exercise> = SeedData.defaultExercises,
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
        movementType: com.bearbones.kumalift.domain.model.MovementType = com.bearbones.kumalift.domain.model.MovementType.COMPOUND
    ): Exercise {
        val cleanId = "custom_${name.lowercase().replace(Regex("[^a-z0-9]"), "_")}_${_exercises.value.size + 1}"
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
            val updated = listOf(newEx) + current
            onSaveCustom?.invoke(updated.filter { it.id.startsWith("custom_") })
            updated
        }
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
    private val onSave: ((List<RoutineBlueprint>) -> Unit)? = null
) {
    private val _routines = MutableStateFlow(initialRoutines)
    val routines: StateFlow<List<RoutineBlueprint>> = _routines.asStateFlow()

    fun getRoutineById(id: String): RoutineBlueprint? {
        return _routines.value.find { it.id == id }
    }

    fun addRoutine(routine: RoutineBlueprint) {
        _routines.update { current ->
            val updated = listOf(routine) + current
            onSave?.invoke(updated)
            updated
        }
    }

    fun deleteRoutine(routineId: String) {
        _routines.update { current ->
            val updated = current.filterNot { it.id == routineId }
            onSave?.invoke(updated)
            updated
        }
    }
}

class PersonalRecordRepository {
    private val _prs = MutableStateFlow(SeedData.personalRecords)
    val personalRecords: StateFlow<List<PersonalRecord>> = _prs.asStateFlow()

    fun detectAndRecordPr(exercise: Exercise, weightKg: Double, reps: Int): PersonalRecord? {
        val currentPr = _prs.value.find { it.exerciseId == exercise.id }
        val currentMax = currentPr?.weightKg ?: 0.0

        if (weightKg > currentMax && reps > 0) {
            val delta = if (currentMax > 0) weightKg - currentMax else weightKg
            val newPr = PersonalRecord(
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                weightKg = weightKg,
                reps = reps,
                estimated1RmKg = weightKg * (1.0 + reps / 30.0),
                dateAchieved = "Today",
                deltaWeightKg = delta,
                prTier = "${reps}RM PR"
            )
            _prs.update { list ->
                list.filterNot { it.exerciseId == exercise.id } + newPr
            }
            return newPr
        }
        return null
    }
}

class WorkoutRepository(
    private val prRepository: PersonalRecordRepository
) {
    private val _activeWorkout = MutableStateFlow<Workout?>(null)
    val activeWorkout: StateFlow<Workout?> = _activeWorkout.asStateFlow()

    private val _completedWorkouts = MutableStateFlow<List<Workout>>(emptyList())
    val completedWorkouts: StateFlow<List<Workout>> = _completedWorkouts.asStateFlow()

    init {
        // Pre-populate one sample active workout (Push Day) matching the Stitch design
        startSamplePushDayWorkout()
    }

    fun startWorkoutFromRoutine(routine: RoutineBlueprint) {
        val workoutExercises = routine.exerciseTemplates.mapIndexed { index, exercise ->
            val sets = (1..routine.plannedSetsPerExercise).map { setIndex ->
                SetLog(
                    id = "set_${exercise.id}_$setIndex",
                    setNumber = setIndex,
                    setType = if (setIndex == 1) SetType.NORMAL else SetType.NORMAL,
                    weightKg = exercise.currentPrWeightKg?.let { it * 0.75 } ?: 60.0,
                    reps = 8,
                    isCompleted = false,
                    targetWeightKg = exercise.currentPrWeightKg?.let { it * 0.75 } ?: 60.0,
                    targetReps = 8
                )
            }
            WorkoutExercise(
                id = "we_${exercise.id}_$index",
                exercise = exercise,
                sets = sets
            )
        }

        _activeWorkout.value = Workout(
            id = "w_${routine.id}_now",
            title = routine.title,
            splitType = routine.splitCategory,
            startTimeEpochMs = 0L,
            exercises = workoutExercises,
            isFinished = false
        )
    }

    fun startEmptyWorkout(title: String = "Freestyle Workout") {
        _activeWorkout.value = Workout(
            id = "w_freestyle_now",
            title = title,
            splitType = "Freestyle",
            startTimeEpochMs = 0L,
            exercises = emptyList(),
            isFinished = false
        )
    }

    fun startSamplePushDayWorkout() {
        val benchPress = SeedData.defaultExercises[0]
        val inclineDb = SeedData.defaultExercises[1]
        val cableFly = SeedData.defaultExercises[2]
        val tricepPushdown = SeedData.defaultExercises[8]

        val exercises = listOf(
            WorkoutExercise(
                id = "we_bench",
                exercise = benchPress,
                sets = listOf(
                    SetLog("s1", 1, SetType.NORMAL, 85.0, 8, isCompleted = true, previousWeightKg = 82.5, previousReps = 8),
                    SetLog("s2", 2, SetType.NORMAL, 85.0, 8, isCompleted = true, previousWeightKg = 85.0, previousReps = 8),
                    SetLog("s3", 3, SetType.NORMAL, 85.0, 7, isCompleted = false, previousWeightKg = 85.0, previousReps = 8)
                )
            ),
            WorkoutExercise(
                id = "we_incline",
                exercise = inclineDb,
                sets = listOf(
                    SetLog("s4", 1, SetType.NORMAL, 32.0, 10, isCompleted = true, previousWeightKg = 30.0, previousReps = 10),
                    SetLog("s5", 2, SetType.NORMAL, 32.0, 10, isCompleted = true, previousWeightKg = 32.0, previousReps = 10),
                    SetLog("s6", 3, SetType.NORMAL, 32.0, 8, isCompleted = true, previousWeightKg = 32.0, previousReps = 9)
                )
            ),
            WorkoutExercise(
                id = "we_cable_fly",
                exercise = cableFly,
                sets = listOf(
                    SetLog("s7", 1, SetType.NORMAL, 15.0, 12, isCompleted = true, previousWeightKg = 15.0, previousReps = 12),
                    SetLog("s8", 2, SetType.NORMAL, 15.0, 12, isCompleted = true, previousWeightKg = 15.0, previousReps = 12),
                    SetLog("s9", 3, SetType.DROP, 12.5, 15, isCompleted = true, previousWeightKg = 12.5, previousReps = 15)
                )
            ),
            WorkoutExercise(
                id = "we_tricep",
                exercise = tricepPushdown,
                sets = listOf(
                    SetLog("s10", 1, SetType.NORMAL, 27.5, 12, isCompleted = false, previousWeightKg = 25.0, previousReps = 12),
                    SetLog("s11", 2, SetType.NORMAL, 27.5, 12, isCompleted = false, previousWeightKg = 27.5, previousReps = 12),
                    SetLog("s12", 3, SetType.FAILURE, 27.5, 10, isCompleted = false, previousWeightKg = 27.5, previousReps = 10)
                )
            )
        )

        _activeWorkout.value = Workout(
            id = "w_active_push",
            title = "Push Day",
            splitType = "PPL Split",
            startTimeEpochMs = 0L,
            exercises = exercises,
            durationSeconds = 2058, // 34m 18s
            isFinished = false
        )
    }

    fun toggleSetCompletion(
        exerciseId: String,
        setId: String,
        currentHeartRateBpm: Int? = null,
        onCompleted: (Boolean) -> Unit
    ) {
        _activeWorkout.update { currentWorkout ->
            if (currentWorkout == null) return@update null
            val updatedExercises = currentWorkout.exercises.map { we ->
                if (we.exercise.id == exerciseId) {
                    val updatedSets = we.sets.map { set ->
                        if (set.id == setId) {
                            val newCompletion = !set.isCompleted
                            if (newCompletion) {
                                prRepository.detectAndRecordPr(we.exercise, set.weightKg, set.reps)
                            }
                            onCompleted(newCompletion)
                            set.copy(
                                isCompleted = newCompletion,
                                heartRateBpm = if (newCompletion) (currentHeartRateBpm ?: set.heartRateBpm) else null
                            )
                        } else set
                    }
                    we.copy(sets = updatedSets)
                } else we
            }
            currentWorkout.copy(exercises = updatedExercises)
        }
    }

    fun updateSet(exerciseId: String, setId: String, weightKg: Double, reps: Int) {
        _activeWorkout.update { currentWorkout ->
            if (currentWorkout == null) return@update null
            val updatedExercises = currentWorkout.exercises.map { we ->
                if (we.exercise.id == exerciseId) {
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
    }

    fun addSetToExercise(exerciseId: String) {
        _activeWorkout.update { currentWorkout ->
            if (currentWorkout == null) return@update null
            val updatedExercises = currentWorkout.exercises.map { we ->
                if (we.exercise.id == exerciseId) {
                    val lastSet = we.sets.lastOrNull()
                    val newSetNum = we.sets.size + 1
                    val newSet = SetLog(
                        id = "set_${exerciseId}_$newSetNum",
                        setNumber = newSetNum,
                        setType = SetType.NORMAL,
                        weightKg = lastSet?.weightKg ?: 20.0,
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
    }

    fun addExerciseToActiveWorkout(exercise: Exercise) {
        _activeWorkout.update { currentWorkout ->
            if (currentWorkout == null) return@update null
            val newWe = WorkoutExercise(
                id = "we_${exercise.id}_${currentWorkout.exercises.size}",
                exercise = exercise,
                sets = listOf(
                    SetLog(
                        id = "set_${exercise.id}_1",
                        setNumber = 1,
                        weightKg = exercise.currentPrWeightKg?.let { it * 0.7 } ?: 20.0,
                        reps = 10,
                        isCompleted = false
                    )
                )
            )
            currentWorkout.copy(exercises = currentWorkout.exercises + newWe)
        }
    }

    fun finishWorkout(
        sessionRpe: Double = 8.5,
        avgHeartRateBpm: Int? = null,
        peakHeartRateBpm: Int? = null,
        activeCaloriesKcal: Double? = null
    ): Workout? {
        val current = _activeWorkout.value ?: return null
        val finished = current.copy(
            isFinished = true,
            sessionRpe = sessionRpe,
            endTimeEpochMs = System.currentTimeMillis(),
            avgHeartRateBpm = avgHeartRateBpm,
            peakHeartRateBpm = peakHeartRateBpm,
            activeCaloriesKcal = activeCaloriesKcal
        )
        _completedWorkouts.update { it + finished }
        _activeWorkout.value = null
        return finished
    }
}
