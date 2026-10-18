package com.devatjunaid.balconysolar.engine

import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

/**
 * Time-step yield simulation for a small PV system.
 *
 * Monthly climate means are turned into a sunny, an average and a cloudy day per calendar day
 * (mean clearness preserved), split into time steps with Collares-Pereira–Rabl / Liu–Jordan,
 * transposed with Perez 1990, and run through IAM, temperature (PVsyst), low-light and a
 * PVWatts inverter model with AC clipping.
 */
object YieldEngine {

    private val CLASS_FACTORS = doubleArrayOf(1.45, 1.0, 0.55)
    private const val MAX_KT = 0.78
    private const val CLASS_WEIGHT = 1.0 / 3.0
    private const val LOW_LIGHT_COEFF = 0.02
    private const val ABSORPTANCE = 0.9

    fun simulate(site: Site, pv: PvSystem, stepMinutes: Int = 15, year: Int = 2025): YieldResult {
        require(1440 % stepMinutes == 0) { "Step must divide a day" }
        val zone = ZoneId.of(site.zoneId)
        val lat = site.latitude.toRad()
        val slots = 1440 / stepMinutes
        val dtH = stepMinutes / 60.0
        val tilt = pv.tiltDeg.coerceIn(0.0, 90.0).toRad()
        val (iamSky, iamGround) = Irradiance.iamDiffuse(tilt)
        val pStc = pv.peakW
        val gamma = pv.tempCoeffPct / 100.0
        val shade = 1 - pv.shadingPct.coerceIn(0.0, 100.0) / 100.0
        val soil = 1 - pv.soilingPct.coerceIn(0.0, 100.0) / 100.0
        val cable = 1 - pv.cableMismatchPct.coerceIn(0.0, 100.0) / 100.0
        val groundView = (1 - cos(tilt)) / 2
        val climate = site.climate

        val stage = DoubleArray(9) // Wh: nominal, iam, shade, soil, lowlight, temp, cable, inverterUnclipped, ac
        val monthlyWh = DoubleArray(12)
        var ghiWh = 0.0
        var poaWh = 0.0
        val classDay = List(12) { List(3) { DoubleArray(slots) } }

        val rt = DoubleArray(slots)
        val rd = DoubleArray(slots)
        val cosZ = DoubleArray(slots)
        val zenith = DoubleArray(slots)
        val cosAoi = DoubleArray(slots)
        val dniExtra = DoubleArray(slots)
        val hourAngle = DoubleArray(slots)

        for (m in 0 until 12) {
            val days = DAYS_IN_MONTH[m]
            val dates = (1..days).map { LocalDate.of(year, m + 1, it) }
            val h0 = DoubleArray(days)
            val decl = DoubleArray(days)
            for ((i, d) in dates.withIndex()) {
                val noon = d.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
                val o = SolarPosition.orbit(SolarPosition.julianDay(noon))
                decl[i] = o.declinationRad
                h0[i] = Irradiance.dailyExtraterrestrialWh(lat, o.declinationRad, o.distanceAu)
            }
            val meanH0 = h0.average()
            val kt = if (meanH0 > 1.0) (climate.ghiDaily[m] * 1000.0 / meanH0) else 0.0
            val kts = classClearness(kt)
            val tMean = climate.tempMean[m]

            for ((i, d) in dates.withIndex()) {
                if (h0[i] <= 1.0) continue
                val ws = Irradiance.sunsetHourAngle(lat, decl[i])
                val wsDeg = ws.toDeg()
                val dayStart = d.atStartOfDay(zone).toInstant().toEpochMilli()
                var sumRt = 0.0
                var sumRd = 0.0
                for (s in 0 until slots) {
                    val t = dayStart + ((s * stepMinutes + stepMinutes / 2.0) * 60_000.0).toLong()
                    val sun = SolarPosition.at(t, site.latitude, site.longitude)
                    val w = sun.hourAngleDeg.toRad()
                    hourAngle[s] = sun.hourAngleDeg
                    rt[s] = Irradiance.collaresPereiraRabl(w, ws)
                    rd[s] = Irradiance.liuJordan(w, ws)
                    sumRt += rt[s] * dtH
                    sumRd += rd[s] * dtH
                    zenith[s] = sun.zenithRad
                    cosZ[s] = cos(sun.zenithRad)
                    cosAoi[s] = Irradiance.cosIncidence(tilt, pv.azimuthDeg, sun.zenithRad, sun.azimuthDeg)
                    dniExtra[s] = Irradiance.extraterrestrialNormal(sun.distanceAu)
                }
                if (sumRt <= 0.0 || sumRd <= 0.0) continue

                for (c in 0 until 3) {
                    val dailyH = kts[c] * h0[i]
                    val dailyHd = Irradiance.erbsDailyDiffuseFraction(kts[c], wsDeg) * dailyH
                    for (s in 0 until slots) {
                        if (rt[s] <= 0.0) continue
                        val g = rt[s] / sumRt * dailyH
                        val d0 = min(g, rd[s] / sumRd * dailyHd)
                        val b = g - d0
                        val dni = if (cosZ[s] > 0.035) min(b / cosZ[s], 0.95 * dniExtra[s]) else 0.0
                        val dhi = g - dni * max(0.0, cosZ[s])
                        val zenDeg = zenith[s].toDeg()
                        val beam = dni * max(0.0, cosAoi[s])
                        val sky = if (tilt < 1e-6) dhi else Irradiance.perezSkyDiffuse(
                            tilt, cosAoi[s], zenith[s], dhi, dni, dniExtra[s], Irradiance.airMass(zenDeg),
                        )
                        val ground = g * pv.albedo * groundView
                        val poa = beam + sky + ground
                        val effective = beam * Irradiance.iamBeam(cosAoi[s]) + sky * iamSky + ground * iamGround

                        val p0 = pStc * poa / 1000.0
                        val p1 = pStc * effective / 1000.0
                        val p2 = p1 * shade
                        val p3 = p2 * soil
                        val p4 = p3 * lowLightFactor(effective * shade * soil)
                        val solarHour = 12.0 + hourAngle[s] / 15.0
                        val tAmb = tMean + climate.diurnalRange / 2 * cos(2 * PI * (solarHour - 15.0) / 24.0)
                        val tCell = tAmb + ABSORPTANCE * poa * (1 - pv.moduleEfficiency) / pv.mounting.heatLossU
                        val p5 = max(0.0, p4 * (1 + gamma * (tCell - 25.0)))
                        val p6 = p5 * cable
                        val acUnclipped = inverterAc(p6, pv.inverterAcW, pv.inverterEfficiency, clip = false)
                        val ac = min(acUnclipped, pv.inverterAcW)

                        val wgt = CLASS_WEIGHT * dtH
                        stage[0] += p0 * wgt; stage[1] += p1 * wgt; stage[2] += p2 * wgt
                        stage[3] += p3 * wgt; stage[4] += p4 * wgt; stage[5] += p5 * wgt
                        stage[6] += p6 * wgt; stage[7] += acUnclipped * wgt; stage[8] += ac * wgt
                        monthlyWh[m] += ac * wgt
                        ghiWh += g * wgt
                        poaWh += poa * wgt
                        classDay[m][c][s] += ac / days
                    }
                }
            }
        }

        val labels = listOf(
            "Reflection off the glass", "Shading", "Dirt & dust", "Low light",
            "Cell temperature", "Cables & mismatch", "Inverter conversion", "Inverter power limit",
        )
        val losses = labels.mapIndexed { i, label ->
            val before = stage[i]
            val lost = before - stage[i + 1]
            LossStep(label, lost / 1000.0, if (before > 0) lost / before * 100 else 0.0)
        }
        val annual = stage[8] / 1000.0
        val typical = List(12) { m -> DoubleArray(slots) { s -> (0 until 3).sumOf { classDay[m][it][s] } / 3 } }
        return YieldResult(
            annualKwh = annual,
            monthlyKwh = monthlyWh.map { it / 1000.0 },
            specificYield = if (pStc > 0) annual / (pStc / 1000.0) else 0.0,
            ghiAnnualKwhM2 = ghiWh / 1000.0,
            poaAnnualKwhM2 = poaWh / 1000.0,
            nominalKwh = stage[0] / 1000.0,
            losses = losses,
            peakAcW = classDay.maxOf { month -> month[0].maxOrNull() ?: 0.0 },
            typicalDayW = typical,
            classDayW = classDay,
            stepMinutes = stepMinutes,
        )
    }

