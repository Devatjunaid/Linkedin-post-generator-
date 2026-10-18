package app.layerline.toolkit3d.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import app.layerline.toolkit3d.R

/** Workshop palette: graphite print bed, lime "go", hot-end orange for heat and warnings. */
object LL {
    val Graphite = Color(0xFF0F1113)
    val Nav = Color(0xFF0A0C0D)
    val Panel = Color(0xFF181B1E)
    val Panel2 = Color(0xFF1F2327)
    val Raised = Color(0xFF2A2F33)
    val Line = Color(0xFF2C3136)
    val LineStrong = Color(0xFF353B41)
    val Dot = Color(0xFF23282C)
    val Ink = Color(0xFFECEEE8)
    val Soft = Color(0xFFC9CEC6)
    val Muted = Color(0xFF9AA19A)
    val Lime = Color(0xFFC8F03C)
    val LimeDeep = Color(0xFF232A14)
    val Hot = Color(0xFFFF8A52)
    val HotDeep = Color(0xFF3A2116)
    val Paper = Color(0xFFECEEE8)
    val PaperLine = Color(0xFFB9BEB6)
    val PaperMuted = Color(0xFF4A5056)

    /** Distinct-lightness colours for the cost breakdown bar (readable on the paper ticket). */
    val Breakdown = listOf(
        Color(0xFF8DB51A), Color(0xFFE8692E), Color(0xFF3C4348),
        Color(0xFF8E969C), Color(0xFF5B7BF0), Color(0xFFC9CEC6),
    )
}

@OptIn(ExperimentalTextApi::class)
object LLFonts {
    val Display = FontFamily(
        Font(R.font.chakra_petch_semibold, FontWeight.SemiBold),
        Font(R.font.chakra_petch_bold, FontWeight.Bold),
    )
    val Mono = FontFamily(
        Font(R.font.plex_mono_regular, FontWeight.Normal),
        Font(R.font.plex_mono_medium, FontWeight.Medium),
        Font(R.font.plex_mono_semibold, FontWeight.SemiBold),
    )
    val Body = FontFamily(
        Font(R.font.familjen_grotesk, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
        Font(R.font.familjen_grotesk, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
        Font(R.font.familjen_grotesk, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    )
}

object LLType {
    fun display(size: TextUnit, weight: FontWeight = FontWeight.Bold, color: Color = LL.Ink) =
        TextStyle(fontFamily = LLFonts.Display, fontWeight = weight, fontSize = size, color = color, lineHeight = size * 1.05f)

    fun mono(size: TextUnit = 12.sp, weight: FontWeight = FontWeight.Normal, color: Color = LL.Ink) =
        TextStyle(fontFamily = LLFonts.Mono, fontWeight = weight, fontSize = size, color = color, letterSpacing = 0.02.sp)

    fun body(size: TextUnit = 14.sp, weight: FontWeight = FontWeight.Normal, color: Color = LL.Ink) =
        TextStyle(fontFamily = LLFonts.Body, fontWeight = weight, fontSize = size, color = color, lineHeight = size * 1.3f)
}

@Composable
fun LayerlineTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = LL.Lime,
        onPrimary = LL.Graphite,
        secondary = LL.Hot,
        onSecondary = LL.Graphite,
        background = LL.Graphite,
        onBackground = LL.Ink,
        surface = LL.Panel,
        onSurface = LL.Ink,
        surfaceVariant = LL.Panel2,
        onSurfaceVariant = LL.Soft,
        outline = LL.LineStrong,
        error = LL.Hot,
    )
    MaterialTheme(colorScheme = scheme, typography = MaterialTheme.typography.copy(
        bodyLarge = LLType.body(16.sp),
        bodyMedium = LLType.body(14.sp),
        labelLarge = LLType.mono(13.sp, FontWeight.SemiBold),
    ), content = content)
}
