package com.example.gymtracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.gymtracker.data.formatWeight
import com.example.gymtracker.ui.theme.BodyFont
import com.example.gymtracker.ui.theme.DisplayFont
import com.example.gymtracker.ui.theme.Gym
import com.example.gymtracker.ui.theme.GymSelectionColors
import com.example.gymtracker.ui.theme.MonoFont

// ─── Text styles (Tailwind text-xs/sm/base/lg/xl/3xl) ────────────────────────

fun mono(size: TextUnit = 12.sp, color: Color = Gym.Dimmer, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = MonoFont, fontSize = size, color = color, fontWeight = weight)

fun display(size: TextUnit, weight: FontWeight = FontWeight.Bold, color: Color = Gym.Foreground, spacing: TextUnit = 0.02.em) =
    TextStyle(fontFamily = DisplayFont, fontSize = size, fontWeight = weight, color = color, letterSpacing = spacing)

fun body(size: TextUnit = 14.sp, color: Color = Gym.Foreground, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = BodyFont, fontSize = size, color = color, fontWeight = weight)

val SectionLabel = mono(12.sp).copy(letterSpacing = 0.1.em)

// ─── Containers ──────────────────────────────────────────────────────────────

val CardShape = RoundedCornerShape(8.dp)
val SmallShape = RoundedCornerShape(4.dp)

fun Modifier.card(shape: Shape = CardShape, background: Color = Gym.Card, border: Color = Gym.CardBorder) =
    this.clip(shape).background(background).border(1.dp, border, shape)

fun Modifier.dashedBorder(color: Color, radius: Dp) = drawBehind {
    val inset = 0.5.dp.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(inset, inset),
        size = Size(size.width - 2 * inset, size.height - 2 * inset),
        cornerRadius = CornerRadius(radius.toPx()),
        style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
    )
}

@Composable
fun CategoryChip(category: String, color: Color = Gym.Muted, minWidth: Boolean = false) {
    Text(
        category,
        style = mono(12.sp, color),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .clip(SmallShape)
            .background(Gym.Chip)
            .then(if (minWidth) Modifier.size(width = 72.dp, height = 22.dp).padding(top = 2.dp) else Modifier)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = SmallShape,
    style: TextStyle = body(12.sp, Gym.OnPrimary, FontWeight.SemiBold),
    padding: Dp = 12.dp,
    icon: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .clip(shape)
            .background(if (enabled) Gym.Primary else Gym.Chip)
            .clickable(onClick = onClick)
            .padding(horizontal = padding, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        icon?.invoke()
        Text(text, style = if (enabled) style else style.copy(color = Gym.Faint))
    }
}

@Composable
fun EmptyState(emoji: String, message: String, modifier: Modifier = Modifier, alpha: Float = 0.35f, content: @Composable () -> Unit = {}) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(emoji, fontSize = 44.sp, modifier = Modifier.alpha(alpha))
        Text(message, style = body(14.sp, Gym.Faint), textAlign = TextAlign.Center)
        content()
    }
}

@Composable
fun FullScreenCenter(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

// ─── Inputs ──────────────────────────────────────────────────────────────────

/** Plain text input with a lime caret, like the web app's bare `<input>`s. */
@Composable
fun BareTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    placeholderColor: Color = Gym.Faint,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    CompositionLocalProvider(LocalTextSelectionColors provides GymSelectionColors) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = style,
            cursorBrush = SolidColor(Gym.Primary),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            modifier = modifier,
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text(placeholder, style = style.copy(color = placeholderColor))
                    inner()
                }
            },
        )
    }
}

/**
 * Numeric input with an underline, mirroring `<input type="number">`.
 * Keeps its own text so partial input like "62." or "" isn't clobbered while typing.
 */
@Composable
fun NumberField(
    value: Double,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    decimal: Boolean = true,
) {
    var text by remember { mutableStateOf(formatWeight(value)) }
    val parsed = text.toDoubleOrNull() ?: 0.0
    // Sync if the value was changed from outside (e.g. a new set copying the previous one).
    if (parsed != value) text = formatWeight(value)

    var focused by remember { mutableStateOf(false) }
    CompositionLocalProvider(LocalTextSelectionColors provides GymSelectionColors) {
        BasicTextField(
            value = text,
            onValueChange = { new ->
                val filtered = new.filter { it.isDigit() || (decimal && (it == '.' || it == ',')) }.replace(',', '.')
                if (filtered.count { it == '.' } > 1) return@BasicTextField
                text = filtered
                onValueChange(filtered.toDoubleOrNull() ?: 0.0)
            },
            singleLine = true,
            textStyle = mono(14.sp, Gym.Foreground).copy(textAlign = TextAlign.Center),
            cursorBrush = SolidColor(Gym.Primary),
            keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
            modifier = modifier
                .onFocusChanged { focused = it.isFocused }
                .underline(if (focused) Gym.Primary else Gym.InputBorder, if (focused) 2.dp else 1.dp)
                .padding(vertical = 2.dp),
        )
    }
}

