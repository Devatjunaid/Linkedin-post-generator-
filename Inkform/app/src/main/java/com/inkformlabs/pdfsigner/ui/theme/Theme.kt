package com.inkformlabs.pdfsigner.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.inkformlabs.pdfsigner.R
import com.inkformlabs.pdfsigner.data.ThemeChoice

/** The colour roles of one Inkform theme. */
@Immutable
data class InkPalette(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val accent: Color,
    val onAccent: Color,
    val accent2: Color,
    val hero: Color,
    val onHero: Color,
    /** Strong accent that is readable as text on [bg]/[surface]. */
    val hi: Color,
    val onHi: Color,
    val isDark: Boolean,
)

fun paletteFor(choice: ThemeChoice): InkPalette = when (choice) {
    ThemeChoice.CITRUS -> InkPalette(
        bg = Color(0xFFF4F2EC), surface = Color(0xFFFFFFFF), surface2 = Color(0xFFEAE7DE),
        ink = Color(0xFF17181C), muted = Color(0xFF5E5F66), line = Color(0xFFDAD6CB),
        accent = Color(0xFFD7F25A), onAccent = Color(0xFF17181C), accent2 = Color(0xFFFF7A45),
        hero = Color(0xFF17181C), onHero = Color(0xFFF4F2EC),
        hi = Color(0xFF4A6B00), onHi = Color(0xFFFFFFFF), isDark = false,
    )
    ThemeChoice.MIDNIGHT -> InkPalette(
        bg = Color(0xFF0D171B), surface = Color(0xFF15232A), surface2 = Color(0xFF1F333C),
        ink = Color(0xFFE6F0EE), muted = Color(0xFF9AB0AC), line = Color(0xFF2A4550),
        accent = Color(0xFF3FD0B6), onAccent = Color(0xFF062019), accent2 = Color(0xFFF5B54A),
        hero = Color(0xFF3FD0B6), onHero = Color(0xFF062019),
        hi = Color(0xFF5FE0C8), onHi = Color(0xFF062019), isDark = true,
    )
    ThemeChoice.CORAL -> InkPalette(
        bg = Color(0xFFFFF6F0), surface = Color(0xFFFFFFFF), surface2 = Color(0xFFFCE6DA),
        ink = Color(0xFF2B1911), muted = Color(0xFF7A5A4C), line = Color(0xFFF0D5C6),
        accent = Color(0xFFFF7A59), onAccent = Color(0xFF2B1911), accent2 = Color(0xFF2F6BFF),
        hero = Color(0xFF2B1911), onHero = Color(0xFFFFF6F0),
        hi = Color(0xFFC2401F), onHi = Color(0xFFFFFFFF), isDark = false,
    )
    ThemeChoice.VIOLET -> InkPalette(
        bg = Color(0xFFF4F2FF), surface = Color(0xFFFFFFFF), surface2 = Color(0xFFE7E3FF),
        ink = Color(0xFF1A1640), muted = Color(0xFF5E5A85), line = Color(0xFFDCD7F7),
        accent = Color(0xFF5B3DF5), onAccent = Color(0xFFFFFFFF), accent2 = Color(0xFFFFB547),
        hero = Color(0xFF5B3DF5), onHero = Color(0xFFFFFFFF),
        hi = Color(0xFF4B2EE0), onHi = Color(0xFFFFFFFF), isDark = false,
    )
}

val LocalInk = staticCompositionLocalOf { paletteFor(ThemeChoice.CITRUS) }

/** Fixed colours used on "paper" (page previews, signature pad), independent of the theme. */
object Paper {
    val sheet = Color(0xFFFFFDF8)
    val line = Color(0xFFCFCBC0)
    val hint = Color(0xFF6E6B62)
    val inkBlack = Color(0xFF17181C)
    val inkBlue = Color(0xFF1F3FBF)
    val inkBurgundy = Color(0xFF8C1D3A)
}

val Bricolage = FontFamily(
    Font(R.font.bricolage_700, FontWeight.Bold),
    Font(R.font.bricolage_800, FontWeight.ExtraBold),
)

