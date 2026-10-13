package com.aquadoselab.poolchemistry.chem

import com.aquadoselab.poolchemistry.data.Pool
import com.aquadoselab.poolchemistry.data.PoolShape
import com.aquadoselab.poolchemistry.data.Sanitizer
import com.aquadoselab.poolchemistry.data.Surface
import com.aquadoselab.poolchemistry.data.Targets
import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sqrt

const val LITERS_PER_GALLON = 3.785411784
const val GALLONS_PER_CUBIC_FOOT = 7.48052
const val FEET_PER_METER = 3.28084

fun round1(v: Double) = round(v * 10) / 10.0

object Volume {
    /** Volume in US gallons from dimensions in feet. */
    fun gallons(
        shape: PoolShape, lengthFt: Double, widthFt: Double, width2Ft: Double,
        shallowFt: Double, deepFt: Double
    ): Double {
        val avg = when {
            shallowFt > 0 && deepFt > 0 -> (shallowFt + deepFt) / 2
            else -> max(shallowFt, deepFt)
        }
        val cubicFeet = when (shape) {
            PoolShape.RECTANGLE -> lengthFt * widthFt * avg
            PoolShape.ROUND -> PI * (lengthFt / 2).pow(2) * avg
            PoolShape.OVAL -> PI * (lengthFt / 2) * (widthFt / 2) * avg
            PoolShape.KIDNEY -> 0.45 * (widthFt + width2Ft) * lengthFt * avg
            PoolShape.CUSTOM -> 0.0
        }
        return cubicFeet * GALLONS_PER_CUBIC_FOOT
    }

    fun gallons(pool: Pool): Double =
        if (pool.shape == PoolShape.CUSTOM) pool.volumeGallons
        else gallons(pool.shape, pool.lengthFt, pool.widthFt, pool.width2Ft, pool.shallowFt, pool.deepFt)
}

enum class Level { LOW, OK, HIGH, UNKNOWN }

/** Recommended free-chlorine levels; they depend on stabilizer (CYA). */
data class FcLevels(val min: Double, val target: Double, val upper: Double, val slam: Double)

/** Acceptable ranges for one pool. */
data class Ranges(
    val phLow: Double, val phHigh: Double,
    val taLow: Double, val taHigh: Double,
    val chLow: Double, val chHigh: Double,
    val cyaLow: Double, val cyaHigh: Double,
    val saltLow: Double, val saltHigh: Double
)

object Chemistry {

    fun fcLevels(cyaPpm: Double, sanitizer: Sanitizer): FcLevels {
        val cya = cyaPpm.coerceAtLeast(0.0)
        val min: Double
        val target: Double
        if (sanitizer == Sanitizer.SALT) {
            min = max(1.0, cya * 0.045)
            target = max(2.0, cya * 0.045 + 2.0)
        } else {
            min = max(1.0, cya * 0.075)
            target = max(2.0, cya * 0.075 + 2.0)
        }
        val slam = max(10.0, cya * 0.4)
        return FcLevels(round1(min), round1(target), round1(target + 3.0), round1(slam))
    }

    fun ranges(pool: Pool): Ranges = ranges(pool.targets, pool.surface)

    fun ranges(t: Targets, surface: Surface): Ranges {
        val chHigh = if (surface == Surface.PLASTER) 650.0 else 450.0
        val chLow = if (surface == Surface.PLASTER) 250.0 else 150.0
        return Ranges(
            phLow = t.ph - 0.2, phHigh = t.ph + 0.2,
            taLow = max(40.0, t.ta - 20), taHigh = t.ta + 40,
            chLow = minOf(chLow, t.ch - 50), chHigh = chHigh,
            cyaLow = max(0.0, t.cya - 10), cyaHigh = t.cya + 30,
            saltLow = t.salt - 400, saltHigh = t.salt + 600
        )
    }

    fun level(value: Double?, low: Double, high: Double): Level = when {
        value == null -> Level.UNKNOWN
        value < low - 1e-9 -> Level.LOW
        value > high + 1e-9 -> Level.HIGH
        else -> Level.OK
    }

    // ---------------------------------------------------------------------------------------
    // Carbonate / cyanurate buffer model. Used to work out acid / base doses from pH, TA, CYA.
    // TA (ppm as CaCO3) -> eq/L: / 50,000.  CYA ppm -> mol/L: / 129,070.
    // ---------------------------------------------------------------------------------------
    private const val PK1 = 6.3
    private const val PK2 = 10.33
    private const val PKA_CYA = 6.88

    private fun cyaAlk(cyaPpm: Double, ph: Double): Double =
        (cyaPpm / 129_070.0) / (1 + 10.0.pow(PKA_CYA - ph))

