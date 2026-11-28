package com.bearbones.kumalift.domain.model

import kotlin.math.abs

enum class HealthBodyMetricResolutionChoice {
    KEEP_KUMALIFT,
    USE_HEALTH_CONNECT
}

data class HealthBodySyncDecision(
    val autoFillWeightKg: Double? = null,
    val autoFillHeightCm: Double? = null,
    val weightConflict: HealthMetricConflict? = null,
    val heightConflict: HealthMetricConflict? = null,
    val syncedWeightRecordId: String? = null,
    val syncedWeightEpochMs: Long? = null,
    val syncedHeightRecordId: String? = null,
    val syncedHeightEpochMs: Long? = null,
    val alreadyUpToDate: Boolean = false
) {
    val hasConflicts: Boolean get() = weightConflict != null || heightConflict != null
    val hasAutoFills: Boolean get() = autoFillWeightKg != null || autoFillHeightCm != null
}

data class HealthMetricConflict(
    val currentKumaValue: Double,
    val healthConnectSample: HealthBodyMeasurementSample
)

/**
 * Predictable, non-destructive conflict resolution strategy for Android Health Connect body metrics:
 * - Never overwrites an existing differing KumaLift profile value silently.
 * - Deduplicates identical already-synced record IDs or identical measurements (< 0.05 tolerance for weight, < 0.2 cm for height).
 * - When multiple Health Connect records exist, selects newest valid sample within realistic bounds.
 */
object HealthBodySyncCoordinator {

    fun pickLatestValidSample(
        samples: List<HealthBodyMeasurementSample>,
        minValid: Double,
        maxValid: Double,
        ownPackageName: String? = null
    ): HealthBodyMeasurementSample? {
        if (samples.isEmpty()) return null
        return samples
            .distinctBy { it.recordId.ifBlank { "${it.recordedAtEpochMs}_${it.value}" } }
            .filter { it.value.isFinite() && it.value in minValid..maxValid }
            .sortedWith(
                compareByDescending<HealthBodyMeasurementSample> { it.recordedAtEpochMs }
                    // Deterministic tie-breaker when two sources write at the exact same timestamp:
                    // prefer external health sources over self-echoed records, then source name/id.
                    .thenBy { sample -> ownPackageName != null && sample.sourceApp == ownPackageName }
                    .thenBy { it.recordId }
            )
            .firstOrNull()
    }

    fun evaluateSync(
        profile: ProfileSettings,
        healthSettings: HealthSettings,
        payload: HealthBodyMetricsSyncPayload
    ): HealthBodySyncDecision {
        val weightSample = payload.weightSample?.takeIf { it.value.isFinite() && it.value in 20.0..400.0 }
        val heightSample = payload.heightSample?.takeIf { it.value.isFinite() && it.value in 50.0..280.0 }

        var autoWeight: Double? = null
        var weightConflict: HealthMetricConflict? = null
        var syncedWeightId: String? = null
        var syncedWeightMs: Long? = null
        var weightUpToDate = false

        if (weightSample != null) {
            val isSameRecord = !healthSettings.lastWeightSyncRecordId.isNullOrBlank() &&
                healthSettings.lastWeightSyncRecordId == weightSample.recordId
            val currentBw = profile.bodyweightKg
            when {
                currentBw == null -> {
                    autoWeight = weightSample.value
                    syncedWeightId = weightSample.recordId
                    syncedWeightMs = weightSample.recordedAtEpochMs
                }
                isSameRecord || abs(currentBw - weightSample.value) < 0.05 -> {
                    weightUpToDate = true
                    syncedWeightId = weightSample.recordId
                    syncedWeightMs = weightSample.recordedAtEpochMs
                }
                else -> {
                    weightConflict = HealthMetricConflict(
                        currentKumaValue = currentBw,
                        healthConnectSample = weightSample
                    )
                }
            }
        }

        var autoHeight: Double? = null
        var heightConflict: HealthMetricConflict? = null
        var syncedHeightId: String? = null
        var syncedHeightMs: Long? = null
        var heightUpToDate = false

        if (heightSample != null) {
            val isSameRecord = !healthSettings.lastHeightSyncRecordId.isNullOrBlank() &&
                healthSettings.lastHeightSyncRecordId == heightSample.recordId
            val currentHt = profile.heightCm
            when {
                currentHt == null -> {
                    autoHeight = heightSample.value
                    syncedHeightId = heightSample.recordId
                    syncedHeightMs = heightSample.recordedAtEpochMs
                }
                isSameRecord || abs(currentHt - heightSample.value) < 0.2 -> {
                    heightUpToDate = true
                    syncedHeightId = heightSample.recordId
                    syncedHeightMs = heightSample.recordedAtEpochMs
                }
                else -> {
                    heightConflict = HealthMetricConflict(
                        currentKumaValue = currentHt,
                        healthConnectSample = heightSample
                    )
                }
            }
        }

        return HealthBodySyncDecision(
            autoFillWeightKg = autoWeight,
            autoFillHeightCm = autoHeight,
            weightConflict = weightConflict,
            heightConflict = heightConflict,
            syncedWeightRecordId = syncedWeightId,
            syncedWeightEpochMs = syncedWeightMs,
            syncedHeightRecordId = syncedHeightId,
            syncedHeightEpochMs = syncedHeightMs,
            alreadyUpToDate = (weightUpToDate || weightSample == null) &&
                (heightUpToDate || heightSample == null) &&
                autoWeight == null &&
                autoHeight == null &&
                weightConflict == null &&
                heightConflict == null &&
                (weightSample != null || heightSample != null)
        )
    }
}
