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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.gymtracker.data.BodyweightEntry
import com.example.gymtracker.data.BodyweightPoint
import com.example.gymtracker.data.ExerciseProgress
import com.example.gymtracker.data.ProgressMetric
import com.example.gymtracker.data.Recommendation
import com.example.gymtracker.data.TARGET_REPS
import com.example.gymtracker.data.WeightAction
import com.example.gymtracker.data.WorkoutExercise
import com.example.gymtracker.data.recommend
import com.example.gymtracker.data.SessionBest
import com.example.gymtracker.data.TREND_WINDOW_DAYS
import com.example.gymtracker.data.Trend
import com.example.gymtracker.data.WeightUnit
import com.example.gymtracker.data.Workout
import com.example.gymtracker.data.bodyweightChange
import com.example.gymtracker.data.bodyweightSeries
import com.example.gymtracker.data.computeProgress
import com.example.gymtracker.data.equipmentSummary
import com.example.gymtracker.data.formatDate
import com.example.gymtracker.data.formatWeight
import com.example.gymtracker.ui.theme.Gym
import kotlin.math.abs
import kotlin.math.roundToLong

private const val BODYWEIGHT_KEY = "__bodyweight__"

@Composable
fun ProgressScreen(
    workouts: List<Workout>,
    bodyweight: List<BodyweightEntry>,
    unit: WeightUnit,
    onLogBodyweight: (Double) -> Unit,
    onDeleteBodyweight: (Long) -> Unit,
) {
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val progress = remember(workouts, bodyweight, unit) { computeProgress(workouts, bodyweight, unit) }
    val bwSeries = remember(bodyweight, unit) { bodyweightSeries(bodyweight, unit) }

    if (selected == BODYWEIGHT_KEY) {
        BackHandler { selected = null }
        BodyweightDetail(bwSeries, unit, onLogBodyweight, onDeleteBodyweight, onBack = { selected = null })
        return
    }
    progress.find { it.key == selected }?.let { ex ->
        BackHandler { selected = null }
        ExerciseDetail(ex, unit, hasBodyweight = bwSeries.isNotEmpty(), rec = recommendationFor(ex, workouts, unit), onBack = { selected = null })
        return
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Column(Modifier.padding(top = 24.dp, bottom = 8.dp)) {
                Text("PROGRESSIVE OVERLOAD", style = SectionLabel, modifier = Modifier.padding(bottom = 4.dp))
                Text("Progress", style = display(30.sp), modifier = Modifier.padding(bottom = 16.dp))
                BodyweightCard(bwSeries, unit, onLog = onLogBodyweight, onOpen = { selected = BODYWEIGHT_KEY })
            }
        }
        if (progress.isEmpty()) {
            item { EmptyState("📈", "Finish a few workouts with completed sets\nto see your trends") }
        } else {
            item {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("EXERCISES", style = SectionLabel, modifier = Modifier.weight(1f))
                    Text("Trend · last ${TREND_WINDOW_DAYS / 7} wks", style = mono(12.sp, Gym.Faint))
                }
            }
            items(progress, key = { it.key }) { ex ->
                ExerciseRow(ex, unit, rec = recommendationFor(ex, workouts, unit), onClick = { selected = ex.key })
            }
        }
    }
}

// ─── Overview ────────────────────────────────────────────────────────────────

@Composable
private fun BodyweightCard(series: List<BodyweightPoint>, unit: WeightUnit, onLog: (Double) -> Unit, onOpen: () -> Unit) {
    val latest = series.lastOrNull()
    if (latest == null) {
        Column(Modifier.fillMaxWidth().card().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Bodyweight", style = display(17.sp, FontWeight.SemiBold))
            Text(
                "Log it to track it over time and to count it as load on pull-ups and other bodyweight exercises.",
                style = body(12.sp, Gym.Dim),
            )
            BodyweightInput(initial = 0.0, unit = unit, onLog = onLog)
        }
        return
    }
    val change = bodyweightChange(series)
    Row(
        Modifier.fillMaxWidth().card().clickable(onClick = onOpen).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text("Bodyweight · 7-day avg", style = mono(12.sp))
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(fmt1(latest.average), style = display(26.sp))
                Text(unit.label, style = mono(12.sp), modifier = Modifier.padding(bottom = 4.dp))
            }
            Text(
                change?.let { "${signed(it)} ${unit.label} in 30 days" } ?: "${series.size} weigh-in${if (series.size == 1) "" else "s"}",
                style = mono(12.sp, Gym.Dim),
            )
        }
        Sparkline(series.takeLast(30).map { it.average }, Modifier.size(width = 88.dp, height = 36.dp))
        ChevronRightIcon(Gym.Primary)
    }
}

