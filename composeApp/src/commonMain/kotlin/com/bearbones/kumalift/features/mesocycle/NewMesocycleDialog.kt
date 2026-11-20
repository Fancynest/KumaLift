package com.bearbones.kumalift.features.mesocycle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.core.localization.LocalStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewMesocycleDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, totalWeeks: Int, deloadWeek: Int) -> Unit
) {
    val colors = LocalKumaColors.current
    val s = LocalStrings.current

    var blockName by remember { mutableStateOf("Hypertrophy Block") }
    var selectedWeeks by remember { mutableIntStateOf(5) }
    val weekOptions = listOf(4, 5, 6, 8)

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(KumaColors.SurfaceContainer)
                .border(1.dp, colors.surfaceVariant, RoundedCornerShape(20.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = s.newMesocycleDialogTitle,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            )

            // Block Name Input
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = s.mesocycleNameLabel,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = colors.textSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                OutlinedTextField(
                    value = blockName,
                    onValueChange = { blockName = it },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedContainerColor = KumaColors.SurfaceDim,
                        unfocusedContainerColor = KumaColors.SurfaceDim,
                        focusedIndicatorColor = colors.accentFill,
                        unfocusedIndicatorColor = colors.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Total Weeks Picker (4, 5, 6, 8)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = s.mesocycleTotalWeeksLabel,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = colors.textSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    weekOptions.forEach { weeks ->
                        val isSelected = weeks == selectedWeeks
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) colors.accentFill else KumaColors.SurfaceDim)
                                .clickable { selectedWeeks = weeks }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$weeks W",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) colors.onAccentFill else colors.textPrimary
                                )
                            )
                        }
                    }
                }
            }

            // Deload Week Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.accentFill.copy(alpha = 0.10f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${s.mesocycleDeloadWeekLabel}: ${s.mesocycleDeloadScheduleHint(selectedWeeks)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = colors.textPrimary,
                        fontSize = 11.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(KumaColors.SurfaceDim)
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = s.cancel,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = colors.textSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.accentFill)
                        .clickable { onConfirm(blockName, selectedWeeks, selectedWeeks) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = s.save,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = colors.onAccentFill,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}
