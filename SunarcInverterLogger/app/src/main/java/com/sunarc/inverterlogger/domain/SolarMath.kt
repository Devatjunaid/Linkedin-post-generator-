package com.sunarc.inverterlogger.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan

data class SunPosition(val elevationDeg: Double, val azimuthDeg: Double)

data class DayCurve(
    val date: LocalDate,
    val sunrise: ZonedDateTime?,
    val sunset: ZonedDateTime?,
    /** Clear-sky output in kW at each step, from sunrise to sunset. */
    val points: List<Pair<ZonedDateTime, Double>>,
    val clearSkyKwh: Double
)

data class PvSystem(
    val kwp: Double,
    val tiltDeg: Double,
    /** Compass direction the panels face: 180 = south, 90 = east, 270 = west. */
    val azimuthDeg: Double,
    val latitude: Double,
    val longitude: Double,
    /** Inverter, wiring, temperature and soiling losses together. */
    val systemEfficiency: Double = 0.86
)

/**
 * Sun position (NOAA equations) and a clear-sky model, so the app can show the sun arc,
 * sunrise/sunset and expected output with no internet connection or weather service.
 */
object SolarMath {
    private fun rad(d: Double) = d * PI / 180.0
    private fun deg(r: Double) = r * 180.0 / PI

    fun sunPosition(time: ZonedDateTime, latitude: Double, longitude: Double): SunPosition {
        val utc = time.withZoneSameInstant(ZoneId.of("UTC"))
        val dayOfYear = utc.dayOfYear
        val hour = utc.hour + utc.minute / 60.0 + utc.second / 3600.0
        val gamma = 2 * PI / 365.0 * (dayOfYear - 1 + (hour - 12) / 24.0)
        val eqTime = 229.18 * (0.000075 + 0.001868 * cos(gamma) - 0.032077 * sin(gamma) -
            0.014615 * cos(2 * gamma) - 0.040849 * sin(2 * gamma))
        val decl = 0.006918 - 0.399912 * cos(gamma) + 0.070257 * sin(gamma) - 0.006758 * cos(2 * gamma) +
            0.000907 * sin(2 * gamma) - 0.002697 * cos(3 * gamma) + 0.00148 * sin(3 * gamma)
        val trueSolarMin = hour * 60 + eqTime + 4 * longitude
        val hourAngle = rad(trueSolarMin / 4.0 - 180.0)
        val latR = rad(latitude)
        val cosZen = (sin(latR) * sin(decl) + cos(latR) * cos(decl) * cos(hourAngle)).coerceIn(-1.0, 1.0)
        val zenith = acos(cosZen)
        val azimuth = (deg(atan2(sin(hourAngle), cos(hourAngle) * sin(latR) - tan(decl) * cos(latR))) + 180.0 + 360.0) % 360.0
        return SunPosition(90.0 - deg(zenith), azimuth)
    }

    /** Sunrise and sunset, or nulls during polar day/night. */
    fun sunriseSunset(date: LocalDate, latitude: Double, longitude: Double, zone: ZoneId): Pair<ZonedDateTime?, ZonedDateTime?> {
        val gamma = 2 * PI / 365.0 * (date.dayOfYear - 1)
        val eqTime = 229.18 * (0.000075 + 0.001868 * cos(gamma) - 0.032077 * sin(gamma) -
            0.014615 * cos(2 * gamma) - 0.040849 * sin(2 * gamma))
        val decl = 0.006918 - 0.399912 * cos(gamma) + 0.070257 * sin(gamma) - 0.006758 * cos(2 * gamma) +
            0.000907 * sin(2 * gamma) - 0.002697 * cos(3 * gamma) + 0.00148 * sin(3 * gamma)
        val latR = rad(latitude)
        val cosHa = (cos(rad(90.833)) / (cos(latR) * cos(decl)) - tan(latR) * tan(decl))
        if (cosHa > 1 || cosHa < -1) return null to null
        val ha = deg(acos(cosHa))
        val riseMinUtc = 720 - 4 * (longitude + ha) - eqTime
        val setMinUtc = 720 - 4 * (longitude - ha) - eqTime
        val midnightUtc = date.atStartOfDay(ZoneId.of("UTC"))
        fun at(min: Double) = midnightUtc.plusSeconds((min * 60).toLong()).withZoneSameInstant(zone)
        return at(riseMinUtc) to at(setMinUtc)
    }

