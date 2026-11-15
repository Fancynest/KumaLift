package com.bearbones.kumalift.core.media

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe LRU Cache for decoded Sprite sheet animations.
 *
 * Constraints per requirement C.3:
 * - One sprite is ~14 MB in memory (2160x1620x4 ARGB).
 * - Max cache bounded to 3 sprites (or ~48 MB limit).
 * - Single-flight deduplication: concurrent requests for the same stem share the same loader.
 * - Eviction removes least-recently-used items when either limit is exceeded.
 */
class SpriteCache(
    val maxEntries: Int = 3,
    val maxMemoryBytes: Long = 48L * 1024L * 1024L // 48 MB
) {
    private val lock = Mutex()
    private val lruMap = LinkedHashMap<String, SpriteAnimation>(maxEntries, 0.75f, true)
    private val inFlight = mutableMapOf<String, Deferred<SpriteAnimation>>()
    private var currentMemoryBytes: Long = 0L

    val entryCount: Int
        get() = lruMap.size

    val memoryUsageBytes: Long
        get() = currentMemoryBytes

    suspend fun getOrLoad(
        stem: String,
        loader: suspend () -> SpriteAnimation
    ): SpriteAnimation {
        // Fast path: cached
        lock.withLock {
            val cached = lruMap[stem]
            if (cached != null) return cached
        }

        // Deduplication
        var deferred: Deferred<SpriteAnimation>?
        var isLeader = false

        lock.withLock {
            val cached = lruMap[stem]
            if (cached != null) return cached

            deferred = inFlight[stem]
            if (deferred == null) {
                val newDeferred = CompletableDeferred<SpriteAnimation>()
                inFlight[stem] = newDeferred
                deferred = newDeferred
                isLeader = true
            }
        }

        if (isLeader) {
            try {
                val loaded = loader()
                lock.withLock {
                    putInternal(stem, loaded)
                    (deferred as CompletableDeferred<SpriteAnimation>).complete(loaded)
                    inFlight.remove(stem)
                }
                return loaded
            } catch (t: Throwable) {
                lock.withLock {
                    (deferred as CompletableDeferred<SpriteAnimation>).completeExceptionally(t)
                    inFlight.remove(stem)
                }
                throw t
            }
        } else {
            return deferred!!.await()
        }
    }

    private fun putInternal(key: String, value: SpriteAnimation) {
        val existing = lruMap.remove(key)
        if (existing != null) {
            currentMemoryBytes -= existing.byteSize
        }
        lruMap[key] = value
        currentMemoryBytes += value.byteSize
        trimToSize()
    }

    private fun trimToSize() {
        val iterator = lruMap.entries.iterator()
        while (iterator.hasNext() && (lruMap.size > maxEntries || currentMemoryBytes > maxMemoryBytes)) {
            val entry = iterator.next()
            currentMemoryBytes -= entry.value.byteSize
            iterator.remove()
        }
    }

    suspend fun get(stem: String): SpriteAnimation? = lock.withLock {
        lruMap[stem]
    }

    suspend fun clear() = lock.withLock {
        lruMap.clear()
        currentMemoryBytes = 0L
    }
}

val DefaultSpriteCache = SpriteCache()
