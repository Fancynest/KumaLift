package com.bearbones.kumalift.domain.ai

import com.bearbones.kumalift.core.ai.AiHttpTransport
import com.bearbones.kumalift.core.ai.AiSecretStore
import com.bearbones.kumalift.core.ai.MAX_AI_CHAT_IMAGES
import com.bearbones.kumalift.core.ai.PlatformAiHttpTransport
import com.bearbones.kumalift.core.ai.deleteAiChatImageFile
import com.bearbones.kumalift.core.ai.loadCompressedImageBase64
import com.bearbones.kumalift.core.ai.saveAiGeneratedImageBase64
import com.bearbones.kumalift.core.localization.AppStrings
import com.bearbones.kumalift.core.util.IdGenerator
import com.bearbones.kumalift.core.util.currentEpochMillis
import com.bearbones.kumalift.data.local.KumaDatabaseRepository
import com.bearbones.kumalift.data.repository.SettingsRepository
import com.bearbones.kumalift.domain.model.AppLanguage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
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
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
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
    private val generatedImageSaver: suspend (String, String) -> String? = ::saveAiGeneratedImageBase64,
    private val nowProvider: () -> LocalDateTime = {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    },
    private val httpTransport: AiHttpTransport = PlatformAiHttpTransport
) {
    private class ActiveRequestControl(
        @kotlin.concurrent.Volatile var suppressCancelBanner: Boolean = false,
        var job: Job? = null
    )

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    @kotlin.concurrent.Volatile
    private var activeRequest: ActiveRequestControl? = null

    private val _threads = MutableStateFlow<List<AiChatThread>>(emptyList())
    val threads: StateFlow<List<AiChatThread>> = _threads.asStateFlow()

    private val _currentThreadId = MutableStateFlow<String?>(null)
    val currentThreadId: StateFlow<String?> = _currentThreadId.asStateFlow()

    private val _isTemporaryChat = MutableStateFlow(false)
    val isTemporaryChat: StateFlow<Boolean> = _isTemporaryChat.asStateFlow()

    private val _messages = MutableStateFlow<List<AiChatMessage>>(emptyList())
    val messages: StateFlow<List<AiChatMessage>> = _messages.asStateFlow()

    private val _isWorking = MutableStateFlow(false)
    val isWorking: StateFlow<Boolean> = _isWorking.asStateFlow()

    private val _liveStepLogs = MutableStateFlow<List<String>>(emptyList())
    val liveStepLogs: StateFlow<List<String>> = _liveStepLogs.asStateFlow()

    init {
        reloadFromDatabase()
    }

    fun reloadFromDatabase() {
        val repo = databaseRepo ?: return
        val saved = repo.getAllAiChatThreads()
        _threads.value = saved
        val desiredId = repo.getSavedActiveAiThreadId()
        val target = desiredId?.let { id -> saved.firstOrNull { it.id == id } }
        _isTemporaryChat.value = false
        if (target != null) {
            _currentThreadId.value = target.id
            _messages.value = repo.getAiChatMessages(target.id)
        } else {
            _currentThreadId.value = null
            _messages.value = emptyList()
            if (desiredId != null) {
                repo.setSavedActiveAiThreadId(null)
            }
        }
    }

    fun sendMessage(
        promptText: String,
        imagePath: String?,
        strings: AppStrings,
        language: AppLanguage
    ) {
        sendMessage(
            promptText = promptText,
            imagePaths = listOfNotNull(imagePath),
            strings = strings,
            language = language
        )
    }

    fun sendMessage(
        promptText: String,
        imagePaths: List<String>,
        strings: AppStrings,
        language: AppLanguage
    ) {
        val cleanText = promptText.trim()
        val resolvedImages = imagePaths
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(MAX_ATTACHED_IMAGES)
        if (cleanText.isBlank() && resolvedImages.isEmpty()) return
        if (_isWorking.value) return

        val apiKey = secretStore.getGeminiApiKey()
        if (apiKey.isBlank()) {
            val targetThreadId = if (_isTemporaryChat.value) TEMP_THREAD_ID else (_currentThreadId.value ?: TEMP_THREAD_ID)
            val errMsg = AiChatMessage(
                id = IdGenerator.nextId("aimsg"),
                role = AiChatRole.ASSISTANT,
                text = strings.aiErrorInvalidKey,
                threadId = targetThreadId,
                isError = true,
                createdAtEpochMs = currentEpochMillis()
            )
            if (isVisibleThread(targetThreadId) || (!_isTemporaryChat.value && _currentThreadId.value == null)) {
                _messages.update { it + errMsg }
            }
            return
        }

        val isTemp = _isTemporaryChat.value
        val activeThreadId = if (isTemp) {
            TEMP_THREAD_ID
        } else {
            ensureActiveThread(initialTitleHint = cleanText.ifBlank { strings.aiTitle })
        }

        val userMsg = AiChatMessage(
            id = IdGenerator.nextId("aimsg"),
            role = AiChatRole.USER,
            text = cleanText,
            threadId = activeThreadId,
            imagePath = resolvedImages.firstOrNull(),
            imagePaths = resolvedImages,
            createdAtEpochMs = currentEpochMillis()
        )
        appendMessage(userMsg, isTemporary = isTemp)
        executeAssistantTurn(
            promptUserMsg = userMsg,
            apiKey = apiKey,
            isTemp = isTemp,
            strings = strings,
            language = language
        )
    }

    /**
     * Updates an existing user prompt in place, discards any outdated responses and subsequent turns
     * after it so conversation context remains consistent, and generates a fresh AI reply.
     */
    fun editUserMessage(
        messageId: String,
        newText: String,
        strings: AppStrings,
        language: AppLanguage
    ): Boolean {
        if (_isWorking.value) return false
        val currentList = _messages.value
        val targetIdx = currentList.indexOfFirst { it.id == messageId }
        if (targetIdx < 0) return false
        val target = currentList[targetIdx]
        if (target.role != AiChatRole.USER) return false

        val cleanText = newText.trim()
        if (cleanText.isBlank() && target.effectiveImagePaths.isEmpty()) return false

        val updatedUserMsg = target.copy(text = cleanText)
        val removedIds = currentList.drop(targetIdx + 1).map { it.id }
        val retained = currentList.take(targetIdx) + updatedUserMsg
        val isTemp = _isTemporaryChat.value

        _messages.value = retained
        if (!isTemp && updatedUserMsg.threadId != TEMP_THREAD_ID) {
            val repo = databaseRepo
            if (repo != null) {
                repo.runInTransaction {
                    repo.deleteAiChatMessagesByIds(removedIds)
                    repo.saveAiChatMessage(updatedUserMsg)
                }
            }
            val isFirstUserMessage = retained.indexOfFirst { it.role == AiChatRole.USER } == targetIdx
            val updatedTitle = if (isFirstUserMessage) {
                cleanText.replace('\n', ' ').take(48).ifBlank { "Kuma AI" }
            } else {
                null
            }
            syncThreadMetadata(updatedUserMsg.threadId, overrideTitle = updatedTitle)
        }

        val apiKey = secretStore.getGeminiApiKey()
        if (apiKey.isBlank()) {
            appendMessage(
                AiChatMessage(
                    id = IdGenerator.nextId("aimsg"),
                    role = AiChatRole.ASSISTANT,
                    text = strings.aiErrorInvalidKey,
                    threadId = updatedUserMsg.threadId,
                    isError = true,
                    createdAtEpochMs = nextAssistantTimestamp(updatedUserMsg.createdAtEpochMs)
                ),
                isTemporary = isTemp
            )
            return true
        }

        executeAssistantTurn(
            promptUserMsg = updatedUserMsg,
            apiKey = apiKey,
            isTemp = isTemp,
            strings = strings,
            language = language
        )
        return true
    }

    /**
     * Replaces an AI response (and any subsequent stale turns after it) by re-running the
     * preceding user prompt without creating duplicate assistant bubbles.
     */
    fun regenerateResponse(
        assistantMessageId: String,
        strings: AppStrings,
        language: AppLanguage
    ): Boolean {
        if (_isWorking.value) return false
        val currentList = _messages.value
        val targetIdx = currentList.indexOfFirst { it.id == assistantMessageId }
        if (targetIdx < 0) return false
        if (currentList[targetIdx].role != AiChatRole.ASSISTANT) return false

        val promptIdx = (targetIdx - 1 downTo 0)
            .firstOrNull { currentList[it].role == AiChatRole.USER }
            ?: return false
        val promptMsg = currentList[promptIdx]
        val removedIds = currentList.drop(promptIdx + 1).map { it.id }
        val isTemp = _isTemporaryChat.value

        _messages.value = currentList.take(promptIdx + 1)
        if (!isTemp && promptMsg.threadId != TEMP_THREAD_ID) {
            databaseRepo?.deleteAiChatMessagesByIds(removedIds)
            syncThreadMetadata(promptMsg.threadId)
        }

        val apiKey = secretStore.getGeminiApiKey()
        if (apiKey.isBlank()) {
            appendMessage(
                AiChatMessage(
                    id = IdGenerator.nextId("aimsg"),
                    role = AiChatRole.ASSISTANT,
                    text = strings.aiErrorInvalidKey,
                    threadId = promptMsg.threadId,
                    isError = true,
                    createdAtEpochMs = nextAssistantTimestamp(promptMsg.createdAtEpochMs)
                ),
                isTemporary = isTemp
            )
            return true
        }

        executeAssistantTurn(
            promptUserMsg = promptMsg,
            apiKey = apiKey,
            isTemp = isTemp,
            strings = strings,
            language = language
        )
        return true
    }

    private fun nextAssistantTimestamp(promptCreatedAtMs: Long): Long =
        maxOf(currentEpochMillis(), promptCreatedAtMs + 1L)

    private fun executeAssistantTurn(
        promptUserMsg: AiChatMessage,
        apiKey: String,
        isTemp: Boolean,
        strings: AppStrings,
        language: AppLanguage
    ) {
        val cleanText = promptUserMsg.text.trim()
        val resolvedImages = promptUserMsg.effectiveImagePaths
        val activeThreadId = promptUserMsg.threadId

        _isWorking.value = true
        _liveStepLogs.value = listOf(strings.aiThinkingStatus)

        val requestControl = ActiveRequestControl()
        activeRequest = requestControl
        val launched = serviceScope.launch(start = CoroutineStart.LAZY) {
            try {
                val modelId = settingsRepository.settings.value.ai.modelId.ifBlank { GeminiApiClient.DEFAULT_MODEL }
                val isCasual = resolvedImages.isEmpty() && isCasualCheckMessage(cleanText)
                val reasoningDepth = resolveReasoningDepth(
                    isCasual = isCasual,
                    hasImage = resolvedImages.isNotEmpty(),
                    promptText = cleanText
                )
                val snapshotMessages = _messages.value
                val promptIdx = snapshotMessages.indexOfLast { it.id == promptUserMsg.id }
                    .let { if (it >= 0) it else snapshotMessages.lastIndex.coerceAtLeast(0) }
                val validBeforeCurrent = snapshotMessages
                    .take(promptIdx)
                    .filter { !it.isError && historyMessageText(it).isNotBlank() }
                val earlierSummary = if (validBeforeCurrent.size > SLIDING_WINDOW_COUNT) {
                    buildCompactContextSummary(validBeforeCurrent.dropLast(SLIDING_WINDOW_COUNT))
                } else {
                    null
                }
                val snapshotJson = if (isCasual) {
                    null
                } else {
                    toolExecutor.buildFastContextSnapshot(
                        strings = strings,
                        activeThreadId = if (isTemp) null else activeThreadId,
                        includeChatHistories = !isTemp,
                        earlierMessagesSummary = earlierSummary
                    )
                }
                val systemPrompt = buildSystemPrompt(
                    preferencesNote = settingsRepository.settings.value.ai.preferencesNote,
                    language = language,
                    strings = strings,
                    nowDt = nowProvider(),
                    snapshotJson = snapshotJson
                )
                val historyEntries = buildAlternatingHistory(validBeforeCurrent.takeLast(SLIDING_WINDOW_COUNT))
                val trailingUnansweredUserText = if (historyEntries.lastOrNull()?.first == AiChatRole.USER) {
                    historyEntries.removeAt(historyEntries.lastIndex).second
                } else {
                    null
                }

                val contents = mutableListOf<JsonObject>()
                historyEntries.forEach { (role, text) ->
                    contents.add(buildJsonObject {
                        put("role", if (role == AiChatRole.USER) "user" else "model")
                        putJsonArray("parts") {
                            add(buildTextPart(text))
                        }
                    })
                }

                val currentParts = mutableListOf<JsonObject>()
                if (!trailingUnansweredUserText.isNullOrBlank()) {
                    currentParts.add(buildTextPart(trailingUnansweredUserText))
                }
                resolvedImages.forEach { path ->
                    val encoded = imageLoader(path)
                    if (encoded != null) {
                        currentParts.add(buildInlineImagePart(encoded.first, encoded.second))
                    }
                }
                val promptBody = cleanText.ifBlank {
                    if (resolvedImages.size > 1) {
                        "Analyze these ${resolvedImages.size} photos in the context of my KumaLift training and goals."
                    } else {
                        "Analyze this photo in the context of my KumaLift training and goals."
                    }
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
                var lastRoundPreamble = ""
                var latestWebSources = emptyList<String>()
                var latestWebSourceLinks = emptyList<AiWebSource>()
                var brokeWithText = false

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
                        latestWebSources = (latestWebSources + turn.webSources).distinct().take(6)
                    }
                    if (turn.webSourceLinks.isNotEmpty()) {
                        latestWebSourceLinks = (latestWebSourceLinks + turn.webSourceLinks).distinctBy { it.url }.take(8)
                    }

                    if (turn.functionCalls.isEmpty()) {
                        finalResponseText = turn.text
                        brokeWithText = true
                        break
                    }

                    contents.add(buildJsonObject {
                        put("role", "model")
                        put("parts", turn.rawModelParts)
                    })

                    val responseParts = buildJsonArray {
                        for (fnCall in turn.functionCalls) {
                            val stepLabel = toolExecutor.stepLabelForCall(fnCall.name, strings)
                            if (!recordedSteps.contains(stepLabel)) {
                                recordedSteps.add(stepLabel)
                                _liveStepLogs.value = recordedSteps.toList()
                            }
                            val resultPayload = when (fnCall.name) {
                                "generate_image" -> executeGenerateImageCall(apiKey, fnCall.args, sessionState, strings)
                                "search_web_live" -> executeSearchWebLiveCall(apiKey, activeModelId, fnCall.args, sessionState, strings)
                                "fetch_web_page" -> executeFetchWebPageCall(fnCall.args, sessionState)
                                else -> toolExecutor.executeCall(fnCall, sessionState, strings)
                            }
                            add(buildFunctionResponsePart(fnCall.name, resultPayload))
                        }
                    }

                    contents.add(buildJsonObject {
                        put("role", "user")
                        put("parts", responseParts)
                    })

                    if (round == MAX_TOOL_ROUNDS - 1 && turn.text.isNotBlank()) {
                        lastRoundPreamble = turn.text
                    }
                }

                if (!brokeWithText && finalResponseText.isBlank()) {
                    coroutineContext.ensureActive()
                    val summaryTurn = try {
                        geminiClient.generateContent(
                            apiKey = apiKey,
                            modelId = activeModelId,
                            systemInstruction = systemPrompt,
                            contents = JsonArray(contents),
                            functionDeclarations = JsonArray(emptyList()),
                            enableWebSearch = false,
                            enableUrlContext = false,
                            reasoningDepth = reasoningDepth
                        )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: GeminiApiException) {
                        if (sessionState.stagedOperations.isEmpty() &&
                            sessionState.nutritionCard == null &&
                            sessionState.assistantImages.isEmpty()
                        ) {
                            throw e
                        }
                        null
                    }
                    if (summaryTurn != null && summaryTurn.text.isNotBlank()) {
                        finalResponseText = summaryTurn.text
                    } else if (lastRoundPreamble.isNotBlank()) {
                        finalResponseText = lastRoundPreamble
                    }
                }

                val sanitizedFinalText = if (finalResponseText.isNotBlank()) {
                    toolExecutor.sanitizeUserFacingText(finalResponseText, strings)
                } else {
                    ""
                }
                coroutineContext.ensureActive()

                val proposal = if (sessionState.stagedOperations.isNotEmpty()) {
                    AiActionProposal(
                        id = IdGenerator.nextId("aiprop"),
                        summary = sanitizedFinalText,
                        status = AiProposalStatus.PENDING,
                        operations = sessionState.stagedOperations.toList()
                    )
                } else {
                    null
                }

                val attachedAssistantImages = sessionState.assistantImages.distinct().take(MAX_AI_CHAT_IMAGES)
                val baseText = sanitizedFinalText.ifBlank {
                    if (proposal != null || sessionState.nutritionCard != null || attachedAssistantImages.isNotEmpty()) "" else strings.aiErrorEmptyResponse
                }
                val isErrorResult = baseText == strings.aiErrorEmptyResponse
                val allWebLinks = (latestWebSourceLinks + sessionState.webSourceLinks)
                    .distinctBy { it.url }
                    .take(8)
                val formattedSources = when {
                    allWebLinks.isNotEmpty() -> allWebLinks.joinToString(" · ") { src ->
                        "[${src.title.replace("[", "").replace("]", "")}](${src.url})"
                    }
                    latestWebSources.isNotEmpty() -> latestWebSources.joinToString(" · ")
                    else -> ""
                }
                val resolvedText = if (!isErrorResult && baseText.isNotBlank() && formattedSources.isNotBlank()) {
                    "$baseText\n\n${strings.aiWebSourcesPrefix}: $formattedSources"
                } else {
                    baseText
                }

                appendMessage(
                    AiChatMessage(
                        id = IdGenerator.nextId("aimsg"),
                        role = AiChatRole.ASSISTANT,
                        text = resolvedText,
                        threadId = activeThreadId,
                        imagePath = attachedAssistantImages.firstOrNull(),
                        imagePaths = attachedAssistantImages,
                        stepLogs = recordedSteps.toList(),
                        proposal = proposal,
                        nutritionCard = sessionState.nutritionCard,
                        isError = isErrorResult,
                        createdAtEpochMs = nextAssistantTimestamp(promptUserMsg.createdAtEpochMs)
                    ),
                    isTemporary = isTemp
                )
            } catch (e: CancellationException) {
                if (!requestControl.suppressCancelBanner) {
                    appendMessage(
                        AiChatMessage(
                            id = IdGenerator.nextId("aimsg"),
                            role = AiChatRole.ASSISTANT,
                            text = strings.aiErrorCancelled,
                            threadId = activeThreadId,
                            isError = true,
                            createdAtEpochMs = nextAssistantTimestamp(promptUserMsg.createdAtEpochMs)
                        ),
                        isTemporary = isTemp
                    )
                }
            } catch (e: GeminiApiException) {
                appendMessage(
                    AiChatMessage(
                        id = IdGenerator.nextId("aimsg"),
                        role = AiChatRole.ASSISTANT,
                        text = e.localizedSummary(strings),
                        threadId = activeThreadId,
                        isError = true,
                        createdAtEpochMs = nextAssistantTimestamp(promptUserMsg.createdAtEpochMs)
                    ),
                    isTemporary = isTemp
                )
            } catch (_: Exception) {
                appendMessage(
                    AiChatMessage(
                        id = IdGenerator.nextId("aimsg"),
                        role = AiChatRole.ASSISTANT,
                        text = strings.aiErrorNetwork,
                        threadId = activeThreadId,
                        isError = true,
                        createdAtEpochMs = nextAssistantTimestamp(promptUserMsg.createdAtEpochMs)
                    ),
                    isTemporary = isTemp
                )
            } finally {
                if (activeRequest === requestControl) {
                    _isWorking.value = false
                    _liveStepLogs.value = emptyList()
                    activeRequest = null
                }
            }
        }
        requestControl.job = launched
        launched.start()
    }

    private suspend fun executeGenerateImageCall(
        apiKey: String,
        args: JsonObject,
        session: KumaAiToolSessionState,
        strings: AppStrings
    ): kotlinx.serialization.json.JsonElement {
        val prompt = (args["prompt"] as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()
        if (prompt.isBlank()) {
            return buildJsonObject { put("error", "Image prompt cannot be blank") }
        }
        return try {
            val (mimeType, base64Data) = geminiClient.generateImage(apiKey, prompt)
            val savedPath = generatedImageSaver(base64Data, mimeType)
                ?: return buildJsonObject { put("error", "Unable to store generated image on this device") }
            if (!session.assistantImages.contains(savedPath)) {
                session.assistantImages.add(savedPath)
            }
            buildJsonObject {
                put("generated", true)
                put("attachedToMessage", true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: GeminiApiException) {
            buildJsonObject { put("error", e.localizedSummary(strings)) }
        } catch (e: Exception) {
            buildJsonObject { put("error", e.message ?: "Image generation failed") }
        }
    }

    private suspend fun executeSearchWebLiveCall(
        apiKey: String,
        modelId: String,
        args: JsonObject,
        session: KumaAiToolSessionState,
        strings: AppStrings
    ): kotlinx.serialization.json.JsonElement {
        val query = (args["query"] as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()
        if (query.isBlank()) {
            return buildJsonObject { put("error", "Search query cannot be blank") }
        }
        return try {
            val searchContents = JsonArray(listOf(buildJsonObject {
                put("role", "user")
                putJsonArray("parts") {
                    add(buildTextPart(query))
                }
            }))
            val turn = geminiClient.generateContent(
                apiKey = apiKey,
                modelId = modelId,
                systemInstruction = "Search the web accurately and summarize key findings with facts and numbers. Do not fabricate URLs.",
                contents = searchContents,
                functionDeclarations = JsonArray(emptyList()),
                enableWebSearch = true,
                enableUrlContext = false,
                reasoningDepth = AiReasoningDepth.BALANCED
            )
            turn.webSourceLinks.forEach { link ->
                if (session.webSourceLinks.none { it.url == link.url }) {
                    session.webSourceLinks.add(link)
                }
            }
            buildJsonObject {
                put("summary", turn.text)
                put("sources", JsonArray(turn.webSourceLinks.map { src ->
                    buildJsonObject {
                        put("title", src.title)
                        put("url", src.url)
                    }
                }))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: GeminiApiException) {
            buildJsonObject { put("error", e.localizedSummary(strings)) }
        } catch (e: Exception) {
            buildJsonObject { put("error", e.message ?: "Web search failed") }
        }
    }

    private suspend fun executeFetchWebPageCall(
        args: JsonObject,
        session: KumaAiToolSessionState
    ): kotlinx.serialization.json.JsonElement {
        val url = (args["url"] as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()
        if (!url.startsWith("https://", ignoreCase = true) && !url.startsWith("http://", ignoreCase = true)) {
            return buildJsonObject { put("error", "Only http:// or https:// URLs are supported") }
        }
        if (!httpTransport.isNetworkOnline()) {
            return buildJsonObject { put("error", "Device is offline") }
        }
        return try {
            val res = httpTransport.get(
                url = url,
                headers = mapOf("Accept" to "text/html,application/xhtml+xml,text/plain,application/json"),
                timeoutMs = 15_000
            )
            if (res.statusCode !in 200..299) {
                return buildJsonObject { put("error", "HTTP ${res.statusCode}") }
            }
            val (title, readableText) = extractReadableWebContent(res.body)
            val displayTitle = title.ifBlank {
                url.removePrefix("https://").removePrefix("http://").substringBefore("/").ifBlank { url }
            }
            if (session.webSourceLinks.none { it.url == url }) {
                session.webSourceLinks.add(AiWebSource(title = displayTitle, url = url))
            }
            buildJsonObject {
                put("url", url)
                put("title", displayTitle)
                put("content", readableText)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            buildJsonObject { put("error", e.message ?: "Failed to fetch URL") }
        }
    }

    fun stopCurrentRequest() {
        activeRequest?.job?.cancel()
    }

    private fun cancelActiveRequestSilently() {
        val prev = activeRequest
        activeRequest = null
        prev?.let { ctrl ->
            ctrl.suppressCancelBanner = true
            ctrl.job?.cancel()
        }
        _isWorking.value = false
        _liveStepLogs.value = emptyList()
    }

    fun approveProposal(messageId: String, strings: AppStrings) {
        val target = _messages.value.firstOrNull { it.id == messageId } ?: return
        val proposal = target.proposal ?: return
        if (proposal.status != AiProposalStatus.PENDING) return

        val (ok, detail) = toolExecutor.commitProposal(proposal, strings)
        val updatedProposal = proposal.copy(
            status = if (ok) AiProposalStatus.APPLIED else AiProposalStatus.FAILED,
            resultNote = if (ok) "${strings.aiCommitSuccessPrefix} $detail" else detail
        )
        val updatedMsg = target.copy(proposal = updatedProposal)
        replaceMessage(updatedMsg)
    }

    fun resolveOperationPreview(
        op: AiProposedOperation,
        status: AiProposalStatus,
        strings: AppStrings
    ): AiOperationPreviewDetail = toolExecutor.resolveOperationPreview(op, status, strings)

    fun cancelProposal(messageId: String) {
        val target = _messages.value.firstOrNull { it.id == messageId } ?: return
        val proposal = target.proposal ?: return
        if (proposal.status != AiProposalStatus.PENDING) return

        val updatedMsg = target.copy(
            proposal = proposal.copy(status = AiProposalStatus.CANCELLED)
        )
        replaceMessage(updatedMsg)
    }

    fun startNewChat(temporary: Boolean = false) {
        val prevTempImages = if (_isTemporaryChat.value || _currentThreadId.value == TEMP_THREAD_ID) {
            _messages.value.flatMap { it.effectiveImagePaths }.distinct()
        } else {
            emptyList()
        }
        cancelActiveRequestSilently()
        _isTemporaryChat.value = temporary
        _currentThreadId.value = null
        _messages.value = emptyList()
        _liveStepLogs.value = emptyList()
        databaseRepo?.setSavedActiveAiThreadId(null)
        deleteUnreferencedTempImages(prevTempImages)
    }

    fun setTemporaryChat(enabled: Boolean) {
        startNewChat(temporary = enabled)
    }

    fun toggleTemporaryChat() {
        setTemporaryChat(!_isTemporaryChat.value)
    }

    fun selectThread(threadId: String) {
        val prevTempImages = if (_isTemporaryChat.value || _currentThreadId.value == TEMP_THREAD_ID) {
            _messages.value.flatMap { it.effectiveImagePaths }.distinct()
        } else {
            emptyList()
        }
        cancelActiveRequestSilently()
        _isTemporaryChat.value = false
        _currentThreadId.value = threadId
        _messages.value = databaseRepo?.getAiChatMessages(threadId) ?: emptyList()
        _liveStepLogs.value = emptyList()
        databaseRepo?.setSavedActiveAiThreadId(threadId)
        deleteUnreferencedTempImages(prevTempImages)
    }

    fun deleteThread(threadId: String) {
        if (_currentThreadId.value == threadId) {
            cancelActiveRequestSilently()
            _currentThreadId.value = null
            _messages.value = emptyList()
            _liveStepLogs.value = emptyList()
            databaseRepo?.setSavedActiveAiThreadId(null)
        }
        databaseRepo?.deleteAiChatThread(threadId)
        _threads.value = databaseRepo?.getAllAiChatThreads() ?: emptyList()
    }

    fun clearChat() {
        val currentId = _currentThreadId.value
        val wasTemp = _isTemporaryChat.value
        val tempImages = if (wasTemp || currentId == TEMP_THREAD_ID) {
            _messages.value.flatMap { it.effectiveImagePaths }.distinct()
        } else {
            emptyList()
        }
        cancelActiveRequestSilently()
        if (!wasTemp && currentId != null) {
            databaseRepo?.deleteAiChatThread(currentId)
            _threads.value = databaseRepo?.getAllAiChatThreads() ?: emptyList()
        }
        _currentThreadId.value = null
        _messages.value = emptyList()
        _liveStepLogs.value = emptyList()
        databaseRepo?.setSavedActiveAiThreadId(null)
        deleteUnreferencedTempImages(tempImages)
    }

    private fun deleteUnreferencedTempImages(paths: List<String>) {
        if (paths.isEmpty()) return
        val savedPaths = databaseRepo?.getAllAiChatMessages()
            ?.flatMap { it.effectiveImagePaths }
            ?.toSet()
            .orEmpty()
        paths.forEach { path ->
            if (path !in savedPaths) {
                deleteAiChatImageFile(path)
            }
        }
    }

    private fun ensureActiveThread(initialTitleHint: String): String {
        val existingId = _currentThreadId.value
        if (existingId != null && existingId != TEMP_THREAD_ID) {
            databaseRepo?.setSavedActiveAiThreadId(existingId)
            return existingId
        }
        val newId = IdGenerator.nextId("aichat")
        val now = currentEpochMillis()
        val initialTitle = initialTitleHint.trim().replace('\n', ' ').take(48).ifBlank { "Kuma AI" }
        val newThread = AiChatThread(
            id = newId,
            title = initialTitle,
            contextSummary = "",
            messageCount = 0,
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )
        _currentThreadId.value = newId
        databaseRepo?.saveAiChatThread(newThread)
        databaseRepo?.setSavedActiveAiThreadId(newId)
        _threads.value = databaseRepo?.getAllAiChatThreads() ?: (_threads.value + newThread)
        return newId
    }

    private fun syncThreadMetadata(threadId: String, overrideTitle: String? = null) {
        val repo = databaseRepo ?: return
        val existing = _threads.value.firstOrNull { it.id == threadId }
        val currentMsgs = if (!_isTemporaryChat.value && _currentThreadId.value == threadId) {
            _messages.value.filter { it.threadId == threadId }
        } else {
            repo.getAiChatMessages(threadId)
        }
        val firstUserPrompt = currentMsgs.firstOrNull { it.role == AiChatRole.USER && it.text.isNotBlank() }
            ?.text?.trim()?.replace('\n', ' ')?.take(48)
        val updated = AiChatThread(
            id = threadId,
            title = overrideTitle?.takeIf { it.isNotBlank() }
                ?: existing?.title?.takeIf { it.isNotBlank() }
                ?: (firstUserPrompt ?: "Kuma AI"),
            contextSummary = buildCompactContextSummary(currentMsgs),
            messageCount = currentMsgs.size,
            createdAtEpochMs = existing?.createdAtEpochMs ?: currentEpochMillis(),
            updatedAtEpochMs = currentEpochMillis()
        )
        repo.saveAiChatThread(updated)
        repo.pruneAiChatStorage()
        _threads.value = repo.getAllAiChatThreads()
    }

    private fun isVisibleThread(threadId: String): Boolean {
        val visibleKey = if (_isTemporaryChat.value) TEMP_THREAD_ID else _currentThreadId.value
        return visibleKey == threadId
    }

    private fun appendMessage(message: AiChatMessage, isTemporary: Boolean) {
        if (isVisibleThread(message.threadId)) {
            _messages.update { it + message }
        }
        if (!isTemporary && message.threadId != TEMP_THREAD_ID) {
            databaseRepo?.saveAiChatMessage(message)
            syncThreadMetadata(message.threadId)
        }
    }

    private fun replaceMessage(message: AiChatMessage) {
        if (isVisibleThread(message.threadId)) {
            _messages.update { list -> list.map { if (it.id == message.id) message else it } }
        }
        if (!_isTemporaryChat.value && message.threadId != TEMP_THREAD_ID) {
            databaseRepo?.saveAiChatMessage(message)
            syncThreadMetadata(message.threadId)
        }
    }

    private fun historyMessageText(msg: AiChatMessage): String {
        val clean = msg.text.trim()
        if (clean.isNotBlank()) return clean
        return when {
            msg.role == AiChatRole.USER && msg.effectiveImagePaths.isNotEmpty() -> {
                val count = msg.effectiveImagePaths.size
                if (count > 1) "[User shared $count photos]" else "[User shared a photo]"
            }
            msg.nutritionCard != null ->
                "[Nutrition card: ${msg.nutritionCard.foodName} (${msg.nutritionCard.caloriesKcal} kcal)]"
            msg.proposal != null ->
                msg.proposal.operations.joinToString(", ") { "${it.title} [${msg.proposal.status.name}]" }
            else -> ""
        }
    }

    /**
     * Normalizes conversation history into strictly alternating USER/MODEL pairs starting with USER,
     * merging adjacent same-role entries caused by dropped error bubbles or window slicing.
     */
    private fun buildAlternatingHistory(msgs: List<AiChatMessage>): MutableList<Pair<AiChatRole, String>> {
        val result = mutableListOf<Pair<AiChatRole, String>>()
        msgs.forEach { msg ->
            val text = historyMessageText(msg)
            if (text.isBlank()) return@forEach
            if (result.isEmpty() && msg.role != AiChatRole.USER) return@forEach
            val prev = result.lastOrNull()
            if (prev != null && prev.first == msg.role) {
                result[result.lastIndex] = prev.first to "${prev.second}\n\n$text"
            } else {
                result.add(msg.role to text)
            }
        }
        return result
    }

    private fun buildCompactContextSummary(msgs: List<AiChatMessage>): String {
        val valid = msgs.filter { !it.isError && (it.text.isNotBlank() || it.proposal != null || it.nutritionCard != null) }
        if (valid.isEmpty()) return ""
        val userTopics = valid
            .filter { it.role == AiChatRole.USER && it.text.isNotBlank() }
            .map { it.text.trim().replace('\n', ' ').take(55) }
        val pickedTopics = if (userTopics.size <= 2) {
            userTopics
        } else {
            listOf(userTopics.first(), userTopics.last())
        }.distinct()
        val outcomes = valid.mapNotNull { m ->
            when {
                m.proposal != null -> m.proposal.operations.joinToString(", ") { op ->
                    "${op.title} [${m.proposal.status.name}]"
                }
                m.nutritionCard != null -> "${m.nutritionCard.foodName} (${m.nutritionCard.caloriesKcal} kcal)"
                m.role == AiChatRole.ASSISTANT && m.text.isNotBlank() ->
                    m.text.trim().replace('\n', ' ').take(70)
                else -> null
            }
        }.takeLast(2)
        return buildString {
            if (pickedTopics.isNotEmpty()) {
                append("User: ")
                append(pickedTopics.joinToString("; "))
            }
            if (outcomes.isNotEmpty()) {
                if (isNotEmpty()) append(" | ")
                append("Context: ")
                append(outcomes.joinToString("; "))
            }
        }.take(240)
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
Reply briefly and naturally only to what the user asked. Never expose internal database field names, JSON keys, snake_case IDs, or enum codes.$userNotesBlock
            """.trimIndent()
        }
        val snapshotBlock = "\n\nLive KumaLift DB Snapshot (fresh on this turn):\n$snapshotJson"
        return """
You are Kuma AI, a highly capable, unrestricted intelligent assistant and pure in-app agent inside KumaLift.
Authoritative device clock today: ${today.dayNameLocalized} (${today.dayNameEn}), ${today.isoDate} ${today.hhMm} (ISO dayOfWeek=${today.isoDayNumber} where 1=Monday..7=Sunday). Yesterday was ${today.yesterdayDate}.
$langHint$userNotesBlock$snapshotBlock

Core Capabilities & Behavior:
1. **Unrestricted Helpful Intelligence**: Answer anything the user asks intelligently, clearly, and directly — fitness programming, biomechanics, nutrition, sports science, general knowledge, math, productivity, troubleshooting, comparisons, schedules, and creative tasks. Never artificially refuse benign questions or claim you lack access to tools you actually have.
2. **Never invent dates/weekdays**: Always use the authoritative device clock above (Today: `${today.dayNameLocalized}, ${today.isoDate}`, Yesterday: `${today.yesterdayDate}`).
3. **Workout & Progress Data Routing**:
   - **Checking whether the user worked out today/yesterday ("kemarin udah latihan belum?", "tadi latihan apa?")**: ALWAYS inspect `yesterdayWorkedOut`, `yesterdayWorkouts`, `todayWorkedOut`, `todayCompletedWorkouts`, and `recentWorkoutsTop5` (each has exact `date`, `dayOfWeek`, `time`, and `relativeDay`), and call `get_recent_workouts` for deeper history.
   - **Local DB first**: Use `Live KumaLift DB Snapshot` immediately when sufficient; call `get_routines`, `get_recent_workouts`, `get_personal_records`, `get_muscle_recovery_and_volume`, or `search_exercises` whenever detailed configs or catalog IDs are needed.
4. **Live Internet Search & Reading (`search_web_live`, `fetch_web_page`)**:
   - Whenever live web info, science articles, macro/food lookups, products, or web URLs are needed, proactively call `search_web_live(query)` and/or `fetch_web_page(url)`.
   - Present real verified clickable links using Markdown `[Title](https://...)`. Never fabricate URLs or image URLs.
5. **Rich Structured Formatting (Markdown Tables, Images, Visuals, Cards)**:
   - Use Markdown tables (`| Column | Column |` with `| --- | --- |` divider) whenever presenting multi-day splits, workout programs, comparison matrices, macro/nutrition breakdowns, or structured schedules so they render as clean tables in the chat UI.
   - For exercise demos: call `search_exercises` then `show_exercise_visuals`. For progress photos: call `get_progress_photos(attachImages=true)`. For food macros: call `present_nutrition_analysis`. For image creation/diagrams/illustrations: call `generate_image(prompt)`. You may also embed verified `https://` image URLs returned by tools as `![caption](https://...)`.
6. **Pure In-App Agent Actions (Full Control via Proposal Preview)**:
   - You can create/switch programs (`stage_create_program`), create custom exercises (`stage_create_exercise`), save exercise form notes (`stage_save_exercise_note`), create/update/delete routines (`stage_create_routine`, `stage_update_routine`, `stage_delete_routine`), schedule/cancel/delete workouts (`stage_schedule_workout`, `stage_cancel_or_delete_schedule`), start mesocycles (`stage_start_mesocycle`), log readiness check-ins (`stage_log_readiness`), log bodyweight (`stage_log_bodyweight`), change goals (`stage_update_training_goal`), add exercises to an active workout (`stage_add_exercise_to_active_workout`), and save AI preferences (`stage_update_ai_preferences`).
   - Always call `stage_update_routine` with the existing `routineId` when modifying an existing routine (avoid duplicates), and call `search_exercises` before referencing exercise IDs. You can batch multiple staged operations in one turn (e.g., `stage_create_program` + `stage_create_routine` + `stage_schedule_workout`).
7. **Screenshots & Photos**: Read every visible exercise, set count, rep target, `kg` load, and rest timer accurately; map to library exercises (or stage custom ones via `stage_create_exercise` if missing).
8. **Cross-Chat Memory**: Use `savedChatHistories` / `currentThreadEarlierContext` and call `search_chat_history` when referencing prior chats.
9. **User-Facing Polish**: Keep replies clear, accurate, and natural. Never leak raw JSON field names (`yesterdayWorkedOut`, `routineId`, `exerciseId`, etc.), internal IDs, or UPPER_CASE enum codes to the user. When actions are staged, tell the user to review and tap Apply on the preview card.
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
        private const val TEMP_THREAD_ID = "__temporary__"
        private const val MAX_ATTACHED_IMAGES = 10
        private const val SLIDING_WINDOW_COUNT = 14
        private const val MAX_TOOL_ROUNDS = 16
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

internal fun extractReadableWebContent(rawBody: String, maxChars: Int = 5500): Pair<String, String> {
    val trimmed = rawBody.trim()
    if (trimmed.isEmpty()) return "" to ""
    val titleMatch = Regex("""<title[^>]*>(.*?)</title>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        .find(trimmed)
    val rawTitle = titleMatch?.groupValues?.getOrNull(1).orEmpty()
    val noHidden = trimmed
        .replace(Regex("""<script\b[^>]*>.*?</script>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
        .replace(Regex("""<style\b[^>]*>.*?</style>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
        .replace(Regex("""<noscript\b[^>]*>.*?</noscript>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
    val withBreaks = noHidden
        .replace(Regex("""<(?:br\s*/?|/(?:p|div|li|tr|h[1-6]|section|article))>""", RegexOption.IGNORE_CASE), "\n")
    val stripped = withBreaks.replace(Regex("""<[^>]+>"""), " ")
    fun decodeEntities(s: String): String = s
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
    val cleanTitle = decodeEntities(rawTitle).replace(Regex("""\s+"""), " ").trim().take(120)
    val cleanBody = decodeEntities(stripped)
        .replace(Regex("""[ \t]+"""), " ")
        .replace(Regex("""\n[ \t]+"""), "\n")
        .replace(Regex("""\n{3,}"""), "\n\n")
        .trim()
        .take(maxChars)
    return cleanTitle to cleanBody
}
