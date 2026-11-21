package com.bearbones.kumalift.core.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import androidx.compose.runtime.Composable
import com.bearbones.kumalift.core.logging.KumaLogger
import com.bearbones.kumalift.core.timer.AndroidAppProvider
import com.bearbones.kumalift.domain.model.BleHeartRateDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

actual object BleHeartRateManager {
    private val HEART_RATE_SERVICE_UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
    private val HEART_RATE_MEASUREMENT_UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
    private val CCC_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private val _isScanning = MutableStateFlow(false)
    actual val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BleHeartRateDevice>>(emptyList())
    actual val discoveredDevices: StateFlow<List<BleHeartRateDevice>> = _discoveredDevices.asStateFlow()

    private val _connectedDevice = MutableStateFlow<BleHeartRateDevice?>(null)
    actual val connectedDevice: StateFlow<BleHeartRateDevice?> = _connectedDevice.asStateFlow()

    private val _currentHeartRateBpm = MutableStateFlow<Int?>(null)
    actual val currentHeartRateBpm: StateFlow<Int?> = _currentHeartRateBpm.asStateFlow()

    actual val isBluetoothEnabled: Boolean
        get() {
            if (isTestMode || AndroidAppProvider.appContext == null) return true
            return getBluetoothAdapter()?.isEnabled == true
        }

    private var activeGatt: BluetoothGatt? = null
    var isTestMode: Boolean = false

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            val device = result?.device ?: return
            val name = try { device.name } catch (_: SecurityException) { null } ?: return
            val bleDevice = BleHeartRateDevice(
                id = device.address,
                name = name,
                rssi = result.rssi
            )
            val current = _discoveredDevices.value
            if (current.none { it.id == bleDevice.id }) {
                _discoveredDevices.value = current + bleDevice
            }
        }

        override fun onScanFailed(errorCode: Int) {
            KumaLogger.w("BleHeartRateManager", "BLE scan failed: $errorCode")
            _isScanning.value = false
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                KumaLogger.i("BleHeartRateManager", "Connected to GATT server, discovering services")
                try {
                    gatt?.discoverServices()
                } catch (e: SecurityException) {
                    KumaLogger.e("BleHeartRateManager", "Missing permission for discoverServices: ${e.message}")
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                KumaLogger.i("BleHeartRateManager", "Disconnected from GATT server")
                _connectedDevice.value = null
                _currentHeartRateBpm.value = null
                try {
                    activeGatt?.close()
                } catch (_: Exception) {}
                activeGatt = null
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && gatt != null) {
                val service = gatt.getService(HEART_RATE_SERVICE_UUID)
                val characteristic = service?.getCharacteristic(HEART_RATE_MEASUREMENT_UUID)
                if (characteristic != null) {
                    try {
                        gatt.setCharacteristicNotification(characteristic, true)
                        val descriptor = characteristic.getDescriptor(CCC_DESCRIPTOR_UUID)
                        if (descriptor != null) {
                            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            gatt.writeDescriptor(descriptor)
                        }
                    } catch (e: SecurityException) {
                        KumaLogger.e("BleHeartRateManager", "Failed to enable notifications: ${e.message}")
                    }
                }
            }
        }

        @Deprecated("Deprecated for API 33, handled for backwards compat")
        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            val data = characteristic?.value ?: return
            parseHeartRate(data)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            parseHeartRate(value)
        }
    }

    private fun parseHeartRate(data: ByteArray) {
        if (data.isEmpty()) return
        val flag = data[0].toInt()
        val format = flag and 0x01
        val bpm = if (format == 0) {
            if (data.size > 1) data[1].toInt() and 0xFF else return
        } else {
            if (data.size > 2) (data[1].toInt() and 0xFF) or ((data[2].toInt() and 0xFF) shl 8) else return
        }
        if (bpm in 30..250) {
            _currentHeartRateBpm.value = bpm
        }
    }

    private fun getBluetoothAdapter(): BluetoothAdapter? {
        val context = AndroidAppProvider.appContext ?: return null
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return manager?.adapter
    }

    actual fun startScan() {
        if (isTestMode || AndroidAppProvider.appContext == null) {
            _isScanning.value = true
            _discoveredDevices.value = listOf(
                BleHeartRateDevice("TEST:01", "Mi Smart Band 7", -65),
                BleHeartRateDevice("TEST:02", "Polar H10", -55)
            )
            return
        }

        val adapter = getBluetoothAdapter() ?: return
        if (!adapter.isEnabled) return

        try {
            _discoveredDevices.value = emptyList()
            _isScanning.value = true
            adapter.bluetoothLeScanner?.startScan(scanCallback)
        } catch (e: SecurityException) {
            KumaLogger.e("BleHeartRateManager", "Missing BLUETOOTH_SCAN permission: ${e.message}")
            _isScanning.value = false
        }
    }

    actual fun stopScan() {
        if (isTestMode || AndroidAppProvider.appContext == null) {
            _isScanning.value = false
            return
        }
        val adapter = getBluetoothAdapter() ?: return
        try {
            adapter.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: SecurityException) {
            KumaLogger.w("BleHeartRateManager", "Error stopping scan: ${e.message}")
        }
        _isScanning.value = false
    }

    actual fun connect(device: BleHeartRateDevice) {
        stopScan()
        if (isTestMode || AndroidAppProvider.appContext == null) {
            _connectedDevice.value = device
            _currentHeartRateBpm.value = 135
            return
        }

        val context = AndroidAppProvider.appContext ?: return
        val adapter = getBluetoothAdapter() ?: return
        try {
            val remote = adapter.getRemoteDevice(device.id)
            activeGatt = remote.connectGatt(context, false, gattCallback)
            _connectedDevice.value = device
        } catch (e: SecurityException) {
            KumaLogger.e("BleHeartRateManager", "Missing BLUETOOTH_CONNECT permission: ${e.message}")
        } catch (e: Exception) {
            KumaLogger.e("BleHeartRateManager", "Error connecting to BLE: ${e.message}")
        }
    }

    actual fun disconnect() {
        if (isTestMode || AndroidAppProvider.appContext == null) {
            _connectedDevice.value = null
            _currentHeartRateBpm.value = null
            return
        }
        try {
            activeGatt?.disconnect()
            activeGatt?.close()
        } catch (_: Exception) {}
        activeGatt = null
        _connectedDevice.value = null
        _currentHeartRateBpm.value = null
    }
}

@Composable
actual fun rememberBluetoothPermissionLauncher(
    onResult: (Boolean) -> Unit
): () -> Unit {
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        onResult(allGranted)
    }

    return {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            launcher.launch(
                arrayOf(
                    android.Manifest.permission.BLUETOOTH_SCAN,
                    android.Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        } else {
            launcher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }
}
