package com.bearbones.kumalift.domain.ai

import com.bearbones.kumalift.core.localization.AppStrings
import com.bearbones.kumalift.core.util.IdGenerator
import com.bearbones.kumalift.core.util.currentEpochMillis
import com.bearbones.kumalift.data.local.KumaDatabaseRepository
import com.bearbones.kumalift.data.repository.DailyReadinessRepository
import com.bearbones.kumalift.data.repository.ExerciseRepository
import com.bearbones.kumalift.data.repository.MesocycleRepository
import com.bearbones.kumalift.data.repository.PersonalRecordRepository
import com.bearbones.kumalift.data.repository.RoutineRepository
import com.bearbones.kumalift.data.repository.SettingsRepository
import com.bearbones.kumalift.data.repository.TrainingProgramRepository
import com.bearbones.kumalift.data.repository.WorkoutRepository
import com.bearbones.kumalift.data.repository.WorkoutScheduleRepository
import com.bearbones.kumalift.domain.model.Equipment
import com.bearbones.kumalift.domain.model.Exercise
import com.bearbones.kumalift.domain.model.MuscleGroup
import com.bearbones.kumalift.domain.model.MuscleRecoveryTracker
import com.bearbones.kumalift.domain.model.MuscleVolumeCalculator
import com.bearbones.kumalift.domain.model.RoutineBlueprint
import com.bearbones.kumalift.domain.model.RoutineExerciseConfig
import com.bearbones.kumalift.domain.model.RoutineSetConfig
import com.bearbones.kumalift.domain.model.RpeTier
import com.bearbones.kumalift.domain.model.TrainingGoal
import com.bearbones.kumalift.domain.model.TrainingProgram
import com.bearbones.kumalift.domain.model.Workout
import com.bearbones.kumalift.domain.model.localizedTitle
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

@Serializable
private data class StageRoutineSetDto(
    val reps: Int = 10,
    val weightKg: Double? = null,
    val restTimerSeconds: Int? = null
)

@Serializable
private data class StageRoutineExerciseDto(
    val exerciseId: String,
    val targetSets: Int = 3,
    val targetReps: Int = 10,
    val targetWeightKg: Double? = null,
    val restTimerSeconds: Int = 75,
    val setConfigs: List<StageRoutineSetDto> = emptyList()
)

@Serializable
private data class StageCreateRoutinePayload(
    val title: String,
    val splitCategory: String = "Custom",
    val muscleFocus: String = "",
    val estimatedDurationMinutes: Int = 45,
    val targetRpeTier: String = "moderate",
    val scheduledDays: List<Int> = emptyList(),
    val exercises: List<StageRoutineExerciseDto>
)

@Serializable
private data class StageUpdateRoutinePayload(
    val routineId: String,
    val title: String? = null,
    val splitCategory: String? = null,
    val muscleFocus: String? = null,
    val estimatedDurationMinutes: Int? = null,
    val scheduledDays: List<Int>? = null,
    val exercises: List<StageRoutineExerciseDto>? = null
)

@Serializable
private data class StageIdPayload(
    val id: String
)

@Serializable
private data class StageScheduleWorkoutPayload(
    val routineId: String,
    val scheduledDate: String,
    val notes: String? = null
)

@Serializable
private data class StageCancelOrDeleteSchedulePayload(
    val eventId: String,
    val deletePermanently: Boolean = false
)

@Serializable
private data class StageStartMesocyclePayload(
    val name: String,
    val totalWeeks: Int = 5,
    val deloadWeek: Int = 5,
    val notes: String = ""
)

@Serializable
private data class StageLogBodyweightPayload(
    val weightKg: Double
)

@Serializable
private data class StageUpdateGoalPayload(
    val goal: TrainingGoal
)

@Serializable
private data class StageTextPayload(
    val text: String
)

class KumaAiToolSessionState {
    val stagedOperations = mutableListOf<AiProposedOperation>()
    var nutritionCard: AiNutritionCardData? = null
}

internal data class AiTodayContext(
    val isoDate: String,
    val yesterdayDate: String,
    val isoDayNumber: Int,
    val dayNameLocalized: String,
    val dayNameEn: String,
    val hhMm: String
)

private val EN_WEEKDAY_NAMES = listOf(
    "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
)

internal fun buildAiTodayContext(nowDt: LocalDateTime, strings: AppStrings): AiTodayContext {
    val isoDay = nowDt.dayOfWeek.ordinal + 1
    val dayEn = EN_WEEKDAY_NAMES.getOrElse(isoDay - 1) { "Monday" }
    val dayLocal = strings.dayNamesFull.getOrElse(isoDay - 1) { dayEn }
    val hhMm = "${nowDt.hour.toString().padStart(2, '0')}:${nowDt.minute.toString().padStart(2, '0')}"
    val yesterday = nowDt.date.minus(DatePeriod(days = 1))
    return AiTodayContext(
        isoDate = nowDt.date.toString(),
        yesterdayDate = yesterday.toString(),
        isoDayNumber = isoDay,
        dayNameLocalized = dayLocal,
        dayNameEn = dayEn,
        hhMm = hhMm
    )
}

