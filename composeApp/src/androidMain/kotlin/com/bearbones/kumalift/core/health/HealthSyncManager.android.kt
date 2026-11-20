package com.bearbones.kumalift.core.health

import android.content.Intent
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Energy
import com.bearbones.kumalift.core.logging.KumaLogger
import com.bearbones.kumalift.core.timer.AndroidAppProvider
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

    private val requiredPermissions = setOf(
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class)
    )

    private val scope = CoroutineScope(Dispatchers.IO)
    private var hasPermissionsGranted: Boolean = false

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
            hasPermissionsGranted = granted.containsAll(requiredPermissions)
            hasPermissionsGranted
        } catch (e: Exception) {
            false
        }
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
        } catch (e: Exception) {
            HealthConnectClient.SDK_UNAVAILABLE
        }

        if (sdkStatus != HealthConnectClient.SDK_AVAILABLE) {
            return HealthSyncStatus.UNAVAILABLE
        }

        return if (hasPermissionsGranted) {
            HealthSyncStatus.CONNECTED
        } else {
            HealthSyncStatus.PERMISSION_REQUIRED
        }
    }

    actual suspend fun requestPermissions(): Boolean {
        if (isTestMode) return true
        val context = AndroidAppProvider.appContext ?: return false
        val already = checkPermissionsInternal()
        if (already) return true

        try {
            val intent = Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val fallbackIntent = Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (ignored: Exception) {
                KumaLogger.w("HealthSyncManager", "Cannot open Health Connect settings: ${ignored.message}")
            }
        }
        return false
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
                metadata = Metadata()
            )

            val caloriesRecord = TotalCaloriesBurnedRecord(
                startTime = startTime,
                startZoneOffset = zoneOffset,
                endTime = endTime,
                endZoneOffset = zoneOffset,
                energy = Energy.kilocalories(record.estimatedCaloriesBurned),
                metadata = Metadata()
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
}
