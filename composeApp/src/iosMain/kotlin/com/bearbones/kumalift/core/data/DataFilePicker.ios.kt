package com.bearbones.kumalift.core.data

import androidx.compose.runtime.Composable

@Composable
actual fun rememberDataExportLauncher(
    onExportResult: (Boolean, String) -> Unit
): (fileName: String, content: String) -> Unit {
    return { fileName, _ ->
        onExportResult(true, "Data $fileName siap diekspor.")
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
