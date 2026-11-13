package com.bearbones.kumalift.core.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

@Composable
actual fun ExerciseThumbnailMedia(
    assetPath: String?,
    contentDescription: String?,
    modifier: Modifier
) {
    if (assetPath.isNullOrBlank()) {
        ThumbnailFallback(modifier)
        return
    }

    val context = LocalContext.current
    val normalizedPath = assetPath.removePrefix("/")
    val assetUri = "file:///android_asset/$normalizedPath"

    // Downsample from disk into a hardware bitmap (160x160 px).
    // Reduces memory footprint from ~4.6MB to ~100KB per image (>97% memory reduction),
    // and eliminates subcomposition passes for silky smooth 120Hz list scrolling.
    val imageRequest = remember(assetUri) {
        ImageRequest.Builder(context)
            .data(assetUri)
            .size(160, 160)
            .allowHardware(true)
            .memoryCacheKey(assetUri)
            .crossfade(false)
            .build()
    }

    AsyncImage(
        model = imageRequest,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier,
        placeholder = remember { ColorPainter(Color(0xFF161A22)) },
        error = remember { ColorPainter(Color(0xFF1C2028)) }
    )
}

@Composable
actual fun ExerciseGifMedia(
    assetPath: String?,
    contentDescription: String?,
    modifier: Modifier
) {
    if (assetPath.isNullOrBlank()) {
        GifFallback(modifier)
        return
    }

    val context = LocalContext.current
    val normalizedPath = assetPath.removePrefix("/")
    val assetUri = "file:///android_asset/$normalizedPath"

    // Uses the global singleton ImageLoader provided by KumaApplication
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data(assetUri)
            .build(),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier,
        loading = {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFF161A22)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 3.dp,
                    color = Color(0xFFC6FF3D)
                )
            }
        },
        error = {
            GifFallback(Modifier.fillMaxSize())
        }
    )
}

@Composable
private fun ThumbnailFallback(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(Color(0xFF1C2028)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.FitnessCenter,
            contentDescription = null,
            tint = Color(0xFF6B7280),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun GifFallback(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(Color(0xFF161A22)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.FitnessCenter,
            contentDescription = null,
            tint = Color(0xFF4B5563),
            modifier = Modifier.size(48.dp)
        )
    }
}
