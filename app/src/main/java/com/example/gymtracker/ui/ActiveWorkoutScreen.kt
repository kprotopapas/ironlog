package com.example.gymtracker.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.gymtracker.data.ActiveWorkout
import com.example.gymtracker.data.BodyweightEntry
import com.example.gymtracker.data.Equipment
import com.example.gymtracker.data.ExerciseSet
import com.example.gymtracker.data.Recommendation
import com.example.gymtracker.data.WeightAction
import com.example.gymtracker.data.Workout
import com.example.gymtracker.data.recommend
import com.example.gymtracker.data.weightChange
import com.example.gymtracker.data.WeightUnit
import com.example.gymtracker.data.WorkoutExercise
import com.example.gymtracker.data.formatDuration
import com.example.gymtracker.data.genId
import com.example.gymtracker.ui.theme.Gym
import kotlinx.coroutines.delay

@Composable
fun ActiveWorkoutScreen(
    workout: ActiveWorkout,
    unit: WeightUnit,
    history: List<Workout>,
    bodyweight: List<BodyweightEntry>,
    onChange: (List<WorkoutExercise>) -> Unit,
    onNameChange: (String) -> Unit,
    lastBodyweight: Double?,
    onFinish: (bodyweightToday: Double?) -> Unit,
    onDiscard: () -> Unit,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var confirmFinish by rememberSaveable { mutableStateOf(false) }
    val exercises = workout.exercises
    val allSets = exercises.flatMap { it.sets }
    val doneSets = allSets.count { it.done }
    val totalSets = allSets.size

    // Elapsed time is derived from the start time, so it stays correct across app restarts.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(workout.startTime) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val elapsed = ((now - workout.startTime) / 1000).coerceAtLeast(0)

    fun addExercise(name: String, category: String, equipment: Equipment?) {
        onChange(
            exercises + WorkoutExercise(
                id = genId(),
                name = name,
                category = category,
                sets = listOf(ExerciseSet(8, 0.0, unit, false)),
                equipment = equipment,
                perSide = equipment?.defaultPerSide ?: false,
            )
        )
    }

    Column(Modifier.fillMaxSize()) {
        // Header
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BareTextField(
                    value = workout.name,
                    onValueChange = onNameChange,
                    placeholder = "Workout name",
                    style = display(22.sp, spacing = 0.04.em),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.weight(1f),
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ClockIcon(Gym.Primary)
                    Text(formatDuration(elapsed), style = mono(14.sp, Gym.Primary))
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("$doneSets/$totalSets sets", style = mono(12.sp, Gym.Dimmer))
                    if (totalSets > 0) {
                        val progress by animateFloatAsState(doneSets.toFloat() / totalSets, label = "progress")
                        Box(Modifier.width(96.dp).height(4.dp).clip(RoundedCornerShape(50)).background(Gym.Chip)) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth(progress).clip(RoundedCornerShape(50)).background(Gym.Primary))
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Discard",
                        style = body(12.sp, Gym.Dimmer),
                        modifier = Modifier
                            .clip(SmallShape)
                            .border(1.dp, Gym.CardBorder, SmallShape)
                            .clickable(onClick = onDiscard)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                    PrimaryButton("Finish", onClick = { confirmFinish = true })
                }
            }
        }
        HorizontalDivider(color = Gym.Divider)

        // Exercises
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (exercises.isEmpty()) {
                item {
                    EmptyState("🏋️", "No exercises yet", alpha = 0.5f)
                }
            }
            itemsIndexed(exercises, key = { _, ex -> ex.id }) { i, ex ->
                val rec = remember(history, bodyweight, unit, ex.name, ex.equipment, ex.perSide) { recommend(ex, history, unit, bodyweight) }
                ExerciseCard(
                    ex = ex,
                    unit = unit,
                    recommendation = rec,
                    onChange = { updated -> onChange(exercises.mapIndexed { j, e -> if (j == i) updated else e }) },
                    onRemove = { onChange(exercises.filterIndexed { j, _ -> j != i }) },
                )
            }
            item {
                AddExerciseButton { showPicker = true }
            }
        }
    }

    if (showPicker) ExercisePicker(onSelect = ::addExercise, onClose = { showPicker = false })
    if (confirmFinish) {
        FinishDialog(
            lastBodyweight = lastBodyweight,
            unit = unit,
            onFinish = {
                confirmFinish = false
                onFinish(it)
            },
            onDismiss = { confirmFinish = false },
        )
    }
}

