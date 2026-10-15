package com.glowdash.warningidentifier.data

import androidx.compose.ui.graphics.Color
import com.glowdash.warningidentifier.ui.theme.WiColors

/** How urgently the driver should react. */
enum class Severity(val label: String, val color: Color, val onColor: Color, val meter: Float) {
    STOP("STOP", WiColors.Red, Color.White, 1f),
    CAUTION("CAUTION", WiColors.Amber, WiColors.OnAmber, 0.62f),
    INFO("INFO", WiColors.Cyan, WiColors.OnCyan, 0.25f),
}

/** The colour the symbol actually glows on a real dashboard. */
enum class LightColor(val label: String, val color: Color) {
    RED("Red", WiColors.Red),
    AMBER("Amber", WiColors.Amber),
    GREEN("Green", WiColors.Lime),
    BLUE("Blue", WiColors.Blue),
}

/**
 * One dashboard symbol.
 *
 * [iconPath] is SVG path data on a 24×24 grid, drawn as a stroke by `LightIcon`.
 * [iconLabel] is optional text drawn in the middle of the symbol (e.g. "ABS").
 */
data class WarningLight(
    val id: String,
    val name: String,
    val aka: String,
    val color: LightColor,
    val severity: Severity,
    val iconPath: String,
    val summary: String,
    val meaning: String,
    val steps: List<String>,
    val canDrive: String,
    val flashingNote: String? = null,
    val iconLabel: String? = null,
    val iconLabelSize: Float = 6f,
    val keywords: String = "",
)
