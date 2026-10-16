package com.example.gymtracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.gymtracker.data.GymTimer
import com.example.gymtracker.data.TIMER_NUDGE_SEC
import com.example.gymtracker.data.TIMER_PRESETS_SEC
import com.example.gymtracker.data.formatClock
import com.example.gymtracker.data.typedClock
import com.example.gymtracker.ui.theme.Gym
import com.example.gymtracker.ui.theme.GymSelectionColors
import kotlinx.coroutines.delay

/** Current time, refreshed while [ticking] so a running clock redraws; every frame when [smooth]. */
@Composable
private fun rememberNow(ticking: Boolean, smooth: Boolean = false): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(ticking, smooth) {
        now = System.currentTimeMillis()
        while (ticking) {
            if (smooth) withFrameMillis {} else delay(200)
            now = System.currentTimeMillis()
        }
    }
    return now
}

/** Top-bar entry to the timer; shows the time while one is going so it can be read from any tab. */
@Composable
fun TimerChip(timer: GymTimer, onClick: () -> Unit) {
    val now = rememberNow(timer.running)
    // The phone sleeping mid-rest would hide the countdown and swallow the alert.
    val view = LocalView.current
    DisposableEffect(timer.running) {
        view.keepScreenOn = timer.running
        onDispose { view.keepScreenOn = false }
    }
    val color = if (timer.active) Gym.PrimaryText else Gym.Dim
    Row(
        Modifier
            .clip(SmallShape)
            .background(if (timer.active) Gym.Surface else Gym.Background)
            .border(1.dp, if (timer.active) Gym.PrimaryDashed else Gym.CardBorder, SmallShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ClockIcon(color)
        Text(if (timer.active) timer.label(now) else "Timer", style = mono(12.sp, color, if (timer.active) FontWeight.Medium else FontWeight.Normal))
    }
}

@Composable
fun TimerDialog(timer: GymTimer, onChange: (GymTimer.(now: Long) -> GymTimer) -> Unit, onDismiss: () -> Unit) {
    val now = rememberNow(timer.running, smooth = !timer.countdown)
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().card(background = Gym.Background).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ModeTab("Timer", timer.countdown) { onChange { if (countdown) this else reset().copy(countdown = true) } }
                ModeTab("Stopwatch", !timer.countdown) { onChange { if (!countdown) this else reset().copy(countdown = false) } }
            }

            // Tabular digits so the fast-changing centiseconds don't make the line jitter.
            val clockStyle = display(64.sp, FontWeight.Bold, if (timer.running) Gym.Primary else Gym.Foreground, spacing = 0.02.em)
                .copy(fontFeatureSettings = "tnum", textAlign = TextAlign.Center)
            if (timer.countdown && !timer.active) {
                TypedClock(timer.duration, clockStyle, onChange = { ms -> onChange { withDuration(ms) } })
            } else {
                Text(timer.label(now, centis = true), style = clockStyle, modifier = Modifier.fillMaxWidth())
            }

            if (timer.countdown) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlineButton("−${TIMER_NUDGE_SEC}s") { onChange { nudged(-TIMER_NUDGE_SEC) } }
                    OutlineButton("+${TIMER_NUDGE_SEC}s") { onChange { nudged(TIMER_NUDGE_SEC) } }
                }
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TIMER_PRESETS_SEC.forEach { sec ->
                        val selected = timer.duration == sec * 1000L
                        Text(
                            formatClock(sec * 1000L),
                            style = mono(14.sp, if (selected) Gym.OnPrimary else Gym.Foreground, FontWeight.Medium),
                            modifier = Modifier
                                .clip(SmallShape)
                                .background(if (selected) Gym.Primary else Gym.Chip)
                                .clickable { onChange { reset().withDuration(sec * 1000L) } }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (timer.active) OutlineButton("Reset") { onChange { reset() } }
                val canStart = timer.active || !timer.countdown || timer.duration > 0
                PrimaryButton(
                    when {
                        timer.running -> "Pause"
                        timer.active -> "Resume"
                        else -> "Start"
                    },
                    onClick = { if (canStart) onChange { if (running) pause(it) else start(it) } },
                    enabled = canStart,
                    padding = 20.dp,
                    style = body(14.sp, Gym.OnPrimary, FontWeight.SemiBold),
                )
            }
        }
    }
}

/**
 * The idle timer's time, typed like a microwave (see [typedClock]); presets and nudges show up here too.
 * The caret stays at the end, so typing pushes digits in from the right and backspace drops the last one.
 */
@Composable
private fun TypedClock(duration: Long, style: TextStyle, onChange: (Long) -> Unit) {
    var text by remember { mutableStateOf(formatClock(duration)) }
    // Sync when the time changes from outside, e.g. a preset; "0:90" and 1:30 are the same time.
    if (typedClock(text).first != duration) text = formatClock(duration)
    var focused by remember { mutableStateOf(false) }
    CompositionLocalProvider(LocalTextSelectionColors provides GymSelectionColors) {
        BasicTextField(
            value = TextFieldValue(text, TextRange(text.length)),
            onValueChange = { new ->
                val (ms, shown) = typedClock(new.text)
                text = shown
                onChange(ms)
            },
            singleLine = true,
            textStyle = style,
            cursorBrush = SolidColor(Gym.Primary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
                .underline(if (focused) Gym.Primary else Gym.InputBorder, if (focused) 2.dp else 1.dp),
        )
    }
}

@Composable
private fun ModeTab(label: String, active: Boolean, onClick: () -> Unit) {
    Text(
        label,
        style = body(14.sp, if (active) Gym.Primary else Gym.Dim, FontWeight.Medium),
        modifier = Modifier
            .clickable(onClick = onClick)
            .then(if (active) Modifier.underline(Gym.Primary, 2.dp) else Modifier)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    )
}

@Composable
private fun OutlineButton(label: String, onClick: () -> Unit) {
    Text(
        label,
        style = body(14.sp, Gym.Dim, FontWeight.Medium),
        modifier = Modifier
            .clip(SmallShape)
            .border(1.dp, Gym.CardBorder, SmallShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}
