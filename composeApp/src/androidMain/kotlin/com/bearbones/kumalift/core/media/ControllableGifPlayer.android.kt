package com.bearbones.kumalift.core.media

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
actual fun ControllableGifPlayer(
    assetPath: String?,
    contentDescription: String?,
    isPlaying: Boolean,
    speed: Float,
    fallbackThumbnailPath: String?,
    modifier: Modifier
) {
    if (assetPath.isNullOrBlank()) {
        ThumbnailFallback(modifier)
        return
    }

    val context = LocalContext.current
    var decodedGif by remember(assetPath) { mutableStateOf<DecodedGif?>(null) }
    var isLoading by remember(assetPath) { mutableStateOf(true) }

    // Persistent playback offset in milliseconds across play/pause and speed changes (Requirement E)
    var timeMs by remember(assetPath) { mutableLongStateOf(0L) }

    // Asynchronously load & decode with bounded LRU cache (Requirement D)
    LaunchedEffect(assetPath) {
        isLoading = true
        try {
            val normalizedPath = assetPath.removePrefix("/")
            val result = DefaultGifCache.getOrDecode(normalizedPath) {
                withContext(Dispatchers.IO) {
                    val bytes = context.assets.open(normalizedPath).use { it.readBytes() }
                    GifParser.parse(bytes, normalizedPath)
                }
            }
            decodedGif = result
        } catch (_: Exception) {
            decodedGif = null
        } finally {
            isLoading = false
        }
    }

    // Animation loop: advances timeMs only when isPlaying is true (Requirement C & E)
    // Automatically cleans up and cancels on exit or when isPlaying becomes false
    val currentSpeed by rememberUpdatedState(speed)
    LaunchedEffect(isPlaying, decodedGif) {
        val gif = decodedGif
        if (!isPlaying || gif == null || gif.totalDurationMs <= 0) return@LaunchedEffect

        var lastFrameTimeNanos = withFrameNanos { it }
        while (isActive) {
            val currentFrameTimeNanos = withFrameNanos { it }
            val deltaNanos = currentFrameTimeNanos - lastFrameTimeNanos
            lastFrameTimeNanos = currentFrameTimeNanos

            val deltaMs = (deltaNanos / 1_000_000.0) * currentSpeed.coerceAtLeast(0.1f)
            timeMs += deltaMs.toLong()
        }
    }

    val gif = decodedGif
    if (gif != null && gif.frames.isNotEmpty()) {
        val frameIndex = gif.findFrameIndex(timeMs)
        val currentFrame = gif.frames[frameIndex]
        val nextFrameIndex = (frameIndex + 1) % gif.frames.size
        val nextFrame = gif.frames[nextFrameIndex]
        val transitionAlpha = gif.calculateTransitionAlpha(timeMs, frameIndex)

        // Ensure Compose ImageBitmaps are created for rendering
        if (currentFrame.imageBitmap == null) {
            currentFrame.imageBitmap = Bitmap.createBitmap(
                currentFrame.pixels,
                currentFrame.width,
                currentFrame.height,
                Bitmap.Config.ARGB_8888
            ).asImageBitmap()
        }
        if (transitionAlpha > 0f && nextFrame.imageBitmap == null) {
            nextFrame.imageBitmap = Bitmap.createBitmap(
                nextFrame.pixels,
                nextFrame.width,
                nextFrame.height,
                Bitmap.Config.ARGB_8888
            ).asImageBitmap()
        }

        Canvas(
            modifier = modifier
        ) {
            val currentImage = currentFrame.imageBitmap ?: return@Canvas

            // Calculate destination rect to fit source into canvas while preserving aspect ratio
            val srcWidth = currentImage.width.toFloat()
            val srcHeight = currentImage.height.toFloat()
            val canvasWidth = size.width
            val canvasHeight = size.height

            val scale = minOf(canvasWidth / srcWidth, canvasHeight / srcHeight)
            val dstW = srcWidth * scale
            val dstH = srcHeight * scale
            val dstLeft = (canvasWidth - dstW) / 2f
            val dstTop = (canvasHeight - dstH) / 2f

            val dstOffset = IntOffset(dstLeft.roundToInt(), dstTop.roundToInt())
            val dstSize = IntSize(dstW.roundToInt(), dstH.roundToInt())

            // 1. Draw base current frame at full opacity (alpha = 1.0f) using FilterQuality.High (Requirement B)
            drawImage(
                image = currentImage,
                dstOffset = dstOffset,
                dstSize = dstSize,
                filterQuality = FilterQuality.High
            )

            // 2. Draw next frame during crossfade window with transitionAlpha (Requirement B)
            if (transitionAlpha > 0f) {
                val nextImage = nextFrame.imageBitmap
                if (nextImage != null) {
                    drawImage(
                        image = nextImage,
                        dstOffset = dstOffset,
                        dstSize = dstSize,
                        alpha = transitionAlpha,
                        filterQuality = FilterQuality.High
                    )
                }
            }
        }
    } else if (isLoading) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = Color(0x66888888)
            )
        }
    } else {
        GifFallback(modifier)
    }
}
