package com.bearbones.kumalift.core.smartwatch

import com.bearbones.kumalift.core.logging.KumaLogger
import com.bearbones.kumalift.domain.model.SmartwatchConnectionStatus
import com.bearbones.kumalift.domain.model.SmartwatchDeviceType
import com.bearbones.kumalift.domain.model.SmartwatchSyncEvent
import com.bearbones.kumalift.domain.model.SmartwatchWorkoutPayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.WatchConnectivity.WCSession

actual object SmartwatchCompanionManager {
    actual val deviceType: SmartwatchDeviceType = SmartwatchDeviceType.APPLE_WATCH

    private val _connectionStatus = MutableStateFlow(SmartwatchConnectionStatus.DISCONNECTED)
    actual val connectionStatus: StateFlow<SmartwatchConnectionStatus> = _connectionStatus.asStateFlow()

    private var eventListener: ((SmartwatchSyncEvent) -> Unit)? = null

    actual fun connect() {
        if (WCSession.isSupported()) {
            val session = WCSession.defaultSession
            session.activateSession()
            _connectionStatus.value = if (session.isPaired() && session.isWatchAppInstalled()) {
                SmartwatchConnectionStatus.CONNECTED
            } else {
                SmartwatchConnectionStatus.DISCONNECTED
            }
        } else {
            _connectionStatus.value = SmartwatchConnectionStatus.UNAVAILABLE
        }
        KumaLogger.i("SmartwatchCompanionManager", "Apple Watch connectivity initialized: ${_connectionStatus.value}")
    }

    actual fun disconnect() {
        _connectionStatus.value = SmartwatchConnectionStatus.DISCONNECTED
        KumaLogger.i("SmartwatchCompanionManager", "Apple Watch connectivity session disconnected")
    }

    actual fun sendActiveWorkoutUpdate(payload: SmartwatchWorkoutPayload) {
        if (_connectionStatus.value != SmartwatchConnectionStatus.CONNECTED) return
        KumaLogger.d(
            "SmartwatchCompanionManager",
            "Relaying workout state to Apple Watch: ${payload.currentExerciseName} Set #${payload.currentSetNumber}"
        )
    }

    actual fun sendRestTimerTick(remainingSeconds: Int) {
        if (_connectionStatus.value != SmartwatchConnectionStatus.CONNECTED) return
        KumaLogger.d("SmartwatchCompanionManager", "Relaying timer tick to Apple Watch: ${remainingSeconds}s")
    }

    actual fun sendHapticPulse() {
        if (_connectionStatus.value != SmartwatchConnectionStatus.CONNECTED) return
        KumaLogger.d("SmartwatchCompanionManager", "Sending watch haptic alert")
    }

    actual fun registerEventListener(listener: (SmartwatchSyncEvent) -> Unit) {
        eventListener = listener
    }

    actual fun unregisterEventListener() {
        eventListener = null
    }

    fun dispatchIncomingEvent(event: SmartwatchSyncEvent) {
        eventListener?.invoke(event)
    }
}
