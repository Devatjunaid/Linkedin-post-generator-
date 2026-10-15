package com.glowdash.warningidentifier.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.glowdash.warningidentifier.R

/** Night-dashboard palette. Accents are slightly softened so long sessions stay easy on the eyes. */
object WiColors {
    val Bg = Color(0xFF0A0C16)
    val Surface = Color(0xFF12162A)
    val SurfaceHigh = Color(0xFF1A1F38)
    val Stroke = Color(0xFF252B47)
    val StrokeStrong = Color(0xFF353D63)
    val TextPrimary = Color(0xFFEEF1FA)
    val TextSecondary = Color(0xFFA3AAC6)
    val TextMuted = Color(0xFF7A83A6)

    val Cyan = Color(0xFF46CBE0)
    val Pink = Color(0xFFEA5A90)
    val Amber = Color(0xFFF0B13E)
    val Red = Color(0xFFEC5B70)
    val Lime = Color(0xFF9FD45A)
    val Blue = Color(0xFF5B8DEF)
    val Violet = Color(0xFF6D4BC9)

    val OnCyan = Color(0xFF03141A)
    val OnAmber = Color(0xFF1A1200)
}

val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_bold, FontWeight.Bold),
)

val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold),
)

object WiType {
    val Display = TextStyle(
        fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 28.sp,
        letterSpacing = (-0.5).sp, color = WiColors.TextPrimary,
    )
    val Title = TextStyle(
        fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 22.sp,
        letterSpacing = (-0.3).sp, color = WiColors.TextPrimary,
    )
    val Section = TextStyle(
        fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 17.sp,
        color = WiColors.TextPrimary,
    )
    val Button = TextStyle(
        fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 17.sp,
    )
    val CardTitle = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp,
        color = WiColors.TextPrimary,
    )
    val Body = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 15.sp,
        lineHeight = 22.sp, color = WiColors.TextSecondary,
    )
    val BodySmall = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 13.sp,
        lineHeight = 19.sp, color = WiColors.TextSecondary,
    )
    val Caption = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
        color = WiColors.TextSecondary,
    )
    val Label = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp,
        letterSpacing = 0.6.sp,
    )
}

@Composable
fun WarningTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = WiColors.Cyan,
            onPrimary = WiColors.OnCyan,
            secondary = WiColors.Pink,
            background = WiColors.Bg,
            onBackground = WiColors.TextPrimary,
            surface = WiColors.Surface,
            onSurface = WiColors.TextPrimary,
            surfaceVariant = WiColors.SurfaceHigh,
            outline = WiColors.Stroke,
        ),
        content = content,
    )
}
