package com.walkies.dogwalk.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

object WalkiesColors {
    val Forest = Color(0xFF18392B)
    val ForestSoft = Color(0xFF2E6650)
    val Ground = Color(0xFFEEF2EC)
    val Card = Color(0xFFFFFFFF)
    val Ink = Color(0xFF1D2621)
    val Muted = Color(0xFF4E5B53)
    val Line = Color(0xFFE2E8E4)
    val Cream = Color(0xFFFFF6EA)

    val Walk = Color(0xFFFF7A3D)
    val WalkTint = Color(0xFFFFE3D4)
    val WalkDeep = Color(0xFFC2410C)

    val Pee = Color(0xFFF5B700)
    val PeeTint = Color(0xFFFFF1C2)
    val PeeDeep = Color(0xFF8A6500)

    val Poo = Color(0xFF7A4E31)
    val PooTint = Color(0xFFEBDDD2)
    val PooDeep = Color(0xFF5A3721)

    val Meal = Color(0xFF2F5BB7)
    val MealTint = Color(0xFFE3ECF9)

    val Sky = Color(0xFFCFE3FF)
    val SkyDeep = Color(0xFF22457F)
}

private val colors = lightColorScheme(
    primary = WalkiesColors.Forest,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5E6DC),
    onPrimaryContainer = WalkiesColors.Forest,
    secondary = WalkiesColors.Walk,
    onSecondary = WalkiesColors.Ink,
    secondaryContainer = WalkiesColors.WalkTint,
    onSecondaryContainer = WalkiesColors.Ink,
    tertiary = WalkiesColors.Pee,
    onTertiary = WalkiesColors.Ink,
    background = WalkiesColors.Ground,
    onBackground = WalkiesColors.Ink,
    surface = WalkiesColors.Card,
    onSurface = WalkiesColors.Ink,
    surfaceVariant = Color(0xFFE3E9E4),
    onSurfaceVariant = WalkiesColors.Muted,
    surfaceContainer = WalkiesColors.Card,
    surfaceContainerHigh = WalkiesColors.Card,
    surfaceContainerHighest = Color(0xFFE3E9E4),
    surfaceContainerLow = WalkiesColors.Ground,
    outline = Color(0xFF9AA69F),
    outlineVariant = Color(0xFFC9D3CD),
)

private val base = Typography()

private val typography = Typography(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Bold),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.Bold),
    bodyLarge = base.bodyLarge,
    bodyMedium = base.bodyMedium,
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Bold),
    labelMedium = base.labelMedium.copy(fontWeight = FontWeight.SemiBold),
)

/** Big stopwatch-style numbers. */
val TimerTextStyle = TextStyle(fontSize = 56.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)

private val shapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
fun WalkiesTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, shapes = shapes, content = content)
}
