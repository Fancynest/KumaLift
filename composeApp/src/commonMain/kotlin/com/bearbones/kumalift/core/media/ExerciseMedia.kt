package com.bearbones.kumalift.core.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform-specific thumbnail image loader (lazy loaded, cached).
 */
@Composable
expect fun ExerciseThumbnailMedia(
    assetPath: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier
)

/**
 * Platform-specific animated GIF player (lazy loaded, animated, error-handled).
 */
@Composable
expect fun ExerciseGifMedia(
    assetPath: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier
)
