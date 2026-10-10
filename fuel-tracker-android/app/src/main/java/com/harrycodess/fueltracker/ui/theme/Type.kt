package com.harrycodess.fueltracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight

private val base = Typography()

/** Tabular figures keep numbers from jumping around as values change. */
private const val TABULAR = "tnum"

val AppTypography = Typography(
    displayMedium = base.displayMedium.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = TABULAR),
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = TABULAR),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = TABULAR),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Medium),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = TABULAR),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)
