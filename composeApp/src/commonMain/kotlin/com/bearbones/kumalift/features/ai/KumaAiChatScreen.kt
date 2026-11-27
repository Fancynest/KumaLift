package com.bearbones.kumalift.features.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumalift.core.ai.AiChatFullscreenImageViewer
import com.bearbones.kumalift.core.ai.AiChatImageThumbnail
import com.bearbones.kumalift.core.ai.AiSecretStore
import com.bearbones.kumalift.core.ai.MAX_AI_CHAT_IMAGES
import com.bearbones.kumalift.core.ai.rememberAiMediaLaunchers
import com.bearbones.kumalift.core.designsystem.KumaBadge
import com.bearbones.kumalift.core.designsystem.KumaCard
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.KumaDeleteConfirmationDialog
import com.bearbones.kumalift.core.designsystem.KumaLogoMark
import com.bearbones.kumalift.core.designsystem.KumaAnimatedIcon
import com.bearbones.kumalift.core.designsystem.KumaPrimaryButton
import com.bearbones.kumalift.core.designsystem.kumaScale
import com.bearbones.kumalift.core.designsystem.rememberKumaPressScale
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.core.localization.LocalStrings
import com.bearbones.kumalift.core.navigation.KumaBackHandler
import com.bearbones.kumalift.data.repository.SettingsRepository
import com.bearbones.kumalift.domain.ai.AiActionProposal
import com.bearbones.kumalift.domain.ai.AiChatMessage
import com.bearbones.kumalift.domain.ai.AiChatRole
import com.bearbones.kumalift.domain.ai.AiChatThread
import com.bearbones.kumalift.domain.ai.AiNutritionCardData
import com.bearbones.kumalift.domain.ai.AiOperationKind
import com.bearbones.kumalift.domain.ai.AiProposalStatus
import com.bearbones.kumalift.domain.ai.KumaAiAgentService
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.random.Random

