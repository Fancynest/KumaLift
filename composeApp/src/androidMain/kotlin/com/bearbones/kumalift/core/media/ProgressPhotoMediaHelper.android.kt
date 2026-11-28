package com.bearbones.kumalift.core.media

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.logging.KumaLogger
import com.bearbones.kumalift.core.timer.AndroidAppProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val PROGRESS_PHOTO_OUTPUT_DIR = "progress_photos"
private const val PROGRESS_PHOTO_CACHE_DIR = "kuma_camera_capture"
private const val PROGRESS_PHOTO_PREFIX = "progress_"

@Composable
actual fun rememberProgressPhotoMediaLaunchers(
    onPhotoReady: (String) -> Unit,
    onCameraPermissionDenied: () -> Unit
): ProgressPhotoMediaLaunchers {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latestOnPhotoReady by rememberUpdatedState(onPhotoReady)

    val launchCamera = rememberPermissionAwareCameraCapture(
        cacheSubDir = PROGRESS_PHOTO_CACHE_DIR,
        outputSubDir = PROGRESS_PHOTO_OUTPUT_DIR,
        filePrefix = PROGRESS_PHOTO_PREFIX,
        onSaved = { path -> latestOnPhotoReady(path) },
        onPermissionDenied = onCameraPermissionDenied
    )

    val handlePickedUri: (Uri?) -> Unit = { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val savedPath = compressAndStoreJpeg(
                    context = context,
                    outputSubDir = PROGRESS_PHOTO_OUTPUT_DIR,
                    filePrefix = PROGRESS_PHOTO_PREFIX,
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
        buildProgressPhotoMediaLaunchers(
            context = context,
            launchCamera = launchCamera,
            onLaunchSingle = { req -> pickSingleLauncher.launch(req) },
            onLaunchFallback = { mime -> getContentLauncher.launch(mime) }
        )
    }
}

private fun buildProgressPhotoMediaLaunchers(
    context: android.content.Context,
    launchCamera: () -> Unit,
    onLaunchSingle: (PickVisualMediaRequest) -> Unit,
    onLaunchFallback: (String) -> Unit
): ProgressPhotoMediaLaunchers = ProgressPhotoMediaLaunchers(
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

@Composable
actual fun ProgressPhotoImage(
    imagePath: String,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale
) {
    val context = LocalContext.current
    val cleanPath = imagePath.removePrefix("file://")
    val file = remember(cleanPath) { File(cleanPath) }

    if (!file.exists() || file.length() <= 0L) {
        Box(
            modifier = modifier.background(KumaColors.SurfaceDim),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.BrokenImage,
                contentDescription = contentDescription,
                tint = KumaColors.TextMuted
            )
        }
        return
    }

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data(file)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
        error = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(KumaColors.SurfaceDim),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BrokenImage,
                    contentDescription = contentDescription,
                    tint = KumaColors.TextMuted
                )
            }
        }
    )
}

actual fun deleteProgressPhotoFile(localPath: String?) {
    if (localPath.isNullOrBlank()) return
    try {
        val file = File(localPath.removePrefix("file://"))
        if (file.isFile && file.parentFile?.name == PROGRESS_PHOTO_OUTPUT_DIR) {
            file.delete()
        }
    } catch (e: Exception) {
        KumaLogger.w("ProgressPhoto", "Failed deleting local photo: ${e.message}")
    }
}

actual fun shareProgressPhotoWithSummary(
    localPath: String?,
    summaryText: String,
    chooserTitle: String
): Boolean {
    val context = AndroidAppProvider.appContext ?: return false
    return try {
        val cleanPath = localPath?.removePrefix("file://").orEmpty()
        val file = if (cleanPath.isNotBlank()) File(cleanPath) else null
        val intent = if (file != null && file.exists()) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                if (summaryText.isNotBlank()) {
                    putExtra(Intent.EXTRA_TEXT, summaryText)
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, summaryText)
            }
        }
        val chooser = Intent.createChooser(intent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
        true
    } catch (e: Exception) {
        KumaLogger.w("ProgressPhoto", "Share failed: ${e.message}")
        false
    }
}

actual fun sharePlainTextContent(
    text: String,
    chooserTitle: String
): Boolean {
    val context = AndroidAppProvider.appContext ?: return false
    return try {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(sendIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
        true
    } catch (e: Exception) {
        KumaLogger.w("Share", "Plain text share failed: ${e.message}")
        false
    }
}

actual fun openAppPermissionSettings(): Boolean {
    val context = AndroidAppProvider.appContext ?: return false
    return try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    } catch (e: Exception) {
        KumaLogger.w("Permissions", "Cannot open app settings: ${e.message}")
        false
    }
}
