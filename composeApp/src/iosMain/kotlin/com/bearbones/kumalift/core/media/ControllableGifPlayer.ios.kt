package com.bearbones.kumalift.core.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import com.bearbones.kumalift.core.designsystem.LocalKumaColors

@Composable
actual fun ControllableGifPlayer(
    assetPath: String?,
    contentDescription: String?,
    isPlaying: Boolean,
    speed: Float,
    fallbackThumbnailPath: String?,
    modifier: Modifier
) {
    Box(
        modifier = modifier.background(LocalKumaColors.current.mediaPlaceholder),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.FitnessCenter,
            contentDescription = contentDescription,
            tint = LocalKumaColors.current.mediaPlaceholderIcon,
            modifier = Modifier.size(48.dp)
        )
    }
}
