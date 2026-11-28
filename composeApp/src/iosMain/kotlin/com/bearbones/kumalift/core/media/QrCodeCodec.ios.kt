package com.bearbones.kumalift.core.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

internal actual fun platformCreateBitmapFromArgb(
    pixels: IntArray,
    width: Int,
    height: Int
): ImageBitmap = ImageBitmap(1, 1)

actual fun encodeQrCodeMatrix(content: String): QrCodeMatrix? = null

@Composable
actual fun QrPlanScannerDialog(
    onCodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
) = Unit
