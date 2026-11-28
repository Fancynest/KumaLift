package com.bearbones.kumalift.core.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

enum class ShareSaveStatus {
    SUCCESS,
    ERROR_WRITE,
    ERROR_UNSUPPORTED
}

data class SharePhotoMediaLaunchers(
    val launchCamera: () -> Unit,
    val launchGallery: () -> Unit
)

@Composable
expect fun rememberSharePhotoMediaLaunchers(
    onPhotoReady: (String) -> Unit,
    onCameraPermissionDenied: () -> Unit
): SharePhotoMediaLaunchers

expect object ShareImageExporter {
    suspend fun shareBitmap(
        bitmap: ImageBitmap,
        filePrefix: String,
        chooserTitle: String
    ): Boolean

    suspend fun saveBitmapToGallery(
        bitmap: ImageBitmap,
        filePrefix: String
    ): ShareSaveStatus

    fun cleanupOldSharedImages(maxKeep: Int = 20)

    suspend fun decodeSharePhotoBitmap(
        localPath: String,
        maxDimensionPx: Int = 2048
    ): ImageBitmap?

    suspend fun loadExerciseThumbnailBitmaps(
        assetPaths: List<String>
    ): Map<String, ImageBitmap>

    fun deleteTempSharePhoto(localPath: String?)
}
