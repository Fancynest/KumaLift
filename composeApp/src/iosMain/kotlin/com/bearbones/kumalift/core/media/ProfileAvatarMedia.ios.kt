package com.bearbones.kumalift.core.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.bearbones.kumalift.core.designsystem.LocalKumaColors

@Composable
actual fun ProfileAvatarMedia(
    avatarUri: String?,
    fallbackInitial: String,
    modifier: Modifier
) {
    val colors = LocalKumaColors.current
    Box(
        modifier = modifier
            .background(Color(0xFF111317), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = fallbackInitial.take(1).ifBlank { "G" }.uppercase(),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = colors.accentText
            )
        )
    }
}
