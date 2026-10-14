package com.devatjunaid.renewalert.ui.theme

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devatjunaid.renewalert.R
import com.devatjunaid.renewalert.data.ThemeMode

object Brand {
    val Blue = Color(0xFF3D63FF)
    val Indigo = Color(0xFF4F4BF0)
    val Violet = Color(0xFF6A3DE8)
    val Orange = Color(0xFFFF8A3D)
    val Gold = Color(0xFFFFB547)
    val Navy = Color(0xFF0B1026)
    val NavyLight = Color(0xFF24206B)
    val gradient = Brush.linearGradient(listOf(Blue, Violet))
    val coin = Brush.linearGradient(listOf(Gold, Orange))
}

@Immutable
data class RenewAlertColors(
    val background: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val onSurface: Color,
    val muted: Color,
    val outline: Color,
    val divider: Color,
    val accent: Color,
    val accentSoft: Color,
    val onAccentSoft: Color,
    val hero: Color,
    val onHero: Color,
    val onHeroMuted: Color,
    val warn: Color,
    val warnContainer: Color,
    val warnText: Color,
    val success: Color,
    val successContainer: Color,
    val successText: Color,
    val danger: Color,
    val isDark: Boolean,
)

private val LightColors = RenewAlertColors(
    background = Color(0xFFF3F4F0),
    surface = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFE8E9E4),
    onSurface = Color(0xFF15171C),
    muted = Color(0xFF5B616B),
    outline = Color(0xFFE2E4DE),
    divider = Color(0xFFEEEFEB),
    accent = Color(0xFF3355EE),
    accentSoft = Color(0xFFE6ECFD),
    onAccentSoft = Color(0xFF1C3FB8),
    hero = Color(0xFF15171C),
    onHero = Color(0xFFFFFFFF),
    onHeroMuted = Color(0xFFC9CCD2),
    warn = Color(0xFFE07A1F),
    warnContainer = Color(0xFFFDEEDF),
    warnText = Color(0xFF8A3D08),
    success = Color(0xFF1B7A4B),
    successContainer = Color(0xFFE1F2E8),
    successText = Color(0xFF135C38),
    danger = Color(0xFFB3261E),
    isDark = false,
)

private val DarkColors = RenewAlertColors(
    background = Color(0xFF0F1115),
    surface = Color(0xFF181B21),
    surfaceAlt = Color(0xFF22262E),
    onSurface = Color(0xFFECEDEF),
    muted = Color(0xFFA2A8B3),
    outline = Color(0xFF2C3039),
    divider = Color(0xFF242830),
    accent = Color(0xFF8AA2FF),
    accentSoft = Color(0xFF232C4D),
    onAccentSoft = Color(0xFFC3D0FF),
    hero = Color(0xFF1E2230),
    onHero = Color(0xFFFFFFFF),
    onHeroMuted = Color(0xFFB9BDC6),
    warn = Color(0xFFFF9E57),
    warnContainer = Color(0xFF3A2414),
    warnText = Color(0xFFFFC08F),
    success = Color(0xFF4CC38A),
    successContainer = Color(0xFF16301F),
    successText = Color(0xFF9FE0B9),
    danger = Color(0xFFFF8A80),
    isDark = true,
)

val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_semibold, FontWeight.SemiBold),
    Font(R.font.space_grotesk_bold, FontWeight.Bold),
)

val DmSans = FontFamily(
    Font(R.font.dm_sans_regular, FontWeight.Normal),
    Font(R.font.dm_sans_medium, FontWeight.Medium),
    Font(R.font.dm_sans_semibold, FontWeight.SemiBold),
    Font(R.font.dm_sans_bold, FontWeight.Bold),
)

private val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 44.sp, letterSpacing = (-1).sp),
    displayMedium = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 36.sp, letterSpacing = (-0.8).sp),
    displaySmall = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 30.sp, letterSpacing = (-0.6).sp),
    headlineLarge = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = (-0.4).sp),
    headlineSmall = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Bold, fontSize = 18.sp),
    titleMedium = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Bold, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val LocalRenewAlertColors = staticCompositionLocalOf { LightColors }

