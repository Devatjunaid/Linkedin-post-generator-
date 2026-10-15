package com.voltora.evcalculator.domain

import com.voltora.evcalculator.data.EvModel
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

enum class ChargeLocation(val label: String, val isDc: Boolean, val powerOptions: List<Double>) {
    HOME("Home", false, listOf(2.3, 3.7, 7.4, 11.0, 22.0)),
    PUBLIC_AC("Public AC", false, listOf(7.4, 11.0, 22.0)),
    DC_FAST("DC fast", true, listOf(50.0, 100.0, 150.0, 250.0, 350.0)),
}

enum class DrivingStyle(val label: String, val factor: Double, val avgSpeedKmh: Double) {
    CITY("City", 0.88, 35.0),
    MIXED("Mixed", 1.00, 70.0),
    HIGHWAY("Highway", 1.18, 100.0),
    WINTER("Winter", 1.35, 70.0),
}

data class ChargeResult(
    val energyToBatteryKwh: Double,
    val energyBilledKwh: Double,
    val cost: Double,
    val minutes: Double,
    val effectivePowerKw: Double,
    val rangeAddedKm: Double,
    val acLimitedByCar: Boolean,
)

sealed interface TripLeg {
    data class Drive(val km: Double, val minutes: Double, val fromSoc: Double, val toSoc: Double, val endLabel: String) : TripLeg
    data class Stop(
        val atKm: Double, val fromSoc: Double, val toSoc: Double,
        val kwh: Double, val minutes: Double, val cost: Double,
    ) : TripLeg
}

data class TripPlan(
    val distanceKm: Double,
    val legs: List<TripLeg>,
    val stops: Int,
    val totalEnergyKwh: Double,
    val homeEnergyCost: Double,
    val chargingCost: Double,
    val chargingMinutes: Double,
    val drivingMinutes: Double,
    val arrivalSoc: Double,
    /** SoC to leave with so that no stop is needed, or null if a full battery is not enough. */
    val noStopStartSoc: Double?,
    val petrolCost: Double,
    val feasible: Boolean,
) {
    val totalCost: Double get() = homeEnergyCost + chargingCost
    val costPer100Km: Double get() = if (distanceKm > 0) totalCost / distanceKm * 100 else 0.0
    val savingsVsPetrol: Double get() = petrolCost - totalCost
}

object EvMath {
    /** Wall-to-battery efficiency for AC charging (on-board charger + cable losses). */
    const val AC_EFFICIENCY = 0.90
    /** DC chargers bill at the charger meter; ~5 % is lost before the cells. */
    const val DC_EFFICIENCY = 0.95

    fun kwhPer100(model: EvModel, style: DrivingStyle): Double = model.realKwhPer100 * style.factor

    fun rangeKm(model: EvModel, soc: Double, style: DrivingStyle = DrivingStyle.MIXED): Double =
        model.batteryKwh * soc / kwhPer100(model, style) * 100.0

    fun charge(
        model: EvModel,
        fromSoc: Double,
        toSoc: Double,
        location: ChargeLocation,
        chargerKw: Double,
        pricePerKwh: Double,
        style: DrivingStyle = DrivingStyle.MIXED,
    ): ChargeResult {
        val from = fromSoc.coerceIn(0.0, 1.0)
        val to = toSoc.coerceIn(from, 1.0)
        val toBattery = (to - from) * model.batteryKwh
        return if (location.isDc) {
            val billed = toBattery / DC_EFFICIENCY
            val minutes = dcMinutes(model, from, to, chargerKw)
            ChargeResult(
                energyToBatteryKwh = toBattery,
                energyBilledKwh = billed,
                cost = billed * pricePerKwh,
                minutes = minutes,
                effectivePowerKw = if (minutes > 0) billed / (minutes / 60.0) else 0.0,
                rangeAddedKm = toBattery / kwhPer100(model, style) * 100.0,
                acLimitedByCar = false,
            )
        } else {
            val power = min(chargerKw, model.maxAcKw)
            val billed = toBattery / AC_EFFICIENCY
            // Above ~90 % most cars taper AC charging slightly; add a small penalty.
            val taper = max(0.0, to - max(from, 0.9)) * model.batteryKwh / AC_EFFICIENCY / power * 0.35
            val minutes = (billed / power + taper) * 60.0
            ChargeResult(
                energyToBatteryKwh = toBattery,
                energyBilledKwh = billed,
                cost = billed * pricePerKwh,
                minutes = minutes,
                effectivePowerKw = power,
                rangeAddedKm = toBattery / kwhPer100(model, style) * 100.0,
                acLimitedByCar = chargerKw > model.maxAcKw,
            )
        }
    }

    /**
     * Generic DC charge curve (fraction of peak power vs state of charge). The curve shape is
     * calibrated per car so that 10→80 % matches the manufacturer figure.
     */
    private val curve = listOf(
        0.00 to 0.70, 0.10 to 1.00, 0.50 to 0.80, 0.80 to 0.45, 0.90 to 0.25, 1.00 to 0.10,
    )

