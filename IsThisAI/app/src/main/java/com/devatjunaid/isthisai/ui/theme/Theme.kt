package com.devatjunaid.isthisai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.devatjunaid.isthisai.R
import com.devatjunaid.isthisai.engine.Verdict

val Ink = Color(0xFF0D0E0B)
val Surface = Color(0xFF171914)
val SurfaceHigh = Color(0xFF1F221B)
val Line = Color(0xFF2A2D25)
val LineStrong = Color(0xFF3A3E33)
val Paper = Color(0xFFEEEADF)
val Muted = Color(0xFF9A9D90)
val Dim = Color(0xFF5E6256)
val Lime = Color(0xFFD4FF3A)
val AiOrange = Color(0xFFFF7A45)
val AiAmber = Color(0xFFFFB547)
val CameraBlue = Color(0xFF6FA8FF)
val Lavender = Color(0xFFC9B6FF)
val Danger = Color(0xFFFF6B6B)

fun verdictColor(v: Verdict): Color = when (v) {
    Verdict.AI_GENERATED -> AiOrange
    Verdict.AI_EDITED -> AiAmber
    Verdict.CAMERA -> CameraBlue
    Verdict.OTHER_LABEL -> Lavender
    Verdict.NO_LABEL -> Paper
}

val Display = FontFamily(
    Font(R.font.bricolage_regular, FontWeight.Normal),
    Font(R.font.bricolage_semibold, FontWeight.SemiBold),
    Font(R.font.bricolage_extrabold, FontWeight.ExtraBold),
)

val Mono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_semibold, FontWeight.SemiBold),
)

private val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, lineHeight = 36.sp, letterSpacing = (-0.9).sp),
    headlineLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 32.sp, letterSpacing = (-0.8).sp),
    headlineMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.ExtraBold, fontSize = 25.sp, lineHeight = 28.sp, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp, lineHeight = 24.sp, letterSpacing = (-0.3).sp),
    titleMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp),
    labelMedium = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Normal, fontSize = 12.sp, letterSpacing = 0.4.sp),
    labelSmall = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Normal, fontSize = 11.sp, letterSpacing = 0.5.sp),
)

private val Scheme = darkColorScheme(
    primary = Paper,
    onPrimary = Ink,
    secondary = Lime,
    onSecondary = Ink,
    background = Ink,
    onBackground = Paper,
    surface = Surface,
    onSurface = Paper,
    surfaceVariant = SurfaceHigh,
    onSurfaceVariant = Muted,
    outline = Line,
    error = Danger,
)

@Composable
fun IsThisAiTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = AppTypography, content = content)
}
