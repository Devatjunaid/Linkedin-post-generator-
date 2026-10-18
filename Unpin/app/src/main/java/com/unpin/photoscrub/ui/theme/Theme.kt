package com.unpin.photoscrub.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.unpin.photoscrub.R

val Paper = Color(0xFFF3F0E9)
val Ink = Color(0xFF15161A)
val InkSoft = Color(0xFF24252B)
val InkLine = Color(0xFF3A3B42)
val Muted = Color(0xFF5B5A55)
val MutedOnInk = Color(0xFFC9C4B8)
val Line = Color(0xFFE2DDD1)
val LineSoft = Color(0xFFEEEAE0)
val Card = Color(0xFFFFFFFF)
val Signal = Color(0xFFFF5B1F)
val SignalSoft = Color(0xFFFF8A5C)
val SignalTint = Color(0xFFFFE3D6)
val SignalDeep = Color(0xFF9A3009)
val Clean = Color(0xFF1F6F5C)
val CleanTint = Color(0xFFDCEDE7)
val CleanDeep = Color(0xFF123F34)
val Sky = Color(0xFF9CB7C9)
val Sun = Color(0xFFF6D27A)
val Hill = Color(0xFF3E5A4C)
val HillDark = Color(0xFF2B4036)

@OptIn(ExperimentalTextApi::class)
private fun bricolage(weight: Int) = Font(
    R.font.bricolage_grotesque,
    FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight), FontVariation.Setting("opsz", 48f)),
)

@OptIn(ExperimentalTextApi::class)
private fun instrument(weight: Int) = Font(
    R.font.instrument_sans,
    FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

@OptIn(ExperimentalTextApi::class)
private fun mono(weight: Int) = Font(
    R.font.jetbrains_mono,
    FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Display = FontFamily(bricolage(500), bricolage(700), bricolage(800))
val Body = FontFamily(instrument(400), instrument(500), instrument(600), instrument(700))
val Mono = FontFamily(mono(400), mono(500))

object UnpinText {
    val hero = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 35.sp, letterSpacing = (-1).sp)
    val title = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 26.sp, letterSpacing = (-0.4).sp)
    val section = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 22.sp)
    val number = TextStyle(fontFamily = Display, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 30.sp, letterSpacing = (-0.8).sp)
    val body = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp)
    val bodyStrong = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp)
    val small = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = 12.5.sp, lineHeight = 17.sp)
    val button = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    val label = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 1.5.sp)
    val mono = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Normal, fontSize = 12.sp)
}

@Composable
fun UnpinTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Ink, onPrimary = Paper, secondary = Signal, onSecondary = Ink,
            background = Paper, onBackground = Ink, surface = Card, onSurface = Ink,
            surfaceVariant = Paper, onSurfaceVariant = Muted, outline = Line, error = SignalDeep,
        ),
        typography = Typography(bodyMedium = UnpinText.body, bodyLarge = UnpinText.body, labelLarge = UnpinText.button),
        content = content,
    )
}
