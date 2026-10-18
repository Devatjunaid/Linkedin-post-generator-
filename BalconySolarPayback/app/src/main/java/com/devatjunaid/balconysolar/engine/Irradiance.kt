package com.devatjunaid.balconysolar.engine

import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan

/** Irradiance building blocks: extraterrestrial radiation, diffuse split, hourly shape and plane-of-array transposition. */
object Irradiance {

    /** Total solar irradiance at 1 AU, W/m² (Kopp & Lean 2011). */
    const val SOLAR_CONSTANT = 1361.0

    fun extraterrestrialNormal(distanceAu: Double): Double = SOLAR_CONSTANT / (distanceAu * distanceAu)

    /** Sunset hour angle in radians for a latitude and declination (geometric horizon). */
    fun sunsetHourAngle(latitudeRad: Double, declinationRad: Double): Double =
        acos((-tan(latitudeRad) * tan(declinationRad)).coerceIn(-1.0, 1.0))

    /** Daily extraterrestrial irradiation on a horizontal plane, Wh/m² (Duffie & Beckman eq. 1.10.3). */
    fun dailyExtraterrestrialWh(latitudeRad: Double, declinationRad: Double, distanceAu: Double): Double {
        val ws = sunsetHourAngle(latitudeRad, declinationRad)
        val h0 = 24.0 / PI * extraterrestrialNormal(distanceAu) *
            (cos(latitudeRad) * cos(declinationRad) * sin(ws) + ws * sin(latitudeRad) * sin(declinationRad))
        return max(0.0, h0)
    }

    /** Daily diffuse fraction from the daily clearness index (Erbs, Klein & Duffie 1982). */
    fun erbsDailyDiffuseFraction(kt: Double, sunsetHourAngleDeg: Double): Double {
        val f = if (sunsetHourAngleDeg <= 81.4) {
            if (kt < 0.715) 1.0 - 0.2727 * kt + 2.4495 * kt.pow(2) - 11.9514 * kt.pow(3) + 9.3879 * kt.pow(4) else 0.143
        } else {
            if (kt < 0.722) 1.0 + 0.2832 * kt - 2.5557 * kt.pow(2) + 0.8448 * kt.pow(3) else 0.175
        }
        return f.coerceIn(0.1, 1.0)
    }

    /** Collares-Pereira & Rabl ratio of hourly to daily global irradiation, per hour. */
    fun collaresPereiraRabl(hourAngleRad: Double, sunsetHourAngleRad: Double): Double {
        if (kotlin.math.abs(hourAngleRad) >= sunsetHourAngleRad) return 0.0
        val a = 0.409 + 0.5016 * sin(sunsetHourAngleRad - PI / 3)
        val b = 0.6609 - 0.4767 * sin(sunsetHourAngleRad - PI / 3)
        return max(0.0, PI / 24 * (a + b * cos(hourAngleRad)) * liuJordanCore(hourAngleRad, sunsetHourAngleRad))
    }

    /** Liu & Jordan ratio of hourly to daily diffuse irradiation, per hour. */
    fun liuJordan(hourAngleRad: Double, sunsetHourAngleRad: Double): Double {
        if (kotlin.math.abs(hourAngleRad) >= sunsetHourAngleRad) return 0.0
        return max(0.0, PI / 24 * liuJordanCore(hourAngleRad, sunsetHourAngleRad))
    }

    private fun liuJordanCore(w: Double, ws: Double): Double =
        (cos(w) - cos(ws)) / (sin(ws) - ws * cos(ws))

    /** Relative air mass, Kasten & Young (1989). */
    fun airMass(zenithDeg: Double): Double {
        val z = zenithDeg.coerceAtMost(89.9)
        return 1.0 / (cos(z.toRad()) + 0.50572 * (96.07995 - z).pow(-1.6364))
    }

    /** Cosine of the angle of incidence of beam radiation on a tilted surface. */
    fun cosIncidence(tiltRad: Double, surfaceAzimuthDeg: Double, zenithRad: Double, sunAzimuthDeg: Double): Double =
        cos(zenithRad) * cos(tiltRad) + sin(zenithRad) * sin(tiltRad) * cos((sunAzimuthDeg - surfaceAzimuthDeg).toRad())

    // Perez et al. (1990) "allsitescomposite1990" coefficients, one row per sky-clearness bin.
    private val epsilonUpper = doubleArrayOf(1.065, 1.230, 1.500, 1.950, 2.800, 4.500, 6.200)
    private val f11 = doubleArrayOf(-0.008, 0.130, 0.330, 0.568, 0.873, 1.132, 1.060, 0.678)
    private val f12 = doubleArrayOf(0.588, 0.683, 0.487, 0.187, -0.392, -1.237, -1.600, -0.327)
    private val f13 = doubleArrayOf(-0.062, -0.151, -0.221, -0.295, -0.362, -0.412, -0.359, -0.250)
    private val f21 = doubleArrayOf(-0.060, -0.019, 0.055, 0.109, 0.226, 0.288, 0.264, 0.156)
    private val f22 = doubleArrayOf(0.072, 0.066, -0.064, -0.152, -0.462, -0.823, -1.127, -1.377)
    private val f23 = doubleArrayOf(-0.022, -0.029, -0.026, -0.014, 0.001, 0.056, 0.131, 0.251)

    /** Sky-diffuse irradiance on a tilted plane with the Perez (1990) anisotropic model, W/m². */
    fun perezSkyDiffuse(
        tiltRad: Double,
        cosAoi: Double,
        zenithRad: Double,
        dhi: Double,
        dni: Double,
        dniExtra: Double,
        airMass: Double,
    ): Double {
        if (dhi <= 0.0) return 0.0
        val kappa = 1.041
        val z3 = zenithRad.pow(3)
        val epsilon = ((dhi + dni) / dhi + kappa * z3) / (1 + kappa * z3)
        val delta = dhi * airMass / dniExtra
        var bin = epsilonUpper.size
        for (i in epsilonUpper.indices) if (epsilon < epsilonUpper[i]) { bin = i; break }
        val f1 = max(0.0, f11[bin] + f12[bin] * delta + f13[bin] * zenithRad)
        val f2 = f21[bin] + f22[bin] * delta + f23[bin] * zenithRad
        val a = max(0.0, cosAoi)
        val b = max(cos(85.0.toRad()), cos(zenithRad))
        return max(0.0, dhi * ((1 - f1) * (1 + cos(tiltRad)) / 2 + f1 * a / b + f2 * sin(tiltRad)))
    }

    /** Martin & Ruiz (2001) incidence angle modifier for beam radiation. */
    fun iamBeam(cosAoi: Double, ar: Double = 0.16): Double {
        if (cosAoi <= 0.0) return 0.0
        return (1 - exp(-cosAoi / ar)) / (1 - exp(-1 / ar))
    }

    /** Martin & Ruiz diffuse modifiers for sky and ground-reflected light, as in pvlib. */
    fun iamDiffuse(tiltRad: Double, ar: Double = 0.16): Pair<Double, Double> {
        val c1 = 4 / (3 * PI)
        val c2 = 0.5 * ar - 0.154
        val s = sin(tiltRad)
        val c = cos(tiltRad)
        val skyTerm = s + (PI - tiltRad - s) / (1 + c)
        val sky = 1 - exp(-(c1 + c2 * skyTerm) * skyTerm / ar)
        val ground = if (tiltRad < 1e-4) 0.0 else {
            val gTerm = s + (tiltRad - s) / (1 - c)
            1 - exp(-(c1 + c2 * gTerm) * gTerm / ar)
        }
        return sky to ground
    }
}