/** Confirms finishing, with an optional weigh-in prefilled from the last one. */
@Composable
private fun FinishDialog(lastBodyweight: Double?, unit: WeightUnit, onFinish: (Double?) -> Unit, onDismiss: () -> Unit) {
    var weight by remember { mutableDoubleStateOf(lastBodyweight ?: 0.0) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().card(background = Gym.Background).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Finish workout?", style = display(22.sp))
            Text("Bodyweight today (optional)", style = mono(12.sp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(value = weight, onValueChange = { weight = it }, modifier = Modifier.width(72.dp))
                Text(unit.label, style = body(12.sp, Gym.Faint))
            }
            Text(
                "Counts as load on pull-ups and other bodyweight exercises.",
                style = body(12.sp, Gym.Dim),
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Finish without",
                    style = body(12.sp, Gym.Dimmer),
                    modifier = Modifier
                        .clip(SmallShape)
                        .border(1.dp, Gym.CardBorder, SmallShape)
                        .clickable { onFinish(null) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
                PrimaryButton("Log & finish", onClick = { if (weight > 0) onFinish(weight) }, enabled = weight > 0)
            }
        }
    }
}

@Composable
fun AddExerciseButton(onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .dashedBorder(Gym.InputBorder, 8.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlusIcon(12.dp, Gym.Dim)
        Text("Add exercise", style = body(14.sp, Gym.Dim, FontWeight.Medium))
    }
}

@Composable
fun ExerciseCard(
    ex: WorkoutExercise,
    unit: WeightUnit,
    onChange: (WorkoutExercise) -> Unit,
    onRemove: () -> Unit,
    recommendation: Recommendation? = null,
) {
    var collapsed by rememberSaveable(ex.id) { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(if (collapsed) -90f else 0f, label = "chevron")

    fun addSet() {
        val last = ex.sets.lastOrNull()
        onChange(ex.copy(sets = ex.sets + ExerciseSet(last?.reps ?: 8, last?.weight ?: 0.0, unit, false)))
    }

    Column(Modifier.fillMaxWidth().card().animateContentSize()) {
        Row(
            Modifier.padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            CategoryChip(ex.category, Gym.Dim)
            Spacer(Modifier.width(8.dp))
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(ex.name, style = display(17.sp, FontWeight.SemiBold), modifier = Modifier.weight(1f, fill = false))
                // Only a change from last session gets a mark; the same weight shows nothing.
                when (recommendation?.let { weightChange(ex, it, unit) }) {
                    1 -> Text("▲", style = mono(11.sp, Gym.Success))
                    -1 -> Text("▼", style = mono(11.sp, Gym.Danger))
                    else -> {}
                }
            }
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(50)).clickable { collapsed = !collapsed },
                contentAlignment = Alignment.Center,
            ) {
                ChevronDownIcon(Gym.Muted, Modifier.rotate(chevronRotation))
            }
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(50)).clickable(onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                CloseIcon(14.dp, Gym.Danger, Modifier.alpha(0.75f))
            }
        }

        if (!collapsed) {
            Column(
                Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                EquipmentSelector(
                    equipment = ex.equipment,
                    perSide = ex.perSide,
                    onChange = { e, per -> onChange(ex.copy(equipment = e, perSide = per)) },
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                // Staying put needs no hint; only a change is worth reading.
                recommendation?.takeIf { it.action != WeightAction.HOLD }?.let { rec ->
                    val pending = ex.sets.withIndex().any { (i, s) -> !s.done && s.weight != rec.nextWeightFor(i) }
                    RecommendationHint(
                        rec = rec,
                        equipment = ex.equipment,
                        unit = unit,
                        modifier = Modifier.padding(bottom = 4.dp),
                        onApply = if (pending) {
                            { onChange(ex.copy(sets = ex.sets.mapIndexed { i, s -> if (s.done) s else s.copy(weight = rec.nextWeightFor(i), unit = unit) })) }
                        } else null,
                    )
                }
                Row(
                    Modifier.padding(start = 13.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Spacer(Modifier.width(20.dp))
                    Text(
                        if (ex.equipment?.isBodyweight == true) "Added" else "Weight",
                        style = mono(12.sp, Gym.Faint),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(56.dp),
                    )
                    Spacer(Modifier.width(if (unit == WeightUnit.KG) 28.dp else 33.dp))
                    Text("Reps", style = mono(12.sp, Gym.Faint), textAlign = TextAlign.Center, modifier = Modifier.width(44.dp))
                }
                ex.sets.forEachIndexed { i, set ->
                    SetRow(
                        set = set,
                        idx = i,
                        unit = unit,
                        onUpdate = { s -> onChange(ex.copy(sets = ex.sets.mapIndexed { j, ss -> if (j == i) s else ss })) },
                        onRemove = { onChange(ex.copy(sets = ex.sets.filterIndexed { j, _ -> j != i })) },
                    )
                }
                Row(
                    Modifier
                        .padding(top = 8.dp)
                        .clip(SmallShape)
                        .dashedBorder(Gym.PrimaryDashed, 4.dp)
                        .clickable(onClick = ::addSet)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    PlusIcon(10.dp, Gym.Primary)
                    Text("Add set", style = body(12.sp, Gym.Primary))
                }
            }
        }
    }
}

