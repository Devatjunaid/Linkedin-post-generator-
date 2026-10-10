package com.harrycodess.drawingkids.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object Kid {
    val Cream = Color(0xFFFFF7EC)
    val Ink = Color(0xFF2B2340)
    val Muted = Color(0xFF8A7FA3)
    val Line = Color(0xFFEDE3D3)
    val Outside = Color(0xFFF3EADB)

    val Red = Color(0xFFFF4B4B)
    val Orange = Color(0xFFFF9F43)
    val Yellow = Color(0xFFFFD23F)
    val Gold = Color(0xFFFFC21F)
    val Lime = Color(0xFFB6E33B)
    val Green = Color(0xFF3DD68C)
    val Teal = Color(0xFF2EC4B6)
    val Blue = Color(0xFF4DA3FF)
    val Royal = Color(0xFF3A5BFF)
    val Purple = Color(0xFF9B6BFF)
    val Pink = Color(0xFFFF6FA5)
    val Blush = Color(0xFFFFB3C9)
    val Brown = Color(0xFFA0522D)
    val Sand = Color(0xFFF6D7A7)

    val CardYellow = Color(0xFFFFE07A)
    val CardMint = Color(0xFF9FE3C4)
    val CardLilac = Color(0xFFC9B6FF)
    val PinkSoft = Color(0xFFFFE9F2)
    val OrangeSoft = Color(0xFFFFF4D6)

    /** Crayon box shown on the drawing and coloring screens. */
    val Palette = listOf(
        Red, Orange, Yellow, Lime, Green, Teal, Blue, Royal, Purple, Pink,
        Blush, Color(0xFF7FDBFF), Color(0xFFFFE66D), Color(0xFFC9B6FF), Brown, Sand,
        Color(0xFF6B4226), Color(0xFF8C8C8C), Ink, Color.White,
    )

    val Rainbow = listOf(Red, Orange, Gold, Green, Blue, Purple)
}

val KidFont = FontFamily.SansSerif

fun kidText(size: Int, weight: FontWeight = FontWeight.ExtraBold, color: Color = Kid.Ink) =
    TextStyle(fontFamily = KidFont, fontWeight = weight, fontSize = size.sp, color = color)

@Composable
fun TwilioKidsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Kid.Pink,
            secondary = Kid.Orange,
            background = Kid.Cream,
            surface = Color.White,
            onBackground = Kid.Ink,
            onSurface = Kid.Ink,
        ),
        content = content,
    )
}
