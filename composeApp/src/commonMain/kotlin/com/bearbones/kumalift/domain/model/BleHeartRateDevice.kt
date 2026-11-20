package com.bearbones.kumalift.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class BleHeartRateDevice(
    val id: String,
    val name: String,
    val rssi: Int = 0
)
