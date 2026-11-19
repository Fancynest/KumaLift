package com.bearbones.kumalift.features.settings

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.bearbones.kumalift.core.backup.AutoBackupService
import com.bearbones.kumalift.core.backup.BackupErrorCode
import com.bearbones.kumalift.core.crypto.BackupCrypto
import com.bearbones.kumalift.core.crypto.BackupFileType
import com.bearbones.kumalift.core.data.deleteCustomAudioFile
import com.bearbones.kumalift.core.data.rememberAudioPickerLauncher
import com.bearbones.kumalift.core.data.rememberDataExportBytesLauncher
import com.bearbones.kumalift.core.data.rememberDataExportLauncher
import com.bearbones.kumalift.core.data.rememberDataImportBytesLauncher
import com.bearbones.kumalift.core.data.rememberDataImportLauncher
import com.bearbones.kumalift.core.data.rememberFolderPickerLauncher
import com.bearbones.kumalift.core.localization.label
import com.bearbones.kumalift.core.util.currentEpochMillis
import com.bearbones.kumalift.core.util.formatEpochToDateString
import com.bearbones.kumalift.data.export.DataBackupManager
import com.bearbones.kumalift.data.export.ImportMode
import com.bearbones.kumalift.domain.model.BackupFrequency
import com.bearbones.kumalift.features.routines.WorkoutScheduleModal
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumalift.core.designsystem.KumaCard
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.core.designsystem.kumaKeyboardAvoidance
import com.bearbones.kumalift.core.logging.KumaLogger
import com.bearbones.kumalift.core.media.ProfileAvatarMedia
import com.bearbones.kumalift.core.media.ProfilePhotoCropDialog
import com.bearbones.kumalift.core.media.rememberProfileImagePicker
import com.bearbones.kumalift.core.notification.rememberNotificationPermissionLauncher
import com.bearbones.kumalift.core.notification.rememberWorkoutReminderManager
import com.bearbones.kumalift.core.localization.LocalStrings
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
import com.bearbones.kumalift.core.sound.SoundEvent
import com.bearbones.kumalift.core.sound.SoundManager
import com.bearbones.kumalift.domain.model.RestTimerSoundSource
import com.bearbones.kumalift.domain.model.SoundProfile
import com.bearbones.kumalift.domain.model.SoundVolume
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
    ABOUT
}

