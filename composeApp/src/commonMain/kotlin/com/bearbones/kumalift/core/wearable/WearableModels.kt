package com.bearbones.kumalift.core.wearable

enum class WearableSourceType(val displayName: String) {
    NONE("Tidak Terhubung"),
    HEALTH_CONNECT("Google Health Connect (Smartwatch)"),
    BLUETOOTH_LE("Bluetooth LE Sensor")
}

enum class WearableConnectionState(val displayName: String) {
    DISCONNECTED("Terputus"),
    CONNECTING("Menghubungkan..."),
    CONNECTED("Terhubung"),
    PERMISSION_REQUIRED("Izin Diperlukan"),
    UNAVAILABLE("Tidak Tersedia di Perangkat")
}

enum class HeartRateZone(
    val zoneNumber: Int,
    val nameKey: String,
    val description: String,
    val minPercent: Double,
    val maxPercent: Double
) {
    ZONE_1(1, "Zona 1", "Pemulihan & Pemanasan", 0.50, 0.60),
    ZONE_2(2, "Zona 2", "Aerobik Ringan", 0.60, 0.70),
    ZONE_3(3, "Zona 3", "Kardiorespirasi Moderat", 0.70, 0.80),
    ZONE_4(4, "Zona 4", "Hipertrofi / Ambang Laktat", 0.80, 0.90),
    ZONE_5(5, "Zona 5", "Maksimal / All-out PR Effort", 0.90, 1.00);

    companion object {
        fun calculateZone(bpm: Int, age: Int = 25): HeartRateZone {
            val maxHr = (220 - age).coerceAtLeast(150)
            val fraction = bpm.toDouble() / maxHr.toDouble()
            return when {
                fraction < 0.60 -> ZONE_1
                fraction < 0.70 -> ZONE_2
                fraction < 0.80 -> ZONE_3
                fraction < 0.90 -> ZONE_4
                else -> ZONE_5
            }
        }
    }
}

data class LiveWearableTelemetry(
    val connectionState: WearableConnectionState = WearableConnectionState.DISCONNECTED,
    val currentBpm: Int? = null,
    val currentZone: HeartRateZone? = null,
    val activeCaloriesKcal: Double? = null,
    val avgBpm: Int? = null,
    val peakBpm: Int? = null,
    val deviceName: String? = null,
    val sourceType: WearableSourceType = WearableSourceType.NONE,
    val lastUpdateEpochMs: Long = 0L,
    val isHeartRateRestingReady: Boolean = false
)

data class DiscoveredBleDevice(
    val name: String,
    val address: String,
    val rssi: Int = 0
)

data class WearableSummarySession(
    val avgBpm: Int?,
    val peakBpm: Int?,
    val activeCaloriesKcal: Double?,
    val sampleCount: Int,
    val sourceName: String
)
