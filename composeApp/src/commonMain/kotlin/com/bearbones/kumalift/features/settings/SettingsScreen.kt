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
import com.bearbones.kumalift.core.data.rememberDataExportLauncher
import com.bearbones.kumalift.core.data.rememberDataImportLauncher
import com.bearbones.kumalift.core.util.currentEpochMillis
import com.bearbones.kumalift.data.export.DataBackupManager
import com.bearbones.kumalift.data.export.ImportMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material.icons.filled.Bluetooth
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
import com.bearbones.kumalift.core.media.ProfileAvatarMedia
import com.bearbones.kumalift.core.media.rememberProfileImagePicker
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
import com.bearbones.kumalift.domain.model.UserSettings

enum class SettingsDestination {
    MAIN,
    PROFILE,
    ACCOUNT_USERNAME,
    NOTIFICATIONS,
    WORKOUTS,
    UNITS,
    LANGUAGE,
    THEME,
    EXPORT_IMPORT,
    WEARABLES
}

@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    databaseRepo: com.bearbones.kumalift.data.local.KumaDatabaseRepository? = null,
    workoutRepository: com.bearbones.kumalift.data.repository.WorkoutRepository? = null,
    onNavigateBack: () -> Unit
) {
    var currentDestination by remember { mutableStateOf(SettingsDestination.MAIN) }

    com.bearbones.kumalift.core.navigation.KumaBackHandler(
        enabled = currentDestination != SettingsDestination.MAIN
    ) {
        currentDestination = SettingsDestination.MAIN
    }

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
                    workoutRepository = workoutRepository,
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
                    databaseRepo = databaseRepo,
                    onNavigateBack = { currentDestination = SettingsDestination.MAIN }
                )
            }
            SettingsDestination.WEARABLES -> {
                val wearableManager = com.bearbones.kumalift.core.wearable.rememberWearableManager()
                WearablesSettingsPage(
                    settingsRepository = settingsRepository,
                    wearableManager = wearableManager,
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
                            icon = Icons.Default.Bluetooth,
                            title = if (isEnglish) "Wearables & Sensors" else "Perangkat & Wearable",
                            subtitle = "Health Connect & Bluetooth",
                            onClick = { onNavigateTo(SettingsDestination.WEARABLES) }
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
    var avatarUriState by remember { mutableStateOf(settings.profile.avatarUri) }

    val imagePicker = rememberProfileImagePicker { newPath ->
        avatarUriState = newPath
        settingsRepository.update { current ->
            current.copy(profile = current.profile.copy(avatarUri = newPath))
        }
    }

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
                                    avatarUri = avatarUriState,
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
                        containerColor = colors.accentFill,
                        contentColor = colors.onAccentFill
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
                                .border(2.dp, colors.accentText, CircleShape)
                                .clickable { imagePicker() },
                            contentAlignment = Alignment.Center
                        ) {
                            ProfileAvatarMedia(
                                avatarUri = avatarUriState,
                                fallbackInitial = nameInput.take(1).ifBlank { "G" },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Change Picture",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = colors.accentText,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.clickable { imagePicker() }
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
                                            if (isSelected) colors.accentFill else KumaColors.SurfaceContainer
                                        )
                                        .clickable { selectedSex = sex }
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) colors.onAccentFill else colors.textSecondary
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
                        containerColor = colors.accentFill,
                        contentColor = colors.onAccentFill
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
                                                    if (isSelected) colors.accentFill else KumaColors.SurfaceContainer
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
                                                    color = if (isSelected) colors.onAccentFill else colors.textSecondary
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
                                                    if (isDayActive) colors.accentFill else KumaColors.SurfaceContainer
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
                                                    color = if (isDayActive) colors.onAccentFill else colors.textSecondary
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
                                        containerColor = colors.accentFill,
                                        contentColor = colors.onAccentFill
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
    workoutRepository: com.bearbones.kumalift.data.repository.WorkoutRepository? = null,
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
                                                if (isSelected) colors.accentFill else KumaColors.SurfaceContainer
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
                                                color = if (isSelected) colors.onAccentFill else colors.textSecondary
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
                                                .background(if (isSel) colors.accentFill else KumaColors.SurfaceContainer)
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
                                                    color = if (isSel) colors.onAccentFill else colors.textSecondary
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

                            // Auto-start Rest Timer
                            Spacer(modifier = Modifier.height(14.dp))
                            SettingsDivider()
                            Spacer(modifier = Modifier.height(14.dp))

                            WorkoutSettingToggle(
                                title = "Auto-start Rest Timer",
                                description = "Automatically start countdown rest timer whenever a set is checked as completed",
                                isChecked = settings.logging.restTimerAutoStart,
                                onCheckedChange = { chk ->
                                    settingsRepository.update {
                                        it.copy(logging = it.logging.copy(restTimerAutoStart = chk))
                                    }
                                }
                            )

                            // Confirm Before Finish
                            Spacer(modifier = Modifier.height(14.dp))
                            SettingsDivider()
                            Spacer(modifier = Modifier.height(14.dp))

                            WorkoutSettingToggle(
                                title = "Confirm Before Finish",
                                description = "Show a confirmation prompt before finalizing and closing workout session",
                                isChecked = settings.logging.confirmFinish,
                                onCheckedChange = { chk ->
                                    settingsRepository.update {
                                        it.copy(logging = it.logging.copy(confirmFinish = chk))
                                    }
                                }
                            )
                        }
                    }
                }

                // Calculations & 1RM Analytics Card
                item {
                    KumaCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Calculations & 1RM Analytics",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Estimated 1RM Formula",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    OneRepMaxFormula.EPLEY to "Epley",
                                    OneRepMaxFormula.BRZYCKI to "Brzycki",
                                    OneRepMaxFormula.LOMBARDI to "Lombardi",
                                    OneRepMaxFormula.OCONNER to "O'Conner"
                                ).forEach { (formula, name) ->
                                    val isSel = settings.calculations.oneRepMaxFormula == formula
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) colors.accentFill else KumaColors.SurfaceContainer)
                                            .clickable {
                                                settingsRepository.update {
                                                    it.copy(calculations = it.calculations.copy(oneRepMaxFormula = formula))
                                                }
                                                workoutRepository?.recomputeDerivedStats()
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = name,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSel) colors.onAccentFill else colors.textSecondary
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            SettingsDivider()
                            Spacer(modifier = Modifier.height(14.dp))

                            // Max Reps for 1RM Estimate
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Max reps for 1RM estimate",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(5, 8, 10, 12).forEach { maxReps ->
                                        val isSel = settings.calculations.maxRepsForEstimate == maxReps
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSel) colors.accentFill else KumaColors.SurfaceContainer)
                                                .clickable {
                                                    settingsRepository.update {
                                                        it.copy(calculations = it.calculations.copy(maxRepsForEstimate = maxReps))
                                                    }
                                                    workoutRepository?.recomputeDerivedStats()
                                                }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "$maxReps",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSel) colors.onAccentFill else colors.textSecondary
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            SettingsDivider()
                            Spacer(modifier = Modifier.height(14.dp))

                            // Count warm-up sets in volume
                            WorkoutSettingToggle(
                                title = "Count Warm-up in Volume",
                                description = "Include warm-up sets when calculating total session and weekly tonnage",
                                isChecked = settings.calculations.includeWarmupInVolume,
                                onCheckedChange = { chk ->
                                    settingsRepository.update {
                                        it.copy(calculations = it.calculations.copy(includeWarmupInVolume = chk))
                                    }
                                    workoutRepository?.recomputeDerivedStats()
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
                            if (isSelected) colors.accentFill else Color.Transparent
                        )
                        .clickable { onSelect(option) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) colors.onAccentFill else colors.textSecondary
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
 * Export & Import Data Page with real full-fidelity backup, CSV export,
 * Merge/Replace restore, Safety Snapshot, and Danger Zone.
 */
@Composable
private fun ExportImportDataPage(
    settingsRepository: SettingsRepository,
    databaseRepo: com.bearbones.kumalift.data.local.KumaDatabaseRepository? = null,
    onNavigateBack: () -> Unit
) {
    val colors = LocalKumaColors.current
    val settings by settingsRepository.settings.collectAsState()
    val isEnglish = settings.appearance.language == AppLanguage.EN
    val backupManager = remember(databaseRepo) {
        databaseRepo?.let { DataBackupManager(it) }
    }

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var pendingImportContent by remember { mutableStateOf<String?>(null) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberDataExportLauncher { success, msg ->
        statusMessage = msg
        if (success) {
            settingsRepository.update {
                it.copy(dataBackup = it.dataBackup.copy(lastBackupEpochMs = currentEpochMillis()))
            }
        }
    }

    val importLauncher = rememberDataImportLauncher { content ->
        if (content != null) {
            pendingImportContent = content
        } else {
            statusMessage = if (isEnglish) "Import cancelled or no file selected." else "Impor dibatalkan atau tidak ada file dipilih."
        }
    }

    val storageUsage = remember(settings, statusMessage) {
        backupManager?.getStorageUsage(settings)
    }

    // Dialog for Restore: Merge vs Replace
    if (pendingImportContent != null) {
        AlertDialog(
            onDismissRequest = { pendingImportContent = null },
            title = {
                Text(
                    text = if (isEnglish) "Restore Data" else "Pulihkan Cadangan Data",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = if (isEnglish)
                        "Choose restore mode:\n\n• MERGE: Combines with current data. Preserves existing workouts and updates records.\n\n• REPLACE: Wipes current data and restores this backup completely. An automatic safety snapshot will be created before wiping."
                    else
                        "Pilih metode pemulihan data:\n\n• MERGE (GABUNGKAN): Menggabungkan dengan data saat ini tanpa menghapus sesi yang sudah ada.\n\n• REPLACE (TIMPA): Menghapus seluruh data saat ini dan menggantinya dengan cadangan ini. Snapshot keselamatan akan otomatis dibuat terlebih dahulu."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val content = pendingImportContent!!
                        pendingImportContent = null
                        val result = backupManager?.importBackup(
                            jsonString = content,
                            mode = ImportMode.REPLACE,
                            currentSettings = settings,
                            onSettingsRestored = { settingsRepository.save(it) }
                        )
                        statusMessage = result?.message ?: "Gagal memulihkan cadangan."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accentText)
                ) {
                    Text(
                        text = if (isEnglish) "REPLACE (Full)" else "TIMPA (Replace)",
                        color = colors.onAccentFill,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { pendingImportContent = null }) {
                        Text(if (isEnglish) "Cancel" else "Batal", color = colors.textSecondary)
                    }
                    Button(
                        onClick = {
                            val content = pendingImportContent!!
                            pendingImportContent = null
                            val result = backupManager?.importBackup(
                                jsonString = content,
                                mode = ImportMode.MERGE,
                                currentSettings = settings,
                                onSettingsRestored = { settingsRepository.save(it) }
                            )
                            statusMessage = result?.message ?: "Gagal menggabungkan cadangan."
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KumaColors.SurfaceContainerHigh)
                    ) {
                        Text(
                            text = if (isEnglish) "MERGE" else "GABUNGKAN",
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        )
    }

    // Confirmation: Clear Workout History Only
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = {
                Text(
                    text = if (isEnglish) "Clear Workout History?" else "Hapus Riwayat Latihan?",
                    fontWeight = FontWeight.Bold,
                    color = KumaColors.ErrorRed
                )
            },
            text = {
                Text(
                    text = if (isEnglish)
                        "All completed workouts and set logs will be permanently deleted. Custom exercises, routines, and settings will be preserved."
                    else
                        "Seluruh riwayat sesi latihan dan log set akan dihapus secara permanen. Gerakan kustom, program latihan, dan preferensi pengaturan akan tetap dipertahankan."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearHistoryDialog = false
                        databaseRepo?.clearWorkoutHistoryOnly()
                        statusMessage = if (isEnglish) "Workout history cleared successfully." else "Riwayat latihan berhasil dibersihkan."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KumaColors.ErrorRed)
                ) {
                    Text(if (isEnglish) "DELETE HISTORY" else "HAPUS RIWAYAT", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text(if (isEnglish) "Cancel" else "Batal", color = colors.textSecondary)
                }
            }
        )
    }

    // Confirmation: Delete All Data
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = {
                Text(
                    text = if (isEnglish) "Delete All Data?" else "Hapus Seluruh Data Aplikasi?",
                    fontWeight = FontWeight.Bold,
                    color = KumaColors.ErrorRed
                )
            },
            text = {
                Text(
                    text = if (isEnglish)
                        "This will permanently delete all workouts, PR records, custom exercises, routines, and settings. This cannot be undone."
                    else
                        "Tindakan ini akan menghapus permanen seluruh riwayat, rekor PR, gerakan kustom, program latihan, dan semua pengaturan. Tindakan ini tidak dapat dibatalkan."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAllDialog = false
                        databaseRepo?.wipeAllUserData()
                        settingsRepository.save(UserSettings())
                        statusMessage = if (isEnglish) "All data deleted. App restored to fresh install." else "Semua data telah dihapus. Aplikasi kembali ke kondisi awal."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KumaColors.ErrorRed)
                ) {
                    Text(if (isEnglish) "DELETE EVERYTHING" else "HAPUS SEMUA", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text(if (isEnglish) "Cancel" else "Batal", color = colors.textSecondary)
                }
            }
        )
    }

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
                    text = if (isEnglish) "Export & Import Data" else "Cadangan & Pemulihan Data",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Backup & Restore Actions
                item {
                    Text(
                        text = if (isEnglish) "BACKUP & RESTORE (LOCAL ONLY)" else "CADANGAN & PEMULIHAN (LOKAL)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = colors.accentText,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    KumaCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SettingsNavigationRow(
                                icon = Icons.Default.FileDownload,
                                title = if (isEnglish) "Export Backup (JSON)" else "Ekspor Cadangan Lengkap (JSON)",
                                subtitle = if (isEnglish) "Full-fidelity backup with schema v1" else "Cadangkan seluruh riwayat, PR, program, dan pengaturan",
                                onClick = {
                                    if (backupManager != null) {
                                        val jsonPayload = backupManager.exportJson(settings)
                                        val now = currentEpochMillis()
                                        exportLauncher("kumalift_backup_$now.json", jsonPayload)
                                    } else {
                                        statusMessage = "Database repository tidak tersedia."
                                    }
                                }
                            )
                            SettingsDivider()
                            SettingsNavigationRow(
                                icon = Icons.Default.FileUpload,
                                title = if (isEnglish) "Restore from Backup" else "Pulihkan dari Cadangan",
                                subtitle = if (isEnglish) "Restore with Merge or Replace options" else "Pilih file cadangan JSON dan opsi Gabungkan atau Timpa",
                                onClick = {
                                    importLauncher()
                                }
                            )
                            SettingsDivider()
                            SettingsNavigationRow(
                                icon = Icons.Default.Restore,
                                title = if (isEnglish) "Restore Safety Snapshot" else "Pulihkan Snapshot Keselamatan",
                                subtitle = if (isEnglish) "Revert to automatic pre-replace snapshot" else "Kembalikan data sebelum penimpaan terakhir dilakukan",
                                onClick = {
                                    if (backupManager != null) {
                                        val result = backupManager.restoreSafetySnapshot(settings) {
                                            settingsRepository.save(it)
                                        }
                                        statusMessage = result.message
                                    }
                                }
                            )
                        }
                    }
                }

                // Section 2: CSV Data Export
                item {
                    Text(
                        text = if (isEnglish) "CSV EXPORT" else "EKSPOR FORMAT CSV",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = colors.textSecondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    KumaCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SettingsNavigationRow(
                                icon = Icons.Default.FileDownload,
                                title = if (isEnglish) "Export Workouts CSV" else "Ekspor Riwayat Latihan (CSV)",
                                subtitle = if (isEnglish) "Spreadsheet compatible workout log" else "Format CSV yang dapat dibuka di Excel atau Google Sheets",
                                onClick = {
                                    if (backupManager != null) {
                                        val csvFiles = backupManager.exportCsvFiles(settings)
                                        val workoutsCsv = csvFiles["workouts.csv"] ?: ""
                                        val now = currentEpochMillis()
                                        exportLauncher("kumalift_workouts_$now.csv", workoutsCsv)
                                    }
                                }
                            )
                        }
                    }
                }

                // Section 3: Read-only Data Metrics
                item {
                    Text(
                        text = if (isEnglish) "STORAGE & STATUS" else "STATUS PENYIMPANAN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = colors.textSecondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    KumaCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isEnglish) "Storage Used" else "Penyimpanan Terpakai",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = storageUsage?.let {
                                        "${it.workoutsCount} ${if (isEnglish) "workouts" else "sesi"} · ${it.setsCount} set"
                                    } ?: "—",
                                    style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                                )
                            }
                            SettingsDivider()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isEnglish) "Last Backup" else "Cadangan Terakhir",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = settings.dataBackup.lastBackupEpochMs?.let {
                                        if (isEnglish) "Recorded" else "Tersimpan"
                                    } ?: if (isEnglish) "Never" else "Belum pernah",
                                    style = MaterialTheme.typography.bodySmall.copy(color = colors.accentText)
                                )
                            }
                        }
                    }
                }

                // Status Message Toast Banner
                if (statusMessage != null) {
                    item {
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

                // Section 4: Danger Zone
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isEnglish) "DANGER ZONE" else "ZONA BERBAHAYA",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = KumaColors.ErrorRed,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    KumaCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = KumaColors.ErrorRed.copy(alpha = 0.5f)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SettingsNavigationRow(
                                icon = Icons.Default.History,
                                title = if (isEnglish) "Clear Workout History" else "Hapus Riwayat Latihan",
                                subtitle = if (isEnglish) "Delete all workouts and sets (keeps routines & settings)" else "Hapus semua sesi dan set (program dan pengaturan tetap aman)",
                                onClick = { showClearHistoryDialog = true }
                            )
                            SettingsDivider()
                            SettingsNavigationRow(
                                icon = Icons.Default.DeleteForever,
                                title = if (isEnglish) "Delete All Data" else "Hapus Semua Data Aplikasi",
                                subtitle = if (isEnglish) "Factory reset: delete all local data and preferences" else "Hapus permanen seluruh data dan kembalikan ke kondisi baru",
                                onClick = { showDeleteAllDialog = true }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(60.dp))
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
