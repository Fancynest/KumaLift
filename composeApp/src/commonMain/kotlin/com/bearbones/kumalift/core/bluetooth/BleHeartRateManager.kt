package com.bearbones.kumalift.core.bluetooth

import com.bearbones.kumalift.domain.model.BleHeartRateDevice
import kotlinx.coroutines.flow.StateFlow

expect object BleHeartRateManager {
    val isScanning: StateFlow<Boolean>
    val discoveredDevices: StateFlow<List<BleHeartRateDevice>>
    val connectedDevice: StateFlow<BleHeartRateDevice?>
    val currentHeartRateBpm: StateFlow<Int?>

    fun startScan()
    fun stopScan()
    fun connect(device: BleHeartRateDevice)
    fun disconnect()
}
