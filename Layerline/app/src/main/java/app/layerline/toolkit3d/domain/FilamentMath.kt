package app.layerline.toolkit3d.domain

import kotlin.math.PI
import kotlin.math.max

object FilamentMath {

    /** Grams of filament in one metre for a given diameter (mm) and density (g/cm³). */
    fun gramsPerMeter(diameterMm: Double, density: Double): Double {
        val radiusCm = diameterMm / 20.0
        val areaCm2 = PI * radiusCm * radiusCm
        return areaCm2 * 100.0 * density
    }

    fun metersFromGrams(grams: Double, diameterMm: Double, density: Double): Double {
        val gpm = gramsPerMeter(diameterMm, density)
        return if (gpm > 0) grams / gpm else 0.0
    }

    fun gramsFromMeters(meters: Double, diameterMm: Double, density: Double): Double =
        meters * gramsPerMeter(diameterMm, density)

    /** Filament left on a spool weighed on a kitchen scale. */
    fun remainingFromGross(grossGrams: Double, tareGrams: Double): Double = max(0.0, grossGrams - tareGrams)
}