@Composable
private fun SetRow(
    set: ExerciseSet,
    idx: Int,
    unit: WeightUnit,
    onUpdate: (ExerciseSet) -> Unit,
    onRemove: () -> Unit,
) {
    val shape = SmallShape
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (set.done) Gym.Primary.copy(alpha = 0.06f) else Gym.Background)
            .border(1.dp, if (set.done) Gym.Primary.copy(alpha = 0.4f) else Gym.CardBorder, shape)
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("${idx + 1}", style = mono(12.sp, Gym.Faint), textAlign = TextAlign.Center, modifier = Modifier.width(20.dp))
        NumberField(
            value = set.weight,
            onValueChange = { onUpdate(set.copy(weight = it)) },
            modifier = Modifier.width(56.dp),
        )
        Text(unit.label, style = body(12.sp, Gym.Faint))
        Text("×", style = body(12.sp, Gym.Fainter))
        NumberField(
            value = set.reps.toDouble(),
            onValueChange = { onUpdate(set.copy(reps = it.toInt())) },
            decimal = false,
            modifier = Modifier.width(44.dp),
        )
        Text("reps", style = body(12.sp, Gym.Faint))
        Spacer(Modifier.weight(1f))
        // Done toggle — a larger touch target around the 24dp box.
        Box(
            Modifier.size(32.dp).clip(SmallShape).clickable { onUpdate(set.copy(done = !set.done)) },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(24.dp)
                    .clip(SmallShape)
                    .background(if (set.done) Gym.Primary else Gym.Background)
                    .border(1.dp, if (set.done) Gym.Primary else Gym.InputBorder, SmallShape),
                contentAlignment = Alignment.Center,
            ) {
                if (set.done) CheckIcon(Gym.OnPrimary)
            }
        }
        Box(
            Modifier.size(24.dp).clip(SmallShape).clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            CloseIcon(10.dp, Gym.Danger, Modifier.alpha(0.75f))
        }
    }
}
