package com.bearbones.kumalift.core.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
actual fun ExerciseThumbnailMedia(
    assetPath: String?,
    contentDescription: String?,
    modifier: Modifier
) {
    Box(
        modifier = modifier.background(Color(0xFF1C2028)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.FitnessCenter,
            contentDescription = contentDescription,
            tint = Color(0xFF6B7280),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
actual fun ExerciseGifMedia(
    assetPath: String?,
    contentDescription: String?,
    modifier: Modifier
) {
    Box(
        modifier = modifier.background(Color(0xFF161A22)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.FitnessCenter,
            contentDescription = contentDescription,
            tint = Color(0xFF4B5563),
            modifier = Modifier.size(48.dp)
        )
    }
}
