package com.harrycodess.fueltracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val base = Typography()

/** Tabular figures keep numbers from jumping around as values change. */
private const val TABULAR = "tnum"

val AppTypography = Typography(
    displayLarge = base.displayLarge.copy(fontWeight = FontWeight.Black, fontFeatureSettings = TABULAR),
    displayMedium = base.displayMedium.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = TABULAR),
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = TABULAR),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = TABULAR),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = TABULAR),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    labelSmall = base.labelSmall.copy(fontWeight = FontWeight.SemiBold),
)

/** Digital-dashboard style for big numeric readouts (trip cost, gauges). */
val ReadoutLarge = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Bold,
    fontSize = 40.sp,
    lineHeight = 44.sp,
    letterSpacing = (-1).sp,
)

val ReadoutMedium = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Bold,
    fontSize = 18.sp,
    lineHeight = 22.sp,
)

/** Small uppercase instrument label ("TRIP COST", "PER KM"). */
val InstrumentLabel = TextStyle(
    fontWeight = FontWeight.SemiBold,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 1.4.sp,
)
