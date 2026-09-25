package com.example.gymtracker.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.gymtracker.data.PresetExercise
import com.example.gymtracker.data.PresetWorkout
import com.example.gymtracker.data.genId
import com.example.gymtracker.ui.theme.Gym

@Composable
fun PresetsScreen(
    presets: List<PresetWorkout>,
    onSave: (PresetWorkout) -> Unit,
    onDelete: (String) -> Unit,
    onStart: (PresetWorkout) -> Unit,
) {
    var editing by remember { mutableStateOf<PresetWorkout?>(null) }

    fun createNew() {
        editing = PresetWorkout(genId(), "", emptyList())
    }

    editing?.let { current ->
        BackHandler { editing = null }
        PresetEditor(
            preset = current,
            onChange = { editing = it },
            onBack = { editing = null },
            onSave = {
                if (current.name.isNotBlank()) {
                    onSave(current)
                    editing = null
                }
            },
        )
        return
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(Modifier.padding(top = 24.dp, bottom = 4.dp)) {
                Text("SAVED ROUTINES", style = SectionLabel, modifier = Modifier.padding(bottom = 4.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Text("Presets", style = display(30.sp), modifier = Modifier.weight(1f))
                    PrimaryButton(
                        text = "New",
                        onClick = ::createNew,
                        shape = RoundedCornerShape(8.dp),
                        style = body(14.sp, Gym.OnPrimary, FontWeight.SemiBold),
                        icon = { PlusIcon(12.dp, Gym.OnPrimary) },
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }
        }

        if (presets.isEmpty()) {
            item {
                EmptyState("⚡", "No presets yet — create one to quick-start workouts") {
                    Text(
                        "Create preset",
                        style = body(14.sp, Gym.Primary),
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clip(CardShape)
                            .border(1.dp, Gym.InputBorder, CardShape)
                            .clickable(onClick = ::createNew)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }

        items(presets, key = { it.id }) { p ->
            PresetCard(p, onEdit = { editing = p }, onDelete = { onDelete(p.id) }, onStart = { onStart(p) })
        }
    }
}

@Composable
private fun PresetCard(p: PresetWorkout, onEdit: () -> Unit, onDelete: () -> Unit, onStart: () -> Unit) {
    Column(Modifier.fillMaxWidth().card()) {
        Row(
            Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(p.name, style = display(17.sp, spacing = 0.03.em))
                Text(
                    "${p.exercises.size} exercises · ${p.exercises.sumOf { it.defaultSets }} sets total",
                    style = mono(12.sp),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text(
                "Edit",
                style = body(12.sp, Gym.Muted),
                modifier = Modifier
                    .clip(SmallShape)
                    .border(1.dp, Gym.InputBorder, SmallShape)
                    .clickable(onClick = onEdit)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
            Box(
                Modifier.size(32.dp).clip(CircleShape).clickable(onClick = onDelete),
                contentAlignment = Alignment.Center,
            ) {
                CloseIcon(12.dp, Gym.Danger, Modifier.alpha(0.75f))
            }
        }
        Column(
            Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            p.exercises.forEach { ex ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(Gym.InputBorder))
                    Text(ex.name, style = mono(12.sp), modifier = Modifier.weight(1f))
                    Text("${ex.defaultSets}×${ex.defaultReps}", style = mono(12.sp))
                }
            }
        }
        Row(
            Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                .fillMaxWidth()
                .card(CardShape, Gym.Surface, Gym.PrimaryDashed)
                .clickable(onClick = onStart)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayIcon(Gym.PrimaryText)
            Text("Start workout", style = body(14.sp, Gym.PrimaryText, FontWeight.SemiBold))
        }
    }
}

@Composable
private fun PresetEditor(
    preset: PresetWorkout,
    onChange: (PresetWorkout) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val nameFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (preset.name.isEmpty()) nameFocus.requestFocus() }

    fun updateExercise(i: Int, transform: (PresetExercise) -> PresetExercise) =
        onChange(preset.copy(exercises = preset.exercises.mapIndexed { j, e -> if (j == i) transform(e) else e }))

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackButton(onBack)
            BareTextField(
                value = preset.name,
                onValueChange = { onChange(preset.copy(name = it)) },
                placeholder = "Preset name…",
                style = display(18.sp, spacing = 0.04.em),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp).focusRequester(nameFocus),
            )
            PrimaryButton("Save", onClick = onSave, enabled = preset.name.isNotBlank())
        }
        HorizontalDivider(color = Gym.Divider)

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(preset.exercises) { i, ex ->
                Column(Modifier.fillMaxWidth().card().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 12.dp)) {
                    Row(
                        Modifier.padding(bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CategoryChip(ex.category)
                        Text(ex.name, style = display(15.sp, FontWeight.SemiBold), modifier = Modifier.weight(1f))
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable {
                                    onChange(preset.copy(exercises = preset.exercises.filterIndexed { j, _ -> j != i }))
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            CloseIcon(12.dp, Gym.Danger, Modifier.alpha(0.75f))
                        }
                    }
                    EquipmentSelector(
                        equipment = ex.equipment,
                        perSide = ex.perSide,
                        onChange = { e, per -> updateExercise(i) { it.copy(equipment = e, perSide = per) } },
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        PresetField("Sets", ex.defaultSets.toDouble(), decimal = false) { v ->
                            updateExercise(i) { it.copy(defaultSets = v.toInt()) }
                        }
                        PresetField("Reps", ex.defaultReps.toDouble(), decimal = false) { v ->
                            updateExercise(i) { it.copy(defaultReps = v.toInt()) }
                        }
                        PresetField("Weight", ex.defaultWeight, decimal = true) { v ->
                            updateExercise(i) { it.copy(defaultWeight = v) }
                        }
                    }
                }
            }
            item {
                AddExerciseButton { showPicker = true }
            }
        }
    }

    if (showPicker) {
        ExercisePicker(
            onSelect = { name, category, equipment ->
                val added = PresetExercise(name, category, 3, 8, 0.0, equipment, equipment?.defaultPerSide ?: false)
                onChange(preset.copy(exercises = preset.exercises + added))
            },
            onClose = { showPicker = false },
        )
    }
}

@Composable
private fun PresetField(label: String, value: Double, decimal: Boolean, onChange: (Double) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = mono(12.sp))
        NumberField(value = value, onValueChange = onChange, decimal = decimal, modifier = Modifier.width(64.dp))
    }
}