@Composable
fun KumaAiChatScreen(
    agentService: KumaAiAgentService,
    secretStore: AiSecretStore,
    settingsRepository: SettingsRepository,
    onNavigateBack: () -> Unit,
    onOpenAiSettings: () -> Unit
) {
    val threads by agentService.threads.collectAsState()
    val currentThreadId by agentService.currentThreadId.collectAsState()
    val isTemporaryChat by agentService.isTemporaryChat.collectAsState()
    val messages by agentService.messages.collectAsState()
    val isWorking by agentService.isWorking.collectAsState()
    val liveSteps by agentService.liveStepLogs.collectAsState()
    val settings by settingsRepository.settings.collectAsState()
    val hasApiKey by secretStore.hasApiKeyFlow.collectAsState()

    val colors = LocalKumaColors.current
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val athleteGreetingName = remember(settings.profile.username, settings.profile.name, s.athleteDefault) {
        settings.profile.username.ifBlank { settings.profile.name.ifBlank { s.athleteDefault } }
    }
    var greetingIndex by rememberSaveable { mutableStateOf(Random.nextInt(5)) }
    var composerText by rememberSaveable { mutableStateOf("") }
    var attachedImagePaths by rememberSaveable(
        stateSaver = listSaver<List<String>, String>(save = { it }, restore = { it })
    ) { mutableStateOf<List<String>>(emptyList()) }
    var fullscreenPreviewState by remember { mutableStateOf<Pair<List<String>, Int>?>(null) }
    var pendingDeleteThread by remember { mutableStateOf<AiChatThread?>(null) }
    var showAttachMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    KumaBackHandler(enabled = fullscreenPreviewState != null || drawerState.isOpen) {
        when {
            fullscreenPreviewState != null -> fullscreenPreviewState = null
            drawerState.isOpen -> scope.launch { drawerState.close() }
        }
    }

    val mediaLaunchers = rememberAiMediaLaunchers { newlyPicked ->
        attachedImagePaths = (attachedImagePaths + newlyPicked)
            .distinct()
            .take(MAX_AI_CHAT_IMAGES)
    }

    LaunchedEffect(messages.size, isWorking) {
        val lastIdx = (messages.size + (if (isWorking) 1 else 0) - 1).coerceAtLeast(0)
        if (lastIdx > 0) {
            listState.animateScrollToItem(lastIdx)
        }
    }

    fullscreenPreviewState?.let { (previewPaths, previewIndex) ->
        AiChatFullscreenImageViewer(
            imagePaths = previewPaths,
            initialIndex = previewIndex,
            closeContentDescription = s.close,
            pageIndicatorProvider = s::aiPhotoPageIndicator,
            onDismiss = { fullscreenPreviewState = null }
        )
    }

    pendingDeleteThread?.let { targetThread ->
        KumaDeleteConfirmationDialog(
            title = s.aiDeleteThreadTitle,
            message = s.aiDeleteThreadMessage(targetThread.title),
            confirmText = s.delete,
            cancelText = s.cancel,
            onConfirm = {
                pendingDeleteThread = null
                agentService.deleteThread(targetThread.id)
            },
            onDismiss = { pendingDeleteThread = null }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = KumaColors.SurfaceDim,
                drawerContentColor = colors.textPrimary,
                modifier = Modifier.widthIn(max = 300.dp)
            ) {
                AiHistorySidebarContent(
                    threads = threads,
                    currentThreadId = currentThreadId,
                    isTemporaryChat = isTemporaryChat,
                    onNewChat = {
                        greetingIndex = (greetingIndex + Random.nextInt(1, 5)) % 5
                        agentService.startNewChat(temporary = false)
                        scope.launch { drawerState.close() }
                    },
                    onToggleTemporary = { enabled ->
                        greetingIndex = (greetingIndex + Random.nextInt(1, 5)) % 5
                        agentService.setTemporaryChat(enabled)
                        scope.launch { drawerState.close() }
                    },
                    onSelectThread = { tid ->
                        agentService.selectThread(tid)
                        scope.launch { drawerState.close() }
                    },
                    onRequestDeleteThread = { t ->
                        pendingDeleteThread = t
                    }
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(KumaColors.CanvasLowest)
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KumaColors.SurfaceDim)
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(KumaColors.SurfaceContainer)
                        .clickable(onClick = onNavigateBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = s.back,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(KumaColors.SurfaceContainer)
                        .clickable { scope.launch { drawerState.open() } },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = s.aiHistoryOpenDesc,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Text(
                    text = s.aiTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpenAiSettings),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )

                val (tempInteraction, tempScale) = rememberKumaPressScale(targetScale = 0.90f)
                val tempHaptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .kumaScale(tempScale)
                        .clip(CircleShape)
                        .background(
                            if (isTemporaryChat) colors.accentFill.copy(alpha = 0.16f)
                            else Color.Transparent
                        )
                        .then(
                            if (isTemporaryChat) Modifier.border(1.dp, colors.accentFillBorder, CircleShape)
                            else Modifier
                        )
                        .clickable(
                            interactionSource = tempInteraction,
                            indication = androidx.compose.foundation.LocalIndication.current
                        ) {
                            tempHaptic.performHapticFeedback(
                                androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove
                            )
                            greetingIndex = (greetingIndex + Random.nextInt(1, 5)) % 5
                            agentService.toggleTemporaryChat()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    KumaAnimatedIcon(
                        imageVector = Icons.Default.VisibilityOff,
                        contentDescription = s.aiTempChatToggleDesc,
                        tint = if (isTemporaryChat) colors.accentText else colors.textSecondary,
                        modifier = Modifier.size(19.dp),
                        rotationDegrees = if (isTemporaryChat) -12f else 0f
                    )
                }

                if (messages.isNotEmpty() || currentThreadId != null) {
                    IconButton(
                        onClick = {
                            greetingIndex = (greetingIndex + Random.nextInt(1, 5)) % 5
                            agentService.startNewChat(temporary = false)
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = s.aiNewChatAction,
                            tint = colors.textSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onOpenAiSettings,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = s.aiSettingsTitle,
                        tint = colors.textSecondary
                    )
                }
            }

            if (isTemporaryChat) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(KumaColors.SurfaceContainer.copy(alpha = 0.7f))
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = colors.accentText,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = s.aiTempChatBadge,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = colors.accentText,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            // Chat Body
            if (hasApiKey && messages.isEmpty() && !isWorking) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (isTemporaryChat) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = colors.accentText,
                                modifier = Modifier.size(52.dp)
                            )
                            Text(
                                text = s.aiTempChatToggleDesc,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary
                                )
                            )
                            Text(
                                text = s.aiTempChatDesc,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = colors.textSecondary,
                                    lineHeight = 19.sp
                                )
                            )
                        } else {
                            KumaLogoMark(
                                modifier = Modifier.size(64.dp),
                                darkColor = KumaColors.CanvasLowest
                            )
                            Text(
                                text = s.aiEmptyGreeting(greetingIndex, athleteGreetingName),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary,
                                    lineHeight = 30.sp
                                )
                            )
                        }
                    }
                }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (!hasApiKey && messages.isEmpty()) {
                    item {
                        KumaCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = KumaColors.SurfaceContainer,
                            borderColor = colors.accentFillBorder,
                            cornerRadius = 16.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = null,
                                    tint = colors.accentText,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = s.aiSetupNeededTitle,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                )
                                Text(
                                    text = s.aiSetupNeededDesc,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                KumaPrimaryButton(
                                    text = s.aiOpenSettingsAction,
                                    onClick = onOpenAiSettings,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                items(messages, key = { it.id }) { msg ->
                    Box(modifier = Modifier.animateItem()) {
                        if (msg.role == AiChatRole.USER) {
                            UserChatBubble(
                                message = msg,
                                onImageClick = { paths, idx ->
                                    fullscreenPreviewState = paths to idx
                                }
                            )
                        } else {
                            AssistantChatMessageBlock(
                                message = msg,
                                onApproveProposal = { agentService.approveProposal(msg.id, s) },
                                onCancelProposal = { agentService.cancelProposal(msg.id) }
                            )
                        }
                    }
                }

                if (isWorking) {
                    item(key = "live_working_indicator") {
                        AssistantThinkingBubble(
                            statusLabel = liveSteps.lastOrNull() ?: s.aiThinkingStatus
                        )
                    }
                }
            }
        }

        // Bottom Composer Dock
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(KumaColors.SurfaceDim)
                .border(1.dp, colors.border)
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Pending Attached Photos Strip (tap any thumbnail to view full size before sending)
            if (attachedImagePaths.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = s.aiSelectedImagesCount(attachedImagePaths.size, MAX_AI_CHAT_IMAGES),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = colors.textSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        if (attachedImagePaths.size >= MAX_AI_CHAT_IMAGES) {
                            Text(
                                text = s.aiMaxPhotosHint,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = colors.accentText
                                )
                            )
                        }
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(attachedImagePaths) { idx, imgPath ->
                            Box(modifier = Modifier.size(64.dp)) {
                                AiChatImageThumbnail(
                                    imagePath = imgPath,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                                        .clickable {
                                            fullscreenPreviewState = attachedImagePaths to idx
                                        }
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(3.dp)
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.72f))
                                        .clickable {
                                            attachedImagePaths = attachedImagePaths.filterIndexed { i, _ -> i != idx }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = s.aiRemoveAttachmentDesc,
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick Prompt Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    s.aiQuickChipRoutine,
                    s.aiQuickChipProgress,
                    s.aiQuickChipSwap,
                    s.aiQuickChipMeal
                ).forEach { chip ->
                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(KumaColors.SurfaceContainer)
                            .border(1.dp, colors.border, PillShape)
                            .clickable(enabled = !isWorking) {
                                composerText = chip
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = chip,
                            maxLines = 1,
                            softWrap = false,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = colors.textSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }

            // Input Pill (+ Attach Menu + Composer) + Send / Stop
            val remainingPhotoSlots = (MAX_AI_CHAT_IMAGES - attachedImagePaths.size).coerceAtLeast(0)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 46.dp)
                        .clip(PillShape)
                        .background(KumaColors.SurfaceContainer)
                        .border(1.dp, colors.border, PillShape)
                        .padding(start = 6.dp, end = 14.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        val canAttachMore = !isWorking && remainingPhotoSlots > 0
                        val attachHaptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                        val (attachInteraction, attachScale) = rememberKumaPressScale(
                            enabled = canAttachMore,
                            targetScale = 0.90f
                        )
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .kumaScale(attachScale)
                                .clip(CircleShape)
                                .background(KumaColors.SurfaceContainerHigh)
                                .clickable(
                                    enabled = canAttachMore,
                                    interactionSource = attachInteraction,
                                    indication = androidx.compose.foundation.LocalIndication.current
                                ) {
                                    attachHaptic.performHapticFeedback(
                                        androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove
                                    )
                                    showAttachMenu = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            KumaAnimatedIcon(
                                imageVector = Icons.Default.Add,
                                contentDescription = s.aiAttachMediaDesc,
                                tint = if (canAttachMore) colors.textPrimary else colors.textSubtle,
                                modifier = Modifier.size(20.dp),
                                rotationDegrees = if (showAttachMenu) 45f else 0f
                            )
                        }

                        DropdownMenu(
                            expanded = showAttachMenu,
                            onDismissRequest = { showAttachMenu = false },
                            modifier = Modifier.background(KumaColors.SurfaceContainer)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = s.aiCameraDesc,
                                        color = colors.textPrimary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.PhotoCamera,
                                        contentDescription = null,
                                        tint = colors.accentText
                                    )
                                },
                                enabled = remainingPhotoSlots > 0,
                                onClick = {
                                    showAttachMenu = false
                                    mediaLaunchers.launchCamera()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = s.aiGalleryDesc,
                                        color = colors.textPrimary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        tint = colors.accentText
                                    )
                                },
                                enabled = remainingPhotoSlots > 0,
                                onClick = {
                                    showAttachMenu = false
                                    mediaLaunchers.launchGallery(remainingPhotoSlots)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (composerText.isEmpty()) {
                            Text(
                                text = s.aiComposerPlaceholder,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = colors.textSubtle
                                )
                            )
                        }
                        BasicTextField(
                            value = composerText,
                            onValueChange = { composerText = it },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = colors.textPrimary
                            ),
                            cursorBrush = SolidColor(colors.accentFill),
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                val sendHaptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                val canSendOrStop = isWorking || composerText.isNotBlank() || attachedImagePaths.isNotEmpty()
                val (sendInteraction, sendScale) = rememberKumaPressScale(
                    enabled = canSendOrStop,
                    targetScale = 0.90f
                )
                val sendBgColor = androidx.compose.animation.animateColorAsState(
                    targetValue = if (isWorking) colors.errorTint
                    else if (composerText.isNotBlank() || attachedImagePaths.isNotEmpty()) colors.accentFill
                    else KumaColors.SurfaceContainerHigh,
                    animationSpec = androidx.compose.animation.core.tween(160)
                ).value
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .kumaScale(sendScale)
                        .clip(CircleShape)
                        .background(sendBgColor)
                        .clickable(
                            enabled = canSendOrStop,
                            interactionSource = sendInteraction,
                            indication = androidx.compose.foundation.LocalIndication.current
                        ) {
                            sendHaptic.performHapticFeedback(
                                androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove
                            )
                            if (isWorking) {
                                agentService.stopCurrentRequest()
                            } else if (composerText.isNotBlank() || attachedImagePaths.isNotEmpty()) {
                                val textToSend = composerText
                                val imgsToSend = attachedImagePaths
                                composerText = ""
                                attachedImagePaths = emptyList()
                                agentService.sendMessage(
                                    promptText = textToSend,
                                    imagePaths = imgsToSend,
                                    strings = s,
                                    language = settings.appearance.language
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    KumaAnimatedIcon(
                        imageVector = if (isWorking) Icons.Default.Stop else Icons.AutoMirrored.Filled.Send,
                        contentDescription = if (isWorking) s.aiStopDesc else s.aiSendDesc,
                        tint = if (isWorking) colors.error
                        else if (composerText.isNotBlank() || attachedImagePaths.isNotEmpty()) colors.onAccentFill
                        else colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun AiHistorySidebarContent(
    threads: List<AiChatThread>,
    currentThreadId: String?,
    isTemporaryChat: Boolean,
    onNewChat: () -> Unit,
    onToggleTemporary: (Boolean) -> Unit,
    onSelectThread: (String) -> Unit,
    onRequestDeleteThread: (AiChatThread) -> Unit
) {
    val colors = LocalKumaColors.current
    val s = LocalStrings.current
    val tz = remember { TimeZone.currentSystemDefault() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KumaLogoMark(
                modifier = Modifier.size(28.dp),
                darkColor = KumaColors.SurfaceDim
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = s.aiTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
                Text(
                    text = s.aiHistoryTitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = colors.textSecondary
                    )
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.accentFill)
                .clickable(onClick = onNewChat)
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.EditNote,
                contentDescription = null,
                tint = colors.onAccentFill,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = s.aiNewChatAction,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = colors.onAccentFill,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(KumaColors.SurfaceContainer)
                .border(
                    1.dp,
                    if (isTemporaryChat) colors.accentFillBorder else colors.border,
                    RoundedCornerShape(12.dp)
                )
                .clickable { onToggleTemporary(!isTemporaryChat) }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.VisibilityOff,
                contentDescription = null,
                tint = if (isTemporaryChat) colors.accentText else colors.textSecondary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = s.aiTempChatToggleDesc,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Medium
                )
            )
            Switch(
                checked = isTemporaryChat,
                onCheckedChange = onToggleTemporary,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.onAccentFill,
                    checkedTrackColor = colors.accentFill,
                    uncheckedThumbColor = colors.textSecondary,
                    uncheckedTrackColor = KumaColors.SurfaceContainerHigh
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.border)
        )

        if (threads.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = s.aiHistoryEmpty,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall.copy(color = colors.textSubtle)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(threads, key = { it.id }) { thread ->
                    val isSelected = !isTemporaryChat && thread.id == currentThreadId
                    val dateLabel = remember(thread.updatedAtEpochMs) {
                        if (thread.updatedAtEpochMs > 0L) {
                            Instant.fromEpochMilliseconds(thread.updatedAtEpochMs)
                                .toLocalDateTime(tz).date.toString()
                        } else ""
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) KumaColors.SurfaceContainerHigh
                                else KumaColors.SurfaceContainer.copy(alpha = 0.6f)
                            )
                            .then(
                                if (isSelected) Modifier.border(1.dp, colors.accentFillBorder, RoundedCornerShape(12.dp))
                                else Modifier
                            )
                            .clickable { onSelectThread(thread.id) }
                            .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = thread.title.ifBlank { s.aiTitle },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (isSelected) colors.accentText else colors.textPrimary
                                )
                            )
                            if (dateLabel.isNotBlank()) {
                                Text(
                                    text = dateLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = colors.textSubtle
                                    )
                                )
                            }
                        }
                        IconButton(
                            onClick = { onRequestDeleteThread(thread) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = s.aiDeleteThreadDesc,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserChatBubble(
    message: AiChatMessage,
    onImageClick: (List<String>, Int) -> Unit
) {
    val colors = LocalKumaColors.current
    val imgs = message.effectiveImagePaths
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 290.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(KumaColors.SurfaceContainerHigh)
                .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (imgs.size == 1) {
                AiChatImageThumbnail(
                    imagePath = imgs.first(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clickable { onImageClick(imgs, 0) }
                )
            } else if (imgs.size > 1) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    imgs.chunked(3).forEachIndexed { rowIdx, rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rowItems.forEachIndexed { colIdx, path ->
                                val globalIdx = rowIdx * 3 + colIdx
                                AiChatImageThumbnail(
                                    imagePath = path,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(82.dp)
                                        .clickable { onImageClick(imgs, globalIdx) }
                                )
                            }
                        }
                    }
                }
            }
            if (message.text.isNotBlank()) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = colors.textPrimary,
                        lineHeight = 20.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun AssistantChatMessageBlock(
    message: AiChatMessage,
    onApproveProposal: () -> Unit,
    onCancelProposal: () -> Unit
) {
    val colors = LocalKumaColors.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        message.nutritionCard?.let { nutrition ->
            NutritionAnalysisCard(data = nutrition)
        }

        if (message.text.isNotBlank()) {
            val codeBg = KumaColors.SurfaceContainerHigh
            val formattedText = remember(message.id, message.text, message.isError, codeBg) {
                if (message.isError) {
                    AnnotatedString(message.text)
                } else {
                    parseChatMarkdown(message.text, codeBg)
                }
            }
            Box(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (message.isError) colors.errorTint else KumaColors.SurfaceContainer)
                    .border(
                        1.dp,
                        if (message.isError) colors.error.copy(alpha = 0.45f) else colors.border,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(14.dp)
            ) {
                Text(
                    text = formattedText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (message.isError) colors.error else colors.textPrimary,
                        lineHeight = 20.sp
                    )
                )
            }
        }

        message.proposal?.let { prop ->
            ProposalReviewCard(
                proposal = prop,
                onApprove = onApproveProposal,
                onCancel = onCancelProposal
            )
        }
    }
}

