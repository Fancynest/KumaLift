package com.bearbones.kumalift.core.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberProfileImagePicker(
    onImagePicked: (String) -> Unit
): () -> Unit {
    return remember { {} }
}
