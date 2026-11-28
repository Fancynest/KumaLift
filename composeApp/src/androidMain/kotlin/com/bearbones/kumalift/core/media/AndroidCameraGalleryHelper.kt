package com.bearbones.kumalift.core.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bearbones.kumalift.core.logging.KumaLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

internal const val DEFAULT_MEDIA_MAX_DIMENSION_PX = 1600
internal const val DEFAULT_MEDIA_JPEG_QUALITY = 90

@Composable
internal fun rememberPermissionAwareCameraCapture(
    cacheSubDir: String,
    outputSubDir: String,
    filePrefix: String,
    onSaved: (String) -> Unit,
    onPermissionDenied: () -> Unit = {}
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latestOnSaved by rememberUpdatedState(onSaved)
    val latestOnDenied by rememberUpdatedState(onPermissionDenied)
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val tempFile = pendingCameraFile
        pendingCameraFile = null
        if (success && tempFile != null && tempFile.exists() && tempFile.length() > 0L) {
            scope.launch(Dispatchers.IO) {
                val savedPath = compressAndStoreJpeg(
                    context = context,
                    outputSubDir = outputSubDir,
                    filePrefix = filePrefix,
                    openStream = { tempFile.inputStream() }
                )
                try { tempFile.delete() } catch (_: Exception) {}
                if (savedPath != null) {
                    withContext(Dispatchers.Main) { latestOnSaved(savedPath) }
                }
            }
        } else if (tempFile != null) {
            try { tempFile.delete() } catch (_: Exception) {}
        }
    }

    fun launchTakePictureNow() {
        var tempFile: File? = null
        try {
            val camDir = File(context.cacheDir, cacheSubDir).apply { mkdirs() }
            val created = File(camDir, "${filePrefix}cam_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg")
            tempFile = created
            pendingCameraFile = created
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                created
            )
            takePictureLauncher.launch(uri)
        } catch (se: SecurityException) {
            KumaLogger.w("KumaMedia", "Camera permission blocked: ${se.message}")
            pendingCameraFile = null
            try { tempFile?.delete() } catch (_: Exception) {}
            latestOnDenied()
        } catch (e: Exception) {
            KumaLogger.w("KumaMedia", "Camera launch failed: ${e.message}")
            pendingCameraFile = null
            try { tempFile?.delete() } catch (_: Exception) {}
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchTakePictureNow()
        } else {
            latestOnDenied()
        }
    }

    return remember(context) {
        {
            val hasCameraPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
            if (hasCameraPermission) {
                launchTakePictureNow()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
}

internal fun launchAndroidGalleryPicker(
    context: Context,
    maxSelectCount: Int = 1,
    onLaunchSingle: (androidx.activity.result.PickVisualMediaRequest) -> Unit,
    onLaunchMultiple: ((androidx.activity.result.PickVisualMediaRequest) -> Unit)? = null,
    onLaunchFallback: (String) -> Unit
) {
    try {
        if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(context)) {
            val request = androidx.activity.result.PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageOnly
            )
            if (maxSelectCount > 1 && onLaunchMultiple != null) {
                onLaunchMultiple(request)
            } else {
                onLaunchSingle(request)
            }
        } else {
            onLaunchFallback("image/*")
        }
    } catch (_: Exception) {
        try {
            onLaunchFallback("image/*")
        } catch (e: Exception) {
            KumaLogger.w("KumaMedia", "Gallery launch failed: ${e.message}")
        }
    }
}

internal fun compressAndStoreJpeg(
    context: Context,
    outputSubDir: String,
    filePrefix: String,
    maxDimensionPx: Int = DEFAULT_MEDIA_MAX_DIMENSION_PX,
    jpegQuality: Int = DEFAULT_MEDIA_JPEG_QUALITY,
    openStream: () -> InputStream?
): String? {
    var tmpInput: File? = null
    var destFile: File? = null
    return try {
        val maxInputBytes = 32L * 1024L * 1024L
        val cacheDir = context.cacheDir.apply { mkdirs() }
        val stagedFile = File.createTempFile("kuma_media_", ".tmp", cacheDir)
        tmpInput = stagedFile

        var totalBytes = 0L
        val copied = openStream()?.use { input ->
            FileOutputStream(stagedFile).use { out ->
                val buf = ByteArray(16 * 1024)
                while (true) {
                    val read = input.read(buf)
                    if (read < 0) break
                    totalBytes += read
                    if (totalBytes > maxInputBytes) {
                        throw IllegalStateException("Image exceeds maximum allowed size")
                    }
                    out.write(buf, 0, read)
                }
                out.flush()
            }
            true
        } ?: false
        if (!copied || totalBytes <= 0L) return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(stagedFile.absolutePath, bounds)
        val srcW = bounds.outWidth
        val srcH = bounds.outHeight
        if (srcW <= 0 || srcH <= 0) return null

        var sampleSize = 1
        while ((srcW / sampleSize) > maxDimensionPx * 2 || (srcH / sampleSize) > maxDimensionPx * 2) {
            sampleSize *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = BitmapFactory.decodeFile(stagedFile.absolutePath, decodeOpts) ?: return null

        val orientation = try {
            ExifInterface(stagedFile.absolutePath).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
        val rotationDegrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

        val oriented = if (rotationDegrees != 0f) {
            val matrix = Matrix().apply { postRotate(rotationDegrees) }
            val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            if (rotated != decoded) decoded.recycle()
            rotated
        } else {
            decoded
        }

        val maxSide = maxOf(oriented.width, oriented.height)
        val finalBitmap = if (maxSide > maxDimensionPx) {
            val scale = maxDimensionPx.toFloat() / maxSide.toFloat()
            val targetW = (oriented.width * scale).toInt().coerceAtLeast(1)
            val targetH = (oriented.height * scale).toInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(oriented, targetW, targetH, true)
            if (scaled != oriented) oriented.recycle()
            scaled
        } else {
            oriented
        }

        val dir = File(context.filesDir, outputSubDir).apply { mkdirs() }
        val dest = File(dir, "$filePrefix${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg")
        destFile = dest
        FileOutputStream(dest).use { out ->
            if (!finalBitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, out)) {
                throw IllegalStateException("Bitmap compression failed")
            }
            out.flush()
        }
        finalBitmap.recycle()
        destFile = null
        dest.absolutePath
    } catch (e: Exception) {
        try { destFile?.delete() } catch (_: Exception) {}
        KumaLogger.w("KumaMedia", "Failed compressing image: ${e.message}")
        null
    } finally {
        try { tmpInput?.delete() } catch (_: Exception) {}
    }
}
