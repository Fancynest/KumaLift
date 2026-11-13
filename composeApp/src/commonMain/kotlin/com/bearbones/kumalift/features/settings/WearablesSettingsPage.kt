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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.bearbones.kumalift.core.wearable.WearableConnectionState
import com.bearbones.kumalift.core.wearable.WearableManager
import com.bearbones.kumalift.data.repository.SettingsRepository

@Composable
fun WearablesSettingsPage(
    settingsRepository: SettingsRepository,
    wearableManager: WearableManager,
    onNavigateBack: () -> Unit
) {
    val settings by settingsRepository.settings.collectAsState()
    val telemetry by wearableManager.telemetry.collectAsState()
    val isScanning by wearableManager.isScanningBle.collectAsState()
    val discoveredDevices by wearableManager.discoveredDevices.collectAsState()
    val colors = LocalKumaColors.current

    val isHcAvailable = wearableManager.isHealthConnectAvailable()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KumaColors.CanvasLowest)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(KumaColors.SurfaceDim)
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(KumaColors.SurfaceContainer)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = KumaColors.OnSurface
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "Perangkat & Wearable",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = KumaColors.TextWhite
                    )
                )
                Text(
                    text = "Health Connect & Bluetooth Sensor",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = KumaColors.TextMuted
                    )
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))

                // Current Telemetry Live Status Card
                KumaCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = KumaColors.SurfaceContainerLow,
                    cornerRadius = 16.dp
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (telemetry.connectionState == WearableConnectionState.CONNECTED)
                                                colors.accentFill
                                            else KumaColors.TextMuted
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = telemetry.connectionState.displayName.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (telemetry.connectionState == WearableConnectionState.CONNECTED)
                                            colors.accentText
                                        else KumaColors.TextMuted
                                    )
                                )
                            }

                            if (telemetry.currentBpm != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = KumaColors.ErrorRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${telemetry.currentBpm} BPM",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = KumaColors.TextWhite
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (telemetry.deviceName != null)
                                "Perangkat Aktif: ${telemetry.deviceName}"
                            else
                                "Belum ada wearable atau sensor terhubung.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = colors.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )

                        if (telemetry.currentZone != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${telemetry.currentZone!!.nameKey}: ${telemetry.currentZone!!.description}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = colors.accentText
                                )
                            )
                        }

                        if (telemetry.connectionState == WearableConnectionState.CONNECTED) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(KumaColors.SurfaceContainer)
                                    .clickable { wearableManager.disconnectWearable() }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Putuskan Koneksi",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = KumaColors.ErrorRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Google Health Connect Card
            item {
                KumaCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = KumaColors.SurfaceContainerLow,
                    cornerRadius = 16.dp
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(KumaColors.SurfaceContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HealthAndSafety,
                                    contentDescription = null,
                                    tint = colors.accentText,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Google Health Connect",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                )
                                Text(
                                    text = if (isHcAvailable) "Tersedia di perangkat (Siap sinkron)" else "Health Connect belum terpasang",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isHcAvailable) colors.accentText else KumaColors.TextMuted
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Menghubungkan KumaLift ke ekosistem jam tangan (Samsung Galaxy Watch, Pixel Watch, Garmin, Fitbit, Amazfit). KumaLift membaca denyut nadi & kalori aktif, serta menyetor log sesi latihan angkat beban.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = KumaColors.TextSecondary,
                                lineHeight = 18.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.accentFill)
                                .clickable { wearableManager.requestHealthConnectPermissions() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Kelola Izin Health Connect",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = colors.onAccentFill
                                )
                            )
                        }
                    }
                }
            }

            // Bluetooth LE Sensor Card
            item {
                KumaCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = KumaColors.SurfaceContainerLow,
                    cornerRadius = 16.dp
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(KumaColors.SurfaceContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bluetooth,
                                        contentDescription = null,
                                        tint = colors.accentText,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Sensor Dada / Armband BLE",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textPrimary
                                        )
                                    )
                                    Text(
                                        text = "Polar H10, Garmin HRM, Magene, CooSpo",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = KumaColors.TextMuted
                                        )
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    if (isScanning) wearableManager.stopBleScan()
                                    else wearableManager.startBleScan()
                                }
                            ) {
                                if (isScanning) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = colors.accentText,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Scan",
                                        tint = colors.accentText
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(KumaColors.SurfaceContainer)
                                .border(1.dp, KumaColors.BorderDefault, RoundedCornerShape(10.dp))
                                .clickable {
                                    if (isScanning) wearableManager.stopBleScan()
                                    else wearableManager.startBleScan()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isScanning) "Sedang Memindai..." else "Pindai Sensor Bluetooth",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                            )
                        }

                        if (discoveredDevices.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Perangkat Ditemukan:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = colors.accentText
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            discoveredDevices.forEach { dev ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(KumaColors.SurfaceContainer)
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = dev.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = colors.textPrimary
                                            )
                                        )
                                        Text(
                                            text = dev.address,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = KumaColors.TextMuted
                                            )
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(colors.accentFill)
                                            .clickable { wearableManager.connectBleDevice(dev.address, dev.name) }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "Hubungkan",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = colors.onAccentFill
                                            )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }

            // Health & Transparency Disclaimer
            item {
                KumaCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = KumaColors.SurfaceContainerLowest,
                    cornerRadius = 14.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Privasi & Standar Medis",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "KumaLift hanya membaca data detak jantung dan kalori dari sensor resmi terkalibrasi milik Anda untuk panduan intensitas angkat beban dan waktu rehat antar-set. KumaLift bukan perangkat medis diagnostik. Semua data biometrik tersimpan secara lokal di perangkat Anda tanpa diunggah ke pihak ketiga.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = KumaColors.TextMuted,
                                lineHeight = 16.sp
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}
