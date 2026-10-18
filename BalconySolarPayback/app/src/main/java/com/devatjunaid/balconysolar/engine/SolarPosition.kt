package com.devatjunaid.balconysolar.engine

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.tan

/**
 * Solar position after the NOAA Solar Calculator (Meeus, "Astronomical Algorithms").
 * Accuracy is about 0.01° in position and under a minute in rise/set times for
 * years 1901–2099 at latitudes within ±72°.
 */
object SolarPosition {

    /** Zenith used by NOAA for sunrise/sunset: 90° + 34' refraction + 16' solar radius. */
    private const val RISE_SET_ZENITH_DEG = 90.833

    data class Sun(
        /** Apparent elevation, corrected for atmospheric refraction. */
        val elevationDeg: Double,
        /** Geometric elevation without refraction (used for irradiance geometry). */
        val trueElevationDeg: Double,
        /** Azimuth clockwise from true north (0 = N, 90 = E, 180 = S, 270 = W). */
        val azimuthDeg: Double,
        val declinationDeg: Double,
        val equationOfTimeMin: Double,
        /** Solar hour angle, negative before solar noon. */
        val hourAngleDeg: Double,
        /** Earth–Sun distance in astronomical units. */
        val distanceAu: Double,
    ) {
        val zenithRad: Double get() = (90.0 - trueElevationDeg).toRad()
    }

    enum class Polar { NONE, POLAR_DAY, POLAR_NIGHT }

    data class SunTimes(
        val sunrise: ZonedDateTime?,
        val sunset: ZonedDateTime?,
        val solarNoon: ZonedDateTime,
        val dayLength: Duration,
        val noonElevationDeg: Double,
        val polar: Polar,
    )

    internal class Orbit(val declinationRad: Double, val equationOfTimeMin: Double, val distanceAu: Double)

    fun julianDay(epochMillis: Long): Double = epochMillis / 86_400_000.0 + 2_440_587.5

    internal fun orbit(julianDay: Double): Orbit {
        val t = (julianDay - 2_451_545.0) / 36_525.0
        val l0 = norm360(280.46646 + t * (36_000.76983 + 0.0003032 * t))
        val m = 357.52911 + t * (35_999.05029 - 0.0001537 * t)
        val e = 0.016708634 - t * (0.000042037 + 0.0000001267 * t)
        val mRad = m.toRad()
        val center = sin(mRad) * (1.914602 - t * (0.004817 + 0.000014 * t)) +
            sin(2 * mRad) * (0.019993 - 0.000101 * t) +
            sin(3 * mRad) * 0.000289
        val trueLongitude = l0 + center
        val trueAnomaly = m + center
        val distance = (1.000001018 * (1 - e * e)) / (1 + e * cos(trueAnomaly.toRad()))
        val omega = 125.04 - 1934.136 * t
        val apparentLongitude = trueLongitude - 0.00569 - 0.00478 * sin(omega.toRad())
        val meanObliquity = 23.0 + (26.0 + (21.448 - t * (46.815 + t * (0.00059 - t * 0.001813))) / 60.0) / 60.0
        val obliquity = meanObliquity + 0.00256 * cos(omega.toRad())
        val declination = asin(sin(obliquity.toRad()) * sin(apparentLongitude.toRad()))
        val y = tan((obliquity / 2).toRad()).pow(2)
        val l0Rad = l0.toRad()
        val eot = 4 * (
            y * sin(2 * l0Rad) - 2 * e * sin(mRad) +
                4 * e * y * sin(mRad) * cos(2 * l0Rad) -
                0.5 * y * y * sin(4 * l0Rad) -
                1.25 * e * e * sin(2 * mRad)
            ).toDeg()
        return Orbit(declination, eot, distance)
    }

    fun at(instant: Instant, latitudeDeg: Double, longitudeDeg: Double): Sun =
        at(instant.toEpochMilli(), latitudeDeg, longitudeDeg)

