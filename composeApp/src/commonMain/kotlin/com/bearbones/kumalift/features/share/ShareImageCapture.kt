package com.bearbones.kumalift.features.share

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.bearbones.kumalift.domain.share.SHARE_CANONICAL_DENSITY
import com.bearbones.kumalift.domain.share.SHARE_EXPORT_WIDTH_PX
import com.bearbones.kumalift.domain.share.SHARE_MAX_HEIGHT_PX
import com.bearbones.kumalift.domain.share.ShareAspectRatio
import com.bearbones.kumalift.domain.share.ShareExportDimensions
import com.bearbones.kumalift.domain.share.calculateCappedExportDimensions
import kotlin.math.roundToInt

@Stable
class ShareCaptureController internal constructor(
    internal val graphicsLayer: GraphicsLayer
) {
    var forceTransparentOverlay by mutableStateOf(false)
        internal set

    var isExportingRender by mutableStateOf(false)
        internal set

    var lastCalculatedDimensions by mutableStateOf(
        ShareExportDimensions(
            widthPx = SHARE_EXPORT_WIDTH_PX,
            heightPx = SHARE_EXPORT_WIDTH_PX,
            scaleFactor = 1f,
            isCapped = false
        )
    )
        internal set

    suspend fun captureBitmap(transparentOnly: Boolean = false): ImageBitmap? {
        return try {
            isExportingRender = true
            if (transparentOnly) {
                forceTransparentOverlay = true
            }
            // Await two rendered frames so GraphicsLayer re-records cleanly without selection outlines
            withFrameNanos {}
            withFrameNanos {}
            graphicsLayer.toImageBitmap()
        } catch (_: Exception) {
            null
        } finally {
            forceTransparentOverlay = false
            isExportingRender = false
        }
    }
}

@Composable
fun rememberShareCaptureController(): ShareCaptureController {
    val graphicsLayer = rememberGraphicsLayer()
    return remember(graphicsLayer) { ShareCaptureController(graphicsLayer) }
}

/**
 * Renders the single parameterized share composable at canonical 1080px-wide density (Density 3f)
 * for deterministic fixed-resolution PNG capture, while scaling visually to fit the on-screen preview.
 */
@Composable
fun SharePreviewStage(
    aspectRatio: ShareAspectRatio,
    controller: ShareCaptureController,
    modifier: Modifier = Modifier,
    maxPreviewHeightDp: Float? = null,
    content: @Composable (isTransparentExport: Boolean) -> Unit
) {
    val hostDensity = LocalDensity.current
    val canonicalDensity = remember { Density(density = SHARE_CANONICAL_DENSITY, fontScale = 1f) }
    val transparentOverlay = controller.forceTransparentOverlay

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        val availableWidthPx = constraints.maxWidth.coerceAtLeast(1)
        val maxHeightPx = if (maxPreviewHeightDp != null) {
            with(hostDensity) { maxPreviewHeightDp.dp.roundToPx() }
        } else {
            Int.MAX_VALUE
        }

        val fixedHeightPx = if (aspectRatio == ShareAspectRatio.TALL_LIST) null else aspectRatio.heightPx

        val measureConstraints = if (fixedHeightPx != null) {
            Constraints.fixed(width = SHARE_EXPORT_WIDTH_PX, height = fixedHeightPx)
        } else {
            Constraints(
                minWidth = SHARE_EXPORT_WIDTH_PX,
                maxWidth = SHARE_EXPORT_WIDTH_PX,
                minHeight = 1,
                maxHeight = Constraints.Infinity
            )
        }

        Layout(
            content = {
                CompositionLocalProvider(LocalDensity provides canonicalDensity) {
                    val cardSizeModifier = if (fixedHeightPx != null) {
                        Modifier
                            .width((SHARE_EXPORT_WIDTH_PX / SHARE_CANONICAL_DENSITY).dp)
                            .height((fixedHeightPx / SHARE_CANONICAL_DENSITY).dp)
                    } else {
                        Modifier.width((SHARE_EXPORT_WIDTH_PX / SHARE_CANONICAL_DENSITY).dp)
                    }
                    Box(
                        modifier = cardSizeModifier.drawWithContent {
                            val naturalW = size.width.roundToInt().coerceAtLeast(1)
                            val naturalH = size.height.roundToInt().coerceAtLeast(1)
                            val capped = calculateCappedExportDimensions(
                                naturalWidthPx = naturalW,
                                naturalHeightPx = naturalH,
                                maxWidthPx = SHARE_EXPORT_WIDTH_PX,
                                maxHeightPx = SHARE_MAX_HEIGHT_PX
                            )
                            controller.lastCalculatedDimensions = capped
                            controller.graphicsLayer.record(
                                size = IntSize(capped.widthPx, capped.heightPx)
                            ) {
                                if (capped.isCapped && capped.scaleFactor < 1f) {
                                    scale(
                                        scaleX = capped.scaleFactor,
                                        scaleY = capped.scaleFactor,
                                        pivot = Offset.Zero
                                    ) {
                                        this@drawWithContent.drawContent()
                                    }
                                } else {
                                    this@drawWithContent.drawContent()
                                }
                            }
                            drawLayer(controller.graphicsLayer)
                        }
                    ) {
                        content(transparentOverlay)
                    }
                }
            }
        ) { measurables, _ ->
            val placeable = measurables.first().measure(measureConstraints)
            val widthScale = availableWidthPx.toFloat() / placeable.width.toFloat()
            val heightScale = if (fixedHeightPx != null && maxHeightPx < Int.MAX_VALUE) {
                maxHeightPx.toFloat() / placeable.height.toFloat()
            } else {
                Float.POSITIVE_INFINITY
            }
            val previewScale = minOf(widthScale, heightScale).coerceIn(0.05f, 2f)
            val scaledW = (placeable.width * previewScale).roundToInt().coerceAtLeast(1)
            val scaledH = (placeable.height * previewScale).roundToInt().coerceAtLeast(1)

            layout(scaledW, scaledH) {
                placeable.placeRelativeWithLayer(0, 0) {
                    scaleX = previewScale
                    scaleY = previewScale
                    transformOrigin = TransformOrigin(0f, 0f)
                }
            }
        }
    }
}
