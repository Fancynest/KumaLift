package com.bearbones.kumalift.core.smartwatch

import com.bearbones.kumalift.core.logging.KumaLogger
import com.bearbones.kumalift.domain.model.SmartwatchConnectionStatus
import com.bearbones.kumalift.domain.model.SmartwatchDeviceType
import com.bearbones.kumalift.domain.model.SmartwatchSyncEvent
import com.bearbones.kumalift.domain.model.SmartwatchWorkoutPayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual object SmartwatchCompanionManager {
    actual val deviceType: SmartwatchDeviceType = SmartwatchDeviceType.WEAR_OS

    private val _connectionStatus = MutableStateFlow(SmartwatchConnectionStatus.DISCONNECTED)
    actual val connectionStatus: StateFlow<SmartwatchConnectionStatus> = _connectionStatus.asStateFlow()

    private var eventListener: ((SmartwatchSyncEvent) -> Unit)? = null

    actual fun connect() {
        _connectionStatus.value = SmartwatchConnectionStatus.CONNECTED
        KumaLogger.i("SmartwatchCompanionManager", "Wear OS companion connected")
    }

    actual fun disconnect() {
        _connectionStatus.value = SmartwatchConnectionStatus.DISCONNECTED
        KumaLogger.i("SmartwatchCompanionManager", "Wear OS companion disconnected")
    }

    actual fun sendActiveWorkoutUpdate(payload: SmartwatchWorkoutPayload) {
        if (_connectionStatus.value != SmartwatchConnectionStatus.CONNECTED) return
        KumaLogger.d(
            "SmartwatchCompanionManager",
            "Broadcasting workout state to watch: ${payload.currentExerciseName} Set #${payload.currentSetNumber}"
        )
    }

    actual fun sendRestTimerTick(remainingSeconds: Int) {
        if (_connectionStatus.value != SmartwatchConnectionStatus.CONNECTED) return
        KumaLogger.d("SmartwatchCompanionManager", "Syncing rest timer tick: ${remainingSeconds}s")
    }

    actual fun sendHapticPulse() {
        if (_connectionStatus.value != SmartwatchConnectionStatus.CONNECTED) return
        KumaLogger.d("SmartwatchCompanionManager", "Sending companion haptic alert")
    }

    actual fun registerEventListener(listener: (SmartwatchSyncEvent) -> Unit) {
        eventListener = listener
    }

    actual fun unregisterEventListener() {
        eventListener = null
    }

    // Invoked by Wear OS data receiver when watch user clicks complete set or skip rest
    fun dispatchIncomingEvent(event: SmartwatchSyncEvent) {
        eventListener?.invoke(event)
    }
}
