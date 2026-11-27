package com.bearbones.kumalift.core.ai

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface AiSecretStore {
    val hasApiKeyFlow: StateFlow<Boolean>
    fun getGeminiApiKey(): String
    fun saveGeminiApiKey(apiKey: String)
    fun clearGeminiApiKey()
    fun hasGeminiApiKey(): Boolean = getGeminiApiKey().isNotBlank()
}

class InMemoryAiSecretStore(initialKey: String = "") : AiSecretStore {
    private var key: String = initialKey.trim()
    private val _hasApiKeyFlow = MutableStateFlow(key.isNotBlank())
    override val hasApiKeyFlow: StateFlow<Boolean> = _hasApiKeyFlow.asStateFlow()

    override fun getGeminiApiKey(): String = key

    override fun saveGeminiApiKey(apiKey: String) {
        key = apiKey.trim()
        _hasApiKeyFlow.value = key.isNotBlank()
    }

    override fun clearGeminiApiKey() {
        key = ""
        _hasApiKeyFlow.value = false
    }
}

@Composable
expect fun rememberAiSecretStore(): AiSecretStore
