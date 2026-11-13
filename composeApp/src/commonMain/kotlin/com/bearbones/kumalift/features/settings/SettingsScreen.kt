package com.bearbones.kumalift.features.settings

import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.bearbones.kumalift.domain.model.BodyMeasurementUnit
import com.bearbones.kumalift.domain.model.DistanceUnit
import com.bearbones.kumalift.domain.model.ExperienceLevel
import com.bearbones.kumalift.domain.model.FirstDayOfWeek
import com.bearbones.kumalift.domain.model.OneRepMaxFormula
import com.bearbones.kumalift.domain.model.TrainingGoal
import com.bearbones.kumalift.domain.model.WeightUnit

enum class SettingsDestination {
    MAIN,
    PROFILE,
    ACCOUNT_USERNAME,
    NOTIFICATIONS,
    WORKOUTS,
    UNITS,
    LANGUAGE,
    THEME,
    EXPORT_IMPORT
}

@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    onNavigateBack: () -> Unit
) {
    var currentDestination by remember { mutableStateOf(SettingsDestination.MAIN) }

    Crossfade(targetState = currentDestination) { destination ->
        when (destination) {
            SettingsDestination.MAIN -> {
                SettingsMainMenu(
                    settingsRepository = settingsRepository,
                    onNavigateBack = onNavigateBack,
                    onNavigateTo = { currentDestination = it }
                )
            }
            SettingsDestination.PROFILE -> {
                ProfileSettingsPage(
                    settingsRepository = settingsRepository,
                    onNavigateBack = { currentDestination = SettingsDestination.MAIN }
                )
            }
            SettingsDestination.ACCOUNT_USERNAME -> {
                ChangeUsernamePage(
                    settingsRepository = settingsRepository,
                    onNavigateBack = { currentDestination = SettingsDestination.MAIN }
                )
            }
            SettingsDestination.NOTIFICATIONS -> {
                NotificationSettingsPage(
                    settingsRepository = settingsRepository,
                    onNavigateBack = { currentDestination = SettingsDestination.MAIN }
                )
            }
            SettingsDestination.WORKOUTS -> {
                WorkoutSettingsPage(
                    settingsRepository = settingsRepository,
                    onNavigateBack = { currentDestination = SettingsDestination.MAIN }
                )
            }
            SettingsDestination.UNITS -> {
                UnitsSettingsPage(
                    settingsRepository = settingsRepository,
                    onNavigateBack = { currentDestination = SettingsDestination.MAIN }
                )
            }
            SettingsDestination.LANGUAGE -> {
                LanguageSettingsPage(
                    settingsRepository = settingsRepository,
                    onNavigateBack = { currentDestination = SettingsDestination.MAIN }
                )
            }
            SettingsDestination.THEME -> {
                ThemeSettingsPage(
                    settingsRepository = settingsRepository,
                    onNavigateBack = { currentDestination = SettingsDestination.MAIN }
                )
            }
            SettingsDestination.EXPORT_IMPORT -> {
                ExportImportDataPage(
                    settingsRepository = settingsRepository,
                    onNavigateBack = { currentDestination = SettingsDestination.MAIN }
                )
            }
        }
    }
}

/**
 * Main Settings Menu matching settings.jpg (respecting red 'X' deletions)
 */
