package com.bearbones.kumalift.core.ai

data class AiHttpResponse(
    val statusCode: Int,
    val body: String
)

interface AiHttpTransport {
    fun isNetworkOnline(): Boolean = true

    suspend fun get(
        url: String,
        headers: Map<String, String> = emptyMap(),
        timeoutMs: Int = 20_000
    ): AiHttpResponse

    suspend fun postJson(
        url: String,
        headers: Map<String, String>,
        body: String,
        timeoutMs: Int = 90_000
    ): AiHttpResponse
}

expect object PlatformAiHttpTransport : AiHttpTransport
