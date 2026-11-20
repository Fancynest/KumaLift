package com.bearbones.kumalift.core.smartwatch

import com.bearbones.kumalift.domain.model.SmartwatchConnectionStatus
import com.bearbones.kumalift.domain.model.SmartwatchDeviceType
import com.bearbones.kumalift.domain.model.SmartwatchSyncEvent
import com.bearbones.kumalift.domain.model.SmartwatchWorkoutPayload
import kotlinx.coroutines.flow.StateFlow

expect object SmartwatchCompanionManager {
    val deviceType: SmartwatchDeviceType
    val connectionStatus: StateFlow<SmartwatchConnectionStatus>
    fun connect()
    fun disconnect()
    fun sendActiveWorkoutUpdate(payload: SmartwatchWorkoutPayload)
    fun sendRestTimerTick(remainingSeconds: Int)
    fun sendHapticPulse()
    fun registerEventListener(listener: (SmartwatchSyncEvent) -> Unit)
    fun unregisterEventListener()
}
