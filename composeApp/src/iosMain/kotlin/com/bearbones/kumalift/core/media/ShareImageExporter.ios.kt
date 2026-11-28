package com.bearbones.kumalift.core.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap

@Composable
actual fun rememberSharePhotoMediaLaunchers(
    onPhotoReady: (String) -> Unit,
    onCameraPermissionDenied: () -> Unit
): SharePhotoMediaLaunchers {
    return remember {
        SharePhotoMediaLaunchers(launchCamera = {}, launchGallery = {})
    }
}

actual object ShareImageExporter {
    actual suspend fun shareBitmap(
        bitmap: ImageBitmap,
        filePrefix: String,
        chooserTitle: String
    ): Boolean = false

    actual suspend fun saveBitmapToGallery(
        bitmap: ImageBitmap,
        filePrefix: String
    ): ShareSaveStatus = ShareSaveStatus.ERROR_UNSUPPORTED

    actual fun cleanupOldSharedImages(maxKeep: Int) = Unit

    actual suspend fun decodeSharePhotoBitmap(
        localPath: String,
        maxDimensionPx: Int
    ): ImageBitmap? = null

    actual suspend fun loadExerciseThumbnailBitmaps(
        assetPaths: List<String>
    ): Map<String, ImageBitmap> = emptyMap()

    actual fun deleteTempSharePhoto(localPath: String?) = Unit
}