@Composable
private fun ExerciseRow(ex: ExerciseProgress, unit: WeightUnit, rec: Recommendation?, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().card().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(ex.name, style = display(16.sp, FontWeight.SemiBold, spacing = 0.03.em), maxLines = 1)
                Text(
                    listOfNotNull(equipmentSummary(ex.equipment, ex.perSide), headlineValue(ex, unit)).joinToString(" · "),
                    style = mono(12.sp),
                    maxLines = 1,
                )
            }
            Sparkline(ex.sessions.takeLast(12).map { it.value }, Modifier.size(width = 72.dp, height = 28.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { TrendBadge(ex.trend, ex.changePct) }
            rec?.let { NextWeightTag(it, ex, unit) }
        }
    }
}

/** Compact "▲ 62.5 kg next" for list rows; the detail view explains why. */
@Composable
private fun NextWeightTag(rec: Recommendation, ex: ExerciseProgress, unit: WeightUnit) {
    val (glyph, color) = when (rec.action) {
        WeightAction.INCREASE -> "▲" to Gym.Success
        WeightAction.HOLD -> "" to Gym.Dimmer
        WeightAction.DECREASE -> "▼" to Gym.Danger
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (glyph.isNotEmpty()) Text(glyph, style = mono(10.sp, color))
        Text("${formatLoad(ex, rec.nextWeight, unit)} next", style = mono(12.sp, Gym.Muted))
    }
}

private fun recommendationFor(ex: ExerciseProgress, workouts: List<Workout>, unit: WeightUnit): Recommendation? =
    recommend(WorkoutExercise("", ex.name, ex.category, emptyList(), equipment = ex.equipment, perSide = ex.perSide), workouts, unit)

/** Status glyph in the status color, label in text color, so state never rides on color alone. */
@Composable
private fun TrendBadge(trend: Trend, changePct: Double?) {
    val (glyph, label, color) = when (trend) {
        Trend.PROGRESSING -> Triple("▲", "Progressing", Gym.Success)
        Trend.STALLED -> Triple("■", "Stalled", Gym.Dimmer)
        Trend.DECLINING -> Triple("▼", "Declining", Gym.Danger)
        Trend.NOT_ENOUGH_DATA -> Triple("○", "Need 3+ sessions in ${TREND_WINDOW_DAYS / 7} wks", Gym.Fainter)
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(glyph, style = mono(10.sp, color))
        Text(label, style = body(12.sp, Gym.Muted, FontWeight.Medium))
        changePct?.let { Text("${signed(it)}%", style = mono(12.sp, Gym.Dim)) }
    }
}

// ─── Exercise detail ─────────────────────────────────────────────────────────