    /** Alkalinity contributed per mol of dissolved inorganic carbon at this pH (α1 + 2·α2). */
    private fun carbFactor(ph: Double): Double {
        val h = 10.0.pow(-ph)
        val k1 = 10.0.pow(-PK1)
        val k2 = 10.0.pow(-PK2)
        val d = h * h + k1 * h + k1 * k2
        return (k1 * h + 2 * k1 * k2) / d
    }

    /** Total dissolved carbonate (mol/L) implied by a TA / pH / CYA reading. */
    fun totalCarbon(taPpm: Double, ph: Double, cyaPpm: Double): Double {
        val carbAlk = max(taPpm / 50_000.0 - cyaAlk(cyaPpm, ph), 1e-6)
        return carbAlk / carbFactor(ph)
    }

    /** Total alkalinity (eq/L) for a given carbonate total, pH and CYA. */
    fun alkalinity(ct: Double, ph: Double, cyaPpm: Double): Double =
        ct * carbFactor(ph) + cyaAlk(cyaPpm, ph)

    /** pH reached for a given alkalinity (eq/L) and carbonate total. */
    fun phFor(alkEq: Double, ct: Double, cyaPpm: Double): Double {
        var lo = 4.0
        var hi = 11.0
        repeat(60) {
            val mid = (lo + hi) / 2
            if (alkalinity(ct, mid, cyaPpm) < alkEq) lo = mid else hi = mid
        }
        return (lo + hi) / 2
    }

    /** Acid needed (eq per litre) to move pH down from [from] to [to]. */
    fun acidEqPerLiter(from: Double, to: Double, taPpm: Double, cyaPpm: Double): Double {
        val ct = totalCarbon(taPpm, from, cyaPpm)
        return max(0.0, alkalinity(ct, from, cyaPpm) - alkalinity(ct, to, cyaPpm))
    }

    /** Sodium carbonate needed (mol per litre) to raise pH from [from] to [to]. */
    fun sodaAshMolPerLiter(from: Double, to: Double, taPpm: Double, cyaPpm: Double): Double {
        val ct = totalCarbon(taPpm, from, cyaPpm)
        val alk0 = alkalinity(ct, from, cyaPpm)
        val a2 = carbFactor(to)
        val n = (ct * a2 + cyaAlk(cyaPpm, to) - alk0) / (2 - a2)
        return max(0.0, n)
    }

    /** pH after adding sodium bicarbonate (mol per litre). */
    fun phAfterBakingSoda(ph: Double, taPpm: Double, cyaPpm: Double, molPerLiter: Double): Double {
        val ct = totalCarbon(taPpm, ph, cyaPpm)
        val alk = alkalinity(ct, ph, cyaPpm)
        return phFor(alk + molPerLiter, ct + molPerLiter, cyaPpm)
    }

    // ---------------------------------------------------------------------------------------
    // Calcite Saturation Index (CSI). Negative = corrosive to plaster/metal, positive = scaling.
    // ---------------------------------------------------------------------------------------
    fun csi(ph: Double, taPpm: Double, chPpm: Double, cyaPpm: Double, tempF: Double, saltPpm: Double): Double? {
        if (chPpm <= 0 || taPpm <= 0) return null
        val carbAlk = taPpm - 0.38772 * cyaPpm / (1 + 10.0.pow(6.83 - ph))
        if (carbAlk <= 0) return null
        val extraNaCl = max(0.0, saltPpm - 1.1678 * chPpm)
        val ionic = (1.5 * chPpm + taPpm) / 50_045.0 + extraNaCl / 58_440.0
        val sq = sqrt(ionic)
        val tempC = (tempF - 32) * 5 / 9
        return ph - 11.677 + log10(chPpm) + log10(carbAlk) -
            2.56 * sq / (1 + 1.65 * sq) - 1412.5 / (tempC + 273.15) + 4.7375
    }

    fun csiLevel(csi: Double?): Level = when {
        csi == null -> Level.UNKNOWN
        csi < -0.3 -> Level.LOW
        csi > 0.3 -> Level.HIGH
        else -> Level.OK
    }

    fun csiMeaning(csi: Double?): String = when {
        csi == null -> "Enter pH, alkalinity and calcium to see water balance."
        csi < -0.6 -> "Corrosive — water can etch plaster and corrode metal. Raise calcium or pH."
        csi < -0.3 -> "Slightly corrosive. Aim to raise calcium, TA or pH a little."
        csi <= 0.3 -> "Balanced — not corrosive, not scaling."
        csi <= 0.6 -> "Slightly scale-forming. Lower pH or TA a little."
        else -> "Scale-forming — expect cloudy water and crusty deposits. Lower pH and TA."
    }
}
