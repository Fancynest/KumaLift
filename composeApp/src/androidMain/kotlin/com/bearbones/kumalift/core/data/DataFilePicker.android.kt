package com.bearbones.kumalift.core.data

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.bearbones.kumalift.core.localization.AppStrings

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
                onExportResult(
                    false,
                    strings.failedToWriteBackupFile(e.message ?: "")
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
                pendingContent = null
                onExportResult(
                    false,
                    strings.failedToOpenFilePicker(e.message ?: "")
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
