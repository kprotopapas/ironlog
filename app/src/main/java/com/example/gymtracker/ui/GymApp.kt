package com.example.gymtracker.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.gymtracker.GymViewModel
import com.example.gymtracker.Tab
import com.example.gymtracker.ui.theme.Gym

@Composable
fun GymApp(vm: GymViewModel) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Gym.Background)
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(Modifier.widthIn(max = 480.dp).fillMaxSize()) {
            TopBar(live = vm.activeWorkout != null, unitLabel = vm.unit.label, onToggleUnit = vm::toggleUnit)

            // Nav
            Box {
                HorizontalDivider(color = Gym.Divider, modifier = Modifier.align(Alignment.BottomCenter))
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Tab.entries.forEach { t ->
                        NavTab(t.label, active = vm.tab == t) { vm.tab = t }
                    }
                }
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (vm.tab) {
                    Tab.LOG -> {
                        val active = vm.activeWorkout
                        if (active != null) {
                            ActiveWorkoutScreen(
                                workout = active,
                                unit = vm.unit,
                                onChange = vm::updateActiveExercises,
                                onNameChange = vm::renameActiveWorkout,
                                onFinish = vm::finishWorkout,
                                onDiscard = vm::discardWorkout,
                            )
                        } else {
                            LogScreen(
                                workouts = vm.workouts,
                                presets = vm.presets,
                                unit = vm.unit,
                                onStartWorkout = { vm.startWorkout() },
                                onStartFromPreset = vm::startFromPreset,
                            )
                        }
                    }
                    Tab.HISTORY -> HistoryScreen(vm.workouts, vm.unit, onDelete = vm::deleteWorkout)
                    Tab.PRESETS -> PresetsScreen(
                        presets = vm.presets,
                        onSave = vm::savePreset,
                        onDelete = vm::deletePreset,
                        onStart = vm::startFromPreset,
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar(live: Boolean, unitLabel: String, onToggleUnit: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("IRONLOG", style = display(18.sp, FontWeight.Black, Gym.Primary, spacing = 0.15.em))
            if (live) {
                val pulse by rememberInfiniteTransition(label = "live").animateFloat(
                    initialValue = 0.18f,
                    targetValue = 0.06f,
                    animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
                    label = "pulse",
                )
                Text(
                    "LIVE",
                    style = mono(12.sp, Gym.PrimaryText, FontWeight.Medium),
                    // Pulse only the tint so the text keeps full contrast.
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Gym.Primary.copy(alpha = pulse))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
        Text(
            unitLabel,
            style = mono(12.sp, Gym.Dim),
            modifier = Modifier
                .clip(SmallShape)
                .border(1.dp, Gym.CardBorder, SmallShape)
                .clickable(onClick = onToggleUnit)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun NavTab(label: String, active: Boolean, onClick: () -> Unit) {
    Text(
        label,
        style = body(14.sp, if (active) Gym.Primary else Gym.Dim, FontWeight.Medium).copy(letterSpacing = 0.05.em),
        modifier = Modifier
            .clickable(onClick = onClick)
            .then(if (active) Modifier.underline(Gym.Primary, 2.dp) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}
