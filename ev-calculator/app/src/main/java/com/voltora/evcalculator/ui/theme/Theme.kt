package com.voltora.evcalculator.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.voltora.evcalculator.R

object Ev {
    val Ink = Color(0xFF0B0B0C)
    val Ink2 = Color(0xFF1F1F22)
    val Ink3 = Color(0xFF2A2A2E)
    val Line = Color(0xFF3A3A3E)
    val Paper = Color(0xFFFFFFFF)
    val Mist = Color(0xFFF2F2F0)
    val Stroke = Color(0xFFE2E2DF)
    val Volt = Color(0xFFD4FF3A)
    val Muted = Color(0xFF5B5B60)
    val MutedOnDark = Color(0xFFA1A1A6)
    val Warn = Color(0xFFFF6B2C)
}

val Display = FontFamily(
    Font(R.font.barlow_condensed_bold, FontWeight.Bold),
    Font(R.font.barlow_condensed_extrabold, FontWeight.ExtraBold),
)

val Body = FontFamily(
    Font(R.font.barlow_regular, FontWeight.Normal),
    Font(R.font.barlow_medium, FontWeight.Medium),
    Font(R.font.barlow_bold, FontWeight.Bold),
)

private val typography = Typography(
    bodyLarge = TextStyle(fontFamily = Body, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = Body, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = Body, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 15.sp),
    labelMedium = TextStyle(fontFamily = Body, fontWeight = FontWeight.Medium, fontSize = 13.sp),
    labelSmall = TextStyle(fontFamily = Body, fontWeight = FontWeight.Medium, fontSize = 11.sp),
    titleLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 28.sp),
    titleMedium = TextStyle(fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 17.sp),
    titleSmall = TextStyle(fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 15.sp),
    headlineLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.ExtraBold, fontSize = 44.sp),
    headlineMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 34.sp),
    displayLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.ExtraBold, fontSize = 96.sp),
)

@Composable
fun EvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Ev.Ink,
            onPrimary = Ev.Paper,
            secondary = Ev.Volt,
            onSecondary = Ev.Ink,
            background = Ev.Paper,
            onBackground = Ev.Ink,
            surface = Ev.Paper,
            onSurface = Ev.Ink,
            surfaceVariant = Ev.Mist,
            onSurfaceVariant = Ev.Muted,
            outline = Ev.Stroke,
        ),
        typography = typography,
        content = content,
    )
}
