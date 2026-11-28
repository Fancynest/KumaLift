package com.bearbones.kumalift.core.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

data class ProgressPhotoMediaLaunchers(
    val launchCamera: () -> Unit,
    val launchGallery: () -> Unit
)

@Composable
expect fun rememberProgressPhotoMediaLaunchers(
    onPhotoReady: (String) -> Unit,
    onCameraPermissionDenied: () -> Unit
): ProgressPhotoMediaLaunchers

@Composable
expect fun ProgressPhotoImage(
    imagePath: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
)

expect fun deleteProgressPhotoFile(localPath: String?)

expect fun shareProgressPhotoWithSummary(
    localPath: String?,
    summaryText: String,
    chooserTitle: String
): Boolean

expect fun sharePlainTextContent(
    text: String,
    chooserTitle: String
): Boolean

expect fun openAppPermissionSettings(): Boolean
