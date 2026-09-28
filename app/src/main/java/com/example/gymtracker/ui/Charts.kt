package com.example.gymtracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymtracker.data.DAY_MILLIS
import com.example.gymtracker.data.formatShortDate
import com.example.gymtracker.ui.theme.Gym
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

data class ChartPoint(val time: Long, val value: Double, val highlight: Boolean = false)

/** A trend line with no axes, for list rows. */
@Composable
fun Sparkline(values: List<Double>, modifier: Modifier = Modifier, color: Color = Gym.Primary) {
    Canvas(modifier) {
        if (values.isEmpty()) return@Canvas
        val pad = 3.dp.toPx()
        val min = values.min()
        val max = values.max()
        val range = (max - min).takeIf { it > 0 } ?: 1.0
        fun pt(i: Int) = Offset(
            x = if (values.size == 1) size.width - pad else pad + (size.width - 2 * pad) * i / (values.size - 1),
            // A flat series sits in the middle rather than on the floor.
            y = if (max == min) size.height / 2 else size.height - pad - ((values[i] - min) / range * (size.height - 2 * pad)).toFloat(),
        )
        if (values.size > 1) {
            val path = Path().apply {
                moveTo(pt(0).x, pt(0).y)
                for (i in 1 until values.size) lineTo(pt(i).x, pt(i).y)
            }
            drawPath(path, color, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        drawCircle(color, 2.5.dp.toPx(), pt(values.lastIndex))
    }
}

/**
 * Line over time with gridlines, y ticks and first/last dates. Tap or drag to select the
 * nearest point on [line]; the caller shows the details. [scatter] draws faint raw readings
 * underneath (e.g. daily weigh-ins under their moving average). Highlighted points get a
 * larger ringed marker.
 */
@Composable
fun TimeSeriesChart(
    line: List<ChartPoint>,
    selected: Int?,
    onSelect: (Int) -> Unit,
    formatTick: (Double) -> String,
    modifier: Modifier = Modifier,
    scatter: List<ChartPoint> = emptyList(),
    height: Dp = 200.dp,
) {
    val measurer = rememberTextMeasurer()
    val tickStyle = mono(11.sp, Gym.Dimmer)
    val currentOnSelect by rememberUpdatedState(onSelect)

    // Plot bounds (left, right) are written while drawing and read by hit testing.
    val bounds = remember { FloatArray(2) }
    val all = line + scatter
    val minTime = all.minOfOrNull { it.time } ?: 0L
    val maxTime = (all.maxOfOrNull { it.time } ?: 0L).let { if (it == minTime) it + DAY_MILLIS else it }
    val start = if (all.isNotEmpty() && all.maxOf { it.time } == minTime) minTime - DAY_MILLIS else minTime

    fun xOf(time: Long) = bounds[0] + (bounds[1] - bounds[0]) * ((time - start).toFloat() / (maxTime - start))

    fun nearest(x: Float): Int? = line.indices.minByOrNull { abs(xOf(line[it].time) - x) }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
            .pointerInput(line, scatter) { detectTapGestures { o -> nearest(o.x)?.let(currentOnSelect) } }
            .pointerInput(line, scatter) {
                detectHorizontalDragGestures { change, _ -> nearest(change.position.x)?.let(currentOnSelect) }
            },
    ) {
        if (all.isEmpty()) return@Canvas
        val ticks = niceTicks(all.minOf { it.value }, all.maxOf { it.value })
        val yMin = ticks.first()
        val yMax = ticks.last()

        val labels = ticks.map { measurer.measure(formatTick(it), tickStyle) }
        val labelWidth = labels.maxOf { it.size.width }
        val markerInset = 7.dp.toPx() // room for the largest marker and its ring
        val plotLeft = labelWidth + 8.dp.toPx() + markerInset
        bounds[0] = plotLeft
        bounds[1] = size.width - markerInset
        val dateHeight = measurer.measure("0", tickStyle).size.height
        val plotTop = labels.first().size.height / 2f
        val plotBottom = size.height - dateHeight - 8.dp.toPx()

        fun yOf(v: Double) = plotBottom - ((v - yMin) / (yMax - yMin) * (plotBottom - plotTop)).toFloat()

        // Gridlines and y ticks
        ticks.forEachIndexed { i, t ->
            val y = yOf(t)
            drawLine(Gym.Divider, Offset(plotLeft - markerInset, y), Offset(size.width, y), 1.dp.toPx())
            drawText(labels[i], topLeft = Offset(labelWidth - labels[i].size.width.toFloat(), y - labels[i].size.height / 2f))
        }

        // First / last date
        val first = measurer.measure(formatShortDate(all.minOf { it.time }), tickStyle)
        val last = measurer.measure(formatShortDate(all.maxOf { it.time }), tickStyle)
        val dateY = size.height - dateHeight
        drawText(first, topLeft = Offset(plotLeft - markerInset, dateY.toFloat()))
        if (all.maxOf { it.time } != all.minOf { it.time }) {
            drawText(last, topLeft = Offset(size.width - last.size.width, dateY.toFloat()))
        }

        // Crosshair behind the data
        selected?.let(line::getOrNull)?.let { p ->
            drawLine(Gym.InputBorder, Offset(xOf(p.time), plotTop), Offset(xOf(p.time), plotBottom), 1.dp.toPx())
        }

        scatter.forEach { p ->
            drawCircle(Gym.Primary.copy(alpha = 0.3f), 3.dp.toPx(), Offset(xOf(p.time), yOf(p.value)))
        }

        if (line.size > 1) {
            val path = Path().apply {
                moveTo(xOf(line[0].time), yOf(line[0].value))
                for (i in 1 until line.size) lineTo(xOf(line[i].time), yOf(line[i].value))
            }
            drawPath(path, Gym.Primary, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        // Plain markers only while they stay readable; highlights and the selection always show.
        val showAll = line.size <= 40
        line.forEachIndexed { i, p ->
            val c = Offset(xOf(p.time), yOf(p.value))
            when {
                i == selected -> ringedDot(c, 6.dp.toPx())
                p.highlight -> ringedDot(c, 5.dp.toPx())
                showAll || line.size == 1 -> drawCircle(Gym.Primary, 2.5.dp.toPx(), c)
            }
        }
    }
}

private fun DrawScope.ringedDot(center: Offset, radius: Float) {
    drawCircle(Gym.Background, radius + 2.dp.toPx(), center)
    drawCircle(Gym.Primary, radius, center)
}

/** 3–6 evenly spaced round tick values that span [min, max]. */
internal fun niceTicks(min: Double, max: Double): List<Double> {
    var lo = min
    var hi = max
    if (hi - lo < 1e-9) {
        val pad = if (abs(lo) > 1e-9) abs(lo) * 0.05 else 1.0
        lo -= pad
        hi += pad
    }
    val rough = (hi - lo) / 4
    val mag = 10.0.pow(floor(log10(rough)))
    val step = listOf(1.0, 2.0, 2.5, 5.0, 10.0).map { it * mag }.first { it >= rough }
    val first = floor(lo / step) * step
    val last = ceil(hi / step) * step
    val n = ((last - first) / step).toInt()
    return (0..n).map { first + it * step }
}
