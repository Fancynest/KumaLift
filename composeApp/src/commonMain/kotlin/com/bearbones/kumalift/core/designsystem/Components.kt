package com.bearbones.kumalift.core.designsystem

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Applies uniform X/Y layer scale without triggering relayout.
 */
fun Modifier.kumaScale(scale: Float): Modifier = graphicsLayer {
    scaleX = scale
    scaleY = scale
}

/**
 * State-animated vector icon with spring morphing between icons and optional spring rotation.
 */
@Composable
fun KumaAnimatedIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    tint: Color,
    modifier: Modifier = Modifier,
    rotationDegrees: Float = 0f
) {
    val animatedRotation = animateFloatAsState(
        targetValue = rotationDegrees,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    ).value
    AnimatedContent(
        targetState = imageVector,
        transitionSpec = {
            (scaleIn(
                initialScale = 0.72f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeIn(tween(120))) togetherWith
                (scaleOut(targetScale = 0.72f, animationSpec = tween(90)) + fadeOut(tween(90)))
        }
    ) { icon ->
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = modifier.graphicsLayer { rotationZ = animatedRotation }
        )
    }
}

/**
 * Material 3 Expressive-inspired floating pill toolbar container.
 */
@Composable
fun KumaFloatingToolbar(
    modifier: Modifier = Modifier,
    fillWidth: Boolean = false,
    containerColor: Color = KumaColors.SurfaceDim,
    borderColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(6.dp),
    content: @Composable RowScope.() -> Unit
) {
    val colors = LocalKumaColors.current
    Surface(
        modifier = modifier,
        shape = PillShape,
        color = containerColor,
        shadowElevation = 10.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, borderColor ?: colors.border)
    ) {
        Row(
            modifier = (if (fillWidth) Modifier.fillMaxWidth() else Modifier).padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = horizontalArrangement,
            content = content
        )
    }
}

/**
 * Returns pressed interaction source and spring-animated scale for tactile UI controls.
 */
@Composable
fun rememberKumaPressScale(
    enabled: Boolean = true,
    targetScale: Float = 0.96f
): Pair<MutableInteractionSource, Float> {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState().value
    val scale = animateFloatAsState(
        targetValue = if (isPressed && enabled) targetScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    ).value
    return interactionSource to scale
}

@Composable
fun KumaPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val colors = LocalKumaColors.current
    val haptic = LocalHapticFeedback.current
    val (interactionSource, pressScale) = rememberKumaPressScale(
        enabled = enabled,
        targetScale = 0.97f
    )
    val buttonBorder = if (colors.isDark || !enabled) Color.Transparent else colors.accentFillBorder
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .kumaScale(pressScale)
            .clip(PillShape)
            .background(if (enabled) colors.accentFill else colors.surfaceBright)
            .border(1.dp, buttonBorder, PillShape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = LocalIndication.current
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = text,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = if (enabled) colors.onAccentFill else colors.textSubtle,
                    letterSpacing = 0.5.sp
                )
            )
            if (icon != null) {
                Box(modifier = Modifier.padding(start = 8.dp)) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (enabled) colors.onAccentFill else colors.textSubtle,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun KumaCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = KumaColors.SurfaceContainerLow,
    borderColor: Color = KumaColors.BorderDefault,
    cornerRadius: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(cornerRadius)),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        content()
    }
}

