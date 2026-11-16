package com.bearbones.kumalift.core.data

import android.content.Intent
import android.net.Uri
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
