package com.bearbones.kumalift.core.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.bearbones.kumalift.core.designsystem.LocalKumaColors

@Composable
actual fun rememberProgressPhotoMediaLaunchers(
    onPhotoReady: (String) -> Unit,
    onCameraPermissionDenied: () -> Unit
): ProgressPhotoMediaLaunchers {
    return remember {
        ProgressPhotoMediaLaunchers(launchCamera = {}, launchGallery = {})
    }
}

@Composable
actual fun ProgressPhotoImage(
    imagePath: String,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale
) {
    val colors = LocalKumaColors.current
    Box(
        modifier = modifier.background(colors.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.PhotoCamera,
            contentDescription = contentDescription,
            tint = colors.textSecondary
        )
    }
}

actual fun deleteProgressPhotoFile(localPath: String?) = Unit

actual fun shareProgressPhotoWithSummary(
    localPath: String?,
    summaryText: String,
    chooserTitle: String
): Boolean = false

actual fun sharePlainTextContent(
    text: String,
    chooserTitle: String
): Boolean = false

actual fun openAppPermissionSettings(): Boolean = false
