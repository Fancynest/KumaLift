package com.bearbones.kumalift.core.data

import androidx.compose.runtime.Composable
import com.bearbones.kumalift.core.localization.AppStrings
import com.bearbones.kumalift.core.localization.LocalStrings

@Composable
expect fun rememberDataExportLauncher(
    strings: AppStrings = LocalStrings.current,
    onExportResult: (Boolean, String) -> Unit
): (fileName: String, content: String) -> Unit

@Composable
expect fun rememberDataImportLauncher(
    onContentRead: (String?) -> Unit
): () -> Unit

@Composable
expect fun rememberFolderPickerLauncher(
    onFolderPicked: (folderUri: String?, folderDisplayName: String?) -> Unit
): () -> Unit

@Composable
expect fun rememberDataExportBytesLauncher(
    strings: AppStrings = LocalStrings.current,
    onExportResult: (Boolean, String) -> Unit
): (fileName: String, mimeType: String, bytes: ByteArray) -> Unit

@Composable
expect fun rememberDataImportBytesLauncher(
    onBytesRead: (ByteArray?) -> Unit
): () -> Unit

@Composable
expect fun rememberAudioPickerLauncher(
    strings: AppStrings = LocalStrings.current,
    onAudioPicked: (fileName: String?, localFilePath: String?, errorMessage: String?) -> Unit
): () -> Unit

expect fun deleteCustomAudioFile(filePath: String?)
