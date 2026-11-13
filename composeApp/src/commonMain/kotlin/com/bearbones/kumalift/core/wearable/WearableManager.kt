package com.bearbones.kumalift.core.wearable

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.StateFlow

expect class WearableManager {
    val telemetry: StateFlow<LiveWearableTelemetry>
    val isScanningBle: StateFlow<Boolean>
    val discoveredDevices: StateFlow<List<DiscoveredBleDevice>>
    val isHealthConnectAuthorized: StateFlow<Boolean>

    fun isHealthConnectAvailable(): Boolean
    fun checkHealthConnectPermissions()
    fun requestHealthConnectPermissions()
    fun startBleScan()
    fun stopBleScan()
    fun connectBleDevice(address: String, name: String)
    fun disconnectWearable()
    fun startActiveWorkoutMonitoring(workoutId: String)
    fun stopActiveWorkoutMonitoring()
    fun writeFinishedWorkout(
        workoutTitle: String,
        startTimeEpochMs: Long,
        endTimeEpochMs: Long,
        totalVolumeKg: Double,
        activeCaloriesKcal: Double?
    )
}

@Composable
expect fun rememberWearableManager(): WearableManager

@Composable
expect fun rememberHealthConnectPermissionLauncher(
    onResult: (Boolean) -> Unit = {}
): () -> Unit
