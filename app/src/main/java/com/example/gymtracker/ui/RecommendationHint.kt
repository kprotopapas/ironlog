package com.example.gymtracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymtracker.data.Equipment
import com.example.gymtracker.data.Recommendation
import com.example.gymtracker.data.TARGET_REPS
import com.example.gymtracker.data.WeightAction
import com.example.gymtracker.data.WeightUnit
import com.example.gymtracker.data.formatShortDate
import com.example.gymtracker.data.formatWeight
import com.example.gymtracker.ui.theme.Gym
import kotlin.math.roundToLong

/**
 * "Go up to 62.5 kg — last 60 kg × 12, 12, 12". Callers skip [WeightAction.HOLD]: staying needs no hint. [onApply], when given, shows a Use button
 * that puts the suggested weight on the sets still to do.
 */
@Composable
fun RecommendationHint(
    rec: Recommendation,
    equipment: Equipment?,
    unit: WeightUnit,
    modifier: Modifier = Modifier,
    onApply: (() -> Unit)? = null,
) {
    val next = weightLabel(rec.nextWeight, equipment, unit)
    val (glyph, headline, color) = when (rec.action) {
        WeightAction.INCREASE -> Triple("▲", "Go up to $next", Gym.Success)
        WeightAction.HOLD -> Triple("", "Stay at $next", Gym.Dimmer)
        WeightAction.DECREASE -> Triple("▼", "Go down to $next", Gym.Danger)
    }
    val why = when (rec.action) {
        WeightAction.INCREASE -> "every set hit ${TARGET_REPS.last}"
        WeightAction.HOLD -> "add reps until every set hits ${TARGET_REPS.last}"
        WeightAction.DECREASE -> "under ${TARGET_REPS.first} reps"
    }
    Row(
        modifier.fillMaxWidth().clip(SmallShape).background(Gym.Surface).padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (glyph.isNotEmpty()) Text(glyph, style = mono(10.sp, color))
        Column(Modifier.weight(1f)) {
            Text(headline, style = body(13.sp, Gym.Foreground, FontWeight.SemiBold))
            Text(
                "${formatShortDate(rec.lastDate)}: ${weightLabel(rec.lastWeight, equipment, unit)} × ${rec.lastReps.joinToString(", ")} · $why",
                style = mono(11.sp, Gym.Dim),
            )
        }
        if (onApply != null) PrimaryButton("Use", onClick = onApply)
    }
}

/** "62.5 kg", or "BW + 10 kg" / "BW" for bodyweight exercises. */
private fun weightLabel(weight: Double, equipment: Equipment?, unit: WeightUnit): String {
    val w = formatWeight((weight * 100).roundToLong() / 100.0)
    return when {
        equipment?.isBodyweight != true -> "$w ${unit.label}"
        weight > 0 -> "BW + $w ${unit.label}"
        else -> "BW"
    }
}
