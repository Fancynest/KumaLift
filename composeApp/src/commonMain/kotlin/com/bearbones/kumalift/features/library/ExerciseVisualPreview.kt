package com.bearbones.kumalift.features.library

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumalift.domain.model.Equipment
import com.bearbones.kumalift.domain.model.Exercise
import com.bearbones.kumalift.domain.model.MuscleGroup

/**
 * Circular thumbnail for exercise list rows — dark background with muscle-group icon.
 */
@Composable
fun ExerciseThumbnail(
    exercise: Exercise,
    modifier: Modifier = Modifier,
    size: Dp = 50.dp
) {
    val muscleColor = muscleAccentColor(exercise.primaryMuscle)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF1C1F26))
            .border(1.5.dp, muscleColor.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = muscleEmoji(exercise.primaryMuscle),
            fontSize = (size.value * 0.42f).sp
        )
    }
}

/**
 * Large animated visual for the exercise detail screen.
 * Dark background with muscle-map silhouette + animated exercise motion.
 */
@Composable
fun ExerciseVisualAnimation(
    exercise: Exercise,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(true) }

    val infiniteTransition = rememberInfiniteTransition()
    val animatedPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    // Subtle glow pulse on the highlighted muscle
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val currentPhase = if (isPlaying) animatedPhase else 0.5f
    val currentGlow = if (isPlaying) glowAlpha else 0.75f
    val muscleColor = muscleAccentColor(exercise.primaryMuscle)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D0F14), Color(0xFF161A22))
                )
            )
            .clickable { isPlaying = !isPlaying },
        contentAlignment = Alignment.Center
    ) {
        // Main anatomical canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            drawAnatomicalFigure(
                exercise = exercise,
                phase = currentPhase,
                glowAlpha = currentGlow,
                accentColor = muscleColor,
                bodyColor = Color(0xFF2E3440),
                outlineColor = Color(0xFF4C566A),
                apparatusColor = Color(0xFF5E6B7A)
            )
        }

        // Muscle label badge — bottom-left
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1C2028).copy(alpha = 0.9f))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(muscleColor)
            )
            Text(
                text = exercise.primaryMuscle.displayName,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = Color(0xFFCDD5E0),
                    fontWeight = FontWeight.Medium
                )
            )
        }

        // Equipment badge — bottom-right
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(14.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1C2028).copy(alpha = 0.9f))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = Color(0xFF8899AA),
                modifier = Modifier.size(10.dp)
            )
            Text(
                text = exercise.equipment.displayName,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = Color(0xFF8899AA)
                )
            )
        }

        // Play / Pause button — top-right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(14.dp)
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0xFF1C2028).copy(alpha = 0.90f))
                .border(1.dp, Color(0xFF3A4455), CircleShape)
                .clickable { isPlaying = !isPlaying },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color(0xFFCDD5E0),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** Returns a vivid accent colour for the given muscle group */
private fun muscleAccentColor(muscle: MuscleGroup): Color = when (muscle) {
    MuscleGroup.CHEST     -> Color(0xFFFF6B6B)
    MuscleGroup.BACK      -> Color(0xFF4ECDC4)
    MuscleGroup.LEGS      -> Color(0xFFFFE66D)
    MuscleGroup.SHOULDERS -> Color(0xFF6BCB77)
    MuscleGroup.ARMS      -> Color(0xFF4D96FF)
    MuscleGroup.CORE      -> Color(0xFFFF9F43)
    else                  -> Color(0xFFBDBDBD)
}

/** Returns a representative emoji icon for quick recognition */
private fun muscleEmoji(muscle: MuscleGroup): String = when (muscle) {
    MuscleGroup.CHEST     -> "💪"
    MuscleGroup.BACK      -> "🏋️"
    MuscleGroup.LEGS      -> "🦵"
    MuscleGroup.SHOULDERS -> "🤸"
    MuscleGroup.ARMS      -> "💪"
    MuscleGroup.CORE      -> "🔥"
    else                  -> "🏃"
}

/**
 * High-fidelity anatomical figure drawn entirely on a dark canvas.
 * Uses filled paths for body segments + a glowing highlight for the target muscle group.
 */
