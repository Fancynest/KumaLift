package com.bearbones.kumalift.core.data

import androidx.compose.runtime.Composable
import com.bearbones.kumalift.core.localization.AppStrings

@Composable
actual fun rememberDataExportLauncher(
    strings: AppStrings,
    onExportResult: (Boolean, String) -> Unit
): (fileName: String, content: String) -> Unit {
    return { fileName, _ ->
        onExportResult(
            true,
            strings.backupFileSavedSuccess
        )
    }
}

@Composable
actual fun rememberDataImportLauncher(
    onContentRead: (String?) -> Unit
): () -> Unit {
    return {
        onContentRead(null)
    }
}

@Composable
actual fun rememberFolderPickerLauncher(
    onFolderPicked: (folderUri: String?, folderDisplayName: String?) -> Unit
): () -> Unit {
    return {
        onFolderPicked(null, null)
    }
}

@Composable
actual fun rememberDataExportBytesLauncher(
    strings: AppStrings,
    onExportResult: (Boolean, String) -> Unit
): (fileName: String, mimeType: String, bytes: ByteArray) -> Unit {
    return { _, _, _ ->
        onExportResult(true, strings.backupFileSavedSuccess)
    }
}

@Composable
actual fun rememberDataImportBytesLauncher(
    onBytesRead: (ByteArray?) -> Unit
): () -> Unit {
    return {
        onBytesRead(null)
    }
}

@Composable
actual fun rememberAudioPickerLauncher(
    strings: AppStrings,
    onAudioPicked: (fileName: String?, localFilePath: String?, errorMessage: String?) -> Unit
): () -> Unit {
    return {
        onAudioPicked(null, null, strings.customSoundInvalidFormat)
    }
}

actual fun deleteCustomAudioFile(filePath: String?) {
    // iOS stub
}
