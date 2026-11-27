package com.bearbones.kumalift.features.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumalift.core.ai.AiSecretStore
import com.bearbones.kumalift.core.designsystem.KumaCard
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.designsystem.kumaKeyboardAvoidance
import com.bearbones.kumalift.core.localization.LocalStrings
import com.bearbones.kumalift.data.repository.SettingsRepository
import com.bearbones.kumalift.domain.ai.GeminiApiClient
import com.bearbones.kumalift.domain.ai.GeminiApiException
import com.bearbones.kumalift.domain.ai.localizedSummary
import kotlinx.coroutines.launch

@Composable
fun AiSettingsPage(
    settingsRepository: SettingsRepository,
    secretStore: AiSecretStore,
    onNavigateBack: () -> Unit,
    onOpenAiChat: (() -> Unit)? = null
) {
    val settings by settingsRepository.settings.collectAsState()
    val colors = LocalKumaColors.current
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    val geminiClient = remember { GeminiApiClient() }

    var apiKeyInput by remember { mutableStateOf(secretStore.getGeminiApiKey()) }
    val hasStoredKey by secretStore.hasApiKeyFlow.collectAsState()
    var keyVisible by remember { mutableStateOf(false) }
    var preferencesInput by remember(settings.ai.preferencesNote) {
        mutableStateOf(settings.ai.preferencesNote)
    }

    var isTesting by remember { mutableStateOf(false) }
    var testStatusMessage by remember { mutableStateOf<String?>(null) }
    var testStatusSuccess by remember { mutableStateOf<Boolean?>(null) }
    var availableModels by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(hasStoredKey) {
        if (!hasStoredKey) {
            availableModels = emptyList()
            return@LaunchedEffect
        }
        val storedKey = secretStore.getGeminiApiKey()
        if (storedKey.isBlank()) return@LaunchedEffect
        geminiClient.listAvailableModels(storedKey).onSuccess { live ->
            if (live.isNotEmpty()) {
                availableModels = live
                val current = settingsRepository.settings.value.ai.modelId.trim()
                if (!live.contains(current)) {
                    val replacement = GeminiApiClient.pickPreferredChatModel(live)
                    if (replacement != null) {
                        settingsRepository.update { cur ->
                            cur.copy(ai = cur.ai.copy(modelId = replacement))
                        }
                    }
                }
            }
        }
    }

    val modelGroups = remember(availableModels, settings.ai.modelId) {
        GeminiApiClient.categorizeModels(
            models = availableModels,
            activeModelId = settings.ai.modelId
        )
    }
    var expandedRecommended by remember { mutableStateOf(true) }
    var expandedFree by remember { mutableStateOf(false) }
    var expandedPaid by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KumaColors.CanvasLowest)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(KumaColors.SurfaceDim)
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(KumaColors.SurfaceContainer)
                    .clickable(onClick = onNavigateBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = s.back,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = s.aiSettingsTitle,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .kumaKeyboardAvoidance()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section: Gemini API Connection
            item {
                AiSettingsSectionLabel(text = s.aiSectionConnection)
                Spacer(modifier = Modifier.height(8.dp))
                KumaCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = s.aiApiKeyStatusLabel,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textPrimary
                                    )
                                )
                                Text(
                                    text = if (hasStoredKey) s.aiApiKeyConfiguredBadge else s.aiApiKeyMissingBadge,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (hasStoredKey) colors.accentText else KumaColors.TextMuted
                                    )
                                )
                            }
                            if (hasStoredKey) {
                                Text(
                                    text = s.aiClearApiKeyAction,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = colors.error,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            secretStore.clearGeminiApiKey()
                                            apiKeyInput = ""
                                            testStatusMessage = null
                                            testStatusSuccess = null
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }

                        AiRowDivider()

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = s.aiApiKeyLabel,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary
                                )
                            )

                            OutlinedTextField(
                                value = apiKeyInput,
                                onValueChange = {
                                    apiKeyInput = it
                                    testStatusMessage = null
                                    testStatusSuccess = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text(s.aiApiKeyPlaceholder) },
                                singleLine = true,
                                visualTransformation = if (keyVisible) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                trailingIcon = {
                                    IconButton(onClick = { keyVisible = !keyVisible }) {
                                        Icon(
                                            imageVector = if (keyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = colors.textSecondary
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.accentFill,
                                    unfocusedBorderColor = colors.border,
                                    focusedTextColor = colors.textPrimary,
                                    unfocusedTextColor = colors.textPrimary
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Text(
                                text = s.aiApiKeyHelper,
                                style = MaterialTheme.typography.bodySmall.copy(color = KumaColors.TextMuted)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        secretStore.saveGeminiApiKey(apiKeyInput)
                                        apiKeyInput = secretStore.getGeminiApiKey()
                                        testStatusMessage = null
                                        testStatusSuccess = null
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.accentFill,
                                        contentColor = colors.onAccentFill
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Text(
                                        text = s.aiSaveApiKeyAction,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        secretStore.saveGeminiApiKey(apiKeyInput)
                                        val keyToTest = secretStore.getGeminiApiKey()
                                        isTesting = true
                                        testStatusMessage = s.aiTestingConnectionStatus
                                        testStatusSuccess = null
                                        scope.launch {
                                            val res = geminiClient.verifyConnection(
                                                apiKey = keyToTest,
                                                modelId = settings.ai.modelId
                                            )
                                            isTesting = false
                                            if (res.isSuccess) {
                                                val info = res.getOrNull()
                                                if (info != null && info.availableModels.isNotEmpty()) {
                                                    availableModels = info.availableModels
                                                }
                                                testStatusSuccess = true
                                                if (info != null && info.modelAutoSwitched) {
                                                    settingsRepository.update { cur ->
                                                        cur.copy(ai = cur.ai.copy(modelId = info.activeModelId))
                                                    }
                                                    testStatusMessage = s.aiConnectionSuccessSwitched(info.activeModelId)
                                                } else {
                                                    testStatusMessage = s.aiConnectionSuccessStatus
                                                }
                                            } else {
                                                testStatusSuccess = false
                                                val err = res.exceptionOrNull() as? GeminiApiException
                                                testStatusMessage = err.localizedSummary(s)
                                            }
                                        }
                                    },
                                    enabled = !isTesting && apiKeyInput.isNotBlank(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, colors.border),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = KumaColors.SurfaceContainer,
                                        contentColor = colors.textPrimary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    if (isTesting) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = colors.accentText
                                        )
                                    } else {
                                        Text(
                                            text = s.aiTestConnectionAction,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.textPrimary
                                            )
                                        )
                                    }
                                }
                            }

                            testStatusMessage?.let { statusMsg ->
                                val bg = when (testStatusSuccess) {
                                    true -> colors.accentTint
                                    false -> colors.errorTint
                                    null -> KumaColors.SurfaceContainer
                                }
                                val fg = when (testStatusSuccess) {
                                    true -> colors.accentTextOnTint
                                    false -> colors.error
                                    null -> colors.textSecondary
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(bg)
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (testStatusSuccess == true) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = fg,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = statusMsg,
                                        style = MaterialTheme.typography.bodySmall.copy(color = fg)
                                    )
                                }
                            }
                        }

                        AiRowDivider()

                        val uriHandler = LocalUriHandler.current
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    runCatching { uriHandler.openUri(AI_STUDIO_API_KEY_URL) }
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = s.aiGetApiKeyAction,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textPrimary
                                    )
                                )
                                Text(
                                    text = s.aiGetApiKeySubtitle,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = KumaColors.TextMuted
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = colors.accentText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Section: Model Selection
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AiSettingsSectionLabel(text = s.aiSectionModel)
                    Text(
                        text = s.aiActiveModelSummary(settings.ai.modelId),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = colors.textSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                val onSelectModel: (String) -> Unit = { modelId ->
                    settingsRepository.update { cur ->
                        cur.copy(ai = cur.ai.copy(modelId = modelId))
                    }
                }
                KumaCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        ModelDropdownSection(
                            title = s.aiModelGroupRecommended,
                            subtitle = s.aiModelGroupRecommendedDesc,
                            countLabel = s.aiModelCountBadge(modelGroups.recommended.size),
                            models = modelGroups.recommended,
                            selectedModelId = settings.ai.modelId,
                            expanded = expandedRecommended,
                            onToggle = { expandedRecommended = !expandedRecommended },
                            onSelectModel = onSelectModel
                        )

                        if (modelGroups.freeTier.isNotEmpty()) {
                            AiRowDivider()
                            ModelDropdownSection(
                                title = s.aiModelGroupFree,
                                subtitle = s.aiModelGroupFreeDesc,
                                countLabel = s.aiModelCountBadge(modelGroups.freeTier.size),
                                models = modelGroups.freeTier,
                                selectedModelId = settings.ai.modelId,
                                expanded = expandedFree,
                                onToggle = { expandedFree = !expandedFree },
                                onSelectModel = onSelectModel
                            )
                        }

                        if (modelGroups.paidOnly.isNotEmpty()) {
                            AiRowDivider()
                            ModelDropdownSection(
                                title = s.aiModelGroupPaid,
                                subtitle = s.aiModelGroupPaidDesc,
                                countLabel = s.aiModelCountBadge(modelGroups.paidOnly.size),
                                models = modelGroups.paidOnly,
                                selectedModelId = settings.ai.modelId,
                                expanded = expandedPaid,
                                onToggle = { expandedPaid = !expandedPaid },
                                onSelectModel = onSelectModel
                            )
                        }
                    }
                }
            }

            // Section: Persistent Training Constraints
            item {
                AiSettingsSectionLabel(text = s.aiSectionNotes)
                Spacer(modifier = Modifier.height(8.dp))
                KumaCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = preferencesInput,
                            onValueChange = { updated ->
                                preferencesInput = updated
                                settingsRepository.update { cur ->
                                    cur.copy(ai = cur.ai.copy(preferencesNote = updated))
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(s.aiPreferencesPlaceholder) },
                            minLines = 2,
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.accentFill,
                                unfocusedBorderColor = colors.border,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Text(
                            text = s.aiPreferencesHelper,
                            style = MaterialTheme.typography.bodySmall.copy(color = KumaColors.TextMuted)
                        )
                    }
                }
            }

            // Optional navigation row into Kuma AI (clean utilitarian list row, no neon CTA pill)
            if (onOpenAiChat != null) {
                item {
                    KumaCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onOpenAiChat)
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = null,
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = s.aiOpenChatTooltip,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textPrimary
                                    )
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = colors.textSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun AiSettingsSectionLabel(text: String) {
    val colors = LocalKumaColors.current
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            color = colors.accentText,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
    )
}

@Composable
private fun ModelDropdownSection(
    title: String,
    subtitle: String,
    countLabel: String,
    models: List<String>,
    selectedModelId: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    onSelectModel: (String) -> Unit
) {
    val colors = LocalKumaColors.current
    val containsSelected = models.contains(selectedModelId)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                    )
                    Text(
                        text = countLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (containsSelected) colors.accentText else KumaColors.TextMuted,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(color = KumaColors.TextMuted)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KumaColors.SurfaceContainer.copy(alpha = 0.45f))
            ) {
                models.forEachIndexed { index, modelId ->
                    val selected = selectedModelId == modelId
                    AiRowDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectModel(modelId) }
                            .padding(start = 24.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = modelId,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) colors.textPrimary else colors.textSecondary
                            )
                        )
                        if (selected) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = colors.accentText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (index == models.lastIndex) {
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AiRowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(KumaColors.SurfaceContainer)
    )
}

private const val AI_STUDIO_API_KEY_URL = "https://aistudio.google.com/apikey"
