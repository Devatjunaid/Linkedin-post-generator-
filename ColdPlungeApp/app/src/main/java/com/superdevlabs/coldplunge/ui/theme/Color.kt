package com.superdevlabs.coldplunge.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object K {
    val Ink = Color(0xFF0B0F14)
    val InkDeep = Color(0xFF070B10)
    val Surface = Color(0xFF131A22)
    val Surface2 = Color(0xFF1A232E)
    val Glass = Color(0x0FFFFFFF)
    val Line = Color(0x17FFFFFF)
    val Text = Color(0xFFEEF3F7)
    val Muted = Color(0xFF8A99A8)
    val Faint = Color(0xFF5F6E7D)

    val Ice = Color(0xFF5CE1FF)
    val IceLight = Color(0xFFB8F3FF)
    val IceDeep = Color(0xFF2B7BFF)
    val Ember = Color(0xFFFF7A3D)
    val EmberDeep = Color(0xFFFF3D5A)
    val Gold = Color(0xFFFFC56B)
    val Violet = Color(0xFF8B5CFF)
    val Rest = Color(0xFF5A6878)

    val thermalSweep = listOf(Ice, IceDeep, Violet, EmberDeep, Ember, Gold, Ice)
    val iceBrush = Brush.linearGradient(listOf(IceLight, Ice, IceDeep))
    val heatBrush = Brush.linearGradient(listOf(Gold, Ember, EmberDeep))
    val restBrush = Brush.linearGradient(listOf(Color(0x33FFFFFF), Color(0x1AFFFFFF)))
}
