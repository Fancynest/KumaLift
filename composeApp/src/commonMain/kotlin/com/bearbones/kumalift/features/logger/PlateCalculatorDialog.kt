package com.bearbones.kumalift.features.logger

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.bearbones.kumalift.domain.model.PlateCalculator
import com.bearbones.kumalift.domain.model.WeightUnit

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlateCalculatorDialog(
    initialWeightKg: Double,
    unit: WeightUnit = WeightUnit.KG,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    val s = LocalStrings.current
    val colors = LocalKumaColors.current

    var selectedBarWeight by remember { mutableStateOf(20.0) }
    var targetWeight by remember { mutableStateOf(if (initialWeightKg > 0.0) initialWeightKg else 60.0) }

    val calcResult = remember(targetWeight, selectedBarWeight) {
        PlateCalculator.calculate(
            targetWeightKg = targetWeight,
            barWeightKg = selectedBarWeight
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
                        text = "Barbell Plate Breakdown",
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
                                onClick = { targetWeight = (targetWeight - 2.5).coerceAtLeast(selectedBarWeight) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "-2.5", tint = colors.textPrimary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${targetWeight.toString().removeSuffix(".0")} kg",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colors.accentText
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { targetWeight += 2.5 },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "+2.5", tint = colors.textPrimary)
                            }
                        }

                        // Quick weight delta buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            listOf(-10.0, -5.0, 5.0, 10.0).forEach { delta ->
                                val label = if (delta > 0) "+${delta.toInt()}" else "${delta.toInt()}"
                                Box(
                                    modifier = Modifier
                                        .clip(PillShape)
                                        .background(KumaColors.SurfaceContainerHigh)
                                        .clickable {
                                            targetWeight = (targetWeight + delta).coerceAtLeast(selectedBarWeight)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label,
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
                        listOf(
                            20.0 to "Olympic (20 kg)",
                            15.0 to "Women (15 kg)",
                            10.0 to "EZ (10 kg)"
                        ).forEach { (barWeight, barLabel) ->
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
                                    text = "${barWeight.toInt()} kg",
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
                        text = "${s.loadEachSide}: ${calcResult.weightPerSideKg} kg",
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
                            .background(Color(0xFF141416), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Barbell shaft line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .background(Color(0xFF5A6065))
                        )

                        // Center Barbell Sleeve with Plates
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // Collar stop
                            Box(
                                modifier = Modifier
                                    .width(10.dp)
                                    .height(55.dp)
                                    .background(Color(0xFF374151), RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(3.dp))

                            if (calcResult.platesPerSide.isEmpty()) {
                                Text(
                                    text = "Bare Barbell",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = KumaColors.TextMuted,
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            } else {
                                // Draw plates from largest to smallest
                                calcResult.platesPerSide.forEach { plateCount ->
                                    repeat(plateCount.countPerSide) {
                                        val plateHeight = (80 * plateCount.heightFraction).dp
                                        Box(
                                            modifier = Modifier
                                                .width(16.dp)
                                                .height(plateHeight)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(Color(plateCount.hexColor))
                                                .border(1.dp, Color.Black.copy(alpha = 0.3f), RoundedCornerShape(3.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = plateCount.weightKg.toString().removeSuffix(".0"),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = if (plateCount.hexColor == 0xFFFAFAFA || plateCount.hexColor == 0xFFFDD835) Color.Black else Color.White,
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
                                            .background(Color(item.hexColor))
                                    )
                                    Text(
                                        text = "${item.countPerSide} × ${item.weightKg.toString().removeSuffix(".0")} kg",
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
            KumaPrimaryButton(
                text = "${s.save} (${calcResult.totalCalculatedWeightKg.toString().removeSuffix(".0")} kg)",
                onClick = {
                    onConfirm(calcResult.totalCalculatedWeightKg)
                    onDismiss()
                }
            )
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
