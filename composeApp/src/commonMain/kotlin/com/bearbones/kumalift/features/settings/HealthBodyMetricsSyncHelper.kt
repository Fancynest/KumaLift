package com.bearbones.kumalift.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.health.HealthSyncManager
import com.bearbones.kumalift.core.health.rememberHealthPermissionLauncher
import com.bearbones.kumalift.core.localization.LocalStrings
import com.bearbones.kumalift.core.util.NumberFormatUtils
import com.bearbones.kumalift.core.util.UnitConverters
import com.bearbones.kumalift.data.repository.SettingsRepository
import com.bearbones.kumalift.domain.model.FitnessHistoryOrganizer
import com.bearbones.kumalift.domain.model.HealthBodyMetricResolutionChoice
import com.bearbones.kumalift.domain.model.HealthBodySyncCoordinator
import com.bearbones.kumalift.domain.model.HealthBodySyncDecision
import com.bearbones.kumalift.domain.model.HealthSyncStatus
import com.bearbones.kumalift.domain.model.WeightUnit
import kotlinx.coroutines.launch

class HealthBodyMetricsSyncController(
    val syncNow: () -> Unit,
    val statusMessage: String?,
    val isSyncing: Boolean,
    val clearMessage: () -> Unit
)

@Composable
fun rememberHealthBodyMetricsSyncController(
    settingsRepository: SettingsRepository,
    onStatusUpdated: (HealthSyncStatus) -> Unit = {}
): HealthBodyMetricsSyncController {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    var isSyncing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var pendingDecision by remember { mutableStateOf<HealthBodySyncDecision?>(null) }

    fun executeSync() {
        if (isSyncing) return
        scope.launch {
            isSyncing = true
            statusMessage = null
            val payload = HealthSyncManager.readLatestBodyMetrics()
            onStatusUpdated(payload.status)

            val current = settingsRepository.settings.value
            val weightUnit = current.units.weightUnit
            val lang = current.appearance.language
            val unitStr = if (weightUnit == WeightUnit.LB) s.unitLb else s.unitKg

            if (payload.status == HealthSyncStatus.UNAVAILABLE) {
                statusMessage = s.healthUnavailableStatus
                isSyncing = false
                return@launch
            }
            if (!payload.hasWeightReadPermission && !payload.hasHeightReadPermission) {
                statusMessage = s.healthPermissionRequiredStatus
                isSyncing = false
                return@launch
            }

            val decision = HealthBodySyncCoordinator.evaluateSync(
                profile = current.profile,
                healthSettings = current.health,
                payload = payload
            )

            if (decision.hasAutoFills || decision.syncedWeightRecordId != null || decision.syncedHeightRecordId != null) {
                settingsRepository.update { prev ->
                    prev.copy(
                        profile = prev.profile.copy(
                            bodyweightKg = decision.autoFillWeightKg ?: prev.profile.bodyweightKg,
                            heightCm = decision.autoFillHeightCm ?: prev.profile.heightCm
                        ),
                        health = prev.health.copy(
                            lastWeightSyncRecordId = decision.syncedWeightRecordId ?: prev.health.lastWeightSyncRecordId,
                            lastWeightSyncEpochMs = decision.syncedWeightEpochMs ?: prev.health.lastWeightSyncEpochMs,
                            lastHeightSyncRecordId = decision.syncedHeightRecordId ?: prev.health.lastHeightSyncRecordId,
                            lastHeightSyncEpochMs = decision.syncedHeightEpochMs ?: prev.health.lastHeightSyncEpochMs
                        )
                    )
                }
            }

            when {
                decision.hasConflicts -> {
                    pendingDecision = decision
                }
                decision.hasAutoFills -> {
                    val wTxt = decision.autoFillWeightKg?.let { kg ->
                        val disp = NumberFormatUtils.formatDecimal(UnitConverters.toDisplayWeight(kg, weightUnit), lang, 1, trimTrailingZeros = true)
                        "$disp $unitStr"
                    }
                    val hTxt = decision.autoFillHeightCm?.let { cm ->
                        val disp = NumberFormatUtils.formatDecimal(cm, lang, 1, trimTrailingZeros = true)
                        "$disp ${s.sizeUnitCm}"
                    }
                    statusMessage = s.healthSyncAutoFilledSummary(wTxt, hTxt)
                }
                decision.alreadyUpToDate -> {
                    statusMessage = s.healthSyncAlreadyUpToDate
                }
                else -> {
                    statusMessage = s.healthSyncNoNewBodyRecords
                }
            }
            isSyncing = false
        }
    }

    val permissionLauncher = rememberHealthPermissionLauncher(includeBodyMetrics = true) { refreshed ->
        onStatusUpdated(refreshed)
        if (HealthSyncManager.hasWeightReadPermission() || HealthSyncManager.hasHeightReadPermission()) {
            executeSync()
        } else {
            statusMessage = s.healthPermissionRequiredStatus
        }
    }

    pendingDecision?.let { decision ->
        HealthBodyMetricsConflictDialog(
            decision = decision,
            settingsRepository = settingsRepository,
            onResolved = { resolvedMsg ->
                pendingDecision = null
                statusMessage = resolvedMsg
            },
            onDismiss = { pendingDecision = null }
        )
    }

    return HealthBodyMetricsSyncController(
        syncNow = {
            if (!HealthSyncManager.hasWeightReadPermission() && !HealthSyncManager.hasHeightReadPermission()) {
                permissionLauncher()
            } else {
                executeSync()
            }
        },
        statusMessage = statusMessage,
        isSyncing = isSyncing,
        clearMessage = { statusMessage = null }
    )
}

