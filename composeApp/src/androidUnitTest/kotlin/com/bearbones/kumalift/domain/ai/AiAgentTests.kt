package com.bearbones.kumalift.domain.ai

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.bearbones.kumalift.core.ai.AiHttpResponse
import com.bearbones.kumalift.core.ai.AiHttpTransport
import com.bearbones.kumalift.core.ai.InMemoryAiSecretStore
import com.bearbones.kumalift.core.localization.EnStrings
import com.bearbones.kumalift.core.localization.IdStrings
import com.bearbones.kumalift.data.export.DataBackupManager
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
import com.bearbones.kumalift.database.KumaDatabase
import com.bearbones.kumalift.domain.model.AppLanguage
import com.bearbones.kumalift.domain.model.Equipment
import com.bearbones.kumalift.domain.model.Exercise
import com.bearbones.kumalift.domain.model.MovementType
import com.bearbones.kumalift.domain.model.MuscleGroup
import com.bearbones.kumalift.domain.model.RoutineBlueprint
import com.bearbones.kumalift.domain.model.RoutineExerciseConfig
import com.bearbones.kumalift.domain.model.SetLog
import com.bearbones.kumalift.domain.model.UserSettings
import com.bearbones.kumalift.domain.model.Workout
import com.bearbones.kumalift.domain.model.WorkoutExercise
import com.bearbones.kumalift.features.ai.ChatMarkdownBlock
import com.bearbones.kumalift.features.ai.extractAssistantRichContent
import com.bearbones.kumalift.features.ai.parseChatMarkdown
import com.bearbones.kumalift.features.ai.splitChatMarkdownBlocks
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AiAgentTests {

    private lateinit var dbRepo: KumaDatabaseRepository
    private lateinit var exerciseRepo: ExerciseRepository
    private lateinit var routineRepo: RoutineRepository
    private lateinit var programRepo: TrainingProgramRepository
    private lateinit var scheduleRepo: WorkoutScheduleRepository
    private lateinit var mesocycleRepo: MesocycleRepository
    private lateinit var readinessRepo: DailyReadinessRepository
    private lateinit var prRepo: PersonalRecordRepository
    private lateinit var workoutRepo: WorkoutRepository
    private lateinit var settingsRepo: SettingsRepository

    private val testExercises = listOf(
        Exercise(
            id = "ex_db_press",
            name = "Dumbbell Bench Press",
            primaryMuscle = MuscleGroup.CHEST,
            equipment = Equipment.DUMBBELL,
            movementType = MovementType.COMPOUND
        ),
        Exercise(
            id = "ex_lat_raise",
            name = "Dumbbell Lateral Raise",
            primaryMuscle = MuscleGroup.SHOULDERS,
            equipment = Equipment.DUMBBELL,
            movementType = MovementType.ISOLATION
        )
    )

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        KumaDatabase.Schema.create(driver)
        dbRepo = KumaDatabaseRepository(KumaDatabase(driver))
        exerciseRepo = ExerciseRepository(initialExercises = testExercises, databaseRepo = dbRepo)
        routineRepo = RoutineRepository(initialRoutines = emptyList(), databaseRepo = dbRepo)
        settingsRepo = SettingsRepository(initialSettings = UserSettings())
        programRepo = TrainingProgramRepository(databaseRepo = dbRepo)
        scheduleRepo = WorkoutScheduleRepository(databaseRepo = dbRepo)
        mesocycleRepo = MesocycleRepository(databaseRepo = dbRepo)
        readinessRepo = DailyReadinessRepository(databaseRepo = dbRepo)
        prRepo = PersonalRecordRepository(databaseRepo = dbRepo)
        workoutRepo = WorkoutRepository(
            prRepository = prRepo,
            databaseRepo = dbRepo,
            scheduleRepository = scheduleRepo,
            mesocycleRepository = mesocycleRepo
        )
    }

    @Test
    fun testReviewBeforeCommitStagesWithoutMutatingThenAppliesOnApproval() = runBlocking {
        val capturedBodies = mutableListOf<String>()
        val scriptedResponses = ArrayDeque(
            listOf(
                // Turn 1: model requests search_exercises (with thoughtSignature preservation check)
                AiHttpResponse(
                    200,
                    """{"candidates":[{"content":{"role":"model","parts":[{"thoughtSignature":"sig_abc","functionCall":{"name":"search_exercises","args":{"equipment":"DUMBBELL"}}}]}}]}"""
                ),
                // Turn 2: model stages routine creation
                AiHttpResponse(
                    200,
                    """{"candidates":[{"content":{"role":"model","parts":[{"functionCall":{"name":"stage_create_routine","args":{"title":"Hotel Dumbbell Push","splitCategory":"Push","estimatedDurationMinutes":30,"exercises":[{"exerciseId":"ex_db_press","targetSets":3,"targetReps":10,"restTimerSeconds":75}]}}}]}}]}"""
                ),
                // Turn 3: model gives user summary
                AiHttpResponse(
                    200,
                    """{"candidates":[{"content":{"role":"model","parts":[{"text":"Draft rutin Hotel Dumbbell Push sudah siap ditinjau."}]}}]}"""
                )
            )
        )

        val transport = object : AiHttpTransport {
            override suspend fun get(url: String, headers: Map<String, String>, timeoutMs: Int): AiHttpResponse {
                error("Unexpected GET $url")
            }

            override suspend fun postJson(
                url: String,
                headers: Map<String, String>,
                body: String,
                timeoutMs: Int
            ): AiHttpResponse {
                capturedBodies.add(body)
                return scriptedResponses.removeFirst()
            }
        }

        val secretStore = InMemoryAiSecretStore("test-api-key")
        val executor = KumaAiToolExecutor(
            exerciseRepo, routineRepo, programRepo, scheduleRepo,
            mesocycleRepo, readinessRepo, prRepo, workoutRepo, settingsRepo, dbRepo
        )
        val service = KumaAiAgentService(
            geminiClient = GeminiApiClient(transport),
            toolExecutor = executor,
            secretStore = secretStore,
            settingsRepository = settingsRepo,
            databaseRepo = dbRepo
        )

        service.sendMessage(
            promptText = "Buatin rutin dumbbell push",
            imagePath = null,
            strings = IdStrings,
            language = AppLanguage.ID
        )

        // Wait briefly for coroutine on Dispatchers.Default
        repeat(40) {
            if (!service.isWorking.value && service.messages.value.size >= 2) return@repeat
            delay(25)
        }

        assertFalse(service.isWorking.value)
        // Crucial invariant: routine MUST NOT be written to RoutineRepository before user clicks approve
        assertTrue(routineRepo.routines.value.isEmpty(), "Routine must remain staged until approved")

        val assistantMsg = service.messages.value.last()
        assertEquals(AiChatRole.ASSISTANT, assistantMsg.role)
        val proposal = assistantMsg.proposal
        assertNotNull(proposal)
        assertEquals(AiProposalStatus.PENDING, proposal.status)
        assertEquals(1, proposal.operations.size)

        // Verify thoughtSignature from Turn 1 was preserved when echoed back in Turn 2
        val secondReq = Json.parseToJsonElement(capturedBodies[1]).jsonObject
        val contentsArr = secondReq["contents"]!!.jsonArray
        assertTrue(contentsArr.toString().contains("sig_abc"), "thoughtSignature must be preserved across tool turns")

        // Verify DB persistence before approval
        val reloadedBeforeApprove = dbRepo.getAllAiChatMessages()
        assertEquals(2, reloadedBeforeApprove.size)
        assertEquals(AiProposalStatus.PENDING, reloadedBeforeApprove.last().proposal?.status)

        // Approve proposal -> commits permanently
        service.approveProposal(assistantMsg.id, IdStrings)
        assertEquals(1, routineRepo.routines.value.size)
        assertEquals("Hotel Dumbbell Push", routineRepo.routines.value.first().title)
        assertEquals(AiProposalStatus.APPLIED, service.messages.value.last().proposal?.status)

        // Verify persistence after restart
        val reloadedAfterApprove = dbRepo.getAllAiChatMessages()
        assertEquals(AiProposalStatus.APPLIED, reloadedAfterApprove.last().proposal?.status)
    }

    @Test
    fun testCancelProposalLeavesRepositoriesUntouched() = runBlocking {
        val responses = ArrayDeque(
            listOf(
                AiHttpResponse(
                    200,
                    """{"candidates":[{"content":{"role":"model","parts":[{"functionCall":{"name":"stage_update_training_goal","args":{"goal":"STRENGTH"}}}]}}]}"""
                ),
                AiHttpResponse(
                    200,
                    """{"candidates":[{"content":{"role":"model","parts":[{"text":"Goal Strength siap ditinjau."}]}}]}"""
                )
            )
        )
        val transport = object : AiHttpTransport {
            override suspend fun get(url: String, headers: Map<String, String>, timeoutMs: Int): AiHttpResponse =
                error("Unexpected GET $url")

            override suspend fun postJson(url: String, headers: Map<String, String>, body: String, timeoutMs: Int) =
                responses.removeFirst()
        }
        val executor = KumaAiToolExecutor(
            exerciseRepo, routineRepo, programRepo, scheduleRepo,
            mesocycleRepo, readinessRepo, prRepo, workoutRepo, settingsRepo, dbRepo
        )
        val service = KumaAiAgentService(
            geminiClient = GeminiApiClient(transport),
            toolExecutor = executor,
            secretStore = InMemoryAiSecretStore("k"),
            settingsRepository = settingsRepo,
            databaseRepo = dbRepo
        )

        service.sendMessage("Ganti goal ke strength", null, IdStrings, AppLanguage.ID)
        repeat(40) {
            if (!service.isWorking.value && service.messages.value.size >= 2) return@repeat
            delay(25)
        }

        val initialGoal = settingsRepo.settings.value.profile.goal
        val msgId = service.messages.value.last().id
        service.cancelProposal(msgId)

        assertEquals(initialGoal, settingsRepo.settings.value.profile.goal)
        assertEquals(AiProposalStatus.CANCELLED, service.messages.value.last().proposal?.status)
    }

    @Test
    fun testMultimodalImageAndNutritionAnalysisCard() = runBlocking {
        var capturedRequest = ""
        val responses = ArrayDeque(
            listOf(
                AiHttpResponse(
                    200,
                    """{"candidates":[{"content":{"role":"model","parts":[{"functionCall":{"name":"present_nutrition_analysis","args":{"foodName":"Nasi Ayam Panggang","caloriesKcal":580,"proteinGrams":44,"carbsGrams":56,"fatGrams":16,"summary":"Tinggi protein."}}}]}}]}"""
                ),
                AiHttpResponse(
                    200,
                    """{"candidates":[{"content":{"role":"model","parts":[{"text":"Bagus buat pemulihan."}]}}]}"""
                )
            )
        )
        val transport = object : AiHttpTransport {
            override suspend fun get(url: String, headers: Map<String, String>, timeoutMs: Int): AiHttpResponse =
                error("Unexpected GET $url")

            override suspend fun postJson(url: String, headers: Map<String, String>, body: String, timeoutMs: Int): AiHttpResponse {
                if (capturedRequest.isEmpty()) capturedRequest = body
                return responses.removeFirst()
            }
        }
        val executor = KumaAiToolExecutor(
            exerciseRepo, routineRepo, programRepo, scheduleRepo,
            mesocycleRepo, readinessRepo, prRepo, workoutRepo, settingsRepo, dbRepo
        )
        val service = KumaAiAgentService(
            geminiClient = GeminiApiClient(transport),
            toolExecutor = executor,
            secretStore = InMemoryAiSecretStore("secret_key_never_backup"),
            settingsRepository = settingsRepo,
            databaseRepo = dbRepo,
            imageLoader = { "image/jpeg" to "BASE64_TEST_IMAGE" }
        )

        service.sendMessage("Cek gizi makan siang", "/tmp/meal.jpg", IdStrings, AppLanguage.ID)
        repeat(40) {
            if (!service.isWorking.value && service.messages.value.size >= 2) return@repeat
            delay(25)
        }

        assertTrue(capturedRequest.contains("BASE64_TEST_IMAGE"))
        val card = service.messages.value.last().nutritionCard
        assertNotNull(card)
        assertEquals("Nasi Ayam Panggang", card.foodName)
        assertEquals(44, card.proteinGrams)

        // Ensure API key never leaks into backup export JSON
        val backupJson = DataBackupManager(dbRepo).exportJson(settingsRepo.settings.value)
        assertFalse(backupJson.contains("secret_key_never_backup"))
    }

    @Test
    fun testGeminiErrorMappingAndVerifyConnectionViaGet() = runBlocking {
        fun getOnlyClient(
            handler: (String) -> AiHttpResponse
        ): GeminiApiClient {
            return GeminiApiClient(object : AiHttpTransport {
                override suspend fun get(url: String, headers: Map<String, String>, timeoutMs: Int) =
                    handler(url)

                override suspend fun postJson(url: String, headers: Map<String, String>, body: String, timeoutMs: Int) =
                    error("verifyConnection must use GET /models instead of POST :generateContent")
            })
        }

        val modelsPayload = """{"models":[
            {"name":"models/text-embedding-004","supportedGenerationMethods":["embedContent"]},
            {"name":"models/gemini-3.5-flash-lite","supportedGenerationMethods":["generateContent"]},
            {"name":"models/gemini-3.8-flash","supportedGenerationMethods":["generateContent"]}
        ]}"""

        val okClient = getOnlyClient { AiHttpResponse(200, modelsPayload) }
        val okRes = okClient.verifyConnection("valid-key", "gemini-3.8-flash")
        assertTrue(okRes.isSuccess)
        assertEquals("gemini-3.8-flash", okRes.getOrThrow().activeModelId)
        assertFalse(okRes.getOrThrow().modelAutoSwitched)

        // When stored model is retired (not in live list), verifyConnection auto-picks preferred live model
        val switchRes = okClient.verifyConnection("valid-key", "gemini-2.5-flash")
        assertTrue(switchRes.isSuccess)
        assertEquals("gemini-3.5-flash-lite", switchRes.getOrThrow().activeModelId)
        assertTrue(switchRes.getOrThrow().modelAutoSwitched)

        val resInvalid = getOnlyClient { AiHttpResponse(403, """{"error":{"message":"API key not valid"}}""") }
            .verifyConnection("bad", GeminiApiClient.DEFAULT_MODEL)
        assertTrue(resInvalid.isFailure)
        val invalidEx = resInvalid.exceptionOrNull() as GeminiApiException
        assertEquals(GeminiErrorType.INVALID_KEY, invalidEx.errorType)
        assertEquals(403, invalidEx.httpStatusCode)

        val resQuota = getOnlyClient { AiHttpResponse(429, """{"error":{"message":"Quota exceeded"}}""") }
            .verifyConnection("ok", GeminiApiClient.DEFAULT_MODEL)
        assertTrue(resQuota.isFailure)
        assertEquals(GeminiErrorType.QUOTA_EXCEEDED, (resQuota.exceptionOrNull() as GeminiApiException).errorType)

        // When models list returns empty, fallback checks specific model and maps 404 cleanly
        val resNotFound = getOnlyClient { url ->
            if (url.contains("pageSize")) {
                AiHttpResponse(200, """{"models":[]}""")
            } else {
                AiHttpResponse(404, """{"error":{"message":"models/gemini-2.5-flash is not found"}}""")
            }
        }.verifyConnection("ok", "gemini-2.5-flash")
        assertEquals(GeminiErrorType.MODEL_UNAVAILABLE, (resNotFound.exceptionOrNull() as GeminiApiException).errorType)

        val resRegion = getOnlyClient { AiHttpResponse(400, """{"error":{"message":"User location is not supported for the API use."}}""") }
            .verifyConnection("ok", GeminiApiClient.DEFAULT_MODEL)
        assertEquals(GeminiErrorType.LOCATION_UNSUPPORTED, (resRegion.exceptionOrNull() as GeminiApiException).errorType)

        val resBadReq = getOnlyClient { AiHttpResponse(400, """{"error":{"message":"Invalid argument"}}""") }
            .verifyConnection("ok", GeminiApiClient.DEFAULT_MODEL)
        assertEquals(GeminiErrorType.BAD_REQUEST, (resBadReq.exceptionOrNull() as GeminiApiException).errorType)
    }

    @Test
    fun testChatSelfHealsRetiredModel404AndPersistsWorkingModel() = runBlocking {
        settingsRepo.update { it.copy(ai = it.ai.copy(modelId = "gemini-2.5-flash")) }
        val postedUrls = mutableListOf<String>()
        val transport = object : AiHttpTransport {
            override suspend fun get(url: String, headers: Map<String, String>, timeoutMs: Int): AiHttpResponse {
                return AiHttpResponse(
                    200,
                    """{"models":[{"name":"models/gemini-3.8-flash","supportedGenerationMethods":["generateContent"]}]}"""
                )
            }

            override suspend fun postJson(url: String, headers: Map<String, String>, body: String, timeoutMs: Int): AiHttpResponse {
                postedUrls.add(url)
                return if (url.contains("gemini-2.5-flash")) {
                    AiHttpResponse(404, """{"error":{"message":"models/gemini-2.5-flash is not found"}}""")
                } else {
                    AiHttpResponse(
                        200,
                        """{"candidates":[{"content":{"role":"model","parts":[{"text":"Siap, ini jadwal latihan hari ini."}]}}]}"""
                    )
                }
            }
        }

        val executor = KumaAiToolExecutor(
            exerciseRepo, routineRepo, programRepo, scheduleRepo,
            mesocycleRepo, readinessRepo, prRepo, workoutRepo, settingsRepo, dbRepo
        )
        val service = KumaAiAgentService(
            geminiClient = GeminiApiClient(transport),
            toolExecutor = executor,
            secretStore = InMemoryAiSecretStore("valid-key"),
            settingsRepository = settingsRepo,
            databaseRepo = dbRepo
        )

        service.sendMessage("cek latihan hari ini", null, IdStrings, AppLanguage.ID)
        repeat(40) {
            if (!service.isWorking.value && service.messages.value.size >= 2) return@repeat
            delay(25)
        }

        assertEquals(2, postedUrls.size)
        assertTrue(postedUrls[0].contains("gemini-2.5-flash"))
        assertTrue(postedUrls[1].contains("gemini-3.8-flash"))
        assertEquals("gemini-3.8-flash", settingsRepo.settings.value.ai.modelId)
        val lastMsg = service.messages.value.last()
        assertFalse(lastMsg.isError)
        assertEquals("Siap, ini jadwal latihan hari ini.", lastMsg.text)
    }

    @Test
    fun testModelCategorizationGroupsRecommendedThreeFreeAndPaid() {
        val rawCatalog = listOf(
            "gemini-4-carbon",
            "gemini-4-neon",
            "gemini-3.5-flash-lite",
            "gemini-3.5-flash-lite-001",
            "gemini-3.8-flash",
            "gemini-4-hydrogen",
            "gemini-2.5-pro",
            "gemini-3.5-flash-exp"
        )
        val groups = GeminiApiClient.categorizeModels(rawCatalog, activeModelId = "gemini-3.8-flash")

        assertEquals(3, groups.recommended.size)
        assertEquals(listOf("gemini-3.5-flash-lite", "gemini-3.8-flash", "gemini-4-hydrogen"), groups.recommended)
        assertFalse(groups.freeTier.contains("gemini-3.5-flash-lite-001"), "Snapshot duplicate -001 should be hidden")
        assertTrue(groups.freeTier.containsAll(listOf("gemini-3.5-flash-lite", "gemini-3.8-flash", "gemini-4-hydrogen", "gemini-3.5-flash-exp")))
        assertFalse(groups.freeTier.contains("gemini-4-neon"))
        assertFalse(groups.freeTier.contains("gemini-4-carbon"))
        assertTrue(groups.paidOnly.containsAll(listOf("gemini-4-neon", "gemini-4-carbon", "gemini-2.5-pro")))
    }

    @Test
    fun testOfflineAndTimeoutMappingAreDistinguishedFromGenericNetwork() = runBlocking {
        val offlineClient = GeminiApiClient(object : AiHttpTransport {
            override fun isNetworkOnline(): Boolean = false
            override suspend fun get(url: String, headers: Map<String, String>, timeoutMs: Int) =
                error("Should short-circuit when offline")
            override suspend fun postJson(url: String, headers: Map<String, String>, body: String, timeoutMs: Int) =
                error("Should short-circuit when offline")
        })
        val offlineRes = offlineClient.verifyConnection("k", GeminiApiClient.DEFAULT_MODEL)
        assertTrue(offlineRes.isFailure)
        assertEquals(GeminiErrorType.OFFLINE, (offlineRes.exceptionOrNull() as GeminiApiException).errorType)

        val timeoutClient = GeminiApiClient(object : AiHttpTransport {
            override suspend fun get(url: String, headers: Map<String, String>, timeoutMs: Int): AiHttpResponse {
                throw java.net.SocketTimeoutException("Read timed out")
            }
            override suspend fun postJson(url: String, headers: Map<String, String>, body: String, timeoutMs: Int): AiHttpResponse {
                throw java.net.SocketTimeoutException("Read timed out")
            }
        })
        val timeoutRes = timeoutClient.verifyConnection("k", GeminiApiClient.DEFAULT_MODEL)
        assertTrue(timeoutRes.isFailure)
        assertEquals(GeminiErrorType.TIMEOUT, (timeoutRes.exceptionOrNull() as GeminiApiException).errorType)
    }

    @Test
    fun testCasualCheckMessageUsesLowThinkingAndSkipsToolSchemasForSpeed() = runBlocking {
        var capturedBody = ""
        val transport = object : AiHttpTransport {
            override suspend fun get(url: String, headers: Map<String, String>, timeoutMs: Int) =
                error("Unexpected GET")
            override suspend fun postJson(url: String, headers: Map<String, String>, body: String, timeoutMs: Int): AiHttpResponse {
                capturedBody = body
                return AiHttpResponse(
                    200,
                    """{"candidates":[{"content":{"role":"model","parts":[{"text":"Halo! Siap bantu."}]}}]}"""
                )
            }
        }
        val executor = KumaAiToolExecutor(
            exerciseRepo, routineRepo, programRepo, scheduleRepo,
            mesocycleRepo, readinessRepo, prRepo, workoutRepo, settingsRepo, dbRepo
        )
        val service = KumaAiAgentService(
            geminiClient = GeminiApiClient(transport),
            toolExecutor = executor,
            secretStore = InMemoryAiSecretStore("k"),
            settingsRepository = settingsRepo,
            databaseRepo = dbRepo
        )

        service.sendMessage("test", null, IdStrings, AppLanguage.ID)
        repeat(40) {
            if (!service.isWorking.value && service.messages.value.size >= 2) return@repeat
            delay(25)
        }

        // Default model is now gemini-2.5-flash -> thinkingBudget: 0
        assertEquals("gemini-2.5-flash", GeminiApiClient.DEFAULT_MODEL)
        assertEquals("gemini-2.5-flash", UserSettings().ai.modelId)
        assertTrue(capturedBody.contains(""""thinkingBudget":0"""))
        assertFalse(capturedBody.contains("functionDeclarations"))
        assertFalse(capturedBody.contains("Live KumaLift DB Snapshot"))
        assertEquals("Halo! Siap bantu.", service.messages.value.last().text)

        settingsRepo.update { it.copy(ai = it.ai.copy(modelId = "gemini-3.5-flash-lite")) }
        service.sendMessage("halo", null, IdStrings, AppLanguage.ID)
        repeat(40) {
            if (!service.isWorking.value && service.messages.value.size >= 4) return@repeat
            delay(25)
        }
        assertTrue(capturedBody.contains(""""thinkingLevel":"minimal""""))
    }

    @Test
    fun testMultiRoutineCreateAndScheduleLinksEachRoutineDeterministically() = runBlocking {
        val secretStore = InMemoryAiSecretStore("initial")
        assertTrue(secretStore.hasApiKeyFlow.value)
        secretStore.clearGeminiApiKey()
        assertFalse(secretStore.hasApiKeyFlow.value)
        secretStore.saveGeminiApiKey("valid")
        assertTrue(secretStore.hasApiKeyFlow.value)

        val executor = KumaAiToolExecutor(
            exerciseRepo, routineRepo, programRepo, scheduleRepo,
            mesocycleRepo, readinessRepo, prRepo, workoutRepo, settingsRepo, dbRepo
        )
        val session = KumaAiToolSessionState()

        val createPush = executor.executeCall(
            GeminiFunctionCall(
                "stage_create_routine",
                Json.parseToJsonElement("""{"title":"Day 1 Chest","splitCategory":"Push","exercises":[{"exerciseId":"ex_db_press"}]}""").jsonObject
            ),
            session,
            IdStrings
        ).jsonObject
        val pushRef = createPush["pendingRoutineId"]!!.jsonPrimitive.content

        val createShoulders = executor.executeCall(
            GeminiFunctionCall(
                "stage_create_routine",
                Json.parseToJsonElement("""{"title":"Day 2 Shoulders","splitCategory":"Push","exercises":[{"exerciseId":"ex_lat_raise"}]}""").jsonObject
            ),
            session,
            IdStrings
        ).jsonObject
        val shoulderRef = createShoulders["pendingRoutineId"]!!.jsonPrimitive.content

        // Schedule first routine AFTER creating second routine using explicit pendingRoutineId
        executor.executeCall(
            GeminiFunctionCall(
                "stage_schedule_workout",
                Json.parseToJsonElement("""{"routineId":"$pushRef","scheduledDate":"2026-12-01"}""").jsonObject
            ),
            session,
            IdStrings
        )
        executor.executeCall(
            GeminiFunctionCall(
                "stage_schedule_workout",
                Json.parseToJsonElement("""{"routineId":"$shoulderRef","scheduledDate":"2026-12-03"}""").jsonObject
            ),
            session,
            IdStrings
        )

        val (ok, _) = executor.commitProposal(
            AiActionProposal(id = "prop_multi", operations = session.stagedOperations)
        )
        assertTrue(ok)
        assertEquals(2, routineRepo.routines.value.size)
        val dec1 = scheduleRepo.events.value.first { it.scheduledDate == "2026-12-01" }
        val dec3 = scheduleRepo.events.value.first { it.scheduledDate == "2026-12-03" }
        assertEquals("Day 1 Chest", dec1.routineTitleSnapshot)
        assertEquals("Day 2 Shoulders", dec3.routineTitleSnapshot)
    }

    @Test
    fun testAuthoritativeDateDayAndTodayScheduleGroundingForHomeMatch() = runBlocking {
        // 2026-11-27 is Friday (Jumat), ISO dayOfWeek = 5
        val fixedNow = LocalDateTime(2026, 11, 27, 13, 48)
        routineRepo.addRoutine(
            RoutineBlueprint(
                id = "r_push_thu",
                title = "Push Day (Chest, Shoulders)",
                splitCategory = "Push",
                muscleFocus = "Chest",
                scheduledDays = listOf(1, 4),
                exerciseConfigs = listOf(RoutineExerciseConfig(exercise = testExercises[0]))
            )
        )
        routineRepo.addRoutine(
            RoutineBlueprint(
                id = "r_pull_fri",
                title = "Pull Day (Back, Biceps)",
                splitCategory = "Pull",
                muscleFocus = "Back",
                scheduledDays = listOf(2, 5),
                exerciseConfigs = listOf(RoutineExerciseConfig(exercise = testExercises[1]))
            )
        )

        var capturedSystemText = ""
        var capturedTimeout = 0
        var capturedBody = ""
        settingsRepo.update { it.copy(ai = it.ai.copy(modelId = "gemini-3.8-flash")) }

        val transport = object : AiHttpTransport {
            override suspend fun get(url: String, headers: Map<String, String>, timeoutMs: Int) =
                error("Unexpected GET")
            override suspend fun postJson(url: String, headers: Map<String, String>, body: String, timeoutMs: Int): AiHttpResponse {
                capturedTimeout = timeoutMs
                capturedBody = body
                if (capturedSystemText.isEmpty()) {
                    capturedSystemText = Json.parseToJsonElement(body).jsonObject["systemInstruction"]!!.toString()
                }
                return AiHttpResponse(
                    200,
                    """{"candidates":[{"content":{"role":"model","parts":[{"text":"Hari ini Jumat, 27 November 2026: Pull Day (Back, Biceps)."}]}}]}"""
                )
            }
        }

        val executor = KumaAiToolExecutor(
            exerciseRepo, routineRepo, programRepo, scheduleRepo,
            mesocycleRepo, readinessRepo, prRepo, workoutRepo, settingsRepo, dbRepo,
            nowProvider = { fixedNow }
        )

        val overview = executor.executeCall(
            GeminiFunctionCall("get_user_overview", Json.parseToJsonElement("{}").jsonObject),
            KumaAiToolSessionState(),
            IdStrings
        ).jsonObject
        assertEquals("2026-11-27", overview["todayDate"]!!.jsonPrimitive.content)
        assertEquals("Jumat", overview["todayDayOfWeek"]!!.jsonPrimitive.content)
        assertEquals("5", overview["todayIsoDayNumber"]!!.jsonPrimitive.content)
        val todayListStr = overview["todayScheduledWorkouts"]!!.jsonArray.toString()
        assertTrue(todayListStr.contains("Pull Day (Back, Biceps)"), "Friday must match Pull Day")
        assertFalse(todayListStr.contains("Push Day"), "Thursday Push Day must not match Friday")

        val service = KumaAiAgentService(
            geminiClient = GeminiApiClient(transport),
            toolExecutor = executor,
            secretStore = InMemoryAiSecretStore("k"),
            settingsRepository = settingsRepo,
            databaseRepo = dbRepo,
            nowProvider = { fixedNow }
        )
        service.sendMessage("hari ini hari apa emang?", null, IdStrings, AppLanguage.ID)
        repeat(40) {
            if (!service.isWorking.value && service.messages.value.size >= 2) return@repeat
            delay(25)
        }

        val fastSnapshot = Json.parseToJsonElement(executor.buildFastContextSnapshot(IdStrings)).jsonObject
        val snapToday = fastSnapshot["todayScheduledWorkouts"]!!.jsonArray.toString()
        val snapRoutines = fastSnapshot["savedRoutines"]!!.jsonArray.toString()
        assertTrue(snapToday.contains("Pull Day (Back, Biceps)"), "Fast snapshot must resolve today's Pull Day")
        assertFalse(snapToday.contains("Push Day"), "Fast snapshot must exclude Thursday Push Day from todayScheduledWorkouts")
        assertTrue(snapRoutines.contains("Push Day"), "Fast snapshot must still summarize saved routines")

        assertTrue(capturedTimeout >= 60_000, "Thinking models need >=60s timeout")
        assertTrue(capturedSystemText.contains("Jumat (Friday), 2026-11-27"), "System prompt must ground authoritative weekday & date")
        assertTrue(capturedSystemText.contains("Pull Day (Back, Biceps)"), "Fast local snapshot must embed today's resolved Home routine immediately")
        assertTrue(capturedBody.contains(""""thinkingLevel":"low""""), "3.8-flash must keep fast low thinking level")
        assertFalse(capturedBody.contains(""""temperature""""), "3.8-flash should omit deprecated temperature")
    }

    @Test
    fun testWebSearchGroundingCombinesWithToolsAndFallsBackCleanlyOn400() = runBlocking {
        val postedBodies = mutableListOf<String>()
        val responses = ArrayDeque(
            listOf(
                // 1) Web-grounded answer returning groundingMetadata sources
                AiHttpResponse(
                    200,
                    """{"candidates":[{"content":{"role":"model","parts":[{"text":"Creatine 3-5g/hari aman dan efektif untuk kekuatan."}]},"groundingMetadata":{"groundingChunks":[{"web":{"uri":"https://examine.com/supplements/creatine","title":"examine.com"}},{"web":{"uri":"https://jissn.biomedcentral.com","title":"jissn.biomedcentral.com"}}]}}]}"""
                ),
                // 2) Simulate an endpoint rejecting combined googleSearch + functionDeclarations
                AiHttpResponse(
                    400,
                    """{"error":{"message":"Tool use with Google Search and function calling is unsupported together"}}"""
                ),
                // 3) Retry succeeds cleanly without googleSearch
                AiHttpResponse(
                    200,
                    """{"candidates":[{"content":{"role":"model","parts":[{"text":"Fallback tanpa web search berhasil."}]}}]}"""
                )
            )
        )

        val transport = object : AiHttpTransport {
            override suspend fun get(url: String, headers: Map<String, String>, timeoutMs: Int) =
                error("Unexpected GET")
            override suspend fun postJson(url: String, headers: Map<String, String>, body: String, timeoutMs: Int): AiHttpResponse {
                postedBodies.add(body)
                return responses.removeFirst()
            }
        }

        val executor = KumaAiToolExecutor(
            exerciseRepo, routineRepo, programRepo, scheduleRepo,
            mesocycleRepo, readinessRepo, prRepo, workoutRepo, settingsRepo, dbRepo
        )
        val service = KumaAiAgentService(
            geminiClient = GeminiApiClient(transport),
            toolExecutor = executor,
            secretStore = InMemoryAiSecretStore("k"),
            settingsRepository = settingsRepo,
            databaseRepo = dbRepo
        )

        service.sendMessage("dosis creatine harian berdasarkan riset terbaru?", null, IdStrings, AppLanguage.ID)
        repeat(40) {
            if (!service.isWorking.value && service.messages.value.size >= 2) return@repeat
            delay(25)
        }

        assertTrue(postedBodies[0].contains(""""googleSearch":{}"""))
        assertFalse(postedBodies[0].contains(""""urlContext""""))
        assertTrue(postedBodies[0].contains("functionDeclarations"))
        val webAnswer = service.messages.value.last()
        assertFalse(webAnswer.isError)
        assertTrue(webAnswer.text.contains("Sumber: [examine.com](https://examine.com/supplements/creatine) · [jissn.biomedcentral.com](https://jissn.biomedcentral.com)"))

        service.sendMessage("cek rutin dumbbell di https://example.com/push-pull", null, IdStrings, AppLanguage.ID)
        repeat(40) {
            if (!service.isWorking.value && service.messages.value.size >= 4) return@repeat
            delay(25)
        }

        assertEquals(3, postedBodies.size)
        assertTrue(postedBodies[1].contains(""""urlContext":{}"""))
        assertTrue(postedBodies[1].contains(""""googleSearch":{}"""))
        assertFalse(postedBodies[2].contains(""""googleSearch""""))
        assertFalse(postedBodies[2].contains(""""urlContext""""))
        assertTrue(postedBodies[2].contains("functionDeclarations"))
        assertEquals("Fallback tanpa web search berhasil.", service.messages.value.last().text)
    }

    @Test
    fun testFreeTier429OnGoogleSearchAutoRetriesWithoutSearchAnd503RetriesOnce() = runBlocking {
        val postedBodies = mutableListOf<String>()
        val responses = ArrayDeque(
            listOf(
                // 1) Turn 1 tries googleSearch on free key -> 429
                AiHttpResponse(429, """{"error":{"message":"Quota exceeded for grounding","status":"RESOURCE_EXHAUSTED"}}"""),
                // 2) Auto-retry without googleSearch succeeds -> caches free-tier search skip
                AiHttpResponse(200, """{"candidates":[{"content":{"role":"model","parts":[{"text":"Hari ini ada Pull Day."}]}}]}"""),
                // 3) Turn 2 sends straight without googleSearch and hits transient 503
                AiHttpResponse(503, """{"error":{"message":"The model is overloaded."}}"""),
                // 4) Auto-retry after brief delay succeeds
                AiHttpResponse(200, """{"candidates":[{"content":{"role":"model","parts":[{"text":"Lancar setelah retry 503."}]}}]}""")
            )
        )

        val transport = object : AiHttpTransport {
            override suspend fun get(url: String, headers: Map<String, String>, timeoutMs: Int) =
                error("Unexpected GET")
            override suspend fun postJson(url: String, headers: Map<String, String>, body: String, timeoutMs: Int): AiHttpResponse {
                postedBodies.add(body)
                return responses.removeFirst()
            }
        }

        val executor = KumaAiToolExecutor(
            exerciseRepo, routineRepo, programRepo, scheduleRepo,
            mesocycleRepo, readinessRepo, prRepo, workoutRepo, settingsRepo, dbRepo
        )
        val client = GeminiApiClient(transport)
        val service = KumaAiAgentService(
            geminiClient = client,
            toolExecutor = executor,
            secretStore = InMemoryAiSecretStore("free-key-1"),
            settingsRepository = settingsRepo,
            databaseRepo = dbRepo
        )

        service.sendMessage("hari ini ada latihan kah?", null, IdStrings, AppLanguage.ID)
        repeat(60) {
            if (!service.isWorking.value && service.messages.value.size >= 2) return@repeat
            delay(25)
        }
        assertEquals("Hari ini ada Pull Day.", service.messages.value.last().text)
        assertTrue(postedBodies[0].contains(""""googleSearch":{}"""))
        assertFalse(postedBodies[1].contains(""""googleSearch""""))

        service.sendMessage("cek target reps bench press?", null, IdStrings, AppLanguage.ID)
        repeat(80) {
            if (!service.isWorking.value && service.messages.value.size >= 4) return@repeat
            delay(25)
        }
        assertEquals(4, postedBodies.size)
        // Must have skipped googleSearch immediately on Turn 2
        assertFalse(postedBodies[2].contains(""""googleSearch""""))
        assertEquals("Lancar setelah retry 503.", service.messages.value.last().text)

        // Persistent 503 maps to SERVER_BUSY (not NETWORK)
        val busyClient = GeminiApiClient(object : AiHttpTransport {
            override suspend fun get(url: String, headers: Map<String, String>, timeoutMs: Int) =
                AiHttpResponse(503, """{"error":{"message":"Service unavailable"}}""")
            override suspend fun postJson(url: String, headers: Map<String, String>, body: String, timeoutMs: Int) =
                AiHttpResponse(503, """{"error":{"message":"Service unavailable"}}""")
        })
        val busyRes = busyClient.verifyConnection("k", GeminiApiClient.DEFAULT_MODEL)
        assertTrue(busyRes.isFailure)
        assertEquals(GeminiErrorType.SERVER_BUSY, (busyRes.exceptionOrNull() as GeminiApiException).errorType)
    }

    @Test
    fun testQuota429And404RotatesAcrossFreeModelsAndPreservesSettingOnTotalExhaustion() = runBlocking {
        settingsRepo.update { it.copy(ai = it.ai.copy(modelId = "gemini-2.5-flash")) }
        val postedUrls = mutableListOf<String>()

        val transport = object : AiHttpTransport {
            override suspend fun get(url: String, headers: Map<String, String>, timeoutMs: Int) =
                AiHttpResponse(
                    200,
                    """{"models":[
                        {"name":"models/gemini-3.5-flash-lite","supportedGenerationMethods":["generateContent"]},
                        {"name":"models/gemini-3.8-flash","supportedGenerationMethods":["generateContent"]},
                        {"name":"models/gemini-4-neon","supportedGenerationMethods":["generateContent"]}
                    ]}"""
                )

            override suspend fun postJson(url: String, headers: Map<String, String>, body: String, timeoutMs: Int): AiHttpResponse {
                postedUrls.add(url)
                return when {
                    url.contains("gemini-2.5-flash") ->
                        AiHttpResponse(404, """{"error":{"message":"Model not found"}}""")
                    url.contains("gemini-3.5-flash-lite") ->
                        AiHttpResponse(429, """{"error":{"message":"Quota exceeded, limit: 20"}}""")
                    url.contains("gemini-3.8-flash") ->
                        AiHttpResponse(200, """{"candidates":[{"content":{"role":"model","parts":[{"text":"Berhasil lewat 3.8-flash."}]}}]}""")
                    else ->
                        AiHttpResponse(400, """{"error":{"message":"Should never hit paid neon fallback"}}""")
                }
            }
        }

        val executor = KumaAiToolExecutor(
            exerciseRepo, routineRepo, programRepo, scheduleRepo,
            mesocycleRepo, readinessRepo, prRepo, workoutRepo, settingsRepo, dbRepo
        )
        val service = KumaAiAgentService(
            geminiClient = GeminiApiClient(transport),
            toolExecutor = executor,
            secretStore = InMemoryAiSecretStore("free-key"),
            settingsRepository = settingsRepo,
            databaseRepo = dbRepo
        )

        service.sendMessage("hari ini ada latihan?", null, IdStrings, AppLanguage.ID)
        repeat(60) {
            if (!service.isWorking.value && service.messages.value.size >= 2) return@repeat
            delay(25)
        }

        assertEquals("Berhasil lewat 3.8-flash.", service.messages.value.last().text)
        assertEquals("gemini-3.8-flash", settingsRepo.settings.value.ai.modelId)
        assertFalse(postedUrls.any { it.contains("gemini-4-neon") }, "Free fallback must skip paid models")

        // Verify detailed 429 summary exposes Google's reason cleanly
        val quotaEx = GeminiApiException(GeminiErrorType.QUOTA_EXCEEDED, "Quota exceeded for free_tier_requests, limit: 20", 429)
        assertTrue(quotaEx.localizedSummary(IdStrings).contains("limit: 20"))
    }

    @Test
    fun testMarkdownParserSupportsBoldItalicUnderlineStrikeCodeBulletsAndHeadings() {
        val sample = """
            # Jadwal Hari Ini
            Latihan **Pull Day**, program *Hypertrophy*, fokus ***otot punggung***, catatan __penting__, <u>wajib</u>, ~~salah~~, snake_case_id, `gemini-2.5-flash`, 3 * 4
            - Dumbbell Row
            * Hammer Curl
        """.trimIndent()
        val parsed = parseChatMarkdown(sample, Color.DarkGray)
        val plain = parsed.text

        assertFalse(plain.contains("**"), "Raw ** markers must be stripped")
        assertFalse(plain.contains("__"), "Raw __ underline markers must be stripped")
        assertFalse(plain.contains("<u>"), "Raw <u> underline tags must be stripped")
        assertFalse(plain.contains("~~"), "Raw ~~ strike markers must be stripped")
        assertFalse(plain.contains("# "), "Raw heading hashes must be stripped")
        assertTrue(plain.contains("snake_case_id"), "Underscores inside identifiers stay untouched")
        assertTrue(plain.contains("3 * 4"), "Isolated spaced math asterisk stays literal")
        assertTrue(plain.contains("• Dumbbell Row"))
        assertTrue(plain.contains("• Hammer Curl"))

        val styles = parsed.spanStyles
        assertTrue(styles.any { it.item.fontWeight == FontWeight.Bold && plain.substring(it.start, it.end) == "Pull Day" })
        assertTrue(styles.any { it.item.fontStyle == FontStyle.Italic && plain.substring(it.start, it.end) == "Hypertrophy" })
        assertTrue(styles.any { it.item.fontWeight == FontWeight.Bold && it.item.fontStyle == FontStyle.Italic && plain.substring(it.start, it.end) == "otot punggung" })
        assertTrue(styles.any { it.item.textDecoration == TextDecoration.Underline && plain.substring(it.start, it.end) == "penting" })
        assertTrue(styles.any { it.item.textDecoration == TextDecoration.Underline && plain.substring(it.start, it.end) == "wajib" })
        assertTrue(styles.any { it.item.textDecoration == TextDecoration.LineThrough && plain.substring(it.start, it.end) == "salah" })
    }

    @Test
    fun testTransientQuota429FailoverDoesNotOverwriteSelected25ModelAndAppliesReasoningDepth() = runBlocking {
        settingsRepo.update { it.copy(ai = it.ai.copy(modelId = "gemini-2.5-flash")) }
        val postedRequests = mutableListOf<Pair<String, String>>()

        val transport = object : AiHttpTransport {
            override suspend fun get(url: String, headers: Map<String, String>, timeoutMs: Int) =
                AiHttpResponse(
                    200,
                    """{"models":[
                        {"name":"models/gemini-2.5-flash","supportedGenerationMethods":["generateContent"]},
                        {"name":"models/gemini-2.5-flash-lite","supportedGenerationMethods":["generateContent"]},
                        {"name":"models/gemini-3.5-flash-lite","supportedGenerationMethods":["generateContent"]}
                    ]}"""
                )

            override suspend fun postJson(url: String, headers: Map<String, String>, body: String, timeoutMs: Int): AiHttpResponse {
                postedRequests.add(url to body)
                return when {
                    postedRequests.size == 1 ->
                        AiHttpResponse(200, """{"candidates":[{"content":{"role":"model","parts":[{"text":"Otot dada pulih 95%."}]}}]}""")
                    url.contains("gemini-2.5-flash:") ->
                        AiHttpResponse(429, """{"error":{"message":"Quota exceeded"}}""")
                    else ->
                        AiHttpResponse(200, """{"candidates":[{"content":{"role":"model","parts":[{"text":"Berhasil lewat 2.5-flash-lite."}]}}]}""")
                }
            }
        }

        val executor = KumaAiToolExecutor(
            exerciseRepo, routineRepo, programRepo, scheduleRepo,
            mesocycleRepo, readinessRepo, prRepo, workoutRepo, settingsRepo, dbRepo
        )
        val service = KumaAiAgentService(
            geminiClient = GeminiApiClient(transport),
            toolExecutor = executor,
            secretStore = InMemoryAiSecretStore("free-key"),
            settingsRepository = settingsRepo,
            databaseRepo = dbRepo
        )

        // Balanced non-casual query uses non-zero thinking budget (1024 on 2.5-flash)
        service.sendMessage("cek otot pulih", null, IdStrings, AppLanguage.ID)
        repeat(60) {
            if (!service.isWorking.value && service.messages.value.size >= 2) return@repeat
            delay(25)
        }
        assertTrue(postedRequests[0].second.contains(""""thinkingBudget":1024"""))

        // Complex routine prompt uses deep thinking (2048 on 2.5-flash, 1024 on 2.5-flash-lite),
        // and transient 429 rotation NEVER overwrites user's selected gemini-2.5-flash setting
        service.sendMessage("edit rutin push kemarin 60kg", null, IdStrings, AppLanguage.ID)
        repeat(60) {
            if (!service.isWorking.value && service.messages.value.size >= 4) return@repeat
            delay(25)
        }
        assertTrue(postedRequests[1].first.contains("gemini-2.5-flash"))
        assertTrue(postedRequests[1].second.contains(""""thinkingBudget":2048"""))
        val failoverReq = postedRequests.last()
        assertTrue(failoverReq.first.contains("gemini-2.5-flash-lite"))
        assertTrue(failoverReq.second.contains(""""thinkingBudget":1024"""))
        assertEquals("Berhasil lewat 2.5-flash-lite.", service.messages.value.last().text)
        assertEquals("gemini-2.5-flash", settingsRepo.settings.value.ai.modelId)
    }

    @Test
    fun testYesterdayWorkoutIsDetectedInFastSnapshotAndRecentWorkouts() {
        val tz = TimeZone.currentSystemDefault()
        val fixedNow = LocalDateTime(2026, 11, 27, 13, 48)
        val olderEpoch = LocalDateTime(2026, 11, 20, 17, 0).toInstant(tz).toEpochMilliseconds()
        val yesterdayEpoch = LocalDateTime(2026, 11, 26, 18, 30).toInstant(tz).toEpochMilliseconds()

        val oldWorkout = Workout(
            id = "w_old",
            title = "Pull Lama",
            startTimeEpochMs = olderEpoch,
            durationSeconds = 2400,
            isFinished = true,
            exercises = listOf(
                WorkoutExercise(
                    id = "we_old",
                    exercise = testExercises[1],
                    sets = listOf(SetLog(id = "s_old", setNumber = 1, weightKg = 10.0, reps = 12, isCompleted = true))
                )
            )
        )
        val yesterdayWorkout = Workout(
            id = "w_yday",
            title = "Push Kemarin",
            startTimeEpochMs = yesterdayEpoch,
            durationSeconds = 3000,
            isFinished = true,
            exercises = listOf(
                WorkoutExercise(
                    id = "we_yday",
                    exercise = testExercises[0],
                    sets = listOf(
                        SetLog(id = "s1", setNumber = 1, weightKg = 60.0, reps = 10, isCompleted = true),
                        SetLog(id = "s2", setNumber = 2, weightKg = 80.0, reps = 8, isCompleted = true)
                    )
                )
            )
        )
        // Intentionally supply oldest-first to verify chronological descending sorting
        workoutRepo.setCompletedWorkoutsForTesting(listOf(oldWorkout, yesterdayWorkout))

        val executor = KumaAiToolExecutor(
            exerciseRepo, routineRepo, programRepo, scheduleRepo,
            mesocycleRepo, readinessRepo, prRepo, workoutRepo, settingsRepo, dbRepo,
            nowProvider = { fixedNow }
        )

        val snap = Json.parseToJsonElement(executor.buildFastContextSnapshot(IdStrings)).jsonObject
        assertEquals("2026-11-26", snap["yesterdayDate"]!!.jsonPrimitive.content)
        assertEquals("true", snap["yesterdayWorkedOut"]!!.jsonPrimitive.content)
        assertEquals("false", snap["todayWorkedOut"]!!.jsonPrimitive.content)
        val ydayArr = snap["yesterdayWorkouts"]!!.jsonArray
        assertEquals(1, ydayArr.size)
        assertEquals("Push Kemarin", ydayArr[0].jsonObject["title"]!!.jsonPrimitive.content)
        assertEquals("YESTERDAY", ydayArr[0].jsonObject["relativeDay"]!!.jsonPrimitive.content)
        assertEquals("Push Kemarin", snap["recentWorkoutsTop5"]!!.jsonArray[0].jsonObject["title"]!!.jsonPrimitive.content)

        val recent = executor.executeCall(
            GeminiFunctionCall("get_recent_workouts", Json.parseToJsonElement("""{"limit":5}""").jsonObject),
            KumaAiToolSessionState(),
            IdStrings
        ).jsonObject
        assertEquals("true", recent["yesterdayWorkedOut"]!!.jsonPrimitive.content)
        val firstWorkout = recent["workouts"]!!.jsonArray[0].jsonObject
        assertEquals("2026-11-26", firstWorkout["date"]!!.jsonPrimitive.content)
        assertEquals("YESTERDAY", firstWorkout["relativeDay"]!!.jsonPrimitive.content)
        assertEquals("80.0", firstWorkout["exercises"]!!.jsonArray[0].jsonObject["bestWeightKg"]!!.jsonPrimitive.content)
    }

    @Test
    fun testStageRoutineCreateAndInPlaceUpdatePreserveTargetWeightAndSetConfigs() {
        val executor = KumaAiToolExecutor(
            exerciseRepo, routineRepo, programRepo, scheduleRepo,
            mesocycleRepo, readinessRepo, prRepo, workoutRepo, settingsRepo, dbRepo
        )
        val createSession = KumaAiToolSessionState()
        executor.executeCall(
            GeminiFunctionCall(
                "stage_create_routine",
                Json.parseToJsonElement(
                    """{
                        "title":"Upper Strength",
                        "splitCategory":"Push",
                        "exercises":[
                            {
                                "exerciseId":"ex_db_press",
                                "setConfigs":[
                                    {"reps":12,"weightKg":60.0,"restTimerSeconds":90},
                                    {"reps":10,"weightKg":70.0,"restTimerSeconds":90}
                                ]
                            },
                            {
                                "exerciseId":"ex_lat_raise",
                                "targetSets":4,
                                "targetReps":15,
                                "targetWeightKg":12.5,
                                "restTimerSeconds":60
                            }
                        ]
                    }"""
                ).jsonObject
            ),
            createSession,
            IdStrings
        )

        val (createdOk, _) = executor.commitProposal(AiActionProposal(id = "p_create", operations = createSession.stagedOperations))
        assertTrue(createdOk)
        assertEquals(1, routineRepo.routines.value.size)

        val saved = routineRepo.routines.value.first()
        assertEquals(70.0, saved.effectiveConfigs[0].targetWeightKg)
        assertEquals(2, saved.effectiveConfigs[0].setConfigs.size)
        assertEquals(60.0, saved.effectiveConfigs[0].setConfigs[0].targetWeightKg)
        assertEquals(12, saved.effectiveConfigs[0].setConfigs[0].targetReps)
        assertEquals(12.5, saved.effectiveConfigs[1].targetWeightKg)
        assertEquals(4, saved.effectiveConfigs[1].setConfigs.size)

        val routinesJson = executor.executeCall(
            GeminiFunctionCall("get_routines", Json.parseToJsonElement("{}").jsonObject),
            KumaAiToolSessionState(),
            IdStrings
        ).jsonObject
        val firstExJson = routinesJson["routines"]!!.jsonArray[0].jsonObject["exercises"]!!.jsonArray[0].jsonObject
        assertEquals("70.0", firstExJson["targetWeightKg"]!!.jsonPrimitive.content)
        assertEquals(2, firstExJson["setConfigs"]!!.jsonArray.size)

        // Update existing routine IN PLACE (no duplicate created)
        val updateSession = KumaAiToolSessionState()
        executor.executeCall(
            GeminiFunctionCall(
                "stage_update_routine",
                Json.parseToJsonElement(
                    """{
                        "routineId":"${saved.id}",
                        "exercises":[
                            {"exerciseId":"ex_db_press","targetSets":3,"targetReps":8,"targetWeightKg":75.0,"restTimerSeconds":120}
                        ]
                    }"""
                ).jsonObject
            ),
            updateSession,
            IdStrings
        )
        val (updatedOk, _) = executor.commitProposal(AiActionProposal(id = "p_update", operations = updateSession.stagedOperations))
        assertTrue(updatedOk)
        assertEquals(1, routineRepo.routines.value.size, "In-place update must not duplicate routine")
        val updated = routineRepo.routines.value.first()
        assertEquals(saved.id, updated.id)
        assertEquals(75.0, updated.effectiveConfigs[0].targetWeightKg)
        assertEquals(8, updated.effectiveConfigs[0].targetReps)
        assertEquals(3, updated.effectiveConfigs[0].setConfigs.size)
    }

    @Test
    fun testFivePersonalizedEmptyGreetingsInEnAndIdAreDistinctAndContainName() {
        val name = "Gabriel"
        val enGreetings = (0 until 5).map { EnStrings.aiEmptyGreeting(it, name) }
        val idGreetings = (0 until 5).map { IdStrings.aiEmptyGreeting(it, name) }

        assertEquals(5, enGreetings.distinct().size, "All 5 EN greetings must differ")
        assertEquals(5, idGreetings.distinct().size, "All 5 ID greetings must differ")
        enGreetings.forEach { msg ->
            assertTrue(msg.contains(name), "EN greeting '$msg' must include user's name")
            assertTrue(msg.isNotBlank())
        }
        idGreetings.forEach { msg ->
            assertTrue(msg.contains(name), "ID greeting '$msg' must include user's name")
            assertTrue(msg.isNotBlank())
        }
        assertEquals(enGreetings[0], EnStrings.aiEmptyGreeting(5, name))
        assertEquals(idGreetings[2], IdStrings.aiEmptyGreeting(-3, name))
    }

    @Test
    fun testTemporaryChatDoesNotPersistOrLeakToHistorySidebarOrBackup() = runBlocking {
        val transport = object : AiHttpTransport {
            override suspend fun get(url: String, headers: Map<String, String>, timeoutMs: Int) =
                error("Unexpected GET")
            override suspend fun postJson(url: String, headers: Map<String, String>, body: String, timeoutMs: Int) =
                AiHttpResponse(
                    200,
                    """{"candidates":[{"content":{"role":"model","parts":[{"text":"Jawaban sementara tidak disimpan."}]}}]}"""
                )
        }
        val executor = KumaAiToolExecutor(
            exerciseRepo, routineRepo, programRepo, scheduleRepo,
            mesocycleRepo, readinessRepo, prRepo, workoutRepo, settingsRepo, dbRepo
        )
        val service = KumaAiAgentService(
            geminiClient = GeminiApiClient(transport),
            toolExecutor = executor,
            secretStore = InMemoryAiSecretStore("k"),
            settingsRepository = settingsRepo,
            databaseRepo = dbRepo
        )

        service.setTemporaryChat(true)
        assertTrue(service.isTemporaryChat.value)
        service.sendMessage("rahasia temporary chat", null, IdStrings, AppLanguage.ID)
        repeat(40) {
            if (!service.isWorking.value && service.messages.value.size >= 2) return@repeat
            delay(25)
        }

        assertEquals(2, service.messages.value.size)
        assertTrue(dbRepo.getAllAiChatThreads().isEmpty(), "Temporary chat must never create saved thread")
        assertTrue(dbRepo.getAllAiChatMessages().isEmpty(), "Temporary chat must never write to SQLite")
        assertTrue(service.threads.value.isEmpty(), "Temporary chat must never appear in sidebar")

        // Turning temporary chat off clears volatile messages cleanly
        service.setTemporaryChat(false)
        assertFalse(service.isTemporaryChat.value)
        assertTrue(service.messages.value.isEmpty())
    }

    @Test
    fun testSavedHistorySidebarMultipleThreadsBackupSizeUnbloatedAndCrossThreadContext() = runBlocking {
        val backupBeforeChats = DataBackupManager(dbRepo).exportJson(settingsRepo.settings.value)
        val capturedSystems = mutableListOf<String>()
        val queuedReplies = ArrayDeque(
            listOf(
                """{"candidates":[{"content":{"role":"model","parts":[{"text":"Noted, hindari overhead barbell kalau bahu kiri sakit, pakai incline cable."}]}}]}""",
                """{"candidates":[{"content":{"role":"model","parts":[{"text":"Oatmeal 60g + whey + telur = 600 kkal, 48g protein."}]}}]}""",
                """{"candidates":[{"content":{"role":"model","parts":[{"text":"Di chat sebelumnya kita bahas bahu kiri sensitif dan menu 600 kkal."}]}}]}"""
            )
        )
        val transport = object : AiHttpTransport {
            override suspend fun get(url: String, headers: Map<String, String>, timeoutMs: Int) =
                error("Unexpected GET")
            override suspend fun postJson(url: String, headers: Map<String, String>, body: String, timeoutMs: Int): AiHttpResponse {
                capturedSystems.add(Json.parseToJsonElement(body).jsonObject["systemInstruction"]!!.toString())
                return AiHttpResponse(200, queuedReplies.removeFirst())
            }
        }
        val executor = KumaAiToolExecutor(
            exerciseRepo, routineRepo, programRepo, scheduleRepo,
            mesocycleRepo, readinessRepo, prRepo, workoutRepo, settingsRepo, dbRepo
        )
        val service = KumaAiAgentService(
            geminiClient = GeminiApiClient(transport),
            toolExecutor = executor,
            secretStore = InMemoryAiSecretStore("k"),
            settingsRepository = settingsRepo,
            databaseRepo = dbRepo
        )

        // Thread 1
        service.sendMessage("bahu kiri agak sakit pas overhead press", null, IdStrings, AppLanguage.ID)
        repeat(40) {
            if (!service.isWorking.value && service.messages.value.size >= 2) return@repeat
            delay(25)
        }
        val thread1Id = service.currentThreadId.value
        assertNotNull(thread1Id)

        // Start Thread 2 -> separate thread preserved in sidebar
        service.startNewChat()
        assertTrue(service.messages.value.isEmpty())
        service.sendMessage("saran menu sarapan 600 kkal", null, IdStrings, AppLanguage.ID)
        repeat(40) {
            if (!service.isWorking.value && service.messages.value.size >= 2) return@repeat
            delay(25)
        }
        val thread2Id = service.currentThreadId.value
        assertNotNull(thread2Id)
        assertFalse(thread1Id == thread2Id)
        assertEquals(2, service.threads.value.size)

        // Switching back to Thread 1 restores exact conversation
        service.selectThread(thread1Id)
        assertEquals(2, service.messages.value.size)
        assertTrue(service.messages.value.first().text.contains("bahu kiri"))

        // Start Thread 3 -> AI automatically receives cross-thread context index (`savedChatHistories`)
        service.startNewChat()
        service.sendMessage("ingat saran bahu dan sarapan dari chat lalu?", null, IdStrings, AppLanguage.ID)
        repeat(40) {
            if (!service.isWorking.value && service.messages.value.size >= 2) return@repeat
            delay(25)
        }
        val sysPrompt3 = capturedSystems.last()
        assertTrue(sysPrompt3.contains("savedChatHistories"), "Snapshot must expose savedChatHistories across threads")
        assertTrue(sysPrompt3.contains("bahu kiri"), "Thread 1 context summary must carry into new chats")
        assertTrue(sysPrompt3.contains("600 kkal") || sysPrompt3.contains("menu sarapan"), "Thread 2 context must carry into new chats")

        // Deep recall tool can fetch exact messages by keyword or threadId
        val searchByQuery = executor.executeCall(
            GeminiFunctionCall("search_chat_history", Json.parseToJsonElement("""{"query":"bahu"}""").jsonObject),
            KumaAiToolSessionState(),
            IdStrings
        ).jsonObject
        assertTrue(searchByQuery["messages"]!!.jsonArray.toString().contains("overhead barbell"))

        val searchByThread = executor.executeCall(
            GeminiFunctionCall("search_chat_history", Json.parseToJsonElement("""{"threadId":"$thread2Id"}""").jsonObject),
            KumaAiToolSessionState(),
            IdStrings
        ).jsonObject
        assertTrue(searchByThread["messages"]!!.jsonArray.toString().contains("48g protein"))

        // Backup payload size stays 100% unbloated by chat transcripts
        val backupAfterChats = DataBackupManager(dbRepo).exportJson(settingsRepo.settings.value)
        assertFalse(backupAfterChats.contains("overhead barbell"))
        assertEquals(
            Json.parseToJsonElement(backupBeforeChats).jsonObject.filterKeys { it != "exportedAtEpochMs" },
            Json.parseToJsonElement(backupAfterChats).jsonObject.filterKeys { it != "exportedAtEpochMs" }
        )

        // Deleting a single thread removes only that thread
        service.deleteThread(thread1Id)
        assertEquals(2, service.threads.value.size) // Thread 2 & Thread 3 remain
        assertFalse(service.threads.value.any { it.id == thread1Id })
    }

    @Test
    fun testMultiImageUploadUpTo10PersistsAndSendsAllToGemini() = runBlocking {
        var capturedBody = ""
        val transport = object : AiHttpTransport {
            override suspend fun get(url: String, headers: Map<String, String>, timeoutMs: Int) =
                error("Unexpected GET")
            override suspend fun postJson(url: String, headers: Map<String, String>, body: String, timeoutMs: Int): AiHttpResponse {
                capturedBody = body
                return AiHttpResponse(
                    200,
                    """{"candidates":[{"content":{"role":"model","parts":[{"text":"Semua 10 foto sudah dianalisis."}]}}]}"""
                )
            }
        }
        val executor = KumaAiToolExecutor(
            exerciseRepo, routineRepo, programRepo, scheduleRepo,
            mesocycleRepo, readinessRepo, prRepo, workoutRepo, settingsRepo, dbRepo
        )
        val service = KumaAiAgentService(
            geminiClient = GeminiApiClient(transport),
            toolExecutor = executor,
            secretStore = InMemoryAiSecretStore("k"),
            settingsRepository = settingsRepo,
            databaseRepo = dbRepo,
            imageLoader = { path -> "image/jpeg" to "B64_$path" }
        )

        // Supply 12 photos -> capped safely to max 10
        val twelvePaths = (1..12).map { "/tmp/workout_shot_$it.jpg" }
        service.sendMessage("baca semua screenshot jadwal latihan ini", twelvePaths, IdStrings, AppLanguage.ID)
        repeat(40) {
            if (!service.isWorking.value && service.messages.value.size >= 2) return@repeat
            delay(25)
        }

        val userMsg = service.messages.value.first()
        assertEquals(10, userMsg.effectiveImagePaths.size)
        assertTrue(capturedBody.contains("B64_/tmp/workout_shot_10.jpg"))
        assertFalse(capturedBody.contains("B64_/tmp/workout_shot_11.jpg"))
        assertTrue(capturedBody.contains(""""thinkingBudget":2048"""), "Multi-image requests must use DEEP reasoning")

        val reloaded = dbRepo.getAiChatMessages(userMsg.threadId).first()
        assertEquals(10, reloaded.effectiveImagePaths.size)
        assertEquals("/tmp/workout_shot_10.jpg", reloaded.effectiveImagePaths.last())
    }

    @Test
    fun testSwitchingToNewChatWhileWorkingDoesNotPolluteNewChatOrZeroPreviousThread() = runBlocking {
        val slowTransport = object : AiHttpTransport {
            override suspend fun get(url: String, headers: Map<String, String>, timeoutMs: Int) =
                error("Unexpected GET")
            override suspend fun postJson(url: String, headers: Map<String, String>, body: String, timeoutMs: Int): AiHttpResponse {
                delay(400)
                return AiHttpResponse(
                    200,
                    """{"candidates":[{"content":{"role":"model","parts":[{"text":"Selesai."}]}}]}"""
                )
            }
        }
        val executor = KumaAiToolExecutor(
            exerciseRepo, routineRepo, programRepo, scheduleRepo,
            mesocycleRepo, readinessRepo, prRepo, workoutRepo, settingsRepo, dbRepo
        )
        val service = KumaAiAgentService(
            geminiClient = GeminiApiClient(slowTransport),
            toolExecutor = executor,
            secretStore = InMemoryAiSecretStore("k"),
            settingsRepository = settingsRepo,
            databaseRepo = dbRepo
        )

        service.sendMessage("pertanyaan di thread A", null, IdStrings, AppLanguage.ID)
        val threadAId = service.currentThreadId.value!!
        delay(40)
        assertTrue(service.isWorking.value)

        // Switching to a brand-new chat mid-request silently cancels without injecting error into new chat
        service.startNewChat(temporary = false)
        delay(120)

        assertFalse(service.isWorking.value)
        assertTrue(service.messages.value.isEmpty(), "New chat must stay clean without cancelled banner from previous thread")
        val threadAAfter = service.threads.value.first { it.id == threadAId }
        assertEquals(1, threadAAfter.messageCount)

        // Explicit Stop button on same thread DOES show cancelled banner
        service.sendMessage("pertanyaan yang distop manual", null, IdStrings, AppLanguage.ID)
        delay(40)
        service.stopCurrentRequest()
        repeat(25) {
            if (!service.isWorking.value && service.messages.value.size >= 2) return@repeat
            delay(20)
        }
        assertEquals(2, service.messages.value.size)
        assertTrue(service.messages.value.last().isError)
        assertEquals(IdStrings.aiErrorCancelled, service.messages.value.last().text)
    }

    @Test
    fun testSearchExercisesMapsAliasesAndCommitRejectsInactiveAddExercise() {
        val bandCurl = exerciseRepo.addCustomExercise(
            name = "Band Bicep Curl",
            primaryMuscle = MuscleGroup.ARMS,
            equipment = Equipment.BAND,
            movementType = MovementType.ISOLATION
        )
        val executor = KumaAiToolExecutor(
            exerciseRepo, routineRepo, programRepo, scheduleRepo,
            mesocycleRepo, readinessRepo, prRepo, workoutRepo, settingsRepo, dbRepo
        )
        val session = KumaAiToolSessionState()

        val searchRes = executor.executeCall(
            call = GeminiFunctionCall(
                name = "search_exercises",
                args = buildJsonObject {
                    put("muscleGroup", "BICEPS")
                    put("equipment", "RESISTANCE_BAND")
                }
            ),
            session = session,
            strings = EnStrings
        )
        val ids = searchRes.jsonObject["exercises"]!!.jsonArray
            .map { it.jsonObject["id"]!!.jsonPrimitive.content }
        assertTrue(ids.contains(bandCurl.id))

        workoutRepo.startEmptyWorkout("Temp Active")
        val stageRes = executor.executeCall(
            call = GeminiFunctionCall(
                name = "stage_add_exercise_to_active_workout",
                args = buildJsonObject { put("exerciseId", bandCurl.id) }
            ),
            session = session,
            strings = EnStrings
        )
        assertEquals("DRAFTED_FOR_USER_REVIEW", stageRes.jsonObject["status"]?.jsonPrimitive?.content)
        assertEquals(1, session.stagedOperations.size)

        // Workout ends before user taps Approve -> commit must fail cleanly
        workoutRepo.cancelWorkout()
        val (ok, _) = executor.commitProposal(
            AiActionProposal(
                id = "prop_inactive_test",
                summary = "Add",
                status = AiProposalStatus.PENDING,
                operations = session.stagedOperations.toList()
            )
        )
        assertFalse(ok)
    }

    @Test
    fun testNewChatPersistsActiveStateAndDoesNotRedirectToOlderHistoryOnReload() = runBlocking {
        val transport = object : AiHttpTransport {
            override suspend fun get(url: String, headers: Map<String, String>, timeoutMs: Int) =
                error("Unexpected GET")
            override suspend fun postJson(url: String, headers: Map<String, String>, body: String, timeoutMs: Int) =
                AiHttpResponse(
                    200,
                    """{"candidates":[{"content":{"role":"model","parts":[{"text":"Ready for push day."}]}}]}"""
                )
        }
        val executor = KumaAiToolExecutor(
            exerciseRepo, routineRepo, programRepo, scheduleRepo,
            mesocycleRepo, readinessRepo, prRepo, workoutRepo, settingsRepo, dbRepo
        )
        fun createService() = KumaAiAgentService(
            geminiClient = GeminiApiClient(transport),
            toolExecutor = executor,
            secretStore = InMemoryAiSecretStore("k"),
            settingsRepository = settingsRepo,
            databaseRepo = dbRepo
        )

        val service1 = createService()
        service1.sendMessage("push day workout", null, EnStrings, AppLanguage.EN)
        repeat(40) {
            if (!service1.isWorking.value && service1.messages.value.size >= 2) return@repeat
            delay(25)
        }
        val thread1Id = service1.currentThreadId.value
        assertNotNull(thread1Id)
        assertEquals(2, service1.messages.value.size)

        // Click New Chat -> navigates away / recreates service -> must stay on empty New Chat
        service1.startNewChat(temporary = false)
        assertNull(service1.currentThreadId.value)
        assertTrue(service1.messages.value.isEmpty())

        val serviceAfterNavAway = createService()
        assertNull(
            serviceAfterNavAway.currentThreadId.value,
            "Returning after New Chat must stay in new chat without redirecting to older history"
        )
        assertTrue(serviceAfterNavAway.messages.value.isEmpty())
        assertEquals(1, serviceAfterNavAway.threads.value.size)
        assertEquals(thread1Id, serviceAfterNavAway.threads.value.first().id)

        // Selecting an existing thread restores & persists it until explicitly changed
        serviceAfterNavAway.selectThread(thread1Id)
        val serviceWithSelectedThread = createService()
        assertEquals(thread1Id, serviceWithSelectedThread.currentThreadId.value)
        assertEquals(2, serviceWithSelectedThread.messages.value.size)

        // Deleting active thread resets cleanly to empty New Chat
        serviceWithSelectedThread.deleteThread(thread1Id)
        assertNull(serviceWithSelectedThread.currentThreadId.value)
        assertTrue(serviceWithSelectedThread.messages.value.isEmpty())
    }

    @Test
    fun testEditUserMessageTruncatesOutdatedContextAndGeneratesRevisedReply() = runBlocking {
        val capturedRequestBodies = mutableListOf<String>()
        val queuedReplies = ArrayDeque(
            listOf(
                """{"candidates":[{"content":{"role":"model","parts":[{"text":"Old reply one"}]}}]}""",
                """{"candidates":[{"content":{"role":"model","parts":[{"text":"Old reply two"}]}}]}""",
                """{"candidates":[{"content":{"role":"model","parts":[{"text":"Fresh pull day reply"}]}}]}"""
            )
        )
        val transport = object : AiHttpTransport {
            override suspend fun get(url: String, headers: Map<String, String>, timeoutMs: Int) =
                error("Unexpected GET")
            override suspend fun postJson(url: String, headers: Map<String, String>, body: String, timeoutMs: Int): AiHttpResponse {
                capturedRequestBodies.add(body)
                return AiHttpResponse(200, queuedReplies.removeFirst())
            }
        }
        val executor = KumaAiToolExecutor(
            exerciseRepo, routineRepo, programRepo, scheduleRepo,
            mesocycleRepo, readinessRepo, prRepo, workoutRepo, settingsRepo, dbRepo
        )
        val service = KumaAiAgentService(
            geminiClient = GeminiApiClient(transport),
            toolExecutor = executor,
            secretStore = InMemoryAiSecretStore("k"),
            settingsRepository = settingsRepo,
            databaseRepo = dbRepo
        )

        service.sendMessage("leg day plan", null, EnStrings, AppLanguage.EN)
        repeat(40) {
            if (!service.isWorking.value && service.messages.value.size >= 2) return@repeat
            delay(25)
        }
        service.sendMessage("add calf raises", null, EnStrings, AppLanguage.EN)
        repeat(40) {
            if (!service.isWorking.value && service.messages.value.size >= 4) return@repeat
            delay(25)
        }
        assertEquals(4, service.messages.value.size)
        val firstPromptId = service.messages.value.first().id
        val threadId = service.currentThreadId.value!!

        // Edit opening prompt -> drops outdated response & subsequent turn, updates title, regenerates reply
        val started = service.editUserMessage(firstPromptId, "revised pull day plan", EnStrings, AppLanguage.EN)
        assertTrue(started)
        repeat(40) {
            if (!service.isWorking.value && service.messages.value.size >= 2) return@repeat
            delay(25)
        }

        assertEquals(2, service.messages.value.size)
        assertEquals("revised pull day plan", service.messages.value[0].text)
        assertEquals("Fresh pull day reply", service.messages.value[1].text)

        val lastBody = capturedRequestBodies.last()
        assertTrue(lastBody.contains("revised pull day plan"))
        assertFalse(lastBody.contains("Old reply one"))
        assertFalse(lastBody.contains("add calf raises"))
        assertFalse(lastBody.contains("Old reply two"))

        val reloadedMsgs = dbRepo.getAiChatMessages(threadId)
        assertEquals(2, reloadedMsgs.size)
        assertEquals("revised pull day plan", reloadedMsgs[0].text)
        assertEquals("Fresh pull day reply", reloadedMsgs[1].text)
        assertEquals("revised pull day plan", service.threads.value.first { it.id == threadId }.title)
    }

    @Test
    fun testRegenerateResponseReplacesPreviousReplyWithoutDuplication() = runBlocking {
        val capturedRequestBodies = mutableListOf<String>()
        val queuedReplies = ArrayDeque(
            listOf(
                """{"candidates":[{"content":{"role":"model","parts":[{"text":"First answer"}]}}]}""",
                """{"candidates":[{"content":{"role":"model","parts":[{"text":"Second answer"}]}}]}""",
                """{"candidates":[{"content":{"role":"model","parts":[{"text":"Regenerated second answer"}]}}]}"""
            )
        )
        val transport = object : AiHttpTransport {
            override suspend fun get(url: String, headers: Map<String, String>, timeoutMs: Int) =
                error("Unexpected GET")
            override suspend fun postJson(url: String, headers: Map<String, String>, body: String, timeoutMs: Int): AiHttpResponse {
                capturedRequestBodies.add(body)
                return AiHttpResponse(200, queuedReplies.removeFirst())
            }
        }
        val executor = KumaAiToolExecutor(
            exerciseRepo, routineRepo, programRepo, scheduleRepo,
            mesocycleRepo, readinessRepo, prRepo, workoutRepo, settingsRepo, dbRepo
        )
        val service = KumaAiAgentService(
            geminiClient = GeminiApiClient(transport),
            toolExecutor = executor,
            secretStore = InMemoryAiSecretStore("k"),
            settingsRepository = settingsRepo,
            databaseRepo = dbRepo
        )

        // Blank assistant error cannot be regenerated without a preceding prompt
        assertFalse(service.regenerateResponse("nonexistent", EnStrings, AppLanguage.EN))

        service.sendMessage("first question", null, EnStrings, AppLanguage.EN)
        repeat(40) {
            if (!service.isWorking.value && service.messages.value.size >= 2) return@repeat
            delay(25)
        }
        service.sendMessage("follow up question", null, EnStrings, AppLanguage.EN)
        repeat(40) {
            if (!service.isWorking.value && service.messages.value.size >= 4) return@repeat
            delay(25)
        }
        val secondAssistantId = service.messages.value[3].id
        val threadId = service.currentThreadId.value!!

        val started = service.regenerateResponse(secondAssistantId, EnStrings, AppLanguage.EN)
        assertTrue(started)
        repeat(40) {
            if (!service.isWorking.value && service.messages.value.size >= 4) return@repeat
            delay(25)
        }

        assertEquals(4, service.messages.value.size)
        assertEquals("First answer", service.messages.value[1].text)
        assertEquals("follow up question", service.messages.value[2].text)
        assertEquals("Regenerated second answer", service.messages.value[3].text)

        val lastBody = capturedRequestBodies.last()
        assertTrue(lastBody.contains("first question"))
        assertTrue(lastBody.contains("First answer"))
        assertTrue(lastBody.contains("follow up question"))
        assertFalse(lastBody.contains("\"Second answer\""))

        val reloaded = dbRepo.getAiChatMessages(threadId)
        assertEquals(4, reloaded.size)
        assertEquals("Regenerated second answer", reloaded.last().text)
    }

    @Test
    fun testCopyableContentAndDisclaimerStrings() {
        assertEquals(
            "Kuma AI can make mistakes. Please verify important information.",
            EnStrings.aiDisclaimerText
        )
        assertTrue(IdStrings.aiDisclaimerText.isNotBlank())

        val userMsg = AiChatMessage(id = "u1", role = AiChatRole.USER, text = "Analyze squat depth")
        assertEquals("Analyze squat depth", userMsg.copyableContent())

        val assistantTextMsg = AiChatMessage(
            id = "a1",
            role = AiChatRole.ASSISTANT,
            text = "Keep brace tight and knees tracking toes."
        )
        assertEquals("Keep brace tight and knees tracking toes.", assistantTextMsg.copyableContent())

        val assistantNutritionOnly = AiChatMessage(
            id = "a2",
            role = AiChatRole.ASSISTANT,
            text = "",
            nutritionCard = AiNutritionCardData(
                foodName = "Chicken Rice Bowl",
                caloriesKcal = 620,
                proteinGrams = 48,
                carbsGrams = 68,
                fatGrams = 14,
                summary = "Solid post-workout meal."
            )
        )
        val copiedNutrition = assistantNutritionOnly.copyableContent()
        assertTrue(copiedNutrition.contains("Chicken Rice Bowl"))
        assertTrue(copiedNutrition.contains("620 kcal"))
        assertTrue(copiedNutrition.contains("Solid post-workout meal."))
    }

    @Test
    fun testChangePreviewDiffTracksReplacedModifiedAddedRemovedAndVerifiesApply() = runBlocking {
        val rearDelt = exerciseRepo.addCustomExercise(
            name = "Dumbbell Rear Delt Raise",
            primaryMuscle = MuscleGroup.SHOULDERS,
            equipment = Equipment.DUMBBELL,
            movementType = MovementType.ISOLATION
        )
        val pullup = exerciseRepo.addCustomExercise(
            name = "Pull Up",
            primaryMuscle = MuscleGroup.BACK,
            equipment = Equipment.BODYWEIGHT,
            movementType = MovementType.COMPOUND
        )
        val lyingRow = exerciseRepo.addCustomExercise(
            name = "Lying Rear Delt Row",
            primaryMuscle = MuscleGroup.SHOULDERS,
            equipment = Equipment.DUMBBELL,
            movementType = MovementType.ISOLATION
        )
        val pullRoutine = RoutineBlueprint(
            id = "rt_custom_pull",
            title = "Pull Day (Back, Biceps)",
            splitCategory = "Pull",
            muscleFocus = "BACK",
            estimatedDurationMinutes = 45,
            exerciseConfigs = listOf(
                RoutineExerciseConfig(testExercises[0], targetSets = 3, targetReps = 10, targetWeightKg = 20.0, restTimerSeconds = 90),
                RoutineExerciseConfig(pullup, targetSets = 3, targetReps = 8, restTimerSeconds = 90),
                RoutineExerciseConfig(lyingRow, targetSets = 3, targetReps = 12, targetWeightKg = 10.0, restTimerSeconds = 60)
            )
        )
        routineRepo.addRoutine(pullRoutine)

        val executor = KumaAiToolExecutor(
            exerciseRepo, routineRepo, programRepo, scheduleRepo,
            mesocycleRepo, readinessRepo, prRepo, workoutRepo, settingsRepo, dbRepo
        )
        val session = KumaAiToolSessionState()
        executor.executeCall(
            call = GeminiFunctionCall(
                name = "stage_update_routine",
                args = Json.parseToJsonElement(
                    """
                    {
                      "routineId": "rt_custom_pull",
                      "exercises": [
                        {"exerciseId": "ex_db_press", "targetSets": 4, "targetReps": 8, "targetWeightKg": 25.0, "restTimerSeconds": 120},
                        {"exerciseId": "${rearDelt.id}", "targetSets": 3, "targetReps": 15, "targetWeightKg": 8.0, "restTimerSeconds": 60},
                        {"exerciseId": "ex_lat_raise", "targetSets": 3, "targetReps": 12, "targetWeightKg": 10.0, "restTimerSeconds": 60}
                      ]
                    }
                    """.trimIndent()
                ).jsonObject
            ),
            session = session,
            strings = EnStrings
        )

        val op = session.stagedOperations.single()
        val detail = op.previewDetail
        assertNotNull(detail)
        assertEquals("Pull Day (Back, Biceps)", detail.routineName)
        assertEquals(3, detail.updatedExercises.size)
        assertTrue(detail.exerciseDiffs.any { it.status == AiExerciseDiffStatus.MODIFIED && it.before?.name == "Dumbbell Bench Press" && it.after?.sets == 4 })
        assertTrue(detail.exerciseDiffs.any { it.status == AiExerciseDiffStatus.REMOVED && it.before?.name == "Pull Up" })
        assertTrue(detail.exerciseDiffs.any { it.status == AiExerciseDiffStatus.REPLACED && it.before?.name == "Lying Rear Delt Row" && it.after?.name == "Dumbbell Rear Delt Raise" })
        assertTrue(detail.exerciseDiffs.any { it.status == AiExerciseDiffStatus.ADDED && it.after?.name == "Dumbbell Lateral Raise" })

        // Legacy fallback resolver works even when previewDetail is absent
        val legacyOp = op.copy(previewDetail = null)
        val resolvedLegacy = executor.resolveOperationPreview(legacyOp, AiProposalStatus.PENDING, EnStrings)
        assertEquals(3, resolvedLegacy.updatedExercises.size)
        assertEquals("Dumbbell Bench Press", resolvedLegacy.updatedExercises.first().name)

        // Apply succeeds and verified live persisted data matches
        val proposal = AiActionProposal(id = "p_diff", operations = listOf(op))
        val (ok, note) = executor.commitProposal(proposal, EnStrings)
        assertTrue(ok)
        assertFalse(note.contains("rt_custom_pull"))
        assertEquals(3, routineRepo.getRoutineById("rt_custom_pull")?.effectiveConfigs?.size)

        // Deleting routine first makes apply fail instead of wrongly marking APPLIED
        routineRepo.deleteRoutine("rt_custom_pull")
        val (failOk, failMsg) = executor.commitProposal(proposal, EnStrings)
        assertFalse(failOk)
        assertFalse(failMsg.contains("rt_custom_pull"))
    }

    @Test
    fun testUpdateRoutineSyncsExerciseTemplatesAndScheduleCancelDeleteCommitSafely() {
        val routine = RoutineBlueprint(
            id = "rt_sync_check",
            title = "Upper Starter",
            splitCategory = "Upper",
            muscleFocus = "CHEST",
            estimatedDurationMinutes = 40,
            plannedSetsPerExercise = 3,
            exerciseConfigs = listOf(
                RoutineExerciseConfig(testExercises[0], targetSets = 3, targetReps = 10)
            )
        )
        routineRepo.addRoutine(routine)

        val executor = KumaAiToolExecutor(
            exerciseRepo, routineRepo, programRepo, scheduleRepo,
            mesocycleRepo, readinessRepo, prRepo, workoutRepo, settingsRepo, dbRepo
        )
        val session = KumaAiToolSessionState()

        // Non-primitive scheduledDays elements must not crash
        executor.executeCall(
            call = GeminiFunctionCall(
                name = "stage_update_routine",
                args = Json.parseToJsonElement(
                    """
                    {
                      "routineId": "rt_sync_check",
                      "scheduledDays": [1, {"bad": 2}, 4],
                      "exercises": [
                        {"exerciseId": "ex_lat_raise", "targetSets": 4, "targetReps": 12}
                      ]
                    }
                    """.trimIndent()
                ).jsonObject
            ),
            session = session,
            strings = EnStrings
        )
        val updateProp = AiActionProposal(id = "p_sync", operations = session.stagedOperations.toList())
        val (updOk, _) = executor.commitProposal(updateProp, EnStrings)
        assertTrue(updOk)
        val updatedRoutine = routineRepo.getRoutineById("rt_sync_check")!!
        assertEquals(listOf("ex_lat_raise"), updatedRoutine.exerciseTemplates.map { it.id })
        assertEquals(4, updatedRoutine.plannedSetsPerExercise)
        assertEquals(listOf(1, 4), updatedRoutine.scheduledDays)

        // Schedule cancel + delete proposals commit without StageIdPayload crash
        val event = scheduleRepo.scheduleRoutine(
            routineId = updatedRoutine.id,
            routineTitle = updatedRoutine.title,
            splitType = updatedRoutine.splitCategory,
            scheduledDate = kotlinx.datetime.LocalDate(2026, 11, 29)
        )
        val cancelSession = KumaAiToolSessionState()
        executor.executeCall(
            call = GeminiFunctionCall(
                name = "stage_cancel_or_delete_schedule",
                args = Json.parseToJsonElement("""{"eventId":"${event.id}","deletePermanently":false}""").jsonObject
            ),
            session = cancelSession,
            strings = EnStrings
        )
        val (cancelOk, _) = executor.commitProposal(
            AiActionProposal(id = "p_cancel", operations = cancelSession.stagedOperations.toList()),
            EnStrings
        )
        assertTrue(cancelOk)
        assertTrue(scheduleRepo.events.value.first { it.id == event.id }.isCancelled)

        val deleteSession = KumaAiToolSessionState()
        executor.executeCall(
            call = GeminiFunctionCall(
                name = "stage_cancel_or_delete_schedule",
                args = Json.parseToJsonElement("""{"eventId":"${event.id}","deletePermanently":true}""").jsonObject
            ),
            session = deleteSession,
            strings = EnStrings
        )
        val deleteProp = AiActionProposal(id = "p_delete", operations = deleteSession.stagedOperations.toList())
        val (delOk, _) = executor.commitProposal(deleteProp, EnStrings)
        assertTrue(delOk)
        assertFalse(scheduleRepo.events.value.any { it.id == event.id })

        // Re-committing delete on missing event must fail instead of returning APPLIED
        val (delAgainOk, _) = executor.commitProposal(deleteProp, EnStrings)
        assertFalse(delAgainOk)
    }

    @Test
    fun testHistoryAlternationPreservesImageTurnsAndMergesAfterErrorBubble() = runBlocking {
        val capturedBodies = mutableListOf<String>()
        val responses = ArrayDeque(
            listOf(
                AiHttpResponse(200, """{"candidates":[{"content":{"role":"model","parts":[{"text":"Looks like 600 kcal."}]}}]}"""),
                AiHttpResponse(500, """{"error":{"message":"backend busy"}}"""),
                AiHttpResponse(200, """{"candidates":[{"content":{"role":"model","parts":[{"text":"Got it."}]}}]}""")
            )
        )
        val transport = object : AiHttpTransport {
            override suspend fun get(url: String, headers: Map<String, String>, timeoutMs: Int) =
                error("Unexpected GET")
            override suspend fun postJson(url: String, headers: Map<String, String>, body: String, timeoutMs: Int): AiHttpResponse {
                capturedBodies.add(body)
                return responses.removeFirst()
            }
        }
        val executor = KumaAiToolExecutor(
            exerciseRepo, routineRepo, programRepo, scheduleRepo,
            mesocycleRepo, readinessRepo, prRepo, workoutRepo, settingsRepo, dbRepo
        )
        val service = KumaAiAgentService(
            geminiClient = GeminiApiClient(transport),
            toolExecutor = executor,
            secretStore = InMemoryAiSecretStore("k"),
            settingsRepository = settingsRepo,
            databaseRepo = dbRepo,
            imageLoader = { path -> "image/jpeg" to "FAKE_$path" }
        )

        // Turn 1: image-only user prompt (no caption text)
        service.sendMessage("", imagePaths = listOf("/tmp/meal1.jpg"), strings = EnStrings, language = AppLanguage.EN)
        repeat(40) { if (!service.isWorking.value && service.messages.value.size >= 2) return@repeat; delay(25) }

        // Turn 2: fails with 500 error bubble
        service.sendMessage("Add whey scoop", imagePaths = emptyList(), strings = EnStrings, language = AppLanguage.EN)
        repeat(40) { if (!service.isWorking.value && service.messages.value.size >= 4) return@repeat; delay(25) }

        // Turn 3: follow-up after error bubble
        service.sendMessage("Try again", imagePaths = emptyList(), strings = EnStrings, language = AppLanguage.EN)
        repeat(40) { if (!service.isWorking.value && service.messages.value.size >= 6) return@repeat; delay(25) }

        val thirdReqContents = Json.parseToJsonElement(capturedBodies.last()).jsonObject["contents"]!!.jsonArray
        val roles = thirdReqContents.map { it.jsonObject["role"]!!.jsonPrimitive.content }
        assertEquals("user", roles.first())
        assertEquals("user", roles.last())
        for (i in 1 until roles.size) {
            assertFalse(roles[i] == roles[i - 1], "Adjacent same role in $roles")
        }
        val firstTurnText = thirdReqContents.first().jsonObject["parts"]!!.jsonArray.toString()
        assertTrue(firstTurnText.contains("shared a photo"), "Image-only user turn missing from history: $firstTurnText")
    }

    @Test
    fun testDeleteThreadRemovesUnreferencedAiChatImagesOnly() = runBlocking {
        val tempRoot = java.nio.file.Files.createTempDirectory("kuma_ai_img_test").toFile()
        val imgDir = java.io.File(tempRoot, "ai_chat_images").apply { mkdirs() }
        val orphanFile = java.io.File(imgDir, "ai_img_orphan.jpg").apply { writeText("img1") }
        val sharedFile = java.io.File(imgDir, "ai_img_shared.jpg").apply { writeText("img2") }
        val outsideFile = java.io.File(tempRoot, "outside.jpg").apply { writeText("keep") }

        try {
            val transport = object : AiHttpTransport {
                override suspend fun get(url: String, headers: Map<String, String>, timeoutMs: Int) = error("Unexpected")
                override suspend fun postJson(url: String, headers: Map<String, String>, body: String, timeoutMs: Int) =
                    AiHttpResponse(200, """{"candidates":[{"content":{"role":"model","parts":[{"text":"OK"}]}}]}""")
            }
            val executor = KumaAiToolExecutor(
                exerciseRepo, routineRepo, programRepo, scheduleRepo,
                mesocycleRepo, readinessRepo, prRepo, workoutRepo, settingsRepo, dbRepo
            )
            val service = KumaAiAgentService(
                geminiClient = GeminiApiClient(transport),
                toolExecutor = executor,
                secretStore = InMemoryAiSecretStore("k"),
                settingsRepository = settingsRepo,
                databaseRepo = dbRepo,
                imageLoader = { "image/jpeg" to "B64" }
            )

            service.sendMessage("Check 1", imagePaths = listOf(orphanFile.absolutePath, sharedFile.absolutePath, outsideFile.absolutePath), strings = EnStrings, language = AppLanguage.EN)
            repeat(40) { if (!service.isWorking.value && service.messages.value.size >= 2) return@repeat; delay(25) }
            val thread1Id = service.currentThreadId.value!!

            service.startNewChat()
            service.sendMessage("Check 2", imagePaths = listOf(sharedFile.absolutePath), strings = EnStrings, language = AppLanguage.EN)
            repeat(40) { if (!service.isWorking.value && service.messages.value.size >= 2) return@repeat; delay(25) }

            service.deleteThread(thread1Id)
            assertFalse(orphanFile.exists(), "Orphan ai_chat_images file should be deleted")
            assertTrue(sharedFile.exists(), "Shared file referenced by thread 2 must remain")
            assertTrue(outsideFile.exists(), "File outside ai_chat_images must never be deleted")
        } finally {
            tempRoot.deleteRecursively()
        }
    }

    @Test
    fun testSanitizeUserFacingTextRemovesInternalIdsSlugsAndJsonKeys() = runBlocking {
        val starterPull = RoutineBlueprint(
            id = "starter_pull_row",
            title = "Pull Day (Back, Biceps)",
            splitCategory = "Pull",
            muscleFocus = "focus_pull",
            estimatedDurationMinutes = 45,
            exerciseConfigs = listOf(
                RoutineExerciseConfig(testExercises[0], targetSets = 3, targetReps = 10)
            ),
            presetKey = "starter_pull",
            isUserRenamed = false
        )
        routineRepo.addRoutine(starterPull)

        val transport = object : AiHttpTransport {
            override suspend fun get(url: String, headers: Map<String, String>, timeoutMs: Int) =
                error("Unexpected GET")
            override suspend fun postJson(url: String, headers: Map<String, String>, body: String, timeoutMs: Int) =
                AiHttpResponse(
                    200,
                    """{"candidates":[{"content":{"role":"model","parts":[{"text":"Yesterday you did not complete your workout (yesterdayWorkedOut: false), so your starter_pull session was missed."}]}}]}"""
                )
        }
        val executor = KumaAiToolExecutor(
            exerciseRepo, routineRepo, programRepo, scheduleRepo,
            mesocycleRepo, readinessRepo, prRepo, workoutRepo, settingsRepo, dbRepo
        )
        val service = KumaAiAgentService(
            geminiClient = GeminiApiClient(transport),
            toolExecutor = executor,
            secretStore = InMemoryAiSecretStore("k"),
            settingsRepository = settingsRepo,
            databaseRepo = dbRepo
        )

        // Direct sanitizer checks (both EN and ID screenshot leaks)
        val enSanitized = executor.sanitizeUserFacingText(
            "Yesterday you did not complete your workout (yesterdayWorkedOut: false), so your starter_pull session was missed.",
            EnStrings
        )
        assertFalse(enSanitized.contains("yesterdayWorkedOut"))
        assertFalse(enSanitized.contains("starter_pull"))
        assertTrue(enSanitized.contains(EnStrings.routinePullDayTitle))

        val idSanitized = executor.sanitizeUserFacingText(
            "Untuk latihan Pull Day, susunan rutinitas bawaan (starter_pull) belum optimal (todayWorkoutStatus: REST_DAY).",
            IdStrings
        )
        assertFalse(idSanitized.contains("starter_pull"))
        assertFalse(idSanitized.contains("todayWorkoutStatus"))
        assertFalse(idSanitized.contains("REST_DAY"))

        // End-to-end AI service sanitizes emitted assistant message while keeping internal snapshot working
        service.sendMessage("check yesterday workout", null, EnStrings, AppLanguage.EN)
        repeat(40) {
            if (!service.isWorking.value && service.messages.value.size >= 2) return@repeat
            delay(25)
        }
        val reply = service.messages.value.last().text
        assertFalse(reply.contains("yesterdayWorkedOut"))
        assertFalse(reply.contains("starter_pull"))
        assertTrue(reply.contains(EnStrings.routinePullDayTitle))
    }

    @Test
    fun testRichMarkdownLinksAndInlineImagesAreExtractedAndClickable() {
        val sample = """
            1. Cek panduan [Examine Creatine](https://examine.com/supplements/creatine)
            ![Bench Diagram](https://cdn.example.com/bench.png)
            Atau kunjungi https://jissn.biomedcentral.com/articles/10.1186.
        """.trimIndent()

        val extracted = extractAssistantRichContent(
            rawText = sample,
            attachedImages = listOf("file:///android_asset/images/0025-EIeI8Vf.jpg")
        )
        assertFalse(extracted.cleanText.contains("![Bench Diagram]"))
        assertEquals(
            listOf("file:///android_asset/images/0025-EIeI8Vf.jpg", "https://cdn.example.com/bench.png"),
            extracted.images
        )
        assertEquals(2, extracted.webLinks.size)
        assertEquals("https://examine.com/supplements/creatine", extracted.webLinks[0].url)
        assertEquals("https://jissn.biomedcentral.com/articles/10.1186", extracted.webLinks[1].url)

        val annotated = parseChatMarkdown(extracted.cleanText, Color.DarkGray, Color.Green)
        assertTrue(annotated.text.contains("1. Cek panduan Examine Creatine"))
        val links = annotated.getLinkAnnotations(0, annotated.text.length)
        assertEquals(2, links.size)
        assertEquals("https://examine.com/supplements/creatine", (links[0].item as LinkAnnotation.Url).url)
        assertEquals("https://jissn.biomedcentral.com/articles/10.1186", (links[1].item as LinkAnnotation.Url).url)
    }

    @Test
    fun testAssistantShowExerciseVisualsAndGenerateImagePersistInMessage() = runBlocking {
        exerciseRepo = ExerciseRepository(
            initialExercises = listOf(
                Exercise(
                    id = "ex_visual_bench",
                    name = "Barbell Bench Press",
                    primaryMuscle = MuscleGroup.CHEST,
                    equipment = Equipment.BARBELL,
                    movementType = MovementType.COMPOUND,
                    thumbnailUrl = "images/0025-EIeI8Vf.jpg",
                    gifUrl = "anim/0025-EIeI8Vf.webp"
                )
            ),
            databaseRepo = dbRepo
        )

        val postResponses = ArrayDeque(
            listOf(
                AiHttpResponse(
                    200,
                    """{"candidates":[{"content":{"role":"model","parts":[{"functionCall":{"name":"show_exercise_visuals","args":{"exerciseIds":["ex_visual_bench"]}}},{"functionCall":{"name":"generate_image","args":{"prompt":"gym dumbbell illustration"}}}]}}]}"""
                ),
                AiHttpResponse(
                    200,
                    """{"candidates":[{"content":{"role":"model","parts":[{"inlineData":{"mimeType":"image/png","data":"AQID"}}]}}]}"""
                ),
                AiHttpResponse(
                    200,
                    """{"candidates":[{"content":{"role":"model","parts":[{"text":"Ini visual gerakan dan ilustrasinya!"}]}}]}"""
                )
            )
        )

        val transport = object : AiHttpTransport {
            override suspend fun get(url: String, headers: Map<String, String>, timeoutMs: Int) =
                AiHttpResponse(200, """{"models":[{"name":"models/gemini-2.5-flash-image","supportedGenerationMethods":["generateContent"]}]}""")
            override suspend fun postJson(url: String, headers: Map<String, String>, body: String, timeoutMs: Int) =
                postResponses.removeFirst()
        }

        val executor = KumaAiToolExecutor(
            exerciseRepo, routineRepo, programRepo, scheduleRepo,
            mesocycleRepo, readinessRepo, prRepo, workoutRepo, settingsRepo, dbRepo
        )
        val service = KumaAiAgentService(
            geminiClient = GeminiApiClient(transport),
            toolExecutor = executor,
            secretStore = InMemoryAiSecretStore("k"),
            settingsRepository = settingsRepo,
            databaseRepo = dbRepo,
            generatedImageSaver = { _, _ -> "/data/user/0/app/files/ai_chat_images/ai_img_gen_1.jpg" }
        )

        service.sendMessage("tunjukin gambar bench press & buatin ilustrasinya", null, IdStrings, AppLanguage.ID)
        repeat(50) {
            if (!service.isWorking.value && service.messages.value.size >= 2) return@repeat
            delay(25)
        }

        val reply = service.messages.value.last()
        assertFalse(reply.isError)
        assertEquals(
            listOf(
                "file:///android_asset/images/0025-EIeI8Vf.jpg",
                "/data/user/0/app/files/ai_chat_images/ai_img_gen_1.jpg"
            ),
            reply.effectiveImagePaths
        )
        val reloaded = dbRepo.getAiChatMessages(reply.threadId).last()
        assertEquals(reply.effectiveImagePaths, reloaded.effectiveImagePaths)
    }

    @Test
    fun testSanitizerPreservesFourDigitNumbersEscapesDollarSignsAndShowsEmptyKeyErrorInNewChat() = runBlocking {
        exerciseRepo = ExerciseRepository(
            initialExercises = listOf(
                Exercise(
                    id = "1700",
                    name = "Dumbbell Push Press",
                    primaryMuscle = MuscleGroup.SHOULDERS,
                    equipment = Equipment.DUMBBELL,
                    movementType = MovementType.COMPOUND
                ),
                Exercise(
                    id = "ex_custom_dollar",
                    name = "Gain \$100 Curl",
                    primaryMuscle = MuscleGroup.ARMS,
                    equipment = Equipment.DUMBBELL,
                    movementType = MovementType.ISOLATION
                )
            ),
            databaseRepo = dbRepo
        )
        val executor = KumaAiToolExecutor(
            exerciseRepo, routineRepo, programRepo, scheduleRepo,
            mesocycleRepo, readinessRepo, prRepo, workoutRepo, settingsRepo, dbRepo
        )

        val cleaned = executor.sanitizeUserFacingText(
            "Target kalori 1700 kcal dan coba ex_custom_dollar.",
            IdStrings
        )
        assertTrue(cleaned.contains("1700 kcal"), "4-digit numbers must not be rewritten as built-in numeric exercise IDs")
        assertTrue(cleaned.contains("Gain \$100 Curl"))

        // Blank API key in fresh non-temp chat must emit visible error message
        val transport = object : AiHttpTransport {
            override suspend fun get(url: String, headers: Map<String, String>, timeoutMs: Int) =
                AiHttpResponse(200, """{"name":{"nested":"invalid"}}""")
            override suspend fun postJson(url: String, headers: Map<String, String>, body: String, timeoutMs: Int) =
                error("Unexpected POST")
        }
        val client = GeminiApiClient(transport)
        val verifyRes = client.verifyConnection("k", "gemini-2.5-flash")
        assertTrue(verifyRes.isFailure)

        val noKeyService = KumaAiAgentService(
            geminiClient = client,
            toolExecutor = executor,
            secretStore = InMemoryAiSecretStore(""),
            settingsRepository = settingsRepo,
            databaseRepo = dbRepo
        )
        noKeyService.sendMessage("halo", null, IdStrings, AppLanguage.ID)
        assertEquals(1, noKeyService.messages.value.size)
        assertTrue(noKeyService.messages.value.first().isError)
        assertEquals(IdStrings.aiErrorInvalidKey, noKeyService.messages.value.first().text)
    }

    @Test
    fun testMarkdownTableSplittingAndWebToolsAndPureAgentStages() = runBlocking {
        val rawMarkdown = """
            Berikut perbandingan split latihan:

            | Program | Hari | Fokus |
            | :--- | :---: | ---: |
            | Push Pull Legs | 6 | Hipertrofi |
            | Upper / Lower | 4 | **Kekuatan** |

            Lihat [Panduan Latihan](https://example.com/guide).
        """.trimIndent()
        val blocks = splitChatMarkdownBlocks(rawMarkdown)
        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is ChatMarkdownBlock.TextBlock)
        val table = blocks[1] as ChatMarkdownBlock.TableBlock
        assertEquals(listOf("Program", "Hari", "Fokus"), table.headers)
        assertEquals(2, table.rows.size)
        assertEquals(listOf("Upper / Lower", "4", "**Kekuatan**"), table.rows[1])
        assertTrue(blocks[2] is ChatMarkdownBlock.TextBlock)

        val (pageTitle, pageBody) = extractReadableWebContent(
            "<html><head><title>Creatine &amp; Strength</title><script>bad()</script></head><body><h1>Study</h1><p>5g daily works well.</p></body></html>"
        )
        assertEquals("Creatine & Strength", pageTitle)
        assertTrue(pageBody.contains("Study"))
        assertTrue(pageBody.contains("5g daily works well."))
        assertFalse(pageBody.contains("bad()"))

        val executor = KumaAiToolExecutor(
            exerciseRepo, routineRepo, programRepo, scheduleRepo,
            mesocycleRepo, readinessRepo, prRepo, workoutRepo, settingsRepo, dbRepo
        )
        val session = KumaAiToolSessionState()
        executor.executeCall(
            GeminiFunctionCall(
                name = "stage_create_program",
                args = buildJsonObject { put("name", "Powerbuilding Block") }
            ),
            session,
            EnStrings
        )
        executor.executeCall(
            GeminiFunctionCall(
                name = "stage_create_exercise",
                args = buildJsonObject {
                    put("name", "Bulgarian Split Squat")
                    put("primaryMuscle", "LEGS")
                    put("equipment", "DUMBBELL")
                }
            ),
            session,
            EnStrings
        )
        executor.executeCall(
            GeminiFunctionCall(
                name = "stage_save_exercise_note",
                args = buildJsonObject {
                    put("exerciseId", "ex_db_press")
                    put("note", "Keep elbows tucked 45 degrees")
                }
            ),
            session,
            EnStrings
        )
        executor.executeCall(
            GeminiFunctionCall(
                name = "stage_log_readiness",
                args = buildJsonObject {
                    put("sleep", 5)
                    put("soreness", 4)
                    put("energy", 5)
                    put("stress", 4)
                    put("notes", "Ready to hit PRs")
                }
            ),
            session,
            EnStrings
        )
        assertEquals(4, session.stagedOperations.size)
        val proposal = AiActionProposal(
            id = "prop_pure_agent",
            summary = "Create program, exercise, note & readiness",
            operations = session.stagedOperations.toList()
        )
        val (ok, _) = executor.commitProposal(proposal, EnStrings)
        assertTrue(ok)
        assertTrue(programRepo.programs.value.any { it.name == "Powerbuilding Block" })
        assertTrue(exerciseRepo.exercises.value.any { it.name == "Bulgarian Split Squat" })
        assertEquals("Keep elbows tucked 45 degrees", exerciseRepo.getExerciseById("ex_db_press")?.pinnedNote)
        assertNotNull(readinessRepo.todayCheckIn.value)
        assertEquals("Ready to hit PRs", readinessRepo.todayCheckIn.value?.notes)

        val scriptedResponses = ArrayDeque(
            listOf(
                AiHttpResponse(
                    200,
                    """{"candidates":[{"content":{"role":"model","parts":[{"functionCall":{"name":"fetch_web_page","args":{"url":"https://example.com/study"}}}]}}]}"""
                ),
                AiHttpResponse(
                    200,
                    """{"candidates":[{"content":{"role":"model","parts":[{"text":"Ringkasan riset: volume 10-20 set per minggu efektif."}]}}]}"""
                )
            )
        )
        val transport = object : AiHttpTransport {
            override suspend fun get(url: String, headers: Map<String, String>, timeoutMs: Int): AiHttpResponse {
                return AiHttpResponse(200, "<html><title>Hypertrophy Review</title><body><p>10 to 20 sets per week.</p></body></html>")
            }

            override suspend fun postJson(url: String, headers: Map<String, String>, body: String, timeoutMs: Int): AiHttpResponse {
                return scriptedResponses.removeFirst()
            }
        }
        val service = KumaAiAgentService(
            geminiClient = GeminiApiClient(transport),
            toolExecutor = executor,
            secretStore = InMemoryAiSecretStore("valid_test_key"),
            settingsRepository = settingsRepo,
            databaseRepo = dbRepo,
            httpTransport = transport
        )
        service.sendMessage("Cari info volume latihan", null, IdStrings, AppLanguage.ID)
        var guard = 0
        while (service.isWorking.value && guard < 100) {
            delay(20)
            guard++
        }
        val assistant = service.messages.value.lastOrNull { it.role == AiChatRole.ASSISTANT }
        assertNotNull(assistant)
        assertTrue(assistant.text.contains("Hypertrophy Review"))
        assertTrue(assistant.text.contains("https://example.com/study"))
    }
}