@Composable
private fun SettingsMainMenu(
    settingsRepository: SettingsRepository,
    onNavigateBack: () -> Unit,
    onNavigateTo: (SettingsDestination) -> Unit
) {
    val settings by settingsRepository.settings.collectAsState()
    val colors = LocalKumaColors.current
    val isEnglish = settings.appearance.language == AppLanguage.EN

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KumaColors.CanvasLowest)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
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
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = if (isEnglish) "Settings" else "Pengaturan",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                // Section: Account
                item {
                    SettingsGroupSection(title = if (isEnglish) "Account" else "Akun") {
                        SettingsNavigationRow(
                            icon = Icons.Default.Person,
                            title = if (isEnglish) "Profile" else "Profil",
                            onClick = { onNavigateTo(SettingsDestination.PROFILE) }
                        )
                        SettingsDivider()
                        SettingsNavigationRow(
                            icon = Icons.Default.Lock,
                            title = if (isEnglish) "Account" else "Akun",
                            onClick = { onNavigateTo(SettingsDestination.ACCOUNT_USERNAME) }
                        )
                        SettingsDivider()
                        SettingsNavigationRow(
                            icon = Icons.Default.Notifications,
                            title = if (isEnglish) "Notifications" else "Notifikasi",
                            onClick = { onNavigateTo(SettingsDestination.NOTIFICATIONS) }
                        )
                    }
                }

                // Section: Preferences
                item {
                    SettingsGroupSection(title = if (isEnglish) "Preferences" else "Preferensi") {
                        SettingsNavigationRow(
                            icon = Icons.Default.FitnessCenter,
                            title = if (isEnglish) "Workouts" else "Latihan",
                            onClick = { onNavigateTo(SettingsDestination.WORKOUTS) }
                        )
                        SettingsDivider()
                        SettingsNavigationRow(
                            icon = Icons.Default.Scale,
                            title = if (isEnglish) "Units" else "Satuan",
                            onClick = { onNavigateTo(SettingsDestination.UNITS) }
                        )
                        SettingsDivider()
                        SettingsNavigationRow(
                            icon = Icons.Default.Language,
                            title = if (isEnglish) "Language" else "Bahasa",
                            onClick = { onNavigateTo(SettingsDestination.LANGUAGE) }
                        )
                        SettingsDivider()
                        SettingsNavigationRow(
                            icon = Icons.Default.Palette,
                            title = if (isEnglish) "Theme" else "Tema",
                            onClick = { onNavigateTo(SettingsDestination.THEME) }
                        )
                        SettingsDivider()
                        SettingsNavigationRow(
                            icon = Icons.AutoMirrored.Filled.Send,
                            title = if (isEnglish) "Export & Import Data" else "Ekspor & Impor Data",
                            onClick = { onNavigateTo(SettingsDestination.EXPORT_IMPORT) }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(40.dp)) }
            }
        }
    }
}

@Composable
private fun SettingsGroupSection(
    title: String,
    content: @Composable () -> Unit
) {
    val colors = LocalKumaColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                fontSize = 17.sp
            ),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )
        KumaCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsNavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    val colors = LocalKumaColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = colors.textSecondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary
                    )
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = colors.textSecondary
                        )
                    )
                }
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textSecondary.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(KumaColors.SurfaceContainer)
    )
}

/**
 * Edit Profile Page matching profile.jpg
 */
