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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.documentfile.provider.DocumentFile
import com.bearbones.kumalift.core.localization.AppStrings
import com.bearbones.kumalift.core.logging.KumaLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

private const val MAX_IMPORT_FILE_BYTES = 50L * 1024L * 1024L
private const val MAX_CUSTOM_AUDIO_BYTES = 20L * 1024L * 1024L

private fun readBoundedBytes(stream: java.io.InputStream, maxBytes: Long): ByteArray {
    val out = ByteArrayOutputStream()
    val buf = ByteArray(16 * 1024)
    var total = 0L
    while (true) {
        val n = stream.read(buf)
        if (n < 0) break
        total += n
        if (total > maxBytes) {
            throw IllegalStateException("File exceeds maximum allowed size")
        }
        out.write(buf, 0, n)
    }
    return out.toByteArray()
}

@Composable
actual fun rememberDataExportLauncher(
    strings: AppStrings,
    onExportResult: (Boolean, String) -> Unit
): (fileName: String, content: String) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latestOnExportResult by rememberUpdatedState(onExportResult)
    val latestStrings by rememberUpdatedState(strings)
    var pendingContent by remember { mutableStateOf<String?>(null) }

    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        val content = pendingContent
        pendingContent = null
        if (uri != null && content != null) {
            scope.launch {
                val (ok, msg) = withContext(Dispatchers.IO) {
                    try {
                        val stream = context.contentResolver.openOutputStream(uri)
                            ?: throw java.io.IOException("Cannot open output stream")
                        stream.use { it.write(content.encodeToByteArray()) }
                        true to latestStrings.backupFileSavedSuccess
                    } catch (e: Exception) {
                        KumaLogger.e("DataExportLauncher", "Failed to write backup file: ${e.message}", e)
                        false to latestStrings.backupErrorWriteFailed
                    }
                }
                latestOnExportResult(ok, msg)
            }
        } else {
            latestOnExportResult(false, latestStrings.backupFileSavingCancelled)
        }
    }

    return remember(context) {
        { fileName: String, content: String ->
            pendingContent = content
            try {
                createDocLauncher.launch(fileName)
            } catch (e: Exception) {
                KumaLogger.e("DataExportLauncher", "Failed to launch document picker: ${e.message}", e)
                pendingContent = null
                latestOnExportResult(false, latestStrings.backupErrorFolderInaccessible)
            }
        }
    }
}

@Composable
actual fun rememberDataImportLauncher(
    onContentRead: (String?) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latestOnContentRead by rememberUpdatedState(onContentRead)

    val openDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val text = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            readBoundedBytes(stream, MAX_IMPORT_FILE_BYTES).decodeToString()
                        }
                    } catch (e: Exception) {
                        KumaLogger.w("DataImportLauncher", "Failed to read import file: ${e.message}")
                        null
                    }
                }
                latestOnContentRead(text)
            }
        } else {
            latestOnContentRead(null)
        }
    }

    return remember(context) {
        {
            try {
                openDocLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
            } catch (_: Exception) {
                latestOnContentRead(null)
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
    val scope = rememberCoroutineScope()
    val latestOnExportResult by rememberUpdatedState(onExportResult)
    val latestStrings by rememberUpdatedState(strings)
    var pendingBytes by remember { mutableStateOf<ByteArray?>(null) }

    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        val bytes = pendingBytes
        pendingBytes = null
        if (uri != null && bytes != null) {
            scope.launch {
                val (ok, msg) = withContext(Dispatchers.IO) {
                    try {
                        val stream = context.contentResolver.openOutputStream(uri)
                            ?: throw java.io.IOException("Cannot open output stream")
                        stream.use { it.write(bytes) }
                        true to latestStrings.backupFileSavedSuccess
                    } catch (e: Exception) {
                        KumaLogger.e("DataExportBytesLauncher", "Failed to write backup file: ${e.message}", e)
                        false to latestStrings.backupErrorWriteFailed
                    }
                }
                latestOnExportResult(ok, msg)
            }
        } else {
            latestOnExportResult(false, latestStrings.backupFileSavingCancelled)
        }
    }

    return remember(context) {
        { fileName: String, mimeType: String, bytes: ByteArray ->
            pendingBytes = bytes
            try {
                createDocLauncher.launch(fileName)
            } catch (e: Exception) {
                KumaLogger.e("DataExportBytesLauncher", "Failed to launch document picker: ${e.message}", e)
                pendingBytes = null
                latestOnExportResult(false, latestStrings.backupErrorFolderInaccessible)
            }
        }
    }
}

@Composable
actual fun rememberDataImportBytesLauncher(
    onBytesRead: (ByteArray?) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latestOnBytesRead by rememberUpdatedState(onBytesRead)

    val openDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val bytes = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            readBoundedBytes(stream, MAX_IMPORT_FILE_BYTES)
                        }
                    } catch (e: Exception) {
                        KumaLogger.w("DataImportBytesLauncher", "Failed reading bytes: ${e.message}")
                        null
                    }
                }
                latestOnBytesRead(bytes)
            }
        } else {
            latestOnBytesRead(null)
        }
    }

    return remember(context) {
        {
            try {
                openDocLauncher.launch(arrayOf("*/*", "application/octet-stream", "application/json"))
            } catch (_: Exception) {
                latestOnBytesRead(null)
            }
        }
    }
}

