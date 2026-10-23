package com.driveready.testprep.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

/** DriveReady palette: asphalt navy, road-marking amber, signal blue. */
object Brand {
    val Navy = Color(0xFF0F1B2D)
    val NavyLight = Color(0xFF1B3358)
    val Ink = Color(0xFF15171C)
    val Surface = Color(0xFFF6F5F1)
    val Card = Color(0xFFFFFFFF)
    val Line = Color(0xFFE3E1DA)
    val Muted = Color(0xFF5E6168)
    val Amber = Color(0xFFF5B700)
    val AmberSoft = Color(0xFFFFF4CC)
    val Blue = Color(0xFF1E4FD8)
    val BlueSoft = Color(0xFFE3EAFC)
    val Good = Color(0xFF166534)
    val GoodSoft = Color(0xFFE7F4EC)
    val Bad = Color(0xFFB93C0B)
    val BadSoft = Color(0xFFFDEEE6)
    val SignRed = Color(0xFFD7261E)
    val SignBlue = Color(0xFF1E4FD8)
    val SignYellow = Color(0xFFFFCC00)
    val SignGreen = Color(0xFF0B6E3A)
}

private val colors = lightColorScheme(
    primary = Brand.Navy,
    onPrimary = Color.White,
    secondary = Brand.Amber,
    onSecondary = Brand.Ink,
    tertiary = Brand.Blue,
    background = Brand.Surface,
    onBackground = Brand.Ink,
    surface = Brand.Card,
    onSurface = Brand.Ink,
    surfaceVariant = Color(0xFFEDEBE4),
    onSurfaceVariant = Brand.Muted,
    outline = Brand.Line,
    error = Brand.Bad
)

private val display = FontFamily.SansSerif

private val typography = Typography(
    displaySmall = TextStyle(fontFamily = display, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-0.6).sp),
    headlineMedium = TextStyle(fontFamily = display, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-0.4).sp),
    headlineSmall = TextStyle(fontFamily = display, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.4.sp)
)

private val shapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun DriveReadyTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, shapes = shapes, content = content)
}
