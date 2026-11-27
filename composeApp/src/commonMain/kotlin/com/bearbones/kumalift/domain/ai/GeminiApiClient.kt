package com.bearbones.kumalift.domain.ai

import com.bearbones.kumalift.core.ai.AiHttpTransport
import com.bearbones.kumalift.core.ai.PlatformAiHttpTransport
import com.bearbones.kumalift.core.localization.AppStrings
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

enum class GeminiErrorType {
    INVALID_KEY,
    QUOTA_EXCEEDED,
    MODEL_UNAVAILABLE,
    LOCATION_UNSUPPORTED,
    BAD_REQUEST,
    OFFLINE,
    TIMEOUT,
    SERVER_BUSY,
    NETWORK,
    EMPTY_RESPONSE
}

class GeminiApiException(
    val errorType: GeminiErrorType,
    override val message: String,
    val httpStatusCode: Int? = null
) : Exception(message)

fun GeminiApiException?.localizedSummary(s: AppStrings): String {
    val base = when (this?.errorType) {
        GeminiErrorType.INVALID_KEY -> s.aiErrorInvalidKey
        GeminiErrorType.QUOTA_EXCEEDED -> s.aiErrorQuotaExceeded
        GeminiErrorType.MODEL_UNAVAILABLE -> s.aiErrorModelUnavailable
        GeminiErrorType.LOCATION_UNSUPPORTED -> s.aiErrorLocationUnsupported
        GeminiErrorType.BAD_REQUEST -> s.aiErrorBadRequest
        GeminiErrorType.OFFLINE -> s.aiErrorOffline
        GeminiErrorType.TIMEOUT -> s.aiErrorTimeout
        GeminiErrorType.SERVER_BUSY -> s.aiErrorServerBusy
        GeminiErrorType.EMPTY_RESPONSE -> s.aiErrorEmptyResponse
        GeminiErrorType.NETWORK, null -> s.aiErrorNetwork
    }
    val code = this?.httpStatusCode
    val summary = if (code != null && code > 0) {
        "$base (${s.aiHttpStatusCodeLabel(code)})"
    } else {
        base
    }
    val rawDetail = this?.message?.trim().orEmpty()
    val showDetail = this?.errorType == GeminiErrorType.QUOTA_EXCEEDED &&
        rawDetail.isNotBlank() &&
        !rawDetail.startsWith("HTTP ", ignoreCase = true)
    return if (showDetail) {
        val compact = rawDetail.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty().take(140)
        "$summary\n$compact"
    } else {
        summary
    }
}

data class GeminiFunctionCall(
    val name: String,
    val args: JsonObject
)

data class GeminiTurnResult(
    val text: String,
    val functionCalls: List<GeminiFunctionCall>,
    val rawModelParts: JsonArray,
    val webSources: List<String> = emptyList()
)

data class GeminiVerificationResult(
    val activeModelId: String,
    val availableModels: List<String>,
    val modelAutoSwitched: Boolean
)

enum class AiReasoningDepth {
    FAST_NONE,
    BALANCED,
    DEEP
}

data class GeminiModelCatalogGroups(
    val recommended: List<String>,
    val freeTier: List<String>,
    val paidOnly: List<String>
)

