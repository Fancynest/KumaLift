package com.bearbones.kumalift.core.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap

data class QrCodeMatrix(
    val size: Int,
    val modules: BooleanArray
) {
    operator fun get(x: Int, y: Int): Boolean = modules[y * size + x]

    fun toImageBitmap(targetSizePx: Int = 900): ImageBitmap {
        val quiet = 4
        val totalModules = size + quiet * 2
        val cellPx = (targetSizePx / totalModules).coerceAtLeast(2)
        val dim = totalModules * cellPx
        val pixels = IntArray(dim * dim) { 0xFFFFFFFF.toInt() }
        val black = 0xFF000000.toInt()
        for (my in 0 until size) {
            for (mx in 0 until size) {
                if (this[mx, my]) {
                    val startX = (mx + quiet) * cellPx
                    val startY = (my + quiet) * cellPx
                    for (py in startY until (startY + cellPx)) {
                        val rowOffset = py * dim
                        for (px in startX until (startX + cellPx)) {
                            pixels[rowOffset + px] = black
                        }
                    }
                }
            }
        }
        return platformCreateBitmapFromArgb(pixels, dim, dim)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is QrCodeMatrix) return false
        return size == other.size && modules.contentEquals(other.modules)
    }

    override fun hashCode(): Int = 31 * size + modules.contentHashCode()
}

internal expect fun platformCreateBitmapFromArgb(pixels: IntArray, width: Int, height: Int): ImageBitmap

expect fun encodeQrCodeMatrix(content: String): QrCodeMatrix?

@Composable
expect fun QrPlanScannerDialog(
    onCodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
)