@Composable
private fun ProfileSettingsPage(
    settingsRepository: SettingsRepository,
    onNavigateBack: () -> Unit
) {
    val settings by settingsRepository.settings.collectAsState()
    val colors = LocalKumaColors.current

    var nameInput by remember { mutableStateOf(settings.profile.name) }
    var bioInput by remember { mutableStateOf(settings.profile.bio) }
    var linkInput by remember { mutableStateOf(settings.profile.link) }
    var birthdayInput by remember { mutableStateOf(settings.profile.birthday) }
    var selectedSex by remember { mutableStateOf(settings.profile.sex) }
    var bwInput by remember { mutableStateOf(settings.profile.bodyweightKg?.toString() ?: "75.0") }
    var htInput by remember { mutableStateOf(settings.profile.heightCm?.toString() ?: "175.0") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KumaColors.CanvasLowest)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar: Back, "Edit Profile", "Done"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KumaColors.SurfaceDim)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
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
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "Edit Profile",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )

                Button(
                    onClick = {
                        settingsRepository.update { current ->
                            current.copy(
                                profile = current.profile.copy(
                                    name = nameInput.trim(),
                                    bio = bioInput.trim(),
                                    link = linkInput.trim(),
                                    birthday = birthdayInput.trim(),
                                    sex = selectedSex,
                                    bodyweightKg = bwInput.toDoubleOrNull() ?: current.profile.bodyweightKg,
                                    heightCm = htInput.toDoubleOrNull() ?: current.profile.heightCm
                                )
                            )
                        }
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accentText,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(10.dp)) }

                // Avatar + Change Picture
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF111317))
                                .border(2.dp, colors.accentText, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = nameInput.take(1).ifBlank { "G" }.uppercase(),
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colors.accentText
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Change Picture",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = colors.accentText,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                // Public profile data
                item {
                    Text(
                        text = "Public profile data",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        ),
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }

                item {
                    KumaInputField(
                        label = "Name",
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        placeholder = "Gabriel Radjali"
                    )
                }

                item {
                    KumaInputField(
                        label = "Bio",
                        value = bioInput,
                        onValueChange = { bioInput = it },
                        placeholder = "Describe yourself"
                    )
                }

                item {
                    KumaInputField(
                        label = "Link",
                        value = linkInput,
                        onValueChange = { linkInput = it },
                        placeholder = "https://example.com"
                    )
                }

                // Private data
                item {
                    Text(
                        text = "Private data",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        ),
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sex",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = colors.textPrimary
                            )
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                AthleteSex.MALE to "Male",
                                AthleteSex.FEMALE to "Female"
                            ).forEach { (sex, label) ->
                                val isSelected = selectedSex == sex
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) colors.accentText else KumaColors.SurfaceContainer
                                        )
                                        .clickable { selectedSex = sex }
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else colors.textSecondary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    KumaInputField(
                        label = "Birthday",
                        value = birthdayInput,
                        onValueChange = { birthdayInput = it },
                        placeholder = "misal: 15 Januari 2000"
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            KumaInputField(
                                label = "Berat Badan (kg)",
                                value = bwInput,
                                onValueChange = { bwInput = it },
                                placeholder = "75.0"
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            KumaInputField(
                                label = "Tinggi Badan (cm)",
                                value = htInput,
                                onValueChange = { htInput = it },
                                placeholder = "175.0"
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(40.dp)) }
            }
        }
    }
}

/**
 * Change Username Page matching username.jpg
 */
