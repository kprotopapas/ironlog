package com.example.gymtracker.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.gymtracker.data.WeightUnit
import com.example.gymtracker.data.equipmentSummary
import com.example.gymtracker.data.Workout
import com.example.gymtracker.data.formatDate
import com.example.gymtracker.data.formatDuration
import com.example.gymtracker.data.formatNumber
import com.example.gymtracker.data.formatWeight
import com.example.gymtracker.data.totalVolume
import com.example.gymtracker.ui.theme.Gym

@Composable
fun HistoryScreen(workouts: List<Workout>, unit: WeightUnit, onUpdate: (Workout) -> Unit, onDelete: (String) -> Unit) {
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var editing by rememberSaveable { mutableStateOf(false) }
    val sorted = workouts.sortedByDescending { it.date }
    val workout = sorted.find { it.id == selected }

    if (workout != null && editing) {
        EditWorkoutScreen(
            workout = workout,
            unit = unit,
            onSave = {
                onUpdate(it)
                editing = false
            },
            onCancel = { editing = false },
        )
        return
    }
    if (workout != null) {
        BackHandler { selected = null }
        WorkoutDetail(
            workout = workout,
            unit = unit,
            onBack = { selected = null },
            onEdit = { editing = true },
            onDelete = {
                onDelete(workout.id)
                selected = null
            },
        )
        return
    }

    if (sorted.isEmpty()) {
        FullScreenCenter { EmptyState("📋", "No workouts logged yet") }
        return
    }

    val totalSets = workouts.sumOf { w -> w.exercises.sumOf { it.sets.size } }
    val totalVol = workouts.sumOf { totalVolume(it.exercises) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Column(Modifier.padding(top = 24.dp, bottom = 8.dp)) {
                Text("ALL TIME", style = SectionLabel, modifier = Modifier.padding(bottom = 4.dp))
                Text("History", style = display(30.sp), modifier = Modifier.padding(bottom = 16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "Workouts" to workouts.size.toString(),
                        "Total sets" to totalSets.toString(),
                        "Volume (${unit.label})" to formatNumber(totalVol),
                    ).forEach { (label, value) ->
                        Column(
                            Modifier
                                .weight(1f)
                                .card()
                                .padding(12.dp),
                        ) {
                            Text(label, style = mono(12.sp), maxLines = 1, modifier = Modifier.padding(bottom = 4.dp))
                            Text(value, style = display(20.sp, color = Gym.Primary), maxLines = 1)
                        }
                    }
                }
            }
        }
        items(sorted, key = { it.id }) { w ->
            WorkoutSummaryCard(
                workout = w,
                stats = listOf(
                    "${w.exercises.size} ex",
                    "${w.exercises.sumOf { it.sets.size }} sets",
                    formatDuration(w.duration),
                ),
                onClick = { selected = w.id },
            )
        }
    }
}

@Composable
private fun WorkoutDetail(workout: Workout, unit: WeightUnit, onBack: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackButton(onBack)
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text(workout.name, style = display(18.sp, spacing = 0.03.em))
                Text(formatDate(workout.date), style = mono(12.sp))
            }
            Text(
                "Edit",
                style = body(12.sp, Gym.PrimaryText, FontWeight.Medium),
                modifier = Modifier
                    .clickable(onClick = onEdit)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            )
            Text(
                "Delete",
                style = body(12.sp, Gym.Danger),
                modifier = Modifier
                    .clickable(onClick = onDelete)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            )
        }
        HorizontalDivider(color = Gym.Divider)

        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf(
                "Duration" to formatDuration(workout.duration),
                "Volume" to "${formatNumber(totalVolume(workout.exercises))} ${unit.label}",
                "Exercises" to workout.exercises.size.toString(),
            ).forEach { (label, value) ->
                Column {
                    Text(label, style = mono(12.sp))
                    Text(value, style = mono(14.sp, Gym.Primary, FontWeight.SemiBold), modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
        HorizontalDivider(color = Gym.Divider)

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(workout.exercises, key = { it.id }) { ex ->
                Column(Modifier.fillMaxWidth().card()) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CategoryChip(ex.category)
                        Text(ex.name, style = display(15.sp, FontWeight.SemiBold), modifier = Modifier.weight(1f))
                        equipmentSummary(ex.equipment, ex.perSide)?.let { Text(it, style = mono(12.sp)) }
                    }
                    Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                        ex.sets.forEachIndexed { i, s ->
                            val color = if (s.done) Gym.Primary else Gym.Dimmer
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("${i + 1}", style = mono(12.sp, Gym.Faint), textAlign = TextAlign.Center, modifier = Modifier.width(20.dp))
                                Text("${formatWeight(s.weight)} ${s.unit.label}", style = mono(12.sp, color))
                                Text("×", style = mono(12.sp, Gym.Fainter))
                                Text("${s.reps} reps", style = mono(12.sp, color), modifier = Modifier.weight(1f))
                                if (s.done) Text("✓", style = mono(12.sp, Gym.Primary))
                            }
                            if (i < ex.sets.lastIndex) HorizontalDivider(color = Gym.Divider)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BackButton(onClick: () -> Unit) {
    androidx.compose.foundation.layout.Box(
        Modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        BackIcon(Gym.Muted)
    }
}
