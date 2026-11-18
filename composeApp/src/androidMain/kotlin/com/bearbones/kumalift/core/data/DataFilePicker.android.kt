package com.bearbones.kumalift.core.data

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.documentfile.provider.DocumentFile
import com.bearbones.kumalift.core.localization.AppStrings
import com.bearbones.kumalift.core.logging.KumaLogger

@Composable
actual fun rememberDataExportLauncher(
    strings: AppStrings,
    onExportResult: (Boolean, String) -> Unit
): (fileName: String, content: String) -> Unit {
    val context = LocalContext.current
    var pendingContent by remember { mutableStateOf<String?>(null) }

    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        val content = pendingContent
        if (uri != null && content != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(content.encodeToByteArray())
                }
                onExportResult(
                    true,
                    strings.backupFileSavedSuccess
                )
            } catch (e: Exception) {
                KumaLogger.e("DataExportLauncher", "Failed to write backup file: ${e.message}", e)
                onExportResult(
                    false,
                    strings.backupErrorWriteFailed
                )
            } finally {
                pendingContent = null
            }
        } else {
            pendingContent = null
            onExportResult(
                false,
                strings.backupFileSavingCancelled
            )
        }
    }

    return remember(context, strings) {
        { fileName: String, content: String ->
            pendingContent = content
            try {
                createDocLauncher.launch(fileName)
            } catch (e: Exception) {
                KumaLogger.e("DataExportLauncher", "Failed to launch document picker: ${e.message}", e)
                pendingContent = null
                onExportResult(
                    false,
                    strings.backupErrorFolderInaccessible
                )
            }
        }
    }
}

@Composable
actual fun rememberDataImportLauncher(
    onContentRead: (String?) -> Unit
): () -> Unit {
    val context = LocalContext.current

    val openDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                onContentRead(text)
            } catch (e: Exception) {
                onContentRead(null)
            }
        } else {
            onContentRead(null)
        }
    }

    return remember(context) {
        {
            try {
                openDocLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
            } catch (e: Exception) {
                onContentRead(null)
            }
        }
    }
}

@Composable
actual fun rememberFolderPickerLauncher(
    onFolderPicked: (folderUri: String?, folderDisplayName: String?) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val treeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
                val doc = DocumentFile.fromTreeUri(context, uri)
                val displayName = doc?.name ?: uri.lastPathSegment ?: uri.toString()
                onFolderPicked(uri.toString(), displayName)
            } catch (e: Exception) {
                onFolderPicked(null, null)
            }
        } else {
            onFolderPicked(null, null)
        }
    }

    return remember(treeLauncher) {
        {
            try {
                treeLauncher.launch(null)
            } catch (e: Exception) {
                onFolderPicked(null, null)
            }
        }
    }
}

@Composable
actual fun rememberDataExportBytesLauncher(
    strings: AppStrings,
    onExportResult: (Boolean, String) -> Unit
): (fileName: String, mimeType: String, bytes: ByteArray) -> Unit {
    val context = LocalContext.current
    var pendingBytes by remember { mutableStateOf<ByteArray?>(null) }

    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        val bytes = pendingBytes
        if (uri != null && bytes != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(bytes)
                }
                onExportResult(true, strings.backupFileSavedSuccess)
            } catch (e: Exception) {
                KumaLogger.e("DataExportBytesLauncher", "Failed to write backup file: ${e.message}", e)
                onExportResult(false, strings.backupErrorWriteFailed)
            } finally {
                pendingBytes = null
            }
        } else {
            pendingBytes = null
            onExportResult(false, strings.backupFileSavingCancelled)
        }
    }

    return remember(context, strings) {
        { fileName: String, mimeType: String, bytes: ByteArray ->
            pendingBytes = bytes
            try {
                createDocLauncher.launch(fileName)
            } catch (e: Exception) {
                KumaLogger.e("DataExportBytesLauncher", "Failed to launch document picker: ${e.message}", e)
                pendingBytes = null
                onExportResult(false, strings.backupErrorFolderInaccessible)
            }
        }
    }
}

@Composable
actual fun rememberDataImportBytesLauncher(
    onBytesRead: (ByteArray?) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val openDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                onBytesRead(bytes)
            } catch (e: Exception) {
                onBytesRead(null)
            }
        } else {
            onBytesRead(null)
        }
    }

    return remember(context) {
        {
            try {
                openDocLauncher.launch(arrayOf("*/*", "application/octet-stream", "application/json"))
            } catch (e: Exception) {
                onBytesRead(null)
            }
        }
    }
}

@Composable
actual fun rememberAudioPickerLauncher(
    onAudioPicked: (fileName: String?, localFilePath: String?, errorMessage: String?) -> Unit
): () -> Unit {
    val context = LocalContext.current

    val openDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }

        try {
            val docFile = DocumentFile.fromSingleUri(context, uri)
            val displayName = docFile?.name ?: "custom_sound.mp3"
            val extension = displayName.substringAfterLast('.', "").lowercase()

            val allowedExtensions = setOf("mp3", "wav", "ogg", "opus", "m4a", "aac", "flac")
            if (extension.isNotEmpty() && !allowedExtensions.contains(extension)) {
                onAudioPicked(null, null, "Format file .$extension tidak didukung. Harap gunakan MP3, WAV, atau OGG.")
                return@rememberLauncherForActivityResult
            }

            val targetExt = if (extension.isNotEmpty()) extension else "mp3"
            val destDir = File(context.filesDir, "custom_sounds")
            destDir.mkdirs()
            val destFile = File(destDir, "custom_rest_timer_sound.$targetExt")

            // Copy stream
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: run {
                onAudioPicked(null, null, "Gagal membaca file dari penyimpanan.")
                return@rememberLauncherForActivityResult
            }

            // Validate audio can be prepared with MediaPlayer
            val testPlayer = MediaPlayer()
            var isValid = false
            try {
                testPlayer.setDataSource(destFile.absolutePath)
                testPlayer.prepare()
                isValid = testPlayer.duration > 0
            } catch (t: Throwable) {
                KumaLogger.e("AudioPicker", "Audio file validation failed: ${t.message}", t)
                isValid = false
            } finally {
                try { testPlayer.release() } catch (_: Throwable) {}
            }

            if (!isValid) {
                try { destFile.delete() } catch (_: Throwable) {}
                onAudioPicked(null, null, "File audio rusak atau tidak dapat diputar.")
                return@rememberLauncherForActivityResult
            }

            onAudioPicked(displayName, destFile.absolutePath, null)
        } catch (e: Exception) {
            KumaLogger.e("AudioPicker", "Exception copying audio file: ${e.message}", e)
            onAudioPicked(null, null, "Gagal mengunggah file audio: ${e.message}")
        }
    }

    return remember(context) {
        {
            try {
                openDocLauncher.launch(
                    arrayOf(
                        "audio/*",
                        "audio/mpeg",
                        "audio/mp3",
                        "audio/wav",
                        "audio/x-wav",
                        "audio/ogg",
                        "audio/opus",
                        "audio/mp4",
                        "audio/m4a"
                    )
                )
            } catch (e: Exception) {
                onAudioPicked(null, null, "Gagal membuka pengelola file: ${e.message}")
            }
        }
    }
}

actual fun deleteCustomAudioFile(filePath: String?) {
    if (filePath.isNullOrBlank()) return
    try {
        val file = File(filePath)
        if (file.exists()) {
            file.delete()
        }
    } catch (_: Throwable) {}
}
