package com.bearbones.kumalift.features.library

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.domain.model.Equipment
import com.bearbones.kumalift.domain.model.Exercise
import com.bearbones.kumalift.domain.model.MuscleGroup

/**
 * 180x180 Thumbnail illustration badge for exercise list rows matching exercises-dataset
 */
@Composable
fun ExerciseThumbnail(
    exercise: Exercise,
    modifier: Modifier = Modifier,
    size: Dp = 50.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White)
            .border(1.dp, Color(0xFFE5E7EB), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.72f)) {
            drawExerciseSchematic(
                exercise = exercise,
                phase = 0.5f,
                accentColor = Color(0xFFF97316), // Anatomical muscle highlight
                charcoal = Color(0xFF1F2937),
                apparatusColor = Color(0xFF4B5563)
            )
        }
    }
}

/**
 * Large 240dp visual animated preview on top of the exercise detail screen matching Summary & How To
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

    val currentPhase = if (isPlaying) animatedPhase else 0.5f
    val colors = LocalKumaColors.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(Color.White)
            .clickable { isPlaying = !isPlaying },
        contentAlignment = Alignment.Center
    ) {
        // High-fidelity anatomical workout animation canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            drawExerciseSchematic(
                exercise = exercise,
                phase = currentPhase,
                accentColor = Color(0xFFEF4444), // Highlighted target muscle contraction
                charcoal = Color(0xFF111827),
                apparatusColor = Color(0xFF374151)
            )
        }

        // Top-right Pause / Play control pill
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF1F2937).copy(alpha = 0.85f))
                .clickable { isPlaying = !isPlaying },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause animation" else "Play animation",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        // Attribution watermark at bottom-left
        Text(
            text = "exercises-dataset · ${exercise.equipment.displayName}",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                color = Color(0xFF9CA3AF)
            ),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        )
    }
}

/**
 * Procedural anatomical vector drawing based on muscle group & equipment
 */
