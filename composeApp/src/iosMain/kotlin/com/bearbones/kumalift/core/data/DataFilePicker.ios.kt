package com.bearbones.kumalift.core.data

import androidx.compose.runtime.Composable

@Composable
actual fun rememberDataExportLauncher(
    isEnglish: Boolean,
    onExportResult: (Boolean, String) -> Unit
): (fileName: String, content: String) -> Unit {
    return { fileName, _ ->
        onExportResult(
            true,
            if (isEnglish) "Data $fileName ready for export." else "Data $fileName siap diekspor."
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