class GeminiApiClient(
    private val transport: AiHttpTransport = PlatformAiHttpTransport
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    // Keys where Search Grounding is unsupported/zero-quota (e.g. Free Tier); cleared on Test Connection
    private val searchBlockMutex = Mutex()
    private val searchBlockedKeys = mutableSetOf<String>()

    suspend fun generateContent(
        apiKey: String,
        modelId: String,
        systemInstruction: String,
        contents: JsonArray,
        functionDeclarations: JsonArray,
        enableWebSearch: Boolean = false,
        enableUrlContext: Boolean = false,
        reasoningDepth: AiReasoningDepth = AiReasoningDepth.BALANCED
    ): GeminiTurnResult {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            throw GeminiApiException(GeminiErrorType.INVALID_KEY, "Missing Gemini API key")
        }
        val cleanModel = normalizeModelId(modelId)

        val isThinkingGeneration = cleanModel.startsWith("gemini-3", ignoreCase = true) ||
            cleanModel.startsWith("gemini-4", ignoreCase = true)

        fun buildBody(
            includeExtras: Boolean,
            includeWebSearch: Boolean,
            includeUrlContext: Boolean
        ): String = buildJsonObject {
            put("systemInstruction", buildJsonObject {
                put("parts", buildJsonArray {
                    add(buildJsonObject { put("text", systemInstruction) })
                })
            })
            put("contents", contents)
            if (functionDeclarations.isNotEmpty() || includeWebSearch || includeUrlContext) {
                put("tools", buildJsonArray {
                    if (includeUrlContext) {
                        add(buildJsonObject {
                            put("urlContext", buildJsonObject {})
                        })
                    }
                    if (includeWebSearch) {
                        add(buildJsonObject {
                            put("googleSearch", buildJsonObject {})
                        })
                    }
                    if (functionDeclarations.isNotEmpty()) {
                        add(buildJsonObject {
                            put("functionDeclarations", functionDeclarations)
                        })
                    }
                })
            }
            put("generationConfig", buildJsonObject {
                if (!isThinkingGeneration) {
                    put("temperature", 0.35)
                }
                if (includeExtras) {
                    applyThinkingConfig(cleanModel, reasoningDepth)
                }
            })
        }.toString()

        if (!transport.isNetworkOnline()) {
            throw GeminiApiException(GeminiErrorType.OFFLINE, "Device is offline")
        }

        suspend fun send(bodyText: String) = try {
            transport.postJson(
                url = "$BASE_URL/$cleanModel:generateContent",
                headers = mapOf("x-goog-api-key" to cleanKey),
                body = bodyText,
                timeoutMs = 60_000
            )
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            throw mapTransportFailure(e)
        }

        var useExtras = true
        val isSearchBlocked = searchBlockMutex.withLock { searchBlockedKeys.contains(cleanKey) }
        var useSearch = enableWebSearch && !isSearchBlocked
        var useUrlContext = enableUrlContext && !isSearchBlocked
        var response = send(buildBody(useExtras, useSearch, useUrlContext))

        if (response.statusCode in TRANSIENT_SERVER_CODES) {
            delay(450)
            response = send(buildBody(useExtras, useSearch, useUrlContext))
        }

        if (response.statusCode == 400 && (
                response.body.contains("thinking", ignoreCase = true) ||
                    response.body.contains("temperature", ignoreCase = true) ||
                    response.body.contains("generationConfig", ignoreCase = true)
                )
        ) {
            useExtras = false
            response = send(buildBody(useExtras, useSearch, useUrlContext))
        }

        fun matchesWebToolRejection(statusCode: Int, body: String): Boolean =
            statusCode == 429 || (
                (statusCode == 400 || statusCode == 403) && (
                    body.contains("search", ignoreCase = true) ||
                        body.contains("grounding", ignoreCase = true) ||
                        body.contains("url", ignoreCase = true) ||
                        body.contains("tool", ignoreCase = true) ||
                        body.contains("multiple", ignoreCase = true)
                    )
                )

        val isWebToolRejected = (useSearch || useUrlContext) && matchesWebToolRejection(response.statusCode, response.body)

        if (isWebToolRejected) {
            val originalStatus = response.statusCode
            if (useUrlContext && originalStatus != 429 && response.body.contains("url", ignoreCase = true)) {
                useUrlContext = false
                response = send(buildBody(useExtras, useSearch, false))
                if (useSearch && matchesWebToolRejection(response.statusCode, response.body)) {
                    val searchStatus = response.statusCode
                    useSearch = false
                    response = send(buildBody(useExtras, false, false))
                    if ((searchStatus == 429 || searchStatus == 403) && response.statusCode in 200..299) {
                        searchBlockMutex.withLock { searchBlockedKeys.add(cleanKey) }
                    }
                }
            } else {
                useSearch = false
                useUrlContext = false
                response = send(buildBody(useExtras, false, false))
                if ((originalStatus == 429 || originalStatus == 403) && response.statusCode in 200..299) {
                    searchBlockMutex.withLock { searchBlockedKeys.add(cleanKey) }
                }
            }
        }

        if (response.statusCode !in 200..299) {
            throw mapHttpError(response.statusCode, response.body)
        }

        val root = try {
            json.parseToJsonElement(response.body) as? JsonObject
                ?: throw GeminiApiException(GeminiErrorType.EMPTY_RESPONSE, "Unexpected root JSON type")
        } catch (e: GeminiApiException) {
            throw e
        } catch (e: Exception) {
            throw GeminiApiException(GeminiErrorType.EMPTY_RESPONSE, e.message ?: "Invalid JSON")
        }

        val firstCandidate = (root["candidates"] as? JsonArray)?.firstOrNull() as? JsonObject
            ?: throw GeminiApiException(GeminiErrorType.EMPTY_RESPONSE, "No candidates in response")
        val contentObj = firstCandidate["content"] as? JsonObject
            ?: throw GeminiApiException(GeminiErrorType.EMPTY_RESPONSE, "No content in candidate")
        val rawParts = (contentObj["parts"] as? JsonArray) ?: JsonArray(emptyList())

        val textParts = mutableListOf<String>()
        val calls = mutableListOf<GeminiFunctionCall>()

        rawParts.forEach { element ->
            val partObj = element as? JsonObject ?: return@forEach
            val text = (partObj["text"] as? JsonPrimitive)?.contentOrNull
            if (!text.isNullOrBlank()) {
                textParts.add(text)
            }
            val fnCall = partObj["functionCall"] as? JsonObject
            if (fnCall != null) {
                val fnName = (fnCall["name"] as? JsonPrimitive)?.contentOrNull.orEmpty()
                val fnArgs = (fnCall["args"] as? JsonObject) ?: JsonObject(emptyMap())
                if (fnName.isNotBlank()) {
                    calls.add(GeminiFunctionCall(name = fnName, args = fnArgs))
                }
            }
        }

        val combinedText = textParts.joinToString("\n").trim()
        if (combinedText.isEmpty() && calls.isEmpty()) {
            throw GeminiApiException(GeminiErrorType.EMPTY_RESPONSE, "Empty model parts")
        }

        val webSources = (firstCandidate["groundingMetadata"] as? JsonObject)
            ?.get("groundingChunks")?.let { it as? JsonArray }
            ?.mapNotNull { chunk ->
                val webObj = (chunk as? JsonObject)?.get("web") as? JsonObject ?: return@mapNotNull null
                val title = (webObj["title"] as? JsonPrimitive)?.contentOrNull?.trim()
                val uri = (webObj["uri"] as? JsonPrimitive)?.contentOrNull?.trim()
                title?.takeIf { it.isNotBlank() } ?: uri?.takeIf { it.isNotBlank() }
            }
            ?.distinct()
            ?.take(4)
            .orEmpty()

        return GeminiTurnResult(
            text = combinedText,
            functionCalls = calls,
            rawModelParts = rawParts,
            webSources = webSources
        )
    }

    suspend fun listAvailableModels(apiKey: String): Result<List<String>> {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return Result.failure(GeminiApiException(GeminiErrorType.INVALID_KEY, "Empty API key"))
        }
        if (!transport.isNetworkOnline()) {
            return Result.failure(GeminiApiException(GeminiErrorType.OFFLINE, "Device is offline"))
        }

        val response = try {
            transport.get(
                url = "$BASE_URL?pageSize=200",
                headers = mapOf("x-goog-api-key" to cleanKey),
                timeoutMs = 20_000
            )
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            return Result.failure(mapTransportFailure(e))
        }

        if (response.statusCode !in 200..299) {
            return Result.failure(mapHttpError(response.statusCode, response.body))
        }

        val root = try {
            json.parseToJsonElement(response.body) as? JsonObject
                ?: throw IllegalArgumentException("Unexpected root JSON type")
        } catch (_: Exception) {
            return Result.failure(
                GeminiApiException(
                    GeminiErrorType.NETWORK,
                    "Unexpected non-JSON response",
                    response.statusCode
                )
            )
        }

        val rawModels = (root["models"] as? JsonArray).orEmpty()
        val chatCapable = rawModels.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val rawName = (obj["name"] as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()
            val id = rawName.removePrefix("models/")
            if (!id.startsWith("gemini", ignoreCase = true)) return@mapNotNull null
            val methods = (obj["supportedGenerationMethods"] as? JsonArray)
                ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
                .orEmpty()
            if (!methods.contains("generateContent")) return@mapNotNull null
            id
        }.distinct()

        val filtered = chatCapable.filter { isGeneralChatModel(it) }
        val usable = (filtered.ifEmpty {
            chatCapable.filter { !it.contains("embedding", ignoreCase = true) }
        }).sortedBy { modelPreferenceRank(it) }

        return Result.success(usable)
    }

    suspend fun verifyConnection(apiKey: String, modelId: String): Result<GeminiVerificationResult> {
        return try {
            val cleanKey = apiKey.trim()
            if (cleanKey.isBlank()) {
                return Result.failure(GeminiApiException(GeminiErrorType.INVALID_KEY, "Empty API key"))
            }
            searchBlockMutex.withLock { searchBlockedKeys.remove(cleanKey) }
            val cleanModel = normalizeModelId(modelId)

            val listResult = listAvailableModels(cleanKey)
            if (listResult.isSuccess) {
                val liveModels = listResult.getOrNull().orEmpty()
                if (liveModels.isNotEmpty()) {
                    return if (liveModels.contains(cleanModel)) {
                        Result.success(
                            GeminiVerificationResult(
                                activeModelId = cleanModel,
                                availableModels = liveModels,
                                modelAutoSwitched = false
                            )
                        )
                    } else {
                        val fallback = pickPreferredChatModel(liveModels) ?: cleanModel
                        Result.success(
                            GeminiVerificationResult(
                                activeModelId = fallback,
                                availableModels = liveModels,
                                modelAutoSwitched = fallback != cleanModel
                            )
                        )
                    }
                }
            } else {
                val listErr = listResult.exceptionOrNull() as? GeminiApiException
                if (listErr != null && listErr.errorType != GeminiErrorType.MODEL_UNAVAILABLE) {
                    return Result.failure(listErr)
                }
            }

            // Fallback check when a key/gateway restricts models.list or returns an empty array
            if (!transport.isNetworkOnline()) {
                return Result.failure(GeminiApiException(GeminiErrorType.OFFLINE, "Device is offline"))
            }
            val response = try {
                transport.get(
                    url = "$BASE_URL/$cleanModel",
                    headers = mapOf("x-goog-api-key" to cleanKey),
                    timeoutMs = 20_000
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                return Result.failure(mapTransportFailure(e))
            }

            if (response.statusCode in 200..299) {
                val modelObj = try {
                    json.parseToJsonElement(response.body).jsonObject
                } catch (_: Exception) {
                    return Result.failure(
                        GeminiApiException(
                            GeminiErrorType.NETWORK,
                            "Unexpected non-JSON response",
                            response.statusCode
                        )
                    )
                }
                if (modelObj["name"]?.jsonPrimitive?.contentOrNull.isNullOrBlank()) {
                    return Result.failure(
                        GeminiApiException(
                            GeminiErrorType.EMPTY_RESPONSE,
                            "Missing model metadata",
                            response.statusCode
                        )
                    )
                }
                Result.success(
                    GeminiVerificationResult(
                        activeModelId = cleanModel,
                        availableModels = listOf(cleanModel),
                        modelAutoSwitched = false
                    )
                )
            } else {
                Result.failure(mapHttpError(response.statusCode, response.body))
            }
        } catch (e: GeminiApiException) {
            Result.failure(e)
        }
    }

    private fun mapTransportFailure(e: Exception): GeminiApiException {
        if (!transport.isNetworkOnline()) {
            return GeminiApiException(GeminiErrorType.OFFLINE, "Device is offline")
        }
        val msg = e.message.orEmpty()
        val typeName = e::class.simpleName.orEmpty()
        return if (msg.contains("timeout", ignoreCase = true) ||
            msg.contains("timed out", ignoreCase = true) ||
            typeName.contains("Timeout", ignoreCase = true)
        ) {
            GeminiApiException(GeminiErrorType.TIMEOUT, msg.ifBlank { "Request timeout" })
        } else {
            GeminiApiException(GeminiErrorType.NETWORK, msg.ifBlank { "Network failure" })
        }
    }

    private fun mapHttpError(statusCode: Int, body: String): GeminiApiException {
        val errorObj = try {
            json.parseToJsonElement(body).jsonObject["error"]?.jsonObject
        } catch (_: Exception) {
            null
        }
        val message = errorObj?.get("message")?.jsonPrimitive?.contentOrNull ?: "HTTP $statusCode"
        val statusText = errorObj?.get("status")?.jsonPrimitive?.contentOrNull.orEmpty()

        val errorType = when {
            statusCode == 401 || statusCode == 403 ||
                statusText.equals("UNAUTHENTICATED", ignoreCase = true) ||
                statusText.equals("PERMISSION_DENIED", ignoreCase = true) ||
                message.contains("API key", ignoreCase = true) ||
                message.contains("API_KEY_INVALID", ignoreCase = true) -> GeminiErrorType.INVALID_KEY

            statusCode == 429 ||
                statusText.equals("RESOURCE_EXHAUSTED", ignoreCase = true) ||
                message.contains("quota", ignoreCase = true) ||
                message.contains("rate limit", ignoreCase = true) -> GeminiErrorType.QUOTA_EXCEEDED

            message.contains("location", ignoreCase = true) ||
                message.contains("region", ignoreCase = true) ||
                message.contains("country", ignoreCase = true) -> GeminiErrorType.LOCATION_UNSUPPORTED

            statusCode == 404 ||
                statusText.equals("NOT_FOUND", ignoreCase = true) ||
                (message.contains("model", ignoreCase = true) && message.contains("not found", ignoreCase = true)) -> GeminiErrorType.MODEL_UNAVAILABLE

            statusCode in 400..499 -> GeminiErrorType.BAD_REQUEST

            statusCode in 500..599 -> GeminiErrorType.SERVER_BUSY

            else -> GeminiErrorType.NETWORK
        }
        return GeminiApiException(errorType = errorType, message = message, httpStatusCode = statusCode)
    }

    companion object {
        const val DEFAULT_MODEL = "gemini-2.5-flash"
        private val TRANSIENT_SERVER_CODES = setOf(500, 502, 503, 504)
        val AVAILABLE_MODELS = listOf(
            "gemini-2.5-flash",
            "gemini-2.5-flash-lite",
            "gemini-3.5-flash-lite",
            "gemini-3.8-flash",
            "gemini-4-hydrogen",
            "gemini-4-neon",
            "gemini-4-carbon"
        )
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

        fun normalizeModelId(rawModelId: String): String =
            rawModelId.trim().removePrefix("models/").trim().ifBlank { DEFAULT_MODEL }

        private val EXCLUDED_MODEL_MARKERS = listOf(
            "embedding",
            "aqa",
            "imagen",
            "veo",
            "-tts",
            "native-audio",
            "-live",
            "robotics",
            "computer-use",
            "deep-research",
            "-image"
        )

        private val PAID_TIER_TOKEN_REGEX = Regex("""(^|[-_])(pro|carbon|argon|neon|ultra)([-_]|$)""")
        private val SNAPSHOT_CLONE_SUFFIX_REGEX = Regex("""-(00\d|latest)$""")

        fun isPaidOnlyModel(modelId: String): Boolean {
            return PAID_TIER_TOKEN_REGEX.containsMatchIn(modelId.lowercase())
        }

        fun categorizeModels(
            models: List<String>,
            activeModelId: String? = null
        ): GeminiModelCatalogGroups {
            val cleanActive = activeModelId?.trim()?.removePrefix("models/")?.takeIf { it.isNotBlank() }
            val source = (models.ifEmpty { AVAILABLE_MODELS } + listOfNotNull(cleanActive))
                .map { it.trim().removePrefix("models/") }
                .filter { it.isNotBlank() }
                .distinct()

            val baseSet = source.toSet()
            val cleaned = source.filter { id ->
                if (id == cleanActive) {
                    true
                } else {
                    val withoutSuffix = id.replace(SNAPSHOT_CLONE_SUFFIX_REGEX, "")
                    withoutSuffix == id || !baseSet.contains(withoutSuffix)
                }
            }

            val freeTier = cleaned
                .filter { !isPaidOnlyModel(it) }
                .sortedBy { modelPreferenceRank(it) }
            val paidOnly = cleaned
                .filter { isPaidOnlyModel(it) }
                .sortedBy { modelPreferenceRank(it) }

            val stableFree = freeTier.filter {
                !it.contains("-exp", ignoreCase = true) &&
                    !it.contains("experimental", ignoreCase = true) &&
                    !it.contains("-preview", ignoreCase = true)
            }
            val recommended = (stableFree + freeTier + paidOnly)
                .distinct()
                .take(3)

            return GeminiModelCatalogGroups(
                recommended = recommended,
                freeTier = freeTier,
                paidOnly = paidOnly
            )
        }

        fun pickPreferredChatModel(
            availableModels: List<String>,
            excludeModel: String? = null
        ): String? {
            return availableModels
                .map { it.trim() }
                .filter { it.isNotBlank() && it != excludeModel }
                .distinct()
                .minByOrNull { modelPreferenceRank(it) }
        }

        private fun kotlinx.serialization.json.JsonObjectBuilder.applyThinkingConfig(
            modelId: String,
            reasoningDepth: AiReasoningDepth
        ) {
            val lower = modelId.lowercase()
            when {
                lower.contains("3.5-flash-lite") -> {
                    val level = when (reasoningDepth) {
                        AiReasoningDepth.FAST_NONE -> "minimal"
                        AiReasoningDepth.BALANCED -> "low"
                        AiReasoningDepth.DEEP -> "medium"
                    }
                    put("thinkingConfig", buildJsonObject {
                        put("thinkingLevel", level)
                    })
                }
                lower.startsWith("gemini-3") || lower.startsWith("gemini-4") -> {
                    val level = when (reasoningDepth) {
                        AiReasoningDepth.FAST_NONE,
                        AiReasoningDepth.BALANCED -> "low"
                        AiReasoningDepth.DEEP -> "medium"
                    }
                    put("thinkingConfig", buildJsonObject {
                        put("thinkingLevel", level)
                    })
                }
                lower.startsWith("gemini-2.5") && !lower.contains("pro") -> {
                    val budget = when (reasoningDepth) {
                        AiReasoningDepth.FAST_NONE -> 0
                        AiReasoningDepth.BALANCED -> if (lower.contains("flash-lite")) 512 else 1024
                        AiReasoningDepth.DEEP -> if (lower.contains("flash-lite")) 1024 else 2048
                    }
                    put("thinkingConfig", buildJsonObject {
                        put("thinkingBudget", budget)
                    })
                }
            }
        }

        private fun isGeneralChatModel(modelId: String): Boolean {
            val lower = modelId.lowercase()
            return EXCLUDED_MODEL_MARKERS.none { lower.contains(it) }
        }

        private fun modelPreferenceRank(id: String): Int {
            val lower = id.lowercase()
            val familyRank = when {
                lower == DEFAULT_MODEL -> 0
                lower == "gemini-2.5-flash-lite" -> 1
                lower.contains("2.5") && lower.contains("flash") -> 2
                lower == "gemini-3.5-flash-lite" -> 3
                lower == "gemini-3.8-flash" -> 4
                lower == "gemini-4-hydrogen" -> 5
                lower.contains("3.8") && lower.contains("flash") -> 6
                lower.contains("3.5") && lower.contains("flash") -> 7
                lower.contains("flash-lite") -> 8
                lower.contains("flash") -> 9
                lower.contains("hydrogen") -> 10
                lower.contains("pro") || lower.contains("neon") || lower.contains("carbon") -> 11
                else -> 15
            }
            val stabilityPenalty = (if (lower.contains("-exp") || lower.contains("experimental")) 20 else 0) +
                (if (lower.contains("-preview")) 10 else 0)
            return familyRank * 100 + stabilityPenalty + lower.length
        }
    }
}

internal fun buildFunctionResponsePart(name: String, resultPayload: JsonElement): JsonObject {
    return buildJsonObject {
        put("functionResponse", buildJsonObject {
            put("name", name)
            put("response", buildJsonObject {
                put("result", resultPayload)
            })
        })
    }
}

internal fun buildTextPart(text: String): JsonObject = buildJsonObject {
    put("text", text)
}

internal fun buildInlineImagePart(mimeType: String, base64Data: String): JsonObject = buildJsonObject {
    put("inlineData", buildJsonObject {
        put("mimeType", JsonPrimitive(mimeType))
        put("data", JsonPrimitive(base64Data))
    })
}
