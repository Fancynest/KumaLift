package com.bearbones.kumalift.features.share

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.localization.LocalStrings
import com.bearbones.kumalift.core.media.QrCodeMatrix
import com.bearbones.kumalift.core.media.ShareImageExporter
import com.bearbones.kumalift.core.media.ShareSaveStatus
import com.bearbones.kumalift.core.media.encodeQrCodeMatrix
import kotlinx.coroutines.launch

@Composable
fun KumaQrBarcodeCanvas(
    matrix: QrCodeMatrix,
    modifier: Modifier = Modifier,
    size: Dp = 228.dp
) {
    val qrDark = Color.Black
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val cell = this.size.minDimension / matrix.size.toFloat()
            for (y in 0 until matrix.size) {
                for (x in 0 until matrix.size) {
                    if (matrix[x, y]) {
                        drawRect(
                            color = qrDark,
                            topLeft = Offset(x * cell, y * cell),
                            size = Size(cell + 0.5f, cell + 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProgramQrBarcodeSection(
    qrPayload: String,
    summaryLabel: String,
    filePrefix: String,
    onFeedback: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    val colors = LocalKumaColors.current
    val scope = rememberCoroutineScope()
    val matrix = remember(qrPayload) { encodeQrCodeMatrix(qrPayload) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(KumaColors.SurfaceContainerLow)
            .border(1.dp, colors.accentText.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.QrCode2,
                contentDescription = null,
                tint = colors.accentText,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = s.shareBarcodeProgramTitle,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textPrimary
                )
            )
        }

        if (matrix != null) {
            KumaQrBarcodeCanvas(matrix = matrix, size = 228.dp)
        } else {
            Text(
                text = s.qrPayloadTooLargeError,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = colors.error,
                    fontWeight = FontWeight.Medium
                )
            )
        }

        if (summaryLabel.isNotBlank()) {
            Text(
                text = summaryLabel,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = colors.accentText,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Text(
            text = s.shareBarcodeProgramDesc,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                color = colors.textSecondary
            )
        )

        if (matrix != null) {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        val bmp = matrix.toImageBitmap(720)
                        val status = ShareImageExporter.saveBitmapToGallery(bmp, filePrefix)
                        onFeedback(
                            when (status) {
                                ShareSaveStatus.SUCCESS -> s.barcodeSavedToast
                                ShareSaveStatus.ERROR_UNSUPPORTED -> s.shareUnsupportedOsToast
                                ShareSaveStatus.ERROR_WRITE -> s.shareFailedToast
                            }
                        )
                    }
                },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, colors.border)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp), tint = colors.accentText)
                Spacer(modifier = Modifier.width(6.dp))
                Text(s.saveBarcodePngAction, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