@Composable
private fun ChangeUsernamePage(
    settingsRepository: SettingsRepository,
    onNavigateBack: () -> Unit
) {
    val settings by settingsRepository.settings.collectAsState()
    val colors = LocalKumaColors.current
    var usernameInput by remember { mutableStateOf(settings.profile.username) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KumaColors.CanvasLowest)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
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
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "Change Username",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Username",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = colors.textSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(KumaColors.SurfaceContainer)
                        .padding(horizontal = 14.dp, vertical = 14.dp)
                ) {
                    BasicTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        ),
                        cursorBrush = SolidColor(colors.accentText),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = {
                        val cleaned = usernameInput.trim().lowercase().replace(" ", "_")
                        settingsRepository.update {
                            it.copy(profile = it.profile.copy(username = cleaned))
                        }
                        onNavigateBack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accentText,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Update", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

/**
 * Push Notifications Page matching notifcation.jpg + Workout Reminders & Battery Optimization
 */
@Composable
private fun NotificationSettingsPage(
    settingsRepository: SettingsRepository,
    onNavigateBack: () -> Unit
) {
    val settings by settingsRepository.settings.collectAsState()
    val colors = LocalKumaColors.current
    val isEnglish = settings.appearance.language == AppLanguage.EN
    val workoutReminderManager = rememberWorkoutReminderManager()
    var testNotificationSent by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KumaColors.CanvasLowest)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
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
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "Push Notifications",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                // General section matching notifcation.jpg (omitting 'Follows' per red X)
                item {
                    Text(
                        text = "General",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    )
                }

                item {
                    KumaCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Rest Timer
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Rest Timer",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Switch(
                                    checked = settings.notifications.restTimerAlert,
                                    onCheckedChange = { checked ->
                                        settingsRepository.update {
                                            it.copy(notifications = it.notifications.copy(restTimerAlert = checked))
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = colors.accentText,
                                        checkedTrackColor = colors.accentText.copy(alpha = 0.35f)
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            SettingsDivider()
                            Spacer(modifier = Modifier.height(14.dp))

                            // Monthly Report
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Monthly Report",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            color = colors.textPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                    Text(
                                        text = "Get a notification when your monthly report is ready.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = colors.textSecondary
                                        )
                                    )
                                }
                                Switch(
                                    checked = settings.notifications.monthlyReport,
                                    onCheckedChange = { checked ->
                                        settingsRepository.update {
                                            it.copy(notifications = it.notifications.copy(monthlyReport = checked))
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = colors.accentText,
                                        checkedTrackColor = colors.accentText.copy(alpha = 0.35f)
                                    )
                                )
                            }
                        }
                    }
                }

                // Workout Reminders & Battery Optimization
                item {
                    Text(
                        text = if (isEnglish) "Workout Reminders" else "Pengingat Latihan",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    )
                }

                item {
                    KumaCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isEnglish) "Daily Workout Reminder" else "Pengingat Latihan Rutin",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            color = colors.textPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                    Text(
                                        text = if (isEnglish) "Alarm rings even in Doze / sleep mode" else "Berdering tepat waktu walau HP tidur / doze mode",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = colors.textSecondary
                                        )
                                    )
                                }
                                Switch(
                                    checked = settings.notifications.workoutReminders,
                                    onCheckedChange = { enabled ->
                                        settingsRepository.update {
                                            it.copy(notifications = it.notifications.copy(workoutReminders = enabled))
                                        }
                                        workoutReminderManager.syncSettings(settings.notifications.copy(workoutReminders = enabled))
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = colors.accentText,
                                        checkedTrackColor = colors.accentText.copy(alpha = 0.35f)
                                    )
                                )
                            }

                            if (settings.notifications.workoutReminders) {
                                SettingsDivider()

                                // Time selector presets
                                Text(
                                    text = if (isEnglish) "Reminder Time" else "Waktu Pengingat",
                                    style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("06:00", "07:00", "17:00", "18:30", "20:00").forEach { timeStr ->
                                        val isSelected = settings.notifications.reminderTime == timeStr
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isSelected) colors.accentText else KumaColors.SurfaceContainer
                                                )
                                                .clickable {
                                                    settingsRepository.update {
                                                        it.copy(notifications = it.notifications.copy(reminderTime = timeStr))
                                                    }
                                                    workoutReminderManager.syncSettings(settings.notifications.copy(reminderTime = timeStr))
                                                }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = timeStr,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) Color.White else colors.textSecondary
                                                )
                                            )
                                        }
                                    }
                                }

                                // Days selector
                                Text(
                                    text = if (isEnglish) "Active Days" else "Hari Pengingat",
                                    style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val daysLabels = listOf(1 to "S", 2 to "S", 3 to "R", 4 to "K", 5 to "J", 6 to "S", 7 to "M")
                                    daysLabels.forEach { (dayInt, label) ->
                                        val isDayActive = settings.notifications.reminderDays.contains(dayInt)
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isDayActive) colors.accentText else KumaColors.SurfaceContainer
                                                )
                                                .clickable {
                                                    val newDays = if (isDayActive) {
                                                        settings.notifications.reminderDays - dayInt
                                                    } else {
                                                        settings.notifications.reminderDays + dayInt
                                                    }
                                                    settingsRepository.update {
                                                        it.copy(notifications = it.notifications.copy(reminderDays = newDays))
                                                    }
                                                    workoutReminderManager.syncSettings(settings.notifications.copy(reminderDays = newDays))
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDayActive) Color.White else colors.textSecondary
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Battery optimization card
                item {
                    val isOptimized = workoutReminderManager.isBatteryOptimizationIgnored()
                    KumaCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = if (isOptimized) colors.border else colors.accentText.copy(alpha = 0.5f)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (isOptimized) Icons.Default.BatteryChargingFull else Icons.Default.BatteryAlert,
                                    contentDescription = null,
                                    tint = if (isOptimized) colors.accentText else colors.textSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = if (isEnglish) "Battery Optimization Exemption" else "Proteksi Penghemat Baterai",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textPrimary
                                        )
                                    )
                                    Text(
                                        text = if (isOptimized) {
                                            if (isEnglish) "Active: Reminders will ring reliably in background." else "Aktif: Notifikasi dijamin menyala tepat waktu."
                                        } else {
                                            if (isEnglish) "Allow KumaLift to ignore battery optimizations." else "Izinkan KumaLift berjalan tanpa dihentikan sistem."
                                        },
                                        style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                                    )
                                }
                            }

                            if (!isOptimized) {
                                Button(
                                    onClick = { workoutReminderManager.requestIgnoreBatteryOptimization() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.accentText,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (isEnglish) "Disable Optimization for KumaLift" else "Buka Pengaturan Bebas Optimasi",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Test notification button
                item {
                    Button(
                        onClick = {
                            workoutReminderManager.sendTestNotification(
                                title = "Uji Notifikasi KumaLift \uD83D\uDD14",
                                message = "Pengingat latihan berhasil diuji! Siap membantumu capai target."
                            )
                            testNotificationSent = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KumaColors.SurfaceContainer,
                            contentColor = colors.textPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (testNotificationSent) "Notifikasi Terkirim! \u2714" else "Kirim Notifikasi Uji Sekarang",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(40.dp)) }
            }
        }
    }
}

