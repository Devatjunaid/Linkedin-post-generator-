package com.devatjunaid.balconysolar.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.devatjunaid.balconysolar.R

/** Bundled typefaces (SIL Open Font License, see assets/licenses). */
object AppFonts {
    val display = FontFamily(
        Font(R.font.bricolage_semibold, FontWeight.SemiBold),
        Font(R.font.bricolage_bold, FontWeight.Bold),
    )
    val body = FontFamily(
        Font(R.font.plex_sans_regular, FontWeight.Normal),
        Font(R.font.plex_sans_medium, FontWeight.Medium),
        Font(R.font.plex_sans_semibold, FontWeight.SemiBold),
    )
    val mono = FontFamily(Font(R.font.plex_mono_medium, FontWeight.Medium))
}
