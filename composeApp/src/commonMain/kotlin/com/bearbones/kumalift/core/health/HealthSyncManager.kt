package com.bearbones.kumalift.core.health

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
}