private fun DrawScope.drawAnatomicalFigure(
    exercise: Exercise,
    phase: Float,          // 0..1 animation phase
    glowAlpha: Float,      // 0..1 pulsing alpha for the muscle highlight
    accentColor: Color,    // muscle group highlight colour
    bodyColor: Color,      // base body fill
    outlineColor: Color,   // body segment outline
    apparatusColor: Color  // barbell / dumbbell / cable colour
) {
    val w = size.width
    val h = size.height
    val cx = w / 2f

    when (exercise.primaryMuscle) {
        MuscleGroup.CHEST -> drawBenchPress(cx, w, h, phase, glowAlpha, accentColor, bodyColor, outlineColor, apparatusColor, exercise.equipment == Equipment.DUMBBELL)
        MuscleGroup.BACK  -> drawDeadlift(cx, w, h, phase, glowAlpha, accentColor, bodyColor, outlineColor, apparatusColor)
        MuscleGroup.LEGS  -> drawSquat(cx, w, h, phase, glowAlpha, accentColor, bodyColor, outlineColor, apparatusColor)
        MuscleGroup.SHOULDERS -> drawOverheadPress(cx, w, h, phase, glowAlpha, accentColor, bodyColor, outlineColor, apparatusColor)
        MuscleGroup.ARMS  -> drawCurl(cx, w, h, phase, glowAlpha, accentColor, bodyColor, outlineColor, apparatusColor)
        else              -> drawGenericFigure(cx, w, h, phase, glowAlpha, accentColor, bodyColor, outlineColor)
    }
}

// ─── Bench Press ──────────────────────────────────────────────────────────────
private fun DrawScope.drawBenchPress(
    cx: Float, w: Float, h: Float, phase: Float, glow: Float,
    accent: Color, body: Color, outline: Color, apparatus: Color, isDumbbell: Boolean
) {
    val benchY = h * 0.68f
    val headX  = cx - w * 0.26f
    val headY  = benchY - 20f

    // Bench (thick rounded rect)
    drawRoundRect(
        color = apparatus.copy(alpha = 0.6f),
        topLeft = Offset(cx - w * 0.38f, benchY - 8f),
        size = Size(w * 0.76f, 16f),
        cornerRadius = CornerRadius(8f)
    )
    // Bench legs
    drawLine(apparatus.copy(alpha = 0.5f), Offset(cx - w * 0.28f, benchY + 8f), Offset(cx - w * 0.28f, benchY + h * 0.20f), 6f, cap = StrokeCap.Round)
    drawLine(apparatus.copy(alpha = 0.5f), Offset(cx + w * 0.22f, benchY + 8f), Offset(cx + w * 0.22f, benchY + h * 0.20f), 6f, cap = StrokeCap.Round)

    // Athlete body on bench (torso as rounded rect)
    drawRoundRect(
        color = body,
        topLeft = Offset(headX + 10f, benchY - 32f),
        size = Size(w * 0.42f, 26f),
        cornerRadius = CornerRadius(12f)
    )
    drawRoundRect(
        color = outline,
        topLeft = Offset(headX + 10f, benchY - 32f),
        size = Size(w * 0.42f, 26f),
        cornerRadius = CornerRadius(12f),
        style = Stroke(width = 2f)
    )

    // Head
    drawCircle(body,    radius = 18f, center = Offset(headX, headY))
    drawCircle(outline, radius = 18f, center = Offset(headX, headY), style = Stroke(2f))

    // Pectorals highlight (glow)
    val chestX = cx - w * 0.06f
    val chestY = benchY - 20f
    drawCircle(accent.copy(alpha = glow * 0.35f), radius = 28f, center = Offset(chestX, chestY))
    drawCircle(accent.copy(alpha = glow),         radius = 12f, center = Offset(chestX, chestY))

    // Barbell animation: phase=0 → bar on chest, phase=1 → bar at lockout
    val barY = benchY - 24f - phase * h * 0.30f
    val shoulderX = chestX

    // Upper arms
    drawLine(body, Offset(shoulderX - 10f, chestY - 4f), Offset(cx - w * 0.18f, barY + 6f), 14f, cap = StrokeCap.Round)
    drawLine(body, Offset(shoulderX + 6f,  chestY - 4f), Offset(cx + w * 0.08f, barY + 6f), 14f, cap = StrokeCap.Round)

    if (isDumbbell) {
        drawDumbbell(apparatus, Offset(cx - w * 0.24f, barY - 10f))
        drawDumbbell(apparatus, Offset(cx + w * 0.08f, barY - 10f))
    } else {
        drawBarbell(apparatus, Offset(cx - w * 0.44f, barY), Offset(cx + w * 0.34f, barY))
    }
}

