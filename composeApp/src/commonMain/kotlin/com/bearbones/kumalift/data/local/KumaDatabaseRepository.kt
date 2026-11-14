package com.bearbones.kumalift.data.local

import com.bearbones.kumalift.core.util.IdGenerator
import com.bearbones.kumalift.core.util.currentEpochMillis
import com.bearbones.kumalift.database.KumaDatabase
import com.bearbones.kumalift.domain.model.Equipment
import com.bearbones.kumalift.domain.model.Exercise
import com.bearbones.kumalift.domain.model.MovementType
import com.bearbones.kumalift.domain.model.MuscleGroup
import com.bearbones.kumalift.domain.model.PersonalRecord
import com.bearbones.kumalift.domain.model.RoutineBlueprint
import com.bearbones.kumalift.domain.model.SetLog
import com.bearbones.kumalift.domain.model.SetType
import com.bearbones.kumalift.domain.model.UserSettings
import com.bearbones.kumalift.domain.model.Workout
import com.bearbones.kumalift.domain.model.WorkoutExercise
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class KumaDatabaseRepository(
    private val database: KumaDatabase
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        prettyPrint = false
    }

    private val queries = database.kumaDatabaseQueries

    // ==========================================
    // WORKOUTS
    // ==========================================

    fun getCompletedWorkouts(): List<Workout> {
        val workoutRows = queries.selectAllCompletedWorkouts().executeAsList()
        return workoutRows.map { wRow ->
            val weRows = queries.selectWorkoutExercises(wRow.id).executeAsList()
            val exercises = weRows.map { weRow ->
                val setRows = queries.selectSetsByWorkoutExerciseId(weRow.id).executeAsList()
                val sets = setRows.map { sRow ->
                    SetLog(
                        id = sRow.id,
                        setNumber = sRow.setNumber.toInt(),
                        setType = try { SetType.valueOf(sRow.setType) } catch (_: Exception) { SetType.NORMAL },
                        weightKg = sRow.weightKg,
                        reps = sRow.reps.toInt(),
                        rpe = sRow.rpe,
                        isCompleted = sRow.isCompleted == 1L,
                        targetWeightKg = sRow.targetWeightKg,
                        targetReps = sRow.targetReps?.toInt(),
                        previousWeightKg = sRow.previousWeightKg,
                        previousReps = sRow.previousReps?.toInt(),
                        heartRateBpm = sRow.heartRateBpm?.toInt()
                    )
                }

                val primaryMuscle = try { MuscleGroup.valueOf(weRow.primaryMuscle) } catch (_: Exception) { MuscleGroup.CHEST }
                val equipment = try { Equipment.valueOf(weRow.equipment) } catch (_: Exception) { Equipment.BARBELL }
                val movementType = try { MovementType.valueOf(weRow.movementType) } catch (_: Exception) { MovementType.COMPOUND }

                val exModel = Exercise(
                    id = weRow.exerciseId,
                    name = weRow.exerciseName,
                    primaryMuscle = primaryMuscle,
                    equipment = equipment,
                    movementType = movementType,
                    pinnedNote = weRow.pinnedNote ?: ""
                )

                WorkoutExercise(
                    id = weRow.id,
                    exercise = exModel,
                    sets = sets,
                    notes = weRow.pinnedNote ?: ""
                )
            }

            Workout(
                id = wRow.id,
                title = wRow.title,
                splitType = wRow.splitType,
                startTimeEpochMs = wRow.startTimeEpochMs,
                endTimeEpochMs = wRow.endTimeEpochMs,
                durationSeconds = wRow.durationSeconds,
                exercises = exercises,
                sessionRpe = wRow.sessionRpe,
                notes = wRow.notes ?: "",
                isFinished = wRow.isFinished == 1L,
                avgHeartRateBpm = wRow.avgHeartRateBpm?.toInt(),
                peakHeartRateBpm = wRow.peakHeartRateBpm?.toInt(),
                activeCaloriesKcal = wRow.activeCaloriesKcal
            )
        }
    }

    fun saveCompletedWorkout(workout: Workout) {
        database.transaction {
            queries.insertWorkout(
                id = workout.id,
                title = workout.title,
                splitType = workout.splitType,
                startTimeEpochMs = workout.startTimeEpochMs,
                endTimeEpochMs = workout.endTimeEpochMs ?: currentEpochMillis(),
                durationSeconds = workout.durationSeconds,
                sessionRpe = workout.sessionRpe,
                notes = workout.notes,
                isFinished = 1L,
                avgHeartRateBpm = workout.avgHeartRateBpm?.toLong(),
                peakHeartRateBpm = workout.peakHeartRateBpm?.toLong(),
                activeCaloriesKcal = workout.activeCaloriesKcal
            )

            // Remove existing associations if updating
            queries.deleteSetsByWorkoutId(workout.id)
            queries.deleteWorkoutExercisesByWorkoutId(workout.id)

            workout.exercises.forEachIndexed { orderIndex, we ->
                queries.insertWorkoutExercise(
                    id = we.id,
                    workoutId = workout.id,
                    exerciseId = we.exercise.id,
                    exerciseName = we.exercise.name,
                    primaryMuscle = we.exercise.primaryMuscle.name,
                    equipment = we.exercise.equipment.name,
                    movementType = we.exercise.movementType.name,
                    orderIndex = orderIndex.toLong(),
                    pinnedNote = we.notes.ifBlank { we.exercise.pinnedNote }
                )

                we.sets.forEach { set ->
                    queries.insertSet(
                        id = set.id,
                        workoutExerciseId = we.id,
                        workoutId = workout.id,
                        setNumber = set.setNumber.toLong(),
                        setType = set.setType.name,
                        weightKg = set.weightKg,
                        reps = set.reps.toLong(),
                        isCompleted = if (set.isCompleted) 1L else 0L,
                        rpe = set.rpe,
                        rir = null,
                        targetWeightKg = set.targetWeightKg,
                        targetReps = set.targetReps?.toLong(),
                        previousWeightKg = set.previousWeightKg,
                        previousReps = set.previousReps?.toLong(),
                        heartRateBpm = set.heartRateBpm?.toLong(),
                        completedAtEpochMs = if (set.isCompleted) currentEpochMillis() else null
                    )
                }
            }
        }
    }

    fun deleteWorkout(workoutId: String) {
        database.transaction {
            queries.deleteSetsByWorkoutId(workoutId)
            queries.deleteWorkoutExercisesByWorkoutId(workoutId)
            queries.deleteWorkout(workoutId)
        }
    }

    // ==========================================
    // ACTIVE WORKOUT PERSISTENCE
    // ==========================================

    fun saveActiveWorkout(workout: Workout?) {
        if (workout == null) {
            queries.clearActiveWorkout()
        } else {
            val jsonStr = json.encodeToString(workout)
            queries.insertActiveWorkout(jsonStr, currentEpochMillis())
        }
    }

    fun loadActiveWorkout(staleWorkoutHours: Int): Workout? {
        val row = queries.selectActiveWorkout().executeAsOneOrNull() ?: return null
        val now = currentEpochMillis()
        val maxAgeMs = staleWorkoutHours * 3600000L
        if (now - row.lastUpdatedEpochMs > maxAgeMs) {
            queries.clearActiveWorkout()
            return null
        }
        return try {
            json.decodeFromString<Workout>(row.workoutJson)
        } catch (_: Exception) {
            queries.clearActiveWorkout()
            null
        }
    }

    fun clearActiveWorkout() {
        queries.clearActiveWorkout()
    }

    // ==========================================
    // PERSONAL RECORDS
    // ==========================================

    fun getAllPersonalRecords(): List<PersonalRecord> {
        return queries.selectAllPrs().executeAsList().map { row ->
            PersonalRecord(
                exerciseId = row.exerciseId,
                exerciseName = row.exerciseName,
                weightKg = row.weightKg,
                reps = row.reps.toInt(),
                estimated1RmKg = row.estimated1RmKg,
                dateAchieved = formatDateEpoch(row.dateAchievedEpochMs),
                deltaWeightKg = row.deltaWeightKg,
                prTier = row.prTier,
                dateAchievedEpochMs = row.dateAchievedEpochMs,
                sourceSetId = row.sourceSetId
            )
        }
    }

    fun savePersonalRecord(pr: PersonalRecord) {
        val timestamp = if (pr.dateAchievedEpochMs > 0L) pr.dateAchievedEpochMs else currentEpochMillis()
        queries.insertPr(
            id = IdGenerator.prId(pr.exerciseId),
            exerciseId = pr.exerciseId,
            exerciseName = pr.exerciseName,
            weightKg = pr.weightKg,
            reps = pr.reps.toLong(),
            estimated1RmKg = pr.estimated1RmKg,
            dateAchievedEpochMs = timestamp,
            deltaWeightKg = pr.deltaWeightKg,
            prTier = pr.prTier,
            sourceSetId = pr.sourceSetId
        )
    }

    fun deletePersonalRecordBySourceSet(sourceSetId: String) {
        queries.deletePrBySourceSet(sourceSetId)
    }

    private fun formatDateEpoch(epochMs: Long): String {
        if (epochMs <= 0L) return "Today"
        val instant = kotlinx.datetime.Instant.fromEpochMilliseconds(epochMs)
        val dt = instant.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
        val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val month = monthNames.getOrElse(dt.monthNumber - 1) { "Jan" }
        return "$month ${dt.dayOfMonth}"
    }

    // ==========================================
    // CUSTOM EXERCISES
    // ==========================================

    fun getAllCustomExercises(): List<Exercise> {
        return queries.selectAllCustomExercises().executeAsList().map { row ->
            val pm = try { MuscleGroup.valueOf(row.primaryMuscle) } catch (_: Exception) { MuscleGroup.CHEST }
            val eq = try { Equipment.valueOf(row.equipment) } catch (_: Exception) { Equipment.BARBELL }
            val mt = try { MovementType.valueOf(row.movementType) } catch (_: Exception) { MovementType.COMPOUND }

            val secMuscles = if (row.secondaryMuscles.isNotBlank()) {
                row.secondaryMuscles.split(",").mapNotNull { name ->
                    try { MuscleGroup.valueOf(name.trim()) } catch (_: Exception) { null }
                }
            } else emptyList()

            Exercise(
                id = row.id,
                name = row.name,
                primaryMuscle = pm,
                secondaryMuscles = secMuscles,
                secondaryMuscleDetails = secMuscles.joinToString(", ") { it.displayName },
                equipment = eq,
                equipmentDetail = eq.displayName,
                movementType = mt,
                defaultIncrementKg = row.defaultIncrementKg,
                pinnedNote = row.pinnedNote ?: ""
            )
        }
    }

    fun saveCustomExercise(exercise: Exercise) {
        queries.insertCustomExercise(
            id = exercise.id,
            name = exercise.name,
            primaryMuscle = exercise.primaryMuscle.name,
            secondaryMuscles = exercise.secondaryMuscles.joinToString(",") { it.name },
            equipment = exercise.equipment.name,
            movementType = exercise.movementType.name,
            defaultIncrementKg = exercise.defaultIncrementKg,
            pinnedNote = exercise.pinnedNote,
            createdAtEpochMs = currentEpochMillis()
        )
    }

    fun deleteCustomExercise(id: String) {
        queries.deleteCustomExercise(id)
    }

    // ==========================================
    // ROUTINES
    // ==========================================

    fun getAllCustomRoutines(exerciseLookup: (String) -> Exercise?): List<RoutineBlueprint> {
        val routineRows = queries.selectAllRoutines().executeAsList()
        return routineRows.map { rRow ->
            val reRows = queries.selectRoutineExercises(rRow.id).executeAsList()
            val exercises = reRows.mapNotNull { reRow ->
                exerciseLookup(reRow.exerciseId)
            }

            RoutineBlueprint(
                id = rRow.id,
                title = rRow.title,
                splitCategory = rRow.splitCategory,
                muscleFocus = rRow.muscleFocus,
                estimatedDurationMinutes = rRow.estimatedDurationMinutes.toInt(),
                targetRpeTier = rRow.targetRpeTier,
                exerciseTemplates = exercises,
                plannedSetsPerExercise = rRow.plannedSetsPerExercise.toInt()
            )
        }
    }

    fun saveCustomRoutine(routine: RoutineBlueprint) {
        database.transaction {
            queries.insertRoutine(
                id = routine.id,
                title = routine.title,
                splitCategory = routine.splitCategory,
                muscleFocus = routine.muscleFocus,
                estimatedDurationMinutes = routine.estimatedDurationMinutes.toLong(),
                targetRpeTier = routine.targetRpeTier,
                plannedSetsPerExercise = routine.plannedSetsPerExercise.toLong(),
                isCustom = 1L,
                createdAtEpochMs = currentEpochMillis()
            )

            queries.deleteRoutineExercises(routine.id)
            routine.exerciseTemplates.forEachIndexed { orderIndex, ex ->
                queries.insertRoutineExercise(
                    id = "${routine.id}_${ex.id}_$orderIndex",
                    routineId = routine.id,
                    exerciseId = ex.id,
                    orderIndex = orderIndex.toLong(),
                    targetSets = routine.plannedSetsPerExercise.toLong(),
                    targetReps = 8L,
                    targetWeightKg = null
                )
            }
        }
    }

    fun deleteCustomRoutine(id: String) {
        database.transaction {
            queries.deleteRoutineExercises(id)
            queries.deleteRoutine(id)
        }
    }

    // ==========================================
    // USER SETTINGS
    // ==========================================

    fun loadUserSettings(): UserSettings? {
        val row = queries.selectUserSettings().executeAsOneOrNull() ?: return null
        return try {
            json.decodeFromString<UserSettings>(row.settingsJson)
        } catch (_: Exception) {
            null
        }
    }

    fun saveUserSettings(settings: UserSettings) {
        val jsonStr = json.encodeToString(settings)
        queries.insertUserSettings(jsonStr, settings.settingsVersion.toLong(), currentEpochMillis())
    }

    // ==========================================
    // BACKUP, SAFETY SNAPSHOT & DANGER ZONE
    // ==========================================

    fun saveSafetySnapshot(payloadJson: String) {
        val id = IdGenerator.nextId("snap")
        queries.insertSafetySnapshot(id, currentEpochMillis(), payloadJson)
    }

    fun getLatestSafetySnapshot(): String? {
        return queries.selectLatestSafetySnapshot().executeAsOneOrNull()
    }

    fun wipeAllUserData() {
        database.transaction {
            queries.clearAllSets()
            queries.clearAllWorkoutExercises()
            queries.clearAllWorkouts()
            queries.clearAllPersonalRecords()
            queries.clearAllRoutineExercises()
            queries.clearAllRoutines()
            queries.clearAllCustomExercises()
            queries.clearAllBodyweightLog()
            queries.clearActiveWorkout()
        }
    }

    fun clearWorkoutHistoryOnly() {
        database.transaction {
            queries.clearAllSets()
            queries.clearAllWorkoutExercises()
            queries.clearAllWorkouts()
            queries.clearActiveWorkout()
        }
    }
}
