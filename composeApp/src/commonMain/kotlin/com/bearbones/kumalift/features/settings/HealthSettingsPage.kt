package com.bearbones.kumalift.features.settings

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Sync
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumalift.core.designsystem.KumaCard
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.core.health.HealthSyncManager
import com.bearbones.kumalift.core.localization.LocalStrings
import com.bearbones.kumalift.data.repository.SettingsRepository
import com.bearbones.kumalift.domain.model.HealthPlatformType
import com.bearbones.kumalift.domain.model.HealthSyncStatus
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HealthSettingsPage(
    settingsRepository: SettingsRepository,
    onNavigateBack: () -> Unit
) {
    val settings by settingsRepository.settings.collectAsState()
    val colors = LocalKumaColors.current
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()

    var syncStatus by remember { mutableStateOf(HealthSyncManager.getStatus()) }

    LifecycleResumeEffect(Unit) {
        scope.launch {
            syncStatus = HealthSyncManager.refreshStatus()
        }
        onPauseOrDispose {}
    }

    val platformName = when (HealthSyncManager.platformType) {
        HealthPlatformType.HEALTH_CONNECT -> s.healthConnectLabel
        HealthPlatformType.HEALTH_KIT -> s.appleHealthLabel
        HealthPlatformType.NONE -> s.healthSettingsTitle
    }

    val (statusColor, statusText) = when (syncStatus) {
        HealthSyncStatus.CONNECTED -> Pair(colors.recoveryFresh, s.healthConnectedStatus)
        HealthSyncStatus.PERMISSION_REQUIRED -> Pair(colors.warning, s.healthPermissionRequiredStatus)
        HealthSyncStatus.UNAVAILABLE -> Pair(colors.error, s.healthUnavailableStatus)
        HealthSyncStatus.NOT_CONFIGURED -> Pair(colors.textSecondary, s.healthUnavailableStatus)
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
                        contentDescription = s.back,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = s.healthSettingsTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Platform Status Card
                KumaCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = KumaColors.SurfaceContainerLow,
                    cornerRadius = 16.dp
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = colors.accentText,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = platformName,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(PillShape)
                                    .background(statusColor.copy(alpha = 0.15f))
                                    .border(1.dp, statusColor, PillShape)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = statusText,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        Text(
                            text = s.healthSettingsSubtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = colors.textSecondary
                            )
                        )

                        if (syncStatus == HealthSyncStatus.PERMISSION_REQUIRED) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.accentFill)
                                    .clickable {
                                        scope.launch {
                                            val granted = HealthSyncManager.requestPermissions()
                                            syncStatus = HealthSyncManager.refreshStatus()
                                            if (granted) {
                                                settingsRepository.update {
                                                    it.copy(health = it.health.copy(healthEnabled = true))
                                                }
                                            }
                                        }
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = null,
                                        tint = colors.onAccentFill,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = s.requestHealthPermissionButton,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = colors.onAccentFill
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Integration Toggles Card
                KumaCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = KumaColors.SurfaceContainerLow,
                    cornerRadius = 16.dp
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Master Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = s.healthSyncEnabled,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                            Switch(
                                checked = settings.health.healthEnabled,
                                onCheckedChange = { checked ->
                                    settingsRepository.update {
                                        it.copy(health = it.health.copy(healthEnabled = checked))
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = colors.accentText,
                                    checkedTrackColor = colors.accentText.copy(alpha = 0.35f)
                                )
                            )
                        }

                        // Write Workouts Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = s.healthSyncWorkouts,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Text(
                                    text = s.healthSyncWorkoutsDesc,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = colors.textSecondary
                                    )
                                )
                            }
                            Switch(
                                checked = settings.health.writeWorkouts,
                                onCheckedChange = { checked ->
                                    settingsRepository.update {
                                        it.copy(health = it.health.copy(writeWorkouts = checked))
                                    }
                                },
                                enabled = settings.health.healthEnabled,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = colors.accentText,
                                    checkedTrackColor = colors.accentText.copy(alpha = 0.35f)
                                )
                            )
                        }

                        // Calories Description Notice
                        Text(
                            text = s.healthSyncCaloriesDesc,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = colors.textSecondary
                            )
                        )
                    }
                }
            }
        }
    }
}
