package com.devatjunaid.dripdropdraw.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.devatjunaid.dripdropdraw.R

object Palette {
    val Ink = Color(0xFF1F2430)
    val Paper = Color(0xFFFBFBF6)
    val Grid = Color(0xFFE3E7F2)
    val Muted = Color(0xFF5A6172)
    val Water = Color(0xFF3FA2EC)
    val WaterDark = Color(0xFF2280CC)
    val WaterLight = Color(0xFFBFE3FF)
    val Orange = Color(0xFFFF7A45)
    val Star = Color(0xFFFFC23C)
    val StarOff = Color(0xFFD9DDE5)
    val Green = Color(0xFFB6E35A)
    val InkGreen = Color(0xFF9BDB4D)
    val Wood = Color(0xFFE2A65A)
    val Stone = Color(0xFF9AA7B8)
    val StoneDark = Color(0xFF7C8899)
    val Glass = Color(0xFF9AA7B8)
    val Ball = Color(0xFFFF6B5B)
    val Pink = Color(0xFFFF8FA3)
    val Fire = Color(0xFFFF7A45)
    val FireCore = Color(0xFFFFC23C)
    val Locked = Color(0xFFE4E7EE)
    val LockedLine = Color(0xFFAEB6C4)
    val White = Color.White
    val Scrim = Color(0x991F2430)
}

/** Each world has its own paper tint and wall colours. */
class WorldTheme(
    val name: String,
    val paper: Color,
    val grid: Color,
    val wood: Color,
    val stone: Color,
    val dark: Boolean = false,
)

val Worlds = listOf(
    WorldTheme("Kitchen", Color(0xFFFBFBF6), Color(0xFFE3E7F2), Color(0xFFE2A65A), Color(0xFF9AA7B8)),
    WorldTheme("Garden", Color(0xFFF3FAEC), Color(0xFFD9EBCB), Color(0xFFC98B4A), Color(0xFF8FA58A)),
    WorldTheme("Bathtub", Color(0xFFEEF7FC), Color(0xFFD3E6F2), Color(0xFFF2B8C6), Color(0xFF93A9C2)),
    WorldTheme("Lab", Color(0xFFF5F2FB), Color(0xFFE2DCF2), Color(0xFFB9A4E8), Color(0xFF9C99B8)),
    WorldTheme("Space", Color(0xFF232A4A), Color(0xFF323B63), Color(0xFFFFB86B), Color(0xFF8C95C2), dark = true),
)

val Fredoka = FontFamily(
    Font(R.font.fredoka, FontWeight.Normal),
    Font(R.font.fredoka, FontWeight.Medium),
    Font(R.font.fredoka, FontWeight.SemiBold),
    Font(R.font.fredoka, FontWeight.Bold),
)

val HandFont = FontFamily(Font(R.font.gochi_hand, FontWeight.Normal))