@Composable
private fun ExerciseDetail(ex: ExerciseProgress, unit: WeightUnit, hasBodyweight: Boolean, rec: Recommendation?, onBack: () -> Unit) {
    val canBeRelative = ex.metric == ProgressMetric.E1RM && hasBodyweight
    var relative by rememberSaveable(ex.key) { mutableStateOf(false) }
    val showRelative = relative && canBeRelative
    // Relative values only exist for sessions with a known bodyweight (all of them once any is logged).
    val sessions = if (showRelative) ex.sessions.filter { it.bodyweight != null } else ex.sessions
    fun valueOf(s: SessionBest) = if (showRelative) s.value / s.bodyweight!! else s.value

    var picked by rememberSaveable(ex.key) { mutableStateOf<Int?>(null) }
    val selected = picked?.takeIf { it in sessions.indices } ?: sessions.lastIndex
    val session = sessions[selected]

    Column(Modifier.fillMaxSize()) {
        DetailHeader(ex.name, equipmentSummary(ex.equipment, ex.perSide) ?: ex.category, onBack)

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Hero: the latest session's value
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(
                        when {
                            ex.metric == ProgressMetric.REPS -> "Best set · reps"
                            showRelative -> "Estimated 1RM · × bodyweight"
                            else -> "Estimated 1RM"
                        },
                        style = mono(12.sp),
                    )
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(formatMetric(valueOf(sessions.last()), ex, showRelative), style = display(40.sp))
                        metricUnit(ex, unit, showRelative)?.let {
                            Text(it, style = mono(14.sp), modifier = Modifier.padding(bottom = 7.dp))
                        }
                    }
                    TrendBadge(ex.trend, ex.changePct)
                }
                if (canBeRelative) {
                    Row(Modifier.clip(SmallShape).border(1.dp, Gym.InputBorder, SmallShape)) {
                        ToggleSegment(unit.label, selected = !showRelative) { relative = false }
                        ToggleSegment("× BW", selected = showRelative) { relative = true }
                    }
                }
            }

            rec?.let {
                Column {
                    Text("NEXT SESSION · ${TARGET_REPS.first}–${TARGET_REPS.last} REPS", style = SectionLabel, modifier = Modifier.padding(bottom = 8.dp))
                    RecommendationHint(it, ex.equipment, unit)
                }
            }

            if (ex.metric == ProgressMetric.REPS && ex.equipment?.isBodyweight == true) {
                Text(
                    "Log your bodyweight on the Progress tab to track load for this exercise. Until then it's tracked by reps.",
                    style = body(12.sp, Gym.Dim),
                    modifier = Modifier.fillMaxWidth().clip(SmallShape).background(Gym.Surface).padding(12.dp),
                )
            }

            Column(Modifier.fillMaxWidth().card().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TimeSeriesChart(
                    line = sessions.map { ChartPoint(it.date, valueOf(it), highlight = it.isPr) },
                    selected = selected,
                    onSelect = { picked = it },
                    formatTick = { formatTick(it, showRelative) },
                )
                HorizontalDivider(color = Gym.Divider)
                // Details for the tapped session
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(formatDate(session.date), style = mono(12.sp))
                        Text("Top set ${topSet(ex, session, unit)}", style = body(14.sp, weight = FontWeight.Medium))
                        if (ex.equipment?.isBodyweight == true && session.bodyweight != null) {
                            Text("at ${fmt1(session.bodyweight)} ${unit.label} bodyweight", style = mono(12.sp, Gym.Dim))
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${formatMetric(valueOf(session), ex, showRelative)}${metricUnit(ex, unit, showRelative)?.let { " $it" } ?: ""}",
                            style = mono(14.sp, Gym.Foreground, FontWeight.SemiBold),
                        )
                        if (session.isPr) Text("PR", style = mono(12.sp, Gym.PrimaryText, FontWeight.SemiBold))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(Gym.Primary))
                Text(
                    if (ex.metric == ProgressMetric.E1RM) "PR · best set per session, e1RM = weight × (1 + reps/30)"
                    else "PR · most reps in one set, per session",
                    style = mono(12.sp, Gym.Dim),
                )
            }

            if (ex.repRecords.isNotEmpty()) RepRecords(ex, unit)
        }
    }
}

@Composable
private fun RepRecords(ex: ExerciseProgress, unit: WeightUnit) {
    Column {
        Text("BEST WEIGHT PER REP COUNT", style = SectionLabel, modifier = Modifier.padding(bottom = 8.dp))
        Column(Modifier.fillMaxWidth().card().padding(horizontal = 16.dp, vertical = 4.dp)) {
            ex.repRecords.forEachIndexed { i, r ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${r.reps}", style = mono(14.sp, Gym.Foreground, FontWeight.SemiBold), textAlign = TextAlign.End, modifier = Modifier.width(28.dp))
                    Text(if (r.reps == 1) " rep" else " reps", style = mono(12.sp), modifier = Modifier.width(48.dp))
                    Text(formatLoad(ex, r.logged, unit), style = mono(14.sp, Gym.Foreground), modifier = Modifier.weight(1f))
                    Text(formatDate(r.date), style = mono(12.sp))
                }
                if (i < ex.repRecords.lastIndex) HorizontalDivider(color = Gym.Divider)
            }
        }
    }
}

// ─── Bodyweight detail ───────────────────────────────────────────────────────

