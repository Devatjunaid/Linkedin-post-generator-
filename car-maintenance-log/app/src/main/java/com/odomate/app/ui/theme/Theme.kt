package com.odomate.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.odomate.app.util.findActivity
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.odomate.app.R
import com.odomate.app.data.ThemeMode
import com.odomate.app.domain.Level

val Archivo = FontFamily(
    Font(R.font.archivo_regular, FontWeight.Normal),
    Font(R.font.archivo_medium, FontWeight.Medium),
    Font(R.font.archivo_semibold, FontWeight.SemiBold),
    Font(R.font.archivo_bold, FontWeight.Bold),
    Font(R.font.archivo_extrabold, FontWeight.ExtraBold),
)

val Mono = FontFamily(
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
)

@Immutable
data class Palette(
    val isDark: Boolean,
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val outline: Color,
    val divider: Color,
    val text: Color,
    val muted: Color,
    val faint: Color,
    val accent: Color,
    val onAccent: Color,
    val accentText: Color,
    val accentSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
    val warn: Color,
    val warnSoft: Color,
    val ok: Color,
    val okSoft: Color,
    val lcdBg: Color,
    val lcdText: Color,
) {
    fun levelColor(level: Level): Color = when (level) {
        Level.OVERDUE -> danger
        Level.SOON -> warn
        Level.OK -> ok
        Level.UNKNOWN, Level.DISABLED -> muted
    }

    fun levelSoft(level: Level): Color = when (level) {
        Level.OVERDUE -> dangerSoft
        Level.SOON -> warnSoft
        Level.OK -> okSoft
        Level.UNKNOWN, Level.DISABLED -> surface2
    }
}

val DarkPalette = Palette(
    isDark = true,
    bg = Color(0xFF0E1113),
    surface = Color(0xFF181D21),
    surface2 = Color(0xFF20262B),
    outline = Color(0xFF2A3137),
    divider = Color(0xFF242B30),
    text = Color(0xFFEEF1F3),
    muted = Color(0xFF9AA5AE),
    faint = Color(0xFF5C6770),
    accent = Color(0xFFF5A524),
    onAccent = Color(0xFF1A1204),
    accentText = Color(0xFFF5A524),
    accentSoft = Color(0xFF3A2C12),
    danger = Color(0xFFFF6B57),
    dangerSoft = Color(0xFF3A1F1B),
    warn = Color(0xFFF5A524),
    warnSoft = Color(0xFF3A2C12),
    ok = Color(0xFF46C7A8),
    okSoft = Color(0xFF15302A),
    lcdBg = Color(0xFF1B2A22),
    lcdText = Color(0xFF9BE3B9),
)

val LightPalette = Palette(
    isDark = false,
    bg = Color(0xFFF3F4F2),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFEBEEEC),
    outline = Color(0xFFDADFDC),
    divider = Color(0xFFE6E9E7),
    text = Color(0xFF12171B),
    muted = Color(0xFF56616A),
    faint = Color(0xFF9AA3AA),
    accent = Color(0xFFF5A524),
    onAccent = Color(0xFF1A1204),
    accentText = Color(0xFF8F5A00),
    accentSoft = Color(0xFFFCEFD6),
    danger = Color(0xFFC63B26),
    dangerSoft = Color(0xFFFBE3DF),
    warn = Color(0xFF9A6200),
    warnSoft = Color(0xFFFCEFD6),
    ok = Color(0xFF16806A),
    okSoft = Color(0xFFDDF3EC),
    lcdBg = Color(0xFF1B2A22),
    lcdText = Color(0xFF9BE3B9),
)

private val LocalPalette = staticCompositionLocalOf { DarkPalette }

object AppTheme {
    val colors: Palette
        @Composable get() = LocalPalette.current
}

private val AppTypography = Typography().let { base ->
    Typography(
        displayLarge = base.displayLarge.copy(fontFamily = Archivo, fontWeight = FontWeight.ExtraBold),
        displayMedium = base.displayMedium.copy(fontFamily = Archivo, fontWeight = FontWeight.ExtraBold),
        displaySmall = base.displaySmall.copy(fontFamily = Archivo, fontWeight = FontWeight.ExtraBold),
        headlineLarge = base.headlineLarge.copy(fontFamily = Archivo, fontWeight = FontWeight.ExtraBold),
        headlineMedium = base.headlineMedium.copy(fontFamily = Archivo, fontWeight = FontWeight.ExtraBold),
        headlineSmall = base.headlineSmall.copy(fontFamily = Archivo, fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontFamily = Archivo, fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontFamily = Archivo, fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.copy(fontFamily = Archivo, fontWeight = FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.copy(fontFamily = Archivo),
        bodyMedium = base.bodyMedium.copy(fontFamily = Archivo),
        bodySmall = base.bodySmall.copy(fontFamily = Archivo),
        labelLarge = base.labelLarge.copy(fontFamily = Archivo, fontWeight = FontWeight.SemiBold),
        labelMedium = base.labelMedium.copy(fontFamily = Archivo, fontWeight = FontWeight.SemiBold),
        labelSmall = base.labelSmall.copy(fontFamily = Archivo, fontWeight = FontWeight.SemiBold),
    )
}

object Type {
    val overline = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 1.4.sp)
    val mono = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Bold)
}

@Composable
fun OdoMateTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val p = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) {
        darkColorScheme(
            primary = p.accent, onPrimary = p.onAccent, primaryContainer = p.accentSoft, onPrimaryContainer = p.accentText,
            secondary = p.ok, onSecondary = p.onAccent,
            background = p.bg, onBackground = p.text,
            surface = p.surface, onSurface = p.text, surfaceVariant = p.surface2, onSurfaceVariant = p.muted,
            surfaceContainer = p.surface, surfaceContainerHigh = p.surface2, surfaceContainerHighest = p.surface2,
            surfaceContainerLow = p.surface, surfaceContainerLowest = p.bg,
            outline = p.outline, outlineVariant = p.divider, error = p.danger,
        )
    } else {
        lightColorScheme(
            primary = p.accent, onPrimary = p.onAccent, primaryContainer = p.accentSoft, onPrimaryContainer = p.accentText,
            secondary = p.ok, onSecondary = Color.White,
            background = p.bg, onBackground = p.text,
            surface = p.surface, onSurface = p.text, surfaceVariant = p.surface2, onSurfaceVariant = p.muted,
            surfaceContainer = p.surface, surfaceContainerHigh = p.surface2, surfaceContainerHighest = p.surface2,
            surfaceContainerLow = p.surface, surfaceContainerLowest = p.bg,
            outline = p.outline, outlineVariant = p.divider, error = p.danger,
        )
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
    }
}
