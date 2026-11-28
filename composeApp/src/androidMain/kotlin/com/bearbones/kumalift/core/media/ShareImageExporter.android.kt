package com.bearbones.kumalift.core.media

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.bearbones.kumalift.core.logging.KumaLogger
import com.bearbones.kumalift.core.timer.AndroidAppProvider
import com.bearbones.kumalift.core.util.currentEpochMillis
import com.bearbones.kumalift.domain.share.buildShareExportFileName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private const val SHARE_CARD_PHOTO_DIR = "share_card_photos"
private const val SHARE_CARD_CACHE_DIR = "kuma_camera_capture"
private const val SHARE_EXPORT_CACHE_DIR = "shared_images"
private const val SHARE_CARD_PREFIX = "share_bg_"

@Composable
actual fun rememberSharePhotoMediaLaunchers(
    onPhotoReady: (String) -> Unit,
    onCameraPermissionDenied: () -> Unit
): SharePhotoMediaLaunchers {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latestOnPhotoReady by rememberUpdatedState(onPhotoReady)

    val launchCamera = rememberPermissionAwareCameraCapture(
        cacheSubDir = SHARE_CARD_CACHE_DIR,
        outputSubDir = SHARE_CARD_PHOTO_DIR,
        filePrefix = SHARE_CARD_PREFIX,
        onSaved = { path -> latestOnPhotoReady(path) },
        onPermissionDenied = onCameraPermissionDenied
    )

    val handlePickedUri: (Uri?) -> Unit = { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val savedPath = compressAndStoreJpeg(
                    context = context,
                    outputSubDir = SHARE_CARD_PHOTO_DIR,
                    filePrefix = SHARE_CARD_PREFIX,
                    maxDimensionPx = 2048,
                    openStream = { context.contentResolver.openInputStream(uri) }
                )
                if (savedPath != null) {
                    withContext(Dispatchers.Main) { latestOnPhotoReady(savedPath) }
                }
            }
        }
    }

    val pickSingleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        handlePickedUri(uri)
    }

    val getContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        handlePickedUri(uri)
    }

    return remember(context, launchCamera, pickSingleLauncher, getContentLauncher) {
        buildSharePhotoMediaLaunchers(
            context = context,
            launchCamera = launchCamera,
            onLaunchSingle = { req -> pickSingleLauncher.launch(req) },
            onLaunchFallback = { mime -> getContentLauncher.launch(mime) }
        )
    }
}

private fun buildSharePhotoMediaLaunchers(
    context: Context,
    launchCamera: () -> Unit,
    onLaunchSingle: (androidx.activity.result.PickVisualMediaRequest) -> Unit,
    onLaunchFallback: (String) -> Unit
): SharePhotoMediaLaunchers = SharePhotoMediaLaunchers(
    launchCamera = launchCamera,
    launchGallery = {
        launchAndroidGalleryPicker(
            context = context,
            maxSelectCount = 1,
            onLaunchSingle = onLaunchSingle,
            onLaunchFallback = onLaunchFallback
        )
    }
)

