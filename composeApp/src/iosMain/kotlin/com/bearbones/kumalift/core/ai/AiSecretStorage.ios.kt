package com.bearbones.kumalift.core.ai

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

@OptIn(ExperimentalForeignApi::class)
class IosAiSecretStore : AiSecretStore {
    private val _hasApiKeyFlow by lazy {
        MutableStateFlow(getGeminiApiKey().isNotBlank())
    }
    override val hasApiKeyFlow: StateFlow<Boolean> get() = _hasApiKeyFlow.asStateFlow()

    override fun getGeminiApiKey(): String = memScoped {
        val query = CFDictionaryCreateMutable(null, 4, null, null)
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, CFBridgingRetain(SERVICE_NAME))
        CFDictionaryAddValue(query, kSecAttrAccount, CFBridgingRetain(ACCOUNT_NAME))
        CFDictionaryAddValue(query, kSecReturnData, kCFBooleanTrue)
        CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitOne)

        val result = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(query, result.ptr)
        if (status != errSecSuccess) return ""
        val data = CFBridgingRelease(result.value) as? NSData ?: return ""
        return NSString.create(data = data, encoding = NSUTF8StringEncoding)?.toString()?.trim().orEmpty()
    }

    override fun saveGeminiApiKey(apiKey: String) {
        clearGeminiApiKey()
        val trimmed = apiKey.trim()
        if (trimmed.isBlank()) return
        val nsString = NSString.create(string = trimmed)
        val data = nsString.dataUsingEncoding(NSUTF8StringEncoding) ?: return
        val query = CFDictionaryCreateMutable(null, 4, null, null)
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, CFBridgingRetain(SERVICE_NAME))
        CFDictionaryAddValue(query, kSecAttrAccount, CFBridgingRetain(ACCOUNT_NAME))
        CFDictionaryAddValue(query, kSecValueData, CFBridgingRetain(data))
        SecItemAdd(query, null)
        _hasApiKeyFlow.value = true
    }

    override fun clearGeminiApiKey() {
        val query = CFDictionaryCreateMutable(null, 3, null, null)
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, CFBridgingRetain(SERVICE_NAME))
        CFDictionaryAddValue(query, kSecAttrAccount, CFBridgingRetain(ACCOUNT_NAME))
        SecItemDelete(query)
        _hasApiKeyFlow.value = false
    }

    companion object {
        private const val SERVICE_NAME = "com.bearbones.kumalift.ai"
        private const val ACCOUNT_NAME = "gemini_api_key"
    }
}

@Composable
actual fun rememberAiSecretStore(): AiSecretStore {
    return remember { IosAiSecretStore() }
}
