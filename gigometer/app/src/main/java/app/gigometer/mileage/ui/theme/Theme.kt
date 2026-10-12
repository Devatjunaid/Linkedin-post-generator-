package app.gigometer.mileage.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.gigometer.mileage.R

@Immutable
data class Palette(
    val dark: Boolean,
    val ground: Color,
    val card: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val accent: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val bright: Color,
    val amber: Color,
    val amberSoft: Color,
    val amberText: Color,
    val danger: Color,
    val hero: Color,
    val heroInk: Color,
    val heroMuted: Color,
    val heroTrack: Color,
    val neutralBar: Color,
)

val LightPalette = Palette(
    dark = false,
    ground = Color(0xFFF4F5F0), card = Color(0xFFFFFFFF), ink = Color(0xFF14211B), muted = Color(0xFF55615A),
    line = Color(0xFFE1E5DE), accent = Color(0xFF1B6B45), onAccent = Color.White, accentSoft = Color(0x1A1B6B45),
    bright = Color(0xFF4FD18B), amber = Color(0xFFF2B33D), amberSoft = Color(0xFFFFF1D6), amberText = Color(0xFF7A4700),
    danger = Color(0xFFB4421F), hero = Color(0xFF14211B), heroInk = Color(0xFFFFFFFF), heroMuted = Color(0xFFB9C6BE),
    heroTrack = Color(0xFF2C3B33), neutralBar = Color(0xFFB8C1BB),
)

val DarkPalette = Palette(
    dark = true,
    ground = Color(0xFF0D1612), card = Color(0xFF17241E), ink = Color(0xFFEFF3F0), muted = Color(0xFFA7B4AC),
    line = Color(0xFF26352D), accent = Color(0xFF3FBF7F), onAccent = Color(0xFF07130D), accentSoft = Color(0x263FBF7F),
    bright = Color(0xFF4FD18B), amber = Color(0xFFF2B33D), amberSoft = Color(0xFF3A2C10), amberText = Color(0xFFF5C567),
    danger = Color(0xFFE0714F), hero = Color(0xFF1C2E25), heroInk = Color(0xFFFFFFFF), heroMuted = Color(0xFFB9C6BE),
    heroTrack = Color(0xFF2C3B33), neutralBar = Color(0xFF4A5A51),
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

@OptIn(ExperimentalTextApi::class)
private fun manrope(w: Int) = Font(R.font.manrope, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))

@OptIn(ExperimentalTextApi::class)
private fun grotesk(w: Int) = Font(R.font.space_grotesk, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))

val Manrope = FontFamily(manrope(400), manrope(500), manrope(600), manrope(700), manrope(800))
val Grotesk = FontFamily(grotesk(400), grotesk(500), grotesk(600), grotesk(700))

private val AppTypography = Typography().let { t ->
    Typography(
        displayLarge = t.displayLarge.copy(fontFamily = Grotesk),
        displayMedium = t.displayMedium.copy(fontFamily = Grotesk),
        displaySmall = t.displaySmall.copy(fontFamily = Grotesk),
        headlineLarge = t.headlineLarge.copy(fontFamily = Grotesk, fontWeight = FontWeight.Bold),
        headlineMedium = t.headlineMedium.copy(fontFamily = Grotesk, fontWeight = FontWeight.Bold),
        headlineSmall = t.headlineSmall.copy(fontFamily = Grotesk, fontWeight = FontWeight.Bold),
        titleLarge = t.titleLarge.copy(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold),
        titleMedium = t.titleMedium.copy(fontFamily = Manrope, fontWeight = FontWeight.Bold),
        titleSmall = t.titleSmall.copy(fontFamily = Manrope, fontWeight = FontWeight.Bold),
        bodyLarge = t.bodyLarge.copy(fontFamily = Manrope),
        bodyMedium = t.bodyMedium.copy(fontFamily = Manrope),
        bodySmall = t.bodySmall.copy(fontFamily = Manrope),
        labelLarge = t.labelLarge.copy(fontFamily = Manrope, fontWeight = FontWeight.Bold),
        labelMedium = t.labelMedium.copy(fontFamily = Manrope, fontWeight = FontWeight.Bold),
        labelSmall = t.labelSmall.copy(fontFamily = Manrope, fontWeight = FontWeight.Bold),
    )
}

/** Big tabular numbers (distances, dollars). */
fun numberStyle(size: Int, weight: FontWeight = FontWeight.Bold) =
    TextStyle(fontFamily = Grotesk, fontWeight = weight, fontSize = size.sp, letterSpacing = (-size * 0.03).sp)

@Composable
fun GigometerTheme(content: @Composable () -> Unit) {
    val p = if (isSystemInDarkTheme()) DarkPalette else LightPalette
    val scheme = if (p.dark) darkColorScheme(
        primary = p.accent, onPrimary = p.onAccent, background = p.ground, surface = p.card,
        onBackground = p.ink, onSurface = p.ink, surfaceVariant = p.card, onSurfaceVariant = p.muted,
        outline = p.line, secondary = p.amber, error = p.danger, surfaceContainerHigh = p.card,
        surfaceContainer = p.card, surfaceContainerLow = p.card, surfaceContainerHighest = p.card,
    ) else lightColorScheme(
        primary = p.accent, onPrimary = p.onAccent, background = p.ground, surface = p.card,
        onBackground = p.ink, onSurface = p.ink, surfaceVariant = p.ground, onSurfaceVariant = p.muted,
        outline = p.line, secondary = p.amber, error = p.danger, surfaceContainerHigh = p.card,
        surfaceContainer = p.card, surfaceContainerLow = p.card, surfaceContainerHighest = p.card,
    )
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
    }
}
