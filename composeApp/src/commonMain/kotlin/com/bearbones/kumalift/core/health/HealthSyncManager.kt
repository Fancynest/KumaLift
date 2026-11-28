package com.bearbones.kumalift.core.health

import androidx.compose.runtime.Composable
import com.bearbones.kumalift.domain.model.HealthBodyMetricsSyncPayload
import com.bearbones.kumalift.domain.model.HealthPlatformType
import com.bearbones.kumalift.domain.model.HealthSyncResult
import com.bearbones.kumalift.domain.model.HealthSyncStatus
import com.bearbones.kumalift.domain.model.WorkoutHealthRecord

expect object HealthSyncManager {
    val platformType: HealthPlatformType
    fun getStatus(): HealthSyncStatus
    suspend fun refreshStatus(): HealthSyncStatus
    suspend fun requestPermissions(): Boolean
    suspend fun syncWorkout(record: WorkoutHealthRecord): HealthSyncResult
    fun hasWorkoutSyncPermission(): Boolean
    fun hasWeightReadPermission(): Boolean
    fun hasHeightReadPermission(): Boolean
    fun hasWeightWritePermission(): Boolean
    fun hasHeightWritePermission(): Boolean
    suspend fun readLatestBodyMetrics(): HealthBodyMetricsSyncPayload
    suspend fun writeBodyMetrics(
        bodyweightKg: Double? = null,
        heightCm: Double? = null,
        weightClientRecordId: String? = null,
        heightClientRecordId: String? = null
    ): HealthSyncResult
    fun openHealthSettings(): Boolean
}

@Composable
expect fun rememberHealthPermissionLauncher(
    includeBodyMetrics: Boolean = true,
    onResult: (HealthSyncStatus) -> Unit
): () -> Unit
