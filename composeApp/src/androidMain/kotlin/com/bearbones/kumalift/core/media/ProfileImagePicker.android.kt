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
    try {
        val timestamp = System.currentTimeMillis()
        val destFile = File(context.filesDir, "profile_avatar_$timestamp.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        // Clean up older avatar files in private app storage
        context.filesDir.listFiles { _, name ->
            name.startsWith("profile_avatar_") && name != destFile.name
        }?.forEach { oldFile ->
            try { oldFile.delete() } catch (_: Exception) {}
        }

        onImagePicked(destFile.absolutePath)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
