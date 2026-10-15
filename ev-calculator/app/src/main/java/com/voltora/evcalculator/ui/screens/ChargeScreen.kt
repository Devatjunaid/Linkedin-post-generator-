package com.voltora.evcalculator.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltora.evcalculator.AppState
import com.voltora.evcalculator.ads.NativeAdCard
import com.voltora.evcalculator.domain.ChargeLocation
import com.voltora.evcalculator.domain.ChargeResult
import com.voltora.evcalculator.domain.EvMath
import com.voltora.evcalculator.domain.Formatter
import com.voltora.evcalculator.ui.components.BigText
import com.voltora.evcalculator.ui.components.KeyValueRow
import com.voltora.evcalculator.ui.components.Pill
import com.voltora.evcalculator.ui.components.SectionLabel
import com.voltora.evcalculator.ui.components.Tile
import com.voltora.evcalculator.ui.components.TopBlock
import com.voltora.evcalculator.ui.theme.Body
import com.voltora.evcalculator.ui.theme.Ev
import kotlin.math.roundToInt

private fun AppState.rateFor(loc: ChargeLocation) = when (loc) {
    ChargeLocation.HOME -> homeRate
    ChargeLocation.PUBLIC_AC -> acRate
    ChargeLocation.DC_FAST -> dcRate
}

