package com.bearbones.kumalift.features.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.KumaPrimaryButton
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.core.localization.LocalStrings
import com.bearbones.kumalift.data.repository.RoutineRepository
import com.bearbones.kumalift.data.repository.SettingsRepository
import com.bearbones.kumalift.domain.model.ExperienceLevel
import com.bearbones.kumalift.domain.model.RoutineBlueprint
import com.bearbones.kumalift.domain.model.RoutineExerciseConfig
import com.bearbones.kumalift.domain.model.TrainingGoal
import com.bearbones.kumalift.domain.model.TrainingGoalConfig

@Composable
fun OnboardingScreen(
    settingsRepository: SettingsRepository,
    routineRepository: RoutineRepository,
    onComplete: () -> Unit
) {
    val s = LocalStrings.current
    val colors = LocalKumaColors.current

    var currentStep by remember { mutableStateOf(1) } // 1: Goal, 2: Days, 3: Experience

    var selectedGoal by remember { mutableStateOf(TrainingGoal.HYPERTROPHY) }
    var selectedDaysCount by remember { mutableStateOf(4) }
    var selectedDays by remember { mutableStateOf(listOf(1, 3, 5, 6)) }
    var selectedExperience by remember { mutableStateOf(ExperienceLevel.INTERMEDIATE) }

    fun finishOnboarding(createStarterRoutine: Boolean) {
        settingsRepository.update { current ->
            current.copy(
                profile = current.profile.copy(
                    goal = selectedGoal,
                    trainingDaysPerWeek = selectedDaysCount,
                    selectedTrainingDays = selectedDays,
                    experienceLevel = selectedExperience,
                    hasCompletedOnboarding = true
                )
            )
        }

        if (createStarterRoutine) {
            val starterRoutines = when (selectedGoal) {
                TrainingGoal.HYPERTROPHY -> listOf(
                    RoutineBlueprint(
                        id = "starter_push",
                        title = s.routinePushDayTitle,
                        splitCategory = s.presetRoutineCategories.firstOrNull() ?: "PPL Split",
                        muscleFocus = s.muscleFocusPush,
                        estimatedDurationMinutes = 55,
                        targetRpeTier = s.rpeTiers.getOrElse(2) { "RPE 8–9" },
                        plannedSetsPerExercise = 3
                    ),
                    RoutineBlueprint(
                        id = "starter_pull",
                        title = s.routinePullDayTitle,
                        splitCategory = s.presetRoutineCategories.firstOrNull() ?: "PPL Split",
                        muscleFocus = s.muscleFocusPull,
                        estimatedDurationMinutes = 50,
                        targetRpeTier = s.rpeTiers.getOrElse(2) { "RPE 8–9" },
                        plannedSetsPerExercise = 3
                    )
                )
                TrainingGoal.STRENGTH -> listOf(
                    RoutineBlueprint(
                        id = "starter_strength_a",
                        title = s.routineFullBodyTitle,
                        splitCategory = s.presetRoutineCategories.getOrElse(2) { "Full Body" },
                        muscleFocus = s.muscleFocusCompound,
                        estimatedDurationMinutes = 65,
                        targetRpeTier = s.rpeTiers.getOrElse(2) { "Heavy, RPE 8–9" },
                        plannedSetsPerExercise = 4
                    )
                )
                else -> listOf(
                    RoutineBlueprint(
                        id = "starter_general_full",
                        title = s.routineUpperLowerTitle,
                        splitCategory = s.presetRoutineCategories.getOrElse(1) { "Upper / Lower" },
                        muscleFocus = s.muscleFocusConditioning,
                        estimatedDurationMinutes = 45,
                        targetRpeTier = s.rpeTiers.getOrElse(1) { "Moderate, RPE 7–8" },
                        plannedSetsPerExercise = 3
                    )
                )
            }
            starterRoutines.forEach { routineRepository.addRoutine(it) }
        }

        onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KumaColors.CanvasLowest)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            // Header with Skip button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Step Indicator Pills
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(1, 2, 3).forEach { stepIndex ->
                        Box(
                            modifier = Modifier
                                .width(if (stepIndex == currentStep) 28.dp else 10.dp)
                                .height(6.dp)
                                .clip(PillShape)
                                .background(
                                    if (stepIndex <= currentStep) colors.accentFill
                                    else KumaColors.SurfaceContainerHigh
                                )
                        )
                    }
                }

                // Skip button
                Text(
                    text = s.onboardingSkip,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = KumaColors.TextMuted
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { finishOnboarding(createStarterRoutine = false) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Step Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                when (currentStep) {
                    1 -> {
                        // Step 1: Training Goal
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = s.onboardingStepGoal,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colors.textPrimary
                                )
                            )
                            Text(
                                text = s.onboardingGoalDescription,
                                style = MaterialTheme.typography.bodyMedium.copy(color = KumaColors.TextSecondary)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf(
                                TrainingGoal.HYPERTROPHY to Triple(
                                    s.programHypertrophy,
                                    "8–12 Reps · Pembesaran massa otot & volume terarah",
                                    "⚡ Populer"
                                ),
                                TrainingGoal.STRENGTH to Triple(
                                    s.programStrength,
                                    "3–6 Reps · Kekuatan maksimal pada angkatan compound",
                                    null
                                ),
                                TrainingGoal.ENDURANCE to Triple(
                                    s.programEndurance,
                                    "15–20 Reps · Daya tahan otot, stamina & sirkuit",
                                    null
                                ),
                                TrainingGoal.FAT_LOSS to Triple(
                                    s.programFatLoss,
                                    "10–15 Reps · Kepadatan latihan & pembakaran kalori",
                                    null
                                ),
                                TrainingGoal.GENERAL to Triple(
                                    s.programGeneral,
                                    "8–15 Reps · Kebugaran seimbang & mobilitas fisik",
                                    null
                                )
                            ).forEach { (goal, info) ->
                                val isSelected = selectedGoal == goal
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isSelected) colors.accentFill.copy(alpha = 0.12f) else KumaColors.SurfaceContainer)
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) colors.accentFill else Color.Transparent,
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable { selectedGoal = goal }
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = info.first,
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) colors.accentText else colors.textPrimary
                                                    )
                                                )
                                                if (info.third != null) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(PillShape)
                                                            .background(colors.accentFill.copy(alpha = 0.2f))
                                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = info.third!!,
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                color = colors.accentText,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = info.second,
                                                style = MaterialTheme.typography.bodySmall.copy(color = KumaColors.TextMuted)
                                            )
                                        }

                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(colors.accentFill),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = colors.onAccentFill,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // Step 2: Training Days
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = s.onboardingStepDays,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colors.textPrimary
                                )
                            )
                            Text(
                                text = s.onboardingDaysDescription,
                                style = MaterialTheme.typography.bodyMedium.copy(color = KumaColors.TextSecondary)
                            )
                        }

                        // Day count selection row (1 to 7)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            (1..7).forEach { days ->
                                val isSelected = selectedDaysCount == days
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) colors.accentFill else KumaColors.SurfaceContainer)
                                        .clickable {
                                            selectedDaysCount = days
                                            selectedDays = when (days) {
                                                1 -> listOf(1)
                                                2 -> listOf(1, 4)
                                                3 -> listOf(1, 3, 5)
                                                4 -> listOf(1, 3, 5, 6)
                                                5 -> listOf(1, 2, 3, 5, 6)
                                                6 -> listOf(1, 2, 3, 4, 5, 6)
                                                else -> (1..7).toList()
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$days",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isSelected) colors.onAccentFill else colors.textPrimary
                                        )
                                    )
                                }
                            }
                        }

                        // Day Toggle Pills
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = s.onboardingPickTrainingDays,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = KumaColors.TextSecondary
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val dayNames = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
                                (1..7).forEach { dayNum ->
                                    val isPicked = selectedDays.contains(dayNum)
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(if (isPicked) colors.accentFill else KumaColors.SurfaceContainer)
                                            .clickable {
                                                selectedDays = if (isPicked) {
                                                    if (selectedDays.size > 1) selectedDays - dayNum else selectedDays
                                                } else {
                                                    (selectedDays + dayNum).sorted()
                                                }
                                                selectedDaysCount = selectedDays.size
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayNames[dayNum - 1],
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isPicked) colors.onAccentFill else colors.textPrimary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        // Step 3: Experience & Completion
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = s.onboardingStepExp,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colors.textPrimary
                                )
                            )
                            Text(
                                text = s.onboardingExpDescription,
                                style = MaterialTheme.typography.bodyMedium.copy(color = KumaColors.TextSecondary)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf(
                                Triple(
                                    ExperienceLevel.BEGINNER,
                                    s.levelBeginner,
                                    "${s.levelBeginner} · 0–1 yr"
                                ),
                                Triple(
                                    ExperienceLevel.INTERMEDIATE,
                                    s.levelIntermediate,
                                    "${s.levelIntermediate} · 1–3 yr"
                                ),
                                Triple(
                                    ExperienceLevel.ADVANCED,
                                    s.levelAdvanced,
                                    "${s.levelAdvanced} · 3+ yr"
                                )
                            ).forEach { (exp, title, desc) ->
                                val isSelected = selectedExperience == exp
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isSelected) colors.accentFill.copy(alpha = 0.12f) else KumaColors.SurfaceContainer)
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) colors.accentFill else Color.Transparent,
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable { selectedExperience = exp }
                                        .padding(16.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) colors.accentText else colors.textPrimary
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = desc,
                                            style = MaterialTheme.typography.bodySmall.copy(color = KumaColors.TextMuted)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Action Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                if (currentStep < 3) {
                    KumaPrimaryButton(
                        text = s.onboardingNextStep(currentStep),
                        onClick = { currentStep += 1 }
                    )
                } else {
                    KumaPrimaryButton(
                        text = s.onboardingFinish,
                        onClick = { finishOnboarding(createStarterRoutine = true) }
                    )
                }
            }
        }
    }
}