// ─── Deadlift / Back ──────────────────────────────────────────────────────────
private fun DrawScope.drawDeadlift(
    cx: Float, w: Float, h: Float, phase: Float, glow: Float,
    accent: Color, body: Color, outline: Color, apparatus: Color
) {
    val hipY   = h * 0.52f - phase * h * 0.18f
    val barFloorY = h * 0.82f - phase * h * 0.42f
    val headTilt  = (1f - phase) * w * 0.06f
    val headY     = hipY - h * 0.28f

    // Torso path: leans forward at phase=0, upright at phase=1
    val shoulderX = cx - headTilt * 0.5f
    val shoulderY = hipY - h * 0.22f

    drawBodySegment(body, outline,
        Offset(shoulderX, shoulderY), Offset(cx, hipY), segWidth = 28f)

    // Head
    drawCircle(body,    radius = 20f, center = Offset(cx - headTilt, headY))
    drawCircle(outline, radius = 20f, center = Offset(cx - headTilt, headY), style = Stroke(2f))

    // Lats highlight
    drawCircle(accent.copy(alpha = glow * 0.3f), radius = 32f, center = Offset(shoulderX, shoulderY + 14f))
    drawCircle(accent.copy(alpha = glow),         radius = 14f, center = Offset(shoulderX, shoulderY + 14f))

    // Arms to barbell
    drawLine(body, Offset(shoulderX, shoulderY + 10f), Offset(cx - w * 0.06f, barFloorY), 13f, cap = StrokeCap.Round)

    // Legs
    drawLine(body, Offset(cx, hipY), Offset(cx - w * 0.10f, h * 0.84f), 15f, cap = StrokeCap.Round)
    drawLine(body, Offset(cx, hipY), Offset(cx + w * 0.10f, h * 0.84f), 15f, cap = StrokeCap.Round)

    // Barbell on floor
    drawBarbell(apparatus, Offset(cx - w * 0.44f, barFloorY), Offset(cx + w * 0.32f, barFloorY))
}

// ─── Squat / Legs ─────────────────────────────────────────────────────────────
private fun DrawScope.drawSquat(
    cx: Float, w: Float, h: Float, phase: Float, glow: Float,
    accent: Color, body: Color, outline: Color, apparatus: Color
) {
    val depth = (1f - phase) * h * 0.20f
    val hipY  = h * 0.52f + depth
    val headY = hipY - h * 0.34f

    // Head
    drawCircle(body,    radius = 20f, center = Offset(cx, headY))
    drawCircle(outline, radius = 20f, center = Offset(cx, headY), style = Stroke(2f))

    // Torso
    drawBodySegment(body, outline, Offset(cx, headY + 20f), Offset(cx, hipY), segWidth = 30f)

    // Barbell on shoulders
    drawBarbell(apparatus, Offset(cx - w * 0.36f, headY + 10f), Offset(cx + w * 0.36f, headY + 10f))

    // Quad / Glute highlight
    val quadY = hipY + h * 0.06f
    drawCircle(accent.copy(alpha = glow * 0.3f), radius = 30f, center = Offset(cx - w * 0.09f, quadY))
    drawCircle(accent.copy(alpha = glow * 0.3f), radius = 30f, center = Offset(cx + w * 0.09f, quadY))
    drawCircle(accent.copy(alpha = glow),         radius = 12f, center = Offset(cx - w * 0.09f, quadY))
    drawCircle(accent.copy(alpha = glow),         radius = 12f, center = Offset(cx + w * 0.09f, quadY))

    val kneeY  = hipY + h * 0.22f
    val footY  = h * 0.90f

    // Upper legs
    drawLine(body, Offset(cx - w * 0.06f, hipY), Offset(cx - w * 0.16f, kneeY), 16f, cap = StrokeCap.Round)
    drawLine(body, Offset(cx + w * 0.06f, hipY), Offset(cx + w * 0.16f, kneeY), 16f, cap = StrokeCap.Round)
    // Lower legs
    drawLine(body, Offset(cx - w * 0.16f, kneeY), Offset(cx - w * 0.14f, footY), 13f, cap = StrokeCap.Round)
    drawLine(body, Offset(cx + w * 0.16f, kneeY), Offset(cx + w * 0.14f, footY), 13f, cap = StrokeCap.Round)
}