@Composable
private fun AssistantThinkingBubble(
    statusLabel: String
) {
    val colors = LocalKumaColors.current
    val transition = rememberInfiniteTransition()
    val dot1 by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val dot2 by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 550, delayMillis = 150, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val dot3 by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 550, delayMillis = 300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(KumaColors.SurfaceContainer)
            .border(1.dp, colors.border, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(dot1, dot2, dot3).forEach { alphaVal ->
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .alpha(alphaVal)
                        .clip(CircleShape)
                        .background(colors.accentText)
                )
            }
        }
        Text(
            text = statusLabel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall.copy(
                color = colors.textSecondary,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun NutritionAnalysisCard(data: AiNutritionCardData) {
    val colors = LocalKumaColors.current
    val s = LocalStrings.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(KumaColors.SurfaceContainer)
            .border(1.dp, colors.border, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        KumaBadge(
            text = s.aiNutritionBadge,
            backgroundColor = colors.accentTint,
            contentColor = colors.accentTextOnTint
        )

        Text(
            text = data.foodName,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MacroTile(
                value = "${data.caloriesKcal}",
                label = s.aiNutritionCaloriesLabel,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            MacroTile(
                value = "${data.proteinGrams}g",
                label = s.aiNutritionProteinLabel,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            MacroTile(
                value = "${data.carbsGrams}g",
                label = s.aiNutritionCarbsLabel,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            MacroTile(
                value = "${data.fatGrams}g",
                label = s.aiNutritionFatLabel,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }

        if (data.summary.isNotBlank()) {
            Text(
                text = data.summary,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = colors.textSecondary,
                    lineHeight = 18.sp
                )
            )
        }
    }
}

@Composable
private fun MacroTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalKumaColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(KumaColors.SurfaceContainerHigh)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .padding(horizontal = 6.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = value,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = colors.textPrimary
            )
        )
        Text(
            text = label,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall.copy(
                color = colors.textSecondary
            )
        )
    }
}

@Composable
private fun ProposalReviewCard(
    proposal: AiActionProposal,
    onApprove: () -> Unit,
    onCancel: () -> Unit
) {
    val colors = LocalKumaColors.current
    val s = LocalStrings.current

    val (badgeText, badgeBg, badgeFg) = when (proposal.status) {
        AiProposalStatus.PENDING -> Triple(s.aiReviewProposalBadge, colors.warningTint, colors.warning)
        AiProposalStatus.APPLIED -> Triple(s.aiAppliedBadge, colors.accentTint, colors.accentTextOnTint)
        AiProposalStatus.CANCELLED -> Triple(s.aiCancelledBadge, colors.surfaceBright, colors.textSecondary)
        AiProposalStatus.FAILED -> Triple(s.aiFailedBadge, colors.errorTint, colors.error)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(KumaColors.SurfaceContainer)
            .border(1.dp, colors.border, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        KumaBadge(
            text = badgeText,
            backgroundColor = badgeBg,
            contentColor = badgeFg
        )

        proposal.operations.forEach { op ->
            val (opBg, opFg, opIcon) = when (op.operationKind) {
                AiOperationKind.ADD -> Triple(colors.recoveryFresh.copy(alpha = 0.2f), colors.recoveryFresh, Icons.Default.Add)
                AiOperationKind.UPDATE -> Triple(colors.warningTint, colors.warning, Icons.Default.EditNote)
                AiOperationKind.DELETE -> Triple(colors.errorTint, colors.error, Icons.Default.Remove)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(KumaColors.SurfaceContainerHigh)
                    .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(opBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = opIcon,
                        contentDescription = null,
                        tint = opFg,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = op.title,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    )
                    if (op.subtitle.isNotBlank()) {
                        Text(
                            text = op.subtitle,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = colors.textSecondary
                            )
                        )
                    }
                }
            }
        }

        proposal.resultNote?.takeIf { it.isNotBlank() }?.let { note ->
            Text(
                text = note,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (proposal.status == AiProposalStatus.FAILED) colors.error else colors.accentText
                )
            )
        }

        if (proposal.status == AiProposalStatus.PENDING) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(0.85f)
                        .heightIn(min = 44.dp),
                    shape = PillShape,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = s.aiProposalCancelAction,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = onApprove,
                    modifier = Modifier
                        .weight(1.15f)
                        .heightIn(min = 44.dp),
                    shape = PillShape,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accentFill,
                        contentColor = colors.onAccentFill
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = s.aiProposalApproveAction,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private val HEADING_REGEX = Regex("""^\s{0,3}(#{1,6})\s+(.+)$""")
private val BULLET_REGEX = Regex("""^(\s*)[*-]\s+(.+)$""")

/**
 * Lightweight chat Markdown parser rendering bold, italic, underline, strikethrough,
 * inline/fenced code, headings, quotes, and bullet lists via native Compose AnnotatedString.
 */
internal fun parseChatMarkdown(raw: String, codeBg: Color): AnnotatedString {
    val lines = raw.replace("\r\n", "\n").split("\n")
    return buildAnnotatedString {
        var inCodeBlock = false
        var needsNewline = false

        lines.forEach { line ->
            val trimmedStart = line.trimStart()
            if (trimmedStart.startsWith("```")) {
                inCodeBlock = !inCodeBlock
                return@forEach
            }
            if (needsNewline) append('\n')
            needsNewline = true

            if (inCodeBlock) {
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBg)) {
                    append(line)
                }
                return@forEach
            }

            val heading = HEADING_REGEX.matchEntire(line)
            if (heading != null) {
                val level = heading.groupValues[1].length
                val style = if (level <= 2) {
                    SpanStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp)
                } else {
                    SpanStyle(fontWeight = FontWeight.Bold)
                }
                withStyle(style) {
                    appendInlineMarkdown(heading.groupValues[2].trim(), codeBg)
                }
                return@forEach
            }

            val bullet = BULLET_REGEX.matchEntire(line)
            if (bullet != null) {
                val indent = " ".repeat((bullet.groupValues[1].length / 2) * 2)
                append(indent)
                append("• ")
                appendInlineMarkdown(bullet.groupValues[2], codeBg)
                return@forEach
            }

            if (trimmedStart.startsWith("> ")) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("▎ ") }
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    appendInlineMarkdown(trimmedStart.drop(2), codeBg)
                }
                return@forEach
            }

            appendInlineMarkdown(line, codeBg)
        }
    }
}

