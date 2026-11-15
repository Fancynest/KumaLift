package com.bearbones.kumalift.features.routines

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumalift.core.designsystem.KumaCard
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.core.designsystem.kumaKeyboardAvoidance
import com.bearbones.kumalift.core.localization.LocalStrings
import com.bearbones.kumalift.data.repository.SettingsRepository

data class SchedulePreset(
    val title: String,
    val days: List<Int> // 1=Mon, 2=Tue, 3=Wed, 4=Thu, 5=Fri, 6=Sat, 7=Sun
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WorkoutScheduleModal(
    settingsRepository: SettingsRepository,
    onDismiss: () -> Unit
) {
    val colors = LocalKumaColors.current
    val s = LocalStrings.current
    val settings by settingsRepository.settings.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val currentDays = settings.profile.selectedTrainingDays.ifEmpty {
        listOf(1, 3, 5, 6)
    }
    val selectedDays = remember { mutableStateListOf<Int>().apply { addAll(currentDays) } }

    val daysOfWeek = listOf(
        1 to "Senin",
        2 to "Selasa",
        3 to "Rabu",
        4 to "Kamis",
        5 to "Jumat",
        6 to "Sabtu",
        7 to "Minggu"
    )

    val presets = listOf(
        SchedulePreset("3x Seminggu (Sen, Rab, Jum)", listOf(1, 3, 5)),
        SchedulePreset("4x Seminggu (Sen, Sel, Kam, Jum)", listOf(1, 2, 4, 5)),
        SchedulePreset("5x Seminggu (Senin–Jumat)", listOf(1, 2, 3, 4, 5)),
        SchedulePreset("6x Seminggu (Senin–Sabtu)", listOf(1, 2, 3, 4, 5, 6)),
        SchedulePreset("7x Seminggu (Setiap Hari)", listOf(1, 2, 3, 4, 5, 6, 7))
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = KumaColors.SurfaceDim
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .kumaKeyboardAvoidance()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = s.trainingScheduleTitle,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )

                IconButtonBox(
                    icon = Icons.Default.Close,
                    contentDescription = s.close,
                    onClick = onDismiss
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tentukan frekuensi latihan mingguan secara bebas (1–7 hari). Sistem akan menyesuaikan target latihan mingguan secara otomatis.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = colors.textSecondary,
                    fontSize = 13.sp
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Presets
            Text(
                text = "PILIHAN POLA CEPAT",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = colors.accentText,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { preset ->
                    val isPresetActive = selectedDays.size == preset.days.size &&
                            selectedDays.containsAll(preset.days)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isPresetActive) colors.accentFill.copy(alpha = 0.15f) else KumaColors.SurfaceContainer)
                            .border(
                                1.dp,
                                if (isPresetActive) colors.accentFill else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                selectedDays.clear()
                                selectedDays.addAll(preset.days)
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = preset.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isPresetActive) FontWeight.Bold else FontWeight.Medium,
                                color = if (isPresetActive) colors.accentText else colors.textPrimary
                            )
                        )
                        if (isPresetActive) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = colors.accentText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Individual Days Selection (Monday - Sunday)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PILIH HARI LATIHAN (${selectedDays.size} HARI)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = colors.accentText,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Day circles selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                daysOfWeek.forEach { (dayNum, name) ->
                    val isSelected = selectedDays.contains(dayNum)
                    val shortName = name.take(3)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            if (isSelected) {
                                if (selectedDays.size > 1) { // minimum 1 day
                                    selectedDays.remove(dayNum)
                                }
                            } else {
                                selectedDays.add(dayNum)
                                selectedDays.sort()
                            }
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) colors.accentFill else KumaColors.SurfaceContainer)
                                .border(
                                    1.dp,
                                    if (isSelected) colors.accentFillBorder else Color.Transparent,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name.take(1),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) colors.onAccentFill else colors.textSecondary
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = shortName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                color = if (isSelected) colors.accentText else colors.textSecondary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Summary Card
            KumaCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = KumaColors.SurfaceContainerLow,
                cornerRadius = 14.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Ringkasan Jadwal:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = colors.textSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${selectedDays.size} kali seminggu: " +
                                selectedDays.sorted().joinToString(", ") { day ->
                                    daysOfWeek.firstOrNull { it.first == day }?.second ?: ""
                                },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            Button(
                onClick = {
                    val finalDays = selectedDays.toList().sorted()
                    settingsRepository.update { current ->
                        current.copy(
                            profile = current.profile.copy(
                                trainingDaysPerWeek = finalDays.size,
                                selectedTrainingDays = finalDays
                            )
                        )
                    }
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accentFill,
                    contentColor = colors.onAccentFill
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(s.save, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun IconButtonBox(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    onClick: () -> Unit
) {
    val colors = LocalKumaColors.current
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(KumaColors.SurfaceContainer)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = colors.textPrimary,
            modifier = Modifier.size(18.dp)
        )
    }
}
