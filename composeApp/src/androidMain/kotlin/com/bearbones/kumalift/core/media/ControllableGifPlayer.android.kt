package com.bearbones.kumalift.core.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
    var loadedSprite by remember(assetPath) { mutableStateOf<SpriteAnimation?>(null) }
    var loadedGif by remember(assetPath) { mutableStateOf<DecodedGif?>(null) }
    var isLoading by remember(assetPath) { mutableStateOf(true) }

    // Persistent playback offset in milliseconds across play/pause and speed changes
    var timeMs by remember(assetPath) { mutableLongStateOf(0L) }

    // Asynchronously load sprite sheet with LRU cache, falling back to legacy GIF parser
    LaunchedEffect(assetPath) {
        isLoading = true
        try {
            val stem = resolveAssetStem(assetPath)
            val meta = SpriteManifestRepository.getMeta(context, stem)

            var spriteResult: SpriteAnimation? = null
            if (meta != null) {
                try {
                    spriteResult = DefaultSpriteCache.getOrLoad(stem) {
                        withContext(Dispatchers.IO) {
                            context.assets.open("anim/$stem.webp").use { stream ->
                                val bitmap = BitmapFactory.decodeStream(stream)
                                    ?: throw IllegalStateException("Failed to decode WebP stream for $stem")
                                val imageBitmap = bitmap.asImageBitmap()
                                val byteSize = bitmap.allocationByteCount.toLong()
                                SpriteAnimation(stem, imageBitmap, meta, byteSize)
                            }
                        }
                    }
                } catch (_: Exception) {
                    spriteResult = null
                }
            }

            if (spriteResult != null) {
                loadedSprite = spriteResult
                loadedGif = null
            } else {
                // Fallback to legacy GIF parsing if sprite sheet is not available
                val normalizedPath = assetPath.removePrefix("/")
                val gifResult = DefaultGifCache.getOrDecode(normalizedPath) {
                    withContext(Dispatchers.IO) {
                        val bytes = context.assets.open(normalizedPath).use { it.readBytes() }
                        GifParser.parse(bytes, normalizedPath)
                    }
                }
                loadedGif = gifResult
                loadedSprite = null
            }
        } catch (_: Exception) {
            loadedSprite = null
            loadedGif = null
        } finally {
            isLoading = false
        }
    }

    // Animation loop: advances timeMs only when isPlaying is true
    val currentSpeed by rememberUpdatedState(speed)
    val totalDurationMs = loadedSprite?.totalDurationMs ?: loadedGif?.totalDurationMs ?: 0
    LaunchedEffect(isPlaying, totalDurationMs) {
        if (!isPlaying || totalDurationMs <= 0) return@LaunchedEffect

        var lastFrameTimeNanos = withFrameNanos { it }
        while (isActive) {
            val currentFrameTimeNanos = withFrameNanos { it }
            val deltaNanos = currentFrameTimeNanos - lastFrameTimeNanos
            lastFrameTimeNanos = currentFrameTimeNanos

            val deltaMs = (deltaNanos / 1_000_000.0) * currentSpeed.coerceAtLeast(0.1f)
            timeMs += deltaMs.toLong()
        }
    }

    val sprite = loadedSprite
    val gif = loadedGif

    if (sprite != null && sprite.image != null && sprite.meta.count > 0) {
        val frameIndex = sprite.findFrameIndex(timeMs)
        val nextFrameIndex = (frameIndex + 1) % sprite.meta.count
        val transitionAlpha = sprite.calculateTransitionAlpha(timeMs, frameIndex)

        Canvas(modifier = modifier) {
            val spriteImage = sprite.image ?: return@Canvas
            val canvasWidth = size.width
            val canvasHeight = size.height
            val srcW = sprite.meta.frameW.toFloat()
            val srcH = sprite.meta.frameH.toFloat()

            val scale = minOf(canvasWidth / srcW, canvasHeight / srcH)
            val dstW = srcW * scale
            val dstH = srcH * scale
            val dstLeft = (canvasWidth - dstW) / 2f
            val dstTop = (canvasHeight - dstH) / 2f

            val dstOffset = IntOffset(dstLeft.roundToInt(), dstTop.roundToInt())
            val dstSize = IntSize(dstW.roundToInt(), dstH.roundToInt())

            // 1. Draw base current frame from sprite sheet using FilterQuality.High
            drawImage(
                image = spriteImage,
                srcOffset = sprite.getFrameSrcOffset(frameIndex),
                srcSize = sprite.frameSize,
                dstOffset = dstOffset,
                dstSize = dstSize,
                filterQuality = FilterQuality.High
            )

            // 2. Draw next frame during crossfade window with transitionAlpha
            if (transitionAlpha > 0f) {
                drawImage(
                    image = spriteImage,
                    srcOffset = sprite.getFrameSrcOffset(nextFrameIndex),
                    srcSize = sprite.frameSize,
                    dstOffset = dstOffset,
                    dstSize = dstSize,
                    alpha = transitionAlpha,
                    filterQuality = FilterQuality.High
                )
            }
        }
    } else if (gif != null && gif.frames.isNotEmpty()) {
        val frameIndex = gif.findFrameIndex(timeMs)
        val currentFrame = gif.frames[frameIndex]
        val nextFrameIndex = (frameIndex + 1) % gif.frames.size
        val nextFrame = gif.frames[nextFrameIndex]
        val transitionAlpha = gif.calculateTransitionAlpha(timeMs, frameIndex)

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

        Canvas(modifier = modifier) {
            val currentImage = currentFrame.imageBitmap ?: return@Canvas
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

            drawImage(
                image = currentImage,
                dstOffset = dstOffset,
                dstSize = dstSize,
                filterQuality = FilterQuality.High
            )

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