private fun defaultPower(state: AppState, loc: ChargeLocation) = when (loc) {
    ChargeLocation.HOME -> state.prefs.homeChargerKw
    ChargeLocation.PUBLIC_AC -> 22.0
    ChargeLocation.DC_FAST -> if (state.model.maxDcKw >= 200) 250.0 else 150.0
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChargeScreen(state: AppState) {
    val fmt = state.fmt
    val model = state.model
    var from by rememberSaveable { mutableStateOf(state.prefs.currentSoc.coerceAtMost(0.79f)) }
    var to by rememberSaveable { mutableStateOf(0.8f) }
    var location by rememberSaveable { mutableStateOf(ChargeLocation.HOME) }
    var power by rememberSaveable(location) { mutableStateOf(defaultPower(state, location)) }

    val result = EvMath.charge(model, from.toDouble(), to.toDouble(), location, power, state.rateFor(location))

    Column(
        Modifier
            .fillMaxSize()
            .background(Ev.Paper)
            .verticalScroll(rememberScrollState()),
    ) {
        TopBlock {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Cost to charge", Modifier.weight(1f), Ev.MutedOnDark)
                Text(
                    "${model.name} · ${state.country.code}",
                    color = Ev.Paper, fontFamily = Body, fontSize = 13.sp, maxLines = 1,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(Ev.Ink2).padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
            BigText(fmt.money(result.cost), 88.sp, Ev.Volt, weight = FontWeight.ExtraBold)
            Text(
                "${fmt.kwh(result.energyBilledKwh)} billed · includes ~${lossPercent(location)}% charging loss",
                color = Ev.MutedOnDark, fontFamily = Body, fontSize = 14.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip(Icons.Outlined.Schedule, Formatter.duration(result.minutes))
                InfoChip(Icons.Outlined.Bolt, "+${fmt.distance(result.rangeAddedKm)}")
            }
        }

        Column(
            Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row {
                    SectionLabel("Battery", Modifier.weight(1f))
                    Text(
                        "${fmt.percent(from.toDouble())} → ${fmt.percent(to.toDouble())}",
                        fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ev.Ink,
                    )
                }
                RangeSlider(
                    value = from..to,
                    onValueChange = { r ->
                        from = (r.start * 100).roundToInt() / 100f
                        to = (r.endInclusive * 100).roundToInt() / 100f
                    },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = Ev.Ink,
                        activeTrackColor = Ev.Ink,
                        inactiveTrackColor = Ev.Stroke,
                    ),
                )
                Row {
                    Text(
                        "Now ${fmt.percent(from.toDouble())} · ${Formatter.number(model.batteryKwh * from, 1)} kWh",
                        Modifier.weight(1f), fontFamily = Body, fontSize = 13.sp, color = Ev.Muted,
                    )
                    Text(
                        "Target ${fmt.percent(to.toDouble())} · ${Formatter.number(model.batteryKwh * to, 1)} kWh",
                        fontFamily = Body, fontSize = 13.sp, color = Ev.Muted,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Where are you charging?")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChargeLocation.entries.forEach { loc ->
                        Tile(
                            title = loc.label,
                            subtitle = fmt.rate(state.rateFor(loc)) + "/kWh",
                            selected = loc == location,
                            modifier = Modifier.weight(1f),
                            onClick = { location = loc },
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Charger power")
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    location.powerOptions.forEach { kw ->
                        Pill(fmt.power(kw), kw == power, false, { power = kw })
                    }
                }
                if (result.acLimitedByCar) {
                    Text(
                        "Your ${model.name} accepts up to ${fmt.power(model.maxAcKw)} on AC, so it charges at that speed.",
                        fontFamily = Body, fontSize = 13.sp, color = Ev.Warn,
                    )
                }
                if (location.isDc && power > model.maxDcKw) {
                    Text(
                        "Peak DC for this car is ${fmt.power(model.maxDcKw)}; a bigger charger won't make it faster.",
                        fontFamily = Body, fontSize = 13.sp, color = Ev.Muted,
                    )
                }
            }

            Column(Modifier.animateContentSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                KeyValueRow("Energy into battery", fmt.kwh(result.energyToBatteryKwh))
                KeyValueRow("Energy billed", fmt.kwh(result.energyBilledKwh))
                KeyValueRow("${location.label} rate, ${state.country.name}", fmt.rate(state.rateFor(location)) + " / kWh")
                KeyValueRow("Average power", fmt.power((result.effectivePowerKw * 10).roundToInt() / 10.0))
                KeyValueRow("Time", Formatter.duration(result.minutes))
            }

            NativeAdCard()
        }

        CompareBox(state, from.toDouble(), to.toDouble(), location, result)
    }
}

@Composable
private fun CompareBox(state: AppState, from: Double, to: Double, current: ChargeLocation, currentResult: ChargeResult) {
    val fmt = state.fmt
    val others = ChargeLocation.entries.filter { it != current }.map { loc ->
        loc to EvMath.charge(state.model, from, to, loc, defaultPower(state, loc), state.rateFor(loc))
    }
    val all = others + (current to currentResult)
    val cheapest = all.minByOrNull { it.second.cost }!!
    val dearest = all.maxByOrNull { it.second.cost }!!

    Column(
        Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 18.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Ev.Volt)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row {
            SectionLabel("Same charge elsewhere", Modifier.weight(1f), Ev.Ink)
            if (to > from) {
                Text(
                    "${cheapest.first.label} saves ${fmt.money(dearest.second.cost - cheapest.second.cost)}",
                    fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Ev.Ink,
                )
            }
        }
        others.forEach { (loc, r) ->
            Row(
                Modifier.fillMaxWidth().background(Color.Transparent),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${loc.label} · ${fmt.power(if (loc.isDc) defaultPower(state, loc) else minOf(defaultPower(state, loc), state.model.maxAcKw))} · ${Formatter.duration(r.minutes)}",
                    Modifier.weight(1f), fontFamily = Body, fontSize = 14.sp, color = Ev.Ink,
                )
                BigText(fmt.money(r.cost), 24.sp)
            }
        }
    }
}

@Composable
private fun InfoChip(icon: ImageVector, text: String) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Ev.Ink2)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = Ev.Volt, modifier = Modifier.padding(0.dp))
        Text(text, color = Ev.Paper, fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

private fun lossPercent(loc: ChargeLocation) =
    if (loc.isDc) ((1 / EvMath.DC_EFFICIENCY - 1) * 100).roundToInt() else ((1 / EvMath.AC_EFFICIENCY - 1) * 100).roundToInt()
