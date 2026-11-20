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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Watch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumalift.core.designsystem.KumaCard
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.core.localization.LocalStrings
import com.bearbones.kumalift.core.smartwatch.SmartwatchCompanionManager
import com.bearbones.kumalift.data.repository.SettingsRepository
import com.bearbones.kumalift.domain.model.SmartwatchConnectionStatus
import com.bearbones.kumalift.domain.model.SmartwatchDeviceType

@Composable
fun SmartwatchSettingsPage(
    settingsRepository: SettingsRepository,
    onNavigateBack: () -> Unit
) {
    val settings by settingsRepository.settings.collectAsState()
    val colors = LocalKumaColors.current
    val s = LocalStrings.current
    val watchStatus by SmartwatchCompanionManager.connectionStatus.collectAsState()

    val deviceName = when (SmartwatchCompanionManager.deviceType) {
        SmartwatchDeviceType.WEAR_OS -> "Wear OS"
        SmartwatchDeviceType.APPLE_WATCH -> "Apple Watch"
        SmartwatchDeviceType.GENERIC_COMPANION -> "Smartwatch"
    }

    val isConnected = watchStatus == SmartwatchConnectionStatus.CONNECTED
    val statusColor = if (isConnected) colors.recoveryFresh else colors.warning
    val statusText = if (isConnected) s.smartwatchConnectedStatus else s.smartwatchDisconnectedStatus

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
                    text = s.smartwatchSettingsTitle,
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
                // Connection Status Card
                KumaCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = KumaColors.SurfaceContainerLow,
                    cornerRadius = 16.dp
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Watch,
                                    contentDescription = null,
                                    tint = colors.accentText,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = deviceName,
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
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        Text(
                            text = s.smartwatchSettingsSubtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = colors.textSecondary
                            )
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isConnected) colors.surfaceVariant else colors.accentFill)
                                .clickable {
                                    if (isConnected) {
                                        SmartwatchCompanionManager.disconnect()
                                    } else {
                                        SmartwatchCompanionManager.connect()
                                    }
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isConnected) s.smartwatchDisconnectAction else s.smartwatchConnectAction,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isConnected) colors.textPrimary else colors.onAccentFill
                                )
                            )
                        }
                    }
                }

                // Companion Preferences Card
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = s.smartwatchCompanionEnabled,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                            Switch(
                                checked = settings.smartwatch.companionEnabled,
                                onCheckedChange = { checked ->
                                    settingsRepository.update {
                                        it.copy(smartwatch = it.smartwatch.copy(companionEnabled = checked))
                                    }
                                    if (checked) {
                                        SmartwatchCompanionManager.connect()
                                    } else {
                                        SmartwatchCompanionManager.disconnect()
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = colors.accentText,
                                    checkedTrackColor = colors.accentText.copy(alpha = 0.35f)
                                )
                            )
                        }

                        // Live Timer Sync
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = s.smartwatchLiveTimerSync,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Text(
                                    text = s.smartwatchLiveTimerSyncDesc,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = colors.textSecondary
                                    )
                                )
                            }
                            Switch(
                                checked = settings.smartwatch.liveTimerSync,
                                onCheckedChange = { checked ->
                                    settingsRepository.update {
                                        it.copy(smartwatch = it.smartwatch.copy(liveTimerSync = checked))
                                    }
                                },
                                enabled = settings.smartwatch.companionEnabled,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = colors.accentText,
                                    checkedTrackColor = colors.accentText.copy(alpha = 0.35f)
                                )
                            )
                        }

                        // Haptics on Watch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = s.smartwatchHaptics,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Text(
                                    text = s.smartwatchHapticsDesc,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = colors.textSecondary
                                    )
                                )
                            }
                            Switch(
                                checked = settings.smartwatch.hapticAlertsOnWatch,
                                onCheckedChange = { checked ->
                                    settingsRepository.update {
                                        it.copy(smartwatch = it.smartwatch.copy(hapticAlertsOnWatch = checked))
                                    }
                                },
                                enabled = settings.smartwatch.companionEnabled,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = colors.accentText,
                                    checkedTrackColor = colors.accentText.copy(alpha = 0.35f)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
