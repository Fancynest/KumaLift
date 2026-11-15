package com.bearbones.kumalift.core.media

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Repository for cached exercise animation sprite sheet metadata parsed from `anim/manifest.json`.
 */
object SpriteManifestRepository {
    private var manifest: Map<String, SpriteFrameMeta>? = null
    private val mutex = Mutex()
    private val jsonParser = Json { ignoreUnknownKeys = true }

    /**
     * Resolves metadata for the given exercise asset stem.
     * Returns null if manifest.json does not exist or does not contain the stem.
     */
    suspend fun getMeta(context: Context, stem: String): SpriteFrameMeta? {
        val current = manifest
        if (current != null) return current[stem]

        return mutex.withLock {
            manifest?.let { return it[stem] }
            try {
                withContext(Dispatchers.IO) {
                    val inputStream = context.assets.open("anim/manifest.json")
                    val jsonString = inputStream.use { it.bufferedReader().readText() }
                    val parsed = jsonParser.decodeFromString<Map<String, SpriteFrameMeta>>(jsonString)
                    manifest = parsed
                    parsed[stem]
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * Resets cached manifest (useful for tests).
     */
    fun reset() {
        manifest = null
    }
}

/**
 * Extracts asset stem (filename without extension) from any asset path.
 * E.g. "videos/0001-2gPfomN.gif" -> "0001-2gPfomN"
 *      "anim/0001-2gPfomN.webp" -> "0001-2gPfomN"
 */
fun resolveAssetStem(assetPath: String): String {
    val clean = assetPath.removePrefix("/")
    val filename = clean.substringAfterLast('/')
    return filename.substringBeforeLast('.')
}
