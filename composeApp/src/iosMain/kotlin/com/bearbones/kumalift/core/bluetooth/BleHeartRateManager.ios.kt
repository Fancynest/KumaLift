package com.bearbones.kumalift.core.bluetooth

import com.bearbones.kumalift.domain.model.BleHeartRateDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBCentralManagerDelegateProtocol
import platform.CoreBluetooth.CBPeripheral
import platform.CoreBluetooth.CBPeripheralDelegateProtocol
import platform.CoreBluetooth.CBService
import platform.CoreBluetooth.CBCharacteristic
import platform.CoreBluetooth.CBUUID
import platform.darwin.NSObject

actual object BleHeartRateManager {
    private val _isScanning = MutableStateFlow(false)
    actual val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BleHeartRateDevice>>(emptyList())
    actual val discoveredDevices: StateFlow<List<BleHeartRateDevice>> = _discoveredDevices.asStateFlow()

    private val _connectedDevice = MutableStateFlow<BleHeartRateDevice?>(null)
    actual val connectedDevice: StateFlow<BleHeartRateDevice?> = _connectedDevice.asStateFlow()

    private val _currentHeartRateBpm = MutableStateFlow<Int?>(null)
    actual val currentHeartRateBpm: StateFlow<Int?> = _currentHeartRateBpm.asStateFlow()

    actual fun startScan() {
        _isScanning.value = true
    }

    actual fun stopScan() {
        _isScanning.value = false
    }

    actual fun connect(device: BleHeartRateDevice) {
        _connectedDevice.value = device
        _isScanning.value = false
    }

    actual fun disconnect() {
        _connectedDevice.value = null
        _currentHeartRateBpm.value = null
    }
}
