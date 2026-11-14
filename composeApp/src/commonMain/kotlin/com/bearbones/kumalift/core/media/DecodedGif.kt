package com.bearbones.kumalift.core.media

import androidx.compose.ui.graphics.ImageBitmap

/**
 * A single decoded frame of an animated GIF.
 *
 * @param pixels ARGB_8888 pixel array (width * height).
 * @param width Width in pixels.
 * @param height Height in pixels.
 * @param delayMs Duration to display this frame in milliseconds.
 * @param cumulativeTimeMs Start time offset of this frame within the loop.
 * @param imageBitmap Optional cached Compose ImageBitmap for direct canvas rendering.
 */
class GifFrame(
    val pixels: IntArray,
    val width: Int,
    val height: Int,
    val delayMs: Int,
    val cumulativeTimeMs: Int,
    var imageBitmap: ImageBitmap? = null
) {
    val byteSize: Long
        get() = pixels.size.toLong() * 4L
}

/**
 * A fully decoded animated GIF representation.
 */
class DecodedGif(
    val assetPath: String,
    val width: Int,
    val height: Int,
    val frames: List<GifFrame>,
    val totalDurationMs: Int
) {
    val totalByteSize: Long = frames.sumOf { it.byteSize }

    /**
     * Finds the active frame index for the given playback time offset in milliseconds.
     * Uses binary search for O(log N) lookup.
     */
    fun findFrameIndex(timeMs: Long): Int {
        if (frames.isEmpty()) return -1
        if (frames.size == 1 || totalDurationMs <= 0) return 0

        val loopedTime = (timeMs % totalDurationMs).toInt()
        var low = 0
        var high = frames.size - 1

        while (low <= high) {
            val mid = (low + high) ushr 1
            val frame = frames[mid]
            val frameStart = frame.cumulativeTimeMs
            val frameEnd = frameStart + frame.delayMs

            if (loopedTime in frameStart until frameEnd) {
                return mid
            } else if (loopedTime < frameStart) {
                high = mid - 1
            } else {
                low = mid + 1
            }
        }
        return (frames.size - 1).coerceAtLeast(0)
    }

    /**
     * Calculates the crossfade blend ratio between current frame and next frame.
     *
     * To prevent excessive ghosting while providing smooth motion:
     * - Only transitions in the final window of the frame duration.
     * - Transition window = min(50ms, (frame.delayMs * 0.25f).toInt()).
     * - Returns 0.0f during the stable display window (100% current frame, 0% ghosting).
     * - Linearly ramps from 0.0f to 1.0f during the transition window.
     */
    fun calculateTransitionAlpha(timeMs: Long, currentFrameIndex: Int): Float {
        if (frames.size <= 1 || totalDurationMs <= 0 || currentFrameIndex !in frames.indices) {
            return 0f
        }
        val currentFrame = frames[currentFrameIndex]
        val loopedTime = (timeMs % totalDurationMs).toInt()
        val frameStart = currentFrame.cumulativeTimeMs
        val frameDelay = currentFrame.delayMs
        val frameEnd = frameStart + frameDelay

        // Window for crossfading: max 50ms or 25% of frame duration
        val transitionWindowMs = (frameDelay * 0.25f).toInt().coerceIn(0, 50)
        if (transitionWindowMs <= 0) return 0f

        val transitionStart = frameEnd - transitionWindowMs
        if (loopedTime >= transitionStart) {
            val elapsed = (loopedTime - transitionStart).toFloat()
            return (elapsed / transitionWindowMs).coerceIn(0f, 1f)
        }
        return 0f
    }
}