object RenewAlertTheme {
    val colors: RenewAlertColors
        @Composable get() = LocalRenewAlertColors.current
}

/** Money and big numbers. */
val NumberStyle = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold)

@Composable
fun RenewAlertTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val c = if (dark) DarkColors else LightColors
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.accent, onPrimary = Color(0xFF0F1115), primaryContainer = c.accentSoft,
            onPrimaryContainer = c.onAccentSoft, secondary = c.warn, background = c.background,
            onBackground = c.onSurface, surface = c.surface, onSurface = c.onSurface,
            surfaceVariant = c.surfaceAlt, onSurfaceVariant = c.muted, outline = c.outline,
            outlineVariant = c.divider, error = c.danger, surfaceContainer = c.surface,
            surfaceContainerHigh = c.surfaceAlt, surfaceContainerLow = c.surface,
            surfaceContainerHighest = c.surfaceAlt, surfaceContainerLowest = c.background,
        )
    } else {
        lightColorScheme(
            primary = c.accent, onPrimary = Color.White, primaryContainer = c.accentSoft,
            onPrimaryContainer = c.onAccentSoft, secondary = c.warn, background = c.background,
            onBackground = c.onSurface, surface = c.surface, onSurface = c.onSurface,
            surfaceVariant = c.surfaceAlt, onSurfaceVariant = c.muted, outline = c.outline,
            outlineVariant = c.divider, error = c.danger, surfaceContainer = c.surface,
            surfaceContainerHigh = Color(0xFFF7F8F5), surfaceContainerLow = c.surface,
            surfaceContainerHighest = c.surfaceAlt, surfaceContainerLowest = Color.White,
        )
    }
    CompositionLocalProvider(LocalRenewAlertColors provides c) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, shapes = AppShapes, content = content)
    }
}

/** Tile colours per category: (background, foreground) for light and dark. */
object CategoryColors {
    private val light = mapOf(
        "Video" to (Color(0xFFF0E8FB) to Color(0xFF7A3FCB)),
        "Music" to (Color(0xFFDFF3F1) to Color(0xFF0F7F77)),
        "Productivity" to (Color(0xFFE6ECFD) to Color(0xFF2A55E5)),
        "Cloud" to (Color(0xFFE3F0FB) to Color(0xFF1F6FB2)),
        "Education" to (Color(0xFFE1F2E8) to Color(0xFF1B7A4B)),
        "Health" to (Color(0xFFFDEEDF) to Color(0xFFC2560C)),
        "News" to (Color(0xFFECEDEA) to Color(0xFF4E545E)),
        "Gaming" to (Color(0xFFFCE7EF) to Color(0xFFB4235A)),
        "Shopping" to (Color(0xFFFFF4D6) to Color(0xFF8A6100)),
        "Finance" to (Color(0xFFE5F3E9) to Color(0xFF2E7D32)),
        "Utilities" to (Color(0xFFE9EEF3) to Color(0xFF3E5468)),
    )
    private val fallback = listOf(
        Color(0xFFECEDEA) to Color(0xFF4E545E),
        Color(0xFFE6ECFD) to Color(0xFF2A55E5),
        Color(0xFFF0E8FB) to Color(0xFF7A3FCB),
    )

    /** Strong colour for charts, readable on both themes. */
    fun chart(category: String): Color = forCategory(category, false).second

    fun forCategory(category: String, dark: Boolean): Pair<Color, Color> {
        val pair = light[category] ?: fallback[(category.hashCode() and 0x7fffffff) % fallback.size]
        return if (dark) pair.second.copy(alpha = 0.22f).compositeOverDark() to lighten(pair.second) else pair
    }

    private fun Color.compositeOverDark(): Color {
        val base = Color(0xFF181B21)
        val a = alpha
        return Color(red * a + base.red * (1 - a), green * a + base.green * (1 - a), blue * a + base.blue * (1 - a))
    }

    private fun lighten(c: Color): Color = Color(
        c.red + (1 - c.red) * 0.45f, c.green + (1 - c.green) * 0.45f, c.blue + (1 - c.blue) * 0.45f,
    )
}