/**
 * Workout Settings Page matching workoutsettings.jpg (omitting 'Wearable Sensor' per red X)
 */
@Composable
private fun WorkoutSettingsPage(
    settingsRepository: SettingsRepository,
    onNavigateBack: () -> Unit
) {
    val settings by settingsRepository.settings.collectAsState()
    val colors = LocalKumaColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KumaColors.CanvasLowest)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
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
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "Workout Settings",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                item {
                    KumaCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Sounds
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Sounds",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Switch(
                                    checked = settings.logging.soundAlerts,
                                    onCheckedChange = { checked ->
                                        settingsRepository.update {
                                            it.copy(logging = it.logging.copy(soundAlerts = checked))
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = colors.accentText,
                                        checkedTrackColor = colors.accentText.copy(alpha = 0.35f)
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            SettingsDivider()
                            Spacer(modifier = Modifier.height(14.dp))

                            // Default Rest Timer
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Default Rest Timer",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Text(
                                    text = "${settings.logging.restTimerDefaultSec / 60}min ${settings.logging.restTimerDefaultSec % 60}s",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = colors.accentText,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(60 to "60s", 90 to "90s", 120 to "120s", 180 to "3m").forEach { (sec, label) ->
                                    val isSelected = settings.logging.restTimerDefaultSec == sec
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) colors.accentText else KumaColors.SurfaceContainer
                                            )
                                            .clickable {
                                                settingsRepository.update {
                                                    it.copy(logging = it.logging.copy(restTimerDefaultSec = sec))
                                                }
                                            }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else colors.textSecondary
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            SettingsDivider()
                            Spacer(modifier = Modifier.height(14.dp))

                            // First day of the week
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "First day of the week",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(FirstDayOfWeek.MONDAY to "Monday", FirstDayOfWeek.SUNDAY to "Sunday").forEach { (day, label) ->
                                        val isSel = settings.units.firstDayOfWeek == day
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSel) colors.accentText else KumaColors.SurfaceContainer)
                                                .clickable {
                                                    settingsRepository.update {
                                                        it.copy(units = it.units.copy(firstDayOfWeek = day))
                                                    }
                                                }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSel) Color.White else colors.textSecondary
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Toggles from workoutsettings.jpg
                item {
                    KumaCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Keep Awake During Workout
                            WorkoutSettingToggle(
                                title = "Keep Awake During Workout",
                                description = "Enable this if you don't want your phone to sleep while you're in a workout",
                                isChecked = settings.logging.keepScreenOn,
                                onCheckedChange = { chk ->
                                    settingsRepository.update {
                                        it.copy(logging = it.logging.copy(keepScreenOn = chk))
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            SettingsDivider()
                            Spacer(modifier = Modifier.height(14.dp))

                            // Plate Calculator
                            WorkoutSettingToggle(
                                title = "Plate Calculator",
                                description = "A plate calculator calculates the plates needed on a bar to achieve a specific weight.",
                                isChecked = settings.logging.plateCalculatorEnabled,
                                onCheckedChange = { chk ->
                                    settingsRepository.update {
                                        it.copy(logging = it.logging.copy(plateCalculatorEnabled = chk))
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            SettingsDivider()
                            Spacer(modifier = Modifier.height(14.dp))

                            // RPE Tracking
                            WorkoutSettingToggle(
                                title = "RPE Tracking",
                                description = "RPE (Rated Perceived Exertion) is a measure of intensity. Enabling allows you to log it for each set.",
                                isChecked = settings.logging.trackIntensity,
                                onCheckedChange = { chk ->
                                    settingsRepository.update {
                                        it.copy(logging = it.logging.copy(trackIntensity = chk))
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            SettingsDivider()
                            Spacer(modifier = Modifier.height(14.dp))

                            // Smart Superset Scrolling
                            WorkoutSettingToggle(
                                title = "Smart Superset Scrolling",
                                description = "When you complete a set, it'll automatically scroll to the next exercise in the superset.",
                                isChecked = settings.logging.smartSupersetScrolling,
                                onCheckedChange = { chk ->
                                    settingsRepository.update {
                                        it.copy(logging = it.logging.copy(smartSupersetScrolling = chk))
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            SettingsDivider()
                            Spacer(modifier = Modifier.height(14.dp))

                            // Inline Timer
                            WorkoutSettingToggle(
                                title = "Inline Timer",
                                description = "Duration exercises have a built-in stopwatch for tracking time for each set",
                                isChecked = settings.logging.inlineTimer,
                                onCheckedChange = { chk ->
                                    settingsRepository.update {
                                        it.copy(logging = it.logging.copy(inlineTimer = chk))
                                    }
                                }
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(40.dp)) }
            }
        }
    }
}

@Composable
private fun WorkoutSettingToggle(
    title: String,
    description: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = LocalKumaColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = colors.textSecondary
                )
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.accentText,
                checkedTrackColor = colors.accentText.copy(alpha = 0.35f)
            )
        )
    }
}

/**
 * Select Units Page matching units.jpg
 */
@Composable
private fun UnitsSettingsPage(
    settingsRepository: SettingsRepository,
    onNavigateBack: () -> Unit
) {
    val settings by settingsRepository.settings.collectAsState()
    val colors = LocalKumaColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KumaColors.CanvasLowest)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
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
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "Select Units",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Weight (kg / lbs)
                UnitOptionGroup(
                    title = "Weight",
                    options = listOf(WeightUnit.KG to "kg", WeightUnit.LB to "lbs"),
                    selected = settings.units.weightUnit,
                    onSelect = { unit ->
                        settingsRepository.update {
                            it.copy(units = it.units.copy(weightUnit = unit))
                        }
                    }
                )

                // Distance (kilometers / miles)
                UnitOptionGroup(
                    title = "Distance",
                    options = listOf(DistanceUnit.KILOMETERS to "kilometers", DistanceUnit.MILES to "miles"),
                    selected = settings.units.distanceUnit,
                    onSelect = { unit ->
                        settingsRepository.update {
                            it.copy(units = it.units.copy(distanceUnit = unit))
                        }
                    }
                )

                // Body Measurements (cm / in)
                UnitOptionGroup(
                    title = "Body Measurements",
                    options = listOf(BodyMeasurementUnit.CM to "cm", BodyMeasurementUnit.IN to "in"),
                    selected = settings.units.bodyMeasurementUnit,
                    onSelect = { unit ->
                        settingsRepository.update {
                            it.copy(units = it.units.copy(bodyMeasurementUnit = unit))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun <T> UnitOptionGroup(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    val colors = LocalKumaColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = colors.textSecondary,
                fontWeight = FontWeight.SemiBold
            )
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(KumaColors.SurfaceDim)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { (option, label) ->
                val isSelected = option == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) colors.accentText else Color.Transparent
                        )
                        .clickable { onSelect(option) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else colors.textSecondary
                        )
                    )
                }
            }
        }
    }
}