// ─── Overhead Press / Shoulders ───────────────────────────────────────────────
private fun DrawScope.drawOverheadPress(
    cx: Float, w: Float, h: Float, phase: Float, glow: Float,
    accent: Color, body: Color, outline: Color, apparatus: Color
) {
    val headY      = h * 0.26f
    val shoulderY  = headY + 28f
    val hipY       = h * 0.60f
    val barY       = shoulderY - 10f - phase * h * 0.28f

    drawCircle(body,    radius = 20f, center = Offset(cx, headY))
    drawCircle(outline, radius = 20f, center = Offset(cx, headY), style = Stroke(2f))

    drawBodySegment(body, outline, Offset(cx, shoulderY), Offset(cx, hipY), segWidth = 28f)

    // Legs
    drawLine(body, Offset(cx, hipY), Offset(cx - w * 0.09f, h * 0.88f), 14f, cap = StrokeCap.Round)
    drawLine(body, Offset(cx, hipY), Offset(cx + w * 0.09f, h * 0.88f), 14f, cap = StrokeCap.Round)

    // Deltoid highlight
    drawCircle(accent.copy(alpha = glow * 0.3f), radius = 26f, center = Offset(cx - 22f, shoulderY))
    drawCircle(accent.copy(alpha = glow * 0.3f), radius = 26f, center = Offset(cx + 22f, shoulderY))
    drawCircle(accent.copy(alpha = glow),         radius = 11f, center = Offset(cx - 22f, shoulderY))
    drawCircle(accent.copy(alpha = glow),         radius = 11f, center = Offset(cx + 22f, shoulderY))

    // Arms lifting
    drawLine(body, Offset(cx - 22f, shoulderY), Offset(cx - w * 0.19f, barY + 8f), 13f, cap = StrokeCap.Round)
    drawLine(body, Offset(cx + 22f, shoulderY), Offset(cx + w * 0.19f, barY + 8f), 13f, cap = StrokeCap.Round)

    drawBarbell(apparatus, Offset(cx - w * 0.38f, barY), Offset(cx + w * 0.38f, barY))
}

// ─── Bicep Curl / Arms ────────────────────────────────────────────────────────
private fun DrawScope.drawCurl(
    cx: Float, w: Float, h: Float, phase: Float, glow: Float,
    accent: Color, body: Color, outline: Color, apparatus: Color
) {
    val headY    = h * 0.18f
    val shoulderY = headY + 28f
    val hipY     = h * 0.58f
    val elbowX   = cx + w * 0.14f
    val elbowY   = h * 0.50f
    val handY    = elbowY - phase * h * 0.22f
    val handX    = elbowX - phase * w * 0.06f

    drawCircle(body,    radius = 20f, center = Offset(cx, headY))
    drawCircle(outline, radius = 20f, center = Offset(cx, headY), style = Stroke(2f))
    drawBodySegment(body, outline, Offset(cx, shoulderY), Offset(cx, hipY), segWidth = 28f)
    drawLine(body, Offset(cx, hipY), Offset(cx - w * 0.08f, h * 0.88f), 14f, cap = StrokeCap.Round)
    drawLine(body, Offset(cx, hipY), Offset(cx + w * 0.08f, h * 0.88f), 14f, cap = StrokeCap.Round)

    // Upper arm
    drawLine(body, Offset(cx + 12f, shoulderY), Offset(elbowX, elbowY), 14f, cap = StrokeCap.Round)
    // Bicep glow
    val midArmY = (shoulderY + elbowY) / 2f
    drawCircle(accent.copy(alpha = glow * 0.3f), radius = 20f, center = Offset(elbowX - 4f, midArmY))
    drawCircle(accent.copy(alpha = glow),         radius =  9f, center = Offset(elbowX - 4f, midArmY))
    // Forearm
    drawLine(body, Offset(elbowX, elbowY), Offset(handX, handY), 12f, cap = StrokeCap.Round)
    // Dumbbell
    drawDumbbell(apparatus, Offset(handX - 10f, handY - 14f))
}

