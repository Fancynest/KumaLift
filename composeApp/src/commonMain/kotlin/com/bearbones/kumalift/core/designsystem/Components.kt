package com.bearbones.kumalift.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun KumaPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(PillShape)
            .background(if (enabled) KumaColors.ElectricLime else KumaColors.SurfaceContainerHigh)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = if (enabled) KumaColors.OnElectricLime else KumaColors.TextMuted,
                    letterSpacing = 0.5.sp
                )
            )
            if (icon != null) {
                Box(modifier = Modifier.padding(start = 8.dp)) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (enabled) KumaColors.OnElectricLime else KumaColors.TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun KumaCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = KumaColors.SurfaceContainerLow,
    borderColor: Color = KumaColors.BorderDefault,
    cornerRadius: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(cornerRadius)),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        content()
    }
}

@Composable
fun KumaSetCheckbox(
    isCompleted: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 48x48 touch target
    Box(
        modifier = modifier
            .size(48.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isCompleted) KumaColors.ElectricLime else KumaColors.SurfaceContainer)
                .border(
                    width = if (isCompleted) 0.dp else 1.5.dp,
                    color = if (isCompleted) Color.Transparent else KumaColors.BorderDefault,
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = KumaColors.OnElectricLime,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun KumaBadge(
    text: String,
    modifier: Modifier = Modifier,
    isPr: Boolean = false,
    icon: ImageVector? = null
) {
    val bg = if (isPr) KumaColors.ElectricLimeGlow else KumaColors.SurfaceContainer
    val border = if (isPr) KumaColors.ElectricLime else KumaColors.BorderDefault
    val textColor = if (isPr) KumaColors.ElectricLime else KumaColors.OnSurface

    Row(
        modifier = modifier
            .clip(PillShape)
            .background(bg)
            .border(1.dp, border, PillShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier
                    .size(14.dp)
                    .padding(end = 4.dp)
            )
        }
        Text(
            text = text,
            maxLines = 1,
            softWrap = false,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        )
    }
}

@Composable
fun KumaNumericStepper(
    valueText: String,
    unit: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(KumaColors.SurfaceContainerHigh)
            .border(1.dp, KumaColors.BorderDefault, RoundedCornerShape(12.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Minus button (44x44)
        Box(
            modifier = Modifier
                .size(44.dp)
                .clickable(onClick = onDecrement),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Decrease",
                tint = KumaColors.OnSurface,
                modifier = Modifier.size(18.dp)
            )
        }

        // Center Value
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = valueText,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = KumaColors.TextWhite
                )
            )
            if (unit.isNotEmpty()) {
                Text(
                    text = " $unit",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = KumaColors.TextMuted
                    ),
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }

        // Plus button (44x44)
        Box(
            modifier = Modifier
                .size(44.dp)
                .clickable(onClick = onIncrement),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Increase",
                tint = KumaColors.OnSurface,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