    fun at(epochMillis: Long, latitudeDeg: Double, longitudeDeg: Double): Sun {
        val o = orbit(julianDay(epochMillis))
        val minutesUtc = Math.floorMod(epochMillis, 86_400_000L) / 60_000.0
        val trueSolarTime = normalize(minutesUtc + o.equationOfTimeMin + 4 * longitudeDeg, 1440.0)
        val hourAngle = trueSolarTime / 4 - 180
        val lat = latitudeDeg.toRad()
        val decl = o.declinationRad
        val cosZenith = (sin(lat) * sin(decl) + cos(lat) * cos(decl) * cos(hourAngle.toRad())).coerceIn(-1.0, 1.0)
        val zenith = acos(cosZenith)
        val denominator = cos(lat) * sin(zenith)
        val azimuth = if (abs(denominator) > 1e-9) {
            val cosAz = ((sin(lat) * cosZenith - sin(decl)) / denominator).coerceIn(-1.0, 1.0)
            val a = acos(cosAz).toDeg()
            if (hourAngle > 0) norm360(a + 180) else norm360(540 - a)
        } else {
            if (latitudeDeg > 0) 180.0 else 0.0
        }
        val trueElevation = 90 - zenith.toDeg()
        return Sun(
            elevationDeg = trueElevation + refractionDeg(trueElevation),
            trueElevationDeg = trueElevation,
            azimuthDeg = azimuth,
            declinationDeg = decl.toDeg(),
            equationOfTimeMin = o.equationOfTimeMin,
            hourAngleDeg = hourAngle,
            distanceAu = o.distanceAu,
        )
    }

    /** Atmospheric refraction in degrees for a geometric elevation, NOAA approximation. */
    fun refractionDeg(elevationDeg: Double): Double {
        if (elevationDeg > 85.0) return 0.0
        val te = tan(elevationDeg.toRad())
        val arcSeconds = when {
            elevationDeg > 5.0 -> 58.1 / te - 0.07 / te.pow(3) + 0.000086 / te.pow(5)
            elevationDeg > -0.575 -> 1735.0 + elevationDeg * (-518.2 + elevationDeg * (103.4 + elevationDeg * (-12.79 + elevationDeg * 0.711)))
            else -> -20.772 / te
        }
        return arcSeconds / 3600.0
    }

    /** Sunrise, sunset and solar noon for a local calendar date, iterated to sub-minute precision. */
    fun sunTimes(date: LocalDate, latitudeDeg: Double, longitudeDeg: Double, zone: ZoneId): SunTimes {
        val base = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        fun millisAt(minutesUtc: Double) = base + (minutesUtc * 60_000.0).roundToLong()

        var noon = 720.0 - 4 * longitudeDeg
        repeat(3) { noon = 720.0 - 4 * longitudeDeg - orbit(julianDay(millisAt(noon))).equationOfTimeMin }

        val lat = latitudeDeg.toRad()
        var polar = Polar.NONE
        fun event(rising: Boolean): Double? {
            var t = noon
            repeat(4) {
                val o = orbit(julianDay(millisAt(t)))
                val cosHa = cos(RISE_SET_ZENITH_DEG.toRad()) / (cos(lat) * cos(o.declinationRad)) -
                    tan(lat) * tan(o.declinationRad)
                if (cosHa > 1) { polar = Polar.POLAR_NIGHT; return null }
                if (cosHa < -1) { polar = Polar.POLAR_DAY; return null }
                val ha = acos(cosHa).toDeg()
                val solarNoon = 720.0 - 4 * longitudeDeg - o.equationOfTimeMin
                t = if (rising) solarNoon - 4 * ha else solarNoon + 4 * ha
            }
            return t
        }

        val rise = event(rising = true)
        val set = event(rising = false)
        fun zoned(minutes: Double) = Instant.ofEpochMilli(millisAt(minutes)).atZone(zone)
        val length = when {
            rise != null && set != null -> Duration.ofMillis(((set - rise) * 60_000).roundToLong())
            polar == Polar.POLAR_DAY -> Duration.ofHours(24)
            else -> Duration.ZERO
        }
        return SunTimes(
            sunrise = rise?.let(::zoned),
            sunset = set?.let(::zoned),
            solarNoon = zoned(noon),
            dayLength = length,
            noonElevationDeg = at(millisAt(noon), latitudeDeg, longitudeDeg).elevationDeg,
            polar = if (rise != null && set != null) Polar.NONE else polar,
        )
    }
}

internal fun Double.toRad(): Double = Math.toRadians(this)
internal fun Double.toDeg(): Double = Math.toDegrees(this)
internal fun norm360(v: Double): Double = normalize(v, 360.0)
internal fun normalize(v: Double, period: Double): Double {
    val r = v % period
    return if (r < 0) r + period else r
}
