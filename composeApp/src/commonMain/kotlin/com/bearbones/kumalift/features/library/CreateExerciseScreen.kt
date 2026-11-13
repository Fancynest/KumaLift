package com.bearbones.kumalift.features.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumalift.core.designsystem.KumaCard
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.domain.model.Equipment
import com.bearbones.kumalift.domain.model.Exercise
import com.bearbones.kumalift.domain.model.MovementType
import com.bearbones.kumalift.domain.model.MuscleGroup

/**
 * Full-screen Create Exercise screen styled with authentic KumaLift design language.
 * No photo/camera asset uploader per user specification.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateExerciseScreen(
    onNavigateBack: () -> Unit,
    onSaveExercise: (name: String, primaryMuscle: MuscleGroup, equipment: Equipment, secondaryMuscles: List<MuscleGroup>, movementType: MovementType) -> Unit
) {
    val colors = LocalKumaColors.current

    var nameInput by remember { mutableStateOf("") }
    var selectedEquipment by remember { mutableStateOf(Equipment.BARBELL) }
    var selectedPrimaryMuscle by remember { mutableStateOf(MuscleGroup.CHEST) }
    var selectedSecondaryMuscles by remember { mutableStateOf<Set<MuscleGroup>>(emptySet()) }
    var selectedMovementType by remember { mutableStateOf(MovementType.COMPOUND) }
    var hasAttemptedSubmit by remember { mutableStateOf(false) }

    val isNameValid = nameInput.trim().isNotEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KumaColors.CanvasLowest)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar: Back Arrow + Title + Save Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KumaColors.SurfaceDim)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
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
                        contentDescription = "Batal",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = "Create Exercise",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        fontSize = 18.sp
                    ),
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = {
                        hasAttemptedSubmit = true
                        if (isNameValid) {
                            onSaveExercise(
                                nameInput.trim(),
                                selectedPrimaryMuscle,
                                selectedEquipment,
                                selectedSecondaryMuscles.toList(),
                                selectedMovementType
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isNameValid) colors.accentFill else colors.accentFill.copy(alpha = 0.4f),
                        contentColor = colors.onAccentFill
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Save",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            // Scrollable Form Body
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Section 1: Exercise Name
                KumaCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "EXERCISE NAME",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = colors.accentText,
                                letterSpacing = 1.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(KumaColors.SurfaceContainer)
                                .border(
                                    width = 1.dp,
                                    color = if (hasAttemptedSubmit && !isNameValid) Color(0xFFFF5252) else colors.border,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (nameInput.isEmpty()) {
                                Text(
                                    text = "misal: Incline Dumbbell Press",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                                    color = colors.textSecondary.copy(alpha = 0.6f)
                                )
                            }
                            BasicTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 15.sp,
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.Medium
                                ),
                                cursorBrush = SolidColor(colors.accentText),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (hasAttemptedSubmit && !isNameValid) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Nama latihan wajib diisi",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFFF5252),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }

                // Section 2: Equipment
                KumaCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "EQUIPMENT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = colors.accentText,
                                letterSpacing = 1.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Pilih alat yang digunakan untuk gerakan ini",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = colors.textSecondary,
                                fontSize = 12.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Equipment.entries.forEach { eq ->
                                val isSelected = selectedEquipment == eq
                                Box(
                                    modifier = Modifier
                                        .clip(PillShape)
                                        .background(if (isSelected) colors.accentFill else KumaColors.SurfaceContainer)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) colors.accentFill else colors.border,
                                            shape = PillShape
                                        )
                                        .clickable { selectedEquipment = eq }
                                        .padding(horizontal = 14.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = eq.displayName,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) colors.onAccentFill else colors.textPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 3: Primary Muscle Group (Target Muscle)
                KumaCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "PRIMARY MUSCLE GROUP",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = colors.accentText,
                                letterSpacing = 1.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Kelompok otot primer yang menjadi fokus latihan",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = colors.textSecondary,
                                fontSize = 12.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val availableMuscles = MuscleGroup.entries.filter { it != MuscleGroup.FULL_BODY }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            availableMuscles.forEach { muscle ->
                                val isSelected = selectedPrimaryMuscle == muscle
                                val muscleColor = muscleAccentColor(muscle)

                                Box(
                                    modifier = Modifier
                                        .clip(PillShape)
                                        .background(if (isSelected) colors.accentFill else KumaColors.SurfaceContainer)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) colors.accentFill else colors.border,
                                            shape = PillShape
                                        )
                                        .clickable {
                                            selectedPrimaryMuscle = muscle
                                            // Remove from secondary if it was selected there
                                            selectedSecondaryMuscles = selectedSecondaryMuscles - muscle
                                        }
                                        .padding(horizontal = 14.dp, vertical = 7.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) colors.onAccentFill else muscleColor)
                                        )
                                        Text(
                                            text = muscle.displayName,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) colors.onAccentFill else colors.textPrimary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 4: Other Muscles (Secondary Muscles - Optional)
                KumaCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "OTHER MUSCLES (OPTIONAL)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = colors.accentText,
                                letterSpacing = 1.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Pilih otot bantuan yang ikut bekerja saat latihan",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = colors.textSecondary,
                                fontSize = 12.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val secondaryCandidates = MuscleGroup.entries.filter { 
                            it != MuscleGroup.FULL_BODY && it != selectedPrimaryMuscle 
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            secondaryCandidates.forEach { muscle ->
                                val isSelected = selectedSecondaryMuscles.contains(muscle)
                                Box(
                                    modifier = Modifier
                                        .clip(PillShape)
                                        .background(if (isSelected) colors.accentFill.copy(alpha = 0.25f) else KumaColors.SurfaceContainer)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) colors.accentFill else colors.border,
                                            shape = PillShape
                                        )
                                        .clickable {
                                            selectedSecondaryMuscles = if (isSelected) {
                                                selectedSecondaryMuscles - muscle
                                            } else {
                                                selectedSecondaryMuscles + muscle
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 7.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = colors.accentText,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                        Text(
                                            text = muscle.displayName,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) colors.accentText else colors.textPrimary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 5: Exercise Type (Compound vs Isolation)
                KumaCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "EXERCISE TYPE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = colors.accentText,
                                letterSpacing = 1.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Karakteristik sendi dan pergerakan biomekanik",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = colors.textSecondary,
                                fontSize = 12.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf(
                                MovementType.COMPOUND to "Multi-sendi (e.g. Press, Squat, Row)",
                                MovementType.ISOLATION to "Sendi tunggal (e.g. Curl, Fly, Extension)"
                            ).forEach { (type, description) ->
                                val isSelected = selectedMovementType == type
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) colors.accentFill else KumaColors.SurfaceContainer)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) colors.accentFill else colors.border,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedMovementType = type }
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = type.displayName,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) colors.onAccentFill else colors.textPrimary
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = description,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 11.sp,
                                                color = if (isSelected) colors.onAccentFill.copy(alpha = 0.8f) else colors.textSecondary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
