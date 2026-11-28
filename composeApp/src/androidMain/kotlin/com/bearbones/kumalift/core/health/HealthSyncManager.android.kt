package com.bearbones.kumalift.core.health

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import com.bearbones.kumalift.core.logging.KumaLogger
import com.bearbones.kumalift.core.timer.AndroidAppProvider
import com.bearbones.kumalift.domain.model.HealthBodyMeasurementSample
import com.bearbones.kumalift.domain.model.HealthBodyMetricsSyncPayload
import com.bearbones.kumalift.domain.model.HealthBodySyncCoordinator
import com.bearbones.kumalift.domain.model.HealthPlatformType
import com.bearbones.kumalift.domain.model.HealthSyncResult
import com.bearbones.kumalift.domain.model.HealthSyncStatus
import com.bearbones.kumalift.domain.model.WorkoutHealthRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset

actual object HealthSyncManager {
    actual val platformType: HealthPlatformType = HealthPlatformType.HEALTH_CONNECT

    internal val requiredPermissions = setOf(
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class)
    )
    private val weightReadPerm = HealthPermission.getReadPermission(WeightRecord::class)
    private val weightWritePerm = HealthPermission.getWritePermission(WeightRecord::class)
    private val heightReadPerm = HealthPermission.getReadPermission(HeightRecord::class)
    private val heightWritePerm = HealthPermission.getWritePermission(HeightRecord::class)

    internal val bodyMetricPermissions = setOf(
        weightReadPerm,
        weightWritePerm,
        heightReadPerm,
        heightWritePerm
    )
    internal val allPermissions = requiredPermissions + bodyMetricPermissions

    private val scope = CoroutineScope(Dispatchers.IO)
    @Volatile
    private var hasPermissionsGranted: Boolean = false
    @Volatile
    private var hasWeightRead: Boolean = false
    @Volatile
    private var hasHeightRead: Boolean = false
    @Volatile
    private var hasWeightWrite: Boolean = false
    @Volatile
    private var hasHeightWrite: Boolean = false

    var isTestMode: Boolean = false
    var mockSuccess: Boolean = true

    init {
        scope.launch {
            checkPermissionsInternal()
        }
    }

    private suspend fun checkPermissionsInternal(): Boolean {
        val client = getClient() ?: return false
        return try {
            val granted = client.permissionController.getGrantedPermissions()
            updateGrantedFlags(granted)
            hasPermissionsGranted || hasWeightRead || hasHeightRead || hasWeightWrite || hasHeightWrite
        } catch (_: Exception) {
            false
        }
    }

    internal fun updateGrantedFlags(granted: Set<String>) {
        hasPermissionsGranted = granted.containsAll(requiredPermissions)
        hasWeightRead = weightReadPerm in granted
        hasHeightRead = heightReadPerm in granted
        hasWeightWrite = weightWritePerm in granted
        hasHeightWrite = heightWritePerm in granted
    }

    private fun getClient(): HealthConnectClient? {
        val context = AndroidAppProvider.appContext ?: return null
        return try {
            val status = HealthConnectClient.getSdkStatus(context)
            if (status == HealthConnectClient.SDK_AVAILABLE) {
                HealthConnectClient.getOrCreate(context)
            } else {
                null
            }
        } catch (e: Exception) {
            KumaLogger.w("HealthSyncManager", "Health Connect unavailable: ${e.message}")
            null
        }
    }

    actual fun getStatus(): HealthSyncStatus {
        if (isTestMode) return HealthSyncStatus.CONNECTED
        val context = AndroidAppProvider.appContext ?: return HealthSyncStatus.UNAVAILABLE
        val sdkStatus = try {
            HealthConnectClient.getSdkStatus(context)
        } catch (_: Exception) {
            HealthConnectClient.SDK_UNAVAILABLE
        }

        if (sdkStatus != HealthConnectClient.SDK_AVAILABLE) {
            return HealthSyncStatus.UNAVAILABLE
        }

        return if (hasPermissionsGranted || hasWeightRead || hasHeightRead || hasWeightWrite || hasHeightWrite) {
            HealthSyncStatus.CONNECTED
        } else {
            HealthSyncStatus.PERMISSION_REQUIRED
        }
    }

    actual fun hasWorkoutSyncPermission(): Boolean = isTestMode || hasPermissionsGranted
    actual fun hasWeightReadPermission(): Boolean = isTestMode || hasWeightRead
    actual fun hasHeightReadPermission(): Boolean = isTestMode || hasHeightRead
    actual fun hasWeightWritePermission(): Boolean = isTestMode || hasWeightWrite
    actual fun hasHeightWritePermission(): Boolean = isTestMode || hasHeightWrite

    actual suspend fun refreshStatus(): HealthSyncStatus {
        if (!isTestMode) {
            checkPermissionsInternal()
        }
        return getStatus()
    }

    actual suspend fun requestPermissions(): Boolean {
        if (isTestMode) return true
        val already = checkPermissionsInternal()
        if (already) return true
        openHealthSettings()
        return false
    }

    actual fun openHealthSettings(): Boolean {
        val context = AndroidAppProvider.appContext ?: return false
        return try {
            val intent = Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            try {
                val fallbackIntent = Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                true
            } catch (ignored: Exception) {
                KumaLogger.w("HealthSyncManager", "Cannot open Health Connect settings: ${ignored.message}")
                false
            }
        }
    }

    actual suspend fun syncWorkout(record: WorkoutHealthRecord): HealthSyncResult {
        if (isTestMode || AndroidAppProvider.appContext == null) {
            return HealthSyncResult(
                isSuccess = mockSuccess,
                syncDetails = if (mockSuccess) "Synced ${record.title} to Health Connect" else "Failed to sync"
            )
        }

        val client = getClient()
            ?: return HealthSyncResult(isSuccess = false, syncDetails = "Health Connect not available on this device")

        return try {
            val granted = client.permissionController.getGrantedPermissions()
            updateGrantedFlags(granted)
            if (!hasPermissionsGranted) {
                return HealthSyncResult(
                    isSuccess = false,
                    syncDetails = "Workout permissions required in Health Connect"
                )
            }
            val startTime = Instant.ofEpochMilli(record.startTimeEpochMs)
            val endTime = Instant.ofEpochMilli(record.endTimeEpochMs)
            val zoneOffset = ZoneOffset.systemDefault().rules.getOffset(startTime)

            val exerciseRecord = ExerciseSessionRecord(
                startTime = startTime,
                startZoneOffset = zoneOffset,
                endTime = endTime,
                endZoneOffset = zoneOffset,
                exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
                title = record.title,
                notes = "KumaLift Workout: ${record.totalSetsCompleted} sets, ${record.totalTonnageKg.toInt()} kg total volume",
                metadata = Metadata(recordingMethod = Metadata.RECORDING_METHOD_ACTIVELY_RECORDED)
            )

            val caloriesRecord = TotalCaloriesBurnedRecord(
                startTime = startTime,
                startZoneOffset = zoneOffset,
                endTime = endTime,
                endZoneOffset = zoneOffset,
                energy = Energy.kilocalories(record.estimatedCaloriesBurned),
                metadata = Metadata(recordingMethod = Metadata.RECORDING_METHOD_ACTIVELY_RECORDED)
            )

            client.insertRecords(listOf(exerciseRecord, caloriesRecord))
            KumaLogger.i("HealthSyncManager", "Synced ${record.title} to Health Connect successfully")
            HealthSyncResult(
                isSuccess = true,
                syncDetails = "Synced ${record.title} to Health Connect"
            )
        } catch (e: Exception) {
            KumaLogger.e("HealthSyncManager", "Failed to sync to Health Connect: ${e.message}")
            HealthSyncResult(
                isSuccess = false,
                syncDetails = e.message ?: "Failed to sync workout"
            )
        }
    }

    actual suspend fun readLatestBodyMetrics(): HealthBodyMetricsSyncPayload {
        if (isTestMode) {
            return HealthBodyMetricsSyncPayload(
                status = HealthSyncStatus.CONNECTED,
                hasWeightReadPermission = true,
                hasHeightReadPermission = true
            )
        }
        val context = AndroidAppProvider.appContext
            ?: return HealthBodyMetricsSyncPayload(status = HealthSyncStatus.UNAVAILABLE)
        val sdkStatus = try {
            HealthConnectClient.getSdkStatus(context)
        } catch (_: Exception) {
            HealthConnectClient.SDK_UNAVAILABLE
        }
        if (sdkStatus != HealthConnectClient.SDK_AVAILABLE) {
            return HealthBodyMetricsSyncPayload(status = HealthSyncStatus.UNAVAILABLE)
        }
        val client = getClient()
            ?: return HealthBodyMetricsSyncPayload(status = HealthSyncStatus.UNAVAILABLE)

        return try {
            val granted = client.permissionController.getGrantedPermissions()
            updateGrantedFlags(granted)

            val ownPkg = context.packageName
            val weightSample = if (hasWeightRead) {
                val response = client.readRecords(
                    ReadRecordsRequest(
                        recordType = WeightRecord::class,
                        timeRangeFilter = TimeRangeFilter.after(Instant.EPOCH),
                        ascendingOrder = false,
                        pageSize = 50
                    )
                )
                HealthBodySyncCoordinator.pickLatestValidSample(
                    samples = response.records.map { rec ->
                        HealthBodyMeasurementSample(
                            recordId = rec.metadata.id,
                            value = rec.weight.inKilograms,
                            recordedAtEpochMs = rec.time.toEpochMilli(),
                            sourceApp = rec.metadata.dataOrigin.packageName
                        )
                    },
                    minValid = 20.0,
                    maxValid = 400.0,
                    ownPackageName = ownPkg
                )
            } else {
                null
            }

            val heightSample = if (hasHeightRead) {
                val response = client.readRecords(
                    ReadRecordsRequest(
                        recordType = HeightRecord::class,
                        timeRangeFilter = TimeRangeFilter.after(Instant.EPOCH),
                        ascendingOrder = false,
                        pageSize = 50
                    )
                )
                HealthBodySyncCoordinator.pickLatestValidSample(
                    samples = response.records.map { rec ->
                        HealthBodyMeasurementSample(
                            recordId = rec.metadata.id,
                            value = rec.height.inMeters * 100.0,
                            recordedAtEpochMs = rec.time.toEpochMilli(),
                            sourceApp = rec.metadata.dataOrigin.packageName
                        )
                    },
                    minValid = 50.0,
                    maxValid = 280.0,
                    ownPackageName = ownPkg
                )
            } else {
                null
            }

            HealthBodyMetricsSyncPayload(
                status = getStatus(),
                weightSample = weightSample,
                heightSample = heightSample,
                hasWeightReadPermission = hasWeightRead,
                hasHeightReadPermission = hasHeightRead
            )
        } catch (se: SecurityException) {
            KumaLogger.w("HealthSyncManager", "Body metrics read permission denied: ${se.message}")
            HealthBodyMetricsSyncPayload(
                status = HealthSyncStatus.PERMISSION_REQUIRED,
                hasWeightReadPermission = hasWeightRead,
                hasHeightReadPermission = hasHeightRead,
                errorMessage = null
            )
        } catch (e: Exception) {
            KumaLogger.e("HealthSyncManager", "Failed reading body metrics: ${e.message}")
            HealthBodyMetricsSyncPayload(
                status = getStatus(),
                hasWeightReadPermission = hasWeightRead,
                hasHeightReadPermission = hasHeightRead,
                errorMessage = null
            )
        }
    }

    actual suspend fun writeBodyMetrics(
        bodyweightKg: Double?,
        heightCm: Double?,
        weightClientRecordId: String?,
        heightClientRecordId: String?
    ): HealthSyncResult {
        if (isTestMode || AndroidAppProvider.appContext == null) {
            return HealthSyncResult(isSuccess = mockSuccess)
        }
        val client = getClient()
            ?: return HealthSyncResult(isSuccess = false, syncDetails = "Health Connect unavailable")

        return try {
            val granted = client.permissionController.getGrantedPermissions()
            updateGrantedFlags(granted)
            val now = Instant.now()
            val zoneOffset = ZoneOffset.systemDefault().rules.getOffset(now)
            val recordsToInsert = buildList {
                if (hasWeightWrite && bodyweightKg != null && bodyweightKg.isFinite() && bodyweightKg in 20.0..400.0) {
                    add(
                        WeightRecord(
                            time = now,
                            zoneOffset = zoneOffset,
                            weight = Mass.kilograms(bodyweightKg),
                            metadata = Metadata(
                                clientRecordId = weightClientRecordId ?: "kumalift_weight_${now.toEpochMilli()}",
                                recordingMethod = Metadata.RECORDING_METHOD_MANUAL_ENTRY
                            )
                        )
                    )
                }
                if (hasHeightWrite && heightCm != null && heightCm.isFinite() && heightCm in 50.0..280.0) {
                    add(
                        HeightRecord(
                            time = now,
                            zoneOffset = zoneOffset,
                            height = Length.meters(heightCm / 100.0),
                            metadata = Metadata(
                                clientRecordId = heightClientRecordId ?: "kumalift_height_${now.toEpochMilli()}",
                                recordingMethod = Metadata.RECORDING_METHOD_MANUAL_ENTRY
                            )
                        )
                    )
                }
            }
            if (recordsToInsert.isEmpty()) {
                return HealthSyncResult(isSuccess = false, syncDetails = "No permitted body metrics to write")
            }
            client.insertRecords(recordsToInsert)
            HealthSyncResult(isSuccess = true)
        } catch (e: Exception) {
            KumaLogger.w("HealthSyncManager", "Failed writing body metrics: ${e.message}")
            HealthSyncResult(isSuccess = false, syncDetails = e.message ?: "Write failed")
        }
    }
}

@Composable
actual fun rememberHealthPermissionLauncher(
    includeBodyMetrics: Boolean,
    onResult: (HealthSyncStatus) -> Unit
): () -> Unit {
    val scope = rememberCoroutineScope()
    val latestOnResult by rememberUpdatedState(onResult)

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        HealthSyncManager.updateGrantedFlags(granted)
        scope.launch {
            val refreshed = HealthSyncManager.refreshStatus()
            latestOnResult(refreshed)
        }
    }

    return remember(includeBodyMetrics) {
        {
            if (HealthSyncManager.isTestMode) {
                latestOnResult(HealthSyncManager.getStatus())
            } else {
                val targetPermissions = if (includeBodyMetrics) {
                    HealthSyncManager.allPermissions
                } else {
                    HealthSyncManager.requiredPermissions
                }
                try {
                    permissionLauncher.launch(targetPermissions)
                } catch (e: Exception) {
                    KumaLogger.w("HealthSyncManager", "Permission sheet fallback to settings: ${e.message}")
                    HealthSyncManager.openHealthSettings()
                }
            }
        }
    }
}
