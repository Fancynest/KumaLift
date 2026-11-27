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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.core.logging.KumaLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

private const val MAX_IMAGE_DIMENSION_PX = 1600
private const val JPEG_QUALITY = 90

@Composable
actual fun rememberAiMediaLaunchers(
    onImagesReady: (List<String>) -> Unit
): AiMediaLaunchers {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val latestOnImagesReady by rememberUpdatedState(onImagesReady)
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    var pendingMaxPick by remember { mutableStateOf(MAX_AI_CHAT_IMAGES) }

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
                    withContext(Dispatchers.Main) { latestOnImagesReady(listOf(savedPath)) }
                }
            }
        } else if (file != null) {
            try { file.delete() } catch (_: Exception) {}
        }
    }

    val getMultipleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        val capped = uris.take(pendingMaxPick.coerceIn(1, MAX_AI_CHAT_IMAGES))
        if (capped.isNotEmpty()) {
            scope.launch(Dispatchers.IO) {
                val savedPaths = capped.mapNotNull { uri ->
                    compressAndStoreBitmap(
                        context = context,
                        openStream = { context.contentResolver.openInputStream(uri) }
                    )
                }
                if (savedPaths.isNotEmpty()) {
                    withContext(Dispatchers.Main) { latestOnImagesReady(savedPaths) }
                }
            }
        }
    }

    val pickMultipleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(MAX_AI_CHAT_IMAGES)
    ) { uris: List<Uri> ->
        val capped = uris.take(pendingMaxPick.coerceIn(1, MAX_AI_CHAT_IMAGES))
        if (capped.isNotEmpty()) {
            scope.launch(Dispatchers.IO) {
                val savedPaths = capped.mapNotNull { uri ->
                    compressAndStoreBitmap(
                        context = context,
                        openStream = { context.contentResolver.openInputStream(uri) }
                    )
                }
                if (savedPaths.isNotEmpty()) {
                    withContext(Dispatchers.Main) { latestOnImagesReady(savedPaths) }
                }
            }
        }
    }

    val pickSingleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val savedPath = compressAndStoreBitmap(
                    context = context,
                    openStream = { context.contentResolver.openInputStream(uri) }
                )
                if (savedPath != null) {
                    withContext(Dispatchers.Main) { latestOnImagesReady(listOf(savedPath)) }
                }
            }
        }
    }

    return remember(context, cameraLauncher, pickSingleLauncher, pickMultipleLauncher, getMultipleLauncher) {
        createAiMediaLaunchers(
            context = context,
            onLaunchCamera = { uri -> cameraLauncher.launch(uri) },
            onLaunchSingle = { req -> pickSingleLauncher.launch(req) },
            onLaunchMultiple = { req -> pickMultipleLauncher.launch(req) },
            onLaunchFallback = { mime -> getMultipleLauncher.launch(mime) },
            onSetPendingCamera = { pendingCameraFile = it },
            onSetPendingMaxPick = { pendingMaxPick = it }
        )
    }
}

private fun createAiMediaLaunchers(
    context: Context,
    onLaunchCamera: (Uri) -> Unit,
    onLaunchSingle: (PickVisualMediaRequest) -> Unit,
    onLaunchMultiple: (PickVisualMediaRequest) -> Unit,
    onLaunchFallback: (String) -> Unit,
    onSetPendingCamera: (File?) -> Unit,
    onSetPendingMaxPick: (Int) -> Unit
): AiMediaLaunchers = AiMediaLaunchers(
    launchCamera = {
        try {
            val camDir = File(context.cacheDir, "ai_chat_camera").apply { mkdirs() }
            val tempFile = File(camDir, "cam_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg")
            onSetPendingCamera(tempFile)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            onLaunchCamera(uri)
        } catch (e: Exception) {
            KumaLogger.w("AiMedia", "Camera launch failed: ${e.message}")
            onSetPendingCamera(null)
        }
    },
    launchGallery = { maxSelectCount ->
        val remaining = maxSelectCount.coerceIn(1, MAX_AI_CHAT_IMAGES)
        onSetPendingMaxPick(remaining)
        try {
            if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(context)) {
                val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                if (remaining == 1) {
                    onLaunchSingle(request)
                } else {
                    onLaunchMultiple(request)
                }
            } else {
                onLaunchFallback("image/*")
            }
        } catch (_: Exception) {
            try {
                onLaunchFallback("image/*")
            } catch (e: Exception) {
                KumaLogger.w("AiMedia", "Gallery launch failed: ${e.message}")
            }
        }
    }
)

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
actual fun AiChatFullscreenImageViewer(
    imagePaths: List<String>,
    initialIndex: Int,
    closeContentDescription: String,
    pageIndicatorProvider: (current: Int, total: Int) -> String,
    onDismiss: () -> Unit
) {
    val safePaths = imagePaths.filter { it.isNotBlank() }
    if (safePaths.isEmpty()) return
    val context = LocalContext.current
    val safeStart = initialIndex.coerceIn(0, safePaths.lastIndex)
    val pagerState = rememberPagerState(initialPage = safeStart) { safePaths.size }
    var activeZoomScale by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(pagerState.currentPage) {
        activeZoomScale = 1f
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = activeZoomScale <= 1.05f,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val cleanPath = safePaths[page].removePrefix("file://")
                val file = remember(cleanPath) { File(cleanPath) }
                var scale by remember(page) { mutableFloatStateOf(1f) }
                var panOffset by remember(page) { mutableStateOf(Offset.Zero) }

                LaunchedEffect(pagerState.currentPage) {
                    if (pagerState.currentPage != page) {
                        scale = 1f
                        panOffset = Offset.Zero
                    }
                }

                LaunchedEffect(scale, pagerState.currentPage) {
                    if (pagerState.currentPage == page) {
                        activeZoomScale = scale
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(page) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (scale > 1.05f) {
                                        scale = 1f
                                        panOffset = Offset.Zero
                                    } else {
                                        scale = 2.5f
                                    }
                                }
                            )
                        }
                        .then(
                            if (scale > 1.05f) {
                                Modifier.pointerInput(scale) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val maxPanX = size.width * (scale - 1f) / 2f
                                        val maxPanY = size.height * (scale - 1f) / 2f
                                        panOffset = Offset(
                                            x = (panOffset.x + dragAmount.x).coerceIn(-maxPanX, maxPanX),
                                            y = (panOffset.y + dragAmount.y).coerceIn(-maxPanY, maxPanY)
                                        )
                                    }
                                }
                            } else {
                                Modifier
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (!file.exists() || file.length() <= 0L) {
                        AiImageFallbackBox(Modifier.size(160.dp))
                    } else {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(file)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = panOffset.x,
                                    translationY = panOffset.y
                                ),
                            loading = { AiImageFallbackBox(Modifier.fillMaxSize()) },
                            error = { AiImageFallbackBox(Modifier.fillMaxSize()) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = pageIndicatorProvider(pagerState.currentPage + 1, safePaths.size),
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = closeContentDescription,
                        tint = Color.White
                    )
                }
            }
        }
    }
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
        val dest = File(dir, "ai_img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg")
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