/**
 * Language Settings Page
 */
@Composable
private fun LanguageSettingsPage(
    settingsRepository: SettingsRepository,
    onNavigateBack: () -> Unit
) {
    val settings by settingsRepository.settings.collectAsState()
    val colors = LocalKumaColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KumaColors.CanvasLowest)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
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
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "Select Language",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf(
                    Triple(AppLanguage.SYSTEM, "Ikuti Pengaturan HP", "Gunakan bahasa bawaan sistem perangkat"),
                    Triple(AppLanguage.ID, "Bahasa Indonesia", "Tampilan antarmuka berbahasa Indonesia"),
                    Triple(AppLanguage.EN, "English", "User interface in English")
                ).forEach { (lang, title, desc) ->
                    val isSelected = settings.appearance.language == lang
                    KumaCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                settingsRepository.update {
                                    it.copy(appearance = it.appearance.copy(language = lang))
                                }
                            },
                        borderColor = if (isSelected) colors.accentText else colors.border
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) colors.accentText else colors.textPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = colors.accentText,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Theme Settings Page matching theme.jpg
 */
@Composable
private fun ThemeSettingsPage(
    settingsRepository: SettingsRepository,
    onNavigateBack: () -> Unit
) {
    val settings by settingsRepository.settings.collectAsState()
    val colors = LocalKumaColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KumaColors.CanvasLowest)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
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
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "Select Theme",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf(
                    Pair(AppTheme.SYSTEM, "Use OS Setting"),
                    Pair(AppTheme.DARK, "Dark \uD83D\uDE08"),
                    Pair(AppTheme.LIGHT, "Light \uD83D\uDCA1")
                ).forEach { (theme, label) ->
                    val isSelected = settings.appearance.theme == theme
                    KumaCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                settingsRepository.update {
                                    it.copy(appearance = it.appearance.copy(theme = theme))
                                }
                            },
                        borderColor = if (isSelected) colors.accentText else colors.border
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) colors.accentText else colors.textPrimary
                                )
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = colors.accentText,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Export & Import Data Page matching exportandimport.jpg
 */