@Composable
private fun HealthBodyMetricsConflictDialog(
    decision: HealthBodySyncDecision,
    settingsRepository: SettingsRepository,
    onResolved: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalStrings.current
    val colors = LocalKumaColors.current
    val settings = settingsRepository.settings.value
    val weightUnit = settings.units.weightUnit
    val lang = settings.appearance.language
    val unitStr = if (weightUnit == WeightUnit.LB) s.unitLb else s.unitKg

    var weightChoice by remember(decision) {
        mutableStateOf(
            if (decision.weightConflict != null) HealthBodyMetricResolutionChoice.KEEP_KUMALIFT else null
        )
    }
    var heightChoice by remember(decision) {
        mutableStateOf(
            if (decision.heightConflict != null) HealthBodyMetricResolutionChoice.KEEP_KUMALIFT else null
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KumaColors.SurfaceDim,
        title = {
            Text(
                text = s.healthSyncConflictTitle,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = s.healthSyncConflictSubtitle,
                    style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                )

                decision.weightConflict?.let { conflict ->
                    val currentTxt = NumberFormatUtils.formatDecimal(
                        UnitConverters.toDisplayWeight(conflict.currentKumaValue, weightUnit),
                        lang,
                        1,
                        trimTrailingZeros = true
                    ) + " $unitStr"
                    val hcTxt = NumberFormatUtils.formatDecimal(
                        UnitConverters.toDisplayWeight(conflict.healthConnectSample.value, weightUnit),
                        lang,
                        1,
                        trimTrailingZeros = true
                    ) + " $unitStr"
                    val dateTxt = FitnessHistoryOrganizer.formatFullDateWithYear(
                        conflict.healthConnectSample.recordedAtEpochMs,
                        "",
                        s
                    )
                    ConflictChoiceCard(
                        headline = s.healthConflictWeightLine(currentTxt, hcTxt, dateTxt),
                        selectedChoice = weightChoice ?: HealthBodyMetricResolutionChoice.KEEP_KUMALIFT,
                        keepKumaLabel = s.healthConflictKeepKumaAction,
                        useHealthLabel = s.healthConflictUseHealthAction,
                        onSelect = { weightChoice = it }
                    )
                }

                decision.heightConflict?.let { conflict ->
                    val currentTxt = NumberFormatUtils.formatDecimal(conflict.currentKumaValue, lang, 1, trimTrailingZeros = true) + " ${s.sizeUnitCm}"
                    val hcTxt = NumberFormatUtils.formatDecimal(conflict.healthConnectSample.value, lang, 1, trimTrailingZeros = true) + " ${s.sizeUnitCm}"
                    val dateTxt = FitnessHistoryOrganizer.formatFullDateWithYear(
                        conflict.healthConnectSample.recordedAtEpochMs,
                        "",
                        s
                    )
                    ConflictChoiceCard(
                        headline = s.healthConflictHeightLine(currentTxt, hcTxt, dateTxt),
                        selectedChoice = heightChoice ?: HealthBodyMetricResolutionChoice.KEEP_KUMALIFT,
                        keepKumaLabel = s.healthConflictKeepKumaAction,
                        useHealthLabel = s.healthConflictUseHealthAction,
                        onSelect = { heightChoice = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val wc = weightChoice ?: HealthBodyMetricResolutionChoice.KEEP_KUMALIFT
                    val hc = heightChoice ?: HealthBodyMetricResolutionChoice.KEEP_KUMALIFT
                    settingsRepository.update { prev ->
                        val resolvedWeight = if (decision.weightConflict != null && wc == HealthBodyMetricResolutionChoice.USE_HEALTH_CONNECT) {
                            decision.weightConflict.healthConnectSample.value
                        } else {
                            prev.profile.bodyweightKg
                        }
                        val resolvedHeight = if (decision.heightConflict != null && hc == HealthBodyMetricResolutionChoice.USE_HEALTH_CONNECT) {
                            decision.heightConflict.healthConnectSample.value
                        } else {
                            prev.profile.heightCm
                        }
                        prev.copy(
                            profile = prev.profile.copy(
                                bodyweightKg = resolvedWeight,
                                heightCm = resolvedHeight
                            ),
                            health = prev.health.copy(
                                lastWeightSyncRecordId = decision.weightConflict?.healthConnectSample?.recordId ?: prev.health.lastWeightSyncRecordId,
                                lastWeightSyncEpochMs = decision.weightConflict?.healthConnectSample?.recordedAtEpochMs ?: prev.health.lastWeightSyncEpochMs,
                                lastHeightSyncRecordId = decision.heightConflict?.healthConnectSample?.recordId ?: prev.health.lastHeightSyncRecordId,
                                lastHeightSyncEpochMs = decision.heightConflict?.healthConnectSample?.recordedAtEpochMs ?: prev.health.lastHeightSyncEpochMs
                            )
                        )
                    }
                    onResolved(s.healthSyncAlreadyUpToDate)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accentFill,
                    contentColor = colors.onAccentFill
                )
            ) {
                Text(s.save, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(s.cancel, color = colors.textSecondary)
            }
        }
    )
}

@Composable
private fun ConflictChoiceCard(
    headline: String,
    selectedChoice: HealthBodyMetricResolutionChoice,
    keepKumaLabel: String,
    useHealthLabel: String,
    onSelect: (HealthBodyMetricResolutionChoice) -> Unit
) {
    val colors = LocalKumaColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(KumaColors.SurfaceContainer)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = headline,
            style = MaterialTheme.typography.bodySmall.copy(
                color = colors.textPrimary,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { onSelect(HealthBodyMetricResolutionChoice.KEEP_KUMALIFT) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (selectedChoice == HealthBodyMetricResolutionChoice.KEEP_KUMALIFT) {
                        colors.accentFill.copy(alpha = 0.16f)
                    } else {
                        KumaColors.SurfaceDim
                    }
                )
            ) {
                Text(keepKumaLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = { onSelect(HealthBodyMetricResolutionChoice.USE_HEALTH_CONNECT) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (selectedChoice == HealthBodyMetricResolutionChoice.USE_HEALTH_CONNECT) {
                        colors.accentFill.copy(alpha = 0.16f)
                    } else {
                        KumaColors.SurfaceDim
                    }
                )
            ) {
                Text(useHealthLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
