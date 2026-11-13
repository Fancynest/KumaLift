package com.bearbones.kumalift.core.media

import android.os.Build.VERSION.SDK_INT
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
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

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data(assetUri)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier,
        loading = {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFF161A22)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFFC6FF3D)
                )
            }
        },
        error = {
            ThumbnailFallback(Modifier.fillMaxSize())
        }
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
    val gifImageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .crossfade(true)
            .build()
    }

    val normalizedPath = assetPath.removePrefix("/")
    val assetUri = "file:///android_asset/$normalizedPath"

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data(assetUri)
            .build(),
        imageLoader = gifImageLoader,
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
