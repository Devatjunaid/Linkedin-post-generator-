package com.snaptensio.bplog.ui.theme

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
import com.snaptensio.bplog.R
import com.snaptensio.bplog.data.ThemeMode

object Brand {
    val Blue = Color(0xFF1F4FD8)
    val BlueBright = Color(0xFF2F6BFF)
    val BlueDeep = Color(0xFF1638A8)
    val Coral = Color(0xFFFF5A5F)
    val Night = Color(0xFF0B1220)
    val Systolic = Color(0xFF1F4FD8)
    val Diastolic = Color(0xFFE07A2B)
    val Good = Color(0xFF1E8E5A)
}

@OptIn(ExperimentalTextApi::class)
private fun manrope(weight: Int) = Font(
    R.font.manrope,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

@OptIn(ExperimentalTextApi::class)
val Manrope = FontFamily(manrope(400), manrope(500), manrope(600), manrope(700), manrope(800))

private val LightColors = lightColorScheme(
    primary = Brand.Blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8EEFC),
    onPrimaryContainer = Color(0xFF0E2A7A),
    secondary = Brand.Coral,
    onSecondary = Color.White,
    background = Color(0xFFF4F6F8),
    onBackground = Color(0xFF0F1B2D),
    surface = Color.White,
    onSurface = Color(0xFF0F1B2D),
    surfaceVariant = Color(0xFFEEF1F5),
    onSurfaceVariant = Color(0xFF556274),
    outline = Color(0xFFC9D2DE),
    outlineVariant = Color(0xFFE2E7EE),
    error = Color(0xFFC2272D),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7C9DFF),
    onPrimary = Color(0xFF071A55),
    primaryContainer = Color(0xFF1B2C5C),
    onPrimaryContainer = Color(0xFFDCE4FF),
    secondary = Brand.Coral,
    onSecondary = Color.White,
    background = Brand.Night,
    onBackground = Color(0xFFEEF2F8),
    surface = Color(0xFF16202F),
    onSurface = Color(0xFFEEF2F8),
    surfaceVariant = Color(0xFF1E2A3B),
    onSurfaceVariant = Color(0xFFA9B4C4),
    outline = Color(0xFF3A475B),
    outlineVariant = Color(0xFF263244),
    error = Color(0xFFFF8A8E),
)

private fun t(size: Int, weight: Int, line: Int = (size * 1.35).toInt()) =
    TextStyle(fontFamily = Manrope, fontWeight = FontWeight(weight), fontSize = size.sp, lineHeight = line.sp)

private val AppTypography = Typography(
    displayLarge = t(60, 800, 64),
    displayMedium = t(44, 800, 48),
    displaySmall = t(34, 800, 40),
    headlineLarge = t(28, 800),
    headlineMedium = t(24, 800),
    headlineSmall = t(20, 800),
    titleLarge = t(18, 800),
    titleMedium = t(16, 700),
    titleSmall = t(14, 700),
    bodyLarge = t(16, 500),
    bodyMedium = t(14, 500),
    bodySmall = t(12, 600),
    labelLarge = t(14, 700),
    labelMedium = t(12, 700),
    labelSmall = t(11, 700),
)

@Composable
fun SnapTensioTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, typography = AppTypography, content = content)
}
