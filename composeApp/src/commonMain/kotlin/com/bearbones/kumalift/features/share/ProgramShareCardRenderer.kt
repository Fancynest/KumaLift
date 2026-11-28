package com.bearbones.kumalift.features.share

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumalift.core.designsystem.KumaLogoMark
import com.bearbones.kumalift.core.designsystem.KumaTheme
import com.bearbones.kumalift.core.designsystem.ShareCardPalette
import com.bearbones.kumalift.core.designsystem.ShareTokens
import com.bearbones.kumalift.core.localization.LocalStrings
import com.bearbones.kumalift.domain.share.ProgramShareDayColumn
import com.bearbones.kumalift.domain.share.ProgramShareExerciseRow
import com.bearbones.kumalift.domain.share.ProgramShareRenderData
import com.bearbones.kumalift.domain.share.ProgramShareTemplateStyle
import com.bearbones.kumalift.domain.share.ShareAccentPreset
import com.bearbones.kumalift.domain.share.ShareAspectRatio
import com.bearbones.kumalift.features.library.muscleAccentColor

@Composable
fun ProgramShareCardContent(
    data: ProgramShareRenderData,
    aspectRatio: ShareAspectRatio,
    templateStyle: ProgramShareTemplateStyle,
    accentPreset: ShareAccentPreset,
    showMuscleEquipment: Boolean,
    thumbnailBitmaps: Map<String, ImageBitmap>,
    athleteTag: String = "",
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    val palette = when (templateStyle) {
        ProgramShareTemplateStyle.DARK_KUMA -> ShareTokens.darkProgramPalette(accentPreset)
        ProgramShareTemplateStyle.CLEAN_LIGHT -> ShareTokens.cleanLightProgramPalette(accentPreset)
    }
    val useTwoColumns = data.columns.size >= 2 && aspectRatio != ShareAspectRatio.STORY
    val pairRows = if (useTwoColumns) data.columns.chunked(2) else data.columns.map { listOf(it) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.background)
            .padding(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = data.headerTitle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.textPrimary
                    )
                )
                Text(
                    text = data.subtitle,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textSecondary
                    )
                )
            }
        }

        // Columns Grid (paired side-by-side in 2 columns when >=2 days)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            pairRows.forEach { rowColumns ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    rowColumns.forEach { dayColumn ->
                        ProgramDayColumnView(
                            column = dayColumn,
                            palette = palette,
                            showMuscleEquipment = showMuscleEquipment,
                            thumbnailBitmaps = thumbnailBitmaps,
                            compact = useTwoColumns,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (useTwoColumns && rowColumns.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Subtle Divider + Bottom Branding Footer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.border)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (athleteTag.isNotBlank()) {
                Text(
                    text = athleteTag,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textSecondary
                    )
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                KumaLogoMark(
                    modifier = Modifier.size(18.dp),
                    accentColor = palette.accent,
                    darkColor = palette.onAccent
                )
                Text(
                    text = s.appName,
                    style = KumaTheme.typography.brandLogo.copy(
                        fontSize = 13.sp,
                        color = palette.textPrimary
                    )
                )
            }
        }
    }
}

@Composable
private fun ProgramDayColumnView(
    column: ProgramShareDayColumn,
    palette: ShareCardPalette,
    showMuscleEquipment: Boolean,
    thumbnailBitmaps: Map<String, ImageBitmap>,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(palette.backgroundSecondary)
            .border(1.dp, palette.border, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = column.title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = if (compact) 12.sp else 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = palette.textPrimary
            )
        )

        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            column.exercises.forEach { row ->
                ProgramExerciseRowView(
                    row = row,
                    palette = palette,
                    showMuscleEquipment = showMuscleEquipment,
                    thumbnailBitmap = row.thumbnailPath?.let { thumbnailBitmaps[it] },
                    compact = compact
                )
            }
        }
    }
}

@Composable
private fun ProgramExerciseRowView(
    row: ProgramShareExerciseRow,
    palette: ShareCardPalette,
    showMuscleEquipment: Boolean,
    thumbnailBitmap: ImageBitmap?,
    compact: Boolean
) {
    val thumbSize = if (compact) 28.dp else 34.dp
    val muscleColor = muscleAccentColor(row.primaryMuscle)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(thumbSize)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, muscleColor.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (thumbnailBitmap != null) {
                Image(
                    bitmap = thumbnailBitmap,
                    contentDescription = row.exerciseName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = muscleColor,
                    modifier = Modifier.size(thumbSize * 0.5f)
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = row.exerciseName,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = if (compact) 10.sp else 11.5.sp,
                    lineHeight = if (compact) 12.sp else 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.accent
                )
            )
            Text(
                text = row.setsRepsLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = if (compact) 9.sp else 10.sp,
                    color = palette.textSecondary
                )
            )
            if (showMuscleEquipment) {
                Text(
                    text = "${row.muscleLabel} · ${row.equipmentLabel}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.sp,
                        color = palette.textSecondary.copy(alpha = 0.85f)
                    )
                )
            }
            row.targetDetailsLabel?.let { details ->
                Text(
                    text = details,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textPrimary.copy(alpha = 0.9f)
                    )
                )
            }
        }
    }
}
