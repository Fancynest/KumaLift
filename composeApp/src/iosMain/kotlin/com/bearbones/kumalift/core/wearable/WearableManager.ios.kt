package com.bearbones.kumalift.core.wearable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual class WearableManager {
    private val _telemetry = MutableStateFlow(LiveWearableTelemetry())
    actual val telemetry: StateFlow<LiveWearableTelemetry> = _telemetry.asStateFlow()

    private val _isScanningBle = MutableStateFlow(false)
    actual val isScanningBle: StateFlow<Boolean> = _isScanningBle.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredBleDevice>>(emptyList())
    actual val discoveredDevices: StateFlow<List<DiscoveredBleDevice>> = _discoveredDevices.asStateFlow()

    private val _isHealthConnectAuthorized = MutableStateFlow(false)
    actual val isHealthConnectAuthorized: StateFlow<Boolean> = _isHealthConnectAuthorized.asStateFlow()

    actual fun isHealthConnectAvailable(): Boolean = false
    actual fun checkHealthConnectPermissions() {}
    actual fun requestHealthConnectPermissions() {}
    actual fun startBleScan() {}
    actual fun stopBleScan() {}
    actual fun connectBleDevice(address: String, name: String) {}
    actual fun disconnectWearable() {}
    actual fun startActiveWorkoutMonitoring(workoutId: String) {}
    actual fun stopActiveWorkoutMonitoring() {}
    actual fun writeFinishedWorkout(
        workoutTitle: String,
        startTimeEpochMs: Long,
        endTimeEpochMs: Long,
        totalVolumeKg: Double,
        activeCaloriesKcal: Double?
    ) {}
}

@Composable
actual fun rememberWearableManager(): WearableManager {
    return remember { WearableManager() }
}

@Composable
actual fun rememberHealthConnectPermissionLauncher(
    onResult: (Boolean) -> Unit
): () -> Unit {
    return {
        onResult(false)
    }
}
