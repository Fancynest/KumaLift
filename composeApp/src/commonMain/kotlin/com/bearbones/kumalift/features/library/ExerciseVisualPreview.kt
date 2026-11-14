package com.bearbones.kumalift.features.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bearbones.kumalift.core.media.ControllableGifPlayer
import com.bearbones.kumalift.core.media.ExerciseGifMedia
import com.bearbones.kumalift.core.media.ExerciseThumbnailMedia
import com.bearbones.kumalift.domain.model.Equipment
import com.bearbones.kumalift.domain.model.Exercise
import com.bearbones.kumalift.domain.model.MovementType
import com.bearbones.kumalift.domain.model.MuscleGroup
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Circular thumbnail for exercise list rows — lazy loaded from exercises-dataset `image`.
 * Falls back safely to muscle badge if image not found.
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
            .background(Color(0xFF161A22))
            .border(1.5.dp, muscleColor.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        ExerciseThumbnailMedia(
            assetPath = exercise.thumbnailUrl,
            contentDescription = exercise.name,
            modifier = Modifier.fillMaxSize().clip(CircleShape)
        )
    }
}

/**
 * Calculates the flat background color for the animation panel:
 * muscleAccentColor(primaryMuscle) with alpha 0.06 over pure Color.White.
 */
fun getPanelBackgroundColor(muscle: MuscleGroup): Color {
    val accent = muscleAccentColor(muscle)
    val a = 0.06f
    return Color(
        red = accent.red * a + (1f - a),
        green = accent.green * a + (1f - a),
        blue = accent.blue * a + (1f - a),
        alpha = 1f
    )
}

/**
 * Clean animated visual card for the exercise detail screen:
 * - Panel: fillMaxWidth, height 220.dp, single flat background color (accent @ 0.06 on white).
 * - No text, no badges, no borders, no decorative background shapes (no radial gradients, no concentric rings).
 * - GIF: fixed 180.dp x 180.dp centered, ContentScale.Fit.
 * - Rendered with BlendMode.Multiply so white illustration background seamlessly matches panel color.
 * - Minimal semi-transparent play/pause button in top-right corner.
 */
@Composable
fun ExerciseVisualAnimation(
    exercise: Exercise,
    modifier: Modifier = Modifier
) {
    val panelBgColor = getPanelBackgroundColor(exercise.primaryMuscle)
    var isPlaying by remember { mutableStateOf(true) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(panelBgColor),
        contentAlignment = Alignment.Center
    ) {
        // GIF / Thumbnail container composited with BlendMode.Multiply
        Box(
            modifier = Modifier
                .size(180.dp)
                .drawWithContent {
                    drawIntoCanvas { canvas ->
                        val paint = Paint().apply {
                            blendMode = BlendMode.Multiply
                        }
                        canvas.saveLayer(Rect(Offset.Zero, size), paint)
                        drawContent()
                        canvas.restore()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            ControllableGifPlayer(
                assetPath = exercise.gifUrl,
                contentDescription = exercise.name,
                isPlaying = isPlaying,
                fallbackThumbnailPath = exercise.thumbnailUrl,
                modifier = Modifier.size(180.dp)
            )
        }

        // Minimal Play/Pause button in top-right corner (icon only, semi-transparent white bg, dark icon)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.85f))
                .clickable { isPlaying = !isPlaying },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause animation" else "Play animation",
                tint = Color(0xFF1F2937),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** Returns a vivid accent colour for the given muscle group */
fun muscleAccentColor(muscle: MuscleGroup): Color = when (muscle) {
    MuscleGroup.CHEST     -> Color(0xFFFF6B6B)
    MuscleGroup.BACK      -> Color(0xFF4ECDC4)
    MuscleGroup.LEGS      -> Color(0xFFFFE66D)
    MuscleGroup.SHOULDERS -> Color(0xFF6BCB77)
    MuscleGroup.ARMS      -> Color(0xFF4D96FF)
    MuscleGroup.CORE      -> Color(0xFFFF9F43)
    MuscleGroup.CARDIO    -> Color(0xFFFF5252)
    MuscleGroup.NECK      -> Color(0xFFE056FD)
    else                  -> Color(0xFFBDBDBD)
}

// =========================================================================
// PREVIEWS (Screens 360dp & 412dp with Core, Back, Chest)
// =========================================================================

private val previewChestExercise = Exercise(
    id = "0025",
    name = "Barbell Bench Press",
    primaryMuscle = MuscleGroup.CHEST,
    subMuscle = "pectorals",
    equipment = Equipment.BARBELL,
    movementType = MovementType.COMPOUND,
    thumbnailUrl = "images/0025-EIeI8Vf.jpg",
    gifUrl = "videos/0025-EIeI8Vf.gif"
)

private val previewBackExercise = Exercise(
    id = "0027",
    name = "Barbell Bent Over Row",
    primaryMuscle = MuscleGroup.BACK,
    subMuscle = "upper back",
    equipment = Equipment.BARBELL,
    movementType = MovementType.COMPOUND,
    thumbnailUrl = "images/0027-EIeI8Vf.jpg",
    gifUrl = "videos/0027-EIeI8Vf.gif"
)

private val previewCoreExercise = Exercise(
    id = "0274",
    name = "Cross Crunch",
    primaryMuscle = MuscleGroup.CORE,
    subMuscle = "abs",
    equipment = Equipment.BODYWEIGHT,
    movementType = MovementType.COMPOUND,
    thumbnailUrl = "images/0274-EIeI8Vf.jpg",
    gifUrl = "videos/0274-EIeI8Vf.gif"
)

@Preview
@Composable
fun ExerciseVisualAnimationPreview360dp() {
    Column(
        modifier = Modifier
            .width(360.dp)
            .background(Color(0xFF0C0E12))
            .padding(vertical = 12.dp)
    ) {
        // Dada (Chest) - soft coral tint
        ExerciseVisualAnimation(exercise = previewChestExercise)
        Spacer(modifier = Modifier.height(16.dp))
        // Punggung (Back) - soft teal tint
        ExerciseVisualAnimation(exercise = previewBackExercise)
        Spacer(modifier = Modifier.height(16.dp))
        // Core - soft warm orange/cream tint
        ExerciseVisualAnimation(exercise = previewCoreExercise)
    }
}

@Preview
@Composable
fun ExerciseVisualAnimationPreview412dp() {
    Column(
        modifier = Modifier
            .width(412.dp)
            .background(Color(0xFF0C0E12))
            .padding(vertical = 12.dp)
    ) {
        // Dada (Chest) - soft coral tint
        ExerciseVisualAnimation(exercise = previewChestExercise)
        Spacer(modifier = Modifier.height(16.dp))
        // Punggung (Back) - soft teal tint
        ExerciseVisualAnimation(exercise = previewBackExercise)
        Spacer(modifier = Modifier.height(16.dp))
        // Core - soft warm orange/cream tint
        ExerciseVisualAnimation(exercise = previewCoreExercise)
    }
}
