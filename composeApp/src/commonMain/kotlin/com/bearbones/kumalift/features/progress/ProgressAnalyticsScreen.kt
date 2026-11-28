package com.bearbones.kumalift.features.progress

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumalift.core.designsystem.ChartAxisTickText
import com.bearbones.kumalift.core.designsystem.KumaBadge
import com.bearbones.kumalift.core.designsystem.KumaCard
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.KumaSessionBarChart
import com.bearbones.kumalift.core.designsystem.KumaSessionBarItem
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.core.designsystem.buildDistinctChartDateLabels
import com.bearbones.kumalift.core.designsystem.formatCompactChartValue
import com.bearbones.kumalift.core.designsystem.formatIntegerChartValue
import com.bearbones.kumalift.core.localization.LocalStrings
import com.bearbones.kumalift.core.util.NumberFormatUtils
import com.bearbones.kumalift.core.util.UnitConverters
import com.bearbones.kumalift.core.util.currentEpochMillis
import com.bearbones.kumalift.data.repository.PersonalRecordRepository
import com.bearbones.kumalift.data.repository.SettingsRepository
import com.bearbones.kumalift.domain.model.AppLanguage
import com.bearbones.kumalift.domain.model.DateFormatPreference
import com.bearbones.kumalift.domain.model.FitnessHistoryOrganizer
import com.bearbones.kumalift.domain.model.MuscleVolumeCalculator
import com.bearbones.kumalift.domain.model.PersonalRecord
import com.bearbones.kumalift.domain.model.PrExerciseGroup
import com.bearbones.kumalift.domain.model.WeightUnit
import com.bearbones.kumalift.features.analytics.AnatomyBodyHighlighter
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun ProgressAnalyticsScreen(
    personalRecordRepository: PersonalRecordRepository,
    settingsRepository: SettingsRepository? = null,
    workoutRepository: com.bearbones.kumalift.data.repository.WorkoutRepository? = null,
    listState: LazyListState = rememberLazyListState()
) {
    val settings = settingsRepository?.settings?.collectAsState()?.value
    val s = LocalStrings.current
    val language = settings?.appearance?.language ?: AppLanguage.SYSTEM
    val dateFormat = settings?.units?.dateFormat ?: DateFormatPreference.SYSTEM
    val isLb = settings?.units?.weightUnit == com.bearbones.kumalift.domain.model.WeightUnit.LB
    val mult = if (isLb) 2.20462 else 1.0
    val unitStr = if (isLb) s.unitLb else s.unitKg
    val prs by personalRecordRepository.personalRecords.collectAsState()
    val completedWorkouts by (workoutRepository?.completedWorkouts ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsState()
    val muscleVolumeStats = remember(completedWorkouts) {
        MuscleVolumeCalculator.calculateWeeklyVolume(completedWorkouts)
    }
    var selectedTimeframe by rememberSaveable { mutableStateOf("3M") }
    val timeframes = listOf("1M", "3M", "6M", "1Y", "ALL")

    val cutoffEpochMs = remember(selectedTimeframe) {
        val days = when (selectedTimeframe) {
            "1M" -> 30L
            "3M" -> 90L
            "6M" -> 180L
            "1Y" -> 365L
            else -> null
        }
        days?.let { currentEpochMillis() - it * 86_400_000L }
    }
    val filteredPrs = remember(prs, cutoffEpochMs) {
        if (cutoffEpochMs == null) prs
        else prs.filter { it.dateAchievedEpochMs <= 0L || it.dateAchievedEpochMs >= cutoffEpochMs }
    }
    val filteredWorkouts = remember(completedWorkouts, cutoffEpochMs) {
        if (cutoffEpochMs == null) completedWorkouts
        else completedWorkouts.filter { it.startTimeEpochMs <= 0L || it.startTimeEpochMs >= cutoffEpochMs }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KumaColors.CanvasLowest)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Screen Top Header with statusBarsPadding
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KumaColors.SurfaceDim)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = s.progressAndAnalyticsTitle,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = KumaColors.TextWhite
                    )
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    // Timeframe Segmented Control
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(KumaColors.SurfaceContainer)
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        timeframes.forEach { tf ->
                            val isSelected = tf == selectedTimeframe
                            val tfLabel = when (tf) {
                                "1M" -> s.timeframeMonthShort(1)
                                "3M" -> s.timeframeMonthShort(3)
                                "6M" -> s.timeframeMonthShort(6)
                                "1Y" -> s.timeframeYearShort(1)
                                else -> s.all.uppercase()
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) KumaColors.SurfaceContainerHigh else KumaColors.SurfaceContainer)
                                    .border(
                                        width = if (isSelected) 1.dp else 0.dp,
                                        color = if (isSelected) (if (LocalKumaColors.current.isDark) LocalKumaColors.current.accentFill else LocalKumaColors.current.accentChart) else KumaColors.BorderDefault,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedTimeframe = tf }
                                    .padding(horizontal = 2.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tfLabel,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) LocalKumaColors.current.accentText else KumaColors.TextMuted
                                    )
                                )
                            }
                        }
                    }
                }

                // 1. Estimated 1RM Trajectory Card
                item {
                    val activePrPool = filteredPrs
                    val topPr = activePrPool.maxByOrNull { effectiveOneRmKg(it) }
                    if (topPr == null) {
                        KumaCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = KumaColors.SurfaceContainerLow,
                            cornerRadius = 18.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = s.estimatedOneRepMaxHeader,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = LocalKumaColors.current.accentText,
                                        letterSpacing = 0.8.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = s.noPrsLoggedYet,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = KumaColors.TextMuted,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    } else {
                        val exercisePrs = activePrPool
                            .filter { it.exerciseId == topPr.exerciseId }
                            .sortedBy { it.dateAchievedEpochMs }
                            .takeLast(6)
                        val heroOneRmDisplay = effectiveOneRmKg(topPr) * mult
                        val heroText = "${formatMetricDecimal(heroOneRmDisplay, language)} $unitStr"
                        val actualWeightText = formatMetricDecimal(topPr.weightKg * mult, language)
                        val actualLiftSummary = "$actualWeightText $unitStr × ${topPr.reps} ${s.repsAbbrev}"

                        var selectedPrIdx by remember(exercisePrs) {
                            mutableStateOf((exercisePrs.size - 1).coerceAtLeast(0))
                        }
                        val clampedPrIdx = selectedPrIdx.coerceIn(0, (exercisePrs.size - 1).coerceAtLeast(0))

                        KumaCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = KumaColors.SurfaceContainerLow,
                            cornerRadius = 18.dp
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = s.estimatedOneRepMaxHeader,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = LocalKumaColors.current.accentText,
                                                letterSpacing = 0.8.sp
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = topPr.exerciseName,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(PillShape)
                                            .background(LocalKumaColors.current.accentTint)
                                            .border(
                                                1.dp,
                                                if (LocalKumaColors.current.isDark) LocalKumaColors.current.accentFill else LocalKumaColors.current.accentFillBorder,
                                                PillShape
                                            )
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                                contentDescription = null,
                                                tint = LocalKumaColors.current.accentTextOnTint,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = topPr.prTier.ifBlank { s.prBadge },
                                                maxLines = 1,
                                                softWrap = false,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = LocalKumaColors.current.accentTextOnTint,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = heroText,
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 32.sp,
                                        color = KumaColors.TextWhite
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = s.progressActualLiftLabel(actualLiftSummary),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = KumaColors.TextMuted
                                    )
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                val chartPoints = remember(exercisePrs, mult, language, dateFormat, s.todayShortLabel) {
                                    val xLabels = buildDistinctChartDateLabels(
                                        epochs = exercisePrs.map { it.dateAchievedEpochMs },
                                        fallbacks = exercisePrs.map { it.dateAchieved },
                                        pref = dateFormat,
                                        language = language,
                                        todayFallback = s.todayShortLabel
                                    )
                                    exercisePrs.mapIndexed { idx, pr ->
                                        val valueDisplay = effectiveOneRmKg(pr) * mult
                                        val weightDisplay = pr.weightKg * mult
                                        OneRmPlotPoint(
                                            valueDisplay = valueDisplay,
                                            valueLabel = formatMetricDecimal(valueDisplay, language),
                                            xLabel = xLabels[idx],
                                            detailSummary = "${xLabels[idx]} · ${formatMetricDecimal(weightDisplay, language)} $unitStr × ${pr.reps} ${s.repsAbbrev} · ${s.estimatedOneRmLabel}: ${formatMetricDecimal(valueDisplay, language)} $unitStr"
                                        )
                                    }
                                }

                                OneRepMaxLineChart(
                                    points = chartPoints,
                                    selectedIndex = clampedPrIdx,
                                    onSelectPoint = { selectedPrIdx = it },
                                    language = language
                                )

                                chartPoints.getOrNull(clampedPrIdx)?.let { activePoint ->
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = activePoint.detailSummary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = KumaColors.TextSecondary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Weekly Tonnage & Volume
                item {
                    val activeWorkouts = filteredWorkouts
                    if (activeWorkouts.isEmpty()) {
                        KumaCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = KumaColors.SurfaceContainerLow,
                            cornerRadius = 18.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = s.weeklyVolumeTonnageHeader,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = s.noVolumeHistoryYet,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = KumaColors.TextMuted,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    } else {
                        val recentWorkouts = remember(activeWorkouts) {
                            activeWorkouts.sortedBy { it.startTimeEpochMs }.takeLast(6)
                        }
                        val avgVolDisplay = recentWorkouts.map { it.totalVolumeKg * mult }.average()
                        val formattedAvg = formatIntegerChartValue(avgVolDisplay, language)
                        var selectedBarIdx by remember(recentWorkouts) {
                            mutableStateOf((recentWorkouts.size - 1).coerceAtLeast(0))
                        }
                        val clampedBarIdx = selectedBarIdx.coerceIn(0, (recentWorkouts.size - 1).coerceAtLeast(0))

                        val barItems = remember(recentWorkouts, mult, language, dateFormat, s.todayShortLabel, unitStr) {
                            val xLabels = buildDistinctChartDateLabels(
                                epochs = recentWorkouts.map { it.startTimeEpochMs },
                                fallbacks = recentWorkouts.map { "" },
                                pref = dateFormat,
                                language = language,
                                todayFallback = s.todayShortLabel
                            )
                            recentWorkouts.mapIndexed { idx, w ->
                                val displayVol = w.totalVolumeKg * mult
                                val fullVol = formatIntegerChartValue(displayVol, language)
                                KumaSessionBarItem(
                                    valueDisplay = displayVol,
                                    compactValueLabel = formatCompactChartValue(displayVol, language),
                                    xLabel = xLabels[idx],
                                    detailSummary = "${xLabels[idx]} · ${w.title} · $fullVol $unitStr"
                                )
                            }
                        }

                        KumaCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = KumaColors.SurfaceContainerLow,
                            cornerRadius = 18.dp
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = s.weeklyVolumeTonnageHeader,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = s.progressRecentSessionsCount(recentWorkouts.size),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = KumaColors.TextMuted
                                            )
                                        )
                                    }
                                    Text(
                                        text = "${s.avgPrefix} $formattedAvg $unitStr",
                                        maxLines = 1,
                                        softWrap = false,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = LocalKumaColors.current.accentText,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                KumaSessionBarChart(
                                    bars = barItems,
                                    avgDisplay = avgVolDisplay,
                                    selectedIndex = clampedBarIdx,
                                    onSelectBar = { selectedBarIdx = it },
                                    language = language
                                )

                                barItems.getOrNull(clampedBarIdx)?.let { activeBar ->
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = activeBar.detailSummary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = KumaColors.TextSecondary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Muscle Volume Intelligence (Interactive Vector Body Highlighter & Landmarks)
                item {
                    KumaCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = KumaColors.SurfaceContainerLow,
                        cornerRadius = 18.dp
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = s.muscleVolumeIntelligenceTitle,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            AnatomyBodyHighlighter(statsMap = muscleVolumeStats)
                        }
                    }
                }

                // 3. Hall of Fame (Personal Records)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = s.personalRecordsHeader,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )

                        if (prs.isEmpty()) {
                            KumaCard(
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = KumaColors.SurfaceContainer,
                                cornerRadius = 16.dp
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = s.noPersonalRecordsYet,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = KumaColors.TextMuted,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            val groupedPrs = remember(prs) {
                                com.bearbones.kumalift.domain.model.FitnessHistoryOrganizer.organizePersonalRecords(prs)
                            }
                            GroupedPersonalRecordsSection(
                                groups = groupedPrs,
                                weightUnit = settings?.units?.weightUnit ?: com.bearbones.kumalift.domain.model.WeightUnit.KG,
                                language = language
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

private data class OneRmPlotPoint(
    val valueDisplay: Double,
    val valueLabel: String,
    val xLabel: String,
    val detailSummary: String
)

@Composable
private fun OneRepMaxLineChart(
    points: List<OneRmPlotPoint>,
    selectedIndex: Int,
    onSelectPoint: (Int) -> Unit,
    language: AppLanguage
) {
    if (points.isEmpty()) return
    val colors = LocalKumaColors.current
    val count = points.size
    val values = points.map { it.valueDisplay }
    val rawMin = values.minOrNull() ?: 0.0
    val rawMax = values.maxOrNull() ?: 1.0
    val span = rawMax - rawMin
    val yMin = if (span < 0.25) (rawMin * 0.85).coerceAtLeast(0.0) else (rawMin - span * 0.25).coerceAtLeast(0.0)
    val yMax = if (span < 0.25) (rawMax * 1.15).coerceAtLeast(yMin + 1.0) else (rawMax + span * 0.25).coerceAtLeast(yMin + 1.0)
    val yMid = (yMax + yMin) / 2.0

    val plotHeight = 140.dp
    val topPad = 28.dp
    val bottomPad = 14.dp
    val horizontalSlotPadding = when {
        count <= 1 -> 0.dp
        count == 2 -> 28.dp
        count == 3 -> 12.dp
        else -> 4.dp
    }

    val accentColor = if (colors.isDark) colors.accentFill else colors.accentChart
    val gridColor = colors.border.copy(alpha = 0.45f)
    val inactiveDotFill = colors.textSecondary
    val innerCutoutColor = KumaColors.SurfaceContainer

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(plotHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .width(38.dp)
                    .fillMaxHeight()
                    .padding(top = topPad - 8.dp, bottom = bottomPad - 8.dp, end = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                ChartAxisTickText(formatMetricDecimal(yMax, language))
                ChartAxisTickText(formatMetricDecimal(yMid, language))
                ChartAxisTickText(formatMetricDecimal(yMin, language))
            }

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(KumaColors.SurfaceContainer)
            ) {
                val containerHeight = this.maxHeight
                val usableHeight = (containerHeight - topPad - bottomPad).coerceAtLeast(1.dp)
                val normalizedHeights = values.map { v ->
                    ((v - yMin) / (yMax - yMin)).toFloat().coerceIn(0f, 1f)
                }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val padX = horizontalSlotPadding.toPx()
                    val topPx = topPad.toPx()
                    val bottomPx = bottomPad.toPx()
                    val usableWidthPx = (size.width - padX * 2f).coerceAtLeast(1f)
                    val usableHeightPx = (size.height - topPx - bottomPx).coerceAtLeast(1f)
                    val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))

                    listOf(0f, 0.5f, 1f).forEach { frac ->
                        val gridY = topPx + frac * usableHeightPx
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, gridY),
                            end = Offset(size.width, gridY),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = dash
                        )
                    }

                    val coords = normalizedHeights.mapIndexed { idx, norm ->
                        val cx = padX + usableWidthPx * ((idx + 0.5f) / count)
                        val cy = topPx + (1f - norm) * usableHeightPx
                        Offset(cx, cy)
                    }

                    if (coords.size >= 2) {
                        val areaPath = Path().apply {
                            moveTo(coords.first().x, size.height - bottomPx)
                            coords.forEach { pt -> lineTo(pt.x, pt.y) }
                            lineTo(coords.last().x, size.height - bottomPx)
                            close()
                        }
                        drawPath(
                            path = areaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(accentColor.copy(alpha = 0.22f), accentColor.copy(alpha = 0.0f)),
                                startY = topPx,
                                endY = size.height - bottomPx
                            )
                        )

                        val strokePath = Path().apply {
                            moveTo(coords.first().x, coords.first().y)
                            for (i in 1 until coords.size) {
                                lineTo(coords[i].x, coords[i].y)
                            }
                        }
                        drawPath(
                            path = strokePath,
                            color = accentColor,
                            style = Stroke(
                                width = 2.5.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }

                    coords.forEachIndexed { idx, center ->
                        val active = idx == selectedIndex
                        if (active) {
                            drawCircle(
                                color = accentColor.copy(alpha = 0.25f),
                                radius = 10.dp.toPx(),
                                center = center
                            )
                            drawCircle(
                                color = accentColor,
                                radius = 5.5.dp.toPx(),
                                center = center
                            )
                        } else {
                            drawCircle(
                                color = innerCutoutColor,
                                radius = 5.dp.toPx(),
                                center = center
                            )
                            drawCircle(
                                color = inactiveDotFill,
                                radius = 3.5.dp.toPx(),
                                center = center
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = horizontalSlotPadding)
                ) {
                    points.forEachIndexed { idx, pt ->
                        val active = idx == selectedIndex
                        val dotTopDp = topPad + usableHeight * (1f - normalizedHeights[idx])
                        val labelYDp = (dotTopDp - 25.dp).coerceAtLeast(3.dp)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { onSelectPoint(idx) },
                            contentAlignment = Alignment.TopCenter
                        ) {
                            if (active) {
                                Box(
                                    modifier = Modifier
                                        .offset(y = labelYDp)
                                        .clip(PillShape)
                                        .background(colors.accentFill)
                                        .border(
                                            1.dp,
                                            if (colors.isDark) Color.Transparent else colors.accentFillBorder,
                                            PillShape
                                        )
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = pt.valueLabel,
                                        maxLines = 1,
                                        softWrap = false,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.onAccentFill
                                        )
                                    )
                                }
                            } else {
                                Text(
                                    text = pt.valueLabel,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.offset(y = labelYDp + 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textSecondary
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 38.dp + horizontalSlotPadding, end = horizontalSlotPadding)
        ) {
            points.forEachIndexed { idx, pt ->
                val active = idx == selectedIndex
                Text(
                    text = pt.xLabel,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectPoint(idx) },
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        color = if (active) colors.accentText else colors.textSubtle,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                    )
                )
            }
        }
    }
}

private fun effectiveOneRmKg(pr: PersonalRecord): Double =
    if (pr.estimated1RmKg > 0.0) pr.estimated1RmKg else pr.weightKg

private fun formatMetricDecimal(value: Double, language: AppLanguage): String =
    NumberFormatUtils.formatDecimal(value, language, decimals = 1, trimTrailingZeros = true)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupedPersonalRecordsSection(
    groups: List<PrExerciseGroup>,
    weightUnit: WeightUnit,
    language: AppLanguage
) {
    val expandedState = remember { mutableStateMapOf<String, Boolean>() }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        groups.forEachIndexed { idx, group ->
            val key = group.exerciseId.ifBlank { "${group.exerciseName}_$idx" }
            val isExpanded = expandedState[key] ?: (idx == 0)
            GroupedPersonalRecordCard(
                group = group,
                isExpanded = isExpanded,
                onToggleExpand = { expandedState[key] = !isExpanded },
                weightUnit = weightUnit,
                language = language
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupedPersonalRecordCard(
    group: PrExerciseGroup,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    weightUnit: WeightUnit,
    language: AppLanguage
) {
    val s = LocalStrings.current
    val colors = LocalKumaColors.current
    val unitLabel = if (weightUnit == WeightUnit.LB) s.unitLb else s.unitKg
    val bestOneRmText = formatMetricDecimal(UnitConverters.toDisplayWeight(group.bestOneRmKg, weightUnit), language)

    KumaCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleExpand),
        backgroundColor = KumaColors.SurfaceContainer,
        cornerRadius = 16.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = group.exerciseName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        KumaBadge(
                            text = s.prRecordsCountBadge(group.totalAchievements),
                            isPr = true
                        )
                        if (group.bestOneRmKg > 0.0) {
                            Text(
                                text = s.prBestOneRmSummary("$bestOneRmText $unitLabel"),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = KumaColors.TextMuted,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) s.prCollapseDetailsDesc else s.prExpandDetailsDesc,
                    tint = colors.accentText,
                    modifier = Modifier.size(22.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    group.weightGroups.forEach { weightGroup ->
                        val weightFormatted = formatMetricDecimal(
                            UnitConverters.toDisplayWeight(weightGroup.weightKg, weightUnit),
                            language
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(KumaColors.SurfaceDim)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = s.prWeightGroupHeader("$weightFormatted $unitLabel", weightGroup.records.size),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = colors.accentText,
                                    fontWeight = FontWeight.Bold
                                )
                            )

                            weightGroup.records.forEachIndexed { recIdx, record ->
                                if (recIdx > 0) {
                                    HorizontalDivider(color = KumaColors.BorderDefault.copy(alpha = 0.4f))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${record.reps} ${s.repsAbbrev} — ${s.prRepAchievementBadge(record.reps)}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = colors.textPrimary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                        Text(
                                            text = FitnessHistoryOrganizer.formatFullDateWithYear(
                                                epochMs = record.dateAchievedEpochMs,
                                                fallback = record.dateAchieved,
                                                s = s
                                            ),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = KumaColors.TextMuted,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                    KumaBadge(text = record.prTier, isPr = true)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PersonalRecordItem(pr: PersonalRecord) {
    val s = LocalStrings.current
    val weightFormatted = formatMetricDecimal(pr.weightKg, AppLanguage.EN)
    KumaCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = KumaColors.SurfaceContainer,
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pr.exerciseName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                val e1rmPart = if (pr.estimated1RmKg > 0.0) {
                    val formatted1Rm = formatMetricDecimal(pr.estimated1RmKg, AppLanguage.EN)
                    " · ${s.estimatedOneRmLabel}: $formatted1Rm ${s.unitKg}"
                } else ""
                Text(
                    text = "$weightFormatted ${s.unitKg} × ${pr.reps} ${s.repsAbbrev}$e1rmPart",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = KumaColors.TextMuted,
                        fontSize = 13.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.wrapContentWidth()
            ) {
                KumaBadge(
                    text = pr.prTier,
                    isPr = true
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = pr.dateAchieved,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = KumaColors.TextMuted,
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
