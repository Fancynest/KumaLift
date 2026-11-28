package com.bearbones.kumalift.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.bearbones.kumalift.core.designsystem.KumaFloatingToolbar
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.core.designsystem.kumaScale
import com.bearbones.kumalift.core.designsystem.rememberKumaPressScale
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.bearbones.kumalift.core.ai.rememberAiSecretStore
import com.bearbones.kumalift.core.localization.LocalStrings
import com.bearbones.kumalift.core.localization.resolveAppStrings
import com.bearbones.kumalift.core.logging.KumaLogger
import com.bearbones.kumalift.core.media.deleteProgressPhotoFile
import com.bearbones.kumalift.core.navigation.KumaBackHandler
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.designsystem.KumaLiftTheme
import com.bearbones.kumalift.data.repository.ExerciseRepository
import com.bearbones.kumalift.data.repository.PersonalRecordRepository
import com.bearbones.kumalift.data.repository.ProgressPhotoRepository
import com.bearbones.kumalift.data.repository.RoutineRepository
import com.bearbones.kumalift.data.repository.SettingsRepository
import com.bearbones.kumalift.data.repository.WorkoutRepository
import com.bearbones.kumalift.data.repository.WorkoutScheduleRepository
import com.bearbones.kumalift.data.repository.MesocycleRepository
import com.bearbones.kumalift.data.repository.DailyReadinessRepository
import com.bearbones.kumalift.data.repository.rememberDatabaseRepository
import com.bearbones.kumalift.data.repository.rememberExerciseRepository
import com.bearbones.kumalift.data.repository.rememberRoutineRepository
import com.bearbones.kumalift.data.repository.rememberSettingsRepository
import com.bearbones.kumalift.data.repository.rememberTrainingProgramRepository
import com.bearbones.kumalift.domain.ai.GeminiApiClient
import com.bearbones.kumalift.domain.ai.KumaAiAgentService
import com.bearbones.kumalift.domain.ai.KumaAiToolExecutor
import com.bearbones.kumalift.domain.model.AppLanguage
import com.bearbones.kumalift.domain.model.WorkoutPlanDeepLinkBus
import com.bearbones.kumalift.features.ai.KumaAiChatScreen
import com.bearbones.kumalift.features.home.HomeScreen
import com.bearbones.kumalift.features.library.ExerciseLibraryScreen
import com.bearbones.kumalift.features.library.rememberLibraryFilterState
import com.bearbones.kumalift.features.logger.ActiveWorkoutLoggerScreen
import com.bearbones.kumalift.features.profile.MeScreen
import com.bearbones.kumalift.features.profile.ProgressPhotosScreen
import com.bearbones.kumalift.features.progress.ProgressAnalyticsScreen
import com.bearbones.kumalift.features.routines.RoutinesScreen
import com.bearbones.kumalift.features.settings.SettingsDestination
import com.bearbones.kumalift.features.settings.SettingsScreen
import com.bearbones.kumalift.features.summary.WorkoutSummaryScreen

