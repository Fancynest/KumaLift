package com.bearbones.kumalift.domain.model

import com.bearbones.kumalift.core.bluetooth.BleHeartRateManager
import com.bearbones.kumalift.core.health.HealthSyncManager
import com.bearbones.kumalift.core.smartwatch.SmartwatchCompanionManager
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HealthAndCompanionTests {

    @Test
    fun testCalorieEstimationFormula() {
        // 60 minutes, 80kg bodyweight, 4.5 MET
        // 4.5 * 80 * 1.0 = 360.0 kcal
        val cal60 = WorkoutHealthRecord.estimateCalories(durationMinutes = 60.0, bodyWeightKg = 80.0)
        assertEquals(360.0, cal60)

        // 30 minutes, 70kg bodyweight, 4.5 MET
        // 4.5 * 70 * 0.5 = 157.5 kcal
        val cal30 = WorkoutHealthRecord.estimateCalories(durationMinutes = 30.0, bodyWeightKg = 70.0)
        assertEquals(157.5, cal30)

        // 0 or negative duration returns 0
        assertEquals(0.0, WorkoutHealthRecord.estimateCalories(0.0))
        assertEquals(0.0, WorkoutHealthRecord.estimateCalories(-15.0))
    }

    @Test
    fun testHealthSyncManagerContract() = runBlocking {
        assertNotNull(HealthSyncManager.platformType)
        val initialStatus = HealthSyncManager.getStatus()
        assertNotNull(initialStatus)

        val record = WorkoutHealthRecord(
            workoutId = "w_test_123",
            title = "Chest & Triceps",
            startTimeEpochMs = 1700000000000L,
            endTimeEpochMs = 1700003600000L,
            activeDurationSeconds = 3600L,
            estimatedCaloriesBurned = 350.0,
            totalTonnageKg = 4200.0,
            totalSetsCompleted = 16
        )

        val result = HealthSyncManager.syncWorkout(record)
        assertTrue(result.isSuccess)
        assertTrue(result.syncDetails.isNotEmpty())
    }

    @Test
    fun testSmartwatchCompanionLifecycle() {
        assertNotNull(SmartwatchCompanionManager.deviceType)

        // Initial connection state
        SmartwatchCompanionManager.disconnect()
        assertEquals(SmartwatchConnectionStatus.DISCONNECTED, SmartwatchCompanionManager.connectionStatus.value)

        // Connect
        SmartwatchCompanionManager.connect()
        assertEquals(SmartwatchConnectionStatus.CONNECTED, SmartwatchCompanionManager.connectionStatus.value)

        // Payload snapshot validation
        val payload = SmartwatchWorkoutPayload(
            workoutId = "w_123",
            workoutTitle = "Leg Day",
            currentExerciseName = "Barbell Back Squat",
            currentSetNumber = 3,
            totalSetsForExercise = 4,
            targetWeightKg = 100.0,
            targetReps = 8,
            isRestTimerActive = true,
            restSecondsRemaining = 45,
            elapsedWorkoutSeconds = 1200L
        )
        SmartwatchCompanionManager.sendActiveWorkoutUpdate(payload)
        SmartwatchCompanionManager.sendRestTimerTick(45)
        SmartwatchCompanionManager.sendHapticPulse()

        // Event listener dispatch
        var receivedEvent: SmartwatchSyncEvent? = null
        SmartwatchCompanionManager.registerEventListener { event ->
            receivedEvent = event
        }

        val testEvent = SmartwatchSyncEvent(
            action = SmartwatchSyncEvent.ACTION_COMPLETE_SET,
            timestampEpochMs = 1700000000000L,
            heartRateBpm = 142
        )
        SmartwatchCompanionManager.dispatchIncomingEvent(testEvent)
        assertEquals(testEvent, receivedEvent)

        SmartwatchCompanionManager.unregisterEventListener()
        SmartwatchCompanionManager.disconnect()
        assertEquals(SmartwatchConnectionStatus.DISCONNECTED, SmartwatchCompanionManager.connectionStatus.value)
    }

    @Test
    fun testSmartwatchSettingsDefaults() {
        val settings = SmartwatchSettings()
        assertTrue(settings.liveTimerSync)
        assertTrue(settings.hapticAlertsOnWatch)
        assertTrue(settings.autoConnectOnWorkoutStart)
    }

    @Test
    fun testBleHeartRateManagerLifecycle() {
        BleHeartRateManager.disconnect()
        assertNull(BleHeartRateManager.connectedDevice.value)
        assertNull(BleHeartRateManager.currentHeartRateBpm.value)

        BleHeartRateManager.startScan()
        assertTrue(BleHeartRateManager.isScanning.value)
        assertTrue(BleHeartRateManager.discoveredDevices.value.isNotEmpty())

        val target = BleHeartRateManager.discoveredDevices.value.first()
        BleHeartRateManager.connect(target)
        assertFalse(BleHeartRateManager.isScanning.value)
        assertEquals(target, BleHeartRateManager.connectedDevice.value)
        assertNotNull(BleHeartRateManager.currentHeartRateBpm.value)

        BleHeartRateManager.disconnect()
        assertNull(BleHeartRateManager.connectedDevice.value)
        assertNull(BleHeartRateManager.currentHeartRateBpm.value)
    }
}
