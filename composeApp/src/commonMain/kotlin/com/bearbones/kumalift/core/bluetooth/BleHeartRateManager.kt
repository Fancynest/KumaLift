package com.bearbones.kumalift.core.bluetooth

import androidx.compose.runtime.Composable
import com.bearbones.kumalift.domain.model.BleHeartRateDevice
import kotlinx.coroutines.flow.StateFlow

expect object BleHeartRateManager {
    val isScanning: StateFlow<Boolean>
    val discoveredDevices: StateFlow<List<BleHeartRateDevice>>
    val connectedDevice: StateFlow<BleHeartRateDevice?>
    val currentHeartRateBpm: StateFlow<Int?>
    val isBluetoothEnabled: Boolean

    fun startScan()
    fun stopScan()
    fun connect(device: BleHeartRateDevice)
    fun disconnect()
}

@Composable
expect fun rememberBluetoothPermissionLauncher(onResult: (Boolean) -> Unit): () -> Unit