    /** Clear-sky power on the panel plane in W/m², from a simple direct + diffuse model. */
    fun planeIrradiance(sun: SunPosition, tiltDeg: Double, panelAzimuthDeg: Double): Double {
        if (sun.elevationDeg <= 0.5) return 0.0
        val zenith = rad(90.0 - sun.elevationDeg)
        val airMass = 1.0 / (cos(zenith) + 0.50572 * (96.07995 - deg(zenith)).pow(-1.6364))
        val dni = 1353.0 * 0.7.pow(airMass.pow(0.678))
        val ghi = 1098.0 * cos(zenith) * exp(-0.057 / cos(zenith))
        val dhi = max(ghi - dni * cos(zenith), 0.1 * ghi)
        val tilt = rad(tiltDeg)
        val cosAoi = cos(zenith) * cos(tilt) + sin(zenith) * sin(tilt) * cos(rad(sun.azimuthDeg - panelAzimuthDeg))
        val beam = dni * max(cosAoi, 0.0)
        val diffuse = dhi * (1 + cos(tilt)) / 2
        val reflected = ghi * 0.2 * (1 - cos(tilt)) / 2
        return beam + diffuse + reflected
    }

    fun clearSkyDay(system: PvSystem, date: LocalDate, zone: ZoneId, stepMinutes: Long = 10): DayCurve {
        val (rise, set) = sunriseSunset(date, system.latitude, system.longitude, zone)
        val start = rise ?: date.atStartOfDay(zone)
        val end = set ?: date.atTime(LocalTime.MAX).atZone(zone)
        val points = mutableListOf<Pair<ZonedDateTime, Double>>()
        var t = start
        var kwh = 0.0
        val hours = stepMinutes / 60.0
        while (!t.isAfter(end)) {
            val sun = sunPosition(t, system.latitude, system.longitude)
            val kw = system.kwp * planeIrradiance(sun, system.tiltDeg, system.azimuthDeg) / 1000.0 * system.systemEfficiency
            points += t to kw
            kwh += kw * hours
            t = t.plusMinutes(stepMinutes)
        }
        return DayCurve(date, rise, set, points, kwh)
    }

    /** Where the sun is along today's arc, 0 at sunrise and 1 at sunset. */
    fun dayProgress(now: ZonedDateTime, curve: DayCurve): Float {
        val rise = curve.sunrise ?: return 0.5f
        val set = curve.sunset ?: return 0.5f
        val total = (set.toEpochSecond() - rise.toEpochSecond()).toFloat()
        if (total <= 0f) return 0.5f
        return ((now.toEpochSecond() - rise.toEpochSecond()) / total).coerceIn(0f, 1f)
    }

    /** Share of today's clear-sky energy that should be done by [now]. */
    fun expectedShareSoFar(now: ZonedDateTime, curve: DayCurve): Double {
        if (curve.clearSkyKwh <= 0) return 0.0
        val done = curve.points.zipWithNext().sumOf { (a, b) ->
            if (!b.first.isAfter(now)) (a.second + b.second) / 2 * (b.first.toEpochSecond() - a.first.toEpochSecond()) / 3600.0 else 0.0
        }
        return (done / curve.clearSkyKwh).coerceIn(0.0, 1.0)
    }

    fun nowIn(zone: ZoneId): ZonedDateTime = ZonedDateTime.ofInstant(Instant.now(), zone)

    fun asin01(x: Double) = asin(x.coerceIn(-1.0, 1.0))
}
