package com.example.gymtracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.gymtracker.data.PresetWorkout
import com.example.gymtracker.data.WeightUnit
import com.example.gymtracker.data.Workout
import com.example.gymtracker.data.formatDate
import com.example.gymtracker.data.formatDuration
import com.example.gymtracker.data.formatNumber
import com.example.gymtracker.data.totalVolume
import com.example.gymtracker.ui.theme.Gym

@Composable
fun LogScreen(
    workouts: List<Workout>,
    presets: List<PresetWorkout>,
    unit: WeightUnit,
    onStartWorkout: () -> Unit,
    onStartFromPreset: (PresetWorkout) -> Unit,
) {
    val recent = workouts.sortedByDescending { it.date }.take(3)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 16.dp)) {
            Text("READY TO TRAIN", style = SectionLabel, modifier = Modifier.padding(bottom = 4.dp))
            Text("Start Workout", style = display(30.sp), modifier = Modifier.padding(bottom = 16.dp))
            PrimaryButton(
                text = "EMPTY WORKOUT",
                onClick = onStartWorkout,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                style = display(18.sp, color = Gym.OnPrimary, spacing = 0.05.em),
                icon = { ArrowRightIcon(Gym.OnPrimary) },
                padding = 16.dp,
            )
        }

        if (presets.isNotEmpty()) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                Text("QUICK START", style = SectionLabel, modifier = Modifier.padding(bottom = 12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.take(4).forEach { p ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .card()
                                .clickable { onStartFromPreset(p) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(p.name, style = display(15.sp, FontWeight.SemiBold, spacing = 0.03.em))
                                Text(
                                    "${p.exercises.size} exercises",
                                    style = mono(12.sp),
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                            ChevronRightIcon(Gym.Primary)
                        }
                    }
                }
            }
        }

        if (recent.isNotEmpty()) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
                Text("RECENT WORKOUTS", style = SectionLabel, modifier = Modifier.padding(bottom = 12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recent.forEach { w ->
                        WorkoutSummaryCard(
                            workout = w,
                            stats = listOf(
                                "${w.exercises.size} exercises",
                                formatDuration(w.duration),
                                "${formatNumber(totalVolume(w.exercises))} ${unit.label} vol",
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutSummaryCard(workout: Workout, stats: List<String>, onClick: (() -> Unit)? = null) {
    Column(
        Modifier
            .fillMaxWidth()
            .card()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.Top) {
            Text(
                workout.name,
                style = display(15.sp, FontWeight.SemiBold, spacing = 0.03.em),
                modifier = Modifier.weight(1f),
            )
            Text(formatDate(workout.date), style = mono(12.sp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            stats.forEach { Text(it, style = mono(12.sp)) }
        }
    }
}
