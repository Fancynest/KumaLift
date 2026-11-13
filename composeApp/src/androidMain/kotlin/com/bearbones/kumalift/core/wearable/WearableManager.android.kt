package com.bearbones.kumalift.core.wearable

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.ParcelUuid
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZonedDateTime
import java.util.UUID

actual class WearableManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _telemetry = MutableStateFlow(LiveWearableTelemetry())
    actual val telemetry: StateFlow<LiveWearableTelemetry> = _telemetry.asStateFlow()

    private val _isScanningBle = MutableStateFlow(false)
    actual val isScanningBle: StateFlow<Boolean> = _isScanningBle.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredBleDevice>>(emptyList())
    actual val discoveredDevices: StateFlow<List<DiscoveredBleDevice>> = _discoveredDevices.asStateFlow()

    private val _isHealthConnectAuthorized = MutableStateFlow(false)
    actual val isHealthConnectAuthorized: StateFlow<Boolean> = _isHealthConnectAuthorized.asStateFlow()

    private var activeMonitoringJob: Job? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private val hrHistory = mutableListOf<Int>()

    // Standard Bluetooth SIG Heart Rate Service & Characteristic UUIDs
    private val HEART_RATE_SERVICE_UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
    private val HEART_RATE_MEASUREMENT_UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
    private val CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private val healthPermissions = setOf(
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class)
    )

    init {
        checkHealthConnectPermissions()
    }

    actual fun checkHealthConnectPermissions() {
        if (!isHealthConnectAvailable()) {
            _isHealthConnectAuthorized.value = false
            return
        }
        scope.launch {
            try {
                val client = HealthConnectClient.getOrCreate(context)
                val granted = client.permissionController.getGrantedPermissions()
                val hasAll = granted.containsAll(healthPermissions)
                _isHealthConnectAuthorized.value = hasAll
            } catch (e: Exception) {
                _isHealthConnectAuthorized.value = false
            }
        }
    }

    actual fun isHealthConnectAvailable(): Boolean {
        val status = HealthConnectClient.getSdkStatus(context)
        return status == HealthConnectClient.SDK_AVAILABLE
    }

    actual fun requestHealthConnectPermissions() {
        val status = HealthConnectClient.getSdkStatus(context)
        if (status == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=com.google.android.apps.healthdata&url=healthconnect%3A%2F%2Fonboarding")
                setPackage("com.android.vending")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
            } catch (_: Exception) {}
            return
        }

        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                Intent("android.health.connect.action.MANAGE_HEALTH_PERMISSIONS").apply {
                    putExtra(Intent.EXTRA_PACKAGE_NAME, context.packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallback = Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            } catch (_: Exception) {}
        }
    }

    @SuppressLint("MissingPermission")
    actual fun startBleScan() {
        if (!hasBluetoothPermissions()) {
            _telemetry.update { it.copy(connectionState = WearableConnectionState.PERMISSION_REQUIRED) }
            return
        }

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter
        if (adapter == null || !adapter.isEnabled) {
            _telemetry.update { it.copy(connectionState = WearableConnectionState.UNAVAILABLE) }
            return
        }

        _discoveredDevices.value = emptyList()
        _isScanningBle.value = true

        val scanner = adapter.bluetoothLeScanner ?: return
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(HEART_RATE_SERVICE_UUID))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(listOf(filter), settings, bleScanCallback)

        // Auto stop scan after 15 seconds
        scope.launch {
            delay(15000)
            stopBleScan()
        }
    }

    @SuppressLint("MissingPermission")
    actual fun stopBleScan() {
        if (!hasBluetoothPermissions()) return
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val scanner = bluetoothManager?.adapter?.bluetoothLeScanner
        try {
            scanner?.stopScan(bleScanCallback)
        } catch (_: Exception) {}
        _isScanningBle.value = false
    }

    private val bleScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { device ->
                val deviceName = device.name ?: "Sensor Detak Jantung (${device.address.takeLast(5)})"
                val discovered = DiscoveredBleDevice(
                    name = deviceName,
                    address = device.address,
                    rssi = result.rssi
                )
                _discoveredDevices.update { current ->
                    if (current.none { it.address == discovered.address }) {
                        current + discovered
                    } else {
                        current
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    actual fun connectBleDevice(address: String, name: String) {
        stopBleScan()
        disconnectWearable()

        if (!hasBluetoothPermissions()) {
            _telemetry.update { it.copy(connectionState = WearableConnectionState.PERMISSION_REQUIRED) }
            return
        }

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter ?: return
        val device: BluetoothDevice = try {
            adapter.getRemoteDevice(address)
        } catch (e: Exception) {
            return
        }

        _telemetry.update {
            it.copy(
                connectionState = WearableConnectionState.CONNECTING,
                deviceName = name,
                sourceType = WearableSourceType.BLUETOOTH_LE
            )
        }

        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    @SuppressLint("MissingPermission")
    actual fun disconnectWearable() {
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        } catch (_: Exception) {}
        bluetoothGatt = null

        _telemetry.update {
            it.copy(
                connectionState = WearableConnectionState.DISCONNECTED,
                currentBpm = null,
                currentZone = null,
                sourceType = WearableSourceType.NONE
            )
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _telemetry.update {
                    it.copy(
                        connectionState = WearableConnectionState.CONNECTED,
                        sourceType = WearableSourceType.BLUETOOTH_LE
                    )
                }
                gatt?.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _telemetry.update {
                    it.copy(
                        connectionState = WearableConnectionState.DISCONNECTED,
                        currentBpm = null,
                        currentZone = null
                    )
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && gatt != null) {
                val service = gatt.getService(HEART_RATE_SERVICE_UUID)
                val characteristic = service?.getCharacteristic(HEART_RATE_MEASUREMENT_UUID)
                if (characteristic != null) {
                    gatt.setCharacteristicNotification(characteristic, true)
                    val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
                    if (descriptor != null) {
                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(descriptor)
                    }
                }
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            if (characteristic?.uuid == HEART_RATE_MEASUREMENT_UUID) {
                val value = characteristic.value ?: return
                if (value.isNotEmpty()) {
                    val flag = value[0].toInt()
                    val is16Bit = (flag and 0x01) != 0
                    val bpm = if (is16Bit && value.size >= 3) {
                        (value[1].toInt() and 0xFF) or ((value[2].toInt() and 0xFF) shl 8)
                    } else if (value.size >= 2) {
                        value[1].toInt() and 0xFF
                    } else 0

                    if (bpm in 30..240) {
                        onNewHeartRateSample(bpm)
                    }
                }
            }
        }
    }

    private fun onNewHeartRateSample(bpm: Int) {
        synchronized(hrHistory) {
            hrHistory.add(bpm)
        }
        val peak = hrHistory.maxOrNull() ?: bpm
        val avg = (hrHistory.average()).toInt()
        val zone = HeartRateZone.calculateZone(bpm)
        val isReady = bpm <= 115

        _telemetry.update {
            it.copy(
                connectionState = WearableConnectionState.CONNECTED,
                currentBpm = bpm,
                currentZone = zone,
                peakBpm = peak,
                avgBpm = avg,
                lastUpdateEpochMs = System.currentTimeMillis(),
                isHeartRateRestingReady = isReady
            )
        }
    }

    actual fun startActiveWorkoutMonitoring(workoutId: String) {
        activeMonitoringJob?.cancel()
        hrHistory.clear()

        activeMonitoringJob = scope.launch {
            val hasHealthConnect = isHealthConnectAvailable()

            while (isActive) {
                if (_telemetry.value.sourceType != WearableSourceType.BLUETOOTH_LE && hasHealthConnect) {
                    // Poll recent heart rate & calories from Health Connect every 4 seconds
                    try {
                        val client = HealthConnectClient.getOrCreate(context)
                        val now = Instant.now()
                        val startTime = now.minusSeconds(60)

                        val hrRequest = ReadRecordsRequest(
                            recordType = HeartRateRecord::class,
                            timeRangeFilter = TimeRangeFilter.between(startTime, now)
                        )
                        val hrResponse = client.readRecords(hrRequest)
                        val latestSample = hrResponse.records.lastOrNull()?.samples?.lastOrNull()

                        val calRequest = ReadRecordsRequest(
                            recordType = TotalCaloriesBurnedRecord::class,
                            timeRangeFilter = TimeRangeFilter.between(startTime.minusSeconds(3600), now)
                        )
                        val calResponse = client.readRecords(calRequest)
                        val totalKcal = calResponse.records.sumOf { it.energy.inKilocalories }

                        if (latestSample != null) {
                            val bpm = latestSample.beatsPerMinute.toInt()
                            if (bpm in 30..240) {
                                onNewHeartRateSample(bpm)
                                _telemetry.update {
                                    it.copy(
                                        sourceType = WearableSourceType.HEALTH_CONNECT,
                                        deviceName = "Health Connect",
                                        activeCaloriesKcal = if (totalKcal > 0) totalKcal else it.activeCaloriesKcal
                                    )
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
                delay(4000)
            }
        }
    }

    actual fun stopActiveWorkoutMonitoring() {
        activeMonitoringJob?.cancel()
        activeMonitoringJob = null
    }

    actual fun writeFinishedWorkout(
        workoutTitle: String,
        startTimeEpochMs: Long,
        endTimeEpochMs: Long,
        totalVolumeKg: Double,
        activeCaloriesKcal: Double?
    ) {
        if (!isHealthConnectAvailable()) return

        scope.launch {
            try {
                val client = HealthConnectClient.getOrCreate(context)
                val startInstant = Instant.ofEpochMilli(if (startTimeEpochMs > 0) startTimeEpochMs else System.currentTimeMillis() - 3600_000L)
                val endInstant = Instant.ofEpochMilli(if (endTimeEpochMs > 0) endTimeEpochMs else System.currentTimeMillis())

                val exerciseRecord = ExerciseSessionRecord(
                    startTime = startInstant,
                    startZoneOffset = ZonedDateTime.now().offset,
                    endTime = endInstant,
                    endZoneOffset = ZonedDateTime.now().offset,
                    exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
                    title = "KumaLift: $workoutTitle",
                    notes = "Total volume latihan angkat beban: ${totalVolumeKg.toInt()} kg"
                )

                val recordsToInsert = mutableListOf<androidx.health.connect.client.records.Record>(exerciseRecord)

                if (activeCaloriesKcal != null && activeCaloriesKcal > 0) {
                    val calRecord = TotalCaloriesBurnedRecord(
                        startTime = startInstant,
                        startZoneOffset = ZonedDateTime.now().offset,
                        endTime = endInstant,
                        endZoneOffset = ZonedDateTime.now().offset,
                        energy = Energy.kilocalories(activeCaloriesKcal)
                    )
                    recordsToInsert.add(calRecord)
                }

                client.insertRecords(recordsToInsert)
            } catch (_: Exception) {}
        }
    }

    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
        }
    }
}

@Composable
actual fun rememberWearableManager(): WearableManager {
    val context = LocalContext.current
    return remember(context) { WearableManager(context.applicationContext) }
}

@Composable
actual fun rememberHealthConnectPermissionLauncher(
    onResult: (Boolean) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val permissions = setOf(
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class)
    )

    val launcher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        val allGranted = granted.containsAll(permissions)
        onResult(allGranted)
    }

    return {
        coroutineScope.launch {
            try {
                val status = HealthConnectClient.getSdkStatus(context)
                if (status == HealthConnectClient.SDK_AVAILABLE) {
                    val client = HealthConnectClient.getOrCreate(context)
                    val granted = client.permissionController.getGrantedPermissions()
                    if (!granted.containsAll(permissions)) {
                        launcher.launch(permissions)
                    } else {
                        // All granted, open Health Connect app permissions page
                        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            Intent("android.health.connect.action.MANAGE_HEALTH_PERMISSIONS").apply {
                                putExtra(Intent.EXTRA_PACKAGE_NAME, context.packageName)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        } else {
                            Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        }
                        context.startActivity(intent)
                        onResult(true)
                    }
                } else if (status == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("market://details?id=com.google.android.apps.healthdata&url=healthconnect%3A%2F%2Fonboarding")
                        setPackage("com.android.vending")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            } catch (e: Exception) {
                try {
                    val intent = Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {}
            }
        }
    }
}
