package com.bearbones.kumalift.core.media

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.serialization.Serializable

/**
 * Metadata for a packed sprite sheet of exercise animation frames.
 *
 * @param frameW Width of a single frame in physical pixels (e.g. 540).
 * @param frameH Height of a single frame in physical pixels (e.g. 540).
 * @param cols Number of columns in the sprite grid (max 4).
 * @param rows Number of rows in the sprite grid.
 * @param count Total number of animation frames.
 * @param delays Display duration in milliseconds for each frame.
 */
@Serializable
data class SpriteFrameMeta(
    val frameW: Int,
    val frameH: Int,
    val cols: Int,
    val rows: Int,
    val count: Int,
    val delays: List<Int>
)

/**
 * A decoded sprite sheet animation held as a single ImageBitmap.
 *
 * Preserves:
 * - Single GPU texture / ImageBitmap (no bitmap slicing)
 * - Exact per-frame durations with non-uniform delays
 * - Binary-search frame lookup O(log N)
 * - Soft crossfade alpha blending in the last window of each frame
 */
class SpriteAnimation(
    val stem: String,
    val image: ImageBitmap? = null,
    val meta: SpriteFrameMeta,
    val byteSize: Long
) {
    val totalDurationMs: Int
    val cumulativeDelays: IntArray

    init {
        var cum = 0
        cumulativeDelays = IntArray(meta.delays.size)
        for (i in meta.delays.indices) {
            cumulativeDelays[i] = cum
            cum += meta.delays[i]
        }
        totalDurationMs = cum
    }

    /**
     * Finds the active frame index for the given playback time in milliseconds.
     */
    fun findFrameIndex(timeMs: Long): Int {
        if (meta.count <= 0) return -1
        if (meta.count == 1 || totalDurationMs <= 0) return 0

        val loopedTime = (timeMs % totalDurationMs).toInt()
        var low = 0
        var high = meta.count - 1

        while (low <= high) {
            val mid = (low + high) ushr 1
            val start = cumulativeDelays[mid]
            val end = start + meta.delays[mid]

            if (loopedTime in start until end) {
                return mid
            } else if (loopedTime < start) {
                high = mid - 1
            } else {
                low = mid + 1
            }
        }
        return (meta.count - 1).coerceAtLeast(0)
    }

    /**
     * Computes crossfade blend ratio for the transition between current frame and next frame.
     * Prevents ghosting by only ramping alpha in the final window of the frame duration.
     */
    fun calculateTransitionAlpha(timeMs: Long, currentFrameIndex: Int): Float {
        if (meta.count <= 1 || totalDurationMs <= 0 || currentFrameIndex !in 0 until meta.count) {
            return 0f
        }
        val currentDelay = meta.delays[currentFrameIndex]
        val transitionWindowMs = (currentDelay * 0.25f).toInt().coerceIn(0, 50)
        if (transitionWindowMs <= 0) return 0f

        val loopedTime = (timeMs % totalDurationMs).toInt()
        val frameStart = cumulativeDelays[currentFrameIndex]
        val frameEnd = frameStart + currentDelay
        val transitionStart = frameEnd - transitionWindowMs

        if (loopedTime >= transitionStart) {
            val elapsed = (loopedTime - transitionStart).toFloat()
            return (elapsed / transitionWindowMs).coerceIn(0f, 1f)
        }
        return 0f
    }

    /**
     * Computes source top-left pixel offset within the packed sprite sheet for frame index.
     */
    fun getFrameSrcOffset(frameIndex: Int): IntOffset {
        val col = frameIndex % meta.cols
        val row = frameIndex / meta.cols
        return IntOffset(col * meta.frameW, row * meta.frameH)
    }

    val frameSize: IntSize = IntSize(meta.frameW, meta.frameH)
}
