package com.example.gymtracker.ui

import android.graphics.Matrix
import android.graphics.Region
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.gymtracker.data.WeightAction
import com.example.gymtracker.ui.theme.Gym

private const val FIGURE_WIDTH = 100f
private const val FIGURE_HEIGHT = 220f

/** Hit testing works on integer regions, so shapes are scaled up for sub-unit precision. */
private const val HIT_SCALE = 10f

/**
 * Front and back figures with each muscle group tinted by its suggestion: green ▲ to go up,
 * red ▼ to go down, plain when staying or without data. Tapping a group calls [onSelect].
 */
@Composable
fun BodyDiagram(states: Map<String, WeightAction>, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val description = states.entries.joinToString(", ") { (group, action) ->
        "$group: go ${if (action == WeightAction.INCREASE) "up" else "down"}"
    }.ifEmpty { "No changes suggested" }
    Row(
        modifier.fillMaxWidth().semantics { contentDescription = "Body diagram. $description" },
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        BodyFigure(FrontView, "FRONT", states, onSelect)
        BodyFigure(BackView, "BACK", states, onSelect)
    }
}

@Composable
private fun BodyFigure(view: BodyView, label: String, states: Map<String, WeightAction>, onSelect: (String) -> Unit) {
    val paths = remember(view) { view.parts.map { it.group to PathParser().parsePathString(it.path).toPath() } }
    val regions = remember(paths) {
        val clip = Region(0, 0, (FIGURE_WIDTH * HIT_SCALE).toInt(), (FIGURE_HEIGHT * HIT_SCALE).toInt())
        val m = Matrix().apply { setScale(HIT_SCALE, HIT_SCALE) }
        paths.filter { it.first.isNotEmpty() }.map { (group, path) ->
            group to Region().apply { setPath(android.graphics.Path(path.asAndroidPath()).apply { transform(m) }, clip) }
        }
    }
    val measurer = rememberTextMeasurer()
    val currentOnSelect by rememberUpdatedState(onSelect)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            Modifier
                .width(116.dp)
                .aspectRatio(FIGURE_WIDTH / FIGURE_HEIGHT)
                .pointerInput(regions) {
                    detectTapGestures { tap ->
                        val unit = size.width / FIGURE_WIDTH
                        val x = (tap.x / unit * HIT_SCALE).toInt()
                        val y = (tap.y / unit * HIT_SCALE).toInt()
                        regions.firstOrNull { it.second.contains(x, y) }?.let { currentOnSelect(it.first) }
                    }
                },
        ) {
            val unit = size.width / FIGURE_WIDTH
            scale(unit, pivot = Offset.Zero) {
                paths.forEach { (group, path) ->
                    val (fill, stroke, width) = when {
                        group.isEmpty() -> Triple(Color.Transparent, Gym.CardBorder, 1f)
                        states[group] == WeightAction.INCREASE -> Triple(Gym.Success.copy(alpha = 0.2f), Gym.Success, 1.3f)
                        states[group] == WeightAction.DECREASE -> Triple(Gym.Danger.copy(alpha = 0.2f), Gym.Danger, 1.3f)
                        else -> Triple(Gym.Chip, Gym.InputBorder, 1.1f)
                    }
                    drawPath(path, fill)
                    drawPath(path, stroke, style = Stroke(width, join = StrokeJoin.Round))
                }
            }
            // One glyph per changed group, so the state never rests on colour alone.
            states.forEach { (group, action) ->
                val at = view.glyphAt[group] ?: return@forEach
                val up = action == WeightAction.INCREASE
                val text = measurer.measure(if (up) "▲" else "▼", mono(8.sp, if (up) Gym.Success else Gym.Danger))
                drawText(text, topLeft = Offset(at.x * unit - text.size.width / 2f, at.y * unit - text.size.height / 2f))
            }
        }
        Text(label, style = mono(11.sp, Gym.Dimmer).copy(letterSpacing = 0.15.em), modifier = Modifier.padding(top = 6.dp))
    }
}
