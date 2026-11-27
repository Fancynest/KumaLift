package com.bearbones.kumalift.core.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AndroidAiSecretStore(private val context: Context) : AiSecretStore {
    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    private val _hasApiKeyFlow by lazy {
        MutableStateFlow(getGeminiApiKey().isNotBlank())
    }
    override val hasApiKeyFlow: StateFlow<Boolean> get() = _hasApiKeyFlow.asStateFlow()

    override fun getGeminiApiKey(): String {
        val encoded = prefs.getString(KEY_GEMINI_API_KEY, null) ?: return ""
        return try {
            val raw = Base64.decode(encoded, Base64.NO_WRAP)
            if (raw.size <= IV_LENGTH_BYTES + 16) return ""
            val iv = raw.copyOfRange(0, IV_LENGTH_BYTES)
            val cipherText = raw.copyOfRange(IV_LENGTH_BYTES, raw.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(cipherText), Charsets.UTF_8).trim()
        } catch (_: Exception) {
            ""
        }
    }

    override fun saveGeminiApiKey(apiKey: String) {
        val trimmed = apiKey.trim()
        if (trimmed.isBlank()) {
            clearGeminiApiKey()
            return
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val key = try {
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
            getOrCreateSecretKey()
        } catch (_: Exception) {
            // Regenerate corrupted or permanently invalidated AndroidKeyStore key on new write
            synchronized(keyStoreLock) {
                runCatching {
                    KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(KEYSTORE_ALIAS)
                }
            }
            getOrCreateSecretKey().also { cipher.init(Cipher.ENCRYPT_MODE, it) }
        }
        val iv = requireNotNull(cipher.iv) { "Cipher IV missing" }
        val encrypted = cipher.doFinal(trimmed.toByteArray(Charsets.UTF_8))
        val encoded = Base64.encodeToString(iv + encrypted, Base64.NO_WRAP)
        prefs.edit().putString(KEY_GEMINI_API_KEY, encoded).apply()
        _hasApiKeyFlow.value = true
    }

    override fun clearGeminiApiKey() {
        prefs.edit().remove(KEY_GEMINI_API_KEY).apply()
        _hasApiKeyFlow.value = false
    }

    private fun getOrCreateSecretKey(): SecretKey = synchronized(keyStoreLock) {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
            val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val spec = KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGen.init(spec)
            keyGen.generateKey()
        }
        return (keyStore.getEntry(KEYSTORE_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    companion object {
        private val keyStoreLock = Any()
        private const val PREFS_NAME = "kumalift_ai_secrets"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key_enc"
        private const val KEYSTORE_ALIAS = "kumalift_gemini_byok_key"
        private const val IV_LENGTH_BYTES = 12
        private const val GCM_TAG_BITS = 128
    }
}

@Composable
actual fun rememberAiSecretStore(): AiSecretStore {
    val context = LocalContext.current.applicationContext
    return remember(context) { AndroidAiSecretStore(context) }
}