@Composable
fun KumaSetCheckbox(
    isCompleted: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val (interactionSource, pressScale) = rememberKumaPressScale(targetScale = 0.88f)
    val containerColor = animateColorAsState(
        targetValue = if (isCompleted) KumaColors.ElectricLime else KumaColors.SurfaceContainer,
        animationSpec = tween(durationMillis = 180)
    ).value
    val borderColor = animateColorAsState(
        targetValue = if (isCompleted) Color.Transparent else KumaColors.BorderDefault,
        animationSpec = tween(durationMillis = 180)
    ).value
    // 48x48 touch target
    Box(
        modifier = modifier
            .size(48.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(
                        if (!isCompleted) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
                    )
                    onToggle()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .kumaScale(pressScale)
                .clip(RoundedCornerShape(8.dp))
                .background(containerColor)
                .border(
                    width = if (isCompleted) 0.dp else 1.5.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = isCompleted,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(tween(120)),
                exit = scaleOut(tween(100)) + fadeOut(tween(100))
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = KumaColors.OnElectricLime,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun KumaBadge(
    text: String,
    modifier: Modifier = Modifier,
    isPr: Boolean = false,
    icon: ImageVector? = null,
    backgroundColor: Color? = null,
    contentColor: Color? = null
) {
    val colors = LocalKumaColors.current
    val bg = backgroundColor ?: if (isPr) colors.accentTint else colors.surfaceVariant
    val border = if (backgroundColor != null) {
        (contentColor ?: colors.textPrimary).copy(alpha = 0.35f)
    } else if (isPr) {
        if (colors.isDark) colors.accentFill else colors.accentFillBorder
    } else colors.border
    val textColor = contentColor ?: if (isPr) {
        if (colors.isDark) colors.accentText else colors.accentTextOnTint
    } else colors.textPrimary

    Row(
        modifier = modifier
            .clip(PillShape)
            .background(bg)
            .border(1.dp, border, PillShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier
                    .size(14.dp)
                    .padding(end = 4.dp)
            )
        }
        Text(
            text = text,
            maxLines = 1,
            softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        )
    }
}

@Composable
fun KumaNumericStepper(
    valueText: String,
    unit: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(KumaColors.SurfaceContainerHigh)
            .border(1.dp, KumaColors.BorderDefault, RoundedCornerShape(12.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Minus button (44x44)
        Box(
            modifier = Modifier
                .size(44.dp)
                .clickable(onClick = onDecrement),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = null,
                tint = KumaColors.OnSurface,
                modifier = Modifier.size(18.dp)
            )
        }

        // Center Value
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = valueText,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = KumaColors.TextWhite
                )
            )
            if (unit.isNotEmpty()) {
                Text(
                    text = " $unit",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = KumaColors.TextMuted
                    ),
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }

        // Plus button (44x44)
        Box(
            modifier = Modifier
                .size(44.dp)
                .clickable(onClick = onIncrement),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = KumaColors.OnSurface,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun KumaLogoMark(
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFFC6FF3D),
    darkColor: Color = Color(0xFF111317)
) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 512f
        fun x(v: Float) = v * s
        fun y(v: Float) = (v - 14f) * s

        // Ears outer
        drawCircle(color = accentColor, radius = 44f * s, center = Offset(x(146f), y(110f)))
        drawCircle(color = accentColor, radius = 44f * s, center = Offset(x(366f), y(110f)))
        // Ears inner
        drawCircle(color = darkColor, radius = 16f * s, center = Offset(x(146f), y(110f)))
        drawCircle(color = darkColor, radius = 16f * s, center = Offset(x(366f), y(110f)))

        // Head chamfered
        val headPath = Path().apply {
            moveTo(x(176f), y(118f))
            lineTo(x(336f), y(118f))
            lineTo(x(396f), y(178f))
            lineTo(x(396f), y(290f))
            lineTo(x(336f), y(374f))
            lineTo(x(176f), y(374f))
            lineTo(x(116f), y(290f))
            lineTo(x(116f), y(178f))
            close()
        }
        drawPath(headPath, color = accentColor)

        // Eyes
        val leftEye = Path().apply {
            moveTo(x(172f), y(222f))
            lineTo(x(234f), y(238f))
            lineTo(x(230f), y(258f))
            lineTo(x(174f), y(244f))
            close()
        }
        drawPath(leftEye, color = darkColor)

        val rightEye = Path().apply {
            moveTo(x(340f), y(222f))
            lineTo(x(278f), y(238f))
            lineTo(x(282f), y(258f))
            lineTo(x(338f), y(244f))
            close()
        }
        drawPath(rightEye, color = darkColor)

        // Muzzle
        val muzzlePath = Path().apply {
            addOval(Rect(
                x(256f) - 64f * s,
                y(304f) - 46f * s,
                x(256f) + 64f * s,
                y(304f) + 46f * s
            ))
        }
        drawPath(muzzlePath, color = darkColor)

        // Nose
        val nosePath = Path().apply {
            moveTo(x(232f), y(284f))
            quadraticTo(x(256f), y(272f), x(280f), y(284f))
            quadraticTo(x(272f), y(304f), x(256f), y(308f))
            quadraticTo(x(240f), y(304f), x(232f), y(284f))
            close()
        }
        drawPath(nosePath, color = accentColor)

        // Barbell
        drawRoundRect(
            color = accentColor,
            topLeft = Offset(52f * s, 428f * s),
            size = Size(408f * s, 16f * s),
            cornerRadius = CornerRadius(3f * s, 3f * s)
        )
        drawRoundRect(
            color = accentColor,
            topLeft = Offset(84f * s, 396f * s),
            size = Size(30f * s, 80f * s),
            cornerRadius = CornerRadius(4f * s, 4f * s)
        )
        drawRoundRect(
            color = accentColor,
            topLeft = Offset(120f * s, 412f * s),
            size = Size(18f * s, 48f * s),
            cornerRadius = CornerRadius(3f * s, 3f * s)
        )
        drawRoundRect(
            color = accentColor,
            topLeft = Offset(398f * s, 396f * s),
            size = Size(30f * s, 80f * s),
            cornerRadius = CornerRadius(4f * s, 4f * s)
        )
        drawRoundRect(
            color = accentColor,
            topLeft = Offset(374f * s, 412f * s),
            size = Size(18f * s, 48f * s),
            cornerRadius = CornerRadius(3f * s, 3f * s)
        )
    }
}

/**
 * Isolates horizontal scrolling in child components (e.g. LazyRow filter chips)
 * so that overscrolling or dragging at boundaries does not bubble up to the parent HorizontalPager.
 */
@Composable
fun Modifier.isolateHorizontalScroll(): Modifier {
    val connection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // Absorb all horizontal overscroll so parent HorizontalPager never receives it
                return Offset(x = available.x, y = 0f)
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity {
                // Absorb any leftover horizontal fling velocity
                return Velocity(x = available.x, y = 0f)
            }
        }
    }
    return this.nestedScroll(connection)
}