actual object ShareImageExporter {

    actual suspend fun shareBitmap(
        bitmap: ImageBitmap,
        filePrefix: String,
        chooserTitle: String
    ): Boolean {
        val context = AndroidAppProvider.appContext ?: return false
        return withContext(Dispatchers.IO) {
            try {
                val androidBitmap = toSoftwareArgb8888(bitmap.asAndroidBitmap())
                val dir = File(context.cacheDir, SHARE_EXPORT_CACHE_DIR).apply { mkdirs() }
                val fileName = buildShareExportFileName(filePrefix, currentEpochMillis())
                val outFile = File(dir, fileName)
                FileOutputStream(outFile).use { stream ->
                    if (!androidBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                        throw IllegalStateException("PNG compression returned false")
                    }
                    stream.flush()
                }
                cleanupOldSharedImages(maxKeep = 20)
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    outFile
                )
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(sendIntent, chooserTitle).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                withContext(Dispatchers.Main) {
                    context.startActivity(chooser)
                }
                true
            } catch (e: Exception) {
                KumaLogger.w("ShareImageExporter", "Failed sharing PNG image: ${e.message}")
                false
            }
        }
    }

    actual suspend fun saveBitmapToGallery(
        bitmap: ImageBitmap,
        filePrefix: String
    ): ShareSaveStatus {
        val context = AndroidAppProvider.appContext ?: return ShareSaveStatus.ERROR_WRITE
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return ShareSaveStatus.ERROR_UNSUPPORTED
        }
        return withContext(Dispatchers.IO) {
            var insertedUri: Uri? = null
            try {
                val androidBitmap = toSoftwareArgb8888(bitmap.asAndroidBitmap())
                val fileName = buildShareExportFileName(filePrefix, currentEpochMillis())
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/KumaLift")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext ShareSaveStatus.ERROR_WRITE
                insertedUri = uri

                resolver.openOutputStream(uri)?.use { stream ->
                    if (!androidBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                        throw IllegalStateException("PNG compression failed")
                    }
                    stream.flush()
                } ?: throw IllegalStateException("Cannot open MediaStore output stream")

                val doneValues = ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }
                resolver.update(uri, doneValues, null, null)
                ShareSaveStatus.SUCCESS
            } catch (e: Exception) {
                KumaLogger.w("ShareImageExporter", "Failed saving PNG to gallery: ${e.message}")
                insertedUri?.let { uri ->
                    try {
                        context.contentResolver.delete(uri, null, null)
                    } catch (_: Exception) {
                    }
                }
                ShareSaveStatus.ERROR_WRITE
            }
        }
    }

    actual fun cleanupOldSharedImages(maxKeep: Int) {
        val context = AndroidAppProvider.appContext ?: return
        try {
            val dir = File(context.cacheDir, SHARE_EXPORT_CACHE_DIR)
            if (!dir.exists() || !dir.isDirectory) return
            val files = dir.listFiles { file -> file.isFile }?.sortedByDescending { it.lastModified() } ?: return
            files.drop(maxKeep.coerceAtLeast(0)).forEach { oldFile ->
                try {
                    oldFile.delete()
                } catch (_: Exception) {
                }
            }
        } catch (e: Exception) {
            KumaLogger.w("ShareImageExporter", "Cache cleanup error: ${e.message}")
        }
    }

    actual suspend fun decodeSharePhotoBitmap(
        localPath: String,
        maxDimensionPx: Int
    ): ImageBitmap? = withContext(Dispatchers.IO) {
        val cleanPath = localPath.removePrefix("file://").trim()
        if (cleanPath.isBlank()) return@withContext null
        val file = File(cleanPath)
        if (!file.exists() || file.length() <= 0L) return@withContext null
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            var sampleSize = 1
            val longest = maxOf(bounds.outWidth, bounds.outHeight)
            while (longest / sampleSize > maxDimensionPx) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val raw = BitmapFactory.decodeFile(file.absolutePath, decodeOpts) ?: return@withContext null
            val rotated = applyExifOrientationIfNeeded(file.absolutePath, raw)
            rotated.asImageBitmap()
        } catch (e: Exception) {
            KumaLogger.w("ShareImageExporter", "Failed decoding share photo: ${e.message}")
            null
        }
    }

    actual suspend fun loadExerciseThumbnailBitmaps(
        assetPaths: List<String>
    ): Map<String, ImageBitmap> = withContext(Dispatchers.IO) {
        val context = AndroidAppProvider.appContext ?: return@withContext emptyMap()
        val result = mutableMapOf<String, ImageBitmap>()
        val opts = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        assetPaths.distinct().filter { it.isNotBlank() }.forEach { rawPath ->
            val normalized = rawPath.removePrefix("/")
            try {
                context.assets.open(normalized).use { stream ->
                    BitmapFactory.decodeStream(stream, null, opts)?.let { bmp ->
                        result[rawPath] = bmp.asImageBitmap()
                    }
                }
            } catch (_: Exception) {
                // Skip missing or unsupported asset gracefully; UI falls back to muscle icon
            }
        }
        result
    }

    actual fun deleteTempSharePhoto(localPath: String?) {
        if (localPath.isNullOrBlank()) return
        try {
            val file = File(localPath.removePrefix("file://"))
            if (file.exists() && file.parentFile?.name == SHARE_CARD_PHOTO_DIR) {
                file.delete()
            }
        } catch (_: Exception) {
        }
    }
}

private fun toSoftwareArgb8888(bitmap: Bitmap): Bitmap {
    return if (bitmap.config == Bitmap.Config.ARGB_8888) {
        bitmap
    } else {
        bitmap.copy(Bitmap.Config.ARGB_8888, false)
    }
}

private fun applyExifOrientationIfNeeded(path: String, bitmap: Bitmap): Bitmap {
    val orientation = try {
        ExifInterface(path).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
    } catch (_: Exception) {
        ExifInterface.ORIENTATION_NORMAL
    }
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        else -> return bitmap
    }
    val transformed = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    if (transformed !== bitmap) {
        bitmap.recycle()
    }
    return transformed
}
