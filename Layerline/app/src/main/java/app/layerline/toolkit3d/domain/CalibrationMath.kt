package app.layerline.toolkit3d.domain

import java.util.Locale

/** Pure calibration formulas. Each returns null when the inputs cannot produce a result. */
object CalibrationMath {

    /** E-steps: mark [markMm] on the filament, extrude [requestedMm], measure [leftMm] to the mark. */
    fun eSteps(current: Double, markMm: Double, leftMm: Double, requestedMm: Double): Double? {
        val actual = markMm - leftMm
        if (actual <= 0 || requestedMm <= 0 || current <= 0) return null
        return current * requestedMm / actual
    }

    /** Klipper rotation_distance scales the opposite way to e-steps. */
    fun rotationDistance(current: Double, markMm: Double, leftMm: Double, requestedMm: Double): Double? {
        val actual = markMm - leftMm
        if (actual <= 0 || requestedMm <= 0 || current <= 0) return null
        return current * actual / requestedMm
    }

    fun flow(currentPercent: Double, expectedWallMm: Double, measuredWallMm: Double): Double? {
        if (measuredWallMm <= 0 || expectedWallMm <= 0) return null
        return currentPercent * expectedWallMm / measuredWallMm
    }

    fun shrinkageScale(designMm: Double, measuredMm: Double): Double? {
        if (measuredMm <= 0 || designMm <= 0) return null
        return designMm / measuredMm * 100.0
    }

    fun pressureAdvance(start: Double, stepPerMm: Double, bestHeightMm: Double): Double =
        start + stepPerMm * bestHeightMm

    fun retraction(firstBandMm: Double, stepPerBandMm: Double, cleanestBand: Double): Double? {
        if (cleanestBand < 1) return null
        return firstBandMm + (cleanestBand - 1) * stepPerBandMm
    }

    fun temperatureTower(bottomTemp: Double, dropPerBlock: Double, bestBlock: Double): Double? {
        if (bestBlock < 1) return null
        return bottomTemp - (bestBlock - 1) * dropPerBlock
    }

    /** mm³/s pushed through the hotend at a given layer height, line width and speed. */
    fun volumetricFlow(layerHeightMm: Double, lineWidthMm: Double, speedMmS: Double): Double =
        layerHeightMm * lineWidthMm * speedMmS

    fun maxSpeedForFlow(maxFlowMm3S: Double, layerHeightMm: Double, lineWidthMm: Double): Double? {
        val area = layerHeightMm * lineWidthMm
        if (area <= 0) return null
        return maxFlowMm3S / area
    }
}

data class CalcField(
    val key: String,
    val label: String,
    val default: Double,
    val step: Double,
    val unit: String,
    val decimals: Int,
)

data class CalcOutcome(val value: Double, val previous: Double, val gcode: String, val note: String = "")

/** A calculator card in the Calibration screen. */
data class Calibration(
    val id: String,
    val code: String,
    val name: String,
    val hint: String,
    val resultLabel: String,
    val unit: String,
    val decimals: Int,
    val formula: String,
    val fields: List<CalcField>,
    val compute: (Map<String, Double>) -> CalcOutcome?,
) {
    fun defaults(): Map<String, Double> = fields.associate { it.key to it.default }
}

private fun f(value: Double, decimals: Int) = String.format(Locale.US, "%.${decimals}f", value)

object Calibrations {