// ─── Generic / Core ───────────────────────────────────────────────────────────
private fun DrawScope.drawGenericFigure(
    cx: Float, w: Float, h: Float, phase: Float, glow: Float,
    accent: Color, body: Color, outline: Color
) {
    val headY = h * 0.18f
    val hipY  = h * 0.56f

    drawCircle(body,    radius = 20f, center = Offset(cx, headY))
    drawCircle(outline, radius = 20f, center = Offset(cx, headY), style = Stroke(2f))
    drawBodySegment(body, outline, Offset(cx, headY + 20f), Offset(cx, hipY), segWidth = 28f)

    // Core highlight
    val coreY = (headY + hipY) / 2f + 10f
    drawCircle(accent.copy(alpha = glow * 0.3f), radius = 30f, center = Offset(cx, coreY))
    drawCircle(accent.copy(alpha = glow),         radius = 12f, center = Offset(cx, coreY))

    // Arms
    drawLine(body, Offset(cx, headY + 30f), Offset(cx - w * 0.26f, h * 0.44f + phase * 8f), 13f, cap = StrokeCap.Round)
    drawLine(body, Offset(cx, headY + 30f), Offset(cx + w * 0.26f, h * 0.44f - phase * 8f), 13f, cap = StrokeCap.Round)
    // Legs
    drawLine(body, Offset(cx, hipY), Offset(cx - w * 0.10f, h * 0.88f), 14f, cap = StrokeCap.Round)
    drawLine(body, Offset(cx, hipY), Offset(cx + w * 0.10f, h * 0.88f), 14f, cap = StrokeCap.Round)
}

// ─── Shared drawing helpers ───────────────────────────────────────────────────

/** Draws a filled + outlined rounded body segment (thick limb/torso). */
private fun DrawScope.drawBodySegment(
    fill: Color, outline: Color, from: Offset, to: Offset, segWidth: Float
) {
    // Draw slightly wider outline first, then fill on top
    drawLine(outline, from, to, segWidth + 4f, cap = StrokeCap.Round)
    drawLine(fill,    from, to, segWidth,       cap = StrokeCap.Round)
}

/** Draws a classic barbell between two x positions at the same y. */
private fun DrawScope.drawBarbell(color: Color, left: Offset, right: Offset) {
    // Bar shaft
    drawLine(color, left, right, strokeWidth = 6f, cap = StrokeCap.Round)
    // Left plate
    drawRoundRect(color.copy(alpha = 0.9f),
        topLeft = Offset(left.x, left.y - 22f), size = Size(12f, 44f),
        cornerRadius = CornerRadius(4f))
    // Right plate
    drawRoundRect(color.copy(alpha = 0.9f),
        topLeft = Offset(right.x - 12f, right.y - 22f), size = Size(12f, 44f),
        cornerRadius = CornerRadius(4f))
}

/** Draws a single dumbbell centred at the given offset. */
private fun DrawScope.drawDumbbell(color: Color, at: Offset) {
    // Handle
    drawLine(color, Offset(at.x + 4f, at.y + 12f), Offset(at.x + 4f, at.y + 28f),
        strokeWidth = 7f, cap = StrokeCap.Round)
    // Top plate
    drawRoundRect(color.copy(alpha = 0.85f),
        topLeft = Offset(at.x, at.y + 6f), size = Size(16f, 10f),
        cornerRadius = CornerRadius(3f))
    // Bottom plate
    drawRoundRect(color.copy(alpha = 0.85f),
        topLeft = Offset(at.x, at.y + 24f), size = Size(16f, 10f),
        cornerRadius = CornerRadius(3f))
}
