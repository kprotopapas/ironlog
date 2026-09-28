package com.example.gymtracker.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.gymtracker.data.ExerciseSet
import com.example.gymtracker.data.WeightUnit
import com.example.gymtracker.data.Workout
import com.example.gymtracker.data.WorkoutExercise
import com.example.gymtracker.data.formatDate
import com.example.gymtracker.data.genId
import com.example.gymtracker.ui.theme.Gym

/** Edits a logged workout in a draft; nothing is saved until Save. Back cancels. */
@Composable
fun EditWorkoutScreen(workout: Workout, unit: WeightUnit, onSave: (Workout) -> Unit, onCancel: () -> Unit) {
    var draft by remember(workout.id) { mutableStateOf(workout) }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val exercises = draft.exercises

    BackHandler(onBack = onCancel)

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text("EDITING · ${formatDate(workout.date).uppercase()}", style = SectionLabel)
                    BareTextField(
                        value = draft.name,
                        onValueChange = { draft = draft.copy(name = it) },
                        placeholder = "Workout name",
                        style = display(22.sp, spacing = 0.04.em),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    )
                }
                Text(
                    "Cancel",
                    style = body(12.sp, Gym.Dimmer),
                    modifier = Modifier
                        .clip(SmallShape)
                        .border(1.dp, Gym.CardBorder, SmallShape)
                        .clickable(onClick = onCancel)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
                PrimaryButton(
                    "Save",
                    onClick = { onSave(draft.copy(name = draft.name.ifBlank { "Workout" })) },
                    enabled = draft != workout,
                )
            }
        }
        HorizontalDivider(color = Gym.Divider)

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(exercises, key = { _, ex -> ex.id }) { i, ex ->
                ExerciseCard(
                    ex = ex,
                    unit = unit,
                    onChange = { updated ->
                        draft = draft.copy(exercises = exercises.mapIndexed { j, e -> if (j == i) updated else e })
                    },
                    onRemove = { draft = draft.copy(exercises = exercises.filterIndexed { j, _ -> j != i }) },
                )
            }
            item { AddExerciseButton { showPicker = true } }
        }
    }

    if (showPicker) {
        ExercisePicker(
            onSelect = { name, category, equipment ->
                // Sets added to a past workout were done, so they start ticked.
                draft = draft.copy(
                    exercises = exercises + WorkoutExercise(
                        id = genId(),
                        name = name,
                        category = category,
                        sets = listOf(ExerciseSet(8, 0.0, unit, true)),
                        equipment = equipment,
                        perSide = equipment?.defaultPerSide ?: false,
                    ),
                )
            },
            onClose = { showPicker = false },
        )
    }
}
