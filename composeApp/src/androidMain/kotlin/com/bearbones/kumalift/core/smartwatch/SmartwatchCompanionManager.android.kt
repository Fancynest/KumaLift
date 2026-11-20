package com.bearbones.kumalift.core.smartwatch

import com.bearbones.kumalift.core.logging.KumaLogger
import com.bearbones.kumalift.core.timer.AndroidAppProvider
import com.bearbones.kumalift.domain.model.SmartwatchConnectionStatus
import com.bearbones.kumalift.domain.model.SmartwatchDeviceType
import com.bearbones.kumalift.domain.model.SmartwatchSyncEvent
import com.bearbones.kumalift.domain.model.SmartwatchWorkoutPayload
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

actual object SmartwatchCompanionManager : MessageClient.OnMessageReceivedListener {
    actual val deviceType: SmartwatchDeviceType = SmartwatchDeviceType.WEAR_OS

    private val _connectionStatus = MutableStateFlow(SmartwatchConnectionStatus.DISCONNECTED)
    actual val connectionStatus: StateFlow<SmartwatchConnectionStatus> = _connectionStatus.asStateFlow()

    private var eventListener: ((SmartwatchSyncEvent) -> Unit)? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private const val PATH_WORKOUT_UPDATE = "/kumalift/workout_update"
    private const val PATH_TIMER_TICK = "/kumalift/timer_tick"
    private const val PATH_HAPTIC = "/kumalift/haptic"
    private const val PATH_EVENT = "/kumalift/event"

    var isTestMode: Boolean = false

    actual fun connect() {
        if (isTestMode || AndroidAppProvider.appContext == null) {
            _connectionStatus.value = SmartwatchConnectionStatus.CONNECTED
            return
        }
        val context = AndroidAppProvider.appContext ?: return
        _connectionStatus.value = SmartwatchConnectionStatus.CONNECTING
        scope.launch {
            try {
                val nodes = Tasks.await(Wearable.getNodeClient(context).connectedNodes)
                if (nodes.isNotEmpty()) {
                    _connectionStatus.value = SmartwatchConnectionStatus.CONNECTED
                    KumaLogger.i("SmartwatchCompanionManager", "Wear OS node connected: ${nodes.first().id}")
                } else {
                    _connectionStatus.value = SmartwatchConnectionStatus.DISCONNECTED
                    KumaLogger.i("SmartwatchCompanionManager", "No Wear OS nodes found")
                }
                Wearable.getMessageClient(context).addListener(this@SmartwatchCompanionManager)
            } catch (e: Exception) {
                KumaLogger.w("SmartwatchCompanionManager", "Failed to connect Wear OS: ${e.message}")
                _connectionStatus.value = SmartwatchConnectionStatus.DISCONNECTED
            }
        }
    }

    actual fun disconnect() {
        val context = AndroidAppProvider.appContext
        if (context != null) {
            try {
                Wearable.getMessageClient(context).removeListener(this)
            } catch (e: Exception) {
                KumaLogger.w("SmartwatchCompanionManager", "Failed to remove listener: ${e.message}")
            }
        }
        _connectionStatus.value = SmartwatchConnectionStatus.DISCONNECTED
        KumaLogger.i("SmartwatchCompanionManager", "Wear OS companion disconnected")
    }

    actual fun sendActiveWorkoutUpdate(payload: SmartwatchWorkoutPayload) {
        if (_connectionStatus.value != SmartwatchConnectionStatus.CONNECTED) return
        val context = AndroidAppProvider.appContext ?: return
        scope.launch {
            try {
                val json = Json.encodeToString(payload)
                val bytes = json.toByteArray(Charsets.UTF_8)
                val nodes = Tasks.await(Wearable.getNodeClient(context).connectedNodes)
                for (node in nodes) {
                    Wearable.getMessageClient(context).sendMessage(node.id, PATH_WORKOUT_UPDATE, bytes)
                }
            } catch (e: Exception) {
                KumaLogger.w("SmartwatchCompanionManager", "Error sending workout update: ${e.message}")
            }
        }
    }

    actual fun sendRestTimerTick(remainingSeconds: Int) {
        if (_connectionStatus.value != SmartwatchConnectionStatus.CONNECTED) return
        val context = AndroidAppProvider.appContext ?: return
        scope.launch {
            try {
                val bytes = remainingSeconds.toString().toByteArray(Charsets.UTF_8)
                val nodes = Tasks.await(Wearable.getNodeClient(context).connectedNodes)
                for (node in nodes) {
                    Wearable.getMessageClient(context).sendMessage(node.id, PATH_TIMER_TICK, bytes)
                }
            } catch (e: Exception) {
                // Ignore dropped ticks
            }
        }
    }

    actual fun sendHapticPulse() {
        if (_connectionStatus.value != SmartwatchConnectionStatus.CONNECTED) return
        val context = AndroidAppProvider.appContext ?: return
        scope.launch {
            try {
                val nodes = Tasks.await(Wearable.getNodeClient(context).connectedNodes)
                for (node in nodes) {
                    Wearable.getMessageClient(context).sendMessage(node.id, PATH_HAPTIC, ByteArray(0))
                }
            } catch (e: Exception) {
                KumaLogger.w("SmartwatchCompanionManager", "Error sending haptic: ${e.message}")
            }
        }
    }

    actual fun registerEventListener(listener: (SmartwatchSyncEvent) -> Unit) {
        eventListener = listener
    }

    actual fun unregisterEventListener() {
        eventListener = null
    }

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path == PATH_EVENT) {
            try {
                val json = String(event.data, Charsets.UTF_8)
                val syncEvent = Json.decodeFromString<SmartwatchSyncEvent>(json)
                dispatchIncomingEvent(syncEvent)
            } catch (e: Exception) {
                KumaLogger.e("SmartwatchCompanionManager", "Failed to deserialize watch event: ${e.message}")
            }
        }
    }

    fun dispatchIncomingEvent(event: SmartwatchSyncEvent) {
        eventListener?.invoke(event)
    }
}
