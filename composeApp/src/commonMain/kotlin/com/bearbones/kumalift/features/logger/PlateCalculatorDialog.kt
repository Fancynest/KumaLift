package com.bearbones.kumalift.features.logger

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.KumaPrimaryButton
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.core.localization.LocalStrings
import com.bearbones.kumalift.core.util.NumberFormatUtils
import com.bearbones.kumalift.core.util.UnitConverters
import com.bearbones.kumalift.domain.model.AppLanguage
import com.bearbones.kumalift.domain.model.PlateCalculator
import com.bearbones.kumalift.domain.model.WeightUnit

private fun formatWeightDisplay(weight: Double, language: AppLanguage): String =
    NumberFormatUtils.formatDecimal(weight, language, 1, trimTrailingZeros = true)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlateCalculatorDialog(
    initialWeightKg: Double,
    unit: WeightUnit = WeightUnit.KG,
    language: AppLanguage = AppLanguage.SYSTEM,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    val s = LocalStrings.current
    val colors = LocalKumaColors.current

    val isLb = unit == WeightUnit.LB
    val unitLabel = if (isLb) s.unitLb else s.unitKg
    val defaultBar = if (isLb) 45.0 else 20.0
    val defaultTarget = if (isLb) 135.0 else 60.0
    val stepIncrement = if (isLb) 5.0 else 2.5
    val barPresets = if (isLb) listOf(45.0, 35.0, 25.0) else listOf(20.0, 15.0, 10.0)
    val availablePlates = if (isLb) {
        listOf(45.0, 35.0, 25.0, 10.0, 5.0, 2.5)
    } else {
        listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)
    }

    var selectedBarWeight by remember(unit) { mutableStateOf(defaultBar) }
    var targetWeight by remember(unit, initialWeightKg) {
        mutableStateOf(
            if (initialWeightKg > 0.0) UnitConverters.toDisplayWeight(initialWeightKg, unit) else defaultTarget
        )
    }

    val calcResult = remember(targetWeight, selectedBarWeight, isLb) {
        PlateCalculator.calculate(
            targetWeightKg = targetWeight,
            barWeightKg = selectedBarWeight,
            availablePlatesKg = availablePlates,
            useImperialPalette = isLb
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KumaColors.SurfaceContainerLow,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colors.accentFill.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = colors.accentText,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = s.plateCalculatorTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    )
                    Text(
                        text = s.plateCalculatorSubtitle,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = KumaColors.TextMuted
                        )
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Target Weight Adjuster
                KumaCard(
                    backgroundColor = KumaColors.SurfaceContainer,
                    cornerRadius = 14.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = s.totalWeight,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = KumaColors.TextSecondary
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            IconButton(
                                onClick = { targetWeight = (targetWeight - stepIncrement).coerceAtLeast(selectedBarWeight) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "-${formatWeightDisplay(stepIncrement, language)}", tint = colors.textPrimary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${formatWeightDisplay(targetWeight, language)} $unitLabel",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colors.accentText
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { targetWeight += stepIncrement },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "+${formatWeightDisplay(stepIncrement, language)}", tint = colors.textPrimary)
                            }
                        }

                        // Quick weight delta buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            val quickDeltas = if (isLb) listOf(-20.0, -10.0, 10.0, 20.0) else listOf(-10.0, -5.0, 5.0, 10.0)
                            quickDeltas.forEach { delta ->
                                val label = if (delta > 0) "+${delta.toInt()}" else "${delta.toInt()}"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(PillShape)
                                        .background(KumaColors.SurfaceContainerHigh)
                                        .clickable {
                                            targetWeight = (targetWeight + delta).coerceAtLeast(selectedBarWeight)
                                        }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        maxLines = 1,
                                        softWrap = false,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Bar Weight Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = s.barWeight,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = KumaColors.TextSecondary
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        barPresets.forEach { barWeight ->
                            val isSelected = selectedBarWeight == barWeight
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) colors.accentFill else KumaColors.SurfaceContainer)
                                    .clickable {
                                        selectedBarWeight = barWeight
                                        if (targetWeight < barWeight) targetWeight = barWeight
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${barWeight.toInt()} $unitLabel",
                                    maxLines = 1,
                                    softWrap = false,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) colors.onAccentFill else colors.textPrimary
                                    )
                                )
                            }
                        }
                    }
                }

                // Visual Barbell
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(KumaColors.SurfaceContainer)
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${s.loadEachSide}: ${formatWeightDisplay(calcResult.weightPerSideKg, language)} $unitLabel",
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Visual Barbell Drawing
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(colors.plateTrack, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Barbell shaft line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .background(colors.plateBarbell)
                        )

                        // Center Barbell Sleeve with Plates
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // Collar stop
                            Box(
                                modifier = Modifier
                                    .width(10.dp)
                                    .height(55.dp)
                                    .background(colors.plateSleeve, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(3.dp))

                            if (calcResult.platesPerSide.isEmpty()) {
                                Text(
                                    text = s.bareBarbellLabel,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    softWrap = false,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = KumaColors.TextMuted,
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            } else {
                                // Draw plates from largest to smallest
                                calcResult.platesPerSide.forEach { plateCount ->
                                    val (plateColor, onPlateColor) = if (isLb) {
                                        when (plateCount.weightKg) {
                                            45.0 -> Pair(colors.plateBlue, colors.onPlateBlue)
                                            35.0 -> Pair(colors.plateYellow, colors.onPlateYellow)
                                            25.0 -> Pair(colors.plateGreen, colors.onPlateGreen)
                                            10.0 -> Pair(colors.plateWhite, colors.onPlateWhite)
                                            5.0 -> Pair(colors.plateBlack, colors.onPlateBlack)
                                            else -> Pair(colors.plateGray, colors.onPlateGray)
                                        }
                                    } else {
                                        when (plateCount.weightKg) {
                                            25.0 -> Pair(colors.plateRed, colors.onPlateRed)
                                            20.0 -> Pair(colors.plateBlue, colors.onPlateBlue)
                                            15.0 -> Pair(colors.plateYellow, colors.onPlateYellow)
                                            10.0 -> Pair(colors.plateGreen, colors.onPlateGreen)
                                            5.0 -> Pair(colors.plateWhite, colors.onPlateWhite)
                                            2.5 -> Pair(colors.plateBlack, colors.onPlateBlack)
                                            else -> Pair(colors.plateGray, colors.onPlateGray)
                                        }
                                    }
                                    repeat(minOf(plateCount.countPerSide, 24)) {
                                        val plateHeight = (80 * plateCount.heightFraction).dp
                                        Box(
                                            modifier = Modifier
                                                .width(16.dp)
                                                .height(plateHeight)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(plateColor)
                                                .border(1.dp, colors.borderStrong.copy(alpha = 0.3f), RoundedCornerShape(3.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = formatWeightDisplay(plateCount.weightKg, language),
                                                maxLines = 1,
                                                softWrap = false,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = onPlateColor,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(2.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Plates Breakdown Chips
                    if (calcResult.platesPerSide.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            calcResult.platesPerSide.forEach { item ->
                                val chipPlateColor = if (isLb) {
                                    when (item.weightKg) {
                                        45.0 -> colors.plateBlue
                                        35.0 -> colors.plateYellow
                                        25.0 -> colors.plateGreen
                                        10.0 -> colors.plateWhite
                                        5.0 -> colors.plateBlack
                                        else -> colors.plateGray
                                    }
                                } else {
                                    when (item.weightKg) {
                                        25.0 -> colors.plateRed
                                        20.0 -> colors.plateBlue
                                        15.0 -> colors.plateYellow
                                        10.0 -> colors.plateGreen
                                        5.0 -> colors.plateWhite
                                        2.5 -> colors.plateBlack
                                        else -> colors.plateGray
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .clip(PillShape)
                                        .background(KumaColors.SurfaceContainerHigh)
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(chipPlateColor)
                                    )
                                    Text(
                                        text = "${item.countPerSide} × ${formatWeightDisplay(item.weightKg, language)} $unitLabel",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = {
                    onConfirm(UnitConverters.toStorageWeightKg(calcResult.totalCalculatedWeightKg, unit))
                    onDismiss()
                },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = colors.accentFill,
                    contentColor = colors.onAccentFill
                ),
                shape = PillShape
            ) {
                Text(
                    text = "${s.save} (${formatWeightDisplay(calcResult.totalCalculatedWeightKg, language)} $unitLabel)",
                    maxLines = 1,
                    softWrap = false,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = s.cancel,
                    style = MaterialTheme.typography.labelMedium.copy(color = KumaColors.TextMuted)
                )
            }
        }
    )
}

@Composable
private fun KumaCard(
    backgroundColor: Color,
    cornerRadius: androidx.compose.ui.unit.Dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
    ) {
        content()
    }
}
