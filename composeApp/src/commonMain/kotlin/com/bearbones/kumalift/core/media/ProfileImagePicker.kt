package com.bearbones.kumalift.core.media

import androidx.compose.runtime.Composable

/**
 * Platform-specific image picker launcher.
 * Invoking the returned lambda opens the device gallery/photo picker.
 * When an image is picked, it is copied to permanent private local storage
 * and the callback is invoked with the permanent local file path.
 */
@Composable
expect fun rememberProfileImagePicker(
    onImagePicked: (String) -> Unit
): () -> Unit