private fun AnnotatedString.Builder.appendInlineMarkdown(text: String, codeBg: Color) {
    var i = 0
    while (i < text.length) {
        if (text[i] == '\\' && i + 1 < text.length && text[i + 1] in charArrayOf('*', '_', '~', '`')) {
            append(text[i + 1])
            i += 2
            continue
        }

        if (text[i] == '`') {
            val end = text.indexOf('`', i + 1)
            if (end > i + 1) {
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBg)) {
                    append(text.substring(i + 1, end))
                }
                i = end + 1
                continue
            }
        }

        if (text.startsWith("***", i)) {
            val end = findClosingDelimiter(text, "***", i + 3)
            if (end > i + 3) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                    appendInlineMarkdown(text.substring(i + 3, end), codeBg)
                }
                i = end + 3
                continue
            }
        }

        if (text.startsWith("**", i)) {
            val end = findClosingDelimiter(text, "**", i + 2)
            if (end > i + 2) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    appendInlineMarkdown(text.substring(i + 2, end), codeBg)
                }
                i = end + 2
                continue
            }
        }

        if (text.startsWith("__", i)) {
            val end = findClosingDelimiter(text, "__", i + 2)
            if (end > i + 2) {
                withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                    appendInlineMarkdown(text.substring(i + 2, end), codeBg)
                }
                i = end + 2
                continue
            }
        }

        if (text.regionMatches(i, "<u>", 0, 3, ignoreCase = true)) {
            val end = text.indexOf("</u>", i + 3, ignoreCase = true)
            if (end > i + 3) {
                withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                    appendInlineMarkdown(text.substring(i + 3, end), codeBg)
                }
                i = end + 4
                continue
            }
        }

        if (text.startsWith("~~", i)) {
            val end = findClosingDelimiter(text, "~~", i + 2)
            if (end > i + 2) {
                withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                    appendInlineMarkdown(text.substring(i + 2, end), codeBg)
                }
                i = end + 2
                continue
            }
        }

        if (text[i] == '*' && i + 1 < text.length && !text[i + 1].isWhitespace() && text[i + 1] != '*') {
            val end = findSingleEmphasisEnd(text, '*', i + 1)
            if (end > i + 1) {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    appendInlineMarkdown(text.substring(i + 1, end), codeBg)
                }
                i = end + 1
                continue
            }
        }

        if (text[i] == '_' && (i == 0 || !text[i - 1].isLetterOrDigit()) &&
            i + 1 < text.length && !text[i + 1].isWhitespace() && text[i + 1] != '_'
        ) {
            val end = findSingleEmphasisEnd(text, '_', i + 1, requireWordBoundaryAfter = true)
            if (end > i + 1) {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    appendInlineMarkdown(text.substring(i + 1, end), codeBg)
                }
                i = end + 1
                continue
            }
        }

        append(text[i])
        i++
    }
}

private fun findClosingDelimiter(text: String, delim: String, startIndex: Int): Int {
    val idx = text.indexOf(delim, startIndex)
    if (idx <= startIndex) return -1
    if (text[startIndex].isWhitespace() || text[idx - 1].isWhitespace()) return -1
    return idx
}

private fun findSingleEmphasisEnd(
    text: String,
    marker: Char,
    startIndex: Int,
    requireWordBoundaryAfter: Boolean = false
): Int {
    var idx = startIndex
    while (idx < text.length) {
        if (text[idx] == marker && (idx == 0 || text[idx - 1] != '\\') && !text[idx - 1].isWhitespace()) {
            val nextIsSame = text.getOrNull(idx + 1) == marker
            val prevIsSame = idx > startIndex && text.getOrNull(idx - 1) == marker
            val boundaryOk = !requireWordBoundaryAfter || idx + 1 >= text.length || !text[idx + 1].isLetterOrDigit()
            if (!nextIsSame && !prevIsSame && boundaryOk) return idx
        }
        idx++
    }
    return -1
}
