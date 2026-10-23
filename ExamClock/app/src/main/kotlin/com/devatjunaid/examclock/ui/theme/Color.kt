package com.devatjunaid.examclock.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

object Brand {
    val Ink = Color(0xFF15171C)
    val InkRaised = Color(0xFF23262E)
    val InkLine = Color(0xFF2C2F38)
    val Paper = Color(0xFFF5F4EF)
    val Blue = Color(0xFF2346D8)
    val BlueLight = Color(0xFF7E9BFF)
    val Orange = Color(0xFFC2410C)
    val OrangeLight = Color(0xFFF2A86B)
    val Green = Color(0xFF1F7A4D)
    val OnInkMuted = Color(0xFFB9BDC9)
}

/**
 * Subject colours: the validated 8-slot categorical palette (colour-blind safe on adjacent pairs),
 * stepped separately for light and dark surfaces. Text never uses these; they mark identity only.
 */
private val subjectLight = listOf(
    Color(0xFF2A78D6), Color(0xFFEB6834), Color(0xFF1BAF7A), Color(0xFFEDA100),
    Color(0xFFE87BA4), Color(0xFF008300), Color(0xFF6250D6), Color(0xFFE34948),
)
private val subjectDark = listOf(
    Color(0xFF3987E5), Color(0xFFD95926), Color(0xFF199E70), Color(0xFFC98500),
    Color(0xFFD55181), Color(0xFF008300), Color(0xFF9085E9), Color(0xFFE66767),
)

/** Readable label colour on top of a subject colour fill (dark ink on yellow, white elsewhere). */
fun onSubjectColor(index: Int): Color = if (((index % 8) + 8) % 8 == 3) Color(0xFF0B0B0B) else Color.White

@Composable
@ReadOnlyComposable
fun subjectColor(index: Int): Color {
    val dark = MaterialTheme.colorScheme.background.approxLuminance() < 0.5f
    val list = if (dark) subjectDark else subjectLight
    return list[((index % list.size) + list.size) % list.size]
}

val subjectColorCount: Int get() = subjectLight.size

private fun Color.approxLuminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue
