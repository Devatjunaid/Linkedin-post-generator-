package com.superdevlabs.coldplunge.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.superdevlabs.coldplunge.R

val Unbounded = FontFamily(
    Font(R.font.unbounded_semibold, FontWeight.SemiBold),
    Font(R.font.unbounded_extrabold, FontWeight.ExtraBold),
)

val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold),
)

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
)

/** App-specific text styles used across screens. */
object KType {
    val Hero = TextStyle(fontFamily = Unbounded, fontWeight = FontWeight.ExtraBold, fontSize = 64.sp, letterSpacing = (-2).sp, color = K.Text)
    val Title = TextStyle(fontFamily = Unbounded, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, letterSpacing = (-0.6).sp, color = K.Text)
    val Heading = TextStyle(fontFamily = Unbounded, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = K.Text)
    val Body = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, color = K.Text)
    val BodyStrong = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = K.Text)
    val Small = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = K.Muted)
    val Label = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, fontSize = 10.sp, letterSpacing = 2.sp, color = K.Muted)
    val Mono = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = K.Text)
    val MonoHuge = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 64.sp, letterSpacing = (-2).sp, color = K.Text)
}

val PlungeTypography = Typography(
    displayLarge = KType.Hero,
    headlineMedium = KType.Title,
    titleMedium = KType.Heading,
    bodyLarge = KType.Body.copy(fontSize = 16.sp),
    bodyMedium = KType.Body,
    bodySmall = KType.Small,
    labelLarge = KType.BodyStrong,
    labelMedium = KType.Small,
    labelSmall = KType.Label,
)
