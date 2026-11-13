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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumalift.core.designsystem.KumaCard
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.core.notification.rememberWorkoutReminderManager
import com.bearbones.kumalift.data.repository.SettingsRepository
import com.bearbones.kumalift.domain.model.AppLanguage
import com.bearbones.kumalift.domain.model.AppTheme
import com.bearbones.kumalift.domain.model.AthleteSex
import com.bearbones.kumalift.domain.model.ExperienceLevel
import com.bearbones.kumalift.domain.model.OneRepMaxFormula
import com.bearbones.kumalift.domain.model.TrainingGoal
import com.bearbones.kumalift.domain.model.WeightUnit

@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    onNavigateBack: () -> Unit
) {
    val settings by settingsRepository.settings.collectAsState()
    val isEnglish = settings.appearance.language == AppLanguage.EN
    val workoutReminderManager = rememberWorkoutReminderManager()

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
                    text = if (isEnglish) "Settings" else "Pengaturan",
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
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // 1. Athlete Profile Editor (Full Spec 3.1)
                item {
                    SettingsSection(
                        title = if (isEnglish) "ATHLETE PROFILE" else "PROFIL ATLET",
                        icon = Icons.Default.Person
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            // Name Input
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = if (isEnglish) "Name" else "Nama Panggilan",
                                    style = MaterialTheme.typography.labelSmall.copy(color = KumaColors.TextMuted)
                                )
                                SettingsInputField(
                                    value = settings.profile.name,
                                    onValueChange = { newName ->
                                        settingsRepository.update {
                                            it.copy(profile = it.profile.copy(name = newName))
                                        }
                                    },
                                    placeholder = if (isEnglish) "e.g. Alex" else "misal: Alex",
                                    keyboardType = KeyboardType.Text
                                )
                            }

                            // Body weight & Height in 1 Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Body Weight
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (isEnglish) "Body Weight (kg)" else "Berat Badan (kg)",
                                        style = MaterialTheme.typography.labelSmall.copy(color = KumaColors.TextMuted)
                                    )
                                    SettingsInputField(
                                        value = settings.profile.bodyweightKg?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "",
                                        onValueChange = { raw ->
                                            val parsed = raw.toDoubleOrNull()
                                            settingsRepository.update {
                                                it.copy(profile = it.profile.copy(bodyweightKg = parsed))
                                            }
                                        },
                                        placeholder = "75.0",
                                        keyboardType = KeyboardType.Decimal
                                    )
                                }

                                // Height
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (isEnglish) "Height (cm)" else "Tinggi Badan (cm)",
                                        style = MaterialTheme.typography.labelSmall.copy(color = KumaColors.TextMuted)
                                    )
                                    SettingsInputField(
                                        value = settings.profile.heightCm?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "",
                                        onValueChange = { raw ->
                                            val parsed = raw.toDoubleOrNull()
                                            settingsRepository.update {
                                                it.copy(profile = it.profile.copy(heightCm = parsed))
                                            }
                                        },
                                        placeholder = "175",
                                        keyboardType = KeyboardType.Decimal
                                    )
                                }
                            }

                            // Training Days Per Week (1..7 Stepper)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isEnglish) "Training Days per Week" else "Target Latihan Mingguan",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = KumaColors.TextWhite
                                        )
                                    )
                                    Text(
                                        text = if (isEnglish) "Target on Home screen" else "Digunakan untuk target kalender beranda",
                                        style = MaterialTheme.typography.labelSmall.copy(color = KumaColors.TextMuted)
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(KumaColors.SurfaceContainer)
                                            .clickable {
                                                if (settings.profile.trainingDaysPerWeek > 1) {
                                                    settingsRepository.update {
                                                        it.copy(profile = it.profile.copy(trainingDaysPerWeek = it.profile.trainingDaysPerWeek - 1))
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Remove,
                                            contentDescription = "Kurang",
                                            tint = KumaColors.OnSurface,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Text(
                                        text = "${settings.profile.trainingDaysPerWeek} ${if (isEnglish) "days" else "hari"}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = LocalKumaColors.current.accentText
                                        ),
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(KumaColors.SurfaceContainer)
                                            .clickable {
                                                if (settings.profile.trainingDaysPerWeek < 7) {
                                                    settingsRepository.update {
                                                        it.copy(profile = it.profile.copy(trainingDaysPerWeek = it.profile.trainingDaysPerWeek + 1))
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Tambah",
                                            tint = KumaColors.OnSurface,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            // Training Goal
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = if (isEnglish) "Training Goal" else "Fokus & Target Latihan",
                                    style = MaterialTheme.typography.labelSmall.copy(color = KumaColors.TextMuted)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        TrainingGoal.HYPERTROPHY to (if (isEnglish) "Hypertrophy" else "Hipertrofi"),
                                        TrainingGoal.STRENGTH to (if (isEnglish) "Strength" else "Kekuatan"),
                                        TrainingGoal.GENERAL to (if (isEnglish) "General" else "Umum")
                                    ).forEach { (goal, label) ->
                                        val isSelected = settings.profile.goal == goal
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSelected) KumaColors.ElectricLime else KumaColors.SurfaceContainer)
                                                .clickable {
                                                    settingsRepository.update {
                                                        it.copy(profile = it.profile.copy(goal = goal))
                                                    }
                                                }
                                                .padding(vertical = 8.dp),
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

                            // Experience Level
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = if (isEnglish) "Experience Level" else "Tingkat Pengalaman",
                                    style = MaterialTheme.typography.labelSmall.copy(color = KumaColors.TextMuted)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        ExperienceLevel.BEGINNER to (if (isEnglish) "Beginner" else "Pemula"),
                                        ExperienceLevel.INTERMEDIATE to (if (isEnglish) "Intermediate" else "Menengah"),
                                        ExperienceLevel.ADVANCED to (if (isEnglish) "Advanced" else "Lanjut")
                                    ).forEach { (level, label) ->
                                        val isSelected = settings.profile.experienceLevel == level
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSelected) KumaColors.ElectricLime else KumaColors.SurfaceContainer)
                                                .clickable {
                                                    settingsRepository.update {
                                                        it.copy(profile = it.profile.copy(experienceLevel = level))
                                                    }
                                                }
                                                .padding(vertical = 8.dp),
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

                            // Sex
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = if (isEnglish) "Sex" else "Jenis Kelamin",
                                    style = MaterialTheme.typography.labelSmall.copy(color = KumaColors.TextMuted)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        AthleteSex.MALE to (if (isEnglish) "Male" else "Pria"),
                                        AthleteSex.FEMALE to (if (isEnglish) "Female" else "Wanita"),
                                        AthleteSex.UNSPECIFIED to (if (isEnglish) "Unspecified" else "Rahasia")
                                    ).forEach { (sex, label) ->
                                        val isSelected = settings.profile.sex == sex
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSelected) KumaColors.ElectricLime else KumaColors.SurfaceContainer)
                                                .clickable {
                                                    settingsRepository.update {
                                                        it.copy(profile = it.profile.copy(sex = sex))
                                                    }
                                                }
                                                .padding(vertical = 8.dp),
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
                    }
                }

                // 2. Language Setting (Instantly switches UI language)
                item {
                    SettingsSection(
                        title = if (isEnglish) "LANGUAGE / BAHASA" else "BAHASA / LANGUAGE",
                        icon = Icons.Default.Language
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LanguageOptionRow(
                                title = if (isEnglish) "System Default" else "Ikuti Pengaturan HP",
                                subtitle = if (isEnglish) "Follow device system language" else "Gunakan bahasa bawaan sistem perangkat",
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

                // 3. Units & Formats
                item {
                    SettingsSection(
                        title = if (isEnglish) "UNITS & FORMATS" else "SATUAN BEBAN & FORMAT",
                        icon = Icons.Default.Scale
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                }

                // 4. Timer & Logging
                item {
                    SettingsSection(
                        title = if (isEnglish) "WORKOUT LOGGING & TIMER" else "LATIHAN & REST TIMER",
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
                                        text = if (isEnglish) "Auto-start Rest Timer" else "Mulai Timer Otomatis",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = KumaColors.TextWhite
                                        )
                                    )
                                    Text(
                                        text = if (isEnglish) "Starts rest timer after completing a set" else "Timer istirahat jalan setelah set dicentang",
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
                                text = "${if (isEnglish) "Default Rest Time" else "Durasi Istirahat Bawaan"}: ${settings.logging.restTimerDefaultSec} ${if (isEnglish) "seconds" else "detik"}",
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
                                            text = "${sec}s",
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

                // 5. Workout Reminders & Notifications (Anti-Doze / Anti-Battery Optimization)
                item {
                    val isBatteryOptimized = !workoutReminderManager.isBatteryOptimizationIgnored()
                    var testNotifSent by remember { mutableStateOf(false) }

                    SettingsSection(
                        title = if (isEnglish) "WORKOUT REMINDERS & NOTIFICATIONS" else "PENGINGAT LATIHAN & NOTIFIKASI",
                        icon = Icons.Default.Notifications
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            // Main Toggle Switch: Pengingat Latihan
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isEnglish) "Workout Reminders" else "Pengingat Jadwal Latihan",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = KumaColors.current.textPrimary
                                        )
                                    )
                                    Text(
                                        text = if (isEnglish) "Alert you before your scheduled workout time" else "Kirim notifikasi di HP sebelum waktu latihan dimulai",
                                        style = MaterialTheme.typography.labelSmall.copy(color = KumaColors.current.textSubtle)
                                    )
                                }
                                Switch(
                                    checked = settings.notifications.workoutReminders,
                                    onCheckedChange = { checked ->
                                        val updated = settings.notifications.copy(workoutReminders = checked)
                                        settingsRepository.update { it.copy(notifications = updated) }
                                        workoutReminderManager.syncSettings(updated)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = KumaColors.current.onAccentFill,
                                        checkedTrackColor = KumaColors.current.accentFill,
                                        uncheckedThumbColor = KumaColors.current.textSubtle,
                                        uncheckedTrackColor = KumaColors.current.surfaceBright
                                    )
                                )
                            }

                            if (settings.notifications.workoutReminders) {
                                // Reminder Time Selector
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "${if (isEnglish) "Reminder Time" else "Jam Pengingat"}: ${settings.notifications.reminderTime}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = KumaColors.current.textPrimary
                                        )
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("07:00", "16:00", "17:00", "18:00", "19:00", "20:00").forEach { time ->
                                            val isSelected = settings.notifications.reminderTime == time
                                            Box(
                                                modifier = Modifier
                                                    .clip(PillShape)
                                                    .background(if (isSelected) KumaColors.current.accentFill else KumaColors.current.surfaceVariant)
                                                    .border(
                                                        1.dp,
                                                        if (isSelected) (if (KumaColors.current.isDark) Color.Transparent else KumaColors.current.accentFillBorder) else KumaColors.current.border,
                                                        PillShape
                                                    )
                                                    .clickable {
                                                        val updated = settings.notifications.copy(reminderTime = time)
                                                        settingsRepository.update { it.copy(notifications = updated) }
                                                        workoutReminderManager.syncSettings(updated)
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = time,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSelected) KumaColors.current.onAccentFill else KumaColors.current.textPrimary
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }

                                // Days Multi-select
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = if (isEnglish) "Reminder Days" else "Hari Pengingat Latihan",
                                        style = MaterialTheme.typography.labelSmall.copy(color = KumaColors.current.textSubtle)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        val dayLabels = if (isEnglish) {
                                            listOf(1 to "M", 2 to "T", 3 to "W", 4 to "T", 5 to "F", 6 to "S", 7 to "S")
                                        } else {
                                            listOf(1 to "S", 2 to "S", 3 to "R", 4 to "K", 5 to "J", 6 to "S", 7 to "M")
                                        }
                                        dayLabels.forEach { (dayNum, label) ->
                                            val isSelected = settings.notifications.reminderDays.contains(dayNum)
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) KumaColors.current.accentFill else KumaColors.current.surfaceVariant)
                                                    .border(
                                                        1.dp,
                                                        if (isSelected) (if (KumaColors.current.isDark) Color.Transparent else KumaColors.current.accentFillBorder) else KumaColors.current.border,
                                                        CircleShape
                                                    )
                                                    .clickable {
                                                        val currentDays = settings.notifications.reminderDays.toMutableSet()
                                                        if (isSelected) {
                                                            if (currentDays.size > 1) currentDays.remove(dayNum)
                                                        } else {
                                                            currentDays.add(dayNum)
                                                        }
                                                        val updated = settings.notifications.copy(reminderDays = currentDays)
                                                        settingsRepository.update { it.copy(notifications = updated) }
                                                        workoutReminderManager.syncSettings(updated)
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) KumaColors.current.onAccentFill else KumaColors.current.textPrimary
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }

                                // Streak Reminder Switch
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isEnglish) "Streak Protector" else "Pengingat Streak Aktif",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = KumaColors.current.textPrimary
                                            )
                                        )
                                        Text(
                                            text = if (isEnglish) "Alert at 20:00 if streak is active and no workout logged" else "Ingatkan jam 20:00 jika streak aktif belum latihan hari ini",
                                            style = MaterialTheme.typography.labelSmall.copy(color = KumaColors.current.textSubtle)
                                        )
                                    }
                                    Switch(
                                        checked = settings.notifications.streakReminder,
                                        onCheckedChange = { checked ->
                                            val updated = settings.notifications.copy(streakReminder = checked)
                                            settingsRepository.update { it.copy(notifications = updated) }
                                            workoutReminderManager.syncSettings(updated)
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = KumaColors.current.onAccentFill,
                                            checkedTrackColor = KumaColors.current.accentFill,
                                            uncheckedThumbColor = KumaColors.current.textSubtle,
                                            uncheckedTrackColor = KumaColors.current.surfaceBright
                                        )
                                    )
                                }

                                // Battery Optimization Exemption Card (Anti-Killer)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isBatteryOptimized) KumaColors.current.surfaceBright else KumaColors.current.accentTint)
                                        .border(
                                            1.dp,
                                            if (isBatteryOptimized) KumaColors.current.borderStrong else KumaColors.current.accentFillBorder,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(14.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isBatteryOptimized) Icons.Default.BatteryAlert else Icons.Default.BatteryChargingFull,
                                                contentDescription = null,
                                                tint = if (isBatteryOptimized) KumaColors.current.accentText else KumaColors.current.accentTextOnTint,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = if (isBatteryOptimized) {
                                                    if (isEnglish) "Battery Optimization Active" else "Optimasi Baterai Masih Aktif"
                                                } else {
                                                    if (isEnglish) "Protected from Battery Optimization" else "Bebas dari Optimasi Baterai"
                                                },
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isBatteryOptimized) KumaColors.current.textPrimary else KumaColors.current.accentTextOnTint
                                                )
                                            )
                                        }
                                        Text(
                                            text = if (isBatteryOptimized) {
                                                if (isEnglish) {
                                                    "Android/OEM may delay or kill reminder alarms in Doze mode. Tap below to exempt KumaLift."
                                                } else {
                                                    "Sistem Android/OEM mungkin menunda notifikasi saat HP tidur/Doze. Ketuk tombol di bawah agar selalu tepat waktu."
                                                }
                                            } else {
                                                if (isEnglish) {
                                                    "KumaLift is unrestricted. Reminders will wake up the device and arrive on time."
                                                } else {
                                                    "KumaLift sudah bebas pembatasan. Pengingat akan membangunkan HP dan tiba tepat waktu."
                                                }
                                            },
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = if (isBatteryOptimized) KumaColors.current.textSecondary else KumaColors.current.accentTextOnTint,
                                                fontSize = 12.sp
                                            )
                                        )
                                        if (isBatteryOptimized) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(PillShape)
                                                    .background(KumaColors.current.accentFill)
                                                    .border(
                                                        1.dp,
                                                        if (KumaColors.current.isDark) Color.Transparent else KumaColors.current.accentFillBorder,
                                                        PillShape
                                                    )
                                                    .clickable {
                                                        workoutReminderManager.requestIgnoreBatteryOptimization()
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                                            ) {
                                                Text(
                                                    text = if (isEnglish) "Exempt from Battery Optimization" else "Bebaskan Dari Optimasi Baterai",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = KumaColors.current.onAccentFill
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }

                                // Send Test Notification Action
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(KumaColors.current.surfaceVariant)
                                        .border(1.dp, KumaColors.current.border, RoundedCornerShape(12.dp))
                                        .clickable {
                                            workoutReminderManager.sendTestNotification(
                                                title = if (isEnglish) "Time to Lift! 🏋️‍♂️" else "Waktunya Latihan! 🏋️‍♂️",
                                                message = if (isEnglish) {
                                                    "Test reminder received! KumaLift is ready to keep your streak going."
                                                } else {
                                                    "Notifikasi uji coba berhasil! KumaLift siap menjaga konsistensi latihanmu."
                                                }
                                            )
                                            testNotifSent = true
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = null,
                                            tint = KumaColors.current.accentText,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (testNotifSent) {
                                                if (isEnglish) "Test Notification Sent! ✓" else "Notifikasi Uji Coba Terkirim! ✓"
                                            } else {
                                                if (isEnglish) "Send Test Notification Now" else "Kirim Notifikasi Uji Coba Sekarang"
                                            },
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = KumaColors.current.accentText
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. Calculations (1RM Formula)
                item {
                    SettingsSection(
                        title = if (isEnglish) "ESTIMATED 1RM FORMULA" else "RUMUS ESTIMASI 1RM",
                        icon = Icons.Default.FitnessCenter
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OneRepMaxFormula.entries.forEach { formula ->
                                val isSelected = settings.calculations.oneRepMaxFormula == formula
                                val desc = when (formula) {
                                    OneRepMaxFormula.EPLEY -> "Epley: w × (1 + r/30) [Standard]"
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
                                            if (isSelected) (if (LocalKumaColors.current.isDark) LocalKumaColors.current.accentFill else LocalKumaColors.current.accentChart) else KumaColors.BorderDefault,
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
                                                    color = if (isSelected) LocalKumaColors.current.accentText else LocalKumaColors.current.textPrimary
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
                                                tint = LocalKumaColors.current.accentText,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. Appearance & Theme
                item {
                    SettingsSection(
                        title = if (isEnglish) "THEME & APPEARANCE" else "TEMA & TAMPILAN",
                        icon = Icons.Default.Palette
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                AppTheme.DARK to (if (isEnglish) "Dark" else "Gelap"),
                                AppTheme.LIGHT to (if (isEnglish) "Light" else "Terang"),
                                AppTheme.SYSTEM to (if (isEnglish) "System" else "Sistem")
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

                // 7. Danger Zone & Reset
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
                                        tint = LocalKumaColors.current.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = if (isEnglish) "Reset All Settings" else "Reset Semua Pengaturan",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = LocalKumaColors.current.error
                                            )
                                        )
                                        Text(
                                            text = if (isEnglish) "Restore factory default preferences" else "Kembalikan ke setelan bawaan awal",
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
private fun SettingsInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(KumaColors.SurfaceContainer)
            .border(1.dp, KumaColors.BorderDefault, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = KumaColors.TextMuted
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = KumaColors.TextWhite
            ),
            cursorBrush = SolidColor(KumaColors.ElectricLime),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth()
        )
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
                    tint = LocalKumaColors.current.accentText,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = LocalKumaColors.current.accentText,
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
                if (isSelected) (if (LocalKumaColors.current.isDark) LocalKumaColors.current.accentFill else LocalKumaColors.current.accentChart) else KumaColors.BorderDefault,
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
                        color = if (isSelected) LocalKumaColors.current.accentText else LocalKumaColors.current.textPrimary
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
                    tint = LocalKumaColors.current.accentText,
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
            .background(if (isSelected) LocalKumaColors.current.accentFill else KumaColors.SurfaceContainer)
            .border(
                1.dp,
                if (isSelected) (if (LocalKumaColors.current.isDark) Color.Transparent else LocalKumaColors.current.accentFillBorder) else KumaColors.BorderDefault,
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
                color = if (isSelected) LocalKumaColors.current.onAccentFill else LocalKumaColors.current.textPrimary
            )
        )
    }
}
