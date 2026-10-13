package com.aquadoselab.poolchemistry.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object AppColors {
    val Navy = Color(0xFF0E2A3B)
    val NavySoft = Color(0xFF15405A)
    val Teal = Color(0xFF0B7A93)
    val TealDark = Color(0xFF075A6C)
    val Aqua = Color(0xFF2BB7CF)
    val AquaLight = Color(0xFF5FD0E4)
    val Ground = Color(0xFFF3F8FA)
    val Card = Color(0xFFFFFFFF)
    val Ink = Color(0xFF0E2A3B)
    val InkSoft = Color(0xFF2F4756)
    val Muted = Color(0xFF4E6573)
    val OnNavyMuted = Color(0xFFC9DCE4)
    val Border = Color(0xFFE3ECF0)
    val BorderStrong = Color(0xFFD5E2E8)
    val Track = Color(0xFFE3ECF0)
    val Zone = Color(0xFFA9D8E3)
    val OkBg = Color(0xFFE1F3F7)
    val OkText = Color(0xFF075A6C)
    val WarnBg = Color(0xFFFDEFE3)
    val WarnText = Color(0xFF9A4510)
    val WarnBorder = Color(0xFFF2C9A3)
    val WarnDot = Color(0xFFF2A65A)
    val Danger = Color(0xFFB42318)
    val DangerBg = Color(0xFFFDECEA)
}

private val Colors = lightColorScheme(
    primary = AppColors.Teal,
    onPrimary = Color.White,
    primaryContainer = AppColors.OkBg,
    onPrimaryContainer = AppColors.OkText,
    secondary = AppColors.Navy,
    onSecondary = Color.White,
    background = AppColors.Ground,
    onBackground = AppColors.Ink,
    surface = AppColors.Card,
    onSurface = AppColors.Ink,
    surfaceVariant = AppColors.Ground,
    onSurfaceVariant = AppColors.Muted,
    surfaceContainer = AppColors.Card,
    surfaceContainerHigh = AppColors.Card,
    surfaceContainerHighest = AppColors.Ground,
    outline = AppColors.BorderStrong,
    outlineVariant = AppColors.Border,
    error = AppColors.Danger
)

private val Base = FontFamily.Default

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontFamily = Base, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = Base, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, letterSpacing = (-0.4).sp),
    headlineSmall = TextStyle(fontFamily = Base, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleLarge = TextStyle(fontFamily = Base, fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = Base, fontWeight = FontWeight.Bold, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = Base, fontWeight = FontWeight.Bold, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = Base, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Base, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Base, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Base, fontWeight = FontWeight.Bold, fontSize = 15.sp),
    labelMedium = TextStyle(fontFamily = Base, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = Base, fontWeight = FontWeight.Bold, fontSize = 11.sp)
)

@Composable
fun AquaDoseTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = AppTypography, content = content)
}
