package com.bearbones.kumalift.app

import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.bearbones.kumalift.core.navigation.KumaBackHandler
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.designsystem.KumaLiftTheme
import com.bearbones.kumalift.data.repository.ExerciseRepository
import com.bearbones.kumalift.data.repository.PersonalRecordRepository
import com.bearbones.kumalift.data.repository.RoutineRepository
import com.bearbones.kumalift.data.repository.SettingsRepository
import com.bearbones.kumalift.data.repository.WorkoutRepository
import com.bearbones.kumalift.data.repository.rememberDatabaseRepository
import com.bearbones.kumalift.data.repository.rememberExerciseRepository
import com.bearbones.kumalift.data.repository.rememberRoutineRepository
import com.bearbones.kumalift.data.repository.rememberSettingsRepository
import com.bearbones.kumalift.domain.model.AppLanguage
import com.bearbones.kumalift.features.home.HomeScreen
import com.bearbones.kumalift.features.library.ExerciseLibraryScreen
import com.bearbones.kumalift.features.logger.ActiveWorkoutLoggerScreen
import com.bearbones.kumalift.features.profile.MeScreen
import com.bearbones.kumalift.features.progress.ProgressAnalyticsScreen
import com.bearbones.kumalift.features.routines.RoutinesScreen
import com.bearbones.kumalift.features.settings.SettingsScreen
import com.bearbones.kumalift.features.summary.WorkoutSummaryScreen

enum class Screen {
    HOME,
    ROUTINES,
    LIBRARY,
    PROGRESS,
    ME,
    ACTIVE_LOGGER,
    SUMMARY,
    SETTINGS
}

