package com.bearbones.kumalift.data.local

import com.bearbones.kumalift.core.util.IdGenerator
import com.bearbones.kumalift.core.util.currentEpochMillis
import com.bearbones.kumalift.database.KumaDatabase
import com.bearbones.kumalift.domain.ai.AiActionProposal
import com.bearbones.kumalift.domain.ai.AiChatMessage
import com.bearbones.kumalift.domain.ai.AiChatRole
import com.bearbones.kumalift.domain.ai.AiNutritionCardData
import com.bearbones.kumalift.domain.model.Equipment
import com.bearbones.kumalift.domain.model.Exercise
import com.bearbones.kumalift.domain.model.MovementType
import com.bearbones.kumalift.domain.model.MuscleGroup
import com.bearbones.kumalift.domain.model.PersonalRecord
import com.bearbones.kumalift.domain.model.RoutineBlueprint
import com.bearbones.kumalift.domain.model.RoutineExerciseConfig
import com.bearbones.kumalift.domain.model.RoutineSetConfig
import com.bearbones.kumalift.domain.model.SetLog
import com.bearbones.kumalift.domain.model.SetType
import com.bearbones.kumalift.domain.model.TrainingGoal
import com.bearbones.kumalift.domain.model.TrainingProgram
import com.bearbones.kumalift.domain.model.UserSettings
import com.bearbones.kumalift.domain.model.Workout
import com.bearbones.kumalift.domain.model.WorkoutExercise
import com.bearbones.kumalift.domain.model.WorkoutScheduleEvent
import com.bearbones.kumalift.domain.model.ScheduleStatus
import com.bearbones.kumalift.domain.model.MesocycleBlock
import com.bearbones.kumalift.domain.model.MesocycleStatus
import com.bearbones.kumalift.domain.model.DailyReadinessCheckIn
import com.bearbones.kumalift.domain.model.ReadinessRecommendation
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

                val (extractedSsId, cleanNote) = extractSupersetGroup(weRow.pinnedNote)
                WorkoutExercise(
                    id = weRow.id,
                    exercise = exModel,
                    sets = sets,
                    notes = cleanNote,
                    supersetGroupId = extractedSsId
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
                    pinnedNote = formatNoteWithSuperset(we.notes.ifBlank { we.exercise.pinnedNote }, we.supersetGroupId)
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
        val prId = "pr_${pr.exerciseId}_${pr.reps}"
        queries.insertPr(
            id = prId,
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

    fun deletePersonalRecordsForExercise(exerciseId: String) {
        queries.deletePrsByExerciseId(exerciseId)
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
                secondaryMuscleDetails = "",
                equipment = eq,
                equipmentDetail = "",
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
            val configs = reRows.mapNotNull { reRow ->
                val ex = exerciseLookup(reRow.exerciseId) ?: return@mapNotNull null
                val ssId = if (reRow.id.contains("__SS_")) reRow.id.substringAfter("__SS_") else null
                val sets: List<RoutineSetConfig> = try {
                    if (reRow.setConfigsJson.isNotBlank()) {
                        json.decodeFromString(reRow.setConfigsJson)
                    } else emptyList()
                } catch (_: Exception) {
                    emptyList()
                }
                RoutineExerciseConfig(
                    exercise = ex,
                    targetSets = reRow.targetSets.toInt(),
                    targetReps = reRow.targetReps.toInt(),
                    targetWeightKg = reRow.targetWeightKg,
                    restTimerSeconds = reRow.restTimerSeconds.toInt(),
                    supersetGroupId = ssId,
                    setConfigs = sets
                )
            }
            val exercises = configs.map { it.exercise }
            val scheduledDays = rRow.scheduledDays.split(",").mapNotNull { it.trim().toIntOrNull() }

            RoutineBlueprint(
                id = rRow.id,
                programId = rRow.programId,
                title = rRow.title,
                splitCategory = rRow.splitCategory,
                muscleFocus = rRow.muscleFocus,
                estimatedDurationMinutes = rRow.estimatedDurationMinutes.toInt(),
                targetRpeTier = rRow.targetRpeTier,
                exerciseTemplates = exercises,
                exerciseConfigs = configs,
                plannedSetsPerExercise = rRow.plannedSetsPerExercise.toInt(),
                scheduledDays = scheduledDays,
                presetKey = rRow.presetKey,
                isUserRenamed = rRow.isUserRenamed == 1L
            )
        }
    }

    fun getRoutinesForProgram(programId: String, exerciseLookup: (String) -> Exercise?): List<RoutineBlueprint> {
        val routineRows = queries.selectRoutinesByProgram(programId).executeAsList()
        return routineRows.map { rRow ->
            val reRows = queries.selectRoutineExercises(rRow.id).executeAsList()
            val configs = reRows.mapNotNull { reRow ->
                val ex = exerciseLookup(reRow.exerciseId) ?: return@mapNotNull null
                val ssId = if (reRow.id.contains("__SS_")) reRow.id.substringAfter("__SS_") else null
                val sets: List<RoutineSetConfig> = try {
                    if (reRow.setConfigsJson.isNotBlank()) {
                        json.decodeFromString(reRow.setConfigsJson)
                    } else emptyList()
                } catch (_: Exception) {
                    emptyList()
                }
                RoutineExerciseConfig(
                    exercise = ex,
                    targetSets = reRow.targetSets.toInt(),
                    targetReps = reRow.targetReps.toInt(),
                    targetWeightKg = reRow.targetWeightKg,
                    restTimerSeconds = reRow.restTimerSeconds.toInt(),
                    supersetGroupId = ssId,
                    setConfigs = sets
                )
            }
            val exercises = configs.map { it.exercise }
            val scheduledDays = rRow.scheduledDays.split(",").mapNotNull { it.trim().toIntOrNull() }

            RoutineBlueprint(
                id = rRow.id,
                programId = rRow.programId,
                title = rRow.title,
                splitCategory = rRow.splitCategory,
                muscleFocus = rRow.muscleFocus,
                estimatedDurationMinutes = rRow.estimatedDurationMinutes.toInt(),
                targetRpeTier = rRow.targetRpeTier,
                exerciseTemplates = exercises,
                exerciseConfigs = configs,
                plannedSetsPerExercise = rRow.plannedSetsPerExercise.toInt(),
                scheduledDays = scheduledDays,
                presetKey = rRow.presetKey,
                isUserRenamed = rRow.isUserRenamed == 1L
            )
        }
    }

    fun saveCustomRoutine(routine: RoutineBlueprint) {
        database.transaction {
            queries.insertRoutine(
                id = routine.id,
                programId = routine.programId,
                title = routine.title,
                splitCategory = routine.splitCategory,
                muscleFocus = routine.muscleFocus,
                estimatedDurationMinutes = routine.estimatedDurationMinutes.toLong(),
                targetRpeTier = routine.targetRpeTier,
                plannedSetsPerExercise = routine.plannedSetsPerExercise.toLong(),
                isCustom = 1L,
                createdAtEpochMs = currentEpochMillis(),
                scheduledDays = routine.scheduledDays.joinToString(","),
                presetKey = routine.presetKey,
                isUserRenamed = if (routine.isUserRenamed) 1L else 0L
            )

            val configs = routine.effectiveConfigs
            queries.deleteRoutineExercises(routine.id)
            configs.forEachIndexed { orderIndex, cfg ->
                val ssSuffix = if (cfg.supersetGroupId != null) "__SS_${cfg.supersetGroupId}" else ""
                val setsJson = try {
                    json.encodeToString(cfg.effectiveSets)
                } catch (_: Exception) {
                    ""
                }
                queries.insertRoutineExercise(
                    id = "${routine.id}_${cfg.exercise.id}_${orderIndex}$ssSuffix",
                    routineId = routine.id,
                    exerciseId = cfg.exercise.id,
                    orderIndex = orderIndex.toLong(),
                    targetSets = cfg.effectiveSets.size.toLong(),
                    targetReps = cfg.targetReps.toLong(),
                    targetWeightKg = cfg.targetWeightKg,
                    restTimerSeconds = cfg.restTimerSeconds.toLong(),
                    setConfigsJson = setsJson
                )
            }
        }
    }

    private fun formatNoteWithSuperset(note: String, supersetGroupId: String?): String {
        return if (supersetGroupId != null) {
            "[SS:$supersetGroupId] $note".trim()
        } else {
            note
        }
    }

    private fun extractSupersetGroup(rawNote: String?): Pair<String?, String> {
        if (rawNote == null) return null to ""
        val regex = Regex("^\\[SS:([a-zA-Z0-9_-]+)\\]\\s*(.*)", RegexOption.DOT_MATCHES_ALL)
        val match = regex.find(rawNote)
        return if (match != null) {
            match.groupValues[1] to match.groupValues[2]
        } else {
            null to rawNote
        }
    }

    fun deleteCustomRoutine(id: String) {
        database.transaction {
            queries.deleteRoutineExercises(id)
            queries.deleteRoutine(id)
        }
    }

    fun deleteCustomRoutinesForProgram(programId: String) {
        database.transaction {
            val routinesInProgram = queries.selectRoutinesByProgram(programId).executeAsList()
            routinesInProgram.forEach { r ->
                queries.deleteRoutineExercises(r.id)
            }
            queries.deleteRoutinesByProgram(programId)
        }
    }

    // ==========================================
    // TRAINING PROGRAMS
    // ==========================================

    private fun parseActiveDays(raw: String): List<Int> {
        val days = raw.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..7 }.distinct().take(3)
        return days.ifEmpty { listOf(1, 3, 5) }
    }

    private fun formatActiveDays(days: List<Int>): String {
        return days.filter { it in 1..7 }.distinct().take(3).sorted().joinToString(",")
    }

    fun getAllTrainingPrograms(): List<TrainingProgram> {
        val rows = queries.selectAllTrainingPrograms().executeAsList()
        return rows.map { row ->
            val goal = try {
                TrainingGoal.valueOf(row.category)
            } catch (_: Exception) {
                TrainingGoal.HYPERTROPHY
            }
            TrainingProgram(
                id = row.id,
                name = row.name,
                description = row.description,
                category = goal,
                targetDaysPerWeek = row.targetDaysPerWeek.toInt(),
                activeDays = parseActiveDays(row.activeDays),
                isDefault = row.isDefault == 1L,
                createdAtEpochMs = row.createdAtEpochMs,
                presetKey = row.presetKey,
                isUserRenamed = row.isUserRenamed == 1L
            )
        }
    }

    fun getTrainingProgramById(id: String): TrainingProgram? {
        val row = queries.selectTrainingProgramById(id).executeAsOneOrNull() ?: return null
        val goal = try {
            TrainingGoal.valueOf(row.category)
        } catch (_: Exception) {
            TrainingGoal.HYPERTROPHY
        }
        return TrainingProgram(
            id = row.id,
            name = row.name,
            description = row.description,
            category = goal,
            targetDaysPerWeek = row.targetDaysPerWeek.toInt(),
            activeDays = parseActiveDays(row.activeDays),
            isDefault = row.isDefault == 1L,
            createdAtEpochMs = row.createdAtEpochMs,
            presetKey = row.presetKey,
            isUserRenamed = row.isUserRenamed == 1L
        )
    }

    fun saveTrainingProgram(program: TrainingProgram) {
        queries.insertTrainingProgram(
            id = program.id,
            name = program.name,
            description = program.description,
            category = program.category.name,
            targetDaysPerWeek = program.targetDaysPerWeek.toLong(),
            activeDays = formatActiveDays(program.activeDays),
            isDefault = if (program.isDefault) 1L else 0L,
            createdAtEpochMs = if (program.createdAtEpochMs > 0L) program.createdAtEpochMs else currentEpochMillis(),
            presetKey = program.presetKey,
            isUserRenamed = if (program.isUserRenamed) 1L else 0L
        )
    }

    fun deleteTrainingProgram(id: String) {
        database.transaction {
            deleteCustomRoutinesForProgram(id)
            queries.deleteTrainingProgram(id)
        }
    }

    fun clearAllTrainingPrograms() {
        queries.clearAllTrainingPrograms()
    }

    // ==========================================
    // USER SETTINGS
    // ==========================================

    fun loadUserSettings(): UserSettings? {
        val row = queries.selectUserSettings().executeAsOneOrNull() ?: return null
        return try {
            val migrated = SettingsMigration.migrateJson(row.settingsJson)
            json.decodeFromString<UserSettings>(migrated)
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
            queries.clearAllTrainingPrograms()
            queries.clearAllWorkoutScheduleEvents()
            queries.clearAllMesocycles()
            queries.clearAllDailyReadiness()
        }
        saveTrainingProgram(TrainingProgram.DEFAULT_HYPERTROPHY)
    }

    fun clearWorkoutHistoryOnly() {
        database.transaction {
            queries.clearAllSets()
            queries.clearAllWorkoutExercises()
            queries.clearAllWorkouts()
            queries.clearActiveWorkout()
        }
    }

    // ==========================================
    // WORKOUT SCHEDULE EVENTS
    // ==========================================

    fun getAllScheduleEvents(): List<WorkoutScheduleEvent> {
        return queries.selectAllScheduleEvents().executeAsList().map { it.toDomain() }
    }

    fun getScheduleEventsByDate(dateString: String): List<WorkoutScheduleEvent> {
        return queries.selectScheduleEventsByDate(dateString).executeAsList().map { it.toDomain() }
    }

    fun getScheduleEventsByDateRange(startDate: String, endDate: String): List<WorkoutScheduleEvent> {
        return queries.selectScheduleEventsByDateRange(startDate, endDate).executeAsList().map { it.toDomain() }
    }

    fun getScheduleEventById(id: String): WorkoutScheduleEvent? {
        return queries.selectScheduleEventById(id).executeAsOneOrNull()?.toDomain()
    }

    fun insertScheduleEvent(event: WorkoutScheduleEvent) {
        queries.insertScheduleEvent(
            id = event.id,
            routineId = event.routineId,
            scheduledDate = event.scheduledDate,
            status = event.status.name,
            linkedWorkoutId = event.linkedWorkoutId,
            routineTitleSnapshot = event.routineTitleSnapshot,
            splitTypeSnapshot = event.splitTypeSnapshot,
            notes = event.notes,
            createdAtEpochMs = event.createdAtEpochMs,
            updatedAtEpochMs = event.updatedAtEpochMs
        )
    }

    fun updateScheduleEventDate(id: String, newDate: String, updatedAtEpochMs: Long = currentEpochMillis()) {
        queries.updateScheduleEventDate(
            scheduledDate = newDate,
            updatedAtEpochMs = updatedAtEpochMs,
            id = id
        )
    }

    fun updateScheduleEventStatus(
        id: String,
        status: ScheduleStatus,
        linkedWorkoutId: String? = null,
        updatedAtEpochMs: Long = currentEpochMillis()
    ) {
        queries.updateScheduleEventStatus(
            status = status.name,
            linkedWorkoutId = linkedWorkoutId,
            updatedAtEpochMs = updatedAtEpochMs,
            id = id
        )
    }

    fun deleteScheduleEvent(id: String) {
        queries.deleteScheduleEvent(id)
    }

    fun clearAllWorkoutScheduleEvents() {
        queries.clearAllWorkoutScheduleEvents()
    }

    private fun com.bearbones.kumalift.database.Workout_schedule_events.toDomain(): WorkoutScheduleEvent {
        return WorkoutScheduleEvent(
            id = id,
            routineId = routineId,
            scheduledDate = scheduledDate,
            status = try { ScheduleStatus.valueOf(status) } catch (_: Exception) { ScheduleStatus.PLANNED },
            linkedWorkoutId = linkedWorkoutId,
            routineTitleSnapshot = routineTitleSnapshot,
            splitTypeSnapshot = splitTypeSnapshot,
            notes = notes,
            createdAtEpochMs = createdAtEpochMs,
            updatedAtEpochMs = updatedAtEpochMs
        )
    }

    // ==========================================
    // MESOCYCLES
    // ==========================================

    fun getAllMesocycles(): List<MesocycleBlock> {
        return queries.selectAllMesocycles().executeAsList().map { it.toDomain() }
    }

    fun getActiveMesocycle(): MesocycleBlock? {
        return queries.selectActiveMesocycle().executeAsOneOrNull()?.toDomain()
    }

    fun getMesocycleById(id: String): MesocycleBlock? {
        return queries.selectMesocycleById(id).executeAsOneOrNull()?.toDomain()
    }

    fun insertOrReplaceMesocycle(block: MesocycleBlock) {
        queries.insertOrReplaceMesocycle(
            id = block.id,
            name = block.name,
            programId = block.programId,
            totalWeeks = block.totalWeeks.toLong(),
            currentWeek = block.currentWeek.toLong(),
            deloadWeek = block.deloadWeek.toLong(),
            isDeloadActive = if (block.isDeloadActive) 1L else 0L,
            startDateEpochMs = block.startDateEpochMs,
            endDateEpochMs = block.endDateEpochMs,
            status = block.status.name,
            notes = block.notes,
            createdAtEpochMs = if (block.createdAtEpochMs > 0L) block.createdAtEpochMs else currentEpochMillis()
        )
    }

    fun updateMesocycleWeek(id: String, currentWeek: Int, isDeloadActive: Boolean) {
        queries.updateMesocycleWeek(
            currentWeek = currentWeek.toLong(),
            isDeloadActive = if (isDeloadActive) 1L else 0L,
            id = id
        )
    }

    fun updateMesocycleStatus(id: String, status: MesocycleStatus, endDateEpochMs: Long?) {
        queries.updateMesocycleStatus(
            status = status.name,
            endDateEpochMs = endDateEpochMs,
            id = id
        )
    }

    fun deleteMesocycle(id: String) {
        queries.deleteMesocycle(id)
    }

    fun clearAllMesocycles() {
        queries.clearAllMesocycles()
    }

    fun getAllDailyReadiness(): List<DailyReadinessCheckIn> {
        return queries.selectAllReadinessCheckIns().executeAsList().map { it.toDomain() }
    }

    fun getDailyReadinessByDate(dateString: String): DailyReadinessCheckIn? {
        return queries.selectReadinessByDate(dateString).executeAsOneOrNull()?.toDomain()
    }

    fun insertOrReplaceDailyReadiness(checkIn: DailyReadinessCheckIn) {
        queries.insertOrReplaceReadiness(
            id = checkIn.id,
            dateString = checkIn.dateString,
            sleepScore = checkIn.sleepScore.toLong(),
            sorenessScore = checkIn.sorenessScore.toLong(),
            energyScore = checkIn.energyScore.toLong(),
            stressScore = checkIn.stressScore.toLong(),
            overallPercentage = checkIn.overallPercentage.toLong(),
            recommendation = checkIn.recommendation.name,
            notes = checkIn.notes,
            createdAtEpochMs = if (checkIn.createdAtEpochMs > 0L) checkIn.createdAtEpochMs else currentEpochMillis()
        )
    }

    fun deleteDailyReadinessById(id: String) {
        queries.deleteReadinessById(id)
    }

    fun clearAllDailyReadiness() {
        queries.clearAllDailyReadiness()
    }

    private fun com.bearbones.kumalift.database.Daily_readiness.toDomain(): DailyReadinessCheckIn {
        return DailyReadinessCheckIn(
            id = id,
            dateString = dateString,
            sleepScore = sleepScore.toInt(),
            sorenessScore = sorenessScore.toInt(),
            energyScore = energyScore.toInt(),
            stressScore = stressScore.toInt(),
            overallPercentage = overallPercentage.toInt(),
            recommendation = try { ReadinessRecommendation.valueOf(recommendation) } catch (_: Exception) { ReadinessRecommendation.MODERATE_LOAD },
            notes = notes,
            createdAtEpochMs = createdAtEpochMs
        )
    }

    private fun com.bearbones.kumalift.database.Mesocycles.toDomain(): MesocycleBlock {
        return MesocycleBlock(
            id = id,
            name = name,
            programId = programId,
            totalWeeks = totalWeeks.toInt(),
            currentWeek = currentWeek.toInt(),
            deloadWeek = deloadWeek.toInt(),
            isDeloadActive = isDeloadActive == 1L,
            startDateEpochMs = startDateEpochMs,
            endDateEpochMs = endDateEpochMs,
            status = try { MesocycleStatus.valueOf(status) } catch (_: Exception) { MesocycleStatus.ACTIVE },
            notes = notes,
            createdAtEpochMs = createdAtEpochMs
        )
    }

    // ==========================================
    // KUMA AI CHAT MESSAGES
    // ==========================================

    fun getAllAiChatMessages(): List<AiChatMessage> {
        return queries.selectAllAiChatMessages().executeAsList().map { row ->
            val steps = try {
                json.decodeFromString<List<String>>(row.stepLogsJson)
            } catch (_: Exception) {
                emptyList()
            }
            val proposal = row.proposalJson?.takeIf { it.isNotBlank() }?.let { raw ->
                try { json.decodeFromString<AiActionProposal>(raw) } catch (_: Exception) { null }
            }
            val nutrition = row.nutritionJson?.takeIf { it.isNotBlank() }?.let { raw ->
                try { json.decodeFromString<AiNutritionCardData>(raw) } catch (_: Exception) { null }
            }
            AiChatMessage(
                id = row.id,
                role = try { AiChatRole.valueOf(row.role) } catch (_: Exception) { AiChatRole.ASSISTANT },
                text = row.messageText,
                imagePath = row.imagePath,
                stepLogs = steps,
                proposal = proposal,
                nutritionCard = nutrition,
                isError = row.isError == 1L,
                createdAtEpochMs = row.createdAtEpochMs
            )
        }
    }

    fun saveAiChatMessage(message: AiChatMessage) {
        queries.insertOrReplaceAiChatMessage(
            id = message.id,
            role = message.role.name,
            messageText = message.text,
            imagePath = message.imagePath,
            stepLogsJson = json.encodeToString(message.stepLogs),
            proposalJson = message.proposal?.let { json.encodeToString(it) },
            nutritionJson = message.nutritionCard?.let { json.encodeToString(it) },
            isError = if (message.isError) 1L else 0L,
            createdAtEpochMs = if (message.createdAtEpochMs > 0L) message.createdAtEpochMs else currentEpochMillis()
        )
    }

    fun clearAllAiChatMessages() {
        queries.clearAllAiChatMessages()
    }
}
