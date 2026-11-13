package com.bearbones.kumalift.app

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.KumaLiftTheme
import com.bearbones.kumalift.data.repository.ExerciseRepository
import com.bearbones.kumalift.data.repository.PersonalRecordRepository
import com.bearbones.kumalift.data.repository.RoutineRepository
import com.bearbones.kumalift.data.repository.WorkoutRepository
import com.bearbones.kumalift.features.home.HomeScreen
import com.bearbones.kumalift.features.library.ExerciseLibraryScreen
import com.bearbones.kumalift.features.logger.ActiveWorkoutLoggerScreen
import com.bearbones.kumalift.features.progress.ProgressAnalyticsScreen
import com.bearbones.kumalift.features.routines.RoutinesScreen
import com.bearbones.kumalift.features.summary.WorkoutSummaryScreen

enum class Screen {
    HOME,
    ROUTINES,
    LIBRARY,
    PROGRESS,
    ACTIVE_LOGGER,
    SUMMARY
}

@Composable
fun App() {
    val exerciseRepository = remember { ExerciseRepository() }
    val routineRepository = remember { RoutineRepository() }
    val personalRecordRepository = remember { PersonalRecordRepository() }
    val workoutRepository = remember { WorkoutRepository(personalRecordRepository) }

    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    val activeWorkout by workoutRepository.activeWorkout.collectAsState()

    KumaLiftTheme {
        Scaffold(
            bottomBar = {
                // Show bottom navigation bar only on main screens (not inside fullscreen active logger or summary)
                if (currentScreen != Screen.ACTIVE_LOGGER && currentScreen != Screen.SUMMARY) {
                    KumaBottomNavigation(
                        currentScreen = currentScreen,
                        hasActiveWorkout = activeWorkout != null,
                        onTabSelected = { currentScreen = it },
                        onActiveLoggerClick = { currentScreen = Screen.ACTIVE_LOGGER }
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Crossfade(targetState = currentScreen) { screen ->
                    when (screen) {
                        Screen.HOME -> HomeScreen(
                            workoutRepository = workoutRepository,
                            onStartWorkout = { currentScreen = Screen.ACTIVE_LOGGER },
                            onStartFreestyle = { currentScreen = Screen.ACTIVE_LOGGER }
                        )
                        Screen.ROUTINES -> RoutinesScreen(
                            routineRepository = routineRepository,
                            workoutRepository = workoutRepository,
                            onStartRoutine = { currentScreen = Screen.ACTIVE_LOGGER }
                        )
                        Screen.LIBRARY -> ExerciseLibraryScreen(
                            exerciseRepository = exerciseRepository,
                            workoutRepository = workoutRepository,
                            onExerciseSelected = {
                                if (activeWorkout != null) {
                                    currentScreen = Screen.ACTIVE_LOGGER
                                }
                            }
                        )
                        Screen.PROGRESS -> ProgressAnalyticsScreen(
                            personalRecordRepository = personalRecordRepository
                        )
                        Screen.ACTIVE_LOGGER -> ActiveWorkoutLoggerScreen(
                            workoutRepository = workoutRepository,
                            onNavigateBack = { currentScreen = Screen.HOME },
                            onFinishWorkout = { currentScreen = Screen.SUMMARY },
                            onAddExerciseClick = { currentScreen = Screen.LIBRARY }
                        )
                        Screen.SUMMARY -> WorkoutSummaryScreen(
                            onSaveAndClose = { currentScreen = Screen.HOME }
                        )
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
    onTabSelected: (Screen) -> Unit,
    onActiveLoggerClick: () -> Unit
) {
    Column {
        // Floating Active Session Banner if workout is currently active
        if (hasActiveWorkout) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(KumaColors.ElectricLime)
                    .clickable(onClick = onActiveLoggerClick)
                    .padding(horizontal = 16.dp),
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
                        text = "  ACTIVE WORKOUT IN PROGRESS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = KumaColors.OnElectricLime
                        )
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "RESUME",
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

        // Main Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(KumaColors.SurfaceDim)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            NavItem(
                icon = Icons.Default.Home,
                label = "Home",
                isSelected = currentScreen == Screen.HOME,
                onClick = { onTabSelected(Screen.HOME) }
            )
            NavItem(
                icon = Icons.Default.FitnessCenter,
                label = "Workouts",
                isSelected = currentScreen == Screen.ROUTINES,
                onClick = { onTabSelected(Screen.ROUTINES) }
            )
            NavItem(
                icon = Icons.Default.MenuBook,
                label = "Library",
                isSelected = currentScreen == Screen.LIBRARY,
                onClick = { onTabSelected(Screen.LIBRARY) }
            )
            NavItem(
                icon = Icons.Default.ShowChart,
                label = "Progress",
                isSelected = currentScreen == Screen.PROGRESS,
                onClick = { onTabSelected(Screen.PROGRESS) }
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
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) KumaColors.ElectricLime else KumaColors.TextMuted,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) KumaColors.ElectricLime else KumaColors.TextMuted
            )
        )
    }
}
