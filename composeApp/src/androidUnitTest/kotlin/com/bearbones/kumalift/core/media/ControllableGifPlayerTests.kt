package com.bearbones.kumalift.core.media

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ControllableGifPlayerTests {

    // =========================================================================
    // Requirement A: Frame Timing Accuracy & Non-uniform Delays
    // =========================================================================

    @Test
    fun testNonUniformFrameDelaysAndCumulativeDuration() {
        // Construct frames with non-uniform delays: 80ms, 150ms, 250ms
        val f0 = GifFrame(IntArray(10), 10, 1, delayMs = 80, cumulativeTimeMs = 0)
        val f1 = GifFrame(IntArray(10), 10, 1, delayMs = 150, cumulativeTimeMs = 80)
        val f2 = GifFrame(IntArray(10), 10, 1, delayMs = 250, cumulativeTimeMs = 230)

        val gif = DecodedGif(
            assetPath = "test.gif",
            width = 10,
            height = 1,
            frames = listOf(f0, f1, f2),
            totalDurationMs = 480
        )

        assertEquals(480, gif.totalDurationMs)

        // Time 0..79 -> Frame 0
        assertEquals(0, gif.findFrameIndex(0))
        assertEquals(0, gif.findFrameIndex(40))
        assertEquals(0, gif.findFrameIndex(79))

        // Time 80..229 -> Frame 1
        assertEquals(1, gif.findFrameIndex(80))
        assertEquals(1, gif.findFrameIndex(150))
        assertEquals(1, gif.findFrameIndex(229))

        // Time 230..479 -> Frame 2
        assertEquals(2, gif.findFrameIndex(230))
        assertEquals(2, gif.findFrameIndex(300))
        assertEquals(2, gif.findFrameIndex(479))

        // Looped time: 480 -> 0 (Frame 0), 480 + 100 = 580 -> 100 (Frame 1)
        assertEquals(0, gif.findFrameIndex(480))
        assertEquals(1, gif.findFrameIndex(580))
    }

    @Test
    fun testFallbackDelayForZeroOrTooSmallDelays() {
        // GIF with Graphic Control Extension containing delay 0 and delay 1 (10ms)
        // Header: GIF89a (6 bytes)
        // Logical Screen: 1x1, no GCT (7 bytes: 01 00 01 00 00 00 00)
        // GCE 1: 21 F9 04 00 00 00 00 00 -> delay = 0
        // Image 1: 2C 00 00 00 00 01 00 01 00 00 02 02 44 01 00
        // Trailer: 3B
        val mockGifBytes = byteArrayOf(
            'G'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(),
            '8'.code.toByte(), '9'.code.toByte(), 'a'.code.toByte(),
            1, 0, 1, 0, 0, 0, 0, // 1x1 screen, no GCT
            0x21.toByte(), 0xF9.toByte(), 4, 0, 0, 0, 0, 0, // delay 0 -> should fallback to 100ms
            0x2C.toByte(), 0, 0, 0, 0, 1, 0, 1, 0, 0, // 1x1 image descriptor
            2, 2, 0x44.toByte(), 1, 0, // LZW data (clear 4, pixel 0, eoi 5)
            0x3B.toByte() // Trailer
        )

        val decoded = GifParser.parse(mockGifBytes, "fallback_test.gif")
        assertEquals(1, decoded.frames.size)
        assertEquals(100, decoded.frames[0].delayMs, "Zero delay must fallback to 100ms")
        assertEquals(100, decoded.totalDurationMs)
    }

    // =========================================================================
    // Requirement B: Crossfade Rendering & Ghosting Prevention
    // =========================================================================

    @Test
    fun testCrossfadeTransitionAlphaPreventsExcessiveGhosting() {
        // Frame delay 200ms, start 0. Transition window = min(50, 200 * 0.25) = 50ms.
        // Stable window: 0..149ms (transitionAlpha == 0f)
        // Transition window: 150..199ms (transitionAlpha ramps 0f -> 1f)
        val f0 = GifFrame(IntArray(1), 1, 1, delayMs = 200, cumulativeTimeMs = 0)
        val f1 = GifFrame(IntArray(1), 1, 1, delayMs = 200, cumulativeTimeMs = 200)

        val gif = DecodedGif("cf.gif", 1, 1, listOf(f0, f1), 400)

        // Stable window: absolutely 0.0f alpha for next frame -> no ghosting
        assertEquals(0.0f, gif.calculateTransitionAlpha(0, 0))
        assertEquals(0.0f, gif.calculateTransitionAlpha(50, 0))
        assertEquals(0.0f, gif.calculateTransitionAlpha(100, 0))
        assertEquals(0.0f, gif.calculateTransitionAlpha(149, 0))

        // Transition window: 150ms -> 0f, 175ms -> 0.5f, 199ms -> ~0.98f
        assertEquals(0.0f, gif.calculateTransitionAlpha(150, 0))
        val midAlpha = gif.calculateTransitionAlpha(175, 0)
        assertTrue(midAlpha in 0.49f..0.51f, "Mid-transition alpha should be ~0.5, was $midAlpha")
        val endAlpha = gif.calculateTransitionAlpha(199, 0)
        assertTrue(endAlpha > 0.9f, "End-transition alpha should approach 1.0, was $endAlpha")
    }

    // =========================================================================
    // Requirement D: Bitmap Cache Optimization (LRU, 20 assets, memory cap)
    // =========================================================================

    @Test
    fun testCacheEvictsOldestWhenMaxEntriesExceeded() = runBlocking {
        val cache = GifBitmapCache(maxEntries = 20, maxMemoryBytes = 100L * 1024L * 1024L)

        // Insert 25 distinct assets
        for (i in 1..25) {
            val frame = GifFrame(IntArray(10), 10, 1, 100, 0)
            val gif = DecodedGif("asset_$i.gif", 10, 1, listOf(frame), 100)
            cache.getOrDecode("asset_$i.gif") { gif }
        }

        assertEquals(20, cache.entryCount, "Cache must be capped at maxEntries = 20")
        // Assets 1..5 should have been evicted
        for (i in 1..5) {
            assertEquals(null, cache.get("asset_$i.gif"), "Oldest asset_$i should have been evicted")
        }
        // Assets 6..25 should be present
        for (i in 6..25) {
            assertNotNull(cache.get("asset_$i.gif"), "Recent asset_$i should be present")
        }
    }

    @Test
    fun testCacheEvictsWhenMemoryBytesExceeded() = runBlocking {
        // 1 frame of 100x100 pixels = 10,000 ints * 4 bytes = 40,000 bytes (~40 KB)
        // Cap memory at 100 KB -> can hold at most 2 items
        val cache = GifBitmapCache(maxEntries = 20, maxMemoryBytes = 90_000L)

        val frame1 = GifFrame(IntArray(10_000), 100, 100, 100, 0)
        val gif1 = DecodedGif("big_1.gif", 100, 100, listOf(frame1), 100)
        cache.getOrDecode("big_1.gif") { gif1 }

        val frame2 = GifFrame(IntArray(10_000), 100, 100, 100, 0)
        val gif2 = DecodedGif("big_2.gif", 100, 100, listOf(frame2), 100)
        cache.getOrDecode("big_2.gif") { gif2 }

        assertEquals(2, cache.entryCount)

        // Inserting 3rd big asset causes memory eviction
        val frame3 = GifFrame(IntArray(10_000), 100, 100, 100, 0)
        val gif3 = DecodedGif("big_3.gif", 100, 100, listOf(frame3), 100)
        cache.getOrDecode("big_3.gif") { gif3 }

        assertTrue(cache.memoryUsageBytes <= 90_000L, "Memory usage should not exceed cap")
        assertEquals(null, cache.get("big_1.gif"), "Oldest big_1 should be evicted due to memory pressure")
    }

    @Test
    fun testCacheSingleFlightPreventsDuplicateConcurrentDecoding() = runBlocking {
        val cache = GifBitmapCache(maxEntries = 20)
        val decodeInvocationCount = AtomicInteger(0)

        // Launch 20 concurrent requests for the exact same asset
        val jobs = (1..20).map {
            async(Dispatchers.Default) {
                cache.getOrDecode("concurrent.gif") {
                    decodeInvocationCount.incrementAndGet()
                    val frame = GifFrame(IntArray(10), 10, 1, 100, 0)
                    DecodedGif("concurrent.gif", 10, 1, listOf(frame), 100)
                }
            }
        }

        val results = jobs.awaitAll()
        assertEquals(20, results.size)
        // The decoder must have only been invoked ONCE despite 20 concurrent callers
        assertEquals(1, decodeInvocationCount.get(), "Single-flight must guarantee exactly 1 decode")
    }

    // =========================================================================
    // Real Asset Decoding Verification
    // =========================================================================

    @Test
    fun testRealExerciseGifDecoding() {
        val assetFile = File("src/androidMain/assets/videos/0025-EIeI8Vf.gif")
        if (!assetFile.exists()) {
            println("Skipping real asset test: file not found at ${assetFile.absolutePath}")
            return
        }

        val bytes = assetFile.readBytes()
        assertTrue(bytes.isNotEmpty())

        val decoded = GifParser.parse(bytes, "0025-EIeI8Vf.gif")
        assertNotNull(decoded)
        assertTrue(decoded.width > 0, "Width should be > 0")
        assertTrue(decoded.height > 0, "Height should be > 0")
        assertTrue(decoded.frames.isNotEmpty(), "Frames should not be empty")
        assertTrue(decoded.totalDurationMs > 0, "Total duration should be > 0")

        // Validate frame timings
        for (i in decoded.frames.indices) {
            val frame = decoded.frames[i]
            assertTrue(frame.delayMs >= 10, "Frame $i delay should be >= 10ms")
            if (i > 0) {
                val prev = decoded.frames[i - 1]
                assertEquals(
                    prev.cumulativeTimeMs + prev.delayMs,
                    frame.cumulativeTimeMs,
                    "Cumulative time of frame $i must equal previous frame end"
                )
            }
        }
    }
}
