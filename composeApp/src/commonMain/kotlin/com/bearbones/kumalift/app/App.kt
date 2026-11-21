package com.bearbones.kumalift.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.bearbones.kumalift.core.localization.LocalStrings
import com.bearbones.kumalift.core.localization.resolveAppStrings
import com.bearbones.kumalift.core.logging.KumaLogger
import com.bearbones.kumalift.core.navigation.KumaBackHandler
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.designsystem.KumaLiftTheme
import com.bearbones.kumalift.data.repository.ExerciseRepository
import com.bearbones.kumalift.data.repository.PersonalRecordRepository
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
import com.bearbones.kumalift.domain.model.AppLanguage
import com.bearbones.kumalift.features.home.HomeScreen
import com.bearbones.kumalift.features.library.ExerciseLibraryScreen
import com.bearbones.kumalift.features.library.rememberLibraryFilterState
import com.bearbones.kumalift.features.logger.ActiveWorkoutLoggerScreen
import com.bearbones.kumalift.features.profile.MeScreen
import com.bearbones.kumalift.features.progress.ProgressAnalyticsScreen
import com.bearbones.kumalift.features.routines.RoutinesScreen
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
            staleWorkoutHours = settingsRepository.settings.value.logging.staleWorkoutHours
        )
    }

    val coroutineScope = rememberCoroutineScope()
    val navigator = rememberAppNavigator()
    val saveableStateHolder = rememberSaveableStateHolder()

    // Hoisted states surviving navigation across tabs and process recreation
    val libraryFilterState = rememberLibraryFilterState()
    val libraryListState = rememberLazyListState()
    val routinesListState = rememberLazyListState()
    val progressListState = rememberLazyListState()

    val activeWorkout by workoutRepository.activeWorkout.collectAsState()
    val settings by settingsRepository.settings.collectAsState()
    val appStrings = resolveAppStrings(settings.appearance.language)

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
                            onStartWorkout = { navigator.push(Overlay.ActiveLogger) },
                            onStartFreestyle = { navigator.push(Overlay.ActiveLogger) },
                            onOpenSettings = { navigator.push(Overlay.Settings) }
                        )
                        Tab.ROUTINES -> RoutinesScreen(
                            routineRepository = routineRepository,
                            workoutRepository = workoutRepository,
                            trainingProgramRepository = trainingProgramRepository,
                            exerciseRepository = exerciseRepository,
                            settingsRepository = settingsRepository,
                            listState = routinesListState,
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
                            onNavigateToSettings = { navigator.push(Overlay.Settings) },
                            onNavigateToProgress = { navigator.selectTab(Tab.PROGRESS) },
                            onNavigateToLibrary = { navigator.selectTab(Tab.LIBRARY) },
                            onNavigateToRoutines = { navigator.selectTab(Tab.ROUTINES) },
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
                if (navigator.currentOverlay != null) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = KumaColors.CanvasLowest
                    ) {
                        when (navigator.currentOverlay) {
                            is Overlay.ActiveLogger -> {
                                ActiveWorkoutLoggerScreen(
                                    workoutRepository = workoutRepository,
                                    settingsRepository = settingsRepository,
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
                                SettingsScreen(
                                    settingsRepository = settingsRepository,
                                    databaseRepo = databaseRepo,
                                    workoutRepository = workoutRepository,
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

@Composable
fun KumaBottomNavigation(
    selectedTab: Tab,
    hasActiveWorkout: Boolean,
    onTabSelected: (Tab) -> Unit,
    onActiveLoggerClick: () -> Unit
) {
    val s = LocalStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(KumaColors.SurfaceDim)
            .navigationBarsPadding()
    ) {
        // Floating Active Session Banner if workout is currently active
        if (hasActiveWorkout) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(KumaColors.ElectricLime)
                    .clickable(onClick = onActiveLoggerClick)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(KumaColors.OnElectricLime)
                    )
                    Text(
                        text = "  ${s.workoutInProgress}",
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

        // Main Navigation Bar with comfortable height & spacing
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
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
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) colors.accentText else colors.textSubtle,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                color = if (isSelected) colors.accentText else colors.textSecondary
            )
        )
    }
}
