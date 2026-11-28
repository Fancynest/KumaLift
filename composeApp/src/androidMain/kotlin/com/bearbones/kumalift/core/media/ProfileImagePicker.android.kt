package com.bearbones.kumalift.core.media

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val MAX_AVATAR_BYTES = 25L * 1024L * 1024L

@Composable
actual fun rememberProfileImagePicker(
    onImagePicked: (String) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latestOnImagePicked by rememberUpdatedState(onImagePicked)

    val handlePickedUri: (Uri?) -> Unit = { uri ->
        if (uri != null) {
            scope.launch {
                val path = withContext(Dispatchers.IO) {
                    saveAvatarFromUri(context, uri)
                }
                if (path != null) {
                    latestOnImagePicked(path)
                }
            }
        }
    }

    val getContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        handlePickedUri(uri)
    }

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        handlePickedUri(uri)
    }

    return remember(context) {
        {
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
                } catch (_: Exception) {
                }
            }
        }
    }
}

private fun saveAvatarFromUri(
    context: Context,
    uri: Uri
): String? {
    var destFile: File? = null
    return try {
        val timestamp = System.currentTimeMillis()
        val targetFile = File(context.filesDir, "profile_avatar_$timestamp.jpg")
        destFile = targetFile
        var totalBytes = 0L
        val copied = context.contentResolver.openInputStream(uri)?.use { input ->
            targetFile.outputStream().use { output ->
                val buffer = ByteArray(16 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    totalBytes += read
                    if (totalBytes > MAX_AVATAR_BYTES) {
                        throw IllegalStateException("Avatar exceeds max size")
                    }
                    output.write(buffer, 0, read)
                }
                output.flush()
            }
            true
        } ?: false
        if (!copied || totalBytes <= 0L) {
            targetFile.delete()
            return null
        }
        // Clean up older avatar files in private app storage
        context.filesDir.listFiles { _, name ->
            name.startsWith("profile_avatar_") && name != targetFile.name
        }?.forEach { oldFile ->
            try { oldFile.delete() } catch (_: Exception) {}
        }

        targetFile.absolutePath
    } catch (_: Exception) {
        try { destFile?.delete() } catch (_: Exception) {}
        null
    }
}
