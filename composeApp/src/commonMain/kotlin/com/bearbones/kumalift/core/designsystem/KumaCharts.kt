package com.bearbones.kumalift.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumalift.core.util.NumberFormatUtils
import com.bearbones.kumalift.core.util.resolveIsEnglish
import com.bearbones.kumalift.domain.model.AppLanguage
import com.bearbones.kumalift.domain.model.DateFormatPreference
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToLong

data class KumaSessionBarItem(
    val valueDisplay: Double,
    val compactValueLabel: String,
    val xLabel: String,
    val detailSummary: String
)

@Composable
fun KumaSessionBarChart(
    bars: List<KumaSessionBarItem>,
    avgDisplay: Double,
    selectedIndex: Int,
    onSelectBar: (Int) -> Unit,
    language: AppLanguage
) {
    if (bars.isEmpty()) return
    val colors = LocalKumaColors.current
    val count = bars.size
    val maxVal = (bars.maxOfOrNull { it.valueDisplay } ?: 1.0).coerceAtLeast(1.0)
    val yMax = maxVal * 1.2
    val avgFraction = (avgDisplay / yMax).toFloat().coerceIn(0f, 1f)

    val plotHeight = 140.dp
    val topLabelArea = 24.dp
    val bottomBaselinePad = 6.dp
    val horizontalSlotPadding = when {
        count <= 1 -> 0.dp
        count == 2 -> 28.dp
        count == 3 -> 12.dp
        else -> 4.dp
    }

    val accentColor = if (colors.isDark) colors.accentFill else colors.accentChart
    val gridColor = colors.border.copy(alpha = 0.4f)
    val avgLineColor = colors.accentText.copy(alpha = 0.6f)
    val zeroLabel = NumberFormatUtils.formatIntegerWithSeparators(0L, language)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(plotHeight),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(
                modifier = Modifier
                    .width(42.dp)
                    .fillMaxHeight()
                    .padding(top = topLabelArea - 8.dp, bottom = bottomBaselinePad - 2.dp, end = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                ChartAxisTickText(formatCompactChartValue(yMax, language))
                ChartAxisTickText(formatCompactChartValue(yMax / 2.0, language))
                ChartAxisTickText(zeroLabel)
            }

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(KumaColors.SurfaceContainer)
                    .padding(bottom = bottomBaselinePad)
            ) {
                val barPlotHeight = (this.maxHeight - topLabelArea - bottomBaselinePad).coerceAtLeast(1.dp)

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val topPx = topLabelArea.toPx()
                    val usableHeightPx = (size.height - topPx).coerceAtLeast(1f)
                    val gridDash = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))

                    listOf(0f, 0.5f, 1f).forEach { frac ->
                        val gy = topPx + frac * usableHeightPx
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, gy),
                            end = Offset(size.width, gy),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = if (frac < 1f) gridDash else null
                        )
                    }

                    val avgY = topPx + (1f - avgFraction) * usableHeightPx
                    drawLine(
                        color = avgLineColor,
                        start = Offset(0f, avgY),
                        end = Offset(size.width, avgY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = horizontalSlotPadding),
                    verticalAlignment = Alignment.Bottom
                ) {
                    bars.forEachIndexed { idx, item ->
                        val active = idx == selectedIndex
                        val heightFraction = (item.valueDisplay / yMax).toFloat().coerceIn(0.06f, 1f)
                        val barDp = (barPlotHeight * heightFraction).coerceAtLeast(6.dp)

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { onSelectBar(idx) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Text(
                                text = item.compactValueLabel,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (active) colors.accentText else colors.textSecondary
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(26.dp)
                                    .height(barDp)
                                    .clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp))
                                    .background(if (active) accentColor else colors.surfaceBright)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 42.dp + horizontalSlotPadding, end = horizontalSlotPadding)
        ) {
            bars.forEachIndexed { idx, item ->
                val active = idx == selectedIndex
                Text(
                    text = item.xLabel,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectBar(idx) },
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

@Composable
fun ChartAxisTickText(label: String) {
    Text(
        text = label,
        maxLines = 1,
        softWrap = false,
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 9.sp,
            color = KumaColors.TextMuted
        )
    )
}

fun formatCompactChartValue(value: Double, language: AppLanguage): String {
    return if (value >= 10_000.0) {
        NumberFormatUtils.formatDecimal(value / 1000.0, language, decimals = 1, trimTrailingZeros = true) + "k"
    } else {
        NumberFormatUtils.formatIntegerWithSeparators(value.roundToLong(), language)
    }
}

fun formatIntegerChartValue(value: Double, language: AppLanguage): String =
    NumberFormatUtils.formatIntegerWithSeparators(value.roundToLong(), language)

fun buildDistinctChartDateLabels(
    epochs: List<Long>,
    fallbacks: List<String>,
    pref: DateFormatPreference,
    language: AppLanguage,
    todayFallback: String
): List<String> {
    val baseDates = epochs.mapIndexed { idx, epoch ->
        if (epoch <= 0L) {
            fallbacks.getOrNull(idx)?.trim()?.takeIf { it.isNotBlank() }?.take(6) ?: todayFallback
        } else {
            val ldt = Instant.fromEpochMilliseconds(epoch).toLocalDateTime(TimeZone.currentSystemDefault())
            val d = ldt.dayOfMonth.toString().padStart(2, '0')
            val m = ldt.monthNumber.toString().padStart(2, '0')
            val resolved = if (pref == DateFormatPreference.SYSTEM) {
                if (resolveIsEnglish(language)) DateFormatPreference.MDY else DateFormatPreference.DMY
            } else {
                pref
            }
            when (resolved) {
                DateFormatPreference.MDY -> "$m/$d"
                DateFormatPreference.YMD -> "$m-$d"
                DateFormatPreference.DMY, DateFormatPreference.SYSTEM -> "$d/$m"
            }
        }
    }

    val counts = baseDates.groupingBy { it }.eachCount()
    val seenPerDate = mutableMapOf<String, Int>()
    return baseDates.mapIndexed { idx, dateStr ->
        if ((counts[dateStr] ?: 0) <= 1) {
            dateStr
        } else {
            val epoch = epochs[idx]
            if (epoch > 0L) {
                val ldt = Instant.fromEpochMilliseconds(epoch).toLocalDateTime(TimeZone.currentSystemDefault())
                val hh = ldt.hour.toString().padStart(2, '0')
                val mm = ldt.minute.toString().padStart(2, '0')
                "$hh:$mm"
            } else {
                val n = (seenPerDate[dateStr] ?: 0) + 1
                seenPerDate[dateStr] = n
                "$dateStr #$n"
            }
        }
    }
}
