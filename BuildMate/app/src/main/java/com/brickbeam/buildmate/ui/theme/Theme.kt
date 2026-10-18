package com.brickbeam.buildmate.ui.theme

import android.graphics.Typeface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.brickbeam.buildmate.calc.Category
import com.brickbeam.buildmate.data.ThemeId

@Immutable
data class Palette(
    val id: ThemeId,
    val name: String,
    val tagline: String,
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val ink: Color,
    val mute: Color,
    val accent: Color,
    val onAccent: Color,
    /** Accent used for text/icons on [bg]/[surface] (meets 4.5:1). */
    val accentText: Color,
    val glow: Color,
    val isDark: Boolean,
    val blueprintGrid: Boolean = false,
    val danger: Color,
    val success: Color,
)

object Palettes {
    val Night = Palette(
        ThemeId.NIGHT, "Concrete Night", "Dark · site at dusk",
        bg = Color(0xFF15171C), surface = Color(0xFF20232A), surface2 = Color(0xFF2A2E37),
        line = Color(0xFF343945), ink = Color(0xFFF3EFE8), mute = Color(0xFFA7ADB8),
        accent = Color(0xFFFF7A1A), onAccent = Color(0xFF1A0E05), accentText = Color(0xFFFF9A4D),
        glow = Color(0x2EFF7A1A), isDark = true, danger = Color(0xFFFF6B6B), success = Color(0xFF4CD08A),
    )
    val Blueprint = Palette(
        ThemeId.BLUEPRINT, "Blueprint", "Dark · engineer's sheet",
        bg = Color(0xFF0D2744), surface = Color(0xFF123358), surface2 = Color(0xFF18416D),
        line = Color(0xFF2A5A8C), ink = Color(0xFFEAF4FF), mute = Color(0xFFA9C3DE),
        accent = Color(0xFF62D6FF), onAccent = Color(0xFF04223A), accentText = Color(0xFF7FDEFF),
        glow = Color(0x2E62D6FF), isDark = true, blueprintGrid = true,
        danger = Color(0xFFFF8A80), success = Color(0xFF69F0AE),
    )
    val Terracotta = Palette(
        ThemeId.TERRACOTTA, "Terracotta", "Light · warm clay",
        bg = Color(0xFFF3ECE2), surface = Color(0xFFFFFFFF), surface2 = Color(0xFFEADFD1),
        line = Color(0xFFDCCDBA), ink = Color(0xFF2A1D15), mute = Color(0xFF6E5D50),
        accent = Color(0xFFB9472A), onAccent = Color(0xFFFFFFFF), accentText = Color(0xFFA63D22),
        glow = Color(0x24B9472A), isDark = false, danger = Color(0xFFB3261E), success = Color(0xFF1E7F4F),
    )
    val HiVis = Palette(
        ThemeId.HIVIS, "Hi-Vis", "Light · outdoor glare",
        bg = Color(0xFFF5F5EF), surface = Color(0xFFFFFFFF), surface2 = Color(0xFFECECE2),
        line = Color(0xFFD9D9CC), ink = Color(0xFF111111), mute = Color(0xFF55554D),
        accent = Color(0xFFFFD60A), onAccent = Color(0xFF111111), accentText = Color(0xFF111111),
        glow = Color(0x59FFD60A), isDark = false, danger = Color(0xFFB3261E), success = Color(0xFF1E7F4F),
    )

    val ALL = listOf(Night, Blueprint, Terracotta, HiVis)
    fun of(id: ThemeId) = ALL.first { it.id == id }
}

/** Material colours stay the same in every theme so users learn them. */
object MaterialColors {
    val Brick = Color(0xFFC4532F)
    val BrickDark = Color(0xFFA9452A)
    val BrickLight = Color(0xFFCF6440)
    val Mortar = Color(0xFFB8B0A3)
    val Cement = Color(0xFFA3AAB3)
    val CementDark = Color(0xFF7D858F)
    val Tile = Color(0xFF2BB3A3)
    val TileLight = Color(0xFFD8F1EE)
    val Paint = Color(0xFF8B5CF6)
    val PaintPink = Color(0xFFEC4899)
    val Steel = Color(0xFF6B8196)
    val SteelDark = Color(0xFF5A6F83)
    val SteelLight = Color(0xFFC9D3DC)
    val Rust = Color(0xFFB7652B)
    val Wood = Color(0xFF8A5A2B)

    fun of(category: Category) = when (category) {
        Category.BRICK -> Brick
        Category.CEMENT -> Cement
        Category.STEEL -> Steel
        Category.TILE -> Tile
        Category.PAINT -> Paint
        Category.OTHER -> Color(0xFF9E9E9E)
    }
}

/** Condensed display face from the platform (no font download needed) + monospace figures. */
object BmFonts {
    val Condensed: FontFamily = FontFamily(Typeface.create("sans-serif-condensed", Typeface.BOLD))
    val Body: FontFamily = FontFamily.SansSerif
    val Mono: FontFamily = FontFamily.Monospace
}

object BmType {
    val display = TextStyle(fontFamily = BmFonts.Condensed, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 30.sp)
    val title = TextStyle(fontFamily = BmFonts.Condensed, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 24.sp)
    val section = TextStyle(fontFamily = BmFonts.Body, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    val body = TextStyle(fontFamily = BmFonts.Body, fontSize = 14.sp, lineHeight = 19.sp)
    val label = TextStyle(fontFamily = BmFonts.Body, fontSize = 12.sp, lineHeight = 15.sp)
    val tiny = TextStyle(fontFamily = BmFonts.Body, fontSize = 11.sp, lineHeight = 14.sp)
    val figure = TextStyle(fontFamily = BmFonts.Mono, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    val bigFigure = TextStyle(fontFamily = BmFonts.Mono, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 44.sp)
}

val LocalPalette = staticCompositionLocalOf { Palettes.Night }
val LocalMotion = staticCompositionLocalOf { true }

@Composable
fun BuildMateTheme(themeId: ThemeId, motion: Boolean, content: @Composable () -> Unit) {
    val p = Palettes.of(themeId)
    val scheme = if (p.isDark) {
        darkColorScheme(
            primary = p.accent, onPrimary = p.onAccent, secondary = p.accentText,
            background = p.bg, onBackground = p.ink, surface = p.surface, onSurface = p.ink,
            surfaceVariant = p.surface2, onSurfaceVariant = p.mute, outline = p.line,
            surfaceContainer = p.surface, surfaceContainerHigh = p.surface2,
            surfaceContainerHighest = p.surface2, error = p.danger,
        )
    } else {
        lightColorScheme(
            primary = if (p.id == ThemeId.HIVIS) p.ink else p.accent,
            onPrimary = if (p.id == ThemeId.HIVIS) p.accent else p.onAccent,
            secondary = p.accentText,
            background = p.bg, onBackground = p.ink, surface = p.surface, onSurface = p.ink,
            surfaceVariant = p.surface2, onSurfaceVariant = p.mute, outline = p.line,
            surfaceContainer = p.surface, surfaceContainerHigh = p.surface2,
            surfaceContainerHighest = p.surface2, error = p.danger,
        )
    }
    val typography = Typography(
        bodyLarge = BmType.body.copy(fontSize = 16.sp),
        bodyMedium = BmType.body,
        bodySmall = BmType.label,
        titleLarge = BmType.title,
        titleMedium = BmType.section.copy(fontSize = 16.sp),
        labelLarge = BmType.section,
    )
    CompositionLocalProvider(LocalPalette provides p, LocalMotion provides motion) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}
