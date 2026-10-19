package com.sunarc.inverterlogger.ui.theme

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
import com.sunarc.inverterlogger.R

/** The "night instrument panel" palette: graphite ground, sun amber and sky blue. */
object Palette {
    val Night = Color(0xFF0C0F14)
    val Panel = Color(0xFF121821)
    val Surface = Color(0xFF151A22)
    val SurfaceHigh = Color(0xFF1D2430)
    val Line = Color(0xFF2A3240)
    val LineSoft = Color(0xFF222A36)
    val Ink = Color(0xFFF2F4F7)
    val InkSoft = Color(0xFFC9D0DA)
    val Muted = Color(0xFF9AA4B2)
    val Faint = Color(0xFF6E7887)
    val Sun = Color(0xFFFFB020)
    val SunSoft = Color(0xFFFFC65C)
    val SunPale = Color(0xFFFFE3A3)
    val SunDeep = Color(0xFF3A3220)
    val OnSun = Color(0xFF1A1204)
    val Sky = Color(0xFF5AB8FF)
    val SkyDeep = Color(0xFF2D4A66)
    val SkyNight = Color(0xFF101A24)
    val PanelBlue = Color(0xFF1E3550)
    val Lcd = Color(0xFF9CB08A)
    val LcdInk = Color(0xFF1E2A18)
}

@OptIn(ExperimentalTextApi::class)
private fun variable(res: Int, weight: Int) =
    Font(res, weight = FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

val DisplayFont = FontFamily(
    variable(R.font.space_grotesk, 500),
    variable(R.font.space_grotesk, 600),
    variable(R.font.space_grotesk, 700)
)

val BodyFont = FontFamily(
    variable(R.font.ibm_plex_sans, 400),
    variable(R.font.ibm_plex_sans, 500),
    variable(R.font.ibm_plex_sans, 600)
)

val MonoFont = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_mono_semibold, FontWeight.SemiBold)
)

private val SunarcTypography = Typography(
    displayLarge = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = 46.sp, letterSpacing = (-1.5).sp),
    headlineLarge = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 34.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold, fontSize = 26.sp),
    headlineSmall = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleLarge = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleMedium = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Medium, fontSize = 15.sp),
    titleSmall = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Medium, fontSize = 13.sp),
    bodyLarge = TextStyle(fontFamily = BodyFont, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = BodyFont, fontSize = 13.sp, lineHeight = 19.sp),
    bodySmall = TextStyle(fontFamily = BodyFont, fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = MonoFont, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.5.sp)
)

private val SunarcColors = darkColorScheme(
    primary = Palette.Sun,
    onPrimary = Palette.OnSun,
    secondary = Palette.Sky,
    onSecondary = Palette.Night,
    background = Palette.Night,
    onBackground = Palette.Ink,
    surface = Palette.Surface,
    onSurface = Palette.Ink,
    surfaceVariant = Palette.SurfaceHigh,
    onSurfaceVariant = Palette.InkSoft,
    surfaceContainer = Palette.Surface,
    surfaceContainerHigh = Palette.SurfaceHigh,
    outline = Palette.Line,
    outlineVariant = Palette.LineSoft,
    error = Color(0xFFFF8A7A),
    onError = Palette.Night
)

/** Mono numbers for readings. */
val Readout = TextStyle(fontFamily = MonoFont, fontWeight = FontWeight.SemiBold, fontSize = 22.sp)

@Composable
fun SunarcTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SunarcColors, typography = SunarcTypography, content = content)
}
