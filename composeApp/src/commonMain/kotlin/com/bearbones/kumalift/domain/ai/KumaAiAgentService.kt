package com.bearbones.kumalift.domain.ai

import com.bearbones.kumalift.core.ai.AiSecretStore
import com.bearbones.kumalift.core.ai.loadCompressedImageBase64
import com.bearbones.kumalift.core.localization.AppStrings
import com.bearbones.kumalift.core.util.IdGenerator
import com.bearbones.kumalift.core.util.currentEpochMillis
import com.bearbones.kumalift.data.local.KumaDatabaseRepository
import com.bearbones.kumalift.data.repository.SettingsRepository
import com.bearbones.kumalift.domain.model.AppLanguage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlin.coroutines.coroutineContext

class KumaAiAgentService(
    private val geminiClient: GeminiApiClient,
    private val toolExecutor: KumaAiToolExecutor,
    private val secretStore: AiSecretStore,
    private val settingsRepository: SettingsRepository,
    private val databaseRepo: KumaDatabaseRepository? = null,
    private val imageLoader: suspend (String) -> Pair<String, String>? = ::loadCompressedImageBase64,
    private val nowProvider: () -> LocalDateTime = {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    }
) {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var activeJob: Job? = null

    private val _messages = MutableStateFlow(databaseRepo?.getAllAiChatMessages() ?: emptyList())
    val messages: StateFlow<List<AiChatMessage>> = _messages.asStateFlow()

    private val _isWorking = MutableStateFlow(false)
    val isWorking: StateFlow<Boolean> = _isWorking.asStateFlow()

    private val _liveStepLogs = MutableStateFlow<List<String>>(emptyList())
    val liveStepLogs: StateFlow<List<String>> = _liveStepLogs.asStateFlow()

    fun sendMessage(
        promptText: String,
        imagePath: String?,
        strings: AppStrings,
        language: AppLanguage
    ) {
        val cleanText = promptText.trim()
        if (cleanText.isBlank() && imagePath.isNullOrBlank()) return
        if (_isWorking.value) return

        val apiKey = secretStore.getGeminiApiKey()
        if (apiKey.isBlank()) {
            appendMessage(
                AiChatMessage(
                    id = IdGenerator.nextId("aimsg"),
                    role = AiChatRole.ASSISTANT,
                    text = strings.aiErrorInvalidKey,
                    isError = true,
                    createdAtEpochMs = currentEpochMillis()
                )
            )
            return
        }

        val userMsg = AiChatMessage(
            id = IdGenerator.nextId("aimsg"),
            role = AiChatRole.USER,
            text = cleanText,
            imagePath = imagePath,
            createdAtEpochMs = currentEpochMillis()
        )
        appendMessage(userMsg)

        _isWorking.value = true
        _liveStepLogs.value = listOf(strings.aiThinkingStatus)

        activeJob = serviceScope.launch {
            try {
                val modelId = settingsRepository.settings.value.ai.modelId.ifBlank { GeminiApiClient.DEFAULT_MODEL }
                val isCasual = imagePath.isNullOrBlank() && isCasualCheckMessage(cleanText)
                val reasoningDepth = resolveReasoningDepth(
                    isCasual = isCasual,
                    hasImage = !imagePath.isNullOrBlank(),
                    promptText = cleanText
                )
                val snapshotJson = if (isCasual) null else toolExecutor.buildFastContextSnapshot(strings)
                val systemPrompt = buildSystemPrompt(
                    preferencesNote = settingsRepository.settings.value.ai.preferencesNote,
                    language = language,
                    strings = strings,
                    nowDt = nowProvider(),
                    snapshotJson = snapshotJson
                )
                val historyWindow = _messages.value
                    .dropLast(1)
                    .filter { !it.isError && it.text.isNotBlank() }
                    .takeLast(SLIDING_WINDOW_COUNT)

                val contents = mutableListOf<JsonObject>()
                historyWindow.forEach { msg ->
                    contents.add(buildJsonObject {
                        put("role", if (msg.role == AiChatRole.USER) "user" else "model")
                        putJsonArray("parts") {
                            add(buildTextPart(msg.text))
                        }
                    })
                }

                val currentParts = mutableListOf<JsonObject>()
                if (!imagePath.isNullOrBlank()) {
                    val encoded = imageLoader(imagePath)
                    if (encoded != null) {
                        currentParts.add(buildInlineImagePart(encoded.first, encoded.second))
                    }
                }
                val promptBody = cleanText.ifBlank {
                    "Analyze this photo in the context of my KumaLift training and goals."
                }
                currentParts.add(buildTextPart(promptBody))
                contents.add(buildJsonObject {
                    put("role", "user")
                    put("parts", JsonArray(currentParts))
                })

                val sessionState = KumaAiToolSessionState()
                val recordedSteps = mutableListOf<String>()
                val declarations = if (isCasual) {
                    JsonArray(emptyList())
                } else {
                    toolExecutor.functionDeclarations()
                }
                val allowWebSearch = !isCasual
                val allowUrlContext = allowWebSearch && URL_REGEX.containsMatchIn(cleanText)
                var activeModelId = modelId
                val failedModelsThisRequest = mutableSetOf<String>()
                var cachedLiveModels: List<String>? = null
                var finalResponseText = ""
                var latestWebSources = emptyList<String>()

                for (round in 0 until MAX_TOOL_ROUNDS) {
                    coroutineContext.ensureActive()
                    val turn = try {
                        geminiClient.generateContent(
                            apiKey = apiKey,
                            modelId = activeModelId,
                            systemInstruction = systemPrompt,
                            contents = JsonArray(contents),
                            functionDeclarations = declarations,
                            enableWebSearch = allowWebSearch,
                            enableUrlContext = allowUrlContext,
                            reasoningDepth = reasoningDepth
                        )
                    } catch (e: GeminiApiException) {
                        if (e.errorType != GeminiErrorType.MODEL_UNAVAILABLE &&
                            e.errorType != GeminiErrorType.QUOTA_EXCEEDED
                        ) {
                            throw e
                        }
                        val shouldPersistSwitch = e.errorType == GeminiErrorType.MODEL_UNAVAILABLE &&
                            activeModelId == modelId
                        failedModelsThisRequest.add(activeModelId)
                        var lastErr: GeminiApiException = e
                        val live = cachedLiveModels ?: geminiClient.listAvailableModels(apiKey)
                            .getOrNull().orEmpty().also { cachedLiveModels = it }
                        val preferFreeOnly = !GeminiApiClient.isPaidOnlyModel(activeModelId)
                        val pool = live.ifEmpty { GeminiApiClient.AVAILABLE_MODELS }
                            .filter { candidate ->
                                !failedModelsThisRequest.contains(candidate) &&
                                    (!preferFreeOnly || !GeminiApiClient.isPaidOnlyModel(candidate))
                            }

                        var recoveredTurn: GeminiTurnResult? = null
                        val triedPool = pool.toMutableList()
                        for (attempt in 0 until MAX_MODEL_FAILOVERS) {
                            val candidate = GeminiApiClient.pickPreferredChatModel(triedPool) ?: break
                            triedPool.remove(candidate)
                            failedModelsThisRequest.add(candidate)
                            try {
                                val nextTurn = geminiClient.generateContent(
                                    apiKey = apiKey,
                                    modelId = candidate,
                                    systemInstruction = systemPrompt,
                                    contents = JsonArray(contents),
                                    functionDeclarations = declarations,
                                    enableWebSearch = allowWebSearch,
                                    enableUrlContext = allowUrlContext,
                                    reasoningDepth = reasoningDepth
                                )
                                activeModelId = candidate
                                // Only persist model change if user's chosen model was retired/404;
                                // transient 429 quota hits must not silently overwrite the selected model.
                                if (shouldPersistSwitch) {
                                    settingsRepository.update { cur ->
                                        cur.copy(ai = cur.ai.copy(modelId = candidate))
                                    }
                                }
                                recoveredTurn = nextTurn
                                break
                            } catch (nextErr: GeminiApiException) {
                                lastErr = nextErr
                                if (nextErr.errorType != GeminiErrorType.MODEL_UNAVAILABLE &&
                                    nextErr.errorType != GeminiErrorType.QUOTA_EXCEEDED
                                ) {
                                    throw nextErr
                                }
                            }
                        }
                        recoveredTurn ?: throw lastErr
                    }

                    if (turn.webSources.isNotEmpty()) {
                        latestWebSources = (latestWebSources + turn.webSources).distinct().take(4)
                    }

                    if (turn.functionCalls.isEmpty()) {
                        finalResponseText = turn.text
                        break
                    }

                    contents.add(buildJsonObject {
                        put("role", "model")
                        put("parts", turn.rawModelParts)
                    })

                    val responseParts = buildJsonArray {
                        turn.functionCalls.forEach { fnCall ->
                            val stepLabel = toolExecutor.stepLabelForCall(fnCall.name, strings)
                            if (!recordedSteps.contains(stepLabel)) {
                                recordedSteps.add(stepLabel)
                                _liveStepLogs.value = recordedSteps.toList()
                            }
                            val resultPayload = toolExecutor.executeCall(fnCall, sessionState, strings)
                            add(buildFunctionResponsePart(fnCall.name, resultPayload))
                        }
                    }

                    contents.add(buildJsonObject {
                        put("role", "user")
                        put("parts", responseParts)
                    })

                    if (round == MAX_TOOL_ROUNDS - 1 && turn.text.isNotBlank()) {
                        finalResponseText = turn.text
                    }
                }

                val proposal = if (sessionState.stagedOperations.isNotEmpty()) {
                    AiActionProposal(
                        id = IdGenerator.nextId("aiprop"),
                        summary = finalResponseText,
                        status = AiProposalStatus.PENDING,
                        operations = sessionState.stagedOperations.toList()
                    )
                } else {
                    null
                }

                val baseText = finalResponseText.ifBlank {
                    if (proposal != null || sessionState.nutritionCard != null) "" else strings.aiErrorEmptyResponse
                }
                val isErrorResult = baseText == strings.aiErrorEmptyResponse
                val resolvedText = if (!isErrorResult && baseText.isNotBlank() && latestWebSources.isNotEmpty()) {
                    "$baseText\n\n${strings.aiWebSourcesPrefix}: ${latestWebSources.joinToString(" · ")}"
                } else {
                    baseText
                }

                appendMessage(
                    AiChatMessage(
                        id = IdGenerator.nextId("aimsg"),
                        role = AiChatRole.ASSISTANT,
                        text = resolvedText,
                        stepLogs = recordedSteps.toList(),
                        proposal = proposal,
                        nutritionCard = sessionState.nutritionCard,
                        isError = isErrorResult,
                        createdAtEpochMs = currentEpochMillis()
                    )
                )
            } catch (e: CancellationException) {
                appendMessage(
                    AiChatMessage(
                        id = IdGenerator.nextId("aimsg"),
                        role = AiChatRole.ASSISTANT,
                        text = strings.aiErrorCancelled,
                        isError = true,
                        createdAtEpochMs = currentEpochMillis()
                    )
                )
            } catch (e: GeminiApiException) {
                appendMessage(
                    AiChatMessage(
                        id = IdGenerator.nextId("aimsg"),
                        role = AiChatRole.ASSISTANT,
                        text = e.localizedSummary(strings),
                        isError = true,
                        createdAtEpochMs = currentEpochMillis()
                    )
                )
            } catch (_: Exception) {
                appendMessage(
                    AiChatMessage(
                        id = IdGenerator.nextId("aimsg"),
                        role = AiChatRole.ASSISTANT,
                        text = strings.aiErrorNetwork,
                        isError = true,
                        createdAtEpochMs = currentEpochMillis()
                    )
                )
            } finally {
                _isWorking.value = false
                _liveStepLogs.value = emptyList()
                activeJob = null
            }
        }
    }

    fun stopCurrentRequest() {
        activeJob?.cancel()
    }

    fun approveProposal(messageId: String, strings: AppStrings) {
        val target = _messages.value.firstOrNull { it.id == messageId } ?: return
        val proposal = target.proposal ?: return
        if (proposal.status != AiProposalStatus.PENDING) return

        val (ok, detail) = toolExecutor.commitProposal(proposal)
        val updatedProposal = proposal.copy(
            status = if (ok) AiProposalStatus.APPLIED else AiProposalStatus.FAILED,
            resultNote = if (ok) "${strings.aiCommitSuccessPrefix} $detail" else detail
        )
        val updatedMsg = target.copy(proposal = updatedProposal)
        replaceMessage(updatedMsg)
    }

    fun cancelProposal(messageId: String) {
        val target = _messages.value.firstOrNull { it.id == messageId } ?: return
        val proposal = target.proposal ?: return
        if (proposal.status != AiProposalStatus.PENDING) return

        val updatedMsg = target.copy(
            proposal = proposal.copy(status = AiProposalStatus.CANCELLED)
        )
        replaceMessage(updatedMsg)
    }

    fun clearChat() {
        activeJob?.cancel()
        databaseRepo?.clearAllAiChatMessages()
        _messages.value = emptyList()
    }

    private fun appendMessage(message: AiChatMessage) {
        _messages.update { it + message }
        databaseRepo?.saveAiChatMessage(message)
    }

    private fun replaceMessage(message: AiChatMessage) {
        _messages.update { list -> list.map { if (it.id == message.id) message else it } }
        databaseRepo?.saveAiChatMessage(message)
    }

    private fun buildSystemPrompt(
        preferencesNote: String,
        language: AppLanguage,
        strings: AppStrings,
        nowDt: LocalDateTime,
        snapshotJson: String?
    ): String {
        val today = buildAiTodayContext(nowDt, strings)
        val langHint = when (language) {
            AppLanguage.ID -> "Always reply in natural, concise Indonesian (Bahasa Indonesia)."
            AppLanguage.EN -> "Always reply in natural, concise English."
            AppLanguage.SYSTEM -> "Reply in the user's message language (Indonesian or English)."
        }
        val userNotesBlock = if (preferencesNote.isNotBlank()) {
            "\nPersistent user training notes/constraints: $preferencesNote"
        } else {
            ""
        }
        if (snapshotJson.isNullOrBlank()) {
            return """
You are Kuma AI inside KumaLift. $langHint
Authoritative clock today: ${today.dayNameLocalized} (${today.dayNameEn}), ${today.isoDate} ${today.hhMm}.
Reply briefly and naturally only to what the user asked.$userNotesBlock
            """.trimIndent()
        }
        val snapshotBlock = "\n\nLive KumaLift DB Snapshot (fresh on this turn):\n$snapshotJson"
        return """
You are Kuma AI, the helpful AI coach and personal assistant inside KumaLift.
Authoritative device clock today: ${today.dayNameLocalized} (${today.dayNameEn}), ${today.isoDate} ${today.hhMm} (ISO dayOfWeek=${today.isoDayNumber} where 1=Monday..7=Sunday). Yesterday was ${today.yesterdayDate}.
$langHint$userNotesBlock$snapshotBlock

Strict Rules:
1. Answer strictly what the user actually asked in their latest message. Do NOT bring up unrelated topics, unsolicited routines, or dump DB stats when the user asks a general question.
2. NEVER invent or guess dates, months, years, or weekdays — always use the authoritative dates above (Today: `${today.dayNameLocalized}, ${today.isoDate}`, Yesterday: `${today.yesterdayDate}`).
3. Fast Data Routing & Workout History Truth:
   - **Checking whether the user worked out today/yesterday ("kemarin udah latihan belum?", "tadi latihan apa?")**: ALWAYS inspect `yesterdayWorkedOut`, `yesterdayWorkouts`, `todayWorkedOut`, `todayCompletedWorkouts`, and `recentWorkoutsTop5` (each includes exact `date`, `dayOfWeek`, `time`, and `relativeDay`). If a completed workout exists for `${today.yesterdayDate}`, report YES with its title, exercises, duration, and volume. NEVER claim the user did not work out yesterday without checking those fields AND calling `get_recent_workouts` if older history might be needed.
   - **Local DB first**: When the answer is already in `Live KumaLift DB Snapshot`, answer **directly in 1 turn** using those exact figures. Call `get_routines`, `get_recent_workouts`, or `search_exercises` when you need full exercise configs (`targetWeightKg`, `setConfigs`, `routineId`) or deeper history.
   - **Internet (`googleSearch` & `urlContext`)**: When external facts are needed (nutrition facts, studies, form/programming science, articles, web queries), use Google Search freely when available and synthesize it with DB data cleanly. Think step-by-step before answering. Never fabricate studies, quotes, or live web statistics if no live web search grounding results were returned on this turn.
4. For greetings or quick checks ("test", "ping", "halo"), reply directly in concise plain text without calling tools.
5. Schedule truth in KumaLift: Today's workout uses `todayScheduledWorkouts` (weekly routine/program schedule for `ISO dayOfWeek=${today.isoDayNumber}`, matching Home's Today's Schedule) AND `todayCalendarEvents`. Report exact routine title(s) cleanly.
6. Edit vs. Create Routine (CRITICAL — NO DUPLICATES):
   - Whenever the user asks to edit, adjust, update, fix, match, or modify an existing routine (visible in `savedRoutines` or via `get_routines`), ALWAYS call `stage_update_routine` with its existing `routineId`. NEVER call `stage_create_routine` for an update request.
   - Only call `stage_create_routine` when creating a brand-new routine that does not exist yet.
7. Screenshots & Images (READ EVERY DETAIL):
   - When the user sends a workout screenshot, table, or note photo: carefully read ALL visible exercises, exact set counts, rep numbers/ranges, load weights in `kg` (`targetWeightKg` / per-set `setConfigs`), and rest timers. Map each movement to real IDs via `search_exercises` and preserve every visible `reps`, `kg`, `sets`, and `restTimerSeconds` in `stage_update_routine` or `stage_create_routine` without dropping weights.
   - For gym equipment/form/food photos: answer strictly from what is visible (`present_nutrition_analysis` for food/labels).
8. Never invent or guess `exerciseId` strings — call `search_exercises` before staging routine changes. Always use `stage_*` tools so the user reviews the proposal card before permanent save.
9. Reason clearly, verify numbers against the DB/image before replying, and keep responses natural and concise.
        """.trimIndent()
    }

    private fun resolveReasoningDepth(
        isCasual: Boolean,
        hasImage: Boolean,
        promptText: String
    ): AiReasoningDepth {
        if (isCasual) return AiReasoningDepth.FAST_NONE
        if (hasImage) return AiReasoningDepth.DEEP
        val lower = promptText.lowercase()
        val isComplex = COMPLEX_PROMPT_KEYWORDS.any { lower.contains(it) }
        return if (isComplex) AiReasoningDepth.DEEP else AiReasoningDepth.BALANCED
    }

    private fun isCasualCheckMessage(text: String): Boolean {
        val normalized = text.trim().lowercase()
        if (normalized.isEmpty()) return false
        return normalized in CASUAL_CHECK_INPUTS
    }

    companion object {
        private const val SLIDING_WINDOW_COUNT = 14
        private const val MAX_TOOL_ROUNDS = 8
        private const val MAX_MODEL_FAILOVERS = 3
        private val URL_REGEX = Regex("""https?://\S+""", RegexOption.IGNORE_CASE)
        private val CASUAL_CHECK_INPUTS = setOf(
            "test", "tes", "ping", "hi", "hello", "halo", "hai", "hey", "ok", "oke", "cek"
        )
        private val COMPLEX_PROMPT_KEYWORDS = listOf(
            "buat", "bikin", "edit", "ubah", "ganti", "update", "sesuaikan", "create",
            "modify", "adjust", "routine", "rutinitas", "program", "jadwal", "schedule",
            "analisis", "analyze", "compare", "banding", "riset", "study", "studi", "kenapa", "mengapa", "why", "how"
        )
    }
}
