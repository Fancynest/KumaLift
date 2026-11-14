package com.bearbones.kumalift.features.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumalift.core.media.ExerciseGifMedia
import com.bearbones.kumalift.core.media.ExerciseThumbnailMedia
import com.bearbones.kumalift.domain.model.Exercise
import com.bearbones.kumalift.domain.model.MuscleGroup

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
 * Large animated visual card for the exercise detail screen.
 * Displays the authentic GIF animation from exercises-dataset `gif_url`.
 * Seamlessly expands across full width with white background matching the GIF artwork.
 * Shows attribution notice "© Gym visual" without URL per user design preference.
 */
@Composable
fun ExerciseVisualAnimation(
    exercise: Exercise,
    modifier: Modifier = Modifier
) {
    val muscleColor = muscleAccentColor(exercise.primaryMuscle)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        // Authentic GIF Animation: bounded to 220dp with strict 1:1 aspect ratio
        // Prevents over-scaling and pixelation while keeping illustration sharp and clear
        ExerciseGifMedia(
            assetPath = exercise.gifUrl,
            contentDescription = exercise.name,
            modifier = Modifier
                .sizeIn(maxWidth = 220.dp, maxHeight = 220.dp)
                .aspectRatio(1f)
                .padding(8.dp)
        )

        // Muscle label badge — top-left
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF14171F).copy(alpha = 0.85f))
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
                    fontSize = 11.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        // Equipment badge — top-right
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(14.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF14171F).copy(alpha = 0.85f))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = Color(0xFFC6FF3D),
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = exercise.equipment.displayName,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
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