@Composable
private fun BodyweightDetail(
    series: List<BodyweightPoint>,
    unit: WeightUnit,
    onLog: (Double) -> Unit,
    onDelete: (Long) -> Unit,
    onBack: () -> Unit,
) {
    var picked by rememberSaveable { mutableStateOf<Int?>(null) }
    Column(Modifier.fillMaxSize()) {
        DetailHeader("Bodyweight", "${series.size} weigh-in${if (series.size == 1) "" else "s"}", onBack)
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(Modifier.fillMaxWidth().card().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Log today", style = mono(12.sp))
                    BodyweightInput(initial = series.lastOrNull()?.weight ?: 0.0, unit = unit, onLog = onLog)
                }
            }
            if (series.isNotEmpty()) {
                val selected = picked?.takeIf { it in series.indices } ?: series.lastIndex
                val point = series[selected]
                item {
                    Column(Modifier.fillMaxWidth().card().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        TimeSeriesChart(
                            line = series.map { ChartPoint(it.date, it.average) },
                            scatter = series.map { ChartPoint(it.date, it.weight) },
                            selected = selected,
                            onSelect = { picked = it },
                            formatTick = ::fmt1,
                        )
                        HorizontalDivider(color = Gym.Divider)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(formatDate(point.date), style = mono(12.sp))
                                Text("Weighed ${fmt1(point.weight)} ${unit.label}", style = body(14.sp, weight = FontWeight.Medium))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${fmt1(point.average)} ${unit.label}", style = mono(14.sp, Gym.Foreground, FontWeight.SemiBold))
                                Text("7-day avg", style = mono(12.sp))
                            }
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(width = 16.dp, height = 2.dp).background(Gym.Primary))
                        Text("7-day average", style = mono(12.sp, Gym.Dim))
                        Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(Gym.Primary.copy(alpha = 0.3f)))
                        Text("Weigh-in", style = mono(12.sp, Gym.Dim))
                    }
                }
                item { Text("WEIGH-INS", style = SectionLabel) }
                items(series.reversed(), key = { it.date }) { p ->
                    Row(
                        Modifier.fillMaxWidth().card().padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(formatDate(p.date), style = mono(12.sp), modifier = Modifier.weight(1f))
                        Text("${fmt1(p.weight)} ${unit.label}", style = mono(14.sp, Gym.Foreground))
                        Box(
                            Modifier.size(40.dp).clip(RoundedCornerShape(50)).clickable { onDelete(p.date) },
                            contentAlignment = Alignment.Center,
                        ) {
                            CloseIcon(10.dp, Gym.Danger, Modifier.alpha(0.75f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BodyweightInput(initial: Double, unit: WeightUnit, onLog: (Double) -> Unit) {
    var weight by remember(initial) { mutableDoubleStateOf(initial) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberField(value = weight, onValueChange = { weight = it }, modifier = Modifier.width(72.dp))
        Text(unit.label, style = body(12.sp, Gym.Faint))
        Box(Modifier.weight(1f))
        PrimaryButton("Log", onClick = { if (weight > 0) onLog(weight) }, enabled = weight > 0)
    }
}

// ─── Shared bits ─────────────────────────────────────────────────────────────

@Composable
private fun DetailHeader(title: String, subtitle: String, onBack: () -> Unit) {
    Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        BackButton(onBack)
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Text(title, style = display(18.sp, spacing = 0.03.em))
            Text(subtitle, style = mono(12.sp))
        }
    }
    HorizontalDivider(color = Gym.Divider)
}

@Composable
private fun ToggleSegment(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        style = mono(12.sp, if (selected) Gym.OnPrimary else Gym.Muted),
        modifier = Modifier
            .background(if (selected) Gym.Primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

/** One decimal at most: 96.33 → "96.3", 100.0 → "100". */
private fun fmt1(v: Double): String = formatWeight((v * 10).roundToLong() / 10.0)

private fun signed(v: Double): String {
    val rounded = (v * 10).roundToLong() / 10.0
    return when {
        rounded > 0 -> "+${fmt1(rounded)}"
        rounded < 0 -> "−${fmt1(abs(rounded))}"
        else -> "±0"
    }
}

private fun formatMetric(v: Double, ex: ExerciseProgress, relative: Boolean): String = when {
    ex.metric == ProgressMetric.REPS -> v.roundToLong().toString()
    relative -> "%.2f×".format(v)
    else -> fmt1(v)
}

private fun metricUnit(ex: ExerciseProgress, unit: WeightUnit, relative: Boolean): String? = when {
    ex.metric == ProgressMetric.REPS -> "reps"
    relative -> null
    else -> unit.label
}

private fun formatTick(v: Double, relative: Boolean): String =
    if (relative) "%.2f×".format(v) else formatWeight((v * 100).roundToLong() / 100.0)

/** How a logged weight reads: "BW + 10 kg" for bodyweight exercises, "60 kg" otherwise. */
private fun formatLoad(ex: ExerciseProgress, logged: Double, unit: WeightUnit): String = when {
    ex.equipment?.isBodyweight == true && logged > 0 -> "BW + ${fmt1(logged)} ${unit.label}"
    ex.equipment?.isBodyweight == true -> "BW"
    else -> "${fmt1(logged)} ${unit.label}"
}

private fun topSet(ex: ExerciseProgress, s: SessionBest, unit: WeightUnit) =
    "${formatLoad(ex, s.logged, unit)} × ${s.reps}"

private fun headlineValue(ex: ExerciseProgress, unit: WeightUnit): String = when (ex.metric) {
    ProgressMetric.E1RM -> "e1RM ${fmt1(ex.latest.value)} ${unit.label}"
    ProgressMetric.REPS -> "${ex.latest.reps} reps"
}
