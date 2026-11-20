package com.bearbones.kumalift.core.health

import com.bearbones.kumalift.core.logging.KumaLogger
import com.bearbones.kumalift.domain.model.HealthPlatformType
import com.bearbones.kumalift.domain.model.HealthSyncResult
import com.bearbones.kumalift.domain.model.HealthSyncStatus
import com.bearbones.kumalift.domain.model.WorkoutHealthRecord
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.HealthKit.HKHealthStore
import platform.HealthKit.HKObjectType
import platform.HealthKit.HKQuantity
import platform.HealthKit.HKQuantityTypeIdentifierActiveEnergyBurned
import platform.HealthKit.HKUnit
import platform.HealthKit.HKWorkout
import platform.HealthKit.HKWorkoutActivityTypeTraditionalStrengthTraining
import platform.HealthKit.HKWorkoutType
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCancellableCoroutine

actual object HealthSyncManager {
    actual val platformType: HealthPlatformType = HealthPlatformType.HEALTH_KIT

    private var hasPermissionsGranted: Boolean = false

    actual fun getStatus(): HealthSyncStatus {
        return if (!HKHealthStore.isHealthDataAvailable()) {
            HealthSyncStatus.UNAVAILABLE
        } else if (hasPermissionsGranted) {
            HealthSyncStatus.CONNECTED
        } else {
            HealthSyncStatus.PERMISSION_REQUIRED
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun requestPermissions(): Boolean {
        if (!HKHealthStore.isHealthDataAvailable()) return false
        val store = HKHealthStore()
        val typesToShare = setOfNotNull(
            HKWorkoutType.workoutType(),
            HKObjectType.quantityTypeForIdentifier(HKQuantityTypeIdentifierActiveEnergyBurned)
        )
        return suspendCancellableCoroutine { cont ->
            store.requestAuthorizationToShareTypes(
                typesToShare = typesToShare,
                readTypes = emptySet<Any?>()
            ) { success, error ->
                if (error != null) {
                    KumaLogger.e("HealthSyncManager", "HealthKit authorization failed: ${error.localizedDescription}")
                }
                hasPermissionsGranted = success
                cont.resume(success)
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun syncWorkout(record: WorkoutHealthRecord): HealthSyncResult {
        if (!HKHealthStore.isHealthDataAvailable()) {
            return HealthSyncResult(isSuccess = false, syncDetails = "HealthKit not available on this device")
        }
        return suspendCancellableCoroutine { cont ->
            try {
                val store = HKHealthStore()
                val startDate = NSDate.dateWithTimeIntervalSince1970(record.startTimeEpochMs / 1000.0)
                val endDate = NSDate.dateWithTimeIntervalSince1970(record.endTimeEpochMs / 1000.0)
                val duration = record.activeDurationSeconds.toDouble()
                val energy = HKQuantity.quantityWithUnit(
                    unit = HKUnit.kilocalorieUnit(),
                    doubleValue = record.estimatedCaloriesBurned
                )
                val workout = HKWorkout.workoutWithActivityType(
                    workoutActivityType = HKWorkoutActivityTypeTraditionalStrengthTraining,
                    startDate = startDate,
                    endDate = endDate,
                    duration = duration,
                    totalEnergyBurned = energy,
                    totalDistance = null,
                    metadata = null
                )
                store.saveObject(workout) { success, error ->
                    if (success) {
                        KumaLogger.i("HealthSyncManager", "Synced ${record.title} to Apple HealthKit")
                        cont.resume(HealthSyncResult(isSuccess = true, syncDetails = "Synced ${record.title} to Apple Health"))
                    } else {
                        val msg = error?.localizedDescription ?: "Failed to save workout to HealthKit"
                        KumaLogger.e("HealthSyncManager", "Failed HealthKit save: $msg")
                        cont.resume(HealthSyncResult(isSuccess = false, syncDetails = msg))
                    }
                }
            } catch (e: Exception) {
                KumaLogger.e("HealthSyncManager", "Exception during HealthKit sync: ${e.message}")
                cont.resume(HealthSyncResult(isSuccess = false, syncDetails = e.message ?: "HealthKit error"))
            }
        }
    }
}
