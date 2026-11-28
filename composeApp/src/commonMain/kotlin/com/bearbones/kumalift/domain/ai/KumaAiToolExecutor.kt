package com.bearbones.kumalift.domain.ai

import com.bearbones.kumalift.core.localization.AppStrings
import com.bearbones.kumalift.core.localization.label
import com.bearbones.kumalift.core.util.IdGenerator
import com.bearbones.kumalift.core.util.currentEpochMillis
import com.bearbones.kumalift.data.local.KumaDatabaseRepository
import com.bearbones.kumalift.data.repository.DailyReadinessRepository
import com.bearbones.kumalift.data.repository.ExerciseRepository
import com.bearbones.kumalift.data.repository.MesocycleRepository
import com.bearbones.kumalift.data.repository.PersonalRecordRepository
import com.bearbones.kumalift.data.repository.ProgressPhotoRepository
import com.bearbones.kumalift.data.repository.RoutineRepository
import com.bearbones.kumalift.data.repository.SettingsRepository
import com.bearbones.kumalift.data.repository.TrainingProgramRepository
import com.bearbones.kumalift.data.repository.WorkoutRepository
import com.bearbones.kumalift.data.repository.WorkoutScheduleRepository
import com.bearbones.kumalift.domain.model.DailyReadinessCheckIn
import com.bearbones.kumalift.domain.model.Equipment
import com.bearbones.kumalift.domain.model.Exercise
import com.bearbones.kumalift.domain.model.MuscleGroup
import com.bearbones.kumalift.domain.model.ExperienceLevel
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

@Serializable
private data class StageCreateProgramPayload(
    val name: String,
    val description: String = "",
    val category: TrainingGoal = TrainingGoal.HYPERTROPHY,
    val activeDays: List<Int> = listOf(1, 3, 5),
    val setActive: Boolean = true
)

@Serializable
private data class StageCreateExercisePayload(
    val name: String,
    val primaryMuscle: MuscleGroup,
    val equipment: Equipment = Equipment.BARBELL,
    val secondaryMuscles: List<MuscleGroup> = emptyList()
)

@Serializable
private data class StageSaveExerciseNotePayload(
    val exerciseId: String,
    val note: String
)

@Serializable
private data class StageLogReadinessPayload(
    val sleep: Int,
    val soreness: Int,
    val energy: Int,
    val stress: Int,
    val notes: String = ""
)

