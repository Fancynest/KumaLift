package com.bearbones.kumalift.core.ai

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

const val MAX_AI_CHAT_IMAGES = 10

data class AiMediaLaunchers(
    val launchCamera: () -> Unit,
    val launchGallery: (maxSelectCount: Int) -> Unit
)

@Composable
expect fun rememberAiMediaLaunchers(
    onImagesReady: (List<String>) -> Unit
): AiMediaLaunchers

expect suspend fun loadCompressedImageBase64(localPath: String): Pair<String, String>?

expect suspend fun saveAiGeneratedImageBase64(base64Data: String, mimeType: String = "image/png"): String?

expect fun deleteAiChatImageFile(localPath: String?)

@Composable
expect fun AiChatImageThumbnail(
    imagePath: String,
    contentDescription: String?,
    modifier: Modifier = Modifier
)

@Composable
expect fun AiChatFullscreenImageViewer(
    imagePaths: List<String>,
    initialIndex: Int,
    closeContentDescription: String,
    pageIndicatorProvider: (current: Int, total: Int) -> String,
    onDismiss: () -> Unit
)