/**
 * Global Keyboard Avoidance Modifier for KumaLift.
 *
 * Automatically shifts and adds dynamic padding when the software keyboard appears.
 * Coordinates nested scrolling with the keyboard so that active input fields
 * and lower content can be smoothly scrolled into view.
 */
@OptIn(ExperimentalLayoutApi::class)
fun Modifier.kumaKeyboardAvoidance(
    enableNestedScroll: Boolean = false
): Modifier {
    return this
        .imePadding()
        .then(if (enableNestedScroll) Modifier.imeNestedScroll() else Modifier)
}

/**
 * Reusable scrollable container for dialogs, bottom sheets, and forms containing inputs.
 * Ensures that all inputs, labels, and action buttons remain fully visible and accessible
 * when the software keyboard opens and closes.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KumaKeyboardAwareScrollable(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .imePadding(),
        content = content
    )
}

@Composable
fun KumaDeleteConfirmationDialog(
    title: String,
    message: String,
    confirmText: String,
    cancelText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalKumaColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KumaColors.SurfaceContainer,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = colors.error
                )
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = colors.textSecondary
                )
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.error,
                    contentColor = colors.onError
                )
            ) {
                Text(
                    text = confirmText,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = cancelText,
                    style = MaterialTheme.typography.labelLarge.copy(color = colors.textSecondary)
                )
            }
        }
    )
}