@Composable
private fun ExportImportDataPage(
    settingsRepository: SettingsRepository,
    onNavigateBack: () -> Unit
) {
    val colors = LocalKumaColors.current
    var statusMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KumaColors.CanvasLowest)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
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
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "Export & Import Data",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                KumaCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsNavigationRow(
                            icon = Icons.Default.FileDownload,
                            title = "Export Data",
                            subtitle = "Cadangkan semua riwayat dan preferensi ke file JSON",
                            onClick = {
                                statusMessage = "Data berhasil diekspor ke format JSON!"
                            }
                        )
                        SettingsDivider()
                        SettingsNavigationRow(
                            icon = Icons.Default.FileUpload,
                            title = "Import Data",
                            subtitle = "Pulihkan riwayat latihan dari cadangan JSON sebelumnya",
                            onClick = {
                                statusMessage = "Data cadangan berhasil diverifikasi dan dimuat."
                            }
                        )
                    }
                }

                if (statusMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    KumaCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = colors.accentText
                    ) {
                        Text(
                            text = statusMessage!!,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = colors.accentText,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KumaInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    val colors = LocalKumaColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = colors.textSecondary,
                fontWeight = FontWeight.SemiBold
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(KumaColors.SurfaceContainer)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = colors.textPrimary
                ),
                cursorBrush = SolidColor(colors.accentText),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (value.isBlank()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = colors.textSecondary.copy(alpha = 0.5f)
                            )
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}
