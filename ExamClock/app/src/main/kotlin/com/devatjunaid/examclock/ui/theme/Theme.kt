package com.devatjunaid.examclock.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.devatjunaid.examclock.R
import com.devatjunaid.examclock.data.prefs.ThemeMode

private val LightColors = lightColorScheme(
    primary = Brand.Blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6EBFC),
    onPrimaryContainer = Color(0xFF1A35A8),
    secondary = Brand.Ink,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDEBE4),
    onSecondaryContainer = Brand.Ink,
    tertiary = Brand.Orange,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFCEBDD),
    onTertiaryContainer = Color(0xFF6B2610),
    background = Brand.Paper,
    onBackground = Brand.Ink,
    surface = Color.White,
    onSurface = Brand.Ink,
    surfaceVariant = Color(0xFFF0EEE7),
    onSurfaceVariant = Color(0xFF545A68),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFBFAF7),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF0EEE7),
    surfaceContainerHighest = Color(0xFFE9E7E0),
    outline = Color(0xFFC9C6BB),
    outlineVariant = Color(0xFFE3E1D9),
    error = Color(0xFFB3261E),
    inverseSurface = Brand.Ink,
    inverseOnSurface = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Brand.BlueLight,
    onPrimary = Color(0xFF0B1A5C),
    primaryContainer = Color(0xFF22305F),
    onPrimaryContainer = Color(0xFFDDE3FF),
    secondary = Color(0xFFE7E8EC),
    onSecondary = Brand.Ink,
    secondaryContainer = Brand.InkRaised,
    onSecondaryContainer = Color(0xFFE7E8EC),
    tertiary = Brand.OrangeLight,
    onTertiary = Color(0xFF4A1F00),
    tertiaryContainer = Color(0xFF3B2A1E),
    onTertiaryContainer = Color(0xFFFFDCC2),
    background = Color(0xFF0F1115),
    onBackground = Color(0xFFE7E8EC),
    surface = Color(0xFF171A20),
    onSurface = Color(0xFFE7E8EC),
    surfaceVariant = Brand.InkRaised,
    onSurfaceVariant = Brand.OnInkMuted,
    surfaceContainerLowest = Color(0xFF0C0E11),
    surfaceContainerLow = Color(0xFF14161B),
    surfaceContainer = Color(0xFF171A20),
    surfaceContainerHigh = Color(0xFF1E2128),
    surfaceContainerHighest = Brand.InkRaised,
    outline = Color(0xFF4A4F5C),
    outlineVariant = Brand.InkLine,
    inverseSurface = Color(0xFFE7E8EC),
    inverseOnSurface = Brand.Ink,
)

@OptIn(ExperimentalTextApi::class)
private fun grotesk(weight: Int) = Font(
    R.font.space_grotesk,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Display = FontFamily(grotesk(500), grotesk(600), grotesk(700))

private val base = Typography()

private val AppTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Display, fontWeight = FontWeight.Bold, letterSpacing = (-1.5).sp),
    displayMedium = base.displayMedium.copy(fontFamily = Display, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
    displaySmall = base.displaySmall.copy(fontFamily = Display, fontWeight = FontWeight.Bold),
    headlineLarge = base.headlineLarge.copy(fontFamily = Display, fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontFamily = Display, fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontFamily = Display, fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontFamily = Display, fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

/** Big countdown numerals. */
val CountdownStyle = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 84.sp, lineHeight = 84.sp, letterSpacing = (-2).sp)

@Composable
fun ExamClockTheme(themeMode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
