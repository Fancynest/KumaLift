package com.bearbones.kumalift.core.media

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.File

@Composable
actual fun rememberProfileImagePicker(
    onImagePicked: (String) -> Unit
): () -> Unit {
    val context = LocalContext.current

    val getContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            saveAvatarFromUri(context, uri, onImagePicked)
        }
    }

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            saveAvatarFromUri(context, uri, onImagePicked)
        }
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
            } catch (e: Exception) {
                try {
                    getContentLauncher.launch("image/*")
                } catch (fallbackEx: Exception) {
                    fallbackEx.printStackTrace()
                }
            }
        }
    }
}

private fun saveAvatarFromUri(
    context: Context,
    uri: Uri,
    onImagePicked: (String) -> Unit
) {
    var destFile: File? = null
    try {
        val timestamp = System.currentTimeMillis()
        val targetFile = File(context.filesDir, "profile_avatar_$timestamp.jpg")
        destFile = targetFile
        val copied = context.contentResolver.openInputStream(uri)?.use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
            true
        } ?: false
        if (!copied) {
            targetFile.delete()
            return
        }
        // Clean up older avatar files in private app storage
        context.filesDir.listFiles { _, name ->
            name.startsWith("profile_avatar_") && name != targetFile.name
        }?.forEach { oldFile ->
            try { oldFile.delete() } catch (_: Exception) {}
        }

        onImagePicked(targetFile.absolutePath)
    } catch (e: Exception) {
        try { destFile?.delete() } catch (_: Exception) {}
        e.printStackTrace()
    }
}
