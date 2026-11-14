package com.bearbones.kumalift.core.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Controllable animated GIF player for Compose Multiplatform.
 *
 * Implements:
 * - Accurate per-frame timing and non-uniform delays
 * - Fallback to 100ms for delay <= 10ms or 0ms
 * - Soft crossfade rendering without ghosting or double-compositing brightness shifts
 * - Clean lifecycle management (animation ceases when leaving composition)
 * - Thread-safe LRU bitmap caching bounded by ~20 assets and total memory
 * - Playback state persistence: pausing/resuming preserves current frame timeMs
 * - Speed scaling without resetting frame position
 */
@Composable
expect fun ControllableGifPlayer(
    assetPath: String?,
    contentDescription: String? = null,
    isPlaying: Boolean = true,
    speed: Float = 1.0f,
    fallbackThumbnailPath: String? = null,
    modifier: Modifier = Modifier
)