@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    databaseRepo: com.bearbones.kumalift.data.local.KumaDatabaseRepository? = null,
    workoutRepository: com.bearbones.kumalift.data.repository.WorkoutRepository? = null,
    onNavigateBack: () -> Unit
) {
    val destinationStack = remember { mutableStateListOf(SettingsDestination.MAIN) }
    val currentDestination = destinationStack.lastOrNull() ?: SettingsDestination.MAIN

    DisposableEffect(Unit) {
        onDispose {
            SoundManager.stopSound()
        }
    }

    val popDestination: () -> Unit = {
        SoundManager.stopSound()
        if (destinationStack.size > 1) {
            destinationStack.removeAt(destinationStack.lastIndex)
        } else {
            onNavigateBack()
        }
    }

    com.bearbones.kumalift.core.navigation.KumaBackHandler(
        enabled = destinationStack.size > 1
    ) {
        SoundManager.stopSound()
        if (destinationStack.size > 1) {
            destinationStack.removeAt(destinationStack.lastIndex)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = KumaColors.CanvasLowest
    ) {
        Crossfade(targetState = currentDestination) { destination ->
            when (destination) {
                SettingsDestination.MAIN -> {
                    SettingsMainMenu(
                        settingsRepository = settingsRepository,
                        onNavigateBack = onNavigateBack,
                        onNavigateTo = { target ->
                            if (destinationStack.lastOrNull() != target) {
                                destinationStack.add(target)
                            }
                        }
                    )
                }
                SettingsDestination.PROFILE -> {
                    ProfileSettingsPage(
                        settingsRepository = settingsRepository,
                        onNavigateBack = popDestination
                    )
                }
                SettingsDestination.ACCOUNT_USERNAME -> {
                    ChangeUsernamePage(
                        settingsRepository = settingsRepository,
                        onNavigateBack = popDestination
                    )
                }
                SettingsDestination.NOTIFICATIONS -> {
                    NotificationSettingsPage(
                        settingsRepository = settingsRepository,
                        onNavigateBack = popDestination
                    )
                }
                SettingsDestination.WORKOUTS -> {
                    WorkoutSettingsPage(
                        settingsRepository = settingsRepository,
                        workoutRepository = workoutRepository,
                        onNavigateBack = popDestination
                    )
                }
                SettingsDestination.UNITS -> {
                    UnitsSettingsPage(
                        settingsRepository = settingsRepository,
                        onNavigateBack = popDestination
                    )
                }
                SettingsDestination.LANGUAGE -> {
                    LanguageSettingsPage(
                        settingsRepository = settingsRepository,
                        onNavigateBack = popDestination
                    )
                }
                SettingsDestination.THEME -> {
                    ThemeSettingsPage(
                        settingsRepository = settingsRepository,
                        onNavigateBack = popDestination
                    )
                }
                SettingsDestination.EXPORT_IMPORT -> {
                    ExportImportDataPage(
                        settingsRepository = settingsRepository,
                        databaseRepo = databaseRepo,
                        onNavigateBack = popDestination
                    )
                }
                SettingsDestination.ABOUT -> {
                    AboutSettingsPage(
                        onNavigateBack = popDestination
                    )
                }
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
    val s = LocalStrings.current

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
                        .clickable(onClick = {
                            SoundManager.stopSound()
                            onNavigateBack()
                        }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = s.back,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = s.settingsTitle,
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
                    SettingsGroupSection(title = s.accountGroupTitle) {
                        SettingsNavigationRow(
                            icon = Icons.Default.Person,
                            title = s.profileSettingsRow,
                            onClick = { onNavigateTo(SettingsDestination.PROFILE) }
                        )
                        SettingsDivider()
                        SettingsNavigationRow(
                            icon = Icons.Default.Lock,
                            title = s.accountSettingsRow,
                            onClick = { onNavigateTo(SettingsDestination.ACCOUNT_USERNAME) }
                        )
                        SettingsDivider()
                        SettingsNavigationRow(
                            icon = Icons.Default.Notifications,
                            title = s.notificationsSettingsRow,
                            onClick = { onNavigateTo(SettingsDestination.NOTIFICATIONS) }
                        )
                    }
                }

                // Section: Preferences
                item {
                    SettingsGroupSection(title = s.preferencesGroupTitle) {
                        SettingsNavigationRow(
                            icon = Icons.Default.FitnessCenter,
                            title = s.workoutsSettingsRow,
                            onClick = { onNavigateTo(SettingsDestination.WORKOUTS) }
                        )
                        SettingsDivider()
                        SettingsNavigationRow(
                            icon = Icons.Default.Scale,
                            title = s.unitsSettingsRow,
                            onClick = { onNavigateTo(SettingsDestination.UNITS) }
                        )
                        SettingsDivider()
                        SettingsNavigationRow(
                            icon = Icons.Default.Language,
                            title = s.languageSettingsRow,
                            onClick = { onNavigateTo(SettingsDestination.LANGUAGE) }
                        )
                        SettingsDivider()
                        SettingsNavigationRow(
                            icon = Icons.Default.Palette,
                            title = s.themeSettingsRow,
                            onClick = { onNavigateTo(SettingsDestination.THEME) }
                        )
                        SettingsDivider()
                        SettingsNavigationRow(
                            icon = Icons.AutoMirrored.Filled.Send,
                            title = s.exportImportSettingsRow,
                            onClick = { onNavigateTo(SettingsDestination.EXPORT_IMPORT) }
                        )
                        SettingsDivider()
                        SettingsNavigationRow(
                            icon = Icons.Default.Info,
                            title = s.aboutLicensesSettingsRow,
                            onClick = { onNavigateTo(SettingsDestination.ABOUT) }
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
    val s = LocalStrings.current

    var nameInput by remember { mutableStateOf(settings.profile.name) }
    var bioInput by remember { mutableStateOf(settings.profile.bio) }
    var birthdayInput by remember { mutableStateOf(settings.profile.birthday) }
    var selectedSex by remember { mutableStateOf(settings.profile.sex) }
    var selectedGoal by remember { mutableStateOf(settings.profile.goal) }
    var showScheduleModal by remember { mutableStateOf(false) }
    var bwInput by remember { mutableStateOf(settings.profile.bodyweightKg?.toString() ?: "") }
    var htInput by remember { mutableStateOf(settings.profile.heightCm?.toString() ?: "") }
    var avatarUriState by remember { mutableStateOf(settings.profile.avatarUri) }
    var pendingCropUri by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberProfileImagePicker { newPath ->
        pendingCropUri = newPath
    }

    if (pendingCropUri != null) {
        ProfilePhotoCropDialog(
            imageUri = pendingCropUri!!,
            onCancel = { pendingCropUri = null },
            onConfirm = { confirmedUri ->
                avatarUriState = confirmedUri
                pendingCropUri = null
            }
        )
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
                        .clickable(onClick = {
                            SoundManager.stopSound()
                            onNavigateBack()
                        }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = s.back,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = s.editProfileTitle,
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
                                    birthday = birthdayInput.trim(),
                                    sex = selectedSex,
                                    goal = selectedGoal,
                                    bodyweightKg = bwInput.toDoubleOrNull(),
                                    heightCm = htInput.toDoubleOrNull()
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
                    Text(s.done, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .kumaKeyboardAvoidance()
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
                                .background(colors.surfaceVariant)
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
                            text = s.changePictureButton,
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
                        text = s.publicProfileSection,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        ),
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }

                item {
                    KumaInputField(
                        label = s.profileNameInputLabel,
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        placeholder = s.profileNamePlaceholder
                    )
                }

                item {
                    KumaInputField(
                        label = s.profileBioLabel,
                        value = bioInput,
                        onValueChange = { bioInput = it },
                        placeholder = s.profileBioPlaceholder
                    )
                }

                // Private data
                item {
                    Text(
                        text = s.privateProfileSection,
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
                            text = s.sexFieldLabel,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = colors.textPrimary
                            )
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                AthleteSex.MALE to s.sexMale,
                                AthleteSex.FEMALE to s.sexFemale
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
                                        .border(
                                            1.dp,
                                            if (isSelected) colors.accentFill else colors.border,
                                            RoundedCornerShape(8.dp)
                                        )
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
                        label = s.birthdayFieldLabel,
                        value = birthdayInput,
                        onValueChange = { birthdayInput = it },
                        placeholder = s.birthdayFieldPlaceholder
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            KumaInputField(
                                label = "${s.bodyWeightMeasureLabel} (${s.unitKg})",
                                value = bwInput,
                                onValueChange = { bwInput = it },
                                placeholder = "75.0"
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            KumaInputField(
                                label = "${s.bodyHeightMeasureLabel} (${s.sizeUnitCm})",
                                value = htInput,
                                onValueChange = { htInput = it },
                                placeholder = "175.0"
                            )
                        }
                    }
                }

                // Training Goal Section
                item {
                    Text(
                        text = s.trainingGoalTitle,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        ),
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            TrainingGoal.HYPERTROPHY to s.programHypertrophy,
                            TrainingGoal.STRENGTH to s.programStrength,
                            TrainingGoal.ENDURANCE to s.programEndurance,
                            TrainingGoal.FAT_LOSS to s.programFatLoss,
                            TrainingGoal.GENERAL to s.programGeneral
                        ).forEach { (goal, label) ->
                            val isSelected = selectedGoal == goal
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) colors.accentFill else KumaColors.SurfaceContainer)
                                    .clickable { selectedGoal = goal }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .border(
                                        1.dp,
                                        if (isSelected) colors.accentFill else colors.border,
                                        RoundedCornerShape(10.dp)
                                    )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) colors.onAccentFill else colors.textPrimary
                                        )
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = colors.onAccentFill,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Training Schedule Section
                item {
                    Text(
                        text = s.trainingScheduleTitle,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        ),
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(KumaColors.SurfaceContainer)
                            .clickable { showScheduleModal = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = s.trainingScheduleTitle,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textPrimary
                                    )
                                )
                                Text(
                                    text = s.trainingDaysSubtitle(settings.profile.trainingDaysPerWeek),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = colors.textSecondary
                                    )
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(40.dp)) }
            }
        }

        if (showScheduleModal) {
            WorkoutScheduleModal(
                settingsRepository = settingsRepository,
                onDismiss = { showScheduleModal = false }
            )
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
    val s = LocalStrings.current
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
                        .clickable(onClick = {
                            SoundManager.stopSound()
                            onNavigateBack()
                        }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = s.back,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = s.accountSettingsRow,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .kumaKeyboardAvoidance()
                    .padding(20.dp)
            ) {
                Text(
                    text = s.usernameInputLabel,
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
                    Text(s.updateButton, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp))
                }
            }
        }
    }
}

