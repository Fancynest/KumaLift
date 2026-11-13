package com.bearbones.kumalift.core.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import java.io.File

@Composable
actual fun ProfileAvatarMedia(
    avatarUri: String?,
    fallbackInitial: String,
    modifier: Modifier
) {
    val colors = LocalKumaColors.current
    val context = LocalContext.current

    val cleanPath = avatarUri?.removePrefix("file://")
    val file = remember(cleanPath) {
        if (!cleanPath.isNullOrBlank()) File(cleanPath) else null
    }
    val isValid = file != null && file.exists() && file.length() > 0L

    if (!isValid) {
        AvatarInitialFallback(fallbackInitial, modifier, colors.accentText)
        return
    }

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data(file)
            .crossfade(true)
            .build(),
        contentDescription = "Profile Avatar",
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(CircleShape),
        loading = {
            AvatarInitialFallback(fallbackInitial, Modifier.fillMaxSize(), colors.accentText)
        },
        error = {
            AvatarInitialFallback(fallbackInitial, Modifier.fillMaxSize(), colors.accentText)
        }
    )
}

@Composable
private fun AvatarInitialFallback(
    initial: String,
    modifier: Modifier,
    accentColor: Color
) {
    Box(
        modifier = modifier
            .background(Color(0xFF111317), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial.take(1).ifBlank { "G" }.uppercase(),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
        )
    }
}
