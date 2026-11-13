package com.bearbones.kumalift.core.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Renders a circular profile avatar from local persistent storage.
 * If avatarUri is null, blank, or missing, renders a fallback with the athlete's initials.
 */
@Composable
expect fun ProfileAvatarMedia(
    avatarUri: String?,
    fallbackInitial: String,
    modifier: Modifier = Modifier
)