@Composable
fun App() {
    val databaseRepo = rememberDatabaseRepository()
    val settingsRepository = rememberSettingsRepository(databaseRepo)
    val exerciseRepository = rememberExerciseRepository(databaseRepo)
    val routineRepository = rememberRoutineRepository(exerciseRepository, databaseRepo)
    val trainingProgramRepository = rememberTrainingProgramRepository(databaseRepo, settingsRepository)
    val personalRecordRepository = remember(databaseRepo) {
        PersonalRecordRepository(
            initialPrs = databaseRepo.getAllPersonalRecords(),
            databaseRepo = databaseRepo,
            calculationSettingsProvider = { settingsRepository.settings.value.calculations },
            dateFormatProvider = { settingsRepository.settings.value.units.dateFormat }
        )
    }
    val progressPhotoRepository = remember(databaseRepo) {
        ProgressPhotoRepository(
            initialPhotos = databaseRepo.getAllProgressPhotos(),
            databaseRepo = databaseRepo,
            deleteLocalFile = { path -> deleteProgressPhotoFile(path) }
        )
    }
    val workoutScheduleRepository = remember(databaseRepo) {
        WorkoutScheduleRepository(databaseRepo)
    }
    val mesocycleRepository = remember(databaseRepo) {
        MesocycleRepository(databaseRepo)
    }
    val dailyReadinessRepository = remember(databaseRepo) {
        DailyReadinessRepository(databaseRepo)
    }
    val workoutRepository = remember(personalRecordRepository, databaseRepo, workoutScheduleRepository, mesocycleRepository) {
        WorkoutRepository(
            prRepository = personalRecordRepository,
            databaseRepo = databaseRepo,
            scheduleRepository = workoutScheduleRepository,
            mesocycleRepository = mesocycleRepository,
            calculationSettingsProvider = { settingsRepository.settings.value.calculations },
            loggingSettingsProvider = { settingsRepository.settings.value.logging },
            staleWorkoutHours = settingsRepository.settings.value.logging.staleWorkoutHours,
            healthSettingsProvider = { settingsRepository.settings.value.health }
        )
    }
    val aiSecretStore = rememberAiSecretStore()
    val geminiApiClient = remember { GeminiApiClient() }
    val aiAgentService = remember(databaseRepo, settingsRepository, aiSecretStore, geminiApiClient) {
        val executor = KumaAiToolExecutor(
            exerciseRepository = exerciseRepository,
            routineRepository = routineRepository,
            trainingProgramRepository = trainingProgramRepository,
            workoutScheduleRepository = workoutScheduleRepository,
            mesocycleRepository = mesocycleRepository,
            dailyReadinessRepository = dailyReadinessRepository,
            personalRecordRepository = personalRecordRepository,
            workoutRepository = workoutRepository,
            settingsRepository = settingsRepository,
            progressPhotoRepository = progressPhotoRepository,
            databaseRepo = databaseRepo
        )
        KumaAiAgentService(
            geminiClient = geminiApiClient,
            toolExecutor = executor,
            secretStore = aiSecretStore,
            settingsRepository = settingsRepository,
            databaseRepo = databaseRepo
        )
    }

    val coroutineScope = rememberCoroutineScope()
    val navigator = rememberAppNavigator()
    val saveableStateHolder = rememberSaveableStateHolder()

    val reloadAllFromDatabase: (Boolean) -> Unit = remember(
        databaseRepo,
        settingsRepository,
        exerciseRepository,
        routineRepository,
        trainingProgramRepository,
        personalRecordRepository,
        progressPhotoRepository,
        workoutScheduleRepository,
        mesocycleRepository,
        dailyReadinessRepository,
        workoutRepository,
        aiAgentService,
        navigator
    ) {
        { resetToOnboarding ->
            val reloadedSettings = databaseRepo.loadUserSettings() ?: com.bearbones.kumalift.domain.model.UserSettings()
            settingsRepository.reload(reloadedSettings)
            exerciseRepository.reloadFromDatabase()
            routineRepository.reloadFromDatabase { id -> exerciseRepository.getExerciseById(id) }
            trainingProgramRepository.reloadFromDatabase(reloadedSettings.profile.activeProgramId)
            personalRecordRepository.reloadFromDatabase()
            progressPhotoRepository.reloadFromDatabase()
            workoutScheduleRepository.reloadFromDatabase()
            mesocycleRepository.reloadFromDatabase()
            dailyReadinessRepository.reloadFromDatabase()
            workoutRepository.reloadFromDatabase()
            aiAgentService.reloadFromDatabase()
            if (resetToOnboarding) {
                navigator.clearOverlays()
                navigator.selectTab(Tab.HOME)
            }
        }
    }

    // Hoisted states surviving navigation across tabs and process recreation
    val libraryFilterState = rememberLibraryFilterState()
    val libraryListState = rememberLazyListState()
    val routinesListState = rememberLazyListState()
    val progressListState = rememberLazyListState()

    val activeWorkout by workoutRepository.activeWorkout.collectAsState()
    val settings by settingsRepository.settings.collectAsState()
    val appStrings = resolveAppStrings(settings.appearance.language)

    val pendingDeepLinkPlan by WorkoutPlanDeepLinkBus.pendingPlanInput.collectAsState()
    LaunchedEffect(pendingDeepLinkPlan, settings.profile.hasCompletedOnboarding) {
        if (!pendingDeepLinkPlan.isNullOrBlank() && settings.profile.hasCompletedOnboarding) {
            navigator.clearOverlays()
            navigator.selectTab(Tab.ROUTINES)
        }
    }

    LaunchedEffect(settings) {
        com.bearbones.kumalift.core.timer.RestTimerCoordinator.userSettingsProvider = { settings }
    }

    LaunchedEffect(settings.security.localLogging) {
        KumaLogger.isEnabled = settings.security.localLogging
    }

    LaunchedEffect(settings.appearance.language) {
        com.bearbones.kumalift.core.notification.syncPlatformNotificationChannels(settings.appearance.language)
    }

    // Root back handler: Screen-specific sub-navigation handlers (e.g. ExerciseDetail,
    // CreateExercise, Settings sub-menus) register their own KumaBackHandler inside their
    // respective composables, which takes priority because they are deeper in the composition tree.
    KumaBackHandler(enabled = navigator.overlays.isNotEmpty() || navigator.selectedTab != Tab.HOME) {
        navigator.back()
    }

    CompositionLocalProvider(
        LocalStrings provides appStrings
    ) {
        KumaLiftTheme(
            theme = settings.appearance.theme,
            highContrast = settings.appearance.highContrast
        ) {
            if (!settings.profile.hasCompletedOnboarding) {
                com.bearbones.kumalift.features.onboarding.OnboardingScreen(
                    settingsRepository = settingsRepository,
                    routineRepository = routineRepository,
                    onComplete = {
                        // Automatically flips hasCompletedOnboarding to true
                    }
                )
            } else {
                Scaffold(
                    containerColor = KumaColors.CanvasLowest,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    // Bottom navigation bar is visible ONLY when no fullscreen overlay is active
                    if (navigator.currentOverlay == null) {
                        KumaBottomNavigation(
                            selectedTab = navigator.selectedTab,
                            hasActiveWorkout = activeWorkout != null,
                            onTabSelected = { targetTab ->
                            if (navigator.selectedTab == targetTab) {
                                // Tapping current tab again: scroll its list to top (never resets filters)
                                when (targetTab) {
                                    Tab.ROUTINES -> coroutineScope.launch { routinesListState.animateScrollToItem(0) }
                                    Tab.LIBRARY -> coroutineScope.launch { libraryListState.animateScrollToItem(0) }
                                    Tab.PROGRESS -> coroutineScope.launch { progressListState.animateScrollToItem(0) }
                                    else -> Unit
                                }
                            } else {
                                navigator.selectTab(targetTab)
                            }
                        },
                        onActiveLoggerClick = {
                            navigator.push(Overlay.ActiveLogger)
                        }
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = paddingValues.calculateBottomPadding())
            ) {
                // Tab content wrapped in SaveableStateProvider so each tab retains scroll and filters
                saveableStateHolder.SaveableStateProvider(navigator.selectedTab) {
                    when (navigator.selectedTab) {
                        Tab.HOME -> HomeScreen(
                            workoutRepository = workoutRepository,
                            settingsRepository = settingsRepository,
                            routineRepository = routineRepository,
                            trainingProgramRepository = trainingProgramRepository,
                            personalRecordRepository = personalRecordRepository,
                            workoutScheduleRepository = workoutScheduleRepository,
                            mesocycleRepository = mesocycleRepository,
                            dailyReadinessRepository = dailyReadinessRepository,
                            onStartWorkout = {
                                if (workoutRepository.activeWorkout.value == null) {
                                    val nowDt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                                    val todayDayOfWeek = nowDt.dayOfWeek.ordinal + 1
                                    val todayRoutine = routineRepository.routines.value.firstOrNull {
                                        it.scheduledDays.contains(todayDayOfWeek)
                                    }
                                    if (todayRoutine != null) {
                                        workoutRepository.startWorkoutFromRoutine(todayRoutine)
                                    } else {
                                        workoutRepository.startEmptyWorkout(appStrings.freestyleWorkoutName)
                                    }
                                }
                                navigator.push(Overlay.ActiveLogger)
                            },
                            onStartFreestyle = {
                                if (workoutRepository.activeWorkout.value == null) {
                                    workoutRepository.startEmptyWorkout(appStrings.freestyleWorkoutName)
                                }
                                navigator.push(Overlay.ActiveLogger)
                            },
                            onOpenSettings = {
                                navigator.pendingSettingsDestinationName = null
                                navigator.push(Overlay.Settings)
                            },
                            onOpenAiChat = { navigator.push(Overlay.AiChat) }
                        )
                        Tab.ROUTINES -> RoutinesScreen(
                            routineRepository = routineRepository,
                            workoutRepository = workoutRepository,
                            trainingProgramRepository = trainingProgramRepository,
                            exerciseRepository = exerciseRepository,
                            settingsRepository = settingsRepository,
                            listState = routinesListState,
                            initialSharedPlanInput = pendingDeepLinkPlan,
                            onConsumeSharedPlanInput = { WorkoutPlanDeepLinkBus.consume() },
                            onStartRoutine = { navigator.push(Overlay.ActiveLogger) }
                        )
                        Tab.LIBRARY -> ExerciseLibraryScreen(
                            exerciseRepository = exerciseRepository,
                            workoutRepository = workoutRepository,
                            settingsRepository = settingsRepository,
                            filterState = libraryFilterState,
                            listState = libraryListState,
                            onExerciseSelected = {
                                if (activeWorkout != null) {
                                    navigator.push(Overlay.ActiveLogger)
                                }
                            }
                        )
                        Tab.PROGRESS -> ProgressAnalyticsScreen(
                            personalRecordRepository = personalRecordRepository,
                            settingsRepository = settingsRepository,
                            workoutRepository = workoutRepository,
                            listState = progressListState
                        )
                        Tab.ME -> MeScreen(
                            workoutRepository = workoutRepository,
                            settingsRepository = settingsRepository,
                            personalRecordRepository = personalRecordRepository,
                            progressPhotoRepository = progressPhotoRepository,
                            onNavigateToSettings = { navigator.push(Overlay.Settings) },
                            onNavigateToProgress = { navigator.selectTab(Tab.PROGRESS) },
                            onNavigateToLibrary = { navigator.selectTab(Tab.LIBRARY) },
                            onNavigateToRoutines = { navigator.selectTab(Tab.ROUTINES) },
                            onOpenProgressPhotos = { navigator.push(Overlay.ProgressPhotos) },
                            onStartWorkout = {
                                if (activeWorkout == null) {
                                    val nowDt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                                    val todayDayOfWeek = nowDt.dayOfWeek.ordinal + 1
                                    val todayRoutine = routineRepository.routines.value.firstOrNull {
                                        it.scheduledDays.contains(todayDayOfWeek)
                                    }
                                    if (todayRoutine != null) {
                                        workoutRepository.startWorkoutFromRoutine(todayRoutine)
                                    } else {
                                        workoutRepository.startEmptyWorkout(appStrings.freestyleWorkoutName)
                                    }
                                }
                                navigator.push(Overlay.ActiveLogger)
                            }
                        )
                    }
                }

                // Fullscreen overlays rendered above tab content
                AnimatedContent(
                    targetState = navigator.currentOverlay,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
                        val enteringOverlay = targetState
                        val exitingOverlay = initialState
                        val isVertical = enteringOverlay is Overlay.ActiveLogger ||
                            enteringOverlay is Overlay.Summary ||
                            (enteringOverlay == null && (exitingOverlay is Overlay.ActiveLogger || exitingOverlay is Overlay.Summary))
                        when {
                            targetState != null && initialState == null -> {
                                val enter = if (isVertical) {
                                    slideInVertically(
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                    ) { it / 5 } + fadeIn(tween(200))
                                } else {
                                    slideInHorizontally(
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                    ) { it / 4 } + fadeIn(tween(180))
                                }
                                enter togetherWith fadeOut(tween(120))
                            }
                            targetState == null && initialState != null -> {
                                val exit = if (isVertical) {
                                    slideOutVertically(tween(200)) { it / 5 } + fadeOut(tween(180))
                                } else {
                                    slideOutHorizontally(tween(180)) { it / 4 } + fadeOut(tween(160))
                                }
                                fadeIn(tween(120)) togetherWith exit
                            }
                            else -> {
                                (slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { it / 5 } + fadeIn(tween(180))) togetherWith
                                    (slideOutHorizontally(tween(150)) { -it / 6 } + fadeOut(tween(150)))
                            }
                        }
                    }
                ) { overlayState ->
                    if (overlayState != null) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = KumaColors.CanvasLowest
                        ) {
                            val currentOverlay: Overlay? = overlayState
                            when (currentOverlay) {
                                is Overlay.ActiveLogger -> {
                                ActiveWorkoutLoggerScreen(
                                    workoutRepository = workoutRepository,
                                    settingsRepository = settingsRepository,
                                    exerciseRepository = exerciseRepository,
                                    onNavigateBack = { navigator.back() },
                                    onFinishWorkout = {
                                        navigator.popOverlay()
                                        navigator.push(Overlay.Summary)
                                    },
                                    onAddExerciseClick = {
                                        navigator.popOverlay()
                                        navigator.selectTab(Tab.LIBRARY)
                                    }
                                )
                            }
                            is Overlay.Summary -> {
                                val lastCompleted = workoutRepository.completedWorkouts.collectAsState().value.firstOrNull()
                                WorkoutSummaryScreen(
                                    workout = activeWorkout ?: lastCompleted,
                                    workoutRepository = workoutRepository,
                                    settingsRepository = settingsRepository,
                                    onSaveAndClose = {
                                        navigator.popOverlay()
                                        navigator.selectTab(Tab.HOME)
                                    }
                                )
                            }
                            is Overlay.Settings -> {
                                val initialDest = navigator.pendingSettingsDestinationName?.let { raw ->
                                    runCatching { SettingsDestination.valueOf(raw) }.getOrNull()
                                } ?: SettingsDestination.MAIN
                                SettingsScreen(
                                    settingsRepository = settingsRepository,
                                    databaseRepo = databaseRepo,
                                    workoutRepository = workoutRepository,
                                    secretStore = aiSecretStore,
                                    geminiClient = geminiApiClient,
                                    initialDestination = initialDest,
                                    onDataMutated = reloadAllFromDatabase,
                                    onOpenAiChat = {
                                        navigator.pendingSettingsDestinationName = null
                                        navigator.popOverlay()
                                        if (navigator.currentOverlay != Overlay.AiChat) {
                                            navigator.push(Overlay.AiChat)
                                        }
                                    },
                                    onNavigateBack = {
                                        navigator.pendingSettingsDestinationName = null
                                        navigator.back()
                                    }
                                )
                            }
                            is Overlay.AiChat -> {
                                KumaAiChatScreen(
                                    agentService = aiAgentService,
                                    secretStore = aiSecretStore,
                                    settingsRepository = settingsRepository,
                                    onNavigateBack = { navigator.back() },
                                    onOpenAiSettings = {
                                        navigator.pendingSettingsDestinationName = SettingsDestination.AI_ASSISTANT.name
                                        navigator.push(Overlay.Settings)
                                    }
                                )
                            }
                            is Overlay.ProgressPhotos -> {
                                ProgressPhotosScreen(
                                    progressPhotoRepository = progressPhotoRepository,
                                    settingsRepository = settingsRepository,
                                    onNavigateBack = { navigator.back() }
                                )
                            }
                            null -> Unit
                            }
                        }
                    }
                }
            }
        }
    }
}
}
}