    val all: List<Calibration> = listOf(
        Calibration(
            id = "esteps", code = "M92", name = "E-steps",
            hint = "Mark 120 mm above the extruder, heat the hotend, extrude 100 mm slowly (G1 E100 F60), measure what is left to the mark.",
            resultLabel = "NEW E-STEPS", unit = "steps/mm", decimals = 2,
            formula = "new = current × requested ÷ (mark − left)",
            fields = listOf(
                CalcField("cur", "Current E-steps", 93.0, 0.1, "st", 2),
                CalcField("mark", "Mark distance", 120.0, 5.0, "mm", 1),
                CalcField("left", "Left to mark", 17.4, 0.2, "mm", 1),
                CalcField("req", "Requested", 100.0, 5.0, "mm", 1),
            ),
            compute = { v ->
                CalibrationMath.eSteps(v.getValue("cur"), v.getValue("mark"), v.getValue("left"), v.getValue("req"))
                    ?.let { CalcOutcome(it, v.getValue("cur"), "M92 E${f(it, 2)}\nM500") }
            },
        ),
        Calibration(
            id = "rotation", code = "KLIP", name = "Rotation dist",
            hint = "Klipper extruders: same 120 mm mark test, the result goes into printer.cfg.",
            resultLabel = "ROTATION DISTANCE", unit = "mm", decimals = 4,
            formula = "new = current × (mark − left) ÷ requested",
            fields = listOf(
                CalcField("cur", "Current rotation_distance", 22.6789, 0.01, "mm", 4),
                CalcField("mark", "Mark distance", 120.0, 5.0, "mm", 1),
                CalcField("left", "Left to mark", 21.0, 0.2, "mm", 1),
                CalcField("req", "Requested", 100.0, 5.0, "mm", 1),
            ),
            compute = { v ->
                CalibrationMath.rotationDistance(v.getValue("cur"), v.getValue("mark"), v.getValue("left"), v.getValue("req"))
                    ?.let { CalcOutcome(it, v.getValue("cur"), "[extruder]\nrotation_distance: ${f(it, 4)}") }
            },
        ),
        Calibration(
            id = "flow", code = "M221", name = "Flow",
            hint = "Print a single-wall cube in vase mode, measure the wall with calipers at four points and average.",
            resultLabel = "NEW FLOW", unit = "%", decimals = 1,
            formula = "new = current × expected ÷ measured",
            fields = listOf(
                CalcField("cur", "Current flow", 100.0, 1.0, "%", 1),
                CalcField("exp", "Expected wall", 0.45, 0.01, "mm", 2),
                CalcField("meas", "Measured wall", 0.48, 0.01, "mm", 2),
            ),
            compute = { v ->
                CalibrationMath.flow(v.getValue("cur"), v.getValue("exp"), v.getValue("meas"))
                    ?.let { CalcOutcome(it, v.getValue("cur"), "M221 S${f(it, 1)}", "Flow ratio ${f(it / 100, 3)}") }
            },
        ),
        Calibration(
            id = "shrink", code = "XY", name = "Shrinkage",
            hint = "Print a 100 mm bar (or calibration cube), let it cool fully, then measure it.",
            resultLabel = "XY SCALE", unit = "%", decimals = 2,
            formula = "scale = design ÷ measured × 100",
            fields = listOf(
                CalcField("design", "Design length", 100.0, 1.0, "mm", 2),
                CalcField("meas", "Measured length", 99.42, 0.02, "mm", 2),
            ),
            compute = { v ->
                CalibrationMath.shrinkageScale(v.getValue("design"), v.getValue("meas"))
                    ?.let { CalcOutcome(it, 100.0, "; slicer XY scale\n${f(it, 2)}%") }
            },
        ),
        Calibration(
            id = "pa", code = "M900", name = "Pressure adv",
            hint = "Print a PA tower and find the height with the sharpest corners.",
            resultLabel = "K FACTOR", unit = "K", decimals = 3,
            formula = "K = start + step × height",
            fields = listOf(
                CalcField("start", "Start value", 0.0, 0.005, "K", 3),
                CalcField("step", "Step per mm", 0.002, 0.001, "K", 3),
                CalcField("h", "Best height", 14.5, 0.5, "mm", 1),
            ),
            compute = { v ->
                val k = CalibrationMath.pressureAdvance(v.getValue("start"), v.getValue("step"), v.getValue("h"))
                CalcOutcome(k, v.getValue("start"), "M900 K${f(k, 3)} ; Marlin\nSET_PRESSURE_ADVANCE ADVANCE=${f(k, 3)} ; Klipper")
            },
        ),
        Calibration(
            id = "retract", code = "G10", name = "Retraction",
            hint = "Print a retraction tower and pick the band with the least stringing.",
            resultLabel = "RETRACT LENGTH", unit = "mm", decimals = 2,
            formula = "length = first + (band − 1) × step",
            fields = listOf(
                CalcField("start", "First band", 0.4, 0.1, "mm", 2),
                CalcField("inc", "Step per band", 0.2, 0.1, "mm", 2),
                CalcField("band", "Cleanest band #", 4.0, 1.0, "", 0),
            ),
            compute = { v ->
                CalibrationMath.retraction(v.getValue("start"), v.getValue("inc"), v.getValue("band"))
                    ?.let { CalcOutcome(it, v.getValue("start"), "M207 S${f(it, 2)} ; firmware retract\n; or set slicer retraction to ${f(it, 2)} mm") }
            },
        ),
        Calibration(
            id = "temp", code = "M104", name = "Temp tower",
            hint = "Print a temperature tower and count blocks from the bottom to the best-looking one.",
            resultLabel = "NOZZLE TEMP", unit = "°C", decimals = 0,
            formula = "temp = bottom − (block − 1) × drop",
            fields = listOf(
                CalcField("base", "Bottom block", 230.0, 5.0, "°C", 0),
                CalcField("step", "Drop per block", 5.0, 1.0, "°C", 0),
                CalcField("block", "Best block #", 4.0, 1.0, "", 0),
            ),
            compute = { v ->
                CalibrationMath.temperatureTower(v.getValue("base"), v.getValue("step"), v.getValue("block"))
                    ?.let { CalcOutcome(it, v.getValue("base"), "M104 S${f(it, 0)}") }
            },
        ),
        Calibration(
            id = "volumetric", code = "MM³", name = "Max flow",
            hint = "Check a print speed against your hotend's volumetric limit (≈15 mm³/s for a standard hotend, 25-35 for high-flow).",
            resultLabel = "VOLUMETRIC FLOW", unit = "mm³/s", decimals = 1,
            formula = "flow = layer height × line width × speed",
            fields = listOf(
                CalcField("h", "Layer height", 0.2, 0.04, "mm", 2),
                CalcField("w", "Line width", 0.45, 0.01, "mm", 2),
                CalcField("v", "Print speed", 200.0, 10.0, "mm/s", 0),
                CalcField("max", "Hotend limit", 15.0, 1.0, "mm³/s", 1),
            ),
            compute = { v ->
                val flow = CalibrationMath.volumetricFlow(v.getValue("h"), v.getValue("w"), v.getValue("v"))
                val maxSpeed = CalibrationMath.maxSpeedForFlow(v.getValue("max"), v.getValue("h"), v.getValue("w"))
                if (maxSpeed == null) null else {
                    val verdict = if (flow > v.getValue("max")) "OVER LIMIT" else "OK"
                    CalcOutcome(
                        flow, v.getValue("max"),
                        "; $verdict\n; max speed ${f(maxSpeed, 0)} mm/s at ${f(v.getValue("max"), 1)} mm³/s",
                    )
                }
            },
        ),
    )

    fun byId(id: String): Calibration = all.firstOrNull { it.id == id } ?: all.first()
}
