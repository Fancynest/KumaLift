package com.bearbones.kumalift.features.library

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
 * Always includes the required attribution notice: "© Gym visual — https://gymvisual.com/".
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
            .height(280.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D0F14), Color(0xFF161A22))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Authentic GIF Animation from dataset (videos/....gif)
        ExerciseGifMedia(
            assetPath = exercise.gifUrl,
            contentDescription = exercise.name,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp, horizontal = 24.dp)
        )

        // Muscle label badge — top-left
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1C2028).copy(alpha = 0.85f))
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
                    color = Color(0xFFCDD5E0),
                    fontWeight = FontWeight.Medium
                )
            )
        }

        // Equipment badge — top-right
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(14.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1C2028).copy(alpha = 0.85f))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = Color(0xFF8899AA),
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = exercise.equipment.displayName,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    color = Color(0xFF8899AA)
                )
            )
        }

        // Mandatory Copyright Attribution Banner — bottom-center (Do not remove per License)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xFF0C0E12).copy(alpha = 0.85f))
                .padding(vertical = 6.dp, horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = exercise.attribution.ifBlank { "© Gym visual — https://gymvisual.com/" },
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = Color(0xFF9CA3AF),
                    fontWeight = FontWeight.Normal
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
