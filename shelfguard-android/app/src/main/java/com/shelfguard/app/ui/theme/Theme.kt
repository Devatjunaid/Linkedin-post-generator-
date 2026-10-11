package com.shelfguard.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Brand {
    val Deep = Color(0xFF0B4A33)
    val Green = Color(0xFF1E6B47)
    val GreenBright = Color(0xFF1A8A5A)
    val Mint = Color(0xFF8EDDB8)
    val MintPale = Color(0xFFCFF3E1)
    val Amber = Color(0xFFF59E0B)
    val AmberTape = Color(0xFFF5A524)
    val AmberDark = Color(0xFFD9861A)
}

/** Colours that carry meaning: each status differs in lightness as well as hue. */
@Immutable
data class StatusColors(
    val expired: Color,
    val expiredContainer: Color,
    val expiring: Color,
    val expiringContainer: Color,
    val low: Color,
    val lowContainer: Color,
    val out: Color,
    val outContainer: Color,
    val ok: Color,
    val okContainer: Color,
)

private val LightStatus = StatusColors(
    expired = Color(0xFFB42318), expiredContainer = Color(0xFFFBE9E7),
    expiring = Color(0xFFA14B05), expiringContainer = Color(0xFFFDF0E1),
    low = Color(0xFF1F4FBF), lowContainer = Color(0xFFE6EDFB),
    out = Color(0xFF4A5550), outContainer = Color(0xFFE9ECE8),
    ok = Color(0xFF1E6B47), okContainer = Color(0xFFE3F0E8),
)

private val DarkStatus = StatusColors(
    expired = Color(0xFFFFB4A9), expiredContainer = Color(0xFF4A1A14),
    expiring = Color(0xFFFFC98A), expiringContainer = Color(0xFF452A0B),
    low = Color(0xFFB3C8FF), lowContainer = Color(0xFF1B2C55),
    out = Color(0xFFC3CCC7), outContainer = Color(0xFF2E3833),
    ok = Color(0xFF8EDDB8), okContainer = Color(0xFF173B2A),
)

val LocalStatusColors = staticCompositionLocalOf { LightStatus }

private val LightScheme = lightColorScheme(
    primary = Brand.Green,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3F0E8),
    onPrimaryContainer = Color(0xFF0B3B26),
    secondary = Brand.AmberDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFDF0E1),
    onSecondaryContainer = Color(0xFF4A2A00),
    background = Color(0xFFF4F5F2),
    onBackground = Color(0xFF17201B),
    surface = Color(0xFFF4F5F2),
    onSurface = Color(0xFF17201B),
    surfaceVariant = Color(0xFFEEF1EC),
    onSurfaceVariant = Color(0xFF5B6660),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF9FAF8),
    surfaceContainerHighest = Color(0xFFEEF1EC),
    outline = Color(0xFFC9CFC9),
    outlineVariant = Color(0xFFE3E6E1),
    error = Color(0xFFB42318),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF7FD4AA),
    onPrimary = Color(0xFF00391F),
    primaryContainer = Color(0xFF175236),
    onPrimaryContainer = Color(0xFFCFF3E1),
    secondary = Color(0xFFFFC98A),
    onSecondary = Color(0xFF452A0B),
    background = Color(0xFF111714),
    onBackground = Color(0xFFE8EEEA),
    surface = Color(0xFF111714),
    onSurface = Color(0xFFE8EEEA),
    surfaceVariant = Color(0xFF232D28),
    onSurfaceVariant = Color(0xFFA3AFA8),
    surfaceContainerLowest = Color(0xFF151C18),
    surfaceContainerLow = Color(0xFF1B2420),
    surfaceContainer = Color(0xFF1B2420),
    surfaceContainerHigh = Color(0xFF212B26),
    surfaceContainerHighest = Color(0xFF29342E),
    outline = Color(0xFF45514B),
    outlineVariant = Color(0xFF2C3732),
    error = Color(0xFFFFB4A9),
)

private val base = FontFamily.SansSerif

private val AppTypography = Typography(
    displaySmall = TextStyle(fontFamily = base, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = base, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, letterSpacing = (-0.4).sp),
    headlineSmall = TextStyle(fontFamily = base, fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontFamily = base, fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = base, fontWeight = FontWeight.Bold, fontSize = 17.sp),
    titleSmall = TextStyle(fontFamily = base, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    bodyLarge = TextStyle(fontFamily = base, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = base, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = base, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = base, fontWeight = FontWeight.Bold, fontSize = 15.sp),
    labelMedium = TextStyle(fontFamily = base, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
    labelSmall = TextStyle(fontFamily = base, fontWeight = FontWeight.Bold, fontSize = 11.sp),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun ShelfGuardTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalStatusColors provides if (dark) DarkStatus else LightStatus) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

object AppTheme {
    val status: StatusColors
        @Composable get() = LocalStatusColors.current
}