@Composable
actual fun rememberAudioPickerLauncher(
    strings: AppStrings,
    onAudioPicked: (fileName: String?, localFilePath: String?, errorMessage: String?) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latestOnAudioPicked by rememberUpdatedState(onAudioPicked)
    val latestStrings by rememberUpdatedState(strings)

    val openDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }

        scope.launch {
            val (fileName, localPath, errorMsg) = withContext(Dispatchers.IO) {
                var tempFile: File? = null
                try {
                    val docFile = DocumentFile.fromSingleUri(context, uri)
                    val displayName = docFile?.name ?: "custom_sound.mp3"
                    val extension = displayName.substringAfterLast('.', "").lowercase()

                    val allowedExtensions = setOf("mp3", "wav", "ogg", "opus", "m4a", "aac", "flac")
                    if (extension.isNotEmpty() && !allowedExtensions.contains(extension)) {
                        return@withContext Triple(null, null, latestStrings.customSoundInvalidFormat)
                    }

                    val targetExt = if (extension.isNotEmpty()) extension else "mp3"
                    val destDir = File(context.filesDir, "custom_sounds")
                    destDir.mkdirs()
                    val destFile = File(destDir, "custom_rest_timer_sound.$targetExt")
                    val stagedTemp = File(destDir, "custom_rest_timer_sound_tmp.$targetExt")
                    tempFile = stagedTemp

                    // Stage upload into temp file first with a size cap so oversized/corrupt uploads stay safe
                    var copiedBytes = 0L
                    val opened = context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(stagedTemp).use { output ->
                            val buf = ByteArray(16 * 1024)
                            while (true) {
                                val n = input.read(buf)
                                if (n < 0) break
                                copiedBytes += n
                                if (copiedBytes > MAX_CUSTOM_AUDIO_BYTES) {
                                    throw IllegalStateException("Audio file exceeds max size")
                                }
                                output.write(buf, 0, n)
                            }
                            output.flush()
                        }
                        true
                    } ?: false
                    if (!opened || copiedBytes <= 0L) {
                        try { stagedTemp.delete() } catch (_: Throwable) {}
                        return@withContext Triple(null, null, latestStrings.backupErrorInvalidFile)
                    }

                    // Validate audio can be prepared with MediaPlayer
                    val testPlayer = MediaPlayer()
                    var isValid = false
                    try {
                        testPlayer.setDataSource(stagedTemp.absolutePath)
                        testPlayer.prepare()
                        isValid = testPlayer.duration > 0
                    } catch (t: Throwable) {
                        KumaLogger.e("AudioPicker", "Audio file validation failed: ${t.message}", t)
                        isValid = false
                    } finally {
                        try { testPlayer.release() } catch (_: Throwable) {}
                    }

                    if (!isValid) {
                        try { stagedTemp.delete() } catch (_: Throwable) {}
                        return@withContext Triple(null, null, latestStrings.customSoundInvalidFormat)
                    }

                    if (destFile.exists()) {
                        try { destFile.delete() } catch (_: Throwable) {}
                    }
                    if (!stagedTemp.renameTo(destFile)) {
                        stagedTemp.copyTo(destFile, overwrite = true)
                        try { stagedTemp.delete() } catch (_: Throwable) {}
                    }
                    tempFile = null

                    Triple(displayName, destFile.absolutePath, null)
                } catch (e: Exception) {
                    try { tempFile?.delete() } catch (_: Throwable) {}
                    KumaLogger.e("AudioPicker", "Exception copying audio file: ${e.message}", e)
                    val err = if (e is IllegalStateException) {
                        latestStrings.customSoundInvalidFormat
                    } else {
                        latestStrings.backupErrorWriteFailed
                    }
                    Triple(null, null, err)
                }
            }
            latestOnAudioPicked(fileName, localPath, errorMsg)
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
                KumaLogger.w("AudioPicker", "Failed launching audio picker: ${e.message}")
                latestOnAudioPicked(null, null, latestStrings.backupErrorFolderInaccessible)
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