val Manrope = FontFamily(
    Font(R.font.manrope_400, FontWeight.Normal),
    Font(R.font.manrope_500, FontWeight.Medium),
    Font(R.font.manrope_600, FontWeight.SemiBold),
    Font(R.font.manrope_700, FontWeight.Bold),
    Font(R.font.manrope_800, FontWeight.ExtraBold),
)

private val InkTypography: Typography = Typography().let { base ->
    fun TextStyle.body() = copy(fontFamily = Manrope)
    fun TextStyle.display(weight: FontWeight = FontWeight.ExtraBold) =
        copy(fontFamily = Bricolage, fontWeight = weight, letterSpacing = (-0.4).sp)
    base.copy(
        displayLarge = base.displayLarge.display(),
        displayMedium = base.displayMedium.display(),
        displaySmall = base.displaySmall.display(),
        headlineLarge = base.headlineLarge.display(),
        headlineMedium = base.headlineMedium.display(),
        headlineSmall = base.headlineSmall.display(),
        titleLarge = base.titleLarge.display(FontWeight.Bold),
        titleMedium = base.titleMedium.body().copy(fontWeight = FontWeight.ExtraBold),
        titleSmall = base.titleSmall.body().copy(fontWeight = FontWeight.Bold),
        bodyLarge = base.bodyLarge.body(),
        bodyMedium = base.bodyMedium.body(),
        bodySmall = base.bodySmall.body(),
        labelLarge = base.labelLarge.body().copy(fontWeight = FontWeight.Bold),
        labelMedium = base.labelMedium.body().copy(fontWeight = FontWeight.Bold),
        labelSmall = base.labelSmall.body().copy(fontWeight = FontWeight.Bold),
    )
}

@Composable
private fun animated(target: Color, label: String): Color =
    animateColorAsState(target, animationSpec = tween(550), label = label).value

@Composable
fun InkformTheme(choice: ThemeChoice, content: @Composable () -> Unit) {
    val t = paletteFor(choice)
    val p = InkPalette(
        bg = animated(t.bg, "bg"),
        surface = animated(t.surface, "surface"),
        surface2 = animated(t.surface2, "surface2"),
        ink = animated(t.ink, "ink"),
        muted = animated(t.muted, "muted"),
        line = animated(t.line, "line"),
        accent = animated(t.accent, "accent"),
        onAccent = animated(t.onAccent, "onAccent"),
        accent2 = animated(t.accent2, "accent2"),
        hero = animated(t.hero, "hero"),
        onHero = animated(t.onHero, "onHero"),
        hi = animated(t.hi, "hi"),
        onHi = animated(t.onHi, "onHi"),
        isDark = t.isDark,
    )
    val scheme = if (p.isDark) {
        darkColorScheme(
            primary = p.hi, onPrimary = p.onHi, secondary = p.accent2, onSecondary = p.bg,
            background = p.bg, onBackground = p.ink, surface = p.surface, onSurface = p.ink,
            surfaceVariant = p.surface2, onSurfaceVariant = p.muted, outline = p.line, outlineVariant = p.line,
            surfaceContainer = p.surface, surfaceContainerHigh = p.surface, surfaceContainerHighest = p.surface2,
            surfaceContainerLow = p.surface, surfaceContainerLowest = p.bg,
            primaryContainer = p.surface2, onPrimaryContainer = p.ink,
            secondaryContainer = p.surface2, onSecondaryContainer = p.ink,
        )
    } else {
        lightColorScheme(
            primary = p.hi, onPrimary = p.onHi, secondary = p.accent2, onSecondary = p.bg,
            background = p.bg, onBackground = p.ink, surface = p.surface, onSurface = p.ink,
            surfaceVariant = p.surface2, onSurfaceVariant = p.muted, outline = p.line, outlineVariant = p.line,
            surfaceContainer = p.surface, surfaceContainerHigh = p.surface, surfaceContainerHighest = p.surface2,
            surfaceContainerLow = p.surface, surfaceContainerLowest = p.bg,
            primaryContainer = p.surface2, onPrimaryContainer = p.ink,
            secondaryContainer = p.surface2, onSecondaryContainer = p.ink,
        )
    }
    MaterialTheme(colorScheme = scheme, typography = InkTypography) {
        CompositionLocalProvider(LocalInk provides p, content = content)
    }
}
