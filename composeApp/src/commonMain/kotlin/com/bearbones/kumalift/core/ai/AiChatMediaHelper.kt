package com.bearbones.kumalift.core.ai

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

data class AiMediaLaunchers(
    val launchCamera: () -> Unit,
    val launchGallery: () -> Unit
)

@Composable
expect fun rememberAiMediaLaunchers(
    onImageReady: (String) -> Unit
): AiMediaLaunchers

expect suspend fun loadCompressedImageBase64(localPath: String): Pair<String, String>?

@Composable
expect fun AiChatImageThumbnail(
    imagePath: String,
    contentDescription: String?,
    modifier: Modifier = Modifier
)
