package com.bearbones.kumalift.core.media

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpritePlayerTests {

    private val json = Json { ignoreUnknownKeys = true }

    // =========================================================================
    // 1. Asset Stem Resolution
    // =========================================================================

    @Test
    fun testResolveAssetStem() {
        assertEquals("0001-2gPfomN", resolveAssetStem("videos/0001-2gPfomN.gif"))
        assertEquals("0001-2gPfomN", resolveAssetStem("anim/0001-2gPfomN.webp"))
        assertEquals("0025-EIeI8Vf", resolveAssetStem("/videos/0025-EIeI8Vf.gif"))
        assertEquals("0025-EIeI8Vf", resolveAssetStem("0025-EIeI8Vf.gif"))
        assertEquals("0025-EIeI8Vf", resolveAssetStem("0025-EIeI8Vf"))
    }

    // =========================================================================
    // 2. Sprite Frame Timing & Non-Uniform Delays
    // =========================================================================

    @Test
    fun testSpriteNonUniformFrameDelaysAndCumulativeDuration() {
        // Delays: 1000ms (pause), 100ms, 100ms, 500ms
        val meta = SpriteFrameMeta(
            frameW = 540,
            frameH = 540,
            cols = 4,
            rows = 1,
            count = 4,
            delays = listOf(1000, 100, 100, 500)
        )
        val sprite = SpriteAnimation("test", null, meta, 1000L)

        assertEquals(1700, sprite.totalDurationMs)

        // Time 0..999 -> Frame 0
        assertEquals(0, sprite.findFrameIndex(0))
        assertEquals(0, sprite.findFrameIndex(500))
        assertEquals(0, sprite.findFrameIndex(999))

        // Time 1000..1099 -> Frame 1
        assertEquals(1, sprite.findFrameIndex(1000))
        assertEquals(1, sprite.findFrameIndex(1050))
        assertEquals(1, sprite.findFrameIndex(1099))

        // Time 1100..1199 -> Frame 2
        assertEquals(2, sprite.findFrameIndex(1100))
        assertEquals(2, sprite.findFrameIndex(1199))

        // Time 1200..1699 -> Frame 3
        assertEquals(3, sprite.findFrameIndex(1200))
        assertEquals(3, sprite.findFrameIndex(1500))
        assertEquals(3, sprite.findFrameIndex(1699))

        // Loop: 1700ms -> Frame 0
        assertEquals(0, sprite.findFrameIndex(1700))
        assertEquals(1, sprite.findFrameIndex(2750)) // 2750 % 1700 = 1050 -> Frame 1
    }

    // =========================================================================
    // 3. Sprite Sheet Grid Source Offset Calculation
    // =========================================================================

    @Test
    fun testSpriteGridSourceOffsetCalculation() {
        val meta = SpriteFrameMeta(
            frameW = 540,
            frameH = 540,
            cols = 4,
            rows = 2,
            count = 6,
            delays = listOf(100, 100, 100, 100, 100, 100)
        )
        val sprite = SpriteAnimation("grid_test", null, meta, 1000L)

        // Row 0
        assertEquals(IntOffset(0, 0), sprite.getFrameSrcOffset(0))
        assertEquals(IntOffset(540, 0), sprite.getFrameSrcOffset(1))
        assertEquals(IntOffset(1080, 0), sprite.getFrameSrcOffset(2))
        assertEquals(IntOffset(1620, 0), sprite.getFrameSrcOffset(3))

        // Row 1
        assertEquals(IntOffset(0, 540), sprite.getFrameSrcOffset(4))
        assertEquals(IntOffset(540, 540), sprite.getFrameSrcOffset(5))

        assertEquals(IntSize(540, 540), sprite.frameSize)
    }

    // =========================================================================
    // 4. Crossfade Alpha Blending (Ghosting Prevention)
    // =========================================================================

    @Test
    fun testSpriteCrossfadeAlphaPreventsGhosting() {
        val meta = SpriteFrameMeta(
            frameW = 540,
            frameH = 540,
            cols = 4,
            rows = 1,
            count = 2,
            delays = listOf(200, 200) // 200ms delay, transition window = min(50, 50) = 50ms
        )
        val sprite = SpriteAnimation("cf_test", null, meta, 1000L)

        // Stable window: 0..149ms (transitionAlpha == 0f, 0% ghosting)
        assertEquals(0.0f, sprite.calculateTransitionAlpha(0, 0))
        assertEquals(0.0f, sprite.calculateTransitionAlpha(100, 0))
        assertEquals(0.0f, sprite.calculateTransitionAlpha(149, 0))

        // Transition window: 150..199ms
        assertEquals(0.0f, sprite.calculateTransitionAlpha(150, 0))
        val midAlpha = sprite.calculateTransitionAlpha(175, 0)
        assertTrue(midAlpha in 0.49f..0.51f, "Mid-transition alpha should be ~0.5, was $midAlpha")
        val endAlpha = sprite.calculateTransitionAlpha(199, 0)
        assertTrue(endAlpha > 0.9f, "End-transition alpha should approach 1.0, was $endAlpha")
    }

    // =========================================================================
    // 5. Sprite Cache (3 Assets Bounded, Memory Cap, Single-Flight)
    // =========================================================================

    @Test
    fun testSpriteCacheBoundedTo3Entries() = runBlocking {
        val cache = SpriteCache(maxEntries = 3, maxMemoryBytes = 100L * 1024L * 1024L)
        val dummyMeta = SpriteFrameMeta(540, 540, 4, 1, 1, listOf(100))

        for (i in 1..5) {
            val anim = SpriteAnimation("stem_$i", null, dummyMeta, 10_000L)
            cache.getOrLoad("stem_$i") { anim }
        }

        assertEquals(3, cache.entryCount, "SpriteCache must be capped at 3 entries (Requirement C.3)")
        assertNull(cache.get("stem_1"), "stem_1 should be evicted")
        assertNull(cache.get("stem_2"), "stem_2 should be evicted")
        assertNotNull(cache.get("stem_3"), "stem_3 should be present")
        assertNotNull(cache.get("stem_4"), "stem_4 should be present")
        assertNotNull(cache.get("stem_5"), "stem_5 should be present")
        Unit
    }

    @Test
    fun testSpriteCacheMemoryEviction() = runBlocking {
        // Cap memory at 30 MB (~2 sprites of 14 MB each)
        val cache = SpriteCache(maxEntries = 3, maxMemoryBytes = 30L * 1024L * 1024L)
        val dummyMeta = SpriteFrameMeta(540, 540, 4, 3, 12, listOf(100))
        val spriteByteSize = 14L * 1024L * 1024L // 14 MB

        cache.getOrLoad("s1") { SpriteAnimation("s1", null, dummyMeta, spriteByteSize) }
        cache.getOrLoad("s2") { SpriteAnimation("s2", null, dummyMeta, spriteByteSize) }

        assertEquals(2, cache.entryCount)
        assertEquals(28L * 1024L * 1024L, cache.memoryUsageBytes)

        // Loading 3rd 14 MB sprite pushes total to 42 MB > 30 MB cap -> evicts s1
        cache.getOrLoad("s3") { SpriteAnimation("s3", null, dummyMeta, spriteByteSize) }

        assertTrue(cache.memoryUsageBytes <= 30L * 1024L * 1024L, "Memory must stay under cap")
        assertNull(cache.get("s1"), "Oldest s1 should be evicted under memory pressure")
        assertNotNull(cache.get("s2"))
        assertNotNull(cache.get("s3"))
        Unit
    }

    @Test
    fun testSpriteCacheSingleFlightDeduplication() = runBlocking {
        val cache = SpriteCache(maxEntries = 3)
        val loadInvocationCount = AtomicInteger(0)
        val dummyMeta = SpriteFrameMeta(540, 540, 4, 1, 1, listOf(100))

        val jobs = (1..15).map {
            async(Dispatchers.Default) {
                cache.getOrLoad("shared_stem") {
                    loadInvocationCount.incrementAndGet()
                    SpriteAnimation("shared_stem", null, dummyMeta, 1000L)
                }
            }
        }

        val results = jobs.awaitAll()
        assertEquals(15, results.size)
        assertEquals(1, loadInvocationCount.get(), "Single-flight must guarantee exactly 1 load call")
        Unit
    }

    // =========================================================================
    // 6. JSON Manifest Parsing
    // =========================================================================

    @Test
    fun testManifestJsonParsing() {
        val jsonString = """
            {
              "0001-2gPfomN": {
                "frameW": 540,
                "frameH": 540,
                "cols": 4,
                "rows": 3,
                "count": 12,
                "delays": [1000, 100, 100, 100, 100, 100, 1000, 100, 100, 100, 100, 100]
              }
            }
        """.trimIndent()

        val parsed = json.decodeFromString<Map<String, SpriteFrameMeta>>(jsonString)
        assertNotNull(parsed["0001-2gPfomN"])
        val meta = parsed["0001-2gPfomN"]!!
        assertEquals(540, meta.frameW)
        assertEquals(540, meta.frameH)
        assertEquals(4, meta.cols)
        assertEquals(3, meta.rows)
        assertEquals(12, meta.count)
        assertEquals(12, meta.delays.size)
        assertEquals(1000, meta.delays[0])
        assertEquals(100, meta.delays[1])
    }
}
