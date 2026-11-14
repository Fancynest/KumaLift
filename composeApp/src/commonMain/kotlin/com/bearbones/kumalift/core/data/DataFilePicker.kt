package com.bearbones.kumalift.core.data

import androidx.compose.runtime.Composable

@Composable
expect fun rememberDataExportLauncher(
    isEnglish: Boolean = false,
    onExportResult: (Boolean, String) -> Unit
): (fileName: String, content: String) -> Unit

@Composable
expect fun rememberDataImportLauncher(
    onContentRead: (String?) -> Unit
): () -> Unit
