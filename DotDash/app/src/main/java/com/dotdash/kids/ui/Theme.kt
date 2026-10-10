package com.dotdash.kids.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/** "#RRGGBB" or "#AARRGGBB" → Color. */
fun hex(s: String): Color {
    val v = s.removePrefix("#").toLong(16)
    return if (s.length == 7) Color(0xFF000000 or v) else Color(v)
}

object C {
    val Night = Color(0xFF1E1A4D)
    val Night2 = Color(0xFF2B2570)
    val Night3 = Color(0xFF4A43A0)
    val Deep = Color(0xFF17143D)
    val Ink = Color(0xFF241C5C)
    val InkDark = Color(0xFF120F33)
    val Cream = Color(0xFFFFF8E7)
    val Sun = Color(0xFFFFC93C)
    val SunDark = Color(0xFFC98F00)
    val SunLight = Color(0xFFFFE58A)
    val Coral = Color(0xFFFF5D73)
    val CoralDark = Color(0xFFB83246)
    val Mint = Color(0xFF3EE0B0)
    val MintDark = Color(0xFF1E9E78)
    val Sky = Color(0xFF5CC8FF)
    val SkyDark = Color(0xFF2B8FC7)
    val Grape = Color(0xFFC3A6FF)
    val Muted = Color(0xFFB9B3F0)
    val Lilac = Color(0xFFDCD6FF)
    val Paper = Color(0xFFFFF4DC)
    val PaperEdge = Color(0xFFC9B48A)
    val White = Color.White
}

fun display(size: TextUnit, color: Color = C.Cream, weight: FontWeight = FontWeight.Bold) =
    TextStyle(fontFamily = Fredoka, fontWeight = weight, fontSize = size, color = color)

fun body(size: TextUnit = 16.sp, color: Color = C.Cream, weight: FontWeight = FontWeight.ExtraBold) =
    TextStyle(fontFamily = Nunito, fontWeight = weight, fontSize = size, color = color)