/**
 * Push Notifications Page matching notifcation.jpg + Workout Reminders & Battery Optimization
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun NotificationSettingsPage(
    settingsRepository: SettingsRepository,
    onNavigateBack: () -> Unit
) {
    val settings by settingsRepository.settings.collectAsState()
    val colors = LocalKumaColors.current
    val s = LocalStrings.current
    val workoutReminderManager = rememberWorkoutReminderManager()
    var showCustomTimeDialog by remember { mutableStateOf(false) }
    var hasPostNotificationPermission by remember {
        mutableStateOf(workoutReminderManager.hasNotificationPermission())
    }
    var isCustomPreviewPlaying by remember { mutableStateOf(false) }
    var customSoundError by remember { mutableStateOf<String?>(null) }

    // Dynamic sync on resume: automatically dismisses or restores the warning card
    // when returning from system notification settings.
    LifecycleResumeEffect(workoutReminderManager) {
        val currentPermission = workoutReminderManager.hasNotificationPermission()
        hasPostNotificationPermission = currentPermission
        if (currentPermission && settings.notifications.workoutReminders) {
            workoutReminderManager.syncSettings(settings.notifications, settings.appearance.language)
        }
        onPauseOrDispose {
            SoundManager.stopSound()
            isCustomPreviewPlaying = false
        }
    }

    DisposableEffect(Unit) {
        SoundManager.onPlaybackStopped = {
            isCustomPreviewPlaying = false
        }
        onDispose {
            SoundManager.stopSound()
            SoundManager.onPlaybackStopped = null
            isCustomPreviewPlaying = false
        }
    }

    val requestNotificationPermission = rememberNotificationPermissionLauncher { granted ->
        hasPostNotificationPermission = granted
        if (granted) {
            settingsRepository.update {
                it.copy(notifications = it.notifications.copy(workoutReminders = true))
            }
            workoutReminderManager.syncSettings(settings.notifications.copy(workoutReminders = true), settings.appearance.language)
        }
    }

    val audioPickerLauncher = rememberAudioPickerLauncher { fileName, localPath, errorMsg ->
        if (errorMsg != null) {
            customSoundError = errorMsg
        } else if (fileName != null && localPath != null) {
            customSoundError = null
            settingsRepository.update {
                it.copy(
                    notifications = it.notifications.copy(
                        restTimerSoundSource = RestTimerSoundSource.CUSTOM,
                        customRestTimerSoundPath = localPath,
                        customRestTimerSoundName = fileName
                    )
                )
            }
        }
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
                        .clickable(onClick = {
                            SoundManager.stopSound()
                            onNavigateBack()
                        }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = s.back,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = s.notificationsSettingsRow,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // General section matching notifcation.jpg (omitting 'Follows' per red X)
                item {
                    Text(
                        text = s.generalSectionTitle,
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
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 1. Sound Profile Selector
                            UnitOptionGroup(
                                title = s.soundProfileTitle,
                                options = listOf(
                                    SoundProfile.KUMA to s.soundProfileKuma,
                                    SoundProfile.SILENT_VIBRATE to s.soundProfileSilentVibrate,
                                    SoundProfile.SILENT to s.soundProfileSilent
                                ),
                                selected = settings.notifications.soundProfile,
                                onSelect = { profile ->
                                    SoundManager.stopSound()
                                    isCustomPreviewPlaying = false
                                    settingsRepository.update {
                                        it.copy(notifications = it.notifications.copy(soundProfile = profile))
                                    }
                                    workoutReminderManager.syncSettings(
                                        settings.notifications.copy(soundProfile = profile),
                                        settings.appearance.language
                                    )
                                }
                            )

                            SettingsDivider()

                            // 2. In-App Sound Volume Slider (multiples of 10, default 50%)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = s.soundVolumeTitle,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            text = s.soundVolumeSubtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textSecondary
                                        )
                                    }
                                    Text(
                                        text = if (settings.notifications.soundVolume == 0) "0% (${s.soundProfileSilent})" else "${settings.notifications.soundVolume}%",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = if (settings.notifications.soundVolume == 0) colors.textSecondary else colors.accentFill
                                    )
                                }

                                Slider(
                                    value = settings.notifications.soundVolume.toFloat(),
                                    onValueChange = { newVal ->
                                        val snapped = (((newVal + 5f) / 10f).toInt() * 10).coerceIn(0, 100)
                                        if (snapped != settings.notifications.soundVolume) {
                                            settingsRepository.update {
                                                it.copy(notifications = it.notifications.copy(soundVolume = snapped))
                                            }
                                        }
                                    },
                                    valueRange = 0f..100f,
                                    steps = 9,
                                    colors = SliderDefaults.colors(
                                        thumbColor = colors.accentFill,
                                        activeTrackColor = colors.accentFill,
                                        inactiveTrackColor = colors.surfaceVariant,
                                        activeTickColor = colors.surfaceBright,
                                        inactiveTickColor = colors.textSecondary.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("0%", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                    Text("50%", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                    Text("100%", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                }
                            }

                            SettingsDivider()

                            // 2b. Rest Timer Sound Source Selector
                            UnitOptionGroup(
                                title = s.restTimerSoundSourceTitle,
                                options = listOf(
                                    RestTimerSoundSource.BUILTIN to s.restTimerSoundBuiltin,
                                    RestTimerSoundSource.SYSTEM_DEFAULT to s.restTimerSoundSystemDefault,
                                    RestTimerSoundSource.CUSTOM to s.restTimerSoundCustom
                                ),
                                selected = settings.notifications.restTimerSoundSource,
                                onSelect = { source ->
                                    SoundManager.stopSound()
                                    isCustomPreviewPlaying = false
                                    settingsRepository.update {
                                        it.copy(notifications = it.notifications.copy(restTimerSoundSource = source))
                                    }
                                }
                            )

                            if (settings.notifications.restTimerSoundSource == RestTimerSoundSource.CUSTOM) {
                                val customFileName = settings.notifications.customRestTimerSoundName
                                val customFilePath = settings.notifications.customRestTimerSoundPath

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.surfaceVariant.copy(alpha = 0.5f))
                                        .border(1.dp, colors.surfaceVariant, RoundedCornerShape(12.dp))
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (!customFileName.isNullOrBlank() && !customFilePath.isNullOrBlank()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(colors.accentFill.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.MusicNote,
                                                    contentDescription = null,
                                                    tint = colors.accentText,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = customFileName,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = colors.textPrimary
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = s.customSoundReady,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = colors.accentText,
                                                        fontSize = 11.sp
                                                    )
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    deleteCustomAudioFile(customFilePath)
                                                    SoundManager.stopSound()
                                                    isCustomPreviewPlaying = false
                                                    settingsRepository.update {
                                                        it.copy(
                                                            notifications = it.notifications.copy(
                                                                restTimerSoundSource = RestTimerSoundSource.BUILTIN,
                                                                customRestTimerSoundPath = null,
                                                                customRestTimerSoundName = null
                                                            )
                                                        )
                                                    }
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = s.removeCustomSound,
                                                    tint = KumaColors.ErrorRed,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isCustomPreviewPlaying) colors.accentFill else KumaColors.SurfaceDim)
                                                    .clickable {
                                                        if (isCustomPreviewPlaying) {
                                                            SoundManager.stopSound()
                                                            isCustomPreviewPlaying = false
                                                        } else {
                                                            isCustomPreviewPlaying = true
                                                            SoundManager.previewCustomSound(customFilePath, settings.notifications.soundVolume)
                                                        }
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (isCustomPreviewPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                                        contentDescription = null,
                                                        tint = if (isCustomPreviewPlaying) colors.onAccentFill else colors.accentText,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Text(
                                                        text = if (isCustomPreviewPlaying) s.customSoundPlaying else s.previewSoundsTitle,
                                                        style = MaterialTheme.typography.labelMedium.copy(
                                                            color = if (isCustomPreviewPlaying) colors.onAccentFill else colors.textPrimary,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    )
                                                }
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(KumaColors.SurfaceDim)
                                                    .clickable {
                                                        SoundManager.stopSound()
                                                        isCustomPreviewPlaying = false
                                                        audioPickerLauncher()
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.FolderOpen,
                                                        contentDescription = null,
                                                        tint = colors.textPrimary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Text(
                                                        text = s.replaceCustomSound,
                                                        style = MaterialTheme.typography.labelMedium.copy(
                                                            color = colors.textPrimary,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = s.customSoundNoFileSelected,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Medium,
                                                    color = colors.textPrimary
                                                )
                                            )
                                            Text(
                                                text = s.customSoundSupportedFormats,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = colors.textSecondary
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Button(
                                                onClick = {
                                                    customSoundError = null
                                                    audioPickerLauncher()
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = colors.accentFill,
                                                    contentColor = colors.onAccentFill
                                                ),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.UploadFile,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = s.uploadCustomSound,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    if (customSoundError != null) {
                                        Text(
                                            text = customSoundError ?: "",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = KumaColors.ErrorRed,
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                    }
                                }
                            } else if (settings.notifications.restTimerSoundSource == RestTimerSoundSource.SYSTEM_DEFAULT) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(KumaColors.SurfaceDim)
                                        .clickable {
                                            SoundManager.previewSystemDefaultSound(settings.notifications.soundVolume)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = colors.accentText,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${s.previewSoundsTitle} (${s.restTimerSoundSystemDefault})",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = colors.textPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }

                            SettingsDivider()

                            // 3. In-App Audio Feedback switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = s.inAppSoundAlertsTitle,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            color = colors.textPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                    Text(
                                        text = s.inAppSoundAlertsSubtitle,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = colors.textSecondary
                                        )
                                    )
                                }
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

                            SettingsDivider()

                            // 4. Rest Timer Notification in Background switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = s.restTimerHeader,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            color = colors.textPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                    Text(
                                        text = s.restTimerChannelDescription,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = colors.textSecondary
                                        )
                                    )
                                }
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

                            SettingsDivider()

                            // 5. Tick during last 3 seconds switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = s.countdownTicksTitle,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            color = colors.textPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                    Text(
                                        text = s.countdownTicksSubtitle,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = colors.textSecondary
                                        )
                                    )
                                }
                                Switch(
                                    checked = settings.notifications.countdownBeeps,
                                    onCheckedChange = { checked ->
                                        settingsRepository.update {
                                            it.copy(notifications = it.notifications.copy(countdownBeeps = checked))
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = colors.accentText,
                                        checkedTrackColor = colors.accentText.copy(alpha = 0.35f)
                                    )
                                )
                            }

                            SettingsDivider()

                            // 6. Ongoing Workout Notification switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = s.ongoingWorkoutNotifTitle,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            color = colors.textPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                    Text(
                                        text = s.ongoingWorkoutNotifSubtitle,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = colors.textSecondary
                                        )
                                    )
                                }
                                Switch(
                                    checked = settings.notifications.ongoingWorkout,
                                    onCheckedChange = { checked ->
                                        settingsRepository.update {
                                            it.copy(notifications = it.notifications.copy(ongoingWorkout = checked))
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = colors.accentText,
                                        checkedTrackColor = colors.accentText.copy(alpha = 0.35f)
                                    )
                                )
                            }

                            SettingsDivider()

                            // 7. Preview Sounds Row
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = s.previewSoundsTitle,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = s.previewSoundsSubtitle,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = colors.textSecondary
                                    )
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                val previewSoundsList = listOf(
                                    s.soundPreviewRestEnd to SoundEvent.REST_END,
                                    s.soundPreviewTick to SoundEvent.TICK,
                                    s.soundPreviewSetDone to SoundEvent.SET_DONE,
                                    s.soundPreviewPr to SoundEvent.PR,
                                    s.soundPreviewWorkoutDone to SoundEvent.WORKOUT_DONE,
                                    s.soundPreviewReminder to SoundEvent.REMINDER,
                                    s.soundPreviewError to SoundEvent.ERROR
                                )

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    previewSoundsList.forEach { (label, event) ->
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(KumaColors.SurfaceDim)
                                                .clickable {
                                                    SoundManager.previewSound(event, settings.notifications.soundVolume)
                                                }
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = label,
                                                tint = colors.accentText,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    color = colors.textPrimary,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Workout Reminders & Battery Optimization
                item {
                    Text(
                        text = s.dailyWorkoutReminderTitle,
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
                                        text = s.dailyWorkoutReminderTitle,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            color = colors.textPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                    Text(
                                        text = s.dailyWorkoutReminderSubtitle,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = colors.textSecondary
                                        )
                                    )
                                }
                                Switch(
                                    checked = settings.notifications.workoutReminders,
                                    onCheckedChange = { enabled ->
                                        if (enabled && !workoutReminderManager.hasNotificationPermission()) {
                                            requestNotificationPermission()
                                        } else {
                                            settingsRepository.update {
                                                it.copy(notifications = it.notifications.copy(workoutReminders = enabled))
                                            }
                                            workoutReminderManager.syncSettings(settings.notifications.copy(workoutReminders = enabled), settings.appearance.language)
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = colors.accentText,
                                        checkedTrackColor = colors.accentText.copy(alpha = 0.35f)
                                    )
                                )
                            }

                            // Notification permission alert if workout reminders are turned on but disabled in OS
                            if (settings.notifications.workoutReminders && !hasPostNotificationPermission) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(KumaColors.SurfaceContainer)
                                        .border(1.dp, KumaColors.ErrorRed.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                        .padding(12.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = s.notificationPermissionRequiredTitle,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                color = KumaColors.ErrorRed,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Text(
                                            text = s.notificationPermissionRequiredBody,
                                            style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(colors.accentFill)
                                                .clickable { workoutReminderManager.openNotificationSettings() }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = s.openNotificationSettingsButton,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = colors.onAccentFill,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            if (settings.notifications.workoutReminders) {
                                SettingsDivider()

                                // Time selector presets + Custom
                                Text(
                                    text = s.reminderTimeLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary)
                                )
                                val presets = listOf("06:00", "07:00", "17:00", "18:30", "20:00")
                                val isCustomTime = !presets.contains(settings.notifications.reminderTime)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    presets.forEach { timeStr ->
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
                                                    workoutReminderManager.syncSettings(settings.notifications.copy(reminderTime = timeStr), settings.appearance.language)
                                                }
                                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = timeStr,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) colors.onAccentFill else colors.textSecondary,
                                                    fontSize = 11.sp
                                                )
                                            )
                                        }
                                    }

                                    // Custom button/pill
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isCustomTime) colors.accentFill else KumaColors.SurfaceContainer
                                            )
                                            .clickable { showCustomTimeDialog = true }
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = if (isCustomTime) "${settings.notifications.reminderTime} (${s.custom})" else "${s.custom}...",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isCustomTime) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isCustomTime) colors.onAccentFill else colors.textSecondary,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }

                                // Days selector
                                Text(
                                    text = s.activeDaysLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val daysLabels = s.daysMondayStart.mapIndexed { idx, dayName ->
                                        (idx + 1) to dayName.take(1)
                                    }
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
                                                    workoutReminderManager.syncSettings(settings.notifications.copy(reminderDays = newDays), settings.appearance.language)
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


                item { Spacer(modifier = Modifier.height(40.dp)) }
            }
        }
    }

    // Custom Time Picker Dialog (Redesigned per contohtampilancustomreminder.jpg with flexible 00-59 minute keypad)
    if (showCustomTimeDialog) {
        val initialParts = settings.notifications.reminderTime.split(":")
        var selectedHour by remember { mutableStateOf(initialParts.getOrNull(0)?.toIntOrNull() ?: 17) }
        var selectedMinute by remember { mutableStateOf(initialParts.getOrNull(1)?.toIntOrNull() ?: 0) }
        var isEditingHour by remember { mutableStateOf(true) }
        var hourInputBuffer by remember { mutableStateOf("") }
        var minuteInputBuffer by remember { mutableStateOf("") }

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCustomTimeDialog = false },
            modifier = Modifier.imePadding(),
            containerColor = KumaColors.SurfaceContainerLow,
            title = {
                Text(
                    text = s.selectCustomReminderTimeTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Large Hours : Minutes Display matching contohtampilancustomreminder.jpg
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Hours Box
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                isEditingHour = true
                                hourInputBuffer = ""
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(88.dp)
                                    .height(72.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isEditingHour) colors.accentFill.copy(alpha = 0.15f) else KumaColors.SurfaceContainer)
                                    .border(
                                        width = if (isEditingHour) 2.dp else 1.dp,
                                        color = if (isEditingHour) colors.accentFill else KumaColors.BorderDefault,
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = selectedHour.toString().padStart(2, '0'),
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontSize = 38.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isEditingHour) colors.accentText else colors.textPrimary
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = s.hoursLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isEditingHour) colors.accentText else KumaColors.TextMuted,
                                    fontWeight = if (isEditingHour) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }

                        Text(
                            text = ":",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            ),
                            modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 18.dp)
                        )

                        // Minutes Box
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                isEditingHour = false
                                minuteInputBuffer = ""
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(88.dp)
                                    .height(72.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (!isEditingHour) colors.accentFill.copy(alpha = 0.15f) else KumaColors.SurfaceContainer)
                                    .border(
                                        width = if (!isEditingHour) 2.dp else 1.dp,
                                        color = if (!isEditingHour) colors.accentFill else KumaColors.BorderDefault,
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = selectedMinute.toString().padStart(2, '0'),
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontSize = 38.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!isEditingHour) colors.accentText else colors.textPrimary
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = s.minutesLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (!isEditingHour) colors.accentText else KumaColors.TextMuted,
                                    fontWeight = if (!isEditingHour) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Numeric Keypad matching contohtampilancustomreminder.jpg
                    val keys = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("00", "0", "DEL")
                    )

                    keys.forEach { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                        ) {
                            row.forEach { key ->
                                Box(
                                    modifier = Modifier
                                        .size(width = 76.dp, height = 48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(KumaColors.SurfaceContainer)
                                        .clickable {
                                            when (key) {
                                                "DEL" -> {
                                                    if (isEditingHour) {
                                                        hourInputBuffer = ""
                                                        selectedHour = 0
                                                    } else {
                                                        minuteInputBuffer = ""
                                                        selectedMinute = 0
                                                    }
                                                }
                                                "00" -> {
                                                    if (isEditingHour) {
                                                        selectedHour = 0
                                                        isEditingHour = false
                                                        minuteInputBuffer = ""
                                                    } else {
                                                        selectedMinute = 0
                                                    }
                                                }
                                                else -> {
                                                    val digit = key.toIntOrNull() ?: 0
                                                    if (isEditingHour) {
                                                        val newBuf = (hourInputBuffer + digit).takeLast(2)
                                                        hourInputBuffer = newBuf
                                                        val parsed = newBuf.toIntOrNull() ?: digit
                                                        selectedHour = minOf(23, parsed)
                                                        if (newBuf.length >= 2 || parsed >= 3) {
                                                            isEditingHour = false
                                                            minuteInputBuffer = ""
                                                        }
                                                    } else {
                                                        val newBuf = (minuteInputBuffer + digit).takeLast(2)
                                                        minuteInputBuffer = newBuf
                                                        val parsed = newBuf.toIntOrNull() ?: digit
                                                        selectedMinute = minOf(59, parsed)
                                                    }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (key == "DEL") {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                                            contentDescription = null,
                                            tint = colors.textPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        Text(
                                            text = key,
                                            style = MaterialTheme.typography.titleMedium.copy(
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
                Button(
                    onClick = {
                        val formatted = "${selectedHour.toString().padStart(2, '0')}:${selectedMinute.toString().padStart(2, '0')}"
                        settingsRepository.update {
                            it.copy(notifications = it.notifications.copy(reminderTime = formatted))
                        }
                        workoutReminderManager.syncSettings(settings.notifications.copy(reminderTime = formatted), settings.appearance.language)
                        showCustomTimeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accentFill,
                        contentColor = colors.onAccentFill
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = s.save, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomTimeDialog = false }) {
                    Text(text = s.cancel, style = MaterialTheme.typography.labelLarge.copy(color = colors.textSecondary))
                }
            }
        )
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
    val s = LocalStrings.current

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
                        .clickable(onClick = {
                            SoundManager.stopSound()
                            onNavigateBack()
                        }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = s.back,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = s.workoutsSettingsRow,
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
                                    text = s.soundVibrationTitle,
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
                                    text = s.defaultRestTimeLabel,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Text(
                                    text = "${settings.logging.restTimerDefaultSec / 60}${s.minAbbrev} ${s.secondsShort(settings.logging.restTimerDefaultSec % 60)}",
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
                                listOf(60 to "60s", 90 to "90s", 120 to "120s", 180 to "3${s.minAbbrev}").forEach { (sec, label) ->
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
                                    text = s.firstDayOfWeekTitle,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(FirstDayOfWeek.MONDAY to s.daysMondayStart[0], FirstDayOfWeek.SUNDAY to s.daysSundayStart[0]).forEach { (day, label) ->
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
                                title = s.keepAwakeTitle,
                                description = s.keepAwakeSubtitle,
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
                                title = s.plateCalculatorTitle,
                                description = s.plateCalculatorSubtitle,
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
                                title = s.rpeOptionTitle,
                                description = s.rpeOptionSubtitle,
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
                                title = s.smartSupersetTitle,
                                description = s.smartSupersetSubtitle,
                                isChecked = settings.logging.smartSupersetScrolling,
                                onCheckedChange = { chk ->
                                    settingsRepository.update {
                                        it.copy(logging = it.logging.copy(smartSupersetScrolling = chk))
                                    }
                                }
                            )


                            // Auto-start Rest Timer
                            Spacer(modifier = Modifier.height(14.dp))
                            SettingsDivider()
                            Spacer(modifier = Modifier.height(14.dp))

                            WorkoutSettingToggle(
                                title = s.autoRestTimerTitle,
                                description = s.autoRestTimerSubtitle,
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
                                title = s.confirmFinishTitle,
                                description = s.confirmFinishSubtitle,
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
                                text = s.calculationsHeader,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = s.estimated1RmFormulaTitle,
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
                                    text = s.maxRepsFor1RmTitle,
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
                                title = s.countWarmupInVolumeTitle,
                                description = s.countWarmupInVolumeSubtitle,
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
    val s = LocalStrings.current

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
                        .clickable(onClick = {
                            SoundManager.stopSound()
                            onNavigateBack()
                        }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = s.back,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = s.selectUnitsTitle,
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
                    title = s.weightUnitTitle,
                    options = listOf(WeightUnit.KG to s.unitKg, WeightUnit.LB to s.unitLb),
                    selected = settings.units.weightUnit,
                    onSelect = { unit ->
                        settingsRepository.update {
                            it.copy(units = it.units.copy(weightUnit = unit))
                        }
                    }
                )

                // Distance (kilometers / miles)
                UnitOptionGroup(
                    title = s.distanceUnitTitle,
                    options = listOf(DistanceUnit.KILOMETERS to s.distanceUnitKm, DistanceUnit.MILES to s.distanceUnitMiles),
                    selected = settings.units.distanceUnit,
                    onSelect = { unit ->
                        settingsRepository.update {
                            it.copy(units = it.units.copy(distanceUnit = unit))
                        }
                    }
                )

                // Body Measurements (cm / in)
                UnitOptionGroup(
                    title = s.sizeUnitTitle,
                    options = listOf(BodyMeasurementUnit.CM to s.sizeUnitCm, BodyMeasurementUnit.IN to s.sizeUnitIn),
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
fun <T> UnitOptionGroup(
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
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { (option, label) ->
                val isSelected = option == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) colors.accentFill else Color.Transparent
                        )
                        .clickable { onSelect(option) }
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
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
    val s = LocalStrings.current

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
                        .clickable(onClick = {
                            SoundManager.stopSound()
                            onNavigateBack()
                        }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = s.back,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = s.languageSettingsRow,
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
                    Triple(AppLanguage.SYSTEM, s.systemLanguageTitle, s.systemLanguageSubtitle),
                    Triple(AppLanguage.ID, s.indonesianLanguage, s.indonesianLanguageSubtitle),
                    Triple(AppLanguage.EN, s.englishLanguage, s.englishLanguageSubtitle)
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
    val s = LocalStrings.current

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
                        .clickable(onClick = {
                            SoundManager.stopSound()
                            onNavigateBack()
                        }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = s.back,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = s.selectThemeTitle,
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
                    Pair(AppTheme.SYSTEM, s.themeSystem),
                    Pair(AppTheme.DARK, s.themeDark),
                    Pair(AppTheme.LIGHT, s.themeLight)
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
    val s = LocalStrings.current
    val backupManager = remember(databaseRepo) {
        databaseRepo?.let { DataBackupManager(it) }
    }

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var pendingImportContent by remember { mutableStateOf<String?>(null) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var isRunningBackupNow by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    var pendingImportBytes by remember { mutableStateOf<ByteArray?>(null) }
    var showPasswordPromptDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var showSetPasswordExportDialog by remember { mutableStateOf(false) }
    var exportPasswordInput by remember { mutableStateOf("") }

    val exportBytesLauncher = rememberDataExportBytesLauncher(strings = s) { success, msg ->
        statusMessage = msg
        if (success) {
            settingsRepository.update {
                it.copy(dataBackup = it.dataBackup.copy(lastBackupEpochMs = currentEpochMillis()))
            }
        }
    }

    val importBytesLauncher = rememberDataImportBytesLauncher { bytes ->
        if (bytes != null) {
            val inspection = BackupCrypto.inspectFile(bytes)
            if (inspection.type == BackupFileType.PASSWORD_ENCRYPTED) {
                pendingImportBytes = bytes
                passwordInput = ""
                showPasswordPromptDialog = true
            } else {
                val unpackResult = backupManager?.unpackBackupPayload(bytes)
                if (unpackResult?.isSuccess == true) {
                    pendingImportContent = unpackResult.getOrNull()
                } else {
                    statusMessage = s.failedToRestoreBackup
                }
            }
        } else {
            statusMessage = s.importCancelledOrNoFile
        }
    }

    val folderPickerLauncher = rememberFolderPickerLauncher { uri, name ->
        if (uri != null) {
            settingsRepository.update { current ->
                current.copy(
                    dataBackup = current.dataBackup.copy(
                        autoBackupEnabled = true,
                        autoBackupFolderUri = uri,
                        autoBackupFolderName = name ?: "Backup Folder",
                        lastBackupResultError = null
                    )
                )
            }
            statusMessage = s.backupFolderConfiguredSuccess
        } else {
            if (settings.dataBackup.autoBackupFolderUri == null) {
                settingsRepository.update { current ->
                    current.copy(
                        dataBackup = current.dataBackup.copy(autoBackupEnabled = false)
                    )
                }
            }
        }
    }

    val exportLauncher = rememberDataExportLauncher(strings = s) { success, msg ->
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
            statusMessage = s.importCancelledOrNoFile
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
                    text = s.restoreDataDialogTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = s.restoreDataDialogBody,
                    style = MaterialTheme.typography.bodyMedium
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
                            strings = s,
                            onSettingsRestored = { settingsRepository.save(it) }
                        )
                        statusMessage = result?.message ?: s.failedToRestoreBackup
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accentText)
                ) {
                    Text(
                        text = s.replaceFullButton,
                        color = colors.onAccentFill,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { pendingImportContent = null }) {
                        Text(s.cancel, style = MaterialTheme.typography.labelLarge.copy(color = colors.textSecondary))
                    }
                    Button(
                        onClick = {
                            val content = pendingImportContent!!
                            pendingImportContent = null
                            val result = backupManager?.importBackup(
                                jsonString = content,
                                mode = ImportMode.MERGE,
                                currentSettings = settings,
                                strings = s,
                                onSettingsRestored = { settingsRepository.save(it) }
                            )
                            statusMessage = result?.message ?: s.failedToMergeBackup
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KumaColors.SurfaceContainerHigh)
                    ) {
                        Text(
                            text = s.mergeButton,
                            color = colors.textPrimary,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        )
    }

    // Dialog for Prompting Password on Encrypted Backup Import
    if (showPasswordPromptDialog && pendingImportBytes != null) {
        AlertDialog(
            onDismissRequest = {
                showPasswordPromptDialog = false
                pendingImportBytes = null
            },
            title = {
                Text(
                    text = s.setBackupPasswordTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = s.encryptBackupsSubtitle,
                        style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                    )
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text(s.passwordFieldLabel, style = MaterialTheme.typography.labelMedium) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val bytes = pendingImportBytes!!
                        val unpackResult = backupManager?.unpackBackupPayload(bytes, passwordInput)
                        showPasswordPromptDialog = false
                        pendingImportBytes = null
                        if (unpackResult?.isSuccess == true) {
                            pendingImportContent = unpackResult.getOrNull()
                        } else {
                            statusMessage = BackupErrorCode.WRONG_PASSWORD_OR_CORRUPTED.localizedMessage(s)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accentText)
                ) {
                    Text(s.done, color = colors.onAccentFill, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPasswordPromptDialog = false
                    pendingImportBytes = null
                }) {
                    Text(s.cancel, style = MaterialTheme.typography.labelLarge.copy(color = colors.textSecondary))
                }
            }
        )
    }

    // Dialog for Setting Password on Encrypted Backup Export
    if (showSetPasswordExportDialog) {
        AlertDialog(
            onDismissRequest = { showSetPasswordExportDialog = false },
            title = {
                Text(
                    text = s.encryptBackupsTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = s.backupPasswordWarning,
                        style = MaterialTheme.typography.bodySmall.copy(color = colors.warning)
                    )
                    OutlinedTextField(
                        value = exportPasswordInput,
                        onValueChange = { exportPasswordInput = it },
                        label = { Text(s.passwordFieldLabel, style = MaterialTheme.typography.labelMedium) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pass = exportPasswordInput.trim()
                        showSetPasswordExportDialog = false
                        exportPasswordInput = ""
                        if (pass.isNotEmpty() && backupManager != null) {
                            val encryptedBytes = backupManager.exportPackage(settings, pass)
                            val now = currentEpochMillis()
                            exportBytesLauncher("KumaLift_Encrypted_$now.kumalift", "application/octet-stream", encryptedBytes)
                        }
                    },
                    enabled = exportPasswordInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accentText)
                ) {
                    Text(s.save, color = colors.onAccentFill, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetPasswordExportDialog = false }) {
                    Text(s.cancel, style = MaterialTheme.typography.labelLarge.copy(color = colors.textSecondary))
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
                    text = s.clearHistoryConfirmTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = KumaColors.ErrorRed)
                )
            },
            text = {
                Text(
                    text = s.clearHistoryDialogBody,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearHistoryDialog = false
                        databaseRepo?.clearWorkoutHistoryOnly()
                        statusMessage = s.workoutHistoryClearedSuccess
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KumaColors.ErrorRed)
                ) {
                    Text(s.deleteHistoryButton, color = Color.White, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text(s.cancel, style = MaterialTheme.typography.labelLarge.copy(color = colors.textSecondary))
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
                    text = s.deleteAllDataConfirmTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = KumaColors.ErrorRed)
                )
            },
            text = {
                Text(
                    text = s.deleteAllDataDialogBody,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAllDialog = false
                        databaseRepo?.wipeAllUserData()
                        settingsRepository.save(UserSettings())
                        statusMessage = s.allDataDeletedSuccess
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KumaColors.ErrorRed)
                ) {
                    Text(s.deleteEverythingButton, color = Color.White, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text(s.cancel, style = MaterialTheme.typography.labelLarge.copy(color = colors.textSecondary))
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
                        .clickable(onClick = {
                            SoundManager.stopSound()
                            onNavigateBack()
                        }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = s.back,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = s.exportImportSettingsRow,
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
                // Section: Local Folder Auto-Backup
                item {
                    Text(
                        text = s.autoBackupHeader,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = colors.accentText,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    KumaCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // 1. Master Toggle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Storage,
                                        contentDescription = null,
                                        tint = colors.accentText
                                    )
                                    Column {
                                        Text(
                                            text = s.autoBackupToFolderTitle,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.textPrimary
                                            )
                                        )
                                        Text(
                                            text = s.autoBackupToFolderSubtitle,
                                            style = MaterialTheme.typography.bodySmall.copy(color = KumaColors.TextMuted)
                                        )
                                    }
                                }
                                Switch(
                                    checked = settings.dataBackup.autoBackupEnabled,
                                    onCheckedChange = { isChecked ->
                                        if (isChecked) {
                                            if (settings.dataBackup.autoBackupFolderUri.isNullOrBlank()) {
                                                folderPickerLauncher()
                                            } else {
                                                settingsRepository.update { current ->
                                                    current.copy(
                                                        dataBackup = current.dataBackup.copy(
                                                            autoBackupEnabled = true
                                                        )
                                                    )
                                                }
                                            }
                                        } else {
                                            settingsRepository.update { current ->
                                                current.copy(
                                                    dataBackup = current.dataBackup.copy(
                                                        autoBackupEnabled = false
                                                    )
                                                )
                                            }
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = colors.accentText,
                                        checkedTrackColor = colors.accentText.copy(alpha = 0.35f)
                                    )
                                )
                            }

                            SettingsDivider()

                            // 2. Destination Folder Selector Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { folderPickerLauncher() }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = colors.accentText
                                    )
                                    Column {
                                        Text(
                                            text = s.autoBackupFolderLabel,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.textPrimary
                                            )
                                        )
                                        Text(
                                            text = settings.dataBackup.autoBackupFolderName
                                                ?: s.autoBackupNoFolderSelected,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = if (settings.dataBackup.autoBackupFolderName != null)
                                                    colors.textSecondary else colors.warning
                                            )
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.surfaceVariant)
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = s.autoBackupSelectFolder,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = colors.accentText,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }

                            // 3. Frequency & Retention Settings
                            if (settings.dataBackup.autoBackupEnabled) {
                                SettingsDivider()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = colors.accentText
                                        )
                                        Text(
                                            text = s.autoBackupFrequencyLabel,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Medium,
                                                color = colors.textPrimary
                                            )
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(
                                            BackupFrequency.DAILY,
                                            BackupFrequency.WEEKLY,
                                            BackupFrequency.MONTHLY
                                        ).forEach { freq ->
                                            val label = freq.label(s)
                                            val isSelected = settings.dataBackup.autoBackupFrequency == freq
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (isSelected) colors.accentFill else colors.surfaceVariant
                                                    )
                                                    .clickable {
                                                        settingsRepository.update { current ->
                                                            current.copy(
                                                                dataBackup = current.dataBackup.copy(
                                                                    autoBackupFrequency = freq
                                                                )
                                                            )
                                                        }
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = if (isSelected) colors.onAccentFill else colors.textPrimary,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }

                                SettingsDivider()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FilterList,
                                            contentDescription = null,
                                            tint = colors.accentText
                                        )
                                        Text(
                                            text = s.autoBackupKeepLastLabel,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Medium,
                                                color = colors.textPrimary
                                            )
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(3, 5, 10).forEach { count ->
                                            val isSelected = settings.dataBackup.autoBackupKeepLast == count
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (isSelected) colors.accentFill else colors.surfaceVariant
                                                    )
                                                    .clickable {
                                                        settingsRepository.update { current ->
                                                            current.copy(
                                                                dataBackup = current.dataBackup.copy(
                                                                    autoBackupKeepLast = count
                                                                )
                                                            )
                                                        }
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = "$count",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = if (isSelected) colors.onAccentFill else colors.textPrimary,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            SettingsDivider()

                            // 4. "Backup Now" Action Button
                            SettingsNavigationRow(
                                icon = Icons.Default.PlayArrow,
                                title = if (isRunningBackupNow) s.autoBackupRunNow + "..." else s.autoBackupRunNow,
                                subtitle = settings.dataBackup.autoBackupFolderUri?.let {
                                    s.backupNowChooseLocationSubtitle
                                } ?: s.autoBackupNoFolderSelected,
                                onClick = {
                                    if (databaseRepo != null) {
                                        if (settings.dataBackup.autoBackupFolderUri.isNullOrBlank()) {
                                            folderPickerLauncher()
                                        } else {
                                            coroutineScope.launch {
                                                isRunningBackupNow = true
                                                val result = AutoBackupService.runBackupNow(databaseRepo, settings)
                                                isRunningBackupNow = false
                                                if (result.success) {
                                                    statusMessage = s.autoBackupStatusOk(result.fileName ?: "")
                                                } else {
                                                    val code = result.errorCode ?: BackupErrorCode.fromCode(result.errorMessage)
                                                    statusMessage = s.autoBackupStatusFailed(code.localizedMessage(s))
                                                }
                                            }
                                        }
                                    } else {
                                        statusMessage = s.databaseRepoUnavailable
                                    }
                                }
                            )

                            SettingsDivider()

                            // 5. Status line: "Last backup: Today 03:00 (Success)" or "Failed: Folder not found" or "Never"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val lastEpoch = settings.dataBackup.lastBackupEpochMs
                                val lastSuccess = settings.dataBackup.lastBackupResultSuccess
                                val lastError = settings.dataBackup.lastBackupResultError
                                val lastFileName = settings.dataBackup.lastBackupFileName

                                Text(
                                    text = s.lastBackupTitle,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = colors.textSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Text(
                                    text = when {
                                        lastEpoch == null -> s.never
                                        lastSuccess == true -> s.autoBackupStatusOk(
                                            formatEpochToDateString(lastEpoch) + (lastFileName?.let { " ($it)" } ?: "")
                                        )
                                        lastSuccess == false -> s.autoBackupStatusFailed(
                                            settings.dataBackup.localizedError(s) ?: s.backupErrorFolderInaccessible
                                        )
                                        else -> formatEpochToDateString(lastEpoch)
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = when (lastSuccess) {
                                            true -> colors.recoveryFresh
                                            false -> colors.error
                                            null -> colors.accentText
                                        },
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }
                }

                // Section 1: Backup & Restore Actions
                item {
                    Text(
                        text = s.backupRestoreLocalHeader,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = colors.accentText,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = s.manualExportGoogleDriveNote,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = colors.textSecondary
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    KumaCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SettingsNavigationRow(
                                icon = Icons.Default.FileDownload,
                                title = s.exportBackupJsonTitle,
                                subtitle = s.exportBackupJsonSubtitle,
                                onClick = {
                                    if (backupManager != null) {
                                        val jsonPayload = backupManager.exportJson(settings)
                                        val now = currentEpochMillis()
                                        exportLauncher("kumalift_backup_$now.json", jsonPayload)
                                    } else {
                                        statusMessage = s.databaseRepoUnavailable
                                    }
                                }
                            )
                            SettingsDivider()
                            SettingsNavigationRow(
                                icon = Icons.Default.EnhancedEncryption,
                                title = s.encryptBackupsTitle,
                                subtitle = s.encryptBackupsSubtitle,
                                onClick = {
                                    if (backupManager != null) {
                                        exportPasswordInput = ""
                                        showSetPasswordExportDialog = true
                                    } else {
                                        statusMessage = s.databaseRepoUnavailable
                                    }
                                }
                            )
                            SettingsDivider()
                            SettingsNavigationRow(
                                icon = Icons.Default.FileUpload,
                                title = s.restoreFromBackupTitle,
                                subtitle = s.restoreFromBackupSubtitle,
                                onClick = {
                                    importBytesLauncher()
                                }
                            )
                            SettingsDivider()
                            SettingsNavigationRow(
                                icon = Icons.Default.Restore,
                                title = s.restoreSafetySnapshotTitle,
                                subtitle = s.restoreSafetySnapshotSub,
                                onClick = {
                                    if (backupManager != null) {
                                        val result = backupManager.restoreSafetySnapshot(
                                            currentSettings = settings,
                                            strings = s
                                        ) {
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
                        text = s.csvExportHeader,
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
                                title = s.exportWorkoutsCsvTitle,
                                subtitle = s.exportWorkoutsCsvSubtitle,
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

                // Section 3: Diagnostic Logging
                item {
                    Text(
                        text = s.diagnosticLogHeader,
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = s.localLoggingTitle,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = colors.textPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = s.localLoggingSubtitle,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = colors.textSecondary
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Switch(
                                    checked = settings.security.localLogging,
                                    onCheckedChange = { checked: Boolean ->
                                        settingsRepository.update { curr ->
                                            curr.copy(security = curr.security.copy(localLogging = checked))
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = colors.accentText,
                                        checkedTrackColor = colors.accentText.copy(alpha = 0.35f)
                                    )
                                )
                            }
                            if (settings.security.localLogging) {
                                SettingsDivider()
                                SettingsNavigationRow(
                                    icon = Icons.Default.FileDownload,
                                    title = s.exportDiagnosticLogTitle,
                                    subtitle = "${s.exportDiagnosticLogSubtitle} (${s.diagnosticLogEntriesCount(KumaLogger.logCount())})",
                                    onClick = {
                                        val logs = KumaLogger.getLogs()
                                        if (logs.isNotBlank()) {
                                            val now = currentEpochMillis()
                                            exportLauncher("kumalift_diagnostic_$now.log", logs)
                                        } else {
                                            statusMessage = s.noDiagnosticLogsYet
                                        }
                                    }
                                )
                                SettingsDivider()
                                SettingsNavigationRow(
                                    icon = Icons.Default.DeleteForever,
                                    title = s.clearDiagnosticLogTitle,
                                    subtitle = s.clearDiagnosticLogSubtitle,
                                    onClick = {
                                        KumaLogger.clear()
                                        statusMessage = s.diagnosticLogCleared
                                    }
                                )
                            }
                        }
                    }
                }

                // Section 4: Read-only Data Metrics
                item {
                    Text(
                        text = s.storageStatusHeader,
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
                                    text = s.storageUsedTitle,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = storageUsage?.let {
                                        s.storageUsedSubtitle(it.workoutsCount, it.setsCount)
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
                                    text = s.lastBackupTitle,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = settings.dataBackup.lastBackupEpochMs?.let {
                                        s.recorded
                                    } ?: s.never,
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
                        text = s.dangerZoneGroupTitle,
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
                                title = s.clearWorkoutHistoryTitle,
                                subtitle = s.clearWorkoutHistorySubtitle,
                                onClick = { showClearHistoryDialog = true }
                            )
                            SettingsDivider()
                            SettingsNavigationRow(
                                icon = Icons.Default.DeleteForever,
                                title = s.deleteAllDataActionTitle,
                                subtitle = s.deleteAllDataActionSubtitle,
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

@Composable
private fun AboutSettingsPage(
    onNavigateBack: () -> Unit
) {
    val colors = LocalKumaColors.current
    val s = LocalStrings.current

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
                        .clickable(onClick = {
                            SoundManager.stopSound()
                            onNavigateBack()
                        }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = s.back,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = s.aboutTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "KumaLift",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.textPrimary
                            )
                        )
                    }
                }

                item {
                    Text(
                        text = s.mediaAttributionHeader,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = colors.textSecondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    KumaCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = s.exerciseMediaAttributionTitle,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                            )
                            Text(
                                text = s.exerciseMediaAttributionBody,
                                style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = s.openSourceLicensesHeader,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = colors.textSecondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    KumaCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            listOf(
                                "exercises-dataset" to "Hasan Eyldrm (MIT License)",
                                "Compose Multiplatform" to "JetBrains / Google (Apache 2.0)",
                                "SQLDelight" to "Cash App (Apache 2.0)",
                                "Coil" to "Coil-kt Contributors (Apache 2.0)",
                                "Koin" to "Kotzilla (Apache 2.0)"
                            ).forEach { (lib, lic) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = lib,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.textPrimary
                                        )
                                    )
                                    Text(
                                        text = lic,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = colors.textSecondary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "v1.0.0",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = colors.textSecondary.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }
    }
}