private fun DrawScope.drawExerciseSchematic(
    exercise: Exercise,
    phase: Float,
    accentColor: Color,
    charcoal: Color,
    apparatusColor: Color
) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f

    when (exercise.primaryMuscle) {
        MuscleGroup.CHEST -> {
            // Bench Press / Push apparatus & figure
            val benchTopY = cy + h * 0.15f
            // Bench line
            drawLine(
                color = apparatusColor,
                start = Offset(cx - w * 0.4f, benchTopY),
                end = Offset(cx + w * 0.4f, benchTopY),
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )
            // Bench legs
            drawLine(apparatusColor, Offset(cx - w * 0.25f, benchTopY), Offset(cx - w * 0.25f, benchTopY + h * 0.25f), 5f)
            drawLine(apparatusColor, Offset(cx + w * 0.25f, benchTopY), Offset(cx + w * 0.25f, benchTopY + h * 0.25f), 5f)

            // Athlete torso on bench
            val headX = cx - w * 0.22f
            val headY = benchTopY - 14f
            drawCircle(charcoal, radius = 12f, center = Offset(headX, headY))

            // Body line
            drawLine(charcoal, Offset(headX, headY), Offset(cx + w * 0.2f, benchTopY), strokeWidth = 14f, cap = StrokeCap.Round)

            // Target chest muscle (red/orange highlight)
            drawCircle(accentColor, radius = 9f, center = Offset(cx - w * 0.05f, benchTopY - 8f))

            // Arms & Barbell moving along phase (0f = chest touch, 1f = lockout)
            val barY = (benchTopY - 18f) - (phase * h * 0.28f)
            // Left & Right Arms
            drawLine(charcoal, Offset(cx - w * 0.05f, benchTopY - 6f), Offset(cx - w * 0.15f, barY), strokeWidth = 6f, cap = StrokeCap.Round)
            drawLine(charcoal, Offset(cx - w * 0.05f, benchTopY - 6f), Offset(cx + w * 0.05f, barY), strokeWidth = 6f, cap = StrokeCap.Round)

            if (exercise.equipment == Equipment.DUMBBELL) {
                // Two dumbbells
                drawRoundRect(apparatusColor, Offset(cx - w * 0.22f, barY - 12f), Size(18f, 24f), CornerRadius(4f))
                drawRoundRect(apparatusColor, Offset(cx + w * 0.04f, barY - 12f), Size(18f, 24f), CornerRadius(4f))
            } else {
                // Barbell bar
                drawLine(apparatusColor, Offset(cx - w * 0.42f, barY), Offset(cx + w * 0.32f, barY), strokeWidth = 5f, cap = StrokeCap.Round)
                // Plates on bar
                drawRoundRect(apparatusColor, Offset(cx - w * 0.40f, barY - 18f), Size(10f, 36f), CornerRadius(3f))
                drawRoundRect(apparatusColor, Offset(cx + w * 0.28f, barY - 18f), Size(10f, 36f), CornerRadius(3f))
            }
        }

        MuscleGroup.LEGS -> {
            // Squat / Leg Press motion
            val squatDepth = (1f - phase) * h * 0.22f
            val hipY = cy - h * 0.05f + squatDepth
            val kneeY = cy + h * 0.22f

            // Torso & Head
            val headY = hipY - h * 0.25f
            drawCircle(charcoal, radius = 13f, center = Offset(cx, headY))
            drawLine(charcoal, Offset(cx, headY), Offset(cx, hipY), strokeWidth = 14f, cap = StrokeCap.Round)

            // Barbell on back/shoulders
            val barY = headY + 8f
            drawLine(apparatusColor, Offset(cx - w * 0.35f, barY), Offset(cx + w * 0.35f, barY), strokeWidth = 6f, cap = StrokeCap.Round)
            drawRoundRect(apparatusColor, Offset(cx - w * 0.35f, barY - 20f), Size(12f, 40f), CornerRadius(3f))
            drawRoundRect(apparatusColor, Offset(cx + w * 0.25f, barY - 20f), Size(12f, 40f), CornerRadius(3f))

            // Highlight Quad / Glute muscle
            drawCircle(accentColor, radius = 10f, center = Offset(cx - w * 0.08f, hipY + 12f))
            drawCircle(accentColor, radius = 10f, center = Offset(cx + w * 0.08f, hipY + 12f))

            // Thighs & Lower legs
            drawLine(charcoal, Offset(cx, hipY), Offset(cx - w * 0.18f, kneeY), strokeWidth = 9f, cap = StrokeCap.Round)
            drawLine(charcoal, Offset(cx, hipY), Offset(cx + w * 0.18f, kneeY), strokeWidth = 9f, cap = StrokeCap.Round)
            drawLine(charcoal, Offset(cx - w * 0.18f, kneeY), Offset(cx - w * 0.20f, cy + h * 0.42f), strokeWidth = 8f, cap = StrokeCap.Round)
            drawLine(charcoal, Offset(cx + w * 0.18f, kneeY), Offset(cx + w * 0.20f, cy + h * 0.42f), strokeWidth = 8f, cap = StrokeCap.Round)
        }

        MuscleGroup.BACK -> {
            // Deadlift / Row motion
            val barY = cy + h * 0.35f - (phase * h * 0.35f)
            val torsoAngle = 0.4f * (1f - phase)
            val headX = cx - w * 0.1f * (1f - phase)
            val headY = cy - h * 0.25f + (1f - phase) * h * 0.12f

            drawCircle(charcoal, radius = 13f, center = Offset(headX, headY))
            drawLine(charcoal, Offset(headX, headY), Offset(cx, cy + h * 0.05f), strokeWidth = 14f, cap = StrokeCap.Round)

            // Lats / Upper back muscle highlight
            drawCircle(accentColor, radius = 11f, center = Offset(headX + 4f, headY + 22f))

            // Arms holding barbell
            drawLine(charcoal, Offset(headX + 6f, headY + 14f), Offset(cx, barY), strokeWidth = 7f, cap = StrokeCap.Round)

            // Barbell
            drawLine(apparatusColor, Offset(cx - w * 0.36f, barY), Offset(cx + w * 0.36f, barY), strokeWidth = 6f, cap = StrokeCap.Round)
            drawRoundRect(apparatusColor, Offset(cx - w * 0.36f, barY - 22f), Size(12f, 44f), CornerRadius(3f))
            drawRoundRect(apparatusColor, Offset(cx + w * 0.26f, barY - 22f), Size(12f, 44f), CornerRadius(3f))

            // Legs
            drawLine(charcoal, Offset(cx, cy + h * 0.05f), Offset(cx - w * 0.1f, cy + h * 0.42f), strokeWidth = 8f, cap = StrokeCap.Round)
            drawLine(charcoal, Offset(cx, cy + h * 0.05f), Offset(cx + w * 0.1f, cy + h * 0.42f), strokeWidth = 8f, cap = StrokeCap.Round)
        }

        MuscleGroup.SHOULDERS -> {
            // Overhead Press
            val headY = cy - h * 0.08f
            drawCircle(charcoal, radius = 13f, center = Offset(cx, headY))
            drawLine(charcoal, Offset(cx, headY), Offset(cx, cy + h * 0.25f), strokeWidth = 14f, cap = StrokeCap.Round)

            // Deltoids muscle highlight
            drawCircle(accentColor, radius = 9f, center = Offset(cx - 16f, headY + 14f))
            drawCircle(accentColor, radius = 9f, center = Offset(cx + 16f, headY + 14f))

            // Pressing bar overhead
            val barY = headY - 10f - (phase * h * 0.25f)
            drawLine(charcoal, Offset(cx - 16f, headY + 14f), Offset(cx - w * 0.18f, barY), strokeWidth = 6f, cap = StrokeCap.Round)
            drawLine(charcoal, Offset(cx + 16f, headY + 14f), Offset(cx + w * 0.18f, barY), strokeWidth = 6f, cap = StrokeCap.Round)

            drawLine(apparatusColor, Offset(cx - w * 0.36f, barY), Offset(cx + w * 0.36f, barY), strokeWidth = 5f, cap = StrokeCap.Round)
            drawRoundRect(apparatusColor, Offset(cx - w * 0.36f, barY - 18f), Size(10f, 36f), CornerRadius(3f))
            drawRoundRect(apparatusColor, Offset(cx + w * 0.28f, barY - 18f), Size(10f, 36f), CornerRadius(3f))
        }

        MuscleGroup.ARMS -> {
            // Biceps Curl / Triceps Extension
            val headY = cy - h * 0.22f
            drawCircle(charcoal, radius = 13f, center = Offset(cx, headY))
            drawLine(charcoal, Offset(cx, headY), Offset(cx, cy + h * 0.15f), strokeWidth = 14f, cap = StrokeCap.Round)

            // Arm & Forearm curling
            val elbowX = cx + w * 0.12f
            val elbowY = cy - h * 0.02f
            drawLine(charcoal, Offset(cx + 6f, headY + 14f), Offset(elbowX, elbowY), strokeWidth = 7f, cap = StrokeCap.Round)

            // Biceps peak highlight
            drawCircle(accentColor, radius = 9f, center = Offset(elbowX - 4f, elbowY - 10f))

            // Forearm moves with phase
            val handY = elbowY - (phase * h * 0.18f)
            val handX = elbowX - (phase * w * 0.08f)
            drawLine(charcoal, Offset(elbowX, elbowY), Offset(handX, handY), strokeWidth = 6f, cap = StrokeCap.Round)

            // Dumbbell at hand
            drawRoundRect(apparatusColor, Offset(handX - 8f, handY - 12f), Size(16f, 24f), CornerRadius(4f))
        }

        else -> {
            // General Full Body / Core
            val headY = cy - h * 0.15f
            drawCircle(charcoal, radius = 14f, center = Offset(cx, headY))
            drawLine(charcoal, Offset(cx, headY), Offset(cx, cy + h * 0.2f), strokeWidth = 14f, cap = StrokeCap.Round)
            drawCircle(accentColor, radius = 10f, center = Offset(cx, cy + 5f))

            // Limbs
            drawLine(charcoal, Offset(cx, headY + 14f), Offset(cx - w * 0.25f, cy - h * 0.02f + phase * 10f), strokeWidth = 6f, cap = StrokeCap.Round)
            drawLine(charcoal, Offset(cx, headY + 14f), Offset(cx + w * 0.25f, cy - h * 0.02f - phase * 10f), strokeWidth = 6f, cap = StrokeCap.Round)
            drawLine(charcoal, Offset(cx, cy + h * 0.2f), Offset(cx - w * 0.18f, cy + h * 0.42f), strokeWidth = 8f, cap = StrokeCap.Round)
            drawLine(charcoal, Offset(cx, cy + h * 0.2f), Offset(cx + w * 0.18f, cy + h * 0.42f), strokeWidth = 8f, cap = StrokeCap.Round)
        }
    }
}