@Composable
fun App() {
    val databaseRepo = rememberDatabaseRepository()
    val settingsRepository = rememberSettingsRepository(databaseRepo)
    val exerciseRepository = rememberExerciseRepository(databaseRepo)
    val routineRepository = rememberRoutineRepository(exerciseRepository, databaseRepo)
    val personalRecordRepository = remember(databaseRepo) {
        PersonalRecordRepository(
            initialPrs = databaseRepo.getAllPersonalRecords(),
            databaseRepo = databaseRepo,
            calculationSettingsProvider = { settingsRepository.settings.value.calculations },
            dateFormatProvider = { settingsRepository.settings.value.units.dateFormat }
        )
    }
    val workoutRepository = remember(personalRecordRepository, databaseRepo) {
        WorkoutRepository(
            prRepository = personalRecordRepository,
            databaseRepo = databaseRepo,
            calculationSettingsProvider = { settingsRepository.settings.value.calculations },
            loggingSettingsProvider = { settingsRepository.settings.value.logging },
            staleWorkoutHours = settingsRepository.settings.value.logging.staleWorkoutHours
        )
    }

    val mainTabs = remember { listOf(Screen.HOME, Screen.ROUTINES, Screen.LIBRARY, Screen.PROGRESS, Screen.ME) }
    val pagerState = rememberPagerState(initialPage = 0) { mainTabs.size }
    val coroutineScope = rememberCoroutineScope()

    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    val activeWorkout by workoutRepository.activeWorkout.collectAsState()
    val settings by settingsRepository.settings.collectAsState()
    val isEnglish = settings.appearance.language == AppLanguage.EN

    // Sync pager swipe with currentScreen state
    LaunchedEffect(pagerState.currentPage) {
        val tab = mainTabs[pagerState.currentPage]
        if (currentScreen in mainTabs && currentScreen != tab) {
            currentScreen = tab
        }
    }

    // Sync programmatic navigation with pager state
    LaunchedEffect(currentScreen) {
        if (currentScreen in mainTabs) {
            val targetIndex = mainTabs.indexOf(currentScreen)
            if (targetIndex >= 0 && pagerState.currentPage != targetIndex) {
                pagerState.scrollToPage(targetIndex)
            }
        }
    }

    // T10: Handle back navigation at root level
    val isRootHomeScreen = (currentScreen == Screen.HOME || currentScreen !in listOf(Screen.ACTIVE_LOGGER, Screen.SUMMARY, Screen.SETTINGS)) && pagerState.currentPage == 0
    KumaBackHandler(enabled = !isRootHomeScreen) {
        when (currentScreen) {
            Screen.ACTIVE_LOGGER -> {
                currentScreen = mainTabs[pagerState.currentPage]
            }
            Screen.SUMMARY -> {
                currentScreen = Screen.HOME
                coroutineScope.launch { pagerState.scrollToPage(0) }
            }
            Screen.SETTINGS -> {
                currentScreen = Screen.HOME
            }
            else -> {
                if (pagerState.currentPage > 0) {
                    coroutineScope.launch { pagerState.animateScrollToPage(0) }
                }
            }
        }
    }

    KumaLiftTheme(
        theme = settings.appearance.theme,
        highContrast = settings.appearance.highContrast
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                // Show bottom navigation bar only on main screens (not inside fullscreen active logger, summary, or settings)
                if (currentScreen != Screen.ACTIVE_LOGGER && currentScreen != Screen.SUMMARY && currentScreen != Screen.SETTINGS) {
                    val activeTab = if (currentScreen in mainTabs) mainTabs[pagerState.currentPage] else currentScreen
                    KumaBottomNavigation(
                        currentScreen = activeTab,
                        hasActiveWorkout = activeWorkout != null,
                        isEnglish = isEnglish,
                        onTabSelected = { targetScreen ->
                            currentScreen = targetScreen
                            val targetIndex = mainTabs.indexOf(targetScreen)
                            if (targetIndex >= 0) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(targetIndex)
                                }
                            }
                        },
                        onActiveLoggerClick = { currentScreen = Screen.ACTIVE_LOGGER }
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = paddingValues.calculateBottomPadding())
            ) {
                when (currentScreen) {
                    Screen.ACTIVE_LOGGER -> {
                        ActiveWorkoutLoggerScreen(
                            workoutRepository = workoutRepository,
                            settingsRepository = settingsRepository,
                            onNavigateBack = {
                                currentScreen = mainTabs[pagerState.currentPage]
                            },
                            onFinishWorkout = { currentScreen = Screen.SUMMARY },
                            onAddExerciseClick = {
                                currentScreen = Screen.LIBRARY
                                coroutineScope.launch {
                                    pagerState.scrollToPage(mainTabs.indexOf(Screen.LIBRARY))
                                }
                            }
                        )
                    }
                    Screen.SUMMARY -> {
                        WorkoutSummaryScreen(
                            workout = activeWorkout,
                            workoutRepository = workoutRepository,
                            settingsRepository = settingsRepository,
                            onSaveAndClose = {
                                currentScreen = Screen.HOME
                                coroutineScope.launch {
                                    pagerState.scrollToPage(0)
                                }
                            }
                        )
                    }
                    Screen.SETTINGS -> {
                        SettingsScreen(
                            settingsRepository = settingsRepository,
                            databaseRepo = databaseRepo,
                            workoutRepository = workoutRepository,
                            onNavigateBack = {
                                currentScreen = Screen.HOME
                            }
                        )
                    }
                    else -> {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            when (mainTabs[page]) {
                                Screen.HOME -> HomeScreen(
                                    workoutRepository = workoutRepository,
                                    settingsRepository = settingsRepository,
                                    routineRepository = routineRepository,
                                    personalRecordRepository = personalRecordRepository,
                                    onStartWorkout = { currentScreen = Screen.ACTIVE_LOGGER },
                                    onStartFreestyle = { currentScreen = Screen.ACTIVE_LOGGER },
                                    onOpenSettings = { currentScreen = Screen.SETTINGS }
                                )
                                Screen.ROUTINES -> RoutinesScreen(
                                    routineRepository = routineRepository,
                                    workoutRepository = workoutRepository,
                                    exerciseRepository = exerciseRepository,
                                    settingsRepository = settingsRepository,
                                    onStartRoutine = { currentScreen = Screen.ACTIVE_LOGGER }
                                )
                                Screen.LIBRARY -> ExerciseLibraryScreen(
                                    exerciseRepository = exerciseRepository,
                                    workoutRepository = workoutRepository,
                                    settingsRepository = settingsRepository,
                                    onExerciseSelected = {
                                        if (activeWorkout != null) {
                                            currentScreen = Screen.ACTIVE_LOGGER
                                        }
                                    }
                                )
                                Screen.PROGRESS -> ProgressAnalyticsScreen(
                                    personalRecordRepository = personalRecordRepository,
                                    settingsRepository = settingsRepository,
                                    workoutRepository = workoutRepository
                                )
                                Screen.ME -> MeScreen(
                                    workoutRepository = workoutRepository,
                                    settingsRepository = settingsRepository,
                                    personalRecordRepository = personalRecordRepository,
                                    onNavigateToSettings = { currentScreen = Screen.SETTINGS },
                                    onNavigateToProgress = {
                                        currentScreen = Screen.PROGRESS
                                        coroutineScope.launch {
                                            pagerState.scrollToPage(mainTabs.indexOf(Screen.PROGRESS))
                                        }
                                    },
                                    onNavigateToLibrary = {
                                        currentScreen = Screen.LIBRARY
                                        coroutineScope.launch {
                                            pagerState.scrollToPage(mainTabs.indexOf(Screen.LIBRARY))
                                        }
                                    },
                                    onNavigateToRoutines = {
                                        currentScreen = Screen.ROUTINES
                                        coroutineScope.launch {
                                            pagerState.scrollToPage(mainTabs.indexOf(Screen.ROUTINES))
                                        }
                                    },
                                    onStartWorkout = { currentScreen = Screen.ACTIVE_LOGGER }
                                )
                                else -> Unit
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
    currentScreen: Screen,
    hasActiveWorkout: Boolean,
    isEnglish: Boolean = false,
    onTabSelected: (Screen) -> Unit,
    onActiveLoggerClick: () -> Unit
) {
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
                        text = if (isEnglish) "  WORKOUT IN PROGRESS" else "  SESI LATIHAN BERJALAN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = KumaColors.OnElectricLime,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isEnglish) "RESUME" else "LANJUT",
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
                label = if (isEnglish) "Home" else "Beranda",
                isSelected = currentScreen == Screen.HOME,
                onClick = { onTabSelected(Screen.HOME) }
            )
            NavItem(
                icon = Icons.Default.FitnessCenter,
                label = if (isEnglish) "Workouts" else "Latihan",
                isSelected = currentScreen == Screen.ROUTINES,
                onClick = { onTabSelected(Screen.ROUTINES) }
            )
            NavItem(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                label = if (isEnglish) "Exercises" else "Gerakan",
                isSelected = currentScreen == Screen.LIBRARY,
                onClick = { onTabSelected(Screen.LIBRARY) }
            )
            NavItem(
                icon = Icons.AutoMirrored.Filled.ShowChart,
                label = if (isEnglish) "Progress" else "Progres",
                isSelected = currentScreen == Screen.PROGRESS,
                onClick = { onTabSelected(Screen.PROGRESS) }
            )
            NavItem(
                icon = Icons.Default.Person,
                label = if (isEnglish) "Profile" else "Profil",
                isSelected = currentScreen == Screen.ME,
                onClick = { onTabSelected(Screen.ME) }
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
