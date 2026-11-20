package com.bearbones.kumalift.core.smartwatch

import com.bearbones.kumalift.core.logging.KumaLogger
import com.bearbones.kumalift.domain.model.SmartwatchConnectionStatus
import com.bearbones.kumalift.domain.model.SmartwatchDeviceType
import com.bearbones.kumalift.domain.model.SmartwatchSyncEvent
import com.bearbones.kumalift.domain.model.SmartwatchWorkoutPayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970
import platform.WatchConnectivity.WCSession
import platform.WatchConnectivity.WCSessionActivationState
import platform.WatchConnectivity.WCSessionDelegateProtocol
import platform.darwin.NSObject

actual object SmartwatchCompanionManager {
    actual val deviceType: SmartwatchDeviceType = SmartwatchDeviceType.APPLE_WATCH

    private val _connectionStatus = MutableStateFlow(SmartwatchConnectionStatus.DISCONNECTED)
    actual val connectionStatus: StateFlow<SmartwatchConnectionStatus> = _connectionStatus.asStateFlow()

    private var eventListener: ((SmartwatchSyncEvent) -> Unit)? = null

    private class AppleWatchSessionDelegate : NSObject(), WCSessionDelegateProtocol {
        override fun session(
            session: WCSession,
            activationDidCompleteWithState: WCSessionActivationState,
            error: platform.Foundation.NSError?
        ) {
            updateStatus(session)
        }

        override fun sessionDidBecomeInactive(session: WCSession) {
            _connectionStatus.value = SmartwatchConnectionStatus.DISCONNECTED
        }

        override fun sessionDidDeactivate(session: WCSession) {
            _connectionStatus.value = SmartwatchConnectionStatus.DISCONNECTED
            WCSession.defaultSession.activateSession()
        }

        override fun session(
            session: WCSession,
            didReceiveMessage: Map<Any?, *>
        ) {
            val action = didReceiveMessage["action"] as? String ?: return
            val heartRate = (didReceiveMessage["heartRate"] as? Number)?.toInt()
            val event = SmartwatchSyncEvent(
                action = action,
                timestampEpochMs = (NSDate().timeIntervalSince1970 * 1000.0).toLong(),
                heartRateBpm = heartRate
            )
            dispatchIncomingEvent(event)
        }
    }

    private var sessionDelegate: AppleWatchSessionDelegate? = null

    private fun updateStatus(session: WCSession) {
        _connectionStatus.value = if (session.isPaired() && session.isWatchAppInstalled()) {
            SmartwatchConnectionStatus.CONNECTED
        } else {
            SmartwatchConnectionStatus.DISCONNECTED
        }
    }

    actual fun connect() {
        if (WCSession.isSupported()) {
            val session = WCSession.defaultSession
            if (sessionDelegate == null) {
                val del = AppleWatchSessionDelegate()
                sessionDelegate = del
                session.delegate = del
            }
            session.activateSession()
            updateStatus(session)
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
        if (!WCSession.isSupported()) return
        val session = WCSession.defaultSession
        if (!session.isReachable()) return
        try {
            val json = Json.encodeToString(payload)
            val msg = mapOf<Any?, Any?>(
                "type" to "WORKOUT_UPDATE",
                "payload" to json
            )
            session.sendMessage(msg, replyHandler = null, errorHandler = { err ->
                KumaLogger.w("SmartwatchCompanionManager", "Watch send message failed: ${err?.localizedDescription}")
            })
        } catch (e: Exception) {
            KumaLogger.w("SmartwatchCompanionManager", "Failed to encode workout update: ${e.message}")
        }
    }

    actual fun sendRestTimerTick(remainingSeconds: Int) {
        if (!WCSession.isSupported()) return
        val session = WCSession.defaultSession
        if (!session.isReachable()) return
        val msg = mapOf<Any?, Any?>(
            "type" to "REST_TIMER_TICK",
            "remainingSeconds" to remainingSeconds
        )
        session.sendMessage(msg, replyHandler = null, errorHandler = null)
    }

    actual fun sendHapticPulse() {
        if (!WCSession.isSupported()) return
        val session = WCSession.defaultSession
        if (!session.isReachable()) return
        val msg = mapOf<Any?, Any?>("type" to "HAPTIC_PULSE")
        session.sendMessage(msg, replyHandler = null, errorHandler = null)
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