class KumaAiToolExecutor(
    private val exerciseRepository: ExerciseRepository,
    private val routineRepository: RoutineRepository,
    private val trainingProgramRepository: TrainingProgramRepository,
    private val workoutScheduleRepository: WorkoutScheduleRepository,
    private val mesocycleRepository: MesocycleRepository,
    private val dailyReadinessRepository: DailyReadinessRepository,
    private val personalRecordRepository: PersonalRecordRepository,
    private val workoutRepository: WorkoutRepository,
    private val settingsRepository: SettingsRepository,
    private val databaseRepo: KumaDatabaseRepository? = null,
    private val nowProvider: () -> LocalDateTime = {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    }
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    fun functionDeclarations(): JsonArray = buildJsonArray {
        add(fnDecl("get_user_overview", "Read user training goal, authoritative todayDate + yesterdayDate + todayDayOfWeek + todayIsoDayNumber, active program, todayScheduledWorkouts matching Home screen's Today's Schedule, todayCalendarEvents, mesocycle, and today's readiness."))
        add(fnDecl(
            name = "search_exercises",
            toolDoc = "Search KumaLift Exercise Library by name, muscleGroup, or equipment. Always call this before creating or updating routines to get real exerciseId values.",
            props = {
                putJsonObject("query") { put("type", "STRING"); put("description", "Optional exercise name filter") }
                putJsonObject("muscleGroup") { put("type", "STRING"); put("description", "Optional enum: CHEST, BACK, SHOULDERS, BICEPS, TRICEPS, QUADS, HAMSTRINGS, GLUTES, CALVES, CORE, FOREARMS, TRAPS, CARDIO, FULL_BODY") }
                putJsonObject("equipment") { put("type", "STRING"); put("description", "Optional enum: BARBELL, DUMBBELL, CABLE, MACHINE, BODYWEIGHT, KETTLEBELL, RESISTANCE_BAND, SMITH_MACHINE, OTHER") }
                putJsonObject("limit") { put("type", "INTEGER"); put("description", "Max results (default 30)") }
            }
        ))
        add(fnDecl("get_routines", "List all saved user routines with localized title, id, splitCategory, scheduledDays (1=Mon..7=Sun), isScheduledToday flag, and full exercise configs including targetSets, targetReps, targetWeightKg, and per-set setConfigs."))
        add(fnDecl("get_schedule_events", "List today's authoritative date/day, weekly routines scheduled for today, plus planned, completed, and cancelled calendar events."))
        add(fnDecl(
            name = "get_recent_workouts",
            toolDoc = "Read recent completed workout history sorted newest-first, with exact ISO date, dayOfWeek, time, relativeDay (TODAY/YESTERDAY), completed exercises, reps, and weights (kg).",
            props = {
                putJsonObject("limit") { put("type", "INTEGER"); put("description", "Max workouts to return (default 5)") }
            }
        ))
        add(fnDecl("get_personal_records", "List user personal records (weight, reps, estimated 1RM)."))
        add(fnDecl("get_muscle_recovery_and_volume", "Compute current muscle recovery percentages and 7-day volume landmark status per muscle group."))
        add(fnDecl("get_active_workout", "Inspect the currently active workout session if one is in progress."))

        add(fnDecl(
            name = "present_nutrition_analysis",
            toolDoc = "Display a structured nutrition breakdown card to the user after analyzing a food, drink, or nutrition label photo.",
            props = {
                putJsonObject("foodName") { put("type", "STRING") }
                putJsonObject("caloriesKcal") { put("type", "INTEGER") }
                putJsonObject("proteinGrams") { put("type", "INTEGER") }
                putJsonObject("carbsGrams") { put("type", "INTEGER") }
                putJsonObject("fatGrams") { put("type", "INTEGER") }
                putJsonObject("summary") { put("type", "STRING") }
            },
            required = listOf("foodName", "caloriesKcal", "proteinGrams", "carbsGrams", "fatGrams", "summary")
        ))

        add(fnDecl(
            name = "stage_create_routine",
            toolDoc = "Stage creating a BRAND NEW routine for user approval. NEVER use this when editing or updating an existing routine (use stage_update_routine with its routineId instead). Preserve targetWeightKg and per-set setConfigs if specified or visible in screenshots.",
            props = {
                putJsonObject("title") { put("type", "STRING") }
                putJsonObject("splitCategory") { put("type", "STRING"); put("description", "e.g., Push, Pull, Legs, Upper / Lower, Full Body, Custom") }
                putJsonObject("muscleFocus") { put("type", "STRING") }
                putJsonObject("estimatedDurationMinutes") { put("type", "INTEGER") }
                putJsonObject("targetRpeTier") { put("type", "STRING"); put("description", "light, moderate, heavy, or maximal") }
                putJsonObject("scheduledDays") {
                    put("type", "ARRAY")
                    putJsonObject("items") { put("type", "INTEGER") }
                    put("description", "Optional ISO day numbers 1=Mon..7=Sun")
                }
                putJsonObject("exercises") {
                    put("type", "ARRAY")
                    putJsonObject("items") {
                        put("type", "OBJECT")
                        putJsonObject("properties") {
                            putJsonObject("exerciseId") { put("type", "STRING") }
                            putJsonObject("targetSets") { put("type", "INTEGER") }
                            putJsonObject("targetReps") { put("type", "INTEGER") }
                            putJsonObject("targetWeightKg") { put("type", "NUMBER"); put("description", "Optional target load in kg") }
                            putJsonObject("restTimerSeconds") { put("type", "INTEGER") }
                            putJsonObject("setConfigs") {
                                put("type", "ARRAY")
                                put("description", "Optional per-set reps and weightKg when sets have different reps/loads")
                                putJsonObject("items") {
                                    put("type", "OBJECT")
                                    putJsonObject("properties") {
                                        putJsonObject("reps") { put("type", "INTEGER") }
                                        putJsonObject("weightKg") { put("type", "NUMBER") }
                                        putJsonObject("restTimerSeconds") { put("type", "INTEGER") }
                                    }
                                }
                            }
                        }
                        putJsonArray("required") { add(JsonPrimitive("exerciseId")) }
                    }
                }
            },
            required = listOf("title", "exercises")
        ))

        add(fnDecl(
            name = "stage_update_routine",
            toolDoc = "Stage updating an EXISTING routine IN PLACE (keeping its routineId) for user approval. Always prefer this over stage_create_routine whenever the user wants to edit, sync, or modify an existing routine. Preserve targetWeightKg and per-set setConfigs when specified or shown in screenshots.",
            props = {
                putJsonObject("routineId") { put("type", "STRING") }
                putJsonObject("title") { put("type", "STRING") }
                putJsonObject("splitCategory") { put("type", "STRING") }
                putJsonObject("muscleFocus") { put("type", "STRING") }
                putJsonObject("estimatedDurationMinutes") { put("type", "INTEGER") }
                putJsonObject("scheduledDays") {
                    put("type", "ARRAY")
                    putJsonObject("items") { put("type", "INTEGER") }
                }
                putJsonObject("exercises") {
                    put("type", "ARRAY")
                    putJsonObject("items") {
                        put("type", "OBJECT")
                        putJsonObject("properties") {
                            putJsonObject("exerciseId") { put("type", "STRING") }
                            putJsonObject("targetSets") { put("type", "INTEGER") }
                            putJsonObject("targetReps") { put("type", "INTEGER") }
                            putJsonObject("targetWeightKg") { put("type", "NUMBER") }
                            putJsonObject("restTimerSeconds") { put("type", "INTEGER") }
                            putJsonObject("setConfigs") {
                                put("type", "ARRAY")
                                putJsonObject("items") {
                                    put("type", "OBJECT")
                                    putJsonObject("properties") {
                                        putJsonObject("reps") { put("type", "INTEGER") }
                                        putJsonObject("weightKg") { put("type", "NUMBER") }
                                        putJsonObject("restTimerSeconds") { put("type", "INTEGER") }
                                    }
                                }
                            }
                        }
                        putJsonArray("required") { add(JsonPrimitive("exerciseId")) }
                    }
                }
            },
            required = listOf("routineId")
        ))

        add(fnDecl(
            name = "stage_delete_routine",
            toolDoc = "Stage deleting an existing routine for user approval.",
            props = {
                putJsonObject("routineId") { put("type", "STRING") }
            },
            required = listOf("routineId")
        ))

        add(fnDecl(
            name = "stage_schedule_workout",
            toolDoc = "Stage scheduling a routine on a specific YYYY-MM-DD date for user approval.",
            props = {
                putJsonObject("routineId") { put("type", "STRING") }
                putJsonObject("scheduledDate") { put("type", "STRING"); put("description", "ISO date YYYY-MM-DD") }
                putJsonObject("notes") { put("type", "STRING") }
            },
            required = listOf("routineId", "scheduledDate")
        ))

        add(fnDecl(
            name = "stage_cancel_or_delete_schedule",
            toolDoc = "Stage cancelling or deleting a scheduled workout event for user approval.",
            props = {
                putJsonObject("eventId") { put("type", "STRING") }
                putJsonObject("deletePermanently") { put("type", "BOOLEAN") }
            },
            required = listOf("eventId")
        ))

        add(fnDecl(
            name = "stage_start_mesocycle",
            toolDoc = "Stage starting a new multi-week mesocycle block for user approval.",
            props = {
                putJsonObject("name") { put("type", "STRING") }
                putJsonObject("totalWeeks") { put("type", "INTEGER") }
                putJsonObject("deloadWeek") { put("type", "INTEGER") }
                putJsonObject("notes") { put("type", "STRING") }
            },
            required = listOf("name")
        ))

        add(fnDecl(
            name = "stage_log_bodyweight",
            toolDoc = "Stage updating user bodyweight (kg) in profile & log for user approval.",
            props = {
                putJsonObject("weightKg") { put("type", "NUMBER") }
            },
            required = listOf("weightKg")
        ))

        add(fnDecl(
            name = "stage_update_training_goal",
            toolDoc = "Stage changing user training goal (HYPERTROPHY, STRENGTH, ENDURANCE, FAT_LOSS, GENERAL) for user approval.",
            props = {
                putJsonObject("goal") { put("type", "STRING") }
            },
            required = listOf("goal")
        ))

        add(fnDecl(
            name = "stage_add_exercise_to_active_workout",
            toolDoc = "Stage adding an exercise from Exercise Library to the currently active workout session.",
            props = {
                putJsonObject("exerciseId") { put("type", "STRING") }
            },
            required = listOf("exerciseId")
        ))

        add(fnDecl(
            name = "stage_update_ai_preferences",
            toolDoc = "Stage updating long-term AI coaching constraints/preferences (injuries, available equipment) remembered across chats.",
            props = {
                putJsonObject("preferencesNote") { put("type", "STRING") }
            },
            required = listOf("preferencesNote")
        ))
    }

    fun stepLabelForCall(name: String, strings: AppStrings): String = when (name) {
        "get_routines", "get_user_overview" -> strings.aiStepReadingRoutines
        "get_schedule_events" -> strings.aiStepReadingSchedule
        "get_recent_workouts", "get_personal_records", "get_muscle_recovery_and_volume", "get_active_workout" -> strings.aiStepReadingProgress
        "search_exercises" -> strings.aiStepReadingExercises
        "present_nutrition_analysis" -> strings.aiStepPreparingNutrition
        else -> strings.aiStepStagingDraft
    }

    fun executeCall(
        call: GeminiFunctionCall,
        session: KumaAiToolSessionState,
        strings: AppStrings
    ): JsonElement {
        return when (call.name) {
            "get_user_overview" -> handleGetUserOverview(strings)
            "search_exercises" -> handleSearchExercises(call.args)
            "get_routines" -> handleGetRoutines(strings)
            "get_schedule_events" -> handleGetScheduleEvents(strings)
            "get_recent_workouts" -> handleGetRecentWorkouts(call.args, strings)
            "get_personal_records" -> handleGetPersonalRecords()
            "get_muscle_recovery_and_volume" -> handleGetMuscleRecoveryAndVolume()
            "get_active_workout" -> handleGetActiveWorkout()
            "present_nutrition_analysis" -> handlePresentNutritionAnalysis(call.args, session)
            "stage_create_routine" -> handleStageCreateRoutine(call.args, session, strings)
            "stage_update_routine" -> handleStageUpdateRoutine(call.args, session, strings)
            "stage_delete_routine" -> handleStageDeleteRoutine(call.args, session, strings)
            "stage_schedule_workout" -> handleStageScheduleWorkout(call.args, session, strings)
            "stage_cancel_or_delete_schedule" -> handleStageCancelOrDeleteSchedule(call.args, session, strings)
            "stage_start_mesocycle" -> handleStageStartMesocycle(call.args, session, strings)
            "stage_log_bodyweight" -> handleStageLogBodyweight(call.args, session, strings)
            "stage_update_training_goal" -> handleStageUpdateGoal(call.args, session, strings)
            "stage_add_exercise_to_active_workout" -> handleStageAddExerciseToActiveWorkout(call.args, session)
            "stage_update_ai_preferences" -> handleStageUpdateAiPreferences(call.args, session, strings)
            else -> errorJson("Unknown function: ${call.name}")
        }
    }

    private fun resolveTodayScheduledItems(isoDay: Int): List<Pair<TrainingProgram, RoutineBlueprint?>> {
        val programs = trainingProgramRepository.programs.value
        val routines = routineRepository.routines.value

        val routineItems = routines
            .filter { it.scheduledDays.contains(isoDay) }
            .map { routine ->
                val parentProgram = programs.find { it.id == routine.programId }
                    ?: programs.firstOrNull()
                    ?: TrainingProgram.DEFAULT_HYPERTROPHY
                parentProgram to routine
            }

        val includedProgramIds = routineItems.map { it.first.id }.toSet()
        val programItems = programs
            .filter { !includedProgramIds.contains(it.id) && it.activeDays.contains(isoDay) }
            .mapNotNull { program ->
                val programRoutines = routines.filter { it.programId == program.id }
                val routine = programRoutines.firstOrNull { it.scheduledDays.isEmpty() }
                if (routine != null || programRoutines.isEmpty()) program to routine else null
            }

        return routineItems + programItems
    }

    private fun buildTodayScheduledWorkoutsJson(
        todayItems: List<Pair<TrainingProgram, RoutineBlueprint?>>,
        strings: AppStrings
    ): JsonArray = buildJsonArray {
        todayItems.forEach { (program, routine) ->
            add(buildJsonObject {
                put("programName", program.name)
                if (routine != null) {
                    put("routineId", routine.id)
                    put("routineTitle", routine.localizedTitle(strings))
                    put("splitCategory", routine.splitCategory)
                    put("durationMinutes", routine.estimatedDurationMinutes)
                    putJsonArray("exercises") {
                        routine.effectiveConfigs.forEach { cfg ->
                            add(buildJsonObject {
                                put("name", cfg.exercise.name)
                                put("sets", cfg.targetSets)
                                put("reps", cfg.targetReps)
                            })
                        }
                    }
                } else {
                    put("routineTitle", program.name)
                }
            })
        }
    }

    private fun buildCompletedWorkoutJson(
        w: Workout,
        todayCtx: AiTodayContext,
        tz: TimeZone,
        includeSetDetails: Boolean
    ): JsonObject {
        val dt = Instant.fromEpochMilliseconds(w.startTimeEpochMs).toLocalDateTime(tz)
        val dateStr = dt.date.toString()
        val dayEn = EN_WEEKDAY_NAMES.getOrElse(dt.dayOfWeek.ordinal) { dt.dayOfWeek.name }
        val hhMm = "${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}"
        val relativeDay = when (dateStr) {
            todayCtx.isoDate -> "TODAY"
            todayCtx.yesterdayDate -> "YESTERDAY"
            else -> dateStr
        }
        return buildJsonObject {
            put("id", w.id)
            put("title", w.title)
            put("date", dateStr)
            put("dayOfWeek", dayEn)
            put("time", hhMm)
            put("relativeDay", relativeDay)
            put("durationMin", (w.durationSeconds / 60).coerceAtLeast(1))
            put("volumeKg", w.totalVolumeKg)
            put("completedSets", w.totalCompletedSets)
            putJsonArray("exercises") {
                w.exercises.forEach { we ->
                    val doneSets = we.sets.filter { it.isCompleted }
                    add(buildJsonObject {
                        if (includeSetDetails) {
                            put("exerciseId", we.exercise.id)
                        }
                        put("name", we.exercise.name)
                        put("completedSets", doneSets.size)
                        put("bestWeightKg", doneSets.maxOfOrNull { it.weightKg } ?: 0.0)
                        put("bestReps", doneSets.maxOfOrNull { it.reps } ?: 0)
                        if (includeSetDetails && doneSets.isNotEmpty()) {
                            putJsonArray("sets") {
                                doneSets.forEach { s ->
                                    add(buildJsonObject {
                                        put("reps", s.reps)
                                        put("weightKg", s.weightKg)
                                    })
                                }
                            }
                        }
                    })
                }
            }
        }
    }

    private fun buildOverviewPayload(strings: AppStrings): JsonObject {
        val settings = settingsRepository.settings.value
        val todayCtx = buildAiTodayContext(nowProvider(), strings)
        val activeProgram = trainingProgramRepository.activeProgram
        val activeMeso = mesocycleRepository.activeMesocycle.value
        val readiness = dailyReadinessRepository.todayCheckIn.value
        val todayItems = resolveTodayScheduledItems(todayCtx.isoDayNumber)
        val todayCalendarEvents = workoutScheduleRepository.events.value
            .filter { it.scheduledDate == todayCtx.isoDate }

        return buildJsonObject {
            put("todayDate", todayCtx.isoDate)
            put("yesterdayDate", todayCtx.yesterdayDate)
            put("todayDayOfWeek", todayCtx.dayNameLocalized)
            put("todayDayOfWeekEn", todayCtx.dayNameEn)
            put("todayIsoDayNumber", todayCtx.isoDayNumber)
            put("goal", settings.profile.goal.name)
            put("experienceLevel", settings.profile.experienceLevel.name)
            put("trainingDaysPerWeek", settings.profile.trainingDaysPerWeek)
            settings.profile.bodyweightKg?.let { put("bodyweightKg", it) }
            put("activeProgramId", activeProgram.id)
            put("activeProgramName", activeProgram.name)
            if (settings.ai.preferencesNote.isNotBlank()) {
                put("aiPreferencesNote", settings.ai.preferencesNote)
            }
            put(
                "todayWorkoutStatus",
                if (todayItems.isNotEmpty() || todayCalendarEvents.isNotEmpty()) "SCHEDULED" else "REST_DAY"
            )
            put("todayScheduledWorkouts", buildTodayScheduledWorkoutsJson(todayItems, strings))
            if (todayCalendarEvents.isNotEmpty()) {
                putJsonArray("todayCalendarEvents") {
                    todayCalendarEvents.take(5).forEach { ev ->
                        add(buildJsonObject {
                            put("id", ev.id)
                            put("status", ev.status.name)
                            put("routineTitle", ev.routineTitleSnapshot)
                        })
                    }
                }
            }
            if (activeMeso != null) {
                putJsonObject("activeMesocycle") {
                    put("name", activeMeso.name)
                    put("week", "${activeMeso.currentWeek}/${activeMeso.totalWeeks}")
                    put("isDeloadActive", activeMeso.isDeloadActive)
                }
            }
            if (readiness != null) {
                putJsonObject("todayReadiness") {
                    put("percentage", readiness.overallPercentage)
                    put("recommendation", readiness.recommendation.name)
                }
            }
        }
    }

    /** Builds an instant (<1ms) in-memory DB snapshot so read and hybrid queries avoid extra tool round-trips. */
    fun buildFastContextSnapshot(strings: AppStrings): String {
        val tz = TimeZone.currentSystemDefault()
        val todayCtx = buildAiTodayContext(nowProvider(), strings)
        val base = buildOverviewPayload(strings)
        val routines = routineRepository.routines.value
        val sortedWorkouts = workoutRepository.completedWorkouts.value
            .sortedByDescending { it.startTimeEpochMs }
        val todayWorkouts = sortedWorkouts.filter {
            Instant.fromEpochMilliseconds(it.startTimeEpochMs).toLocalDateTime(tz).date.toString() == todayCtx.isoDate
        }
        val yesterdayWorkouts = sortedWorkouts.filter {
            Instant.fromEpochMilliseconds(it.startTimeEpochMs).toLocalDateTime(tz).date.toString() == todayCtx.yesterdayDate
        }
        val prs = personalRecordRepository.personalRecords.value
        val active = workoutRepository.activeWorkout.value

        return buildJsonObject {
            base.forEach { (k, v) -> put(k, v) }
            put("todayWorkedOut", todayWorkouts.isNotEmpty())
            if (todayWorkouts.isNotEmpty()) {
                putJsonArray("todayCompletedWorkouts") {
                    todayWorkouts.forEach { add(buildCompletedWorkoutJson(it, todayCtx, tz, includeSetDetails = false)) }
                }
            }
            put("yesterdayWorkedOut", yesterdayWorkouts.isNotEmpty())
            if (yesterdayWorkouts.isNotEmpty()) {
                putJsonArray("yesterdayWorkouts") {
                    yesterdayWorkouts.forEach { add(buildCompletedWorkoutJson(it, todayCtx, tz, includeSetDetails = false)) }
                }
            }
            if (active != null) {
                putJsonObject("activeWorkout") {
                    put("title", active.title)
                    put("isPaused", active.isPaused)
                    put("exerciseCount", active.exercises.size)
                }
            }
            if (routines.isNotEmpty()) {
                putJsonArray("savedRoutines") {
                    routines.take(10).forEach { r ->
                        add(buildJsonObject {
                            put("id", r.id)
                            put("title", r.localizedTitle(strings))
                            put("split", r.splitCategory)
                            putJsonArray("days") { r.scheduledDays.forEach { add(JsonPrimitive(it)) } }
                            put(
                                "exercises",
                                r.effectiveConfigs.joinToString(", ") { cfg ->
                                    val wt = cfg.targetWeightKg?.let { " @ ${it}kg" } ?: ""
                                    "${cfg.exercise.name} (${cfg.targetSets}x${cfg.targetReps}$wt)"
                                }
                            )
                        })
                    }
                }
            }
            if (sortedWorkouts.isNotEmpty()) {
                putJsonArray("recentWorkoutsTop5") {
                    sortedWorkouts.take(5).forEach { w ->
                        add(buildCompletedWorkoutJson(w, todayCtx, tz, includeSetDetails = false))
                    }
                }
            }
            if (prs.isNotEmpty()) {
                putJsonArray("topPrs") {
                    prs.take(5).forEach { pr ->
                        add(buildJsonObject {
                            put("exercise", pr.exerciseName)
                            put("weightKg", pr.weightKg)
                            put("reps", pr.reps)
                            put("e1rmKg", pr.estimated1RmKg)
                        })
                    }
                }
            }
            if (sortedWorkouts.isNotEmpty()) {
                val recovery = MuscleRecoveryTracker.calculateRecovery(sortedWorkouts)
                val volume = MuscleVolumeCalculator.calculateWeeklyVolume(sortedWorkouts)
                val notableMuscles = recovery.filter {
                    it.readinessPercentage < 95 || (volume[it.muscleGroup]?.weeklySets ?: 0) > 0
                }.take(8)
                if (notableMuscles.isNotEmpty()) {
                    putJsonArray("muscleStatus") {
                        notableMuscles.forEach { rec ->
                            add(buildJsonObject {
                                put("muscle", rec.muscleGroup.name)
                                put("readinessPct", rec.readinessPercentage)
                                volume[rec.muscleGroup]?.let { put("weeklySets", it.weeklySets) }
                            })
                        }
                    }
                }
            }
        }.toString()
    }

    private fun handleGetUserOverview(strings: AppStrings): JsonElement = buildOverviewPayload(strings)

    private fun handleSearchExercises(args: JsonObject): JsonElement {
        val query = args.str("query").orEmpty()
        val muscle = args.str("muscleGroup")?.let { raw ->
            MuscleGroup.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
        }
        val equipment = args.str("equipment")?.let { raw ->
            Equipment.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
        }
        val limit = (args.intVal("limit") ?: 30).coerceIn(1, 50)
        val matches = exerciseRepository.searchExercises(query, muscle, equipment).take(limit)
        return buildJsonObject {
            put("count", matches.size)
            putJsonArray("exercises") {
                matches.forEach { ex ->
                    add(buildJsonObject {
                        put("id", ex.id)
                        put("name", ex.name)
                        put("muscle", ex.primaryMuscle.name)
                        put("equipment", ex.equipment.name)
                        put("movementType", ex.movementType.name)
                    })
                }
            }
        }
    }

    private fun handleGetRoutines(strings: AppStrings): JsonElement {
        val list = routineRepository.routines.value
        val todayIso = buildAiTodayContext(nowProvider(), strings).isoDayNumber
        return buildJsonObject {
            put("count", list.size)
            put("todayIsoDayNumber", todayIso)
            putJsonArray("routines") {
                list.forEach { r ->
                    add(buildJsonObject {
                        put("id", r.id)
                        put("title", r.localizedTitle(strings))
                        put("splitCategory", r.splitCategory)
                        put("durationMinutes", r.estimatedDurationMinutes)
                        put("isScheduledToday", r.scheduledDays.contains(todayIso))
                        putJsonArray("scheduledDays") { r.scheduledDays.forEach { add(JsonPrimitive(it)) } }
                        putJsonArray("scheduledDayNames") {
                            r.scheduledDays.forEach { dayNum ->
                                val name = strings.dayNamesFull.getOrNull(dayNum - 1) ?: "Day $dayNum"
                                add(JsonPrimitive(name))
                            }
                        }
                        putJsonArray("exercises") {
                            r.effectiveConfigs.forEach { cfg ->
                                add(buildJsonObject {
                                    put("exerciseId", cfg.exercise.id)
                                    put("name", cfg.exercise.name)
                                    put("sets", cfg.targetSets)
                                    put("reps", cfg.targetReps)
                                    cfg.targetWeightKg?.let { put("targetWeightKg", it) }
                                    put("restSeconds", cfg.restTimerSeconds)
                                    if (cfg.setConfigs.isNotEmpty()) {
                                        putJsonArray("setConfigs") {
                                            cfg.setConfigs.forEach { setCfg ->
                                                add(buildJsonObject {
                                                    put("setNumber", setCfg.setNumber)
                                                    put("reps", setCfg.targetReps)
                                                    setCfg.targetWeightKg?.let { put("weightKg", it) }
                                                    setCfg.restTimerSeconds?.let { put("restTimerSeconds", it) }
                                                })
                                            }
                                        }
                                    }
                                })
                            }
                        }
                    })
                }
            }
        }
    }

    private fun handleGetScheduleEvents(strings: AppStrings): JsonElement {
        val todayCtx = buildAiTodayContext(nowProvider(), strings)
        val todayItems = resolveTodayScheduledItems(todayCtx.isoDayNumber)
        val list = workoutScheduleRepository.events.value
        return buildJsonObject {
            put("todayDate", todayCtx.isoDate)
            put("todayDayOfWeek", todayCtx.dayNameLocalized)
            put("todayIsoDayNumber", todayCtx.isoDayNumber)
            put("weeklyScheduledToday", buildTodayScheduledWorkoutsJson(todayItems, strings))
            put("count", list.size)
            putJsonArray("events") {
                list.take(30).forEach { ev ->
                    add(buildJsonObject {
                        put("id", ev.id)
                        put("date", ev.scheduledDate)
                        put("status", ev.status.name)
                        ev.routineId?.let { put("routineId", it) }
                        put("routineTitle", ev.routineTitleSnapshot)
                    })
                }
            }
        }
    }

    private fun handleGetRecentWorkouts(args: JsonObject, strings: AppStrings): JsonElement {
        val limit = (args.intVal("limit") ?: 5).coerceIn(1, 15)
        val tz = TimeZone.currentSystemDefault()
        val todayCtx = buildAiTodayContext(nowProvider(), strings)
        val sortedWorkouts = workoutRepository.completedWorkouts.value
            .sortedByDescending { it.startTimeEpochMs }
        val workouts = sortedWorkouts.take(limit)
        val yesterdayWorkedOut = sortedWorkouts.any {
            Instant.fromEpochMilliseconds(it.startTimeEpochMs).toLocalDateTime(tz).date.toString() == todayCtx.yesterdayDate
        }
        return buildJsonObject {
            put("todayDate", todayCtx.isoDate)
            put("yesterdayDate", todayCtx.yesterdayDate)
            put("yesterdayWorkedOut", yesterdayWorkedOut)
            put("count", workouts.size)
            putJsonArray("workouts") {
                workouts.forEach { w ->
                    add(buildCompletedWorkoutJson(w, todayCtx, tz, includeSetDetails = true))
                }
            }
        }
    }

    private fun handleGetPersonalRecords(): JsonElement {
        val prs = personalRecordRepository.personalRecords.value.take(20)
        return buildJsonObject {
            put("count", prs.size)
            putJsonArray("records") {
                prs.forEach { pr ->
                    add(buildJsonObject {
                        put("exerciseName", pr.exerciseName)
                        put("weightKg", pr.weightKg)
                        put("reps", pr.reps)
                        put("estimated1RmKg", pr.estimated1RmKg)
                        put("date", pr.dateAchieved)
                    })
                }
            }
        }
    }

    private fun handleGetMuscleRecoveryAndVolume(): JsonElement {
        val workouts = workoutRepository.completedWorkouts.value
        val recovery = MuscleRecoveryTracker.calculateRecovery(workouts)
        val volume = MuscleVolumeCalculator.calculateWeeklyVolume(workouts)
        return buildJsonObject {
            putJsonArray("muscles") {
                recovery.forEach { rec ->
                    val vol = volume[rec.muscleGroup]
                    if (rec.setsLast72h > 0 || (vol?.weeklySets ?: 0) > 0) {
                        add(buildJsonObject {
                            put("muscle", rec.muscleGroup.name)
                            put("readinessPct", rec.readinessPercentage)
                            put("recoveryStatus", rec.status.name)
                            if (vol != null) {
                                put("weeklySets", vol.weeklySets)
                                put("volumeStatus", vol.status.name)
                            }
                        })
                    }
                }
            }
        }
    }

    private fun handleGetActiveWorkout(): JsonElement {
        val active = workoutRepository.activeWorkout.value ?: return buildJsonObject {
            put("active", false)
        }
        return buildJsonObject {
            put("active", true)
            put("title", active.title)
            put("isPaused", active.isPaused)
            putJsonArray("exercises") {
                active.exercises.forEach { we ->
                    add(buildJsonObject {
                        put("workoutExerciseId", we.id)
                        put("exerciseId", we.exercise.id)
                        put("name", we.exercise.name)
                        put("completedSets", we.sets.count { it.isCompleted })
                        put("totalSets", we.sets.size)
                    })
                }
            }
        }
    }

    private fun handlePresentNutritionAnalysis(args: JsonObject, session: KumaAiToolSessionState): JsonElement {
        val foodName = args.str("foodName")?.takeIf { it.isNotBlank() } ?: return errorJson("Missing foodName")
        val card = AiNutritionCardData(
            foodName = foodName,
            caloriesKcal = (args.intVal("caloriesKcal") ?: 0).coerceIn(0, 10_000),
            proteinGrams = (args.intVal("proteinGrams") ?: 0).coerceIn(0, 1_000),
            carbsGrams = (args.intVal("carbsGrams") ?: 0).coerceIn(0, 1_500),
            fatGrams = (args.intVal("fatGrams") ?: 0).coerceIn(0, 500),
            summary = args.str("summary").orEmpty()
        )
        session.nutritionCard = card
        return buildJsonObject { put("displayed", true) }
    }

    private fun handleStageCreateRoutine(
        args: JsonObject,
        session: KumaAiToolSessionState,
        strings: AppStrings
    ): JsonElement {
        val title = args.str("title")?.trim()?.takeIf { it.isNotBlank() } ?: return errorJson("Routine title cannot be blank")
        val exList = parseExercisesArg(args["exercises"]) ?: return errorJson("exercises must be a non-empty array")
        val missing = exList.filter { exerciseRepository.getExerciseById(it.exerciseId) == null }.map { it.exerciseId }
        if (missing.isNotEmpty()) {
            return errorJson("Unknown exerciseId(s): ${missing.joinToString()}. Call search_exercises to get valid IDs.")
        }

        val duration = (args.intVal("estimatedDurationMinutes") ?: 45).coerceIn(10, 180)
        val totalSets = exList.sumOf { it.targetSets.coerceIn(1, 10) }
        val payload = StageCreateRoutinePayload(
            title = title,
            splitCategory = args.str("splitCategory")?.ifBlank { "Custom" } ?: "Custom",
            muscleFocus = args.str("muscleFocus").orEmpty(),
            estimatedDurationMinutes = duration,
            targetRpeTier = args.str("targetRpeTier")?.ifBlank { "moderate" } ?: "moderate",
            scheduledDays = parseIntList(args["scheduledDays"]).filter { it in 1..7 }.distinct(),
            exercises = exList
        )
        val op = AiProposedOperation(
            id = IdGenerator.nextId("aiop"),
            actionType = AiActionType.CREATE_ROUTINE,
            operationKind = AiOperationKind.ADD,
            title = "${strings.createRoutineTitle}: $title",
            subtitle = strings.routineSubtitle(exList.size, duration),
            payloadJson = json.encodeToString(payload)
        )
        session.stagedOperations.add(op)
        return buildJsonObject {
            put("status", "DRAFTED_FOR_USER_REVIEW")
            put("operationId", op.id)
            put("pendingRoutineId", op.id)
            put("title", op.title)
        }
    }

    private fun handleStageUpdateRoutine(
        args: JsonObject,
        session: KumaAiToolSessionState,
        strings: AppStrings
    ): JsonElement {
        val routineId = args.str("routineId") ?: return errorJson("Missing routineId")
        val existing = routineRepository.getRoutineById(routineId) ?: return errorJson("Routine not found: $routineId")
        val exercises = args["exercises"]?.let { elem ->
            val parsed = parseExercisesArg(elem) ?: return errorJson("Invalid exercises array")
            val missing = parsed.filter { exerciseRepository.getExerciseById(it.exerciseId) == null }.map { it.exerciseId }
            if (missing.isNotEmpty()) return errorJson("Unknown exerciseId(s): ${missing.joinToString()}")
            parsed
        }
        val payload = StageUpdateRoutinePayload(
            routineId = routineId,
            title = args.str("title"),
            splitCategory = args.str("splitCategory"),
            muscleFocus = args.str("muscleFocus"),
            estimatedDurationMinutes = args.intVal("estimatedDurationMinutes")?.coerceIn(10, 180),
            scheduledDays = args["scheduledDays"]?.let { parseIntList(it).filter { d -> d in 1..7 }.distinct() },
            exercises = exercises
        )
        val targetTitle = payload.title ?: existing.title
        val countInfo = exercises?.let { strings.exerciseCountLabel(it.size) } ?: strings.editRoutineTitle
        val op = AiProposedOperation(
            id = IdGenerator.nextId("aiop"),
            actionType = AiActionType.UPDATE_ROUTINE,
            operationKind = AiOperationKind.UPDATE,
            title = "${strings.editRoutineTitle}: $targetTitle",
            subtitle = countInfo,
            payloadJson = json.encodeToString(payload)
        )
        session.stagedOperations.add(op)
        return stagedAck(op)
    }

    private fun handleStageDeleteRoutine(
        args: JsonObject,
        session: KumaAiToolSessionState,
        strings: AppStrings
    ): JsonElement {
        val routineId = args.str("routineId") ?: return errorJson("Missing routineId")
        val existing = routineRepository.getRoutineById(routineId) ?: return errorJson("Routine not found: $routineId")
        val op = AiProposedOperation(
            id = IdGenerator.nextId("aiop"),
            actionType = AiActionType.DELETE_ROUTINE,
            operationKind = AiOperationKind.DELETE,
            title = "${strings.deleteRoutineContentDesc}: ${existing.title}",
            subtitle = strings.exerciseCountLabel(existing.effectiveConfigs.size),
            payloadJson = json.encodeToString(StageIdPayload(routineId))
        )
        session.stagedOperations.add(op)
        return stagedAck(op)
    }

    private fun handleStageScheduleWorkout(
        args: JsonObject,
        session: KumaAiToolSessionState,
        strings: AppStrings
    ): JsonElement {
        val routineId = args.str("routineId") ?: return errorJson("Missing routineId")
        val dateStr = args.str("scheduledDate") ?: return errorJson("Missing scheduledDate")
        val parsedDate = try { LocalDate.parse(dateStr) } catch (_: Exception) {
            return errorJson("Invalid date format '$dateStr'. Expected YYYY-MM-DD.")
        }
        val existingRoutine = routineRepository.getRoutineById(routineId)
        val matchedStagedCreate = session.stagedOperations
            .firstOrNull { it.actionType == AiActionType.CREATE_ROUTINE && it.id == routineId }
            ?: session.stagedOperations.lastOrNull { it.actionType == AiActionType.CREATE_ROUTINE }
        val stagedRoutineTitle = matchedStagedCreate?.let {
            try { json.decodeFromString<StageCreateRoutinePayload>(it.payloadJson).title } catch (_: Exception) { null }
        }
        val titleSnapshot = existingRoutine?.title ?: stagedRoutineTitle
            ?: return errorJson("Routine not found: $routineId. Create or query a routine first.")

        val payload = StageScheduleWorkoutPayload(
            routineId = routineId,
            scheduledDate = parsedDate.toString(),
            notes = args.str("notes")
        )
        val op = AiProposedOperation(
            id = IdGenerator.nextId("aiop"),
            actionType = AiActionType.SCHEDULE_WORKOUT,
            operationKind = AiOperationKind.ADD,
            title = "${strings.scheduleRoutineTitle}: $parsedDate",
            subtitle = titleSnapshot,
            payloadJson = json.encodeToString(payload)
        )
        session.stagedOperations.add(op)
        return stagedAck(op)
    }

    private fun handleStageCancelOrDeleteSchedule(
        args: JsonObject,
        session: KumaAiToolSessionState,
        strings: AppStrings
    ): JsonElement {
        val eventId = args.str("eventId") ?: return errorJson("Missing eventId")
        val event = workoutScheduleRepository.events.value.find { it.id == eventId }
            ?: return errorJson("Schedule event not found: $eventId")
        val deletePerm = args["deletePermanently"]?.jsonPrimitive?.booleanOrNull == true
        val op = AiProposedOperation(
            id = IdGenerator.nextId("aiop"),
            actionType = if (deletePerm) AiActionType.DELETE_SCHEDULE else AiActionType.CANCEL_SCHEDULE,
            operationKind = if (deletePerm) AiOperationKind.DELETE else AiOperationKind.UPDATE,
            title = "${if (deletePerm) strings.deleteSchedule else strings.cancelSchedule}: ${event.scheduledDate}",
            subtitle = event.routineTitleSnapshot,
            payloadJson = json.encodeToString(StageCancelOrDeleteSchedulePayload(eventId, deletePerm))
        )
        session.stagedOperations.add(op)
        return stagedAck(op)
    }

    private fun handleStageStartMesocycle(
        args: JsonObject,
        session: KumaAiToolSessionState,
        strings: AppStrings
    ): JsonElement {
        val name = args.str("name")?.trim()?.takeIf { it.isNotBlank() } ?: return errorJson("Missing mesocycle name")
        val weeks = (args.intVal("totalWeeks") ?: 5).coerceIn(2, 16)
        val deload = (args.intVal("deloadWeek") ?: weeks).coerceIn(2, weeks)
        val payload = StageStartMesocyclePayload(
            name = name,
            totalWeeks = weeks,
            deloadWeek = deload,
            notes = args.str("notes").orEmpty()
        )
        val op = AiProposedOperation(
            id = IdGenerator.nextId("aiop"),
            actionType = AiActionType.START_MESOCYCLE,
            operationKind = AiOperationKind.ADD,
            title = name,
            subtitle = "${strings.mesocycleWeekShort(weeks)} · ${strings.phaseDeload}: ${strings.mesocycleWeekShort(deload)}",
            payloadJson = json.encodeToString(payload)
        )
        session.stagedOperations.add(op)
        return stagedAck(op)
    }

    private fun handleStageLogBodyweight(
        args: JsonObject,
        session: KumaAiToolSessionState,
        strings: AppStrings
    ): JsonElement {
        val weightKg = args["weightKg"]?.jsonPrimitive?.doubleOrNull
            ?: return errorJson("Missing weightKg")
        if (weightKg !in 20.0..400.0) return errorJson("weightKg out of realistic range (20..400)")
        val op = AiProposedOperation(
            id = IdGenerator.nextId("aiop"),
            actionType = AiActionType.LOG_BODYWEIGHT,
            operationKind = AiOperationKind.UPDATE,
            title = "${strings.bodyWeightMeasureLabel}: $weightKg ${strings.unitKg}",
            subtitle = strings.profileTitle,
            payloadJson = json.encodeToString(StageLogBodyweightPayload(weightKg))
        )
        session.stagedOperations.add(op)
        return stagedAck(op)
    }

    private fun handleStageUpdateGoal(
        args: JsonObject,
        session: KumaAiToolSessionState,
        strings: AppStrings
    ): JsonElement {
        val goalRaw = args.str("goal") ?: return errorJson("Missing goal")
        val goal = TrainingGoal.entries.firstOrNull { it.name.equals(goalRaw, ignoreCase = true) }
            ?: return errorJson("Invalid goal '$goalRaw'. Allowed: HYPERTROPHY, STRENGTH, ENDURANCE, FAT_LOSS, GENERAL")
        val op = AiProposedOperation(
            id = IdGenerator.nextId("aiop"),
            actionType = AiActionType.UPDATE_TRAINING_GOAL,
            operationKind = AiOperationKind.UPDATE,
            title = "${strings.trainingGoalTitle}: ${goal.name}",
            subtitle = strings.profileTitle,
            payloadJson = json.encodeToString(StageUpdateGoalPayload(goal))
        )
        session.stagedOperations.add(op)
        return stagedAck(op)
    }

    private fun handleStageAddExerciseToActiveWorkout(
        args: JsonObject,
        session: KumaAiToolSessionState
    ): JsonElement {
        if (workoutRepository.activeWorkout.value == null) {
            return errorJson("No active workout session running.")
        }
        val exerciseId = args.str("exerciseId") ?: return errorJson("Missing exerciseId")
        val exercise = exerciseRepository.getExerciseById(exerciseId)
            ?: return errorJson("Unknown exerciseId: $exerciseId")
        val op = AiProposedOperation(
            id = IdGenerator.nextId("aiop"),
            actionType = AiActionType.ADD_EXERCISE_TO_ACTIVE_WORKOUT,
            operationKind = AiOperationKind.ADD,
            title = exercise.name,
            subtitle = workoutRepository.activeWorkout.value?.title.orEmpty(),
            payloadJson = json.encodeToString(StageIdPayload(exerciseId))
        )
        session.stagedOperations.add(op)
        return stagedAck(op)
    }

    private fun handleStageUpdateAiPreferences(
        args: JsonObject,
        session: KumaAiToolSessionState,
        strings: AppStrings
    ): JsonElement {
        val note = args.str("preferencesNote")?.trim() ?: return errorJson("Missing preferencesNote")
        val op = AiProposedOperation(
            id = IdGenerator.nextId("aiop"),
            actionType = AiActionType.UPDATE_AI_PREFERENCES,
            operationKind = AiOperationKind.UPDATE,
            title = note.take(64),
            subtitle = strings.aiPreferencesLabel,
            payloadJson = json.encodeToString(StageTextPayload(note))
        )
        session.stagedOperations.add(op)
        return stagedAck(op)
    }

    fun commitProposal(proposal: AiActionProposal): Pair<Boolean, String> {
        if (proposal.operations.isEmpty()) return false to "No operations"
        var lastCreatedRoutineId: String? = null
        val createdRoutinesByOpId = mutableMapOf<String, String>()
        val appliedTitles = mutableListOf<String>()

        return try {
            proposal.operations.forEach { op ->
                when (op.actionType) {
                    AiActionType.CREATE_ROUTINE -> {
                        val dto = json.decodeFromString<StageCreateRoutinePayload>(op.payloadJson)
                        val configs = dto.exercises.mapNotNull { item ->
                            val ex = exerciseRepository.getExerciseById(item.exerciseId) ?: return@mapNotNull null
                            item.toRoutineExerciseConfig(ex)
                        }
                        require(configs.isNotEmpty()) { "No valid exercises in routine ${dto.title}" }
                        val routineId = IdGenerator.routineId(dto.title)
                        val blueprint = RoutineBlueprint(
                            id = routineId,
                            programId = trainingProgramRepository.activeProgram.id,
                            title = dto.title,
                            splitCategory = dto.splitCategory,
                            muscleFocus = dto.muscleFocus.ifBlank {
                                configs.map { it.exercise.primaryMuscle.name }.distinct().take(3).joinToString(" · ")
                            },
                            estimatedDurationMinutes = dto.estimatedDurationMinutes,
                            targetRpeTier = RpeTier.fromValue(dto.targetRpeTier).code,
                            exerciseConfigs = configs,
                            plannedSetsPerExercise = configs.firstOrNull()?.targetSets ?: 3,
                            scheduledDays = dto.scheduledDays
                        )
                        routineRepository.addRoutine(blueprint)
                        lastCreatedRoutineId = routineId
                        createdRoutinesByOpId[op.id] = routineId
                        appliedTitles.add(dto.title)
                    }
                    AiActionType.UPDATE_ROUTINE -> {
                        val dto = json.decodeFromString<StageUpdateRoutinePayload>(op.payloadJson)
                        val existing = routineRepository.getRoutineById(dto.routineId)
                            ?: error("Routine ${dto.routineId} no longer exists")
                        val newConfigs = dto.exercises?.mapNotNull { item ->
                            val ex = exerciseRepository.getExerciseById(item.exerciseId) ?: return@mapNotNull null
                            item.toRoutineExerciseConfig(ex)
                        } ?: existing.effectiveConfigs
                        val updated = existing.copy(
                            title = dto.title ?: existing.title,
                            splitCategory = dto.splitCategory ?: existing.splitCategory,
                            muscleFocus = dto.muscleFocus ?: existing.muscleFocus,
                            estimatedDurationMinutes = dto.estimatedDurationMinutes ?: existing.estimatedDurationMinutes,
                            scheduledDays = dto.scheduledDays ?: existing.scheduledDays,
                            exerciseConfigs = newConfigs,
                            isUserRenamed = dto.title != null || existing.isUserRenamed
                        )
                        routineRepository.updateRoutine(updated)
                        appliedTitles.add(updated.title)
                    }
                    AiActionType.DELETE_ROUTINE -> {
                        val dto = json.decodeFromString<StageIdPayload>(op.payloadJson)
                        val existing = routineRepository.getRoutineById(dto.id)
                        routineRepository.deleteRoutine(dto.id)
                        appliedTitles.add(existing?.title ?: dto.id)
                    }
                    AiActionType.SCHEDULE_WORKOUT -> {
                        val dto = json.decodeFromString<StageScheduleWorkoutPayload>(op.payloadJson)
                        val resolvedRoutine = routineRepository.getRoutineById(dto.routineId)
                            ?: createdRoutinesByOpId[dto.routineId]?.let { routineRepository.getRoutineById(it) }
                            ?: lastCreatedRoutineId?.let { routineRepository.getRoutineById(it) }
                            ?: error("Routine not found for scheduling")
                        val date = LocalDate.parse(dto.scheduledDate)
                        workoutScheduleRepository.scheduleRoutine(
                            routineId = resolvedRoutine.id,
                            routineTitle = resolvedRoutine.title,
                            splitType = resolvedRoutine.splitCategory,
                            scheduledDate = date,
                            notes = dto.notes
                        )
                        appliedTitles.add("${resolvedRoutine.title} (${dto.scheduledDate})")
                    }
                    AiActionType.CANCEL_SCHEDULE -> {
                        val dto = json.decodeFromString<StageCancelOrDeleteSchedulePayload>(op.payloadJson)
                        workoutScheduleRepository.cancelEvent(dto.eventId)
                        appliedTitles.add(op.title)
                    }
                    AiActionType.DELETE_SCHEDULE -> {
                        val dto = json.decodeFromString<StageCancelOrDeleteSchedulePayload>(op.payloadJson)
                        workoutScheduleRepository.deleteEvent(dto.eventId)
                        appliedTitles.add(op.title)
                    }
                    AiActionType.START_MESOCYCLE -> {
                        val dto = json.decodeFromString<StageStartMesocyclePayload>(op.payloadJson)
                        mesocycleRepository.startNewMesocycle(
                            name = dto.name,
                            totalWeeks = dto.totalWeeks,
                            programId = trainingProgramRepository.activeProgram.id,
                            deloadWeek = dto.deloadWeek,
                            notes = dto.notes
                        )
                        appliedTitles.add(dto.name)
                    }
                    AiActionType.LOG_BODYWEIGHT -> {
                        val dto = json.decodeFromString<StageLogBodyweightPayload>(op.payloadJson)
                        settingsRepository.updateProfile { it.copy(bodyweightKg = dto.weightKg) }
                        appliedTitles.add("${dto.weightKg} kg")
                    }
                    AiActionType.UPDATE_TRAINING_GOAL -> {
                        val dto = json.decodeFromString<StageUpdateGoalPayload>(op.payloadJson)
                        settingsRepository.updateProfile { it.copy(goal = dto.goal) }
                        appliedTitles.add(dto.goal.name)
                    }
                    AiActionType.ADD_EXERCISE_TO_ACTIVE_WORKOUT -> {
                        val dto = json.decodeFromString<StageIdPayload>(op.payloadJson)
                        val ex = exerciseRepository.getExerciseById(dto.id) ?: error("Exercise not found")
                        workoutRepository.addExerciseToActiveWorkout(ex)
                        appliedTitles.add(ex.name)
                    }
                    AiActionType.UPDATE_AI_PREFERENCES -> {
                        val dto = json.decodeFromString<StageTextPayload>(op.payloadJson)
                        settingsRepository.update { s -> s.copy(ai = s.ai.copy(preferencesNote = dto.text)) }
                        appliedTitles.add(op.title)
                    }
                }
            }
            true to appliedTitles.joinToString(", ")
        } catch (e: Exception) {
            false to (e.message ?: "Commit failed")
        }
    }

    private fun StageRoutineExerciseDto.toRoutineExerciseConfig(ex: Exercise): RoutineExerciseConfig {
        val defaultRest = restTimerSeconds.coerceIn(15, 300)
        val uniformWeight = targetWeightKg?.takeIf { it > 0.0 }
        val resolvedSetConfigs = if (setConfigs.isNotEmpty()) {
            setConfigs.take(12).mapIndexed { index, setDto ->
                RoutineSetConfig(
                    setNumber = index + 1,
                    targetReps = setDto.reps.coerceIn(1, 50),
                    targetWeightKg = (setDto.weightKg ?: uniformWeight)?.takeIf { it > 0.0 },
                    restTimerSeconds = (setDto.restTimerSeconds ?: defaultRest).coerceIn(15, 300)
                )
            }
        } else if (uniformWeight != null) {
            val setsCount = targetSets.coerceIn(1, 12)
            val repsCount = targetReps.coerceIn(1, 50)
            (1..setsCount).map { idx ->
                RoutineSetConfig(
                    setNumber = idx,
                    targetReps = repsCount,
                    targetWeightKg = uniformWeight,
                    restTimerSeconds = defaultRest
                )
            }
        } else {
            emptyList()
        }
        val effectiveSetsCount = resolvedSetConfigs.size.takeIf { it > 0 } ?: targetSets.coerceIn(1, 12)
        val effectiveReps = resolvedSetConfigs.firstOrNull()?.targetReps ?: targetReps.coerceIn(1, 50)
        val effectiveWeight = uniformWeight ?: resolvedSetConfigs.mapNotNull { it.targetWeightKg }.maxOrNull()
        return RoutineExerciseConfig(
            exercise = ex,
            targetSets = effectiveSetsCount,
            targetReps = effectiveReps,
            targetWeightKg = effectiveWeight,
            restTimerSeconds = defaultRest,
            setConfigs = resolvedSetConfigs
        )
    }

    private fun parseExercisesArg(element: JsonElement?): List<StageRoutineExerciseDto>? {
        val arr = element as? JsonArray ?: return null
        if (arr.isEmpty()) return null
        return arr.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val id = obj.str("exerciseId") ?: return@mapNotNull null
            val defaultReps = (obj.intVal("targetReps") ?: 10).coerceIn(1, 50)
            val uniformWeight = obj.doubleVal("targetWeightKg")?.takeIf { it > 0.0 }
            val defaultRest = (obj.intVal("restTimerSeconds") ?: 75).coerceIn(15, 300)
            val parsedSets = (obj["setConfigs"] as? JsonArray)?.mapNotNull { setElem ->
                val setObj = setElem as? JsonObject ?: return@mapNotNull null
                StageRoutineSetDto(
                    reps = (setObj.intVal("reps") ?: defaultReps).coerceIn(1, 50),
                    weightKg = setObj.doubleVal("weightKg")?.takeIf { it > 0.0 } ?: uniformWeight,
                    restTimerSeconds = setObj.intVal("restTimerSeconds")?.coerceIn(15, 300)
                )
            }.orEmpty().take(12)
            StageRoutineExerciseDto(
                exerciseId = id,
                targetSets = (obj.intVal("targetSets") ?: parsedSets.size.takeIf { it > 0 } ?: 3).coerceIn(1, 12),
                targetReps = defaultReps,
                targetWeightKg = uniformWeight,
                restTimerSeconds = defaultRest,
                setConfigs = parsedSets
            )
        }.takeIf { it.isNotEmpty() }
    }

    private fun parseIntList(element: JsonElement?): List<Int> {
        val arr = element as? JsonArray ?: return emptyList()
        return arr.mapNotNull { it.jsonPrimitive.intOrNull }
    }

    private fun stagedAck(op: AiProposedOperation): JsonElement = buildJsonObject {
        put("status", "DRAFTED_FOR_USER_REVIEW")
        put("operationId", op.id)
        put("title", op.title)
    }

    private fun errorJson(msg: String): JsonElement = buildJsonObject {
        put("error", msg)
    }

    private fun JsonObject.str(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }

    private fun JsonObject.intVal(key: String): Int? =
        this[key]?.jsonPrimitive?.intOrNull

    private fun JsonObject.doubleVal(key: String): Double? =
        this[key]?.jsonPrimitive?.doubleOrNull

    private fun fnDecl(
        name: String,
        toolDoc: String,
        required: List<String> = emptyList(),
        props: (kotlinx.serialization.json.JsonObjectBuilder.() -> Unit)? = null
    ): JsonObject = buildJsonObject {
        put("name", name)
        put("description", toolDoc)
        if (props != null) {
            putJsonObject("parameters") {
                put("type", "OBJECT")
                putJsonObject("properties", props)
                if (required.isNotEmpty()) {
                    putJsonArray("required") { required.forEach { add(JsonPrimitive(it)) } }
                }
            }
        }
    }
}
