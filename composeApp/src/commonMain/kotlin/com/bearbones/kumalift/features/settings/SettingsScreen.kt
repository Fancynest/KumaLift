package com.bearbones.kumalift.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumalift.core.designsystem.KumaBadge
import com.bearbones.kumalift.core.designsystem.KumaCard
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.data.repository.SettingsRepository
import com.bearbones.kumalift.domain.model.AppLanguage
import com.bearbones.kumalift.domain.model.AppTheme
import com.bearbones.kumalift.domain.model.OneRepMaxFormula
import com.bearbones.kumalift.domain.model.WeightUnit

@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    onNavigateBack: () -> Unit
) {
    val settings by settingsRepository.settings.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KumaColors.CanvasLowest)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KumaColors.SurfaceDim)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(KumaColors.SurfaceContainer)
                        .clickable(onClick = onNavigateBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = KumaColors.OnSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "Pengaturan",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = KumaColors.TextWhite
                    )
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    // Athlete Card Preview
                    KumaCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = KumaColors.SurfaceContainerLow,
                        cornerRadius = 16.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(KumaColors.SurfaceContainerHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = KumaColors.ElectricLime,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (settings.profile.name.isNotBlank()) settings.profile.name else "Atlet KumaLift",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = KumaColors.TextWhite
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Target: ${settings.profile.trainingDaysPerWeek} hari/minggu · ${settings.profile.experienceLevel.displayName}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = KumaColors.TextMuted)
                                )
                            }
                        }
                    }
                }

                // 1. Language Setting
                item {
                    SettingsSection(
                        title = "BAHASA / LANGUAGE",
                        icon = Icons.Default.Language
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LanguageOptionRow(
                                title = "Ikuti Pengaturan HP",
                                subtitle = "Gunakan bahasa bawaan sistem perangkat",
                                isSelected = settings.appearance.language == AppLanguage.SYSTEM,
                                onClick = {
                                    settingsRepository.update {
                                        it.copy(appearance = it.appearance.copy(language = AppLanguage.SYSTEM))
                                    }
                                }
                            )
                            LanguageOptionRow(
                                title = "Bahasa Indonesia",
                                subtitle = "Tampilan antarmuka berbahasa Indonesia",
                                isSelected = settings.appearance.language == AppLanguage.ID,
                                onClick = {
                                    settingsRepository.update {
                                        it.copy(appearance = it.appearance.copy(language = AppLanguage.ID))
                                    }
                                }
                            )
                            LanguageOptionRow(
                                title = "English",
                                subtitle = "User interface in English",
                                isSelected = settings.appearance.language == AppLanguage.EN,
                                onClick = {
                                    settingsRepository.update {
                                        it.copy(appearance = it.appearance.copy(language = AppLanguage.EN))
                                    }
                                }
                            )
                        }
                    }
                }

                // 2. Units & Formats
                item {
                    SettingsSection(
                        title = "SATUAN BEBAN / UNITS",
                        icon = Icons.Default.Scale
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            UnitSelectChip(
                                label = "Kilogram (kg)",
                                isSelected = settings.units.weightUnit == WeightUnit.KG,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    settingsRepository.update {
                                        it.copy(units = it.units.copy(weightUnit = WeightUnit.KG))
                                    }
                                }
                            )
                            UnitSelectChip(
                                label = "Pound (lb)",
                                isSelected = settings.units.weightUnit == WeightUnit.LB,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    settingsRepository.update {
                                        it.copy(units = it.units.copy(weightUnit = WeightUnit.LB))
                                    }
                                }
                            )
                        }
                    }
                }

                // 3. Timer & Logging
                item {
                    SettingsSection(
                        title = "LATIHAN & REST TIMER",
                        icon = Icons.Default.Timer
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Auto start timer switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Mulai Timer Otomatis",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = KumaColors.TextWhite
                                        )
                                    )
                                    Text(
                                        text = "Timer istirahat jalan setelah set dicentang",
                                        style = MaterialTheme.typography.labelSmall.copy(color = KumaColors.TextMuted)
                                    )
                                }
                                Switch(
                                    checked = settings.logging.restTimerAutoStart,
                                    onCheckedChange = { checked ->
                                        settingsRepository.update {
                                            it.copy(logging = it.logging.copy(restTimerAutoStart = checked))
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = KumaColors.OnElectricLime,
                                        checkedTrackColor = KumaColors.ElectricLime,
                                        uncheckedThumbColor = KumaColors.TextMuted,
                                        uncheckedTrackColor = KumaColors.SurfaceContainerHighest
                                    )
                                )
                            }

                            // Rest timer duration presets
                            Text(
                                text = "Durasi Istirahat Bawaan: ${settings.logging.restTimerDefaultSec} detik",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = KumaColors.TextWhite
                                )
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(60, 90, 120, 150).forEach { sec ->
                                    val isCurrent = settings.logging.restTimerDefaultSec == sec
                                    Box(
                                        modifier = Modifier
                                            .clip(PillShape)
                                            .background(if (isCurrent) KumaColors.ElectricLime else KumaColors.SurfaceContainer)
                                            .clickable {
                                                settingsRepository.update {
                                                    it.copy(logging = it.logging.copy(restTimerDefaultSec = sec))
                                                }
                                            }
                                            .padding(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "${sec}d",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCurrent) KumaColors.OnElectricLime else KumaColors.TextMuted
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Calculations (1RM Formula)
                item {
                    SettingsSection(
                        title = "RUMUS ESTIMASI 1RM",
                        icon = Icons.Default.FitnessCenter
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OneRepMaxFormula.entries.forEach { formula ->
                                val isSelected = settings.calculations.oneRepMaxFormula == formula
                                val desc = when (formula) {
                                    OneRepMaxFormula.EPLEY -> "Epley: w × (1 + r/30) [Standar populer]"
                                    OneRepMaxFormula.BRZYCKI -> "Brzycki: w × 36 / (37 - r)"
                                    OneRepMaxFormula.LOMBARDI -> "Lombardi: w × r^0.10"
                                    OneRepMaxFormula.OCONNER -> "O'Conner: w × (1 + r/40)"
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) KumaColors.SurfaceContainerHigh else KumaColors.SurfaceContainer)
                                        .border(
                                            1.dp,
                                            if (isSelected) KumaColors.ElectricLime else KumaColors.BorderDefault,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            settingsRepository.update {
                                                it.copy(calculations = it.calculations.copy(oneRepMaxFormula = formula))
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = formula.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) KumaColors.ElectricLime else KumaColors.TextWhite
                                                )
                                            )
                                            Text(
                                                text = desc,
                                                style = MaterialTheme.typography.labelSmall.copy(color = KumaColors.TextMuted)
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = KumaColors.ElectricLime,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Appearance & Theme
                item {
                    SettingsSection(
                        title = "TEMA TAMPILAN",
                        icon = Icons.Default.Palette
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                AppTheme.DARK to "Dark (Gelap)",
                                AppTheme.LIGHT to "Light (Terang)",
                                AppTheme.SYSTEM to "Sistem"
                            ).forEach { (theme, label) ->
                                val isSelected = settings.appearance.theme == theme
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) KumaColors.ElectricLime else KumaColors.SurfaceContainer)
                                        .clickable {
                                            settingsRepository.update {
                                                it.copy(appearance = it.appearance.copy(theme = theme))
                                            }
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) KumaColors.OnElectricLime else KumaColors.TextMuted
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // 6. Danger Zone & Reset
                item {
                    KumaCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = KumaColors.SurfaceContainerLow,
                        cornerRadius = 16.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { settingsRepository.resetAll() },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RestartAlt,
                                        contentDescription = null,
                                        tint = Color(0xFFFF5C5C),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Reset Semua Pengaturan",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFFF5C5C)
                                            )
                                        )
                                        Text(
                                            text = "Kembalikan ke setelan bawaan awal",
                                            style = MaterialTheme.typography.labelSmall.copy(color = KumaColors.TextMuted)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    KumaCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = KumaColors.SurfaceContainerLow,
        cornerRadius = 16.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = KumaColors.ElectricLime,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = KumaColors.ElectricLime,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun LanguageOptionRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) KumaColors.SurfaceContainerHigh else KumaColors.SurfaceContainer)
            .border(
                1.dp,
                if (isSelected) KumaColors.ElectricLime else KumaColors.BorderDefault,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) KumaColors.ElectricLime else KumaColors.TextWhite
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(color = KumaColors.TextMuted)
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = KumaColors.ElectricLime,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun UnitSelectChip(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(PillShape)
            .background(if (isSelected) KumaColors.ElectricLime else KumaColors.SurfaceContainer)
            .border(
                1.dp,
                if (isSelected) KumaColors.ElectricLime else KumaColors.BorderDefault,
                PillShape
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = if (isSelected) KumaColors.OnElectricLime else KumaColors.TextWhite
            )
        )
    }
}
