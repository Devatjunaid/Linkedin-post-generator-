package com.devatjunaid.balconysolar.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object Palette {
    val Night = Color(0xFF0B1622)
    val NightDeep = Color(0xFF07101A)
    val Surface = Color(0xFF132338)
    val SurfaceHigh = Color(0xFF1B2F4A)
    val Panel = Color(0xFF101F33)
    val Line = Color(0xFF22385A)
    val LineStrong = Color(0xFF2C4A72)
    val Ink = Color(0xFFEEF3F8)
    val Muted = Color(0xFF9DB2C8)
    val Faint = Color(0xFF6F88A6)
    val Amber = Color(0xFFFFB547)
    val AmberSoft = Color(0xFFFFCD7A)
    val OnAmber = Color(0xFF1A1204)
    val Mint = Color(0xFF5BE3BE)
    val OnMint = Color(0xFF06231B)
    val Sky = Color(0xFF5BA0FF)
    val Cell = Color(0xFF1E4C8F)
    val Coral = Color(0xFFFF9B7A)
    val Dusk = Color(0xFF3A2A5C)
}

private val colors = darkColorScheme(
    primary = Palette.Amber,
    onPrimary = Palette.OnAmber,
    secondary = Palette.Mint,
    onSecondary = Palette.OnMint,
    tertiary = Palette.Sky,
    background = Palette.Night,
    onBackground = Palette.Ink,
    surface = Palette.Surface,
    onSurface = Palette.Ink,
    surfaceVariant = Palette.SurfaceHigh,
    onSurfaceVariant = Palette.Muted,
    surfaceContainer = Palette.Surface,
    surfaceContainerHigh = Palette.SurfaceHigh,
    surfaceContainerHighest = Palette.SurfaceHigh,
    outline = Palette.LineStrong,
    outlineVariant = Palette.Line,
    error = Palette.Coral,
)

object Type {
    val display = TextStyle(fontFamily = AppFonts.display, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 33.sp, letterSpacing = (-0.5).sp)
    val hero = TextStyle(fontFamily = AppFonts.display, fontWeight = FontWeight.Bold, fontSize = 56.sp, lineHeight = 58.sp, letterSpacing = (-1.5).sp)
    val title = TextStyle(fontFamily = AppFonts.display, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 24.sp)
    val stat = TextStyle(fontFamily = AppFonts.display, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 23.sp)
    val body = TextStyle(fontFamily = AppFonts.body, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp)
    val bodyMedium = TextStyle(fontFamily = AppFonts.body, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 21.sp)
    val label = TextStyle(fontFamily = AppFonts.body, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp)
    val labelStrong = TextStyle(fontFamily = AppFonts.body, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 17.sp)
    val mono = TextStyle(fontFamily = AppFonts.mono, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp)
    val button = TextStyle(fontFamily = AppFonts.body, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 20.sp)
}

private val typography = Typography(
    headlineMedium = Type.display,
    titleLarge = Type.title,
    titleMedium = Type.bodyMedium,
    bodyLarge = Type.body,
    bodyMedium = Type.body,
    labelLarge = Type.labelStrong,
    labelMedium = Type.label,
    labelSmall = Type.label,
)

@Composable
fun BalconySolarTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}