class KumaAiToolSessionState {
    val stagedOperations = mutableListOf<AiProposedOperation>()
    var nutritionCard: AiNutritionCardData? = null
    val assistantImages = mutableListOf<String>()
    val webSourceLinks = mutableListOf<AiWebSource>()
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
    },
    private val progressPhotoRepository: ProgressPhotoRepository? = null
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
                putJsonObject("muscleGroup") { put("type", "STRING"); put("description", "Optional enum: CHEST, BACK, LEGS, SHOULDERS, ARMS, CORE, CARDIO, NECK, FULL_BODY") }
                putJsonObject("equipment") { put("type", "STRING"); put("description", "Optional enum: BARBELL, DUMBBELL, CABLE, BODYWEIGHT, MACHINE, SMITH_MACHINE, BAND, KETTLEBELL, OTHER") }
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

        add(fnDecl(
            name = "search_chat_history",
            toolDoc = "Search or inspect messages from past saved conversations (by keyword query and/or threadId) so you understand full details from previous chat histories.",
            props = {
                putJsonObject("query") { put("type", "STRING"); put("description", "Optional keyword or topic to search across past saved chats") }
                putJsonObject("threadId") { put("type", "STRING"); put("description", "Optional threadId from savedChatHistories to read exact messages") }
                putJsonObject("limit") { put("type", "INTEGER"); put("description", "Max messages to return (default 8)") }
            }
        ))

        add(fnDecl(
            name = "show_exercise_visuals",
            toolDoc = "Attach exercise form/movement illustrations from KumaLift Exercise Library directly into your reply bubble. Always call search_exercises first to get exact exerciseId values.",
            props = {
                putJsonObject("exerciseIds") {
                    put("type", "ARRAY")
                    putJsonObject("items") { put("type", "STRING") }
                    put("description", "Up to 6 exerciseId strings to display visually")
                }
            },
            required = listOf("exerciseIds")
        ))

        add(fnDecl(
            name = "get_progress_photos",
            toolDoc = "Read the user's saved private progress photos & body check-ins, and optionally attach them into your reply for visual feedback or comparison.",
            props = {
                putJsonObject("limit") { put("type", "INTEGER"); put("description", "Max records to read (default 4, max 8)") }
                putJsonObject("attachImages") { put("type", "BOOLEAN"); put("description", "Set true when user asks to view/show/compare their photos (default false)") }
            }
        ))

        add(fnDecl(
            name = "generate_image",
            toolDoc = "Generate a custom image or illustration using Gemini when the user asks to create, draw, visualize, or generate an image.",
            props = {
                putJsonObject("prompt") { put("type", "STRING"); put("description", "Detailed visual description in English for generating the image") }
            },
            required = listOf("prompt")
        ))

        add(fnDecl(
            name = "stage_create_program",
            toolDoc = "Stage creating a new training program (and optionally switching to it as active) for user approval.",
            props = {
                putJsonObject("name") { put("type", "STRING") }
                putJsonObject("description") { put("type", "STRING") }
                putJsonObject("category") { put("type", "STRING"); put("description", "HYPERTROPHY, STRENGTH, ENDURANCE, FAT_LOSS, or GENERAL") }
                putJsonObject("activeDays") { put("type", "ARRAY"); putJsonObject("items") { put("type", "INTEGER") } }
                putJsonObject("setActive") { put("type", "BOOLEAN"); put("description", "Default true: switch active program so staged routines belong to it") }
            },
            required = listOf("name")
        ))

        add(fnDecl(
            name = "stage_create_exercise",
            toolDoc = "Stage creating a new custom exercise in KumaLift Exercise Library when no matching movement exists.",
            props = {
                putJsonObject("name") { put("type", "STRING") }
                putJsonObject("primaryMuscle") { put("type", "STRING"); put("description", "CHEST, BACK, SHOULDERS, ARMS, LEGS, CORE, CARDIO, or FULL_BODY") }
                putJsonObject("equipment") { put("type", "STRING"); put("description", "BARBELL, DUMBBELL, CABLE, MACHINE, BODYWEIGHT, KETTLEBELL, BAND, or OTHER") }
                putJsonObject("secondaryMuscles") { put("type", "ARRAY"); putJsonObject("items") { put("type", "STRING") } }
            },
            required = listOf("name", "primaryMuscle")
        ))

        add(fnDecl(
            name = "stage_save_exercise_note",
            toolDoc = "Stage saving a persistent pinned coaching/technique note on an existing exercise.",
            props = {
                putJsonObject("exerciseId") { put("type", "STRING") }
                putJsonObject("note") { put("type", "STRING") }
            },
            required = listOf("exerciseId", "note")
        ))

        add(fnDecl(
            name = "stage_log_readiness",
            toolDoc = "Stage logging today's daily readiness check-in (sleep, soreness, energy, stress from 1..5).",
            props = {
                putJsonObject("sleep") { put("type", "INTEGER"); put("description", "1 (poor) to 5 (great)") }
                putJsonObject("soreness") { put("type", "INTEGER"); put("description", "1 (very sore) to 5 (fresh)") }
                putJsonObject("energy") { put("type", "INTEGER"); put("description", "1 (exhausted) to 5 (energized)") }
                putJsonObject("stress") { put("type", "INTEGER"); put("description", "1 (high stress) to 5 (relaxed)") }
                putJsonObject("notes") { put("type", "STRING") }
            },
            required = listOf("sleep", "soreness", "energy", "stress")
        ))

        add(fnDecl(
            name = "search_web_live",
            toolDoc = "Search the live internet via Google Search for up-to-date facts, scientific studies, nutrition info, guides, products, or clickable web URLs.",
            props = {
                putJsonObject("query") { put("type", "STRING"); put("description", "Concise search query") }
            },
            required = listOf("query")
        ))

        add(fnDecl(
            name = "fetch_web_page",
            toolDoc = "Fetch and read readable text content from a specific https:// or http:// web page URL.",
            props = {
                putJsonObject("url") { put("type", "STRING"); put("description", "Full https:// or http:// URL to read") }
            },
            required = listOf("url")
        ))
    }

    fun stepLabelForCall(name: String, strings: AppStrings): String = when (name) {
        "get_routines", "get_user_overview" -> strings.aiStepReadingRoutines
        "get_schedule_events" -> strings.aiStepReadingSchedule
        "get_recent_workouts", "get_personal_records", "get_muscle_recovery_and_volume", "get_active_workout" -> strings.aiStepReadingProgress
        "search_exercises" -> strings.aiStepReadingExercises
        "search_chat_history" -> strings.aiStepSearchingHistory
        "present_nutrition_analysis" -> strings.aiStepPreparingNutrition
        "show_exercise_visuals" -> strings.aiStepLoadingVisuals
        "get_progress_photos" -> strings.aiStepReadingProgressPhotos
        "generate_image" -> strings.aiStepGeneratingImage
        "search_web_live" -> strings.aiStepSearchingWeb
        "fetch_web_page" -> strings.aiStepReadingWebPage
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
            "search_chat_history" -> handleSearchChatHistory(call.args, strings)
            "show_exercise_visuals" -> handleShowExerciseVisuals(call.args, session)
            "get_progress_photos" -> handleGetProgressPhotos(call.args, session)
            "present_nutrition_analysis" -> handlePresentNutritionAnalysis(call.args, session)
            "stage_create_routine" -> handleStageCreateRoutine(call.args, session, strings)
            "stage_update_routine" -> handleStageUpdateRoutine(call.args, session, strings)
            "stage_delete_routine" -> handleStageDeleteRoutine(call.args, session, strings)
            "stage_create_program" -> handleStageCreateProgram(call.args, session, strings)
            "stage_create_exercise" -> handleStageCreateExercise(call.args, session)
            "stage_save_exercise_note" -> handleStageSaveExerciseNote(call.args, session, strings)
            "stage_log_readiness" -> handleStageLogReadiness(call.args, session, strings)
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
    fun buildFastContextSnapshot(
        strings: AppStrings,
        activeThreadId: String? = null,
        includeChatHistories: Boolean = true,
        earlierMessagesSummary: String? = null
    ): String {
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
        val savedThreads = if (includeChatHistories) {
            databaseRepo?.getAllAiChatThreads().orEmpty().take(10)
        } else {
            emptyList()
        }

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
            if (!earlierMessagesSummary.isNullOrBlank()) {
                put("currentThreadEarlierContext", earlierMessagesSummary)
            }
            if (savedThreads.isNotEmpty()) {
                putJsonArray("savedChatHistories") {
                    savedThreads.forEach { thread ->
                        val dateStr = Instant.fromEpochMilliseconds(thread.updatedAtEpochMs)
                            .toLocalDateTime(tz).date.toString()
                        add(buildJsonObject {
                            put("threadId", thread.id)
                            put("title", thread.title)
                            put("date", dateStr)
                            put("messageCount", thread.messageCount)
                            if (thread.id == activeThreadId) put("isCurrent", true)
                            if (thread.contextSummary.isNotBlank()) put("summary", thread.contextSummary)
                        })
                    }
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
        val muscle = args.str("muscleGroup")?.let { resolveMuscleGroup(it) }
        val equipment = args.str("equipment")?.let { resolveEquipment(it) }
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

    private fun resolveMuscleGroup(raw: String): MuscleGroup? {
        val norm = raw.trim().uppercase().replace(' ', '_').replace('-', '_')
        MuscleGroup.entries.firstOrNull { it.name == norm }?.let { return it }
        return when (norm) {
            "BICEPS", "TRICEPS", "FOREARMS", "ARM" -> MuscleGroup.ARMS
            "QUADS", "QUADRICEPS", "HAMSTRINGS", "GLUTES", "CALVES", "LEG" -> MuscleGroup.LEGS
            "TRAPS", "LATS", "LOWER_BACK", "UPPER_BACK" -> MuscleGroup.BACK
            "ABS", "ABDOMINALS", "OBLIQUES" -> MuscleGroup.CORE
            "DELTS", "DELTOIDS" -> MuscleGroup.SHOULDERS
            else -> null
        }
    }

    private fun resolveEquipment(raw: String): Equipment? {
        val norm = raw.trim().uppercase().replace(' ', '_').replace('-', '_')
        Equipment.entries.firstOrNull { it.name == norm }?.let { return it }
        return when (norm) {
            "RESISTANCE_BAND", "RESISTANCE_BANDS", "BANDS" -> Equipment.BAND
            "SMITH" -> Equipment.SMITH_MACHINE
            "BODY_WEIGHT", "BW" -> Equipment.BODYWEIGHT
            else -> null
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

    private fun handleSearchChatHistory(args: JsonObject, strings: AppStrings): JsonElement {
        val repo = databaseRepo ?: return buildJsonObject {
            put("threadsCount", 0)
            putJsonArray("matches") {}
        }
        val tz = TimeZone.currentSystemDefault()
        val targetThreadId = args.str("threadId")
        val rawQuery = args.str("query").orEmpty()
        val limit = (args.intVal("limit") ?: 8).coerceIn(1, 20)
        val allThreads = repo.getAllAiChatThreads()
        val threadById = allThreads.associateBy { it.id }

        if (targetThreadId != null) {
            val thread = threadById[targetThreadId] ?: return errorJson("Chat thread not found: $targetThreadId")
            val msgs = repo.getAiChatMessages(targetThreadId)
                .filter { !it.isError && it.text.isNotBlank() }
                .let { list ->
                    if (rawQuery.isBlank()) list
                    else list.filter { it.text.contains(rawQuery, ignoreCase = true) }
                }
                .takeLast(limit)
            return buildJsonObject {
                put("threadId", thread.id)
                put("title", thread.title)
                put("contextSummary", thread.contextSummary)
                put("matchCount", msgs.size)
                putJsonArray("messages") {
                    msgs.forEach { m ->
                        val dateStr = Instant.fromEpochMilliseconds(m.createdAtEpochMs)
                            .toLocalDateTime(tz).date.toString()
                        add(buildJsonObject {
                            put("role", m.role.name.lowercase())
                            put("date", dateStr)
                            put("text", m.text.take(420))
                        })
                    }
                }
            }
        }

        val tokens = rawQuery.split(Regex("""\s+""")).map { it.trim() }.filter { it.length >= 2 }
        val matchingThreads = if (tokens.isEmpty()) {
            allThreads.take(limit)
        } else {
            allThreads.filter { t ->
                tokens.any { kw ->
                    t.title.contains(kw, ignoreCase = true) ||
                        t.contextSummary.contains(kw, ignoreCase = true)
                }
            }.take(limit)
        }
        val directMsgHits = if (rawQuery.isNotBlank()) {
            val hits = repo.searchAiChatMessages(rawQuery, limit)
            if (hits.isNotEmpty()) hits
            else tokens.flatMap { repo.searchAiChatMessages(it, 6) }.distinctBy { it.id }.take(limit)
        } else {
            emptyList()
        }

        return buildJsonObject {
            put("todayDate", buildAiTodayContext(nowProvider(), strings).isoDate)
            put("totalSavedThreads", allThreads.size)
            putJsonArray("threads") {
                matchingThreads.forEach { t ->
                    val dateStr = Instant.fromEpochMilliseconds(t.updatedAtEpochMs)
                        .toLocalDateTime(tz).date.toString()
                    add(buildJsonObject {
                        put("threadId", t.id)
                        put("title", t.title)
                        put("date", dateStr)
                        put("messageCount", t.messageCount)
                        put("contextSummary", t.contextSummary)
                    })
                }
            }
            if (directMsgHits.isNotEmpty()) {
                putJsonArray("messages") {
                    directMsgHits.forEach { m ->
                        val dateStr = Instant.fromEpochMilliseconds(m.createdAtEpochMs)
                            .toLocalDateTime(tz).date.toString()
                        val parentTitle = threadById[m.threadId]?.title.orEmpty()
                        add(buildJsonObject {
                            put("threadId", m.threadId)
                            if (parentTitle.isNotBlank()) put("threadTitle", parentTitle)
                            put("role", m.role.name.lowercase())
                            put("date", dateStr)
                            put("text", m.text.take(420))
                        })
                    }
                }
            }
        }
    }

    private fun handleShowExerciseVisuals(args: JsonObject, session: KumaAiToolSessionState): JsonElement {
        val rawIds = (args["exerciseIds"] as? JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { id -> id.isNotBlank() } }
            ?.distinct()
            ?.take(6)
            .orEmpty()
        if (rawIds.isEmpty()) return errorJson("exerciseIds must be a non-empty array")

        val attachedNames = mutableListOf<String>()
        val missingIds = mutableListOf<String>()
        rawIds.forEach { id ->
            val ex = exerciseRepository.getExerciseById(id)
            val assetRel = ex?.thumbnailUrl?.trim()?.takeIf { it.isNotBlank() }
                ?: ex?.gifUrl?.trim()?.takeIf { it.endsWith(".gif", ignoreCase = true) }
            if (ex != null && assetRel != null) {
                val uri = "file:///android_asset/${assetRel.removePrefix("/")}"
                if (!session.assistantImages.contains(uri)) {
                    session.assistantImages.add(uri)
                }
                attachedNames.add(ex.name)
            } else {
                missingIds.add(id)
            }
        }
        return buildJsonObject {
            put("attachedCount", attachedNames.size)
            putJsonArray("attachedExercises") { attachedNames.forEach { add(JsonPrimitive(it)) } }
            if (missingIds.isNotEmpty()) {
                putJsonArray("missingIds") { missingIds.forEach { add(JsonPrimitive(it)) } }
            }
        }
    }

    private fun handleGetProgressPhotos(args: JsonObject, session: KumaAiToolSessionState): JsonElement {
        val repo = progressPhotoRepository ?: return errorJson("Progress photos repository unavailable")
        val limit = (args.intVal("limit") ?: 4).coerceIn(1, 8)
        val attachImages = (args["attachImages"] as? JsonPrimitive)?.booleanOrNull == true
        val photos = repo.photos.value
            .sortedByDescending { it.capturedDateEpochMs }
            .take(limit)
        val tz = TimeZone.currentSystemDefault()
        return buildJsonObject {
            put("count", photos.size)
            putJsonArray("photos") {
                photos.forEachIndexed { idx, photo ->
                    val dateStr = Instant.fromEpochMilliseconds(photo.capturedDateEpochMs)
                        .toLocalDateTime(tz).date.toString()
                    val shouldAttach = attachImages && idx < 4 && photo.imagePath.isNotBlank()
                    if (shouldAttach && !session.assistantImages.contains(photo.imagePath)) {
                        session.assistantImages.add(photo.imagePath)
                    }
                    add(buildJsonObject {
                        put("date", dateStr)
                        photo.bodyweightKg?.let { put("bodyweightKg", it) }
                        photo.waistCm?.let { put("waistCm", it) }
                        photo.chestCm?.let { put("chestCm", it) }
                        photo.armsCm?.let { put("armsCm", it) }
                        if (photo.note.isNotBlank()) put("note", photo.note.take(120))
                        put("attached", shouldAttach)
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
            payloadJson = json.encodeToString(payload),
            previewDetail = buildCreateRoutinePreview(payload)
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
        val beforeTitle = existing.localizedTitle(strings)
        val targetTitle = payload.title ?: beforeTitle
        val preview = buildUpdateRoutinePreview(existing, payload, beforeTitle, targetTitle)
        val countInfo = exercises?.let { strings.exerciseCountLabel(it.size) } ?: strings.editRoutineTitle
        val op = AiProposedOperation(
            id = IdGenerator.nextId("aiop"),
            actionType = AiActionType.UPDATE_ROUTINE,
            operationKind = AiOperationKind.UPDATE,
            title = "${strings.editRoutineTitle}: $targetTitle",
            subtitle = countInfo,
            payloadJson = json.encodeToString(payload),
            previewDetail = preview
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
        val existingTitle = existing.localizedTitle(strings)
        val beforeItems = existing.effectiveConfigs.map { it.toPreviewItem() }
        val op = AiProposedOperation(
            id = IdGenerator.nextId("aiop"),
            actionType = AiActionType.DELETE_ROUTINE,
            operationKind = AiOperationKind.DELETE,
            title = "${strings.deleteRoutineContentDesc}: $existingTitle",
            subtitle = strings.exerciseCountLabel(existing.effectiveConfigs.size),
            payloadJson = json.encodeToString(StageIdPayload(routineId)),
            previewDetail = AiOperationPreviewDetail(
                routineId = routineId,
                beforeRoutineName = existingTitle,
                beforeSplitCategory = existing.splitCategory,
                beforeDurationMinutes = existing.estimatedDurationMinutes,
                beforeScheduledDays = existing.scheduledDays,
                exerciseDiffs = beforeItems.map { AiExerciseDiffEntry(AiExerciseDiffStatus.REMOVED, before = it) }
            )
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
        val titleSnapshot = existingRoutine?.localizedTitle(strings) ?: stagedRoutineTitle
            ?: return errorJson("Routine not found: $routineId. Create or query a routine first.")

        val payload = StageScheduleWorkoutPayload(
            routineId = routineId,
            scheduledDate = parsedDate.toString(),
            notes = args.str("notes")
        )
        val preview = AiOperationPreviewDetail(
            routineId = routineId,
            routineName = titleSnapshot,
            fieldChanges = buildList {
                add(AiFieldChangePreview("routine_name", afterValue = titleSnapshot))
                add(AiFieldChangePreview("schedule_date", afterValue = parsedDate.toString()))
                payload.notes?.takeIf { it.isNotBlank() }?.let { note ->
                    add(AiFieldChangePreview("notes", afterValue = note))
                }
            }
        )
        val op = AiProposedOperation(
            id = IdGenerator.nextId("aiop"),
            actionType = AiActionType.SCHEDULE_WORKOUT,
            operationKind = AiOperationKind.ADD,
            title = "${strings.scheduleRoutineTitle}: $parsedDate",
            subtitle = titleSnapshot,
            payloadJson = json.encodeToString(payload),
            previewDetail = preview
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
        val deletePerm = (args["deletePermanently"] as? JsonPrimitive)?.booleanOrNull == true
        val preview = AiOperationPreviewDetail(
            routineName = event.routineTitleSnapshot,
            fieldChanges = listOf(
                AiFieldChangePreview("routine_name", afterValue = event.routineTitleSnapshot),
                AiFieldChangePreview("schedule_date", afterValue = event.scheduledDate),
                AiFieldChangePreview(
                    fieldKey = "event_status",
                    beforeValue = event.status.name,
                    afterValue = if (deletePerm) "DELETED" else "CANCELLED"
                )
            )
        )
        val op = AiProposedOperation(
            id = IdGenerator.nextId("aiop"),
            actionType = if (deletePerm) AiActionType.DELETE_SCHEDULE else AiActionType.CANCEL_SCHEDULE,
            operationKind = if (deletePerm) AiOperationKind.DELETE else AiOperationKind.UPDATE,
            title = "${if (deletePerm) strings.deleteSchedule else strings.cancelSchedule}: ${event.scheduledDate}",
            subtitle = event.routineTitleSnapshot,
            payloadJson = json.encodeToString(StageCancelOrDeleteSchedulePayload(eventId, deletePerm)),
            previewDetail = preview
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
        val beforeMeso = mesocycleRepository.activeMesocycle.value
        val preview = AiOperationPreviewDetail(
            fieldChanges = buildList {
                add(AiFieldChangePreview("routine_name", beforeValue = beforeMeso?.name, afterValue = name))
                add(
                    AiFieldChangePreview(
                        fieldKey = "weeks",
                        beforeValue = beforeMeso?.totalWeeks?.toString(),
                        afterValue = weeks.toString()
                    )
                )
                add(
                    AiFieldChangePreview(
                        fieldKey = "deload_week",
                        beforeValue = beforeMeso?.deloadWeek?.toString(),
                        afterValue = deload.toString()
                    )
                )
                if (payload.notes.isNotBlank()) {
                    add(AiFieldChangePreview("notes", afterValue = payload.notes))
                }
            }
        )
        val op = AiProposedOperation(
            id = IdGenerator.nextId("aiop"),
            actionType = AiActionType.START_MESOCYCLE,
            operationKind = AiOperationKind.ADD,
            title = name,
            subtitle = "${strings.mesocycleWeekShort(weeks)} · ${strings.phaseDeload}: ${strings.mesocycleWeekShort(deload)}",
            payloadJson = json.encodeToString(payload),
            previewDetail = preview
        )
        session.stagedOperations.add(op)
        return stagedAck(op)
    }

    private fun handleStageLogBodyweight(
        args: JsonObject,
        session: KumaAiToolSessionState,
        strings: AppStrings
    ): JsonElement {
        val weightKg = args.doubleVal("weightKg")
            ?: return errorJson("Missing weightKg")
        if (weightKg !in 20.0..400.0) return errorJson("weightKg out of realistic range (20..400)")
        val beforeWeight = settingsRepository.settings.value.profile.bodyweightKg
        val preview = AiOperationPreviewDetail(
            fieldChanges = listOf(
                AiFieldChangePreview(
                    fieldKey = "bodyweight",
                    beforeValue = beforeWeight?.let { "$it kg" },
                    afterValue = "$weightKg kg",
                    beforeWeightKg = beforeWeight,
                    afterWeightKg = weightKg
                )
            )
        )
        val op = AiProposedOperation(
            id = IdGenerator.nextId("aiop"),
            actionType = AiActionType.LOG_BODYWEIGHT,
            operationKind = AiOperationKind.UPDATE,
            title = "${strings.bodyWeightMeasureLabel}: $weightKg ${strings.unitKg}",
            subtitle = strings.profileTitle,
            payloadJson = json.encodeToString(StageLogBodyweightPayload(weightKg)),
            previewDetail = preview
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
        val beforeGoal = settingsRepository.settings.value.profile.goal
        val preview = AiOperationPreviewDetail(
            fieldChanges = listOf(
                AiFieldChangePreview(
                    fieldKey = "goal",
                    beforeValue = beforeGoal.name,
                    afterValue = goal.name
                )
            )
        )
        val op = AiProposedOperation(
            id = IdGenerator.nextId("aiop"),
            actionType = AiActionType.UPDATE_TRAINING_GOAL,
            operationKind = AiOperationKind.UPDATE,
            title = "${strings.trainingGoalTitle}: ${goal.label(strings)}",
            subtitle = strings.profileTitle,
            payloadJson = json.encodeToString(StageUpdateGoalPayload(goal)),
            previewDetail = preview
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
        val defaultSets = settingsRepository.settings.value.logging.defaultSetCount.coerceIn(1, 10)
        val item = AiExercisePreviewItem(
            exerciseId = exercise.id,
            name = exercise.name,
            primaryMuscleCode = exercise.primaryMuscle.name,
            equipmentCode = exercise.equipment.name,
            sets = defaultSets,
            reps = 10,
            restSeconds = settingsRepository.settings.value.logging.restTimerDefaultSec
        )
        val preview = AiOperationPreviewDetail(
            routineName = workoutRepository.activeWorkout.value?.title.orEmpty(),
            exerciseDiffs = listOf(AiExerciseDiffEntry(AiExerciseDiffStatus.ADDED, after = item)),
            updatedExercises = listOf(item),
            fieldChanges = listOf(
                AiFieldChangePreview("exercise", afterValue = exercise.name),
                AiFieldChangePreview("category", afterValue = exercise.primaryMuscle.name),
                AiFieldChangePreview("equipment", afterValue = exercise.equipment.name)
            )
        )
        val op = AiProposedOperation(
            id = IdGenerator.nextId("aiop"),
            actionType = AiActionType.ADD_EXERCISE_TO_ACTIVE_WORKOUT,
            operationKind = AiOperationKind.ADD,
            title = exercise.name,
            subtitle = workoutRepository.activeWorkout.value?.title.orEmpty(),
            payloadJson = json.encodeToString(StageIdPayload(exerciseId)),
            previewDetail = preview
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
        val beforeNote = settingsRepository.settings.value.ai.preferencesNote.trim().takeIf { it.isNotBlank() }
        val preview = AiOperationPreviewDetail(
            fieldChanges = listOf(
                AiFieldChangePreview(
                    fieldKey = "notes",
                    beforeValue = beforeNote,
                    afterValue = note
                )
            )
        )
        val op = AiProposedOperation(
            id = IdGenerator.nextId("aiop"),
            actionType = AiActionType.UPDATE_AI_PREFERENCES,
            operationKind = AiOperationKind.UPDATE,
            title = note.take(64),
            subtitle = strings.aiPreferencesLabel,
            payloadJson = json.encodeToString(StageTextPayload(note)),
            previewDetail = preview
        )
        session.stagedOperations.add(op)
        return stagedAck(op)
    }

    private fun handleStageCreateProgram(
        args: JsonObject,
        session: KumaAiToolSessionState,
        strings: AppStrings
    ): JsonElement {
        val name = args.str("name")?.trim() ?: return errorJson("Missing program name")
        val description = args.str("description")?.trim().orEmpty()
        val categoryRaw = args.str("category")?.uppercase()
        val category = TrainingGoal.entries.firstOrNull { it.name == categoryRaw } ?: TrainingGoal.HYPERTROPHY
        val activeDays = parseIntList(args["activeDays"]).filter { it in 1..7 }.distinct().sorted()
            .ifEmpty { listOf(1, 3, 5) }
        val setActive = (args["setActive"] as? JsonPrimitive)?.booleanOrNull ?: true
        val payload = StageCreateProgramPayload(
            name = name,
            description = description,
            category = category,
            activeDays = activeDays,
            setActive = setActive
        )
        val preview = buildCreateProgramPreview(payload)
        val op = AiProposedOperation(
            id = IdGenerator.nextId("aiop"),
            actionType = AiActionType.CREATE_PROGRAM,
            operationKind = AiOperationKind.ADD,
            title = name,
            subtitle = category.label(strings),
            payloadJson = json.encodeToString(payload),
            previewDetail = preview
        )
        session.stagedOperations.add(op)
        return stagedAck(op)
    }

    private fun handleStageCreateExercise(
        args: JsonObject,
        session: KumaAiToolSessionState
    ): JsonElement {
        val name = args.str("name")?.trim() ?: return errorJson("Missing exercise name")
        val muscleRaw = args.str("primaryMuscle")?.uppercase()
        val primaryMuscle = MuscleGroup.entries.firstOrNull { it.name == muscleRaw }
            ?: return errorJson("Invalid primaryMuscle")
        val equipRaw = args.str("equipment")?.uppercase()
        val equipment = Equipment.entries.firstOrNull { it.name == equipRaw } ?: Equipment.OTHER
        val secondary = (args["secondaryMuscles"] as? JsonArray)
            ?.mapNotNull { el ->
                val raw = (el as? JsonPrimitive)?.contentOrNull?.uppercase()
                MuscleGroup.entries.firstOrNull { it.name == raw }
            }
            ?.distinct()
            .orEmpty()
        val payload = StageCreateExercisePayload(
            name = name,
            primaryMuscle = primaryMuscle,
            equipment = equipment,
            secondaryMuscles = secondary
        )
        val preview = AiOperationPreviewDetail(
            fieldChanges = listOf(
                AiFieldChangePreview("exercise", afterValue = name),
                AiFieldChangePreview("category", afterValue = primaryMuscle.name),
                AiFieldChangePreview("equipment", afterValue = equipment.name)
            )
        )
        val op = AiProposedOperation(
            id = IdGenerator.nextId("aiop"),
            actionType = AiActionType.CREATE_EXERCISE,
            operationKind = AiOperationKind.ADD,
            title = name,
            subtitle = primaryMuscle.name,
            payloadJson = json.encodeToString(payload),
            previewDetail = preview
        )
        session.stagedOperations.add(op)
        return stagedAck(op)
    }

    private fun handleStageSaveExerciseNote(
        args: JsonObject,
        session: KumaAiToolSessionState,
        strings: AppStrings
    ): JsonElement {
        val exerciseId = args.str("exerciseId") ?: return errorJson("Missing exerciseId")
        val note = args.str("note")?.trim() ?: return errorJson("Missing note")
        val ex = exerciseRepository.getExerciseById(exerciseId)
            ?: return errorJson("Exercise not found: $exerciseId")
        val payload = StageSaveExerciseNotePayload(exerciseId = ex.id, note = note)
        val beforeNote = ex.pinnedNote.trim().takeIf { it.isNotBlank() }
        val preview = AiOperationPreviewDetail(
            fieldChanges = listOf(
                AiFieldChangePreview("exercise", afterValue = ex.name),
                AiFieldChangePreview("notes", beforeValue = beforeNote, afterValue = note)
            )
        )
        val op = AiProposedOperation(
            id = IdGenerator.nextId("aiop"),
            actionType = AiActionType.SAVE_EXERCISE_NOTE,
            operationKind = AiOperationKind.UPDATE,
            title = ex.name,
            subtitle = strings.aiPreviewFieldNotes,
            payloadJson = json.encodeToString(payload),
            previewDetail = preview
        )
        session.stagedOperations.add(op)
        return stagedAck(op)
    }

    private fun handleStageLogReadiness(
        args: JsonObject,
        session: KumaAiToolSessionState,
        strings: AppStrings
    ): JsonElement {
        val sleep = (args.intVal("sleep") ?: return errorJson("Missing sleep")).coerceIn(1, 5)
        val soreness = (args.intVal("soreness") ?: return errorJson("Missing soreness")).coerceIn(1, 5)
        val energy = (args.intVal("energy") ?: return errorJson("Missing energy")).coerceIn(1, 5)
        val stress = (args.intVal("stress") ?: return errorJson("Missing stress")).coerceIn(1, 5)
        val notes = args.str("notes")?.trim().orEmpty()
        val payload = StageLogReadinessPayload(
            sleep = sleep,
            soreness = soreness,
            energy = energy,
            stress = stress,
            notes = notes
        )
        val preview = buildLogReadinessPreview(payload)
        val (pct, _) = DailyReadinessCheckIn.calculate(sleep, soreness, energy, stress)
        val op = AiProposedOperation(
            id = IdGenerator.nextId("aiop"),
            actionType = AiActionType.LOG_READINESS,
            operationKind = AiOperationKind.ADD,
            title = "${strings.dailyReadinessTitle} ($pct%)",
            subtitle = notes.ifBlank { "${sleep}/5 · ${soreness}/5 · ${energy}/5 · ${stress}/5" },
            payloadJson = json.encodeToString(payload),
            previewDetail = preview
        )
        session.stagedOperations.add(op)
        return stagedAck(op)
    }

    fun commitProposal(proposal: AiActionProposal, strings: AppStrings? = null): Pair<Boolean, String> {
        if (proposal.operations.isEmpty()) {
            return false to (strings?.aiCommitErrorGeneric ?: "No operations")
        }
        var lastCreatedRoutineId: String? = null
        var createdProgramId: String? = null
        val createdRoutinesByOpId = mutableMapOf<String, String>()
        val appliedTitles = mutableListOf<String>()

        return try {
            // Pre-validate all operations before mutating any repository so batches remain atomic
            var pendingRoutineCreates = 0
            val pendingCreateOpIds = mutableSetOf<String>()
            proposal.operations.forEach { op ->
                when (op.actionType) {
                    AiActionType.CREATE_ROUTINE -> {
                        val dto = json.decodeFromString<StageCreateRoutinePayload>(op.payloadJson)
                        val validCount = dto.exercises.count { exerciseRepository.getExerciseById(it.exerciseId) != null }
                        require(validCount > 0) {
                            strings?.aiCommitErrorNoValidExercises ?: "No valid exercises in routine ${dto.title}"
                        }
                        pendingRoutineCreates++
                        pendingCreateOpIds.add(op.id)
                    }
                    AiActionType.UPDATE_ROUTINE -> {
                        val dto = json.decodeFromString<StageUpdateRoutinePayload>(op.payloadJson)
                        routineRepository.getRoutineById(dto.routineId)
                            ?: throw IllegalStateException(strings?.aiCommitErrorRoutineMissing ?: "Routine no longer exists")
                        dto.exercises?.let { list ->
                            val validCount = list.count { exerciseRepository.getExerciseById(it.exerciseId) != null }
                            require(validCount > 0) {
                                strings?.aiCommitErrorNoValidExercises ?: "No valid exercises in routine update"
                            }
                        }
                    }
                    AiActionType.SCHEDULE_WORKOUT -> {
                        val dto = json.decodeFromString<StageScheduleWorkoutPayload>(op.payloadJson)
                        val hasRoutine = routineRepository.getRoutineById(dto.routineId) != null ||
                            dto.routineId in pendingCreateOpIds ||
                            pendingRoutineCreates > 0
                        require(hasRoutine) {
                            strings?.aiCommitErrorRoutineMissing ?: "Routine not found for scheduling"
                        }
                        LocalDate.parse(dto.scheduledDate)
                    }
                    AiActionType.ADD_EXERCISE_TO_ACTIVE_WORKOUT -> {
                        val dto = json.decodeFromString<StageIdPayload>(op.payloadJson)
                        check(workoutRepository.activeWorkout.value != null) {
                            strings?.aiCommitErrorNoActiveWorkout ?: "No active workout session"
                        }
                        exerciseRepository.getExerciseById(dto.id)
                            ?: throw IllegalStateException(strings?.aiCommitErrorGeneric ?: "Exercise not found")
                    }
                    AiActionType.DELETE_ROUTINE -> {
                        json.decodeFromString<StageIdPayload>(op.payloadJson)
                    }
                    AiActionType.CANCEL_SCHEDULE,
                    AiActionType.DELETE_SCHEDULE -> {
                        val dto = json.decodeFromString<StageCancelOrDeleteSchedulePayload>(op.payloadJson)
                        check(workoutScheduleRepository.events.value.any { it.id == dto.eventId }) {
                            strings?.aiCommitErrorGeneric ?: "Scheduled event not found"
                        }
                    }
                    AiActionType.START_MESOCYCLE -> {
                        json.decodeFromString<StageStartMesocyclePayload>(op.payloadJson)
                    }
                    AiActionType.LOG_BODYWEIGHT -> {
                        json.decodeFromString<StageLogBodyweightPayload>(op.payloadJson)
                    }
                    AiActionType.UPDATE_TRAINING_GOAL -> {
                        json.decodeFromString<StageUpdateGoalPayload>(op.payloadJson)
                    }
                    AiActionType.UPDATE_AI_PREFERENCES -> {
                        json.decodeFromString<StageTextPayload>(op.payloadJson)
                    }
                    AiActionType.CREATE_PROGRAM -> {
                        val dto = json.decodeFromString<StageCreateProgramPayload>(op.payloadJson)
                        require(dto.name.isNotBlank()) { "Program name required" }
                    }
                    AiActionType.CREATE_EXERCISE -> {
                        val dto = json.decodeFromString<StageCreateExercisePayload>(op.payloadJson)
                        require(dto.name.isNotBlank()) { "Exercise name required" }
                    }
                    AiActionType.SAVE_EXERCISE_NOTE -> {
                        val dto = json.decodeFromString<StageSaveExerciseNotePayload>(op.payloadJson)
                        exerciseRepository.getExerciseById(dto.exerciseId)
                            ?: throw IllegalStateException(strings?.aiCommitErrorGeneric ?: "Exercise not found")
                    }
                    AiActionType.LOG_READINESS -> {
                        json.decodeFromString<StageLogReadinessPayload>(op.payloadJson)
                    }
                }
            }

            proposal.operations.forEach { op ->
                when (op.actionType) {
                    AiActionType.CREATE_PROGRAM -> {
                        val dto = json.decodeFromString<StageCreateProgramPayload>(op.payloadJson)
                        val created = trainingProgramRepository.createProgram(
                            name = dto.name,
                            description = dto.description,
                            category = dto.category,
                            activeDays = dto.activeDays
                        )
                        if (dto.setActive) {
                            trainingProgramRepository.setActiveProgramId(created.id)
                        }
                        createdProgramId = created.id
                        appliedTitles.add(created.name)
                    }
                    AiActionType.CREATE_EXERCISE -> {
                        val dto = json.decodeFromString<StageCreateExercisePayload>(op.payloadJson)
                        val created = exerciseRepository.addCustomExercise(
                            name = dto.name,
                            primaryMuscle = dto.primaryMuscle,
                            equipment = dto.equipment,
                            secondaryMuscles = dto.secondaryMuscles
                        )
                        appliedTitles.add(created.name)
                    }
                    AiActionType.SAVE_EXERCISE_NOTE -> {
                        val dto = json.decodeFromString<StageSaveExerciseNotePayload>(op.payloadJson)
                        val ex = exerciseRepository.getExerciseById(dto.exerciseId)
                            ?: throw IllegalStateException(strings?.aiCommitErrorGeneric ?: "Exercise not found")
                        exerciseRepository.updatePinnedNote(ex.id, dto.note)
                        appliedTitles.add(ex.name)
                    }
                    AiActionType.LOG_READINESS -> {
                        val dto = json.decodeFromString<StageLogReadinessPayload>(op.payloadJson)
                        val checkIn = dailyReadinessRepository.recordCheckIn(
                            sleep = dto.sleep,
                            soreness = dto.soreness,
                            energy = dto.energy,
                            stress = dto.stress,
                            notes = dto.notes
                        )
                        appliedTitles.add("${strings?.dailyReadinessTitle ?: "Daily Readiness"} (${checkIn.overallPercentage}%)")
                    }
                    AiActionType.CREATE_ROUTINE -> {
                        val dto = json.decodeFromString<StageCreateRoutinePayload>(op.payloadJson)
                        val configs = dto.exercises.mapNotNull { item ->
                            val ex = exerciseRepository.getExerciseById(item.exerciseId) ?: return@mapNotNull null
                            item.toRoutineExerciseConfig(ex)
                        }
                        require(configs.isNotEmpty()) {
                            strings?.aiCommitErrorNoValidExercises ?: "No valid exercises in routine ${dto.title}"
                        }
                        val routineId = IdGenerator.routineId(dto.title)
                        val blueprint = RoutineBlueprint(
                            id = routineId,
                            programId = createdProgramId ?: trainingProgramRepository.activeProgram.id,
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
                        check(routineRepository.getRoutineById(routineId) != null) {
                            strings?.aiCommitErrorGeneric ?: "Routine save failed"
                        }
                        lastCreatedRoutineId = routineId
                        createdRoutinesByOpId[op.id] = routineId
                        appliedTitles.add(dto.title)
                    }
                    AiActionType.UPDATE_ROUTINE -> {
                        val dto = json.decodeFromString<StageUpdateRoutinePayload>(op.payloadJson)
                        val existing = routineRepository.getRoutineById(dto.routineId)
                            ?: throw IllegalStateException(strings?.aiCommitErrorRoutineMissing ?: "Routine no longer exists")
                        val newConfigs = dto.exercises?.let { list ->
                            val resolved = list.mapNotNull { item ->
                                val ex = exerciseRepository.getExerciseById(item.exerciseId) ?: return@mapNotNull null
                                item.toRoutineExerciseConfig(ex)
                            }
                            require(resolved.isNotEmpty()) {
                                strings?.aiCommitErrorNoValidExercises ?: "No valid exercises in routine update"
                            }
                            resolved
                        } ?: existing.effectiveConfigs
                        val updated = existing.copy(
                            title = dto.title ?: existing.title,
                            splitCategory = dto.splitCategory ?: existing.splitCategory,
                            muscleFocus = dto.muscleFocus ?: existing.muscleFocus,
                            estimatedDurationMinutes = dto.estimatedDurationMinutes ?: existing.estimatedDurationMinutes,
                            scheduledDays = dto.scheduledDays ?: existing.scheduledDays,
                            exerciseTemplates = newConfigs.map { it.exercise },
                            plannedSetsPerExercise = newConfigs.firstOrNull()?.targetSets ?: existing.plannedSetsPerExercise,
                            exerciseConfigs = newConfigs,
                            isUserRenamed = dto.title != null || existing.isUserRenamed
                        )
                        routineRepository.updateRoutine(updated)
                        check(routineRepository.getRoutineById(updated.id) == updated) {
                            strings?.aiCommitErrorGeneric ?: "Routine update failed"
                        }
                        val displayName = strings?.let { updated.localizedTitle(it) } ?: updated.title
                        appliedTitles.add(displayName)
                    }
                    AiActionType.DELETE_ROUTINE -> {
                        val dto = json.decodeFromString<StageIdPayload>(op.payloadJson)
                        val existing = routineRepository.getRoutineById(dto.id)
                        val deletedName = existing?.let { r -> strings?.let { r.localizedTitle(it) } ?: r.title }
                            ?: op.previewDetail?.beforeRoutineName
                            ?: strings?.workoutRoutinesTitle
                            ?: "Routine"
                        routineRepository.deleteRoutine(dto.id)
                        check(routineRepository.getRoutineById(dto.id) == null) {
                            strings?.aiCommitErrorGeneric ?: "Routine delete failed"
                        }
                        appliedTitles.add(deletedName)
                    }
                    AiActionType.SCHEDULE_WORKOUT -> {
                        val dto = json.decodeFromString<StageScheduleWorkoutPayload>(op.payloadJson)
                        val resolvedRoutine = routineRepository.getRoutineById(dto.routineId)
                            ?: createdRoutinesByOpId[dto.routineId]?.let { routineRepository.getRoutineById(it) }
                            ?: lastCreatedRoutineId?.let { routineRepository.getRoutineById(it) }
                            ?: throw IllegalStateException(strings?.aiCommitErrorRoutineMissing ?: "Routine not found for scheduling")
                        val date = LocalDate.parse(dto.scheduledDate)
                        val displayTitle = strings?.let { resolvedRoutine.localizedTitle(it) } ?: resolvedRoutine.title
                        workoutScheduleRepository.scheduleRoutine(
                            routineId = resolvedRoutine.id,
                            routineTitle = displayTitle,
                            splitType = resolvedRoutine.splitCategory,
                            scheduledDate = date,
                            notes = dto.notes
                        )
                        appliedTitles.add("$displayTitle (${dto.scheduledDate})")
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
                        appliedTitles.add("${dto.weightKg} ${strings?.unitKg ?: "kg"}")
                    }
                    AiActionType.UPDATE_TRAINING_GOAL -> {
                        val dto = json.decodeFromString<StageUpdateGoalPayload>(op.payloadJson)
                        settingsRepository.updateProfile { it.copy(goal = dto.goal) }
                        appliedTitles.add(strings?.let { dto.goal.label(it) } ?: dto.goal.name)
                    }
                    AiActionType.ADD_EXERCISE_TO_ACTIVE_WORKOUT -> {
                        val dto = json.decodeFromString<StageIdPayload>(op.payloadJson)
                        check(workoutRepository.activeWorkout.value != null) {
                            strings?.aiCommitErrorNoActiveWorkout ?: "No active workout session"
                        }
                        val ex = exerciseRepository.getExerciseById(dto.id)
                            ?: throw IllegalStateException(strings?.aiCommitErrorGeneric ?: "Exercise not found")
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
            val joined = appliedTitles.joinToString(", ")
            true to (if (strings != null) sanitizeUserFacingText(joined, strings) else joined)
        } catch (e: Exception) {
            val rawMsg = e.message
            val safeError = if (strings != null) {
                when (rawMsg) {
                    strings.aiCommitErrorRoutineMissing,
                    strings.aiCommitErrorNoActiveWorkout,
                    strings.aiCommitErrorNoValidExercises -> rawMsg
                    else -> strings.aiCommitErrorGeneric
                }
            } else {
                rawMsg ?: "Commit failed"
            }
            false to safeError
        }
    }

    /**
     * Resolves an interactive preview detail for any proposed operation, verifying live routine state
     * when applied and synthesizing Before/After snapshots even for legacy persisted proposals.
     */
    fun resolveOperationPreview(
        op: AiProposedOperation,
        status: AiProposalStatus,
        strings: AppStrings
    ): AiOperationPreviewDetail {
        val base = op.previewDetail ?: synthesizeFallbackPreview(op, strings)
        if (status == AiProposalStatus.APPLIED && base.routineId != null && op.actionType == AiActionType.UPDATE_ROUTINE) {
            val liveRoutine = routineRepository.getRoutineById(base.routineId)
            if (liveRoutine != null) {
                return base.copy(
                    routineName = liveRoutine.localizedTitle(strings),
                    splitCategory = liveRoutine.splitCategory,
                    durationMinutes = liveRoutine.estimatedDurationMinutes,
                    scheduledDays = liveRoutine.scheduledDays,
                    updatedExercises = liveRoutine.effectiveConfigs.map { it.toPreviewItem() }
                )
            }
        }
        return base
    }

    private fun synthesizeFallbackPreview(op: AiProposedOperation, strings: AppStrings): AiOperationPreviewDetail {
        return try {
            when (op.actionType) {
                AiActionType.CREATE_ROUTINE -> {
                    val dto = json.decodeFromString<StageCreateRoutinePayload>(op.payloadJson)
                    buildCreateRoutinePreview(dto)
                }
                AiActionType.UPDATE_ROUTINE -> {
                    val dto = json.decodeFromString<StageUpdateRoutinePayload>(op.payloadJson)
                    val live = routineRepository.getRoutineById(dto.routineId)
                    val items = dto.exercises?.mapNotNull { item ->
                        exerciseRepository.getExerciseById(item.exerciseId)?.let { item.toRoutineExerciseConfig(it).toPreviewItem() }
                    } ?: live?.effectiveConfigs?.map { it.toPreviewItem() }.orEmpty()
                    AiOperationPreviewDetail(
                        routineId = dto.routineId,
                        routineName = dto.title ?: live?.localizedTitle(strings) ?: op.title,
                        splitCategory = dto.splitCategory ?: live?.splitCategory,
                        durationMinutes = dto.estimatedDurationMinutes ?: live?.estimatedDurationMinutes,
                        scheduledDays = dto.scheduledDays ?: live?.scheduledDays.orEmpty(),
                        exerciseDiffs = items.map { AiExerciseDiffEntry(AiExerciseDiffStatus.UNCHANGED, after = it) },
                        updatedExercises = items
                    )
                }
                AiActionType.DELETE_ROUTINE -> {
                    val dto = json.decodeFromString<StageIdPayload>(op.payloadJson)
                    val live = routineRepository.getRoutineById(dto.id)
                    AiOperationPreviewDetail(
                        routineId = dto.id,
                        beforeRoutineName = live?.localizedTitle(strings) ?: op.title,
                        exerciseDiffs = live?.effectiveConfigs?.map {
                            AiExerciseDiffEntry(AiExerciseDiffStatus.REMOVED, before = it.toPreviewItem())
                        }.orEmpty()
                    )
                }
                AiActionType.SCHEDULE_WORKOUT -> {
                    val dto = json.decodeFromString<StageScheduleWorkoutPayload>(op.payloadJson)
                    val rTitle = routineRepository.getRoutineById(dto.routineId)?.localizedTitle(strings) ?: op.subtitle
                    AiOperationPreviewDetail(
                        routineId = dto.routineId,
                        routineName = rTitle,
                        fieldChanges = listOf(
                            AiFieldChangePreview("routine_name", afterValue = rTitle),
                            AiFieldChangePreview("schedule_date", afterValue = dto.scheduledDate)
                        )
                    )
                }
                AiActionType.LOG_BODYWEIGHT -> {
                    val dto = json.decodeFromString<StageLogBodyweightPayload>(op.payloadJson)
                    AiOperationPreviewDetail(
                        fieldChanges = listOf(
                            AiFieldChangePreview("bodyweight", afterValue = "${dto.weightKg} kg", afterWeightKg = dto.weightKg)
                        )
                    )
                }
                AiActionType.UPDATE_TRAINING_GOAL -> {
                    val dto = json.decodeFromString<StageUpdateGoalPayload>(op.payloadJson)
                    AiOperationPreviewDetail(
                        fieldChanges = listOf(AiFieldChangePreview("goal", afterValue = dto.goal.name))
                    )
                }
                AiActionType.ADD_EXERCISE_TO_ACTIVE_WORKOUT -> {
                    val dto = json.decodeFromString<StageIdPayload>(op.payloadJson)
                    val ex = exerciseRepository.getExerciseById(dto.id)
                    val item = ex?.let {
                        AiExercisePreviewItem(
                            exerciseId = it.id,
                            name = it.name,
                            primaryMuscleCode = it.primaryMuscle.name,
                            equipmentCode = it.equipment.name,
                            sets = settingsRepository.settings.value.logging.defaultSetCount.coerceIn(1, 10),
                            reps = 10,
                            restSeconds = settingsRepository.settings.value.logging.restTimerDefaultSec
                        )
                    }
                    AiOperationPreviewDetail(
                        routineName = op.subtitle,
                        exerciseDiffs = listOfNotNull(item?.let { AiExerciseDiffEntry(AiExerciseDiffStatus.ADDED, after = it) }),
                        updatedExercises = listOfNotNull(item)
                    )
                }
                AiActionType.START_MESOCYCLE -> {
                    val dto = json.decodeFromString<StageStartMesocyclePayload>(op.payloadJson)
                    AiOperationPreviewDetail(
                        fieldChanges = listOf(
                            AiFieldChangePreview("routine_name", afterValue = dto.name),
                            AiFieldChangePreview("weeks", afterValue = dto.totalWeeks.toString()),
                            AiFieldChangePreview("deload_week", afterValue = dto.deloadWeek.toString())
                        )
                    )
                }
                AiActionType.CANCEL_SCHEDULE,
                AiActionType.DELETE_SCHEDULE -> {
                    val dto = json.decodeFromString<StageCancelOrDeleteSchedulePayload>(op.payloadJson)
                    AiOperationPreviewDetail(
                        fieldChanges = listOf(
                            AiFieldChangePreview("routine_name", afterValue = op.subtitle.ifBlank { op.title }),
                            AiFieldChangePreview(
                                "event_status",
                                afterValue = if (dto.deletePermanently || op.actionType == AiActionType.DELETE_SCHEDULE) "DELETED" else "CANCELLED"
                            )
                        )
                    )
                }
                AiActionType.UPDATE_AI_PREFERENCES -> {
                    val dto = json.decodeFromString<StageTextPayload>(op.payloadJson)
                    AiOperationPreviewDetail(
                        fieldChanges = listOf(AiFieldChangePreview("notes", afterValue = dto.text))
                    )
                }
                AiActionType.CREATE_PROGRAM -> {
                    val dto = json.decodeFromString<StageCreateProgramPayload>(op.payloadJson)
                    buildCreateProgramPreview(dto)
                }
                AiActionType.CREATE_EXERCISE -> {
                    val dto = json.decodeFromString<StageCreateExercisePayload>(op.payloadJson)
                    AiOperationPreviewDetail(
                        fieldChanges = listOf(
                            AiFieldChangePreview("exercise", afterValue = dto.name),
                            AiFieldChangePreview("category", afterValue = dto.primaryMuscle.name),
                            AiFieldChangePreview("equipment", afterValue = dto.equipment.name)
                        )
                    )
                }
                AiActionType.SAVE_EXERCISE_NOTE -> {
                    val dto = json.decodeFromString<StageSaveExerciseNotePayload>(op.payloadJson)
                    val ex = exerciseRepository.getExerciseById(dto.exerciseId)
                    AiOperationPreviewDetail(
                        fieldChanges = listOf(
                            AiFieldChangePreview("exercise", afterValue = ex?.name ?: op.title),
                            AiFieldChangePreview("notes", afterValue = dto.note)
                        )
                    )
                }
                AiActionType.LOG_READINESS -> {
                    val dto = json.decodeFromString<StageLogReadinessPayload>(op.payloadJson)
                    buildLogReadinessPreview(dto)
                }
            }
        } catch (_: Exception) {
            AiOperationPreviewDetail()
        }
    }

    private fun buildCreateProgramPreview(dto: StageCreateProgramPayload): AiOperationPreviewDetail {
        val changes = buildList {
            add(AiFieldChangePreview("routine_name", afterValue = dto.name))
            add(AiFieldChangePreview("goal", afterValue = dto.category.name))
            if (dto.activeDays.isNotEmpty()) {
                add(AiFieldChangePreview("scheduled_days", afterValue = dto.activeDays.joinToString(",")))
            }
            if (dto.description.isNotBlank()) {
                add(AiFieldChangePreview("notes", afterValue = dto.description))
            }
        }
        return AiOperationPreviewDetail(
            routineName = dto.name,
            scheduledDays = dto.activeDays,
            fieldChanges = changes
        )
    }

    private fun buildLogReadinessPreview(dto: StageLogReadinessPayload): AiOperationPreviewDetail {
        val (pct, _) = DailyReadinessCheckIn.calculate(dto.sleep, dto.soreness, dto.energy, dto.stress)
        val changes = buildList {
            add(
                AiFieldChangePreview(
                    fieldKey = "notes",
                    afterValue = "$pct% (Sleep ${dto.sleep}/5 · Soreness ${dto.soreness}/5 · Energy ${dto.energy}/5 · Stress ${dto.stress}/5)" +
                        if (dto.notes.isNotBlank()) " — ${dto.notes}" else ""
                )
            )
        }
        return AiOperationPreviewDetail(fieldChanges = changes)
    }

    private fun buildCreateRoutinePreview(dto: StageCreateRoutinePayload): AiOperationPreviewDetail {
        val afterItems = dto.exercises.mapNotNull { item ->
            val ex = exerciseRepository.getExerciseById(item.exerciseId) ?: return@mapNotNull null
            item.toRoutineExerciseConfig(ex).toPreviewItem()
        }
        return AiOperationPreviewDetail(
            routineName = dto.title,
            splitCategory = dto.splitCategory,
            durationMinutes = dto.estimatedDurationMinutes,
            scheduledDays = dto.scheduledDays,
            exerciseDiffs = afterItems.map { AiExerciseDiffEntry(AiExerciseDiffStatus.ADDED, after = it) },
            updatedExercises = afterItems,
            fieldChanges = buildList {
                add(AiFieldChangePreview("routine_name", afterValue = dto.title))
                add(AiFieldChangePreview("split", afterValue = dto.splitCategory))
                add(AiFieldChangePreview("duration", afterValue = dto.estimatedDurationMinutes.toString()))
                if (dto.scheduledDays.isNotEmpty()) {
                    add(AiFieldChangePreview("scheduled_days", afterValue = dto.scheduledDays.joinToString(",")))
                }
            }
        )
    }

    private fun buildUpdateRoutinePreview(
        existing: RoutineBlueprint,
        payload: StageUpdateRoutinePayload,
        beforeTitle: String,
        targetTitle: String
    ): AiOperationPreviewDetail {
        val beforeItems = existing.effectiveConfigs.map { it.toPreviewItem() }
        val afterItems = payload.exercises?.mapNotNull { item ->
            val ex = exerciseRepository.getExerciseById(item.exerciseId) ?: return@mapNotNull null
            item.toRoutineExerciseConfig(ex).toPreviewItem()
        } ?: beforeItems
        val diffs = buildRoutineExerciseDiffs(beforeItems, afterItems)
        val afterSplit = payload.splitCategory ?: existing.splitCategory
        val afterDuration = payload.estimatedDurationMinutes ?: existing.estimatedDurationMinutes
        val afterDays = payload.scheduledDays ?: existing.scheduledDays
        val changes = buildList {
            if (beforeTitle != targetTitle) {
                add(AiFieldChangePreview("routine_name", beforeValue = beforeTitle, afterValue = targetTitle))
            }
            if (existing.splitCategory != afterSplit) {
                add(AiFieldChangePreview("split", beforeValue = existing.splitCategory, afterValue = afterSplit))
            }
            if (existing.estimatedDurationMinutes != afterDuration) {
                add(
                    AiFieldChangePreview(
                        "duration",
                        beforeValue = existing.estimatedDurationMinutes.toString(),
                        afterValue = afterDuration.toString()
                    )
                )
            }
            if (existing.scheduledDays.sorted() != afterDays.sorted()) {
                add(
                    AiFieldChangePreview(
                        "scheduled_days",
                        beforeValue = existing.scheduledDays.joinToString(","),
                        afterValue = afterDays.joinToString(",")
                    )
                )
            }
        }
        return AiOperationPreviewDetail(
            routineId = existing.id,
            routineName = targetTitle,
            beforeRoutineName = beforeTitle,
            splitCategory = afterSplit,
            beforeSplitCategory = existing.splitCategory,
            durationMinutes = afterDuration,
            beforeDurationMinutes = existing.estimatedDurationMinutes,
            scheduledDays = afterDays,
            beforeScheduledDays = existing.scheduledDays,
            exerciseDiffs = diffs,
            updatedExercises = afterItems,
            fieldChanges = changes
        )
    }

    private fun RoutineExerciseConfig.toPreviewItem(): AiExercisePreviewItem {
        val resolvedSets = setConfigs.takeIf { it.isNotEmpty() }?.map { setCfg ->
            AiSetPreview(
                setNumber = setCfg.setNumber,
                reps = setCfg.targetReps,
                weightKg = setCfg.targetWeightKg,
                restSeconds = setCfg.restTimerSeconds ?: restTimerSeconds
            )
        } ?: emptyList()
        return AiExercisePreviewItem(
            exerciseId = exercise.id,
            name = exercise.name,
            primaryMuscleCode = exercise.primaryMuscle.name,
            equipmentCode = exercise.equipment.name,
            sets = targetSets,
            reps = targetReps,
            weightKg = targetWeightKg,
            restSeconds = restTimerSeconds,
            setDetails = resolvedSets
        )
    }

    internal fun buildRoutineExerciseDiffs(
        before: List<AiExercisePreviewItem>,
        after: List<AiExercisePreviewItem>
    ): List<AiExerciseDiffEntry> {
        val matchedAfterByBeforeIdx = IntArray(before.size) { -1 }
        val usedAfter = BooleanArray(after.size)

        // Match identical exerciseId occurrences in sequence
        before.forEachIndexed { bIdx, bItem ->
            val matchIdx = after.indices.firstOrNull { aIdx ->
                !usedAfter[aIdx] && bItem.exerciseId.isNotBlank() && after[aIdx].exerciseId == bItem.exerciseId
            }
            if (matchIdx != null) {
                matchedAfterByBeforeIdx[bIdx] = matchIdx
                usedAfter[matchIdx] = true
            }
        }

        val unmatchedBefore = before.indices.filter { matchedAfterByBeforeIdx[it] < 0 }
        val unmatchedAfter = after.indices.filter { !usedAfter[it] }
        val replacedBeforeIndices = mutableMapOf<Int, Int>()
        val replacedAfterIndices = mutableSetOf<Int>()

        if (unmatchedBefore.size == 1 && unmatchedAfter.size == 1 && before.size == after.size) {
            // Single 1-for-1 swap preserving routine size -> treat cleanly as REPLACED
            val bIdx = unmatchedBefore.first()
            val aIdx = unmatchedAfter.first()
            replacedBeforeIndices[bIdx] = aIdx
            replacedAfterIndices.add(aIdx)
        } else if (unmatchedBefore.isNotEmpty() && unmatchedAfter.isNotEmpty()) {
            // Pair same-muscle swaps first (prefer closest list position) so extra removals/additions stay distinct
            val availableAfter = unmatchedAfter.toMutableList()
            unmatchedBefore.forEach { bIdx ->
                val bMuscle = before[bIdx].primaryMuscleCode
                val bTokens = before[bIdx].name.lowercase().split(Regex("[^a-z0-9]+")).filter { it.length >= 3 }.toSet()
                val matchingMuscleIdx = if (bMuscle.isNotBlank()) {
                    availableAfter
                        .filter { aIdx -> after[aIdx].primaryMuscleCode == bMuscle }
                        .minWithOrNull(
                            compareByDescending<Int> { aIdx ->
                                val aTokens = after[aIdx].name.lowercase().split(Regex("[^a-z0-9]+")).filter { it.length >= 3 }
                                aTokens.count { it in bTokens }
                            }.thenBy { aIdx -> kotlin.math.abs(aIdx - bIdx) }
                        )
                } else {
                    null
                }
                if (matchingMuscleIdx != null) {
                    replacedBeforeIndices[bIdx] = matchingMuscleIdx
                    replacedAfterIndices.add(matchingMuscleIdx)
                    availableAfter.remove(matchingMuscleIdx)
                }
            }
        }

        val result = mutableListOf<AiExerciseDiffEntry>()
        val consumedAfter = BooleanArray(after.size)

        before.forEachIndexed { bIdx, bItem ->
            val matchedA = matchedAfterByBeforeIdx[bIdx]
            val replacedA = replacedBeforeIndices[bIdx]
            when {
                matchedA >= 0 -> {
                    val aItem = after[matchedA]
                    consumedAfter[matchedA] = true
                    val isModified = bItem.sets != aItem.sets ||
                        bItem.reps != aItem.reps ||
                        bItem.weightKg != aItem.weightKg ||
                        bItem.restSeconds != aItem.restSeconds ||
                        bItem.setDetails != aItem.setDetails
                    result.add(
                        AiExerciseDiffEntry(
                            status = if (isModified) AiExerciseDiffStatus.MODIFIED else AiExerciseDiffStatus.UNCHANGED,
                            before = bItem,
                            after = aItem
                        )
                    )
                }
                replacedA != null -> {
                    val aItem = after[replacedA]
                    consumedAfter[replacedA] = true
                    result.add(
                        AiExerciseDiffEntry(
                            status = AiExerciseDiffStatus.REPLACED,
                            before = bItem,
                            after = aItem
                        )
                    )
                }
                else -> {
                    result.add(AiExerciseDiffEntry(status = AiExerciseDiffStatus.REMOVED, before = bItem))
                }
            }
        }

        after.forEachIndexed { aIdx, aItem ->
            if (!consumedAfter[aIdx]) {
                result.add(AiExerciseDiffEntry(status = AiExerciseDiffStatus.ADDED, after = aItem))
            }
        }
        return result
    }

    /**
     * Converts accidental internal identifiers, raw JSON field keys, starter slugs, or enum codes
     * in AI-generated text into natural human-readable labels before showing to the user.
     */
    fun sanitizeUserFacingText(rawText: String, strings: AppStrings): String {
        if (rawText.isBlank()) return rawText
        var sanitized = rawText

        // Build resolution maps from live domain repositories + starter slugs
        val slugTitleMap = linkedMapOf<String, String>(
            "starter_push" to strings.routinePushDayTitle,
            "starter_pull" to strings.routinePullDayTitle,
            "starter_strength_a" to strings.routineFullBodyTitle,
            "starter_general_full" to strings.routineUpperLowerTitle,
            "program_hypertrophy" to trainingProgramRepository.activeProgram.name,
            "program_hypertrophy_default" to trainingProgramRepository.activeProgram.name
        )
        routineRepository.routines.value.forEach { r ->
            val label = r.localizedTitle(strings)
            slugTitleMap[r.id] = label
            r.presetKey?.takeIf { it.isNotBlank() }?.let { slugTitleMap[it] = label }
        }
        trainingProgramRepository.programs.value.forEach { p ->
            slugTitleMap[p.id] = p.name
            p.presetKey?.takeIf { it.isNotBlank() }?.let { slugTitleMap[it] = p.name }
        }
        exerciseRepository.exercises.value.forEach { ex ->
            // Skip bare all-digit dataset IDs (e.g. "1700", "2400") so numeric quantities like "1700 kcal" are never corrupted
            if (!ex.id.all { it.isDigit() }) {
                slugTitleMap[ex.id] = ex.name
            }
        }

        // 1. Strip parenthetical or bracketed internal key-value debug leaks e.g. "(yesterdayWorkedOut: false)"
        sanitized = PARENTHETICAL_INTERNAL_KV_REGEX.replace(sanitized, "")

        // 2. Strip bare key: true/false debug leaks e.g. "yesterdayWorkedOut: false"
        sanitized = BARE_INTERNAL_KV_REGEX.replace(sanitized) { match ->
            val key = match.groupValues[1]
            val value = match.groupValues[2].trim().lowercase()
            when {
                key == "todayWorkoutStatus" && value == "rest_day" -> strings.aiStatusRestDay
                key == "todayWorkoutStatus" && value == "scheduled" -> strings.aiStatusScheduled
                else -> ""
            }
        }

        // 3. Replace or deduplicate known slugs/IDs (e.g. "starter_pull", "ex_db_press", "routine_...")
        val orderedKeys = slugTitleMap.keys.sortedByDescending { it.length }
        orderedKeys.forEach { key ->
            val humanLabel = slugTitleMap[key] ?: return@forEach
            val escaped = Regex.escape(key)
            val shortPrefix = humanLabel.substringBefore(" (").trim()
            val parenRegex = Regex("""\s*[(\[]\s*`?$escaped`?\s*[)\]]""")
            sanitized = parenRegex.replace(sanitized) { m ->
                val lookbackStart = (m.range.first - 140).coerceAtLeast(0)
                val recentWindow = sanitized.substring(lookbackStart, m.range.first)
                if (recentWindow.contains(humanLabel, ignoreCase = true) ||
                    (shortPrefix.length >= 4 && recentWindow.contains(shortPrefix, ignoreCase = true))
                ) {
                    ""
                } else {
                    " ($humanLabel)"
                }
            }
            val bareRegex = Regex("""(?<![A-Za-z0-9_])`?$escaped`?(?![A-Za-z0-9_])""")
            sanitized = bareRegex.replace(sanitized) { humanLabel }
        }

        // 4. Strip leftover generated internal ID prefixes and raw backticked field names
        sanitized = GENERATED_PAREN_ID_REGEX.replace(sanitized, "")
        sanitized = GENERATED_BARE_ID_REGEX.replace(sanitized, "")
        sanitized = BACKTICKED_INTERNAL_KEY_REGEX.replace(sanitized, "")

        // 5. Translate standalone raw uppercase enum codes into localized labels
        TrainingGoal.entries.forEach { goal ->
            val labelText = goal.label(strings)
            sanitized = Regex("""\b${goal.name}\b""").replace(sanitized) { labelText }
        }
        ExperienceLevel.entries.forEach { level ->
            val labelText = level.label(strings)
            sanitized = Regex("""\b${level.name}\b""").replace(sanitized) { labelText }
        }
        sanitized = Regex("""\bREST_DAY\b""").replace(sanitized, strings.aiStatusRestDay)
        sanitized = Regex("""\bSCHEDULED\b""").replace(sanitized, strings.aiStatusScheduled)

        // 6. Polish spacing/punctuation artifacts without altering markdown indentation or newlines
        sanitized = EMPTY_PARENS_REGEX.replace(sanitized, "")
        sanitized = SPACE_BEFORE_PUNCT_REGEX.replace(sanitized, "$1")
        sanitized = DOUBLE_COMMA_REGEX.replace(sanitized, ", ")
        sanitized = MULTI_SPACE_MIDLINE_REGEX.replace(sanitized, " ")
        return sanitized.trim()
    }

    private companion object {
        private const val INTERNAL_KEY_ALTERNATION =
            "yesterdayWorkedOut|todayWorkedOut|yesterdayWorkouts|todayCompletedWorkouts|" +
                "todayScheduledWorkouts|todayCalendarEvents|todayWorkoutStatus|recentWorkoutsTop5|" +
                "savedChatHistories|savedRoutines|currentThreadEarlierContext|activeProgramId|" +
                "activeProgramName|activeMesocycle|todayReadiness|aiPreferencesNote|todayDate|" +
                "yesterdayDate|todayDayOfWeekEn|todayIsoDayNumber|routineId|exerciseId|eventId|" +
                "threadId|workoutExerciseId|targetWeightKg|setConfigs|targetSets|targetReps|" +
                "restTimerSeconds|isScheduledToday|scheduledDays|scheduledDayNames|splitCategory|" +
                "muscleFocus|estimatedDurationMinutes|bestWeightKg|bestReps|volumeKg|durationMin|" +
                "completedSets|readinessPct|weeklySets|weeklyScheduledToday|isDeloadActive|payloadJson"

        private val PARENTHETICAL_INTERNAL_KV_REGEX = Regex(
            """\s*[(\[]\s*`?(?:$INTERNAL_KEY_ALTERNATION|id)`?\s*[:=]\s*[^)\]\n]+[)\]]"""
        )
        private val BARE_INTERNAL_KV_REGEX = Regex(
            """\b($INTERNAL_KEY_ALTERNATION)\s*[:=]\s*([A-Za-z0-9_.\-]+)"""
        )
        private val BACKTICKED_INTERNAL_KEY_REGEX = Regex(
            """`(?:$INTERNAL_KEY_ALTERNATION)`"""
        )
        private val GENERATED_PAREN_ID_REGEX = Regex(
            """\s*[(\[]\s*`?(?:aichat|aimsg|aiop|aiprop|routine|rt|ex|we|set|pr|sched|meso|program)_[A-Za-z0-9_-]+`?\s*[)\]]"""
        )
        private val GENERATED_BARE_ID_REGEX = Regex(
            """(?<![A-Za-z0-9_])`?(?:aichat|aimsg|aiop|aiprop|sched|meso)_[A-Za-z0-9_-]+`?(?![A-Za-z0-9_])"""
        )
        private val EMPTY_PARENS_REGEX = Regex("""\s*\(\s*\)""")
        private val SPACE_BEFORE_PUNCT_REGEX = Regex(""" +([,.!?;:])""")
        private val DOUBLE_COMMA_REGEX = Regex(""",\s*,""")
        private val MULTI_SPACE_MIDLINE_REGEX = Regex("""(?<=[^\s]) {2,}""")
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
        return arr.mapNotNull { (it as? JsonPrimitive)?.intOrNull }
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
        (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

    private fun JsonObject.intVal(key: String): Int? =
        (this[key] as? JsonPrimitive)?.intOrNull

    private fun JsonObject.doubleVal(key: String): Double? =
        (this[key] as? JsonPrimitive)?.doubleOrNull

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
