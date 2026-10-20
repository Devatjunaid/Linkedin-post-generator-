package com.superdevlabs.autosubtitles.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.superdevlabs.autosubtitles.R

object AppColors {
    val Ink = Color(0xFF0E0E12)
    val Surface = Color(0xFF17171D)
    val SurfaceHigh = Color(0xFF202029)
    val SurfaceTop = Color(0xFF2A2A35)
    val Line = Color(0xFF2A2A35)
    val Text = Color(0xFFF2F1EC)
    val Muted = Color(0xFF9A99A6)
    val Soft = Color(0xFFB9B8C4)
    val Lime = Color(0xFFD4FF3A)
    val Coral = Color(0xFFFF7A59)
    val Ice = Color(0xFF8DF0FF)
    val Danger = Color(0xFFFF5D5D)
}

@OptIn(ExperimentalTextApi::class)
private fun bricolage(weight: Int) = Font(
    R.font.bricolage,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

@OptIn(ExperimentalTextApi::class)
private fun mono(weight: Int) = Font(
    R.font.jetbrains_mono,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Display = FontFamily(bricolage(400), bricolage(600), bricolage(700), bricolage(800))
val Mono = FontFamily(mono(400), mono(600))

private val typography = Typography(
    displayLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp, lineHeight = 40.sp, letterSpacing = (-1.4).sp),
    headlineMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = (-0.6).sp),
    titleLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 18.sp),
    bodyLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 15.sp),
    labelMedium = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Normal, fontSize = 11.sp, letterSpacing = 1.2.sp),
    labelSmall = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Normal, fontSize = 10.sp, letterSpacing = 0.6.sp),
)

private val colors = darkColorScheme(
    primary = AppColors.Lime,
    onPrimary = AppColors.Ink,
    secondary = AppColors.Coral,
    onSecondary = AppColors.Ink,
    background = AppColors.Ink,
    onBackground = AppColors.Text,
    surface = AppColors.Surface,
    onSurface = AppColors.Text,
    surfaceVariant = AppColors.SurfaceHigh,
    onSurfaceVariant = AppColors.Muted,
    outline = AppColors.Line,
    error = AppColors.Danger,
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}
