package com.bearbones.kumalift.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumalift.core.designsystem.KumaBadge
import com.bearbones.kumalift.core.designsystem.KumaCard
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.KumaPrimaryButton
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.data.repository.WorkoutRepository

@Composable
fun HomeScreen(
    workoutRepository: WorkoutRepository,
    onStartWorkout: () -> Unit,
    onStartFreestyle: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KumaColors.CanvasLowest)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header bar with statusBarsPadding
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KumaColors.SurfaceDim)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(KumaColors.SurfaceContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = KumaColors.ElectricLime,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "KUMALIFT",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp,
                            color = KumaColors.TextWhite
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(KumaColors.SurfaceContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = KumaColors.OnSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    // 1. Athlete Header & Greeting
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "WEEK 6 · HYPERTROPHY PHASE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = KumaColors.ElectricLime,
                                    letterSpacing = 1.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Good morning, Alex",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = 24.sp
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(KumaColors.SurfaceContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ElectricBolt,
                                contentDescription = null,
                                tint = KumaColors.ElectricLime,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Metric Chips in a horizontally scrollable LazyRow (zero squeezing)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        item {
                            KumaBadge(
                                text = "4 Day Streak",
                                icon = Icons.Default.LocalFireDepartment
                            )
                        }
                        item {
                            KumaBadge(
                                text = "18.4k kg This Week",
                                icon = Icons.Default.FitnessCenter
                            )
                        }
                        item {
                            KumaBadge(
                                text = "Target Met: 80%",
                                icon = Icons.Default.Verified
                            )
                        }
                    }
                }

                // 2. Weekly Horizon (7-Day Tracker)
                item {
                    KumaCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = KumaColors.SurfaceContainerLow,
                        cornerRadius = 18.dp
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = KumaColors.ElectricLime,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "This Week",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(PillShape)
                                        .background(KumaColors.ElectricLime)
                                        .padding(horizontal = 12.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "4/5 Workouts",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = KumaColors.OnElectricLime,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // 7 Days Grid with comfortable breathing room
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                DayCircle("M", isDone = true)
                                DayCircle("T", isDone = true)
                                DayCircle("W", isRest = true)
                                DayCircle("T", isDone = true)
                                DayCircle("F", isActive = true)
                                DayCircle("S")
                                DayCircle("S")
                            }
                        }
                    }
                }

                // 3. Today's Workout Card (Hero)
                item {
                    KumaCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = KumaColors.SurfaceContainerLow,
                        cornerRadius = 18.dp
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                KumaBadge(text = "RECOMMENDED TODAY")
                                Text(
                                    text = "Heavy, RPE 8–9",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = KumaColors.TextMuted
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Pull Day A",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontSize = 28.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "4 exercises · 16 sets · ~55 min",
                                style = MaterialTheme.typography.bodyMedium,
                                color = KumaColors.TextSecondary
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Exercise Preview Chips
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                KumaBadge(text = "Deadlift")
                                KumaBadge(text = "Barbell Row")
                                KumaBadge(text = "Lat Pulldown")
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            KumaPrimaryButton(
                                text = "START WORKOUT",
                                icon = Icons.Default.PlayArrow,
                                onClick = onStartWorkout
                            )
                        }
                    }
                }

                // 4. Monthly Quick Glance Metrics (Spacious 3-column)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricTile(
                            modifier = Modifier.weight(1f),
                            label = "SESSIONS",
                            value = "18",
                            subtext = "+3 vs last mo"
                        )
                        MetricTile(
                            modifier = Modifier.weight(1f),
                            label = "VOLUME",
                            value = "64.2k",
                            subtext = "+12% tonnage"
                        )
                        MetricTile(
                            modifier = Modifier.weight(1f),
                            label = "PRS",
                            value = "3",
                            subtext = "New records",
                            isAccent = true
                        )
                    }
                }

                // 5. Recent Workouts & Freestyle Button
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Recent Workouts",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )

                        // Recent Workout Item
                        KumaCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = KumaColors.SurfaceContainer,
                            cornerRadius = 16.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Push Day A",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Yesterday · 14,850 kg · 47 min",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = KumaColors.TextMuted
                                    )
                                }
                                KumaBadge(
                                    text = "3RM PR",
                                    isPr = true
                                )
                            }
                        }

                        // Log Empty / Freestyle Action
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(KumaColors.SurfaceContainer)
                                .border(1.dp, KumaColors.BorderDefault, RoundedCornerShape(14.dp))
                                .clickable {
                                    workoutRepository.startEmptyWorkout()
                                    onStartFreestyle()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = KumaColors.ElectricLime,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Log Empty / Freestyle Workout",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = KumaColors.TextWhite,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

@Composable
fun DayCircle(
    day: String,
    isDone: Boolean = false,
    isRest: Boolean = false,
    isActive: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = day,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (isActive) KumaColors.ElectricLime else KumaColors.TextMuted
        )
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isDone -> KumaColors.ElectricLime
                        isActive -> KumaColors.SurfaceContainerHigh
                        else -> KumaColors.SurfaceContainer
                    }
                )
                .border(
                    width = if (isActive) 1.5.dp else 0.dp,
                    color = if (isActive) KumaColors.ElectricLime else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                isDone -> Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = KumaColors.OnElectricLime,
                    modifier = Modifier.size(18.dp)
                )
                isRest -> Icon(
                    imageVector = Icons.Default.Hotel,
                    contentDescription = null,
                    tint = KumaColors.TextMuted,
                    modifier = Modifier.size(14.dp)
                )
                isActive -> Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = KumaColors.ElectricLime,
                    modifier = Modifier.size(18.dp)
                )
                else -> Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(KumaColors.SurfaceContainerHighest)
                )
            }
        }
    }
}

@Composable
fun MetricTile(
    label: String,
    value: String,
    subtext: String,
    modifier: Modifier = Modifier,
    isAccent: Boolean = false
) {
    KumaCard(
        modifier = modifier,
        backgroundColor = KumaColors.SurfaceContainerLow,
        cornerRadius = 14.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = KumaColors.TextMuted,
                    letterSpacing = 0.5.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = if (isAccent) KumaColors.ElectricLime else KumaColors.TextWhite
                ),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = if (isAccent) KumaColors.ElectricLime else KumaColors.TextMuted
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
