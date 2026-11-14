package com.bearbones.kumalift.core.media

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe LRU Cache for decoded GIF animations.
 *
 * Enforces:
 * - Max entry count (~20 assets per requirement D)
 * - Max total memory limit (default 40 MB)
 * - Single-flight deduplication: concurrent requests for the same assetPath share the same Deferred
 * - Eviction removes least-recently-used items when either limit is exceeded
 */
class GifBitmapCache(
    val maxEntries: Int = 20,
    val maxMemoryBytes: Long = 40L * 1024L * 1024L // 40 MB
) {
    private val lock = Mutex()
    private val lruMap = LinkedHashMap<String, DecodedGif>(maxEntries, 0.75f, true)
    private val inFlight = mutableMapOf<String, Deferred<DecodedGif>>()
    private var currentMemoryBytes: Long = 0L

    val entryCount: Int
        get() = lruMap.size

    val memoryUsageBytes: Long
        get() = currentMemoryBytes

    suspend fun getOrDecode(
        assetPath: String,
        decoder: suspend () -> DecodedGif
    ): DecodedGif {
        // Step 1: Fast path if already cached
        lock.withLock {
            val cached = lruMap[assetPath]
            if (cached != null) {
                return cached
            }
        }

        // Step 2: Single-flight deduplication
        var deferred: Deferred<DecodedGif>?
        var isLeader = false

        lock.withLock {
            val cached = lruMap[assetPath]
            if (cached != null) return cached

            deferred = inFlight[assetPath]
            if (deferred == null) {
                val newDeferred = CompletableDeferred<DecodedGif>()
                inFlight[assetPath] = newDeferred
                deferred = newDeferred
                isLeader = true
            }
        }

        if (isLeader) {
            try {
                val decoded = decoder()
                lock.withLock {
                    putInternal(assetPath, decoded)
                    (deferred as CompletableDeferred<DecodedGif>).complete(decoded)
                    inFlight.remove(assetPath)
                }
                return decoded
            } catch (t: Throwable) {
                lock.withLock {
                    (deferred as CompletableDeferred<DecodedGif>).completeExceptionally(t)
                    inFlight.remove(assetPath)
                }
                throw t
            }
        } else {
            return deferred!!.await()
        }
    }

    private fun putInternal(key: String, value: DecodedGif) {
        val existing = lruMap.remove(key)
        if (existing != null) {
            currentMemoryBytes -= existing.totalByteSize
        }

        lruMap[key] = value
        currentMemoryBytes += value.totalByteSize

        // Evict if over limits
        trimToSize()
    }

    suspend fun get(assetPath: String): DecodedGif? = lock.withLock {
        lruMap[assetPath]
    }

    suspend fun clear() = lock.withLock {
        lruMap.clear()
        inFlight.clear()
        currentMemoryBytes = 0L
    }

    private fun trimToSize() {
        val iterator = lruMap.iterator()
        while (iterator.hasNext() && (lruMap.size > maxEntries || currentMemoryBytes > maxMemoryBytes)) {
            val entry = iterator.next()
            currentMemoryBytes -= entry.value.totalByteSize
            iterator.remove()
        }
    }
}

/** Global singleton cache instance */
val DefaultGifCache = GifBitmapCache(maxEntries = 20, maxMemoryBytes = 40L * 1024L * 1024L)
