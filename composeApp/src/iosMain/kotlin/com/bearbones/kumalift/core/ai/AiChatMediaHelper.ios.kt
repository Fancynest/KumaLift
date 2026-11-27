package com.bearbones.kumalift.core.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.bearbones.kumalift.core.designsystem.LocalKumaColors

@Composable
actual fun rememberAiMediaLaunchers(
    onImagesReady: (List<String>) -> Unit
): AiMediaLaunchers {
    return remember { AiMediaLaunchers(launchCamera = {}, launchGallery = {}) }
}

actual suspend fun loadCompressedImageBase64(localPath: String): Pair<String, String>? = null

@Composable
actual fun AiChatFullscreenImageViewer(
    imagePaths: List<String>,
    initialIndex: Int,
    closeContentDescription: String,
    pageIndicatorProvider: (current: Int, total: Int) -> String,
    onDismiss: () -> Unit
) = Unit

@Composable
actual fun AiChatImageThumbnail(
    imagePath: String,
    contentDescription: String?,
    modifier: Modifier
) {
    val colors = LocalKumaColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Image,
            contentDescription = contentDescription,
            tint = colors.textSecondary
        )
    }
}
