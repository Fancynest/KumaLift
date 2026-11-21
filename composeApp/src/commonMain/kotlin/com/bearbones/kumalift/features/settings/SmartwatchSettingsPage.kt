package com.bearbones.kumalift.features.settings

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumalift.core.bluetooth.BleHeartRateManager
import com.bearbones.kumalift.core.bluetooth.rememberBluetoothPermissionLauncher
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

    val isBleScanning by BleHeartRateManager.isScanning.collectAsState()
    val discoveredBleDevices by BleHeartRateManager.discoveredDevices.collectAsState()
    val connectedBleDevice by BleHeartRateManager.connectedDevice.collectAsState()
    val currentBleBpm by BleHeartRateManager.currentHeartRateBpm.collectAsState()

    val deviceName = when (SmartwatchCompanionManager.deviceType) {
        SmartwatchDeviceType.WEAR_OS -> "Wear OS"
        SmartwatchDeviceType.APPLE_WATCH -> "Apple Watch"
        SmartwatchDeviceType.GENERIC_COMPANION -> "Smartwatch"
    }

    val isConnected = watchStatus == SmartwatchConnectionStatus.CONNECTED
    val isConnecting = watchStatus == SmartwatchConnectionStatus.CONNECTING
    val statusColor = if (isConnected) colors.recoveryFresh else colors.warning
    val statusText = if (isConnected) s.smartwatchConnectedStatus else s.smartwatchDisconnectedStatus

    var bluetoothErrorMessage by remember { mutableStateOf<String?>(null) }

    val requestBluetoothPermission = rememberBluetoothPermissionLauncher { granted ->
        if (granted) {
            if (!BleHeartRateManager.isBluetoothEnabled) {
                bluetoothErrorMessage = s.bluetoothTurnOnPrompt
            } else {
                bluetoothErrorMessage = null
                BleHeartRateManager.startScan()
            }
        } else {
            bluetoothErrorMessage = s.bluetoothPermissionRequired
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

                        // Non-Wear OS notice (Huawei Band, Mi Band, Garmin)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surfaceVariant.copy(alpha = 0.6f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = s.wearOsNoticeText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = colors.textSecondary,
                                    fontSize = 10.sp
                                )
                            )
                        }

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
                            if (isConnecting) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = colors.onAccentFill
                                    )
                                    Text(
                                        text = s.smartwatchConnectingStatus,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = colors.onAccentFill
                                        )
                                    )
                                }
                            } else {
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

                // 3. Bluetooth Heart Rate Monitor (Smartband / Strap) Card
                KumaCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = KumaColors.SurfaceContainerLow,
                    cornerRadius = 16.dp
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = colors.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = s.bleHeartRateTitle,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                )
                            }

                            if (connectedBleDevice != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(PillShape)
                                        .background(colors.recoveryFresh.copy(alpha = 0.15f))
                                        .border(1.dp, colors.recoveryFresh, PillShape)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = s.bleConnectedDeviceFormat(
                                            connectedBleDevice?.name ?: "",
                                            currentBleBpm ?: 0
                                        ),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = colors.recoveryFresh,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }

                        Text(
                            text = s.bleHeartRateSubtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = colors.textSecondary
                            )
                        )

                        if (connectedBleDevice != null) {
                            val infiniteTransition = rememberInfiniteTransition()
                            val heartScale by infiniteTransition.animateFloat(
                                initialValue = 0.92f,
                                targetValue = 1.18f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                )
                            )

                            // Hero Live Telemetry Container
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.surfaceVariant)
                                    .padding(16.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(colors.recoveryFresh.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.BluetoothConnected,
                                                    contentDescription = null,
                                                    tint = colors.recoveryFresh,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = connectedBleDevice?.name ?: "",
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = colors.textPrimary
                                                    )
                                                )
                                                Text(
                                                    text = s.bleBroadcastingStatus,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = colors.recoveryFresh,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    // Live Pulse Counter
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Favorite,
                                            contentDescription = null,
                                            tint = colors.error,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .scale(heartScale)
                                        )
                                        Row(
                                            verticalAlignment = Alignment.Bottom,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = if (currentBleBpm != null && currentBleBpm!! > 0) "${currentBleBpm}" else "--",
                                                style = MaterialTheme.typography.headlineLarge.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = colors.textPrimary,
                                                    fontSize = 38.sp
                                                )
                                            )
                                            Text(
                                                text = s.bleBpmUnit,
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = colors.textSecondary
                                                ),
                                                modifier = Modifier.padding(bottom = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.surfaceVariant)
                                    .border(1.dp, colors.error.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .clickable { BleHeartRateManager.disconnect() }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = s.bleDisconnectAction,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = colors.error
                                    )
                                )
                            }
                        } else {
                            val currentBleError = bluetoothErrorMessage
                            if (currentBleError != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.error.copy(alpha = 0.15f))
                                        .border(1.dp, colors.error.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = currentBleError,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = colors.error,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isBleScanning) colors.surfaceVariant else colors.accentFill)
                                    .clickable {
                                        if (isBleScanning) {
                                            BleHeartRateManager.stopScan()
                                        } else {
                                            requestBluetoothPermission()
                                        }
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (isBleScanning) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = colors.accentText
                                        )
                                    }
                                    Text(
                                        text = if (isBleScanning) s.bleStopScanAction else s.bleScanAction,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isBleScanning) colors.accentText else colors.onAccentFill
                                        )
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.surfaceVariant.copy(alpha = 0.6f))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = colors.accentText,
                                    modifier = Modifier.size(18.dp).padding(top = 2.dp)
                                )
                                Text(
                                    text = s.bleBroadcastTip,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = colors.textSecondary,
                                        lineHeight = 16.sp
                                    )
                                )
                            }

                            if (discoveredBleDevices.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    discoveredBleDevices.forEach { dev ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(colors.surfaceVariant)
                                                .clickable { BleHeartRateManager.connect(dev) }
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(KumaColors.SurfaceContainerHighest),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Bluetooth,
                                                        contentDescription = null,
                                                        tint = colors.accentText,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Column {
                                                    Text(
                                                        text = dev.name,
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = colors.textPrimary
                                                        )
                                                    )
                                                    Text(
                                                        text = s.bleReadyToPair,
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = colors.textSecondary
                                                        )
                                                    )
                                                }
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(colors.accentFill)
                                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = s.smartwatchConnectAction,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = colors.onAccentFill
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            } else if (isBleScanning) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = s.bleScanningStatus,
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
        }
    }
}