@Composable
fun KumaBottomNavigation(
    selectedTab: Tab,
    hasActiveWorkout: Boolean,
    onTabSelected: (Tab) -> Unit,
    onActiveLoggerClick: () -> Unit
) {
    val s = LocalStrings.current
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Floating Active Session Pill when workout is active
        if (hasActiveWorkout) {
            val infiniteTransition = rememberInfiniteTransition()
            val dotAlpha by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 0.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
            val (activeInteraction, activeScale) = rememberKumaPressScale(targetScale = 0.97f)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .kumaScale(activeScale),
                shape = PillShape,
                color = KumaColors.ElectricLime,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(PillShape)
                        .clickable(
                            interactionSource = activeInteraction,
                            indication = LocalIndication.current
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onActiveLoggerClick()
                        }
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .alpha(dotAlpha)
                                .clip(CircleShape)
                                .background(KumaColors.OnElectricLime)
                        )
                        Text(
                            text = "  ${s.workoutInProgress}",
                            maxLines = 1,
                            softWrap = false,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = KumaColors.OnElectricLime,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = s.resume,
                            maxLines = 1,
                            softWrap = false,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = KumaColors.OnElectricLime
                            )
                        )
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = KumaColors.OnElectricLime,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Main Floating Pill Navigation Toolbar
        KumaFloatingToolbar(
            modifier = Modifier.fillMaxWidth(),
            fillWidth = true,
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NavItem(
                icon = Icons.Default.Home,
                label = s.tabHome,
                isSelected = selectedTab == Tab.HOME,
                onClick = { onTabSelected(Tab.HOME) }
            )
            NavItem(
                icon = Icons.Default.FitnessCenter,
                label = s.tabWorkouts,
                isSelected = selectedTab == Tab.ROUTINES,
                onClick = { onTabSelected(Tab.ROUTINES) }
            )
            NavItem(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                label = s.tabExercises,
                isSelected = selectedTab == Tab.LIBRARY,
                onClick = { onTabSelected(Tab.LIBRARY) }
            )
            NavItem(
                icon = Icons.AutoMirrored.Filled.ShowChart,
                label = s.tabProgress,
                isSelected = selectedTab == Tab.PROGRESS,
                onClick = { onTabSelected(Tab.PROGRESS) }
            )
            NavItem(
                icon = Icons.Default.Person,
                label = s.tabProfile,
                isSelected = selectedTab == Tab.ME,
                onClick = { onTabSelected(Tab.ME) }
            )
        }
    }
}

@Composable
fun NavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = KumaColors.current
    val haptic = LocalHapticFeedback.current
    val (interactionSource, pressScale) = rememberKumaPressScale(targetScale = 0.92f)
    val iconScale = animateFloatAsState(
        targetValue = if (isSelected) 1.10f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    ).value
    val tintColor = animateColorAsState(
        targetValue = if (isSelected) colors.accentText else colors.textSubtle,
        animationSpec = tween(180)
    ).value
    Column(
        modifier = Modifier
            .kumaScale(pressScale)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tintColor,
            modifier = Modifier
                .size(24.dp)
                .kumaScale(iconScale)
        )
        Text(
            text = label,
            maxLines = 1,
            softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                color = if (isSelected) colors.accentText else colors.textSecondary
            )
        )
    }
}
