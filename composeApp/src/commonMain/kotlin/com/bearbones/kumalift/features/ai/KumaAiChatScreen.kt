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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bearbones.kumalift.core.ai.AiChatFullscreenImageViewer
import com.bearbones.kumalift.core.ai.AiChatImageThumbnail
import com.bearbones.kumalift.core.ai.AiSecretStore
import com.bearbones.kumalift.core.ai.MAX_AI_CHAT_IMAGES
import com.bearbones.kumalift.core.ai.rememberAiMediaLaunchers
import com.bearbones.kumalift.core.designsystem.DarkColors
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
import com.bearbones.kumalift.core.localization.label
import com.bearbones.kumalift.core.navigation.KumaBackHandler
import com.bearbones.kumalift.core.util.NumberFormatUtils
import com.bearbones.kumalift.data.repository.SettingsRepository
import com.bearbones.kumalift.domain.ai.AiActionProposal
import com.bearbones.kumalift.domain.ai.AiChatMessage
import com.bearbones.kumalift.domain.ai.AiChatRole
import com.bearbones.kumalift.domain.ai.AiChatThread
import com.bearbones.kumalift.domain.ai.AiExerciseDiffEntry
import com.bearbones.kumalift.domain.ai.AiExerciseDiffStatus
import com.bearbones.kumalift.domain.ai.AiExercisePreviewItem
import com.bearbones.kumalift.domain.ai.AiFieldChangePreview
import com.bearbones.kumalift.domain.ai.AiNutritionCardData
import com.bearbones.kumalift.domain.ai.AiOperationKind
import com.bearbones.kumalift.domain.ai.AiOperationPreviewDetail
import com.bearbones.kumalift.domain.ai.AiProposalStatus
import com.bearbones.kumalift.domain.ai.AiProposedOperation
import com.bearbones.kumalift.domain.ai.AiWebSource
import com.bearbones.kumalift.domain.ai.KumaAiAgentService
import com.bearbones.kumalift.domain.ai.copyableContent
import com.bearbones.kumalift.domain.model.AppLanguage
import com.bearbones.kumalift.domain.model.Equipment
import com.bearbones.kumalift.domain.model.MuscleGroup
import com.bearbones.kumalift.domain.model.TrainingGoal
import com.bearbones.kumalift.domain.model.WeightUnit
import com.bearbones.kumalift.domain.model.localizeSplitCategory
import kotlinx.coroutines.delay
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
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

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
    var editingMessageId by rememberSaveable { mutableStateOf<String?>(null) }
    var inspectedProposalMsgId by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    KumaBackHandler(
        enabled = fullscreenPreviewState != null ||
            inspectedProposalMsgId != null ||
            editingMessageId != null ||
            drawerState.isOpen
    ) {
        when {
            fullscreenPreviewState != null -> fullscreenPreviewState = null
            inspectedProposalMsgId != null -> inspectedProposalMsgId = null
            editingMessageId != null -> editingMessageId = null
            drawerState.isOpen -> scope.launch { drawerState.close() }
        }
    }

    val mediaLaunchers = rememberAiMediaLaunchers { newlyPicked ->
        attachedImagePaths = (attachedImagePaths + newlyPicked)
            .distinct()
            .take(MAX_AI_CHAT_IMAGES)
    }

    LaunchedEffect(editingMessageId) {
        val targetId = editingMessageId ?: return@LaunchedEffect
        val idx = messages.indexOfFirst { it.id == targetId }
        if (idx >= 0) {
            listState.animateScrollToItem(idx)
        }
    }

    LaunchedEffect(messages.size, isWorking) {
        val lastIdx = (messages.size + (if (isWorking) 1 else 0) - 1).coerceAtLeast(0)
        if (lastIdx > 0 && editingMessageId == null) {
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

    inspectedProposalMsgId?.let { targetMsgId ->
        val inspectedMsg = messages.firstOrNull { it.id == targetMsgId }
        val inspectedProposal = inspectedMsg?.proposal
        if (inspectedMsg == null || inspectedProposal == null) {
            inspectedProposalMsgId = null
        } else {
            ProposalDetailDialog(
                proposal = inspectedProposal,
                weightUnit = settings.units.weightUnit,
                language = settings.appearance.language,
                resolvePreview = { op ->
                    agentService.resolveOperationPreview(op, inspectedProposal.status, s)
                },
                onApprove = { agentService.approveProposal(inspectedMsg.id, s) },
                onCancel = { agentService.cancelProposal(inspectedMsg.id) },
                onDismiss = { inspectedProposalMsgId = null }
            )
        }
    }

    pendingDeleteThread?.let { targetThread ->
        KumaDeleteConfirmationDialog(
            title = s.aiDeleteThreadTitle,
            message = s.aiDeleteThreadMessage(targetThread.title),
            confirmText = s.delete,
            cancelText = s.cancel,
            onConfirm = {
                pendingDeleteThread = null
                if (editingMessageId != null && currentThreadId == targetThread.id) {
                    editingMessageId = null
                }
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
                        editingMessageId = null
                        composerText = ""
                        attachedImagePaths = emptyList()
                        greetingIndex = (greetingIndex + Random.nextInt(1, 5)) % 5
                        agentService.startNewChat(temporary = false)
                        scope.launch { drawerState.close() }
                    },
                    onToggleTemporary = { enabled ->
                        editingMessageId = null
                        composerText = ""
                        attachedImagePaths = emptyList()
                        greetingIndex = (greetingIndex + Random.nextInt(1, 5)) % 5
                        agentService.setTemporaryChat(enabled)
                        scope.launch { drawerState.close() }
                    },
                    onSelectThread = { tid ->
                        editingMessageId = null
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
                            editingMessageId = null
                            composerText = ""
                            attachedImagePaths = emptyList()
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
                            editingMessageId = null
                            composerText = ""
                            attachedImagePaths = emptyList()
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

                itemsIndexed(messages, key = { _, msg -> msg.id }) { idx, msg ->
                    val hasPrecedingUserMessage = remember(messages, idx) {
                        messages.take(idx).any { it.role == AiChatRole.USER }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.widthIn(max = 820.dp).fillMaxWidth()) {
                            if (msg.role == AiChatRole.USER) {
                                UserChatBubble(
                                    message = msg,
                                    isEditing = editingMessageId == msg.id,
                                    canEdit = !isWorking,
                                    onStartEdit = { editingMessageId = msg.id },
                                    onCancelEdit = { editingMessageId = null },
                                    onSaveEdit = { newText ->
                                        focusManager.clearFocus(force = true)
                                        keyboardController?.hide()
                                        val started = agentService.editUserMessage(
                                            messageId = msg.id,
                                            newText = newText,
                                            strings = s,
                                            language = settings.appearance.language
                                        )
                                        if (started) {
                                            editingMessageId = null
                                        }
                                    },
                                    onImageClick = { paths, imgIdx ->
                                        fullscreenPreviewState = paths to imgIdx
                                    }
                                )
                            } else {
                                AssistantChatMessageBlock(
                                    message = msg,
                                    showRegenerate = hasPrecedingUserMessage,
                                    regenerateEnabled = !isWorking,
                                    onRegenerate = {
                                        editingMessageId = null
                                        agentService.regenerateResponse(
                                            assistantMessageId = msg.id,
                                            strings = s,
                                            language = settings.appearance.language
                                        )
                                    },
                                    onImageClick = { paths, imgIdx ->
                                        fullscreenPreviewState = paths to imgIdx
                                    },
                                    onInspectProposal = { inspectedProposalMsgId = msg.id },
                                    onApproveProposal = { agentService.approveProposal(msg.id, s) },
                                    onCancelProposal = { agentService.cancelProposal(msg.id) }
                                )
                            }
                        }
                    }
                }

                if (isWorking) {
                    item(key = "live_working_indicator") {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(modifier = Modifier.widthIn(max = 820.dp).fillMaxWidth()) {
                                AssistantThinkingBubble(
                                    statusLabel = liveSteps.lastOrNull() ?: s.aiThinkingStatus
                                )
                            }
                        }
                    }
                }
            }
        }

        // Persistent AI Disclaimer Footer (subtle, non-scrolling, right above composer dock)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(KumaColors.CanvasLowest)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = s.aiDisclaimerText,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 760.dp),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = colors.textSubtle,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            )
        }

        // Bottom Composer Dock
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(KumaColors.SurfaceDim)
                .border(1.dp, colors.border)
                .imePadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
        Column(
            modifier = Modifier
                .widthIn(max = 820.dp)
                .fillMaxWidth()
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
                                focusManager.clearFocus(force = true)
                                keyboardController?.hide()
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
private fun AiMessageActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color? = null,
    label: String? = null
) {
    val colors = LocalKumaColors.current
    val haptic = LocalHapticFeedback.current
    val (interactionSource, pressScale) = rememberKumaPressScale(enabled = enabled, targetScale = 0.90f)
    val resolvedTint = tint ?: if (enabled) colors.textSubtle else colors.textSubtle.copy(alpha = 0.4f)

    Row(
        modifier = modifier
            .heightIn(min = 28.dp)
            .kumaScale(pressScale)
            .clip(PillShape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        KumaAnimatedIcon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = resolvedTint,
            modifier = Modifier.size(14.dp)
        )
        AnimatedVisibility(visible = !label.isNullOrBlank()) {
            Text(
                text = label.orEmpty(),
                maxLines = 1,
                softWrap = false,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = resolvedTint,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
private fun CopyMessageActionButton(
    textToCopy: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = textToCopy.isNotBlank()
) {
    val colors = LocalKumaColors.current
    val s = LocalStrings.current
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    var isCopied by remember(textToCopy) { mutableStateOf(false) }

    LaunchedEffect(isCopied) {
        if (isCopied) {
            delay(1600L)
            isCopied = false
        }
    }

    AiMessageActionButton(
        icon = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
        contentDescription = s.aiCopyMessageDesc,
        enabled = enabled,
        tint = if (isCopied) colors.accentText else if (enabled) colors.textSubtle else colors.textSubtle.copy(alpha = 0.4f),
        label = if (isCopied) s.aiCopiedFeedback else null,
        modifier = modifier,
        onClick = {
            if (textToCopy.isNotBlank()) {
                clipboardManager.setText(AnnotatedString(textToCopy))
                isCopied = true
            }
        }
    )
}

@Composable
private fun UserChatBubble(
    message: AiChatMessage,
    isEditing: Boolean,
    canEdit: Boolean,
    onStartEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdit: (String) -> Unit,
    onImageClick: (List<String>, Int) -> Unit
) {
    val colors = LocalKumaColors.current
    val s = LocalStrings.current
    val imgs = message.effectiveImagePaths

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (isEditing) {
            var draftField by remember(message.id, message.text) {
                mutableStateOf(
                    TextFieldValue(
                        text = message.text,
                        selection = TextRange(message.text.length)
                    )
                )
            }
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(message.id) {
                focusRequester.requestFocus()
            }
            val canSave = canEdit && (draftField.text.isNotBlank() || imgs.isNotEmpty())

            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 560.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(KumaColors.SurfaceContainerHigh)
                    .border(1.5.dp, colors.accentFillBorder, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (imgs.isNotEmpty()) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        imgs.forEachIndexed { idx, path ->
                            AiChatImageThumbnail(
                                imagePath = path,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clickable { onImageClick(imgs, idx) }
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(KumaColors.SurfaceContainer)
                        .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    if (draftField.text.isEmpty()) {
                        Text(
                            text = s.aiComposerPlaceholder,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = colors.textSubtle
                            )
                        )
                    }
                    BasicTextField(
                        value = draftField,
                        onValueChange = { draftField = it },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = colors.textPrimary,
                            lineHeight = 20.sp
                        ),
                        cursorBrush = SolidColor(colors.accentFill),
                        maxLines = 8,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onCancelEdit,
                        shape = PillShape,
                        modifier = Modifier.heightIn(min = 36.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = s.cancel,
                            maxLines = 1,
                            softWrap = false,
                            color = colors.textPrimary,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                        )
                    }

                    Button(
                        onClick = { onSaveEdit(draftField.text) },
                        enabled = canSave,
                        shape = PillShape,
                        modifier = Modifier.heightIn(min = 36.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accentFill,
                            contentColor = colors.onAccentFill
                        )
                    ) {
                        Text(
                            text = s.aiEditSaveAction,
                            maxLines = 1,
                            softWrap = false,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(KumaColors.SurfaceContainerHigh)
                    .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChatMessageMediaGallery(
                    images = imgs,
                    singleImageHeight = 170.dp,
                    onImageClick = onImageClick
                )
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

            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CopyMessageActionButton(
                    textToCopy = message.text,
                    enabled = message.text.isNotBlank()
                )
                AiMessageActionButton(
                    icon = Icons.Default.Edit,
                    contentDescription = s.aiEditMessageDesc,
                    enabled = canEdit,
                    onClick = onStartEdit
                )
            }
        }
    }
}

@Composable
private fun ChatMessageMediaGallery(
    images: List<String>,
    singleImageHeight: androidx.compose.ui.unit.Dp,
    onImageClick: (List<String>, Int) -> Unit
) {
    if (images.size == 1) {
        AiChatImageThumbnail(
            imagePath = images.first(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(singleImageHeight)
                .clickable { onImageClick(images, 0) }
        )
    } else if (images.size > 1) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            images.chunked(3).forEachIndexed { rowIdx, rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    rowItems.forEachIndexed { colIdx, path ->
                        val globalIdx = rowIdx * 3 + colIdx
                        AiChatImageThumbnail(
                            imagePath = path,
                            contentDescription = null,
                            modifier = Modifier
                                .size(92.dp)
                                .clickable { onImageClick(images, globalIdx) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AssistantChatMessageBlock(
    message: AiChatMessage,
    showRegenerate: Boolean,
    regenerateEnabled: Boolean,
    onRegenerate: () -> Unit,
    onImageClick: (List<String>, Int) -> Unit,
    onInspectProposal: () -> Unit,
    onApproveProposal: () -> Unit,
    onCancelProposal: () -> Unit
) {
    val colors = LocalKumaColors.current
    val s = LocalStrings.current
    val uriHandler = LocalUriHandler.current
    val copyText = remember(message.id, message.text, message.nutritionCard, message.proposal) {
        message.copyableContent()
    }
    val richContent = remember(message.id, message.text, message.effectiveImagePaths) {
        extractAssistantRichContent(message.text, message.effectiveImagePaths)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        message.nutritionCard?.let { nutrition ->
            NutritionAnalysisCard(data = nutrition)
        }

        if (richContent.images.isNotEmpty() && !message.isError) {
            Box(
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(KumaColors.SurfaceContainer)
                    .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                    .padding(10.dp)
            ) {
                ChatMessageMediaGallery(
                    images = richContent.images,
                    singleImageHeight = 200.dp,
                    onImageClick = onImageClick
                )
            }
        }

        if (richContent.cleanText.isNotBlank()) {
            val codeBg = KumaColors.SurfaceContainerHigh
            val linkColor = colors.accentText
            val markdownBlocks = remember(message.id, richContent.cleanText, message.isError) {
                if (message.isError) {
                    listOf(ChatMarkdownBlock.TextBlock(richContent.cleanText))
                } else {
                    splitChatMarkdownBlocks(richContent.cleanText)
                }
            }
            Box(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (message.isError) colors.errorTint else KumaColors.SurfaceContainer)
                    .border(
                        1.dp,
                        if (message.isError) colors.error.copy(alpha = 0.45f) else colors.border,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    markdownBlocks.forEach { block ->
                        when (block) {
                            is ChatMarkdownBlock.TextBlock -> {
                                val formattedText = remember(
                                    message.id,
                                    block.text,
                                    message.isError,
                                    codeBg,
                                    linkColor,
                                    uriHandler
                                ) {
                                    if (message.isError) {
                                        AnnotatedString(block.text)
                                    } else {
                                        parseChatMarkdown(
                                            raw = block.text,
                                            codeBg = codeBg,
                                            linkColor = linkColor,
                                            onOpenUrl = { url -> runCatching { uriHandler.openUri(url) } }
                                        )
                                    }
                                }
                                Text(
                                    text = formattedText,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (message.isError) colors.error else colors.textPrimary,
                                        lineHeight = 20.sp
                                    )
                                )
                            }
                            is ChatMarkdownBlock.TableBlock -> {
                                MarkdownTableBlock(
                                    table = block,
                                    codeBg = codeBg,
                                    linkColor = linkColor,
                                    onOpenUrl = { url -> runCatching { uriHandler.openUri(url) } }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (richContent.webLinks.isNotEmpty() && !message.isError) {
            FlowRow(
                modifier = Modifier.widthIn(max = 520.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                richContent.webLinks.forEach { link ->
                    Row(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(KumaColors.SurfaceContainerHigh)
                            .border(1.dp, colors.border, PillShape)
                            .clickable { runCatching { uriHandler.openUri(link.url) } }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = colors.accentText,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = link.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.widthIn(max = 180.dp)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = s.aiOpenLinkDesc,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        message.proposal?.let { prop ->
            ProposalReviewCard(
                proposal = prop,
                onInspect = onInspectProposal,
                onApprove = onApproveProposal,
                onCancel = onCancelProposal
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CopyMessageActionButton(
                textToCopy = copyText,
                enabled = copyText.isNotBlank()
            )
            if (showRegenerate) {
                AiMessageActionButton(
                    icon = Icons.Default.Refresh,
                    contentDescription = s.aiRegenerateDesc,
                    enabled = regenerateEnabled,
                    onClick = onRegenerate
                )
            }
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
    onInspect: () -> Unit,
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
            .clickable(onClick = onInspect)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            KumaBadge(
                text = badgeText,
                backgroundColor = badgeBg,
                contentColor = badgeFg
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = s.aiPreviewInspectHint,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = colors.textSubtle,
                        fontSize = 11.sp
                    )
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.textSubtle,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        proposal.operations.forEach { op ->
            val (opBg, opFg, opIcon) = when (op.operationKind) {
                AiOperationKind.ADD -> Triple(colors.recoveryFresh.copy(alpha = 0.2f), colors.recoveryFresh, Icons.Default.Add)
                AiOperationKind.UPDATE -> Triple(colors.warningTint, colors.warning, Icons.Default.EditNote)
                AiOperationKind.DELETE -> Triple(colors.errorTint, colors.error, Icons.Default.Remove)
            }
            val diffHighlight = remember(op.id, op.previewDetail) {
                val diffs = op.previewDetail?.exerciseDiffs.orEmpty().filter { it.status != AiExerciseDiffStatus.UNCHANGED }
                val firstDiff = diffs.firstOrNull()
                when {
                    firstDiff?.status == AiExerciseDiffStatus.REPLACED && firstDiff.before != null && firstDiff.after != null ->
                        "${firstDiff.before.name} -> ${firstDiff.after.name}"
                    firstDiff?.status == AiExerciseDiffStatus.MODIFIED && firstDiff.after != null ->
                        firstDiff.after.name
                    firstDiff?.status == AiExerciseDiffStatus.ADDED && firstDiff.after != null ->
                        "+ ${firstDiff.after.name}"
                    firstDiff?.status == AiExerciseDiffStatus.REMOVED && firstDiff.before != null ->
                        "- ${firstDiff.before.name}"
                    else -> null
                }
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
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
                    if (!diffHighlight.isNullOrBlank()) {
                        Text(
                            text = diffHighlight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = colors.accentText,
                                fontWeight = FontWeight.Medium
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

@Composable
private fun ProposalDetailDialog(
    proposal: AiActionProposal,
    weightUnit: WeightUnit,
    language: AppLanguage,
    resolvePreview: (AiProposedOperation) -> AiOperationPreviewDetail,
    onApprove: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalKumaColors.current
    val s = LocalStrings.current

    val (badgeText, badgeBg, badgeFg) = when (proposal.status) {
        AiProposalStatus.PENDING -> Triple(s.aiReviewProposalBadge, colors.warningTint, colors.warning)
        AiProposalStatus.APPLIED -> Triple(s.aiAppliedBadge, colors.accentTint, colors.accentTextOnTint)
        AiProposalStatus.CANCELLED -> Triple(s.aiCancelledBadge, colors.surfaceBright, colors.textSecondary)
        AiProposalStatus.FAILED -> Triple(s.aiFailedBadge, colors.errorTint, colors.error)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 560.dp)
                .heightIn(max = 680.dp),
            shape = RoundedCornerShape(20.dp),
            color = KumaColors.SurfaceDim,
            tonalElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KumaBadge(
                        text = badgeText,
                        backgroundColor = badgeBg,
                        contentColor = badgeFg
                    )
                    Text(
                        text = s.aiPreviewModalTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = s.close,
                            tint = colors.textSecondary
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.border))

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    proposal.operations.forEach { op ->
                        val preview = remember(op.id, proposal.status) { resolvePreview(op) }
                        ProposalOperationDetailSection(
                            op = op,
                            preview = preview,
                            weightUnit = weightUnit,
                            language = language
                        )
                    }

                    proposal.resultNote?.takeIf { it.isNotBlank() }?.let { note ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (proposal.status == AiProposalStatus.FAILED) colors.errorTint
                                    else colors.accentTint
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                text = note,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (proposal.status == AiProposalStatus.FAILED) colors.error else colors.accentTextOnTint
                                )
                            )
                        }
                    }
                }

                if (proposal.status == AiProposalStatus.PENDING) {
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.border))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onCancel()
                                onDismiss()
                            },
                            modifier = Modifier
                                .weight(0.85f)
                                .heightIn(min = 44.dp),
                            shape = PillShape
                        ) {
                            Text(
                                text = s.aiProposalCancelAction,
                                maxLines = 1,
                                softWrap = false,
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
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProposalOperationDetailSection(
    op: AiProposedOperation,
    preview: AiOperationPreviewDetail,
    weightUnit: WeightUnit,
    language: AppLanguage
) {
    val colors = LocalKumaColors.current
    val s = LocalStrings.current
    val changedDiffs = preview.exerciseDiffs.filter { it.status != AiExerciseDiffStatus.UNCHANGED }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = op.title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
        )

        // Routine Metadata & Field Changes
        val routineHeaderName = preview.routineName ?: preview.beforeRoutineName
        val effectiveMetaMeta = buildList {
            preview.splitCategory?.takeIf { it.isNotBlank() }?.let { add(localizeSplitCategory(it, s)) }
            preview.durationMinutes?.let { add(s.aiPreviewDurationMinutes(it)) }
            if (preview.scheduledDays.isNotEmpty()) {
                val daysStr = preview.scheduledDays.mapNotNull { d -> s.dayNamesShort.getOrNull(d - 1) }.joinToString(", ")
                if (daysStr.isNotBlank()) add(daysStr)
            }
        }
        if (!routineHeaderName.isNullOrBlank() && effectiveMetaMeta.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(KumaColors.SurfaceContainer)
                    .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = routineHeaderName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
                Text(
                    text = effectiveMetaMeta.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                )
            }
        }

        if (preview.fieldChanges.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                preview.fieldChanges.forEach { fc ->
                    FieldChangePreviewRow(fc = fc, weightUnit = weightUnit, language = language)
                }
            }
        }

        // Before vs After Exercise Diffs
        if (changedDiffs.isNotEmpty()) {
            Text(
                text = s.aiPreviewChangesSection,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary
                )
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                changedDiffs.forEach { diff ->
                    ExerciseDiffCard(diff = diff, weightUnit = weightUnit, language = language)
                }
            }
        }

        // Complete Updated Exercise List
        if (preview.updatedExercises.isNotEmpty()) {
            val statusByExerciseId = changedDiffs.mapNotNull { d ->
                d.after?.exerciseId?.takeIf { it.isNotBlank() }?.let { it to d.status }
            }.toMap()
            Text(
                text = s.aiPreviewCompleteListSection,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary
                )
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                preview.updatedExercises.forEachIndexed { idx, ex ->
                    ExercisePreviewSummaryRow(
                        index = idx + 1,
                        item = ex,
                        diffStatus = statusByExerciseId[ex.exerciseId],
                        weightUnit = weightUnit,
                        language = language
                    )
                }
            }
        }

        if (preview.fieldChanges.isEmpty() && changedDiffs.isEmpty() && preview.updatedExercises.isEmpty()) {
            Text(
                text = s.aiPreviewNoChangesDetail,
                style = MaterialTheme.typography.bodySmall.copy(color = colors.textSubtle)
            )
        }
    }
}

@Composable
private fun FieldChangePreviewRow(
    fc: AiFieldChangePreview,
    weightUnit: WeightUnit,
    language: AppLanguage
) {
    val colors = LocalKumaColors.current
    val s = LocalStrings.current
    val label = when (fc.fieldKey) {
        "routine_name" -> s.aiPreviewFieldRoutineName
        "split" -> s.aiPreviewFieldSplit
        "duration" -> s.aiPreviewFieldDuration
        "scheduled_days" -> s.aiPreviewFieldScheduleDays
        "bodyweight" -> s.aiPreviewFieldBodyweight
        "goal" -> s.aiPreviewFieldGoal
        "schedule_date" -> s.aiPreviewFieldScheduleDate
        "event_status" -> s.aiPreviewFieldEventStatus
        "weeks" -> s.aiPreviewFieldWeeks
        "deload_week" -> s.aiPreviewFieldDeloadWeek
        "exercise" -> s.aiPreviewFieldExercise
        "category" -> s.aiPreviewFieldCategory
        "equipment" -> s.aiPreviewFieldEquipment
        "notes" -> s.aiPreviewFieldNotes
        else -> fc.fieldKey
    }
    fun formatFieldVal(raw: String?, weightKg: Double?): String? {
        if (weightKg != null) return NumberFormatUtils.formatWeight(weightKg, weightUnit, language)
        if (raw.isNullOrBlank()) return null
        return when (fc.fieldKey) {
            "split" -> localizeSplitCategory(raw, s)
            "duration" -> raw.toIntOrNull()?.let { s.aiPreviewDurationMinutes(it) } ?: raw
            "scheduled_days" -> raw.split(",").mapNotNull { it.trim().toIntOrNull() }
                .mapNotNull { d -> s.dayNamesShort.getOrNull(d - 1) }
                .joinToString(", ")
                .ifBlank { s.aiStatusRestDay }
            "goal" -> TrainingGoal.entries.firstOrNull { it.name == raw }?.label(s) ?: raw
            "category" -> MuscleGroup.entries.firstOrNull { it.name == raw }?.label(s) ?: raw
            "equipment" -> Equipment.entries.firstOrNull { it.name == raw }?.label(s) ?: raw
            "event_status" -> when (raw) {
                "CANCELLED" -> s.aiCancelledBadge
                "DELETED" -> s.delete
                "PLANNED" -> s.aiStatusScheduled
                else -> raw
            }
            "weeks", "deload_week" -> raw.toIntOrNull()?.let { s.mesocycleWeekShort(it) } ?: raw
            else -> raw
        }
    }

    val beforeDisplay = formatFieldVal(fc.beforeValue, fc.beforeWeightKg)
    val afterDisplay = formatFieldVal(fc.afterValue, fc.afterWeightKg).orEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(KumaColors.SurfaceContainer)
            .border(1.dp, colors.border, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = colors.textSubtle,
                fontWeight = FontWeight.Medium
            )
        )
        if (!beforeDisplay.isNullOrBlank() && beforeDisplay != afterDisplay) {
            Text(
                text = "${s.aiPreviewBeforeLabel}: $beforeDisplay",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = colors.textSecondary,
                    textDecoration = TextDecoration.LineThrough
                )
            )
            Text(
                text = "${s.aiPreviewAfterLabel}: $afterDisplay",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = colors.accentText,
                    fontWeight = FontWeight.SemiBold
                )
            )
        } else {
            Text(
                text = afterDisplay,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

@Composable
private fun ExerciseDiffCard(
    diff: AiExerciseDiffEntry,
    weightUnit: WeightUnit,
    language: AppLanguage
) {
    val colors = LocalKumaColors.current
    val s = LocalStrings.current
    val (statusLabel, badgeBg, badgeFg) = diffBadgeColors(diff.status, colors, s)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(KumaColors.SurfaceContainer)
            .border(1.dp, badgeFg.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        KumaBadge(text = statusLabel, backgroundColor = badgeBg, contentColor = badgeFg)

        diff.before?.let { beforeItem ->
            ExerciseComparisonBlock(
                prefixLabel = if (diff.after != null) s.aiPreviewBeforeLabel else null,
                item = beforeItem,
                isMuted = diff.status == AiExerciseDiffStatus.REMOVED || diff.after != null,
                weightUnit = weightUnit,
                language = language
            )
        }
        diff.after?.let { afterItem ->
            ExerciseComparisonBlock(
                prefixLabel = if (diff.before != null) s.aiPreviewAfterLabel else null,
                item = afterItem,
                isMuted = false,
                weightUnit = weightUnit,
                language = language
            )
        }
    }
}

@Composable
private fun ExerciseComparisonBlock(
    prefixLabel: String?,
    item: AiExercisePreviewItem,
    isMuted: Boolean,
    weightUnit: WeightUnit,
    language: AppLanguage
) {
    val colors = LocalKumaColors.current
    val s = LocalStrings.current
    val configSummary = formatExerciseConfigLine(item, weightUnit, language, s)
    val categorySummary = formatExerciseCategoryLine(item, s)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(KumaColors.SurfaceContainerHigh)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (!prefixLabel.isNullOrBlank()) {
            Text(
                text = prefixLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isMuted) colors.textSubtle else colors.accentText,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        Text(
            text = item.name,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = if (isMuted) colors.textSecondary else colors.textPrimary,
                textDecoration = if (isMuted) TextDecoration.LineThrough else TextDecoration.None
            )
        )
        if (categorySummary.isNotBlank()) {
            Text(
                text = categorySummary,
                style = MaterialTheme.typography.labelSmall.copy(color = colors.textSubtle)
            )
        }
        Text(
            text = configSummary,
            style = MaterialTheme.typography.bodySmall.copy(
                color = if (isMuted) colors.textSubtle else colors.textSecondary
            )
        )
        if (item.setDetails.isNotEmpty() && !isMuted) {
            item.setDetails.forEach { setCfg ->
                val setW = setCfg.weightKg?.takeIf { it > 0.0 }?.let {
                    NumberFormatUtils.formatWeight(it, weightUnit, language)
                }
                Text(
                    text = s.aiPreviewSetLine(setCfg.setNumber, setCfg.reps, setW),
                    style = MaterialTheme.typography.labelSmall.copy(color = colors.textSubtle)
                )
            }
        }
    }
}

@Composable
private fun ExercisePreviewSummaryRow(
    index: Int,
    item: AiExercisePreviewItem,
    diffStatus: AiExerciseDiffStatus?,
    weightUnit: WeightUnit,
    language: AppLanguage
) {
    val colors = LocalKumaColors.current
    val s = LocalStrings.current
    val categorySummary = formatExerciseCategoryLine(item, s)
    val configSummary = formatExerciseConfigLine(item, weightUnit, language, s)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(KumaColors.SurfaceContainer)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .padding(11.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(KumaColors.SurfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$index",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary
                )
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                )
                if (diffStatus != null && diffStatus != AiExerciseDiffStatus.UNCHANGED) {
                    val (label, bg, fg) = diffBadgeColors(diffStatus, colors, s)
                    Spacer(modifier = Modifier.width(6.dp))
                    KumaBadge(text = label, backgroundColor = bg, contentColor = fg)
                }
            }
            if (categorySummary.isNotBlank()) {
                Text(
                    text = categorySummary,
                    style = MaterialTheme.typography.labelSmall.copy(color = colors.textSubtle)
                )
            }
            Text(
                text = configSummary,
                style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
            )
            if (item.setDetails.isNotEmpty()) {
                item.setDetails.forEach { setCfg ->
                    val setW = setCfg.weightKg?.takeIf { it > 0.0 }?.let {
                        NumberFormatUtils.formatWeight(it, weightUnit, language)
                    }
                    Text(
                        text = s.aiPreviewSetLine(setCfg.setNumber, setCfg.reps, setW),
                        style = MaterialTheme.typography.labelSmall.copy(color = colors.textSubtle)
                    )
                }
            }
        }
    }
}

private fun diffBadgeColors(
    status: AiExerciseDiffStatus,
    colors: KumaColors,
    s: com.bearbones.kumalift.core.localization.AppStrings
): Triple<String, Color, Color> = when (status) {
    AiExerciseDiffStatus.ADDED -> Triple(s.aiPreviewDiffAdded, colors.recoveryFresh.copy(alpha = 0.2f), colors.recoveryFresh)
    AiExerciseDiffStatus.MODIFIED -> Triple(s.aiPreviewDiffModified, colors.warningTint, colors.warning)
    AiExerciseDiffStatus.REPLACED -> Triple(s.aiPreviewDiffReplaced, colors.accentTint, colors.accentTextOnTint)
    AiExerciseDiffStatus.REMOVED -> Triple(s.aiPreviewDiffRemoved, colors.errorTint, colors.error)
    AiExerciseDiffStatus.UNCHANGED -> Triple("", Color.Transparent, colors.textSubtle)
}

private fun formatExerciseCategoryLine(
    item: AiExercisePreviewItem,
    s: com.bearbones.kumalift.core.localization.AppStrings
): String {
    val muscle = MuscleGroup.entries.firstOrNull { it.name == item.primaryMuscleCode }?.label(s)
    val equip = Equipment.entries.firstOrNull { it.name == item.equipmentCode }?.label(s)
    return listOfNotNull(muscle, equip).joinToString(" · ")
}

private fun formatExerciseConfigLine(
    item: AiExercisePreviewItem,
    weightUnit: WeightUnit,
    language: AppLanguage,
    s: com.bearbones.kumalift.core.localization.AppStrings
): String {
    val setsReps = s.aiPreviewSetsRepsFormat(item.sets, item.reps)
    val loadText = item.weightKg?.takeIf { it > 0.0 }?.let {
        NumberFormatUtils.formatWeight(it, weightUnit, language)
    }
    val restText = s.aiPreviewRestSeconds(item.restSeconds)
    return listOfNotNull(setsReps, loadText, restText).joinToString(" · ")
}

private val HEADING_REGEX = Regex("""^\s{0,3}(#{1,6})\s+(.+)$""")
private val BULLET_REGEX = Regex("""^(\s*)[*-]\s+(.+)$""")
private val ORDERED_LIST_REGEX = Regex("""^(\s*)(\d+\.)\s+(.+)$""")
private val MARKDOWN_IMAGE_REGEX = Regex("""!\[([^\]]*)\]\((https?://[^\s()]*(?:\([^\s()]*\)[^\s()]*)*|file:///android_asset/[^\s)]+)\)""")
private val MARKDOWN_LINK_REGEX = Regex("""\[([^\]]+)\]\((https?://[^\s()]*(?:\([^\s()]*\)[^\s()]*)*)\)""")
private val BARE_URL_REGEX = Regex("""https?://[^\s<>\]"']+""")
private val EXCESS_BLANK_LINES_REGEX = Regex("""\n{3,}""")

internal data class AssistantRichContent(
    val cleanText: String,
    val images: List<String>,
    val webLinks: List<AiWebSource>
)

private fun isSupportedWebUrl(url: String): Boolean =
    url.startsWith("https://", ignoreCase = true) || url.startsWith("http://", ignoreCase = true)

private fun isSupportedDisplayImageUrl(url: String): Boolean =
    isSupportedWebUrl(url) || url.startsWith("file:///android_asset/", ignoreCase = true)

private fun trimTrailingUrlPunctuation(raw: String): String {
    var end = raw.length
    while (end > 0 && raw[end - 1] in charArrayOf('.', ',', ';', ':', '!', '?')) {
        end--
    }
    while (end > 0 && raw[end - 1] == ')') {
        val candidate = raw.substring(0, end)
        val opens = candidate.count { it == '(' }
        val closes = candidate.count { it == ')' }
        if (closes > opens) end-- else break
    }
    return raw.substring(0, end)
}

internal fun extractAssistantRichContent(
    rawText: String,
    attachedImages: List<String> = emptyList()
): AssistantRichContent {
    val inlineImages = mutableListOf<String>()
    val cleanText = rawText.replace(MARKDOWN_IMAGE_REGEX) { match ->
        val candidateUrl = match.groupValues[2].trim()
        if (isSupportedDisplayImageUrl(candidateUrl)) {
            inlineImages.add(candidateUrl)
            ""
        } else {
            match.value
        }
    }.replace(EXCESS_BLANK_LINES_REGEX, "\n\n").trim()

    val links = linkedMapOf<String, AiWebSource>()
    MARKDOWN_LINK_REGEX.findAll(cleanText).forEach { match ->
        val label = match.groupValues[1].trim()
        val url = trimTrailingUrlPunctuation(match.groupValues[2].trim())
        if (label.isNotBlank() && isSupportedWebUrl(url)) {
            links.putIfAbsent(url, AiWebSource(title = label, url = url))
        }
    }
    BARE_URL_REGEX.findAll(cleanText).forEach { match ->
        val url = trimTrailingUrlPunctuation(match.value.trim())
        if (isSupportedWebUrl(url) && !links.containsKey(url)) {
            val host = url.removePrefix("https://").removePrefix("http://").substringBefore("/").ifBlank { url }
            links[url] = AiWebSource(title = host, url = url)
        }
    }

    val allImages = (attachedImages + inlineImages)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()

    return AssistantRichContent(
        cleanText = cleanText,
        images = allImages,
        webLinks = links.values.take(8)
    )
}

internal sealed interface ChatMarkdownBlock {
    data class TextBlock(val text: String) : ChatMarkdownBlock
    data class TableBlock(
        val headers: List<String>,
        val rows: List<List<String>>
    ) : ChatMarkdownBlock
}

private val TABLE_SEPARATOR_CELL_REGEX = Regex("""^:?-{2,}:?$""")

private fun parsePipeCells(line: String): List<String>? {
    val trimmed = line.trim()
    if (!trimmed.contains('|')) return null
    val unwrapped = trimmed.removePrefix("|").removeSuffix("|")
    val cells = unwrapped.split('|').map { it.trim() }
    return cells.takeIf { it.size >= 2 }
}

private fun isMarkdownTableSeparatorLine(line: String, expectedCols: Int): Boolean {
    val cells = parsePipeCells(line) ?: return false
    if (cells.size != expectedCols) return false
    return cells.all { cell -> cell.isNotEmpty() && TABLE_SEPARATOR_CELL_REGEX.matches(cell) }
}

internal fun splitChatMarkdownBlocks(raw: String): List<ChatMarkdownBlock> {
    val lines = raw.replace("\r\n", "\n").split("\n")
    val blocks = mutableListOf<ChatMarkdownBlock>()
    val textBuf = mutableListOf<String>()
    var inCodeBlock = false
    var i = 0

    fun flushText() {
        val joined = textBuf.joinToString("\n").trim()
        if (joined.isNotEmpty()) {
            blocks.add(ChatMarkdownBlock.TextBlock(joined))
        }
        textBuf.clear()
    }

    while (i < lines.size) {
        val line = lines[i]
        if (line.trimStart().startsWith("```")) {
            inCodeBlock = !inCodeBlock
            textBuf.add(line)
            i++
            continue
        }
        if (!inCodeBlock && i + 1 < lines.size) {
            val headers = parsePipeCells(line)
            if (headers != null && isMarkdownTableSeparatorLine(lines[i + 1], headers.size)) {
                flushText()
                val rows = mutableListOf<List<String>>()
                var j = i + 2
                while (j < lines.size) {
                    val rowCells = parsePipeCells(lines[j]) ?: break
                    val normalizedRow = List(headers.size) { col -> rowCells.getOrNull(col).orEmpty() }
                    rows.add(normalizedRow)
                    j++
                }
                blocks.add(ChatMarkdownBlock.TableBlock(headers = headers, rows = rows))
                i = j
                continue
            }
        }
        textBuf.add(line)
        i++
    }
    flushText()
    return blocks
}

@Composable
private fun MarkdownTableBlock(
    table: ChatMarkdownBlock.TableBlock,
    codeBg: Color,
    linkColor: Color,
    onOpenUrl: ((String) -> Unit)?
) {
    val colors = LocalKumaColors.current
    val hScroll = rememberScrollState()
    val colCount = table.headers.size.coerceAtLeast(1)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, colors.border, RoundedCornerShape(10.dp))
            .background(KumaColors.SurfaceContainerLowest)
            .horizontalScroll(hScroll)
    ) {
        Column(modifier = Modifier.width(IntrinsicSize.Max)) {
            Row(
                modifier = Modifier
                    .background(KumaColors.SurfaceContainerHigh)
                    .padding(vertical = 8.dp, horizontal = 8.dp)
            ) {
                table.headers.forEachIndexed { colIdx, header ->
                    val formattedHeader = remember(header, codeBg, linkColor) {
                        parseChatMarkdown(header, codeBg, linkColor, onOpenUrl)
                    }
                    Text(
                        text = formattedHeader,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = colors.accentText,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier
                            .widthIn(min = 84.dp, max = 190.dp)
                            .padding(horizontal = 6.dp)
                    )
                    if (colIdx < colCount - 1) {
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
            }
            table.rows.forEachIndexed { rowIdx, row ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.border.copy(alpha = 0.65f))
                )
                Row(
                    modifier = Modifier
                        .background(
                            if (rowIdx % 2 == 1) KumaColors.SurfaceContainer.copy(alpha = 0.55f)
                            else Color.Transparent
                        )
                        .padding(vertical = 7.dp, horizontal = 8.dp)
                ) {
                    for (colIdx in 0 until colCount) {
                        val cell = row.getOrNull(colIdx).orEmpty()
                        val formattedCell = remember(rowIdx, colIdx, cell, codeBg, linkColor) {
                            parseChatMarkdown(cell, codeBg, linkColor, onOpenUrl)
                        }
                        Text(
                            text = formattedCell,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = colors.textPrimary,
                                lineHeight = 18.sp
                            ),
                            modifier = Modifier
                                .widthIn(min = 84.dp, max = 190.dp)
                                .padding(horizontal = 6.dp)
                        )
                        if (colIdx < colCount - 1) {
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Lightweight chat Markdown parser rendering bold, italic, underline, strikethrough,
 * clickable web links, inline/fenced code, headings, quotes, ordered/bullet lists via native Compose AnnotatedString.
 */
internal fun parseChatMarkdown(
    raw: String,
    codeBg: Color,
    linkColor: Color = DarkColors.accentText,
    onOpenUrl: ((String) -> Unit)? = null
): AnnotatedString {
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
                    appendInlineMarkdown(heading.groupValues[2].trim(), codeBg, linkColor, onOpenUrl)
                }
                return@forEach
            }

            val bullet = BULLET_REGEX.matchEntire(line)
            if (bullet != null) {
                val indent = " ".repeat((bullet.groupValues[1].length / 2) * 2)
                append(indent)
                append("• ")
                appendInlineMarkdown(bullet.groupValues[2], codeBg, linkColor, onOpenUrl)
                return@forEach
            }

            val ordered = ORDERED_LIST_REGEX.matchEntire(line)
            if (ordered != null) {
                val indent = " ".repeat((ordered.groupValues[1].length / 2) * 2)
                append(indent)
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                    append("${ordered.groupValues[2]} ")
                }
                appendInlineMarkdown(ordered.groupValues[3], codeBg, linkColor, onOpenUrl)
                return@forEach
            }

            if (trimmedStart.startsWith("> ")) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("▎ ") }
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    appendInlineMarkdown(trimmedStart.drop(2), codeBg, linkColor, onOpenUrl)
                }
                return@forEach
            }

            appendInlineMarkdown(line, codeBg, linkColor, onOpenUrl)
        }
    }
}

private fun AnnotatedString.Builder.appendInlineMarkdown(
    text: String,
    codeBg: Color,
    linkColor: Color,
    onOpenUrl: ((String) -> Unit)?
) {
    val linkStyles = TextLinkStyles(
        style = SpanStyle(
            color = linkColor,
            fontWeight = FontWeight.Medium,
            textDecoration = TextDecoration.Underline
        )
    )
    fun buildSafeUrlAnnotation(url: String): LinkAnnotation.Url =
        if (onOpenUrl != null) {
            LinkAnnotation.Url(
                url = url,
                styles = linkStyles,
                linkInteractionListener = { link ->
                    val targetUrl = (link as? LinkAnnotation.Url)?.url
                    if (targetUrl != null) runCatching { onOpenUrl(targetUrl) }
                }
            )
        } else {
            LinkAnnotation.Url(url = url, styles = linkStyles)
        }
    var i = 0
    while (i < text.length) {
        if (text[i] == '\\' && i + 1 < text.length && text[i + 1] in charArrayOf('*', '_', '~', '`', '[', ']')) {
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

        if (text[i] == '[') {
            val closeBracket = text.indexOf(']', i + 1)
            if (closeBracket > i + 1 && closeBracket + 1 < text.length && text[closeBracket + 1] == '(') {
                val closeParen = findMatchingParen(text, closeBracket + 1)
                if (closeParen > closeBracket + 2) {
                    val label = text.substring(i + 1, closeBracket)
                    val rawUrl = text.substring(closeBracket + 2, closeParen).trim()
                    if (isSupportedWebUrl(rawUrl)) {
                        pushLink(buildSafeUrlAnnotation(rawUrl))
                        appendInlineMarkdown(label, codeBg, linkColor, onOpenUrl)
                        pop()
                        i = closeParen + 1
                        continue
                    }
                }
            }
        }

        if (text.regionMatches(i, "https://", 0, 8, ignoreCase = true) ||
            text.regionMatches(i, "http://", 0, 7, ignoreCase = true)
        ) {
            val match = BARE_URL_REGEX.find(text, i)
            if (match != null && match.range.first == i) {
                val cleanUrl = trimTrailingUrlPunctuation(match.value)
                if (isSupportedWebUrl(cleanUrl)) {
                    pushLink(buildSafeUrlAnnotation(cleanUrl))
                    append(cleanUrl)
                    pop()
                    i += cleanUrl.length
                    continue
                }
            }
        }

        if (text.startsWith("***", i)) {
            val end = findClosingDelimiter(text, "***", i + 3)
            if (end > i + 3) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                    appendInlineMarkdown(text.substring(i + 3, end), codeBg, linkColor, onOpenUrl)
                }
                i = end + 3
                continue
            }
        }

        if (text.startsWith("**", i)) {
            val end = findClosingDelimiter(text, "**", i + 2)
            if (end > i + 2) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    appendInlineMarkdown(text.substring(i + 2, end), codeBg, linkColor, onOpenUrl)
                }
                i = end + 2
                continue
            }
        }

        if (text.startsWith("__", i)) {
            val end = findClosingDelimiter(text, "__", i + 2)
            if (end > i + 2) {
                withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                    appendInlineMarkdown(text.substring(i + 2, end), codeBg, linkColor, onOpenUrl)
                }
                i = end + 2
                continue
            }
        }

        if (text.regionMatches(i, "<u>", 0, 3, ignoreCase = true)) {
            val end = text.indexOf("</u>", i + 3, ignoreCase = true)
            if (end > i + 3) {
                withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                    appendInlineMarkdown(text.substring(i + 3, end), codeBg, linkColor, onOpenUrl)
                }
                i = end + 4
                continue
            }
        }

        if (text.startsWith("~~", i)) {
            val end = findClosingDelimiter(text, "~~", i + 2)
            if (end > i + 2) {
                withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                    appendInlineMarkdown(text.substring(i + 2, end), codeBg, linkColor, onOpenUrl)
                }
                i = end + 2
                continue
            }
        }

        if (text[i] == '*' && i + 1 < text.length && !text[i + 1].isWhitespace() && text[i + 1] != '*') {
            val end = findSingleEmphasisEnd(text, '*', i + 1)
            if (end > i + 1) {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    appendInlineMarkdown(text.substring(i + 1, end), codeBg, linkColor, onOpenUrl)
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
                    appendInlineMarkdown(text.substring(i + 1, end), codeBg, linkColor, onOpenUrl)
                }
                i = end + 1
                continue
            }
        }

        append(text[i])
        i++
    }
}

private fun findMatchingParen(text: String, openParenIdx: Int): Int {
    var depth = 0
    for (idx in openParenIdx until text.length) {
        when (text[idx]) {
            '(' -> depth++
            ')' -> {
                depth--
                if (depth == 0) return idx
            }
            '\n', ' ' -> return -1
        }
    }
    return -1
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