fun Modifier.underline(color: Color, width: Dp) = drawBehind {
    val w = width.toPx()
    drawLine(color, Offset(0f, size.height - w / 2), Offset(size.width, size.height - w / 2), w)
}

// ─── Icons (ported from the inline SVGs) ─────────────────────────────────────

/** Draws a stroked icon on a [viewBox]-sized coordinate space, like the SVGs in the web app. */
@Composable
fun StrokeIcon(
    viewBox: Float,
    size: Dp,
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 1.5f,
    fill: Boolean = false,
    draw: Path.() -> Unit,
) {
    Canvas(modifier.size(size)) {
        val scale = this.size.width / viewBox
        val path = Path().apply(draw)
        scale(scale, pivot = Offset.Zero) {
            if (fill) drawPath(path, color)
            else drawPath(path, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Composable
fun CloseIcon(size: Dp, color: Color, modifier: Modifier = Modifier) =
    StrokeIcon(14f, size, color, modifier) { moveTo(2f, 2f); lineTo(12f, 12f); moveTo(12f, 2f); lineTo(2f, 12f) }

@Composable
fun PlusIcon(size: Dp, color: Color, modifier: Modifier = Modifier) =
    StrokeIcon(12f, size, color, modifier) { moveTo(6f, 1f); lineTo(6f, 11f); moveTo(1f, 6f); lineTo(11f, 6f) }

@Composable
fun BackIcon(color: Color, modifier: Modifier = Modifier) =
    StrokeIcon(16f, 16.dp, color, modifier) { moveTo(10f, 3f); lineTo(5f, 8f); lineTo(10f, 13f) }

@Composable
fun ChevronRightIcon(color: Color) =
    StrokeIcon(14f, 14.dp, color) { moveTo(5f, 7f); lineTo(11f, 7f); moveTo(8f, 4f); lineTo(11f, 7f); lineTo(8f, 10f) }

@Composable
fun ArrowRightIcon(color: Color) =
    StrokeIcon(18f, 18.dp, color, strokeWidth = 2f) { moveTo(3f, 9f); lineTo(15f, 9f); moveTo(9f, 3f); lineTo(15f, 9f); lineTo(9f, 15f) }

@Composable
fun ChevronDownIcon(color: Color, modifier: Modifier = Modifier) =
    StrokeIcon(12f, 12.dp, color, modifier) { moveTo(2f, 4f); lineTo(6f, 8f); lineTo(10f, 4f) }

@Composable
fun CheckIcon(color: Color) =
    StrokeIcon(10f, 10.dp, color) { moveTo(1f, 4.5f); lineTo(3.5f, 7f); lineTo(9f, 1.5f) }

@Composable
fun PlayIcon(color: Color) =
    StrokeIcon(12f, 12.dp, color, fill = true) { moveTo(3f, 2f); lineTo(10f, 6f); lineTo(3f, 10f); close() }

@Composable
fun ClockIcon(color: Color) {
    Canvas(Modifier.size(12.dp)) {
        val s = size.width / 12f
        val stroke = Stroke(1.2f * s, cap = StrokeCap.Round)
        drawCircle(color, radius = 5f * s, style = stroke)
        drawPath(Path().apply { moveTo(6f * s, 3f * s); lineTo(6f * s, 6f * s); lineTo(8f * s, 8f * s) }, color, style = stroke)
    }
}

@Composable
fun SearchIcon(color: Color) {
    Canvas(Modifier.size(16.dp)) {
        val s = size.width / 16f
        val stroke = Stroke(1.5f * s, cap = StrokeCap.Round)
        drawCircle(color, radius = 5f * s, center = Offset(7f * s, 7f * s), style = stroke)
        drawLine(color, Offset(11f * s, 11f * s), Offset(14f * s, 14f * s), 1.5f * s, StrokeCap.Round)
    }
}
