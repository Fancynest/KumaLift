package com.bearbones.kumalift.core.ai

import android.content.Context
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
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.core.logging.KumaLogger
import com.bearbones.kumalift.core.media.compressAndStoreJpeg
import com.bearbones.kumalift.core.media.rememberPermissionAwareCameraCapture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val AI_CHAT_OUTPUT_DIR = "ai_chat_images"
private const val AI_CHAT_CACHE_DIR = "ai_chat_camera"
private const val AI_CHAT_PREFIX = "ai_img_"

@Composable
actual fun rememberAiMediaLaunchers(
    onImagesReady: (List<String>) -> Unit
): AiMediaLaunchers {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val latestOnImagesReady by rememberUpdatedState(onImagesReady)
    var pendingMaxPick by remember { mutableStateOf(MAX_AI_CHAT_IMAGES) }

    val launchCamera = rememberPermissionAwareCameraCapture(
        cacheSubDir = AI_CHAT_CACHE_DIR,
        outputSubDir = AI_CHAT_OUTPUT_DIR,
        filePrefix = AI_CHAT_PREFIX,
        onSaved = { savedPath -> latestOnImagesReady(listOf(savedPath)) }
    )

    val getMultipleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        val capped = uris.take(pendingMaxPick.coerceIn(1, MAX_AI_CHAT_IMAGES))
        if (capped.isNotEmpty()) {
            scope.launch(Dispatchers.IO) {
                val savedPaths = capped.mapNotNull { uri ->
                    compressAndStoreJpeg(
                        context = context,
                        outputSubDir = AI_CHAT_OUTPUT_DIR,
                        filePrefix = AI_CHAT_PREFIX,
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
                    compressAndStoreJpeg(
                        context = context,
                        outputSubDir = AI_CHAT_OUTPUT_DIR,
                        filePrefix = AI_CHAT_PREFIX,
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
                val savedPath = compressAndStoreJpeg(
                    context = context,
                    outputSubDir = AI_CHAT_OUTPUT_DIR,
                    filePrefix = AI_CHAT_PREFIX,
                    openStream = { context.contentResolver.openInputStream(uri) }
                )
                if (savedPath != null) {
                    withContext(Dispatchers.Main) { latestOnImagesReady(listOf(savedPath)) }
                }
            }
        }
    }

    return remember(context, launchCamera, pickSingleLauncher, pickMultipleLauncher, getMultipleLauncher) {
        buildAiMediaLaunchers(
            context = context,
            launchCamera = launchCamera,
            onSetMaxPick = { pendingMaxPick = it },
            onLaunchSingle = { req -> pickSingleLauncher.launch(req) },
            onLaunchMultiple = { req -> pickMultipleLauncher.launch(req) },
            onLaunchFallback = { mime -> getMultipleLauncher.launch(mime) }
        )
    }
}

private fun buildAiMediaLaunchers(
    context: Context,
    launchCamera: () -> Unit,
    onSetMaxPick: (Int) -> Unit,
    onLaunchSingle: (PickVisualMediaRequest) -> Unit,
    onLaunchMultiple: (PickVisualMediaRequest) -> Unit,
    onLaunchFallback: (String) -> Unit
): AiMediaLaunchers = AiMediaLaunchers(
    launchCamera = launchCamera,
    launchGallery = { maxSelectCount ->
        val remaining = maxSelectCount.coerceIn(1, MAX_AI_CHAT_IMAGES)
        onSetMaxPick(remaining)
        com.bearbones.kumalift.core.media.launchAndroidGalleryPicker(
            context = context,
            maxSelectCount = remaining,
            onLaunchSingle = onLaunchSingle,
            onLaunchMultiple = onLaunchMultiple,
            onLaunchFallback = onLaunchFallback
        )
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

actual suspend fun saveAiGeneratedImageBase64(base64Data: String, mimeType: String): String? =
    withContext(Dispatchers.IO) {
        val context = com.bearbones.kumalift.core.timer.AndroidAppProvider.appContext ?: return@withContext null
        val bytes = try {
            Base64.decode(base64Data, Base64.DEFAULT)
        } catch (e: Exception) {
            KumaLogger.w("AiMedia", "Invalid generated image base64: ${e.message}")
            return@withContext null
        }
        if (bytes.isEmpty()) return@withContext null
        compressAndStoreJpeg(
            context = context,
            outputSubDir = AI_CHAT_OUTPUT_DIR,
            filePrefix = AI_CHAT_PREFIX,
            openStream = { bytes.inputStream() }
        )
    }

private fun resolveAiImageModel(rawPath: String): Pair<Any?, Boolean> {
    val trimmed = rawPath.trim()
    if (trimmed.isBlank()) return null to false
    if (trimmed.startsWith("https://", ignoreCase = true) || trimmed.startsWith("http://", ignoreCase = true)) {
        return trimmed to false
    }
    if (trimmed.startsWith("file:///android_asset/", ignoreCase = true)) {
        return trimmed to true
    }
    val cleanPath = trimmed.removePrefix("file://")
    val file = File(cleanPath)
    return if (file.exists() && file.length() > 0L) (file to false) else (null to false)
}

@Composable
actual fun AiChatImageThumbnail(
    imagePath: String,
    contentDescription: String?,
    modifier: Modifier
) {
    val context = LocalContext.current
    val (modelData, isAsset) = remember(imagePath) { resolveAiImageModel(imagePath) }
    val shape = RoundedCornerShape(12.dp)

    if (modelData == null) {
        AiImageFallbackBox(modifier = modifier.clip(shape))
        return
    }

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data(modelData)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        contentScale = if (isAsset) ContentScale.Fit else ContentScale.Crop,
        modifier = modifier
            .clip(shape)
            .then(if (isAsset) Modifier.background(Color.White) else Modifier),
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
                val rawPath = safePaths[page]
                val (modelData, isAsset) = remember(rawPath) { resolveAiImageModel(rawPath) }
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
                        .then(if (isAsset) Modifier.background(Color.White) else Modifier)
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
                    if (modelData == null) {
                        AiImageFallbackBox(Modifier.size(160.dp))
                    } else {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(modelData)
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

actual fun deleteAiChatImageFile(localPath: String?) {
    if (localPath.isNullOrBlank()) return
    try {
        val file = File(localPath)
        if (file.isFile && file.parentFile?.name == AI_CHAT_OUTPUT_DIR) {
            file.delete()
        }
    } catch (_: Exception) {
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
