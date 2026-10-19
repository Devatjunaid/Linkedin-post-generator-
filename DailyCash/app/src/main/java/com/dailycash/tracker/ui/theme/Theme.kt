package com.dailycash.tracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object Palette {
    val Ink = Color(0xFF0B0F0E)
    val Surface = Color(0xFF141A18)
    val Surface2 = Color(0xFF101614)
    val Raised = Color(0xFF1A2220)
    val Line = Color(0xFF26302C)
    val Text = Color(0xFFEEF3EF)
    val Muted = Color(0xFF9AA8A2)
    val Faint = Color(0xFF5B6762)
    val Mint = Color(0xFF6EF0B9)
    val Amber = Color(0xFFFFB547)
    val Coral = Color(0xFFFF7A59)
    val Sky = Color(0xFF9DBDF0)

    /** Banknote colourways: background, guilloche line, print ink. */
    data class NoteColors(val bg: Color, val line: Color, val ink: Color)

    val notes = listOf(
        NoteColors(Color(0xFF11302A), Color(0xFF2F7A62), Color(0xFF8FE3C1)),
        NoteColors(Color(0xFF33240F), Color(0xFF8A6326), Color(0xFFF2C77E)),
        NoteColors(Color(0xFF152338), Color(0xFF3A5E8C), Color(0xFF9DBDF0)),
        NoteColors(Color(0xFF3A1A16), Color(0xFF9A4535), Color(0xFFFFA48E)),
        NoteColors(Color(0xFF261D3A), Color(0xFF5E4A8C), Color(0xFFC6A8FF)),
        NoteColors(Color(0xFF0F3033), Color(0xFF2E7B82), Color(0xFF7FE0E8)),
    )

    fun noteFor(code: String): NoteColors = notes[Math.floorMod(code.hashCode(), notes.size)]
}

val Mono = FontFamily.Monospace

private val typography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.Black, fontSize = 52.sp, letterSpacing = (-1.5).sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, letterSpacing = (-0.6).sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = (-0.3).sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 12.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.2.sp),
)

@Composable
fun DailyCashTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Palette.Mint,
            onPrimary = Palette.Ink,
            secondary = Palette.Amber,
            onSecondary = Palette.Ink,
            tertiary = Palette.Sky,
            background = Palette.Ink,
            onBackground = Palette.Text,
            surface = Palette.Surface,
            onSurface = Palette.Text,
            surfaceVariant = Palette.Raised,
            onSurfaceVariant = Palette.Muted,
            surfaceContainer = Palette.Surface,
            surfaceContainerHigh = Palette.Raised,
            surfaceContainerLow = Palette.Surface2,
            outline = Palette.Line,
            outlineVariant = Palette.Line,
            error = Palette.Coral,
        ),
        typography = typography,
        content = content,
    )
}