    private fun curveAt(s: Double): Double {
        for (i in 1 until curve.size) {
            val (x1, y1) = curve[i]
            if (s <= x1) {
                val (x0, y0) = curve[i - 1]
                return y0 + (y1 - y0) * (s - x0) / (x1 - x0)
            }
        }
        return curve.last().second
    }

    private fun integrateMinutes(model: EvModel, from: Double, to: Double, scale: Double, chargerKw: Double): Double {
        if (to <= from) return 0.0
        val steps = max(1, ((to - from) * 200).toInt())
        val ds = (to - from) / steps
        var hours = 0.0
        for (i in 0 until steps) {
            val s = from + ds * (i + 0.5)
            val carKw = model.maxDcKw * curveAt(s) * scale
            val kw = max(1.0, min(carKw, chargerKw * 0.95))
            hours += ds * model.batteryKwh / DC_EFFICIENCY / kw
        }
        return hours * 60.0
    }

    fun dcMinutes(model: EvModel, from: Double, to: Double, chargerKw: Double): Double {
        // Find the scale so that an unlimited charger reproduces the official 10→80 % time.
        val unscaled = integrateMinutes(model, 0.10, 0.80, 1.0, Double.MAX_VALUE)
        val scale = unscaled / model.dc10to80Min
        return integrateMinutes(model, from, to, scale, chargerKw)
    }

    fun planTrip(
        model: EvModel,
        distanceKm: Double,
        startSoc: Double,
        reserveSoc: Double,
        stopTargetSoc: Double,
        dcChargerKw: Double,
        style: DrivingStyle,
        homeRate: Double,
        dcRate: Double,
        petrolPrice: Double,
        petrolLPer100: Double,
        destinationLabel: String,
    ): TripPlan {
        val cap = model.batteryKwh
        val kwhPerKm = kwhPer100(model, style) / 100.0
        val socPerKm = kwhPerKm / cap
        val legs = mutableListOf<TripLeg>()
        var soc = startSoc
        var pos = 0.0
        var stops = 0
        var dcBatteryKwh = 0.0
        var chargingCost = 0.0
        var chargingMinutes = 0.0
        var feasible = stopTargetSoc > reserveSoc + 0.05 && startSoc > 0.0

        while (feasible) {
            val remaining = distanceKm - pos
            val socNeeded = remaining * socPerKm + reserveSoc
            if (soc >= socNeeded - 1e-9) {
                legs += TripLeg.Drive(remaining, remaining / style.avgSpeedKmh * 60, soc, soc - remaining * socPerKm, destinationLabel)
                soc -= remaining * socPerKm
                pos = distanceKm
                break
            }
            if (stops >= 25) { feasible = false; break }
            // Drive until the reserve, then charge.
            val drive = max(0.0, (soc - reserveSoc) / socPerKm)
            if (drive > 0.5) {
                legs += TripLeg.Drive(drive, drive / style.avgSpeedKmh * 60, soc, reserveSoc, "Charging stop ${stops + 1}")
                pos += drive
                soc = reserveSoc
            }
            val stillNeeded = (distanceKm - pos) * socPerKm + reserveSoc
            // Only charge as much as needed on the last stop (rounded up to the next whole %).
            val target = if (stillNeeded <= stopTargetSoc) min(1.0, ceil(stillNeeded * 100) / 100.0) else stopTargetSoc
            val from = soc
            val added = (target - from) * cap
            val minutes = dcMinutes(model, from, target, dcChargerKw)
            val cost = added / DC_EFFICIENCY * dcRate
            legs += TripLeg.Stop(pos, from, target, added, minutes, cost)
            dcBatteryKwh += added
            chargingCost += cost
            chargingMinutes += minutes
            soc = target
            stops++
        }

        val totalEnergy = distanceKm * kwhPerKm
        val homePortion = max(0.0, totalEnergy - dcBatteryKwh)
        val homeCost = homePortion / AC_EFFICIENCY * homeRate
        val drivingMinutes = legs.filterIsInstance<TripLeg.Drive>().sumOf { it.minutes }
        val noStopStart = reserveSoc + distanceKm * socPerKm

        return TripPlan(
            distanceKm = distanceKm,
            legs = legs,
            stops = stops,
            totalEnergyKwh = totalEnergy,
            homeEnergyCost = homeCost,
            chargingCost = chargingCost,
            chargingMinutes = chargingMinutes,
            drivingMinutes = drivingMinutes,
            arrivalSoc = soc,
            noStopStartSoc = if (noStopStart <= 1.0) noStopStart else null,
            petrolCost = distanceKm * petrolLPer100 / 100.0 * petrolPrice,
            feasible = feasible,
        )
    }
}
