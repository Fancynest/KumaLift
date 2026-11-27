package com.bearbones.kumalift.core.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.logging.KumaLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private const val MAX_IMAGE_DIMENSION_PX = 1600
private const val JPEG_QUALITY = 90

@Composable
actual fun rememberAiMediaLaunchers(
    onImageReady: (String) -> Unit
): AiMediaLaunchers {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val file = pendingCameraFile
        pendingCameraFile = null
        if (success && file != null && file.exists() && file.length() > 0L) {
            scope.launch(Dispatchers.IO) {
                val savedPath = compressAndStoreBitmap(
                    context = context,
                    openStream = { file.inputStream() }
                )
                try { file.delete() } catch (_: Exception) {}
                if (savedPath != null) {
                    withContext(Dispatchers.Main) { onImageReady(savedPath) }
                }
            }
        } else if (file != null) {
            try { file.delete() } catch (_: Exception) {}
        }
    }

    val getContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val savedPath = compressAndStoreBitmap(
                    context = context,
                    openStream = { context.contentResolver.openInputStream(uri) }
                )
                if (savedPath != null) {
                    withContext(Dispatchers.Main) { onImageReady(savedPath) }
                }
            }
        }
    }

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val savedPath = compressAndStoreBitmap(
                    context = context,
                    openStream = { context.contentResolver.openInputStream(uri) }
                )
                if (savedPath != null) {
                    withContext(Dispatchers.Main) { onImageReady(savedPath) }
                }
            }
        }
    }

    return remember(context) {
        AiMediaLaunchers(
            launchCamera = {
                try {
                    val camDir = File(context.cacheDir, "ai_chat_camera").apply { mkdirs() }
                    val tempFile = File(camDir, "cam_${System.currentTimeMillis()}.jpg")
                    pendingCameraFile = tempFile
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        tempFile
                    )
                    cameraLauncher.launch(uri)
                } catch (e: Exception) {
                    KumaLogger.w("AiMedia", "Camera launch failed: ${e.message}")
                    pendingCameraFile = null
                }
            },
            launchGallery = {
                try {
                    if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(context)) {
                        pickMediaLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    } else {
                        getContentLauncher.launch("image/*")
                    }
                } catch (_: Exception) {
                    try {
                        getContentLauncher.launch("image/*")
                    } catch (e: Exception) {
                        KumaLogger.w("AiMedia", "Gallery launch failed: ${e.message}")
                    }
                }
            }
        )
    }
}

actual suspend fun loadCompressedImageBase64(localPath: String): Pair<String, String>? =
    withContext(Dispatchers.IO) {
        try {
            val file = File(localPath.removePrefix("file://"))
            if (!file.exists() || file.length() <= 0L) return@withContext null
            val bytes = file.readBytes()
            "image/jpeg" to Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            KumaLogger.w("AiMedia", "Failed reading image base64: ${e.message}")
            null
        }
    }

@Composable
actual fun AiChatImageThumbnail(
    imagePath: String,
    contentDescription: String?,
    modifier: Modifier
) {
    val context = LocalContext.current
    val cleanPath = imagePath.removePrefix("file://")
    val file = remember(cleanPath) { File(cleanPath) }
    val shape = RoundedCornerShape(12.dp)

    if (!file.exists() || file.length() <= 0L) {
        AiImageFallbackBox(modifier = modifier.clip(shape))
        return
    }

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data(file)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(shape),
        loading = { AiImageFallbackBox(Modifier.fillMaxSize()) },
        error = { AiImageFallbackBox(Modifier.fillMaxSize()) }
    )
}

@Composable
private fun AiImageFallbackBox(modifier: Modifier) {
    Box(
        modifier = modifier.background(KumaColors.SurfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.BrokenImage,
            contentDescription = null,
            tint = KumaColors.TextSecondary
        )
    }
}

private fun compressAndStoreBitmap(
    context: Context,
    openStream: () -> java.io.InputStream?
): String? {
    return try {
        val rawBytes = openStream()?.use { it.readBytes() } ?: return null
        if (rawBytes.isEmpty()) return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, bounds)
        val srcW = bounds.outWidth
        val srcH = bounds.outHeight
        if (srcW <= 0 || srcH <= 0) return null

        var sampleSize = 1
        while ((srcW / sampleSize) > MAX_IMAGE_DIMENSION_PX * 2 || (srcH / sampleSize) > MAX_IMAGE_DIMENSION_PX * 2) {
            sampleSize *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOpts) ?: return null

        val orientation = try {
            rawBytes.inputStream().use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
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
        val finalBitmap = if (maxSide > MAX_IMAGE_DIMENSION_PX) {
            val scale = MAX_IMAGE_DIMENSION_PX.toFloat() / maxSide.toFloat()
            val targetW = (oriented.width * scale).toInt().coerceAtLeast(1)
            val targetH = (oriented.height * scale).toInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(oriented, targetW, targetH, true)
            if (scaled != oriented) oriented.recycle()
            scaled
        } else {
            oriented
        }

        val dir = File(context.filesDir, "ai_chat_images").apply { mkdirs() }
        val dest = File(dir, "ai_img_${System.currentTimeMillis()}.jpg")
        FileOutputStream(dest).use { out ->
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            out.flush()
        }
        finalBitmap.recycle()
        dest.absolutePath
    } catch (e: Exception) {
        KumaLogger.w("AiMedia", "Failed compressing AI chat image: ${e.message}")
        null
    }
}