    /** Sunny / average / cloudy clearness indices whose mean equals the monthly [kt]. */
    internal fun classClearness(kt: Double): DoubleArray {
        if (kt <= 0.0) return DoubleArray(3)
        if (kt >= MAX_KT || kt < 0.05) return DoubleArray(3) { kt }
        val clear = min(MAX_KT, CLASS_FACTORS[0] * kt)
        var cloudy = CLASS_FACTORS[2] * kt
        var mid = 3 * kt - clear - cloudy
        if (mid > clear) { mid = clear; cloudy = 3 * kt - 2 * clear }
        return doubleArrayOf(clear, mid, cloudy)
    }

    internal fun lowLightFactor(irradiance: Double): Double = when {
        irradiance <= 0.0 -> 0.0
        irradiance >= 1000.0 -> 1.0
        else -> (1 + LOW_LIGHT_COEFF * ln(irradiance / 1000.0)).coerceIn(0.7, 1.0)
    }

    /** PVWatts v5 inverter efficiency curve; returns AC watts. */
    fun inverterAc(pdc: Double, acLimitW: Double, nominalEfficiency: Double, clip: Boolean = true): Double {
        if (pdc <= 0.0 || acLimitW <= 0.0) return 0.0
        val pdc0 = acLimitW / nominalEfficiency
        val zeta = pdc / pdc0
        val eta = nominalEfficiency / 0.9637 * (-0.0162 * zeta - 0.0059 / zeta + 0.9858)
        val ac = max(0.0, eta * pdc)
        return if (clip) min(ac, acLimitW) else ac
    }

    data class TiltScan(val bestTiltDeg: Double, val bestKwh: Double, val currentKwh: Double, val curve: List<Pair<Double, Double>>)

    /** Annual yield across tilts 0–90° at the current azimuth, coarse 30-minute steps. */
    fun scanTilt(site: Site, pv: PvSystem): TiltScan {
        val curve = (0..90 step 5).map { t ->
            t.toDouble() to simulate(site, pv.copy(tiltDeg = t.toDouble()), stepMinutes = 30).annualKwh
        }
        val best = curve.maxBy { it.second }
        val current = simulate(site, pv, stepMinutes = 30).annualKwh
        return TiltScan(best.first, best.second, current, curve)
    }
}
