package com.example.gymtracker.ui.theme

import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.gymtracker.R

// Blue-on-white palette. Every text color clears WCAG AA (4.5:1) on Background, Card and Chip;
// only Fainter is below that, and it is reserved for decorative marks like "×".
object Gym {
    val Background = Color(0xFFFFFFFF)
    val Foreground = Color(0xFF0F172A) // slate-900, 17:1
    val Card = Color(0xFFF8FAFC) // slate-50
    val CardBorder = Color(0xFFE2E8F0) // slate-200
    val Primary = Color(0xFF2563EB) // blue-600, 5.2:1 on white; white text on it is 5.2:1
    val PrimaryText = Color(0xFF1D4ED8) // blue-700, for text on light blue tints
    val OnPrimary = Color(0xFFFFFFFF)
    val Surface = Color(0xFFEFF6FF) // blue-50
    val Chip = Color(0xFFF1F5F9) // slate-100
    val Divider = Color(0xFFE2E8F0)
    val InputBorder = Color(0xFFCBD5E1) // slate-300
    val PrimaryDashed = Color(0xFF93C5FD) // blue-300
    val Danger = Color(0xFFDC2626) // red-600, 4.8:1
    val Success = Color(0xFF15803D) // green-700, 5.0:1
    val Muted = Color(0xFF475569) // slate-600, 7.6:1
    val Dim = Color(0xFF475569)
    val Dimmer = Color(0xFF64748B) // slate-500, 4.8:1
    val Faint = Color(0xFF64748B)
    val Fainter = Color(0xFF94A3B8) // slate-400, decorative only
}

val DisplayFont = FontFamily(
    Font(R.font.barlow_condensed_medium, FontWeight.Medium),
    Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_condensed_bold, FontWeight.Bold),
    Font(R.font.barlow_condensed_extrabold, FontWeight.ExtraBold),
    Font(R.font.barlow_condensed_black, FontWeight.Black),
)

val BodyFont = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

val MonoFont = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_semibold, FontWeight.SemiBold),
)

val GymSelectionColors = TextSelectionColors(
    handleColor = Gym.Primary,
    backgroundColor = Gym.Primary.copy(alpha = 0.25f),
)

@Composable
fun GymTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Gym.Primary,
            onPrimary = Gym.OnPrimary,
            background = Gym.Background,
            onBackground = Gym.Foreground,
            surface = Gym.Card,
            onSurface = Gym.Foreground,
            error = Gym.Danger,
        ),
        content = content,
    )
}
