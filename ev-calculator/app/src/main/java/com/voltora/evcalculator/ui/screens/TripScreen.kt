package com.voltora.evcalculator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltora.evcalculator.AppState
import com.voltora.evcalculator.ads.NativeAdCard
import com.voltora.evcalculator.domain.DrivingStyle
import com.voltora.evcalculator.domain.EvMath
import com.voltora.evcalculator.domain.Formatter
import com.voltora.evcalculator.domain.TripLeg
import com.voltora.evcalculator.domain.TripPlan
import com.voltora.evcalculator.ui.components.BigText
import com.voltora.evcalculator.ui.components.MistCard
import com.voltora.evcalculator.ui.components.Pill
import com.voltora.evcalculator.ui.components.SectionLabel
import com.voltora.evcalculator.ui.components.TopBlock
import com.voltora.evcalculator.ui.components.VoltButton
import com.voltora.evcalculator.ui.components.VoltSlider
import com.voltora.evcalculator.ui.components.darkFieldColors
import com.voltora.evcalculator.ui.theme.Body
import com.voltora.evcalculator.ui.theme.Ev
import kotlin.math.roundToInt

@Composable
fun TripScreen(
    state: AppState,
    onPlanRequested: (onReady: () -> Unit) -> Unit,
) {
    val fmt = state.fmt
    val focus = LocalFocusManager.current
    var from by rememberSaveable { mutableStateOf("") }
    var to by rememberSaveable { mutableStateOf("") }
    var distanceText by rememberSaveable { mutableStateOf("") }
    var startSoc by rememberSaveable { mutableStateOf(0.9f) }
    var reserve by rememberSaveable { mutableStateOf(0.15f) }
    var stopTarget by rememberSaveable { mutableStateOf(0.8f) }
    var style by rememberSaveable { mutableStateOf(DrivingStyle.HIGHWAY) }
    var dcKw by rememberSaveable { mutableStateOf(150.0) }
    var planned by rememberSaveable { mutableStateOf(false) }

    val distanceValue = distanceText.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 && it < 20000 }
    val distanceKm = distanceValue?.let { fmt.toKm(it) }
    val plan: TripPlan? = if (planned && distanceKm != null) {
        EvMath.planTrip(
            model = state.model, distanceKm = distanceKm,
            startSoc = startSoc.toDouble(), reserveSoc = reserve.toDouble(), stopTargetSoc = stopTarget.toDouble(),
            dcChargerKw = dcKw, style = style, homeRate = state.homeRate, dcRate = state.dcRate,
            petrolPrice = state.petrolPrice, petrolLPer100 = state.prefs.petrolLPer100,
            destinationLabel = to.ifBlank { "Destination" },
        )
    } else null

    Column(
        Modifier
            .fillMaxSize()
            .background(Ev.Paper)
            .verticalScroll(rememberScrollState()),
    ) {
        TopBlock {
            BigText("PLAN A TRIP", 40.sp, Ev.Paper, weight = FontWeight.ExtraBold)
            Column(
                Modifier.clip(RoundedCornerShape(18.dp)).background(Ev.Ink2),
            ) {
                TextField(
                    value = from, onValueChange = { from = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("From (optional)") }, colors = darkFieldColors(),
                )
                TextField(
                    value = to, onValueChange = { to = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("To (optional)") }, colors = darkFieldColors(),
                )
            }
            TextField(
                value = distanceText,
                onValueChange = { v -> distanceText = v.filter { it.isDigit() || it == '.' || it == ',' }.take(6) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Trip distance (${fmt.distanceUnit})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(14.dp),
                colors = darkFieldColors(),
            )
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(distanceKm?.let { fmt.distance(it) } ?: "— ${fmt.distanceUnit}", true, true, null)
                Pill("Leave at ${fmt.percent(startSoc.toDouble())}", false, true, null)
                Pill("Arrive ≥ ${fmt.percent(reserve.toDouble())}", false, true, null)
            }
        }

        Column(
            Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SliderBlock("Battery when you leave", fmt.percent(startSoc.toDouble())) {
                VoltSlider(startSoc, { startSoc = (it * 100).roundToInt() / 100f }, valueRange = 0.05f..1f)
            }
            SliderBlock("Never go below (stops & arrival)", fmt.percent(reserve.toDouble())) {
                VoltSlider(reserve, { reserve = (it * 100).roundToInt() / 100f }, valueRange = 0.05f..0.3f)
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Driving")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DrivingStyle.entries.forEach { s -> Pill(s.label, s == style, false, { style = s }) }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Charge to at stops")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0.7f, 0.8f, 0.9f).forEach { t -> Pill(fmt.percent(t.toDouble()), t == stopTarget, false, { stopTarget = t }) }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Fast chargers on route")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(50.0, 100.0, 150.0, 250.0, 350.0).forEach { kw -> Pill(fmt.power(kw), kw == dcKw, false, { dcKw = kw }) }
                }
            }

            if (!planned || plan == null) {
                VoltButton(
                    if (distanceKm == null) "Enter a distance to plan" else "Plan my trip",
                    onClick = {
                        if (distanceKm != null) {
                            focus.clearFocus()
                            onPlanRequested { planned = true }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    dark = distanceKm == null,
                )
            }

            if (plan != null) {
                Advice(state, plan, startSoc.toDouble())
                Timeline(state, plan, from.ifBlank { "Start" })
                NativeAdCard(showMedia = true)
            }
        }

        if (plan != null) Summary(state, plan)
    }
}

@Composable
private fun SliderBlock(label: String, value: String, slider: @Composable () -> Unit) {
    Column {
        Row {
            SectionLabel(label, Modifier.weight(1f))
            Text(value, fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ev.Ink)
        }
        slider()
    }
}

@Composable
private fun Advice(state: AppState, plan: TripPlan, startSoc: Double) {
    val fmt = state.fmt
    val need = plan.noStopStartSoc
    MistCard {
        SectionLabel("How much to charge before you leave")
        when {
            !plan.feasible -> Text(
                "This trip can't be planned with these settings. Raise the stop charge level or lower the minimum.",
                fontFamily = Body, fontSize = 14.sp, color = Ev.Warn,
            )
            need != null && need <= startSoc -> {
                BigText("YOU'RE GOOD TO GO", 26.sp)
                Text(
                    "Leaving at ${fmt.percent(startSoc)} you arrive with ${fmt.percent(plan.arrivalSoc)}. " +
                        "You only need ${fmt.percent(need)} to make it without stopping.",
                    fontFamily = Body, fontSize = 14.sp, color = Ev.Ink,
                )
            }
            need != null -> {
                val home = EvMath.charge(
                    state.model, startSoc, need, com.voltora.evcalculator.domain.ChargeLocation.HOME,
                    state.prefs.homeChargerKw, state.homeRate,
                )
                BigText("CHARGE TO ${fmt.percent(need)}", 26.sp)
                Text(
                    "Charge to ${fmt.percent(need)} at home and you can skip all ${plan.stops} stop(s). " +
                        "That's ${fmt.money(home.cost)} more at home (${Formatter.duration(home.minutes)} on your ${fmt.power(state.prefs.homeChargerKw)} charger).",
                    fontFamily = Body, fontSize = 14.sp, color = Ev.Ink,
                )
            }
            else -> {
                BigText("CHARGE TO 100%", 26.sp)
                Text(
                    "This trip is longer than a full battery. Leave full and plan ${plan.stops} fast-charging stop(s).",
                    fontFamily = Body, fontSize = 14.sp, color = Ev.Ink,
                )
            }
        }
    }
}

@Composable
private fun Timeline(state: AppState, plan: TripPlan, startLabel: String) {
    val fmt = state.fmt
    Column {
        SectionLabel(
            "Your route · ${Formatter.duration(plan.drivingMinutes + plan.chargingMinutes)}",
            Modifier.padding(bottom = 14.dp),
        )
        TimelinePoint(startLabel, fmt.percent(plan.legs.firstOrNull()?.let {
            when (it) { is TripLeg.Drive -> it.fromSoc; is TripLeg.Stop -> it.fromSoc }
        } ?: 0.0), filled = true)
        plan.legs.forEach { leg ->
            when (leg) {
                is TripLeg.Drive -> {
                    TimelineLine("Drive ${fmt.distance(leg.km)} · ${Formatter.duration(leg.minutes)}")
                    if (!leg.endLabel.startsWith("Charging stop")) {
                        TimelinePoint(leg.endLabel, fmt.percent(leg.toSoc), filled = true, square = true)
                    }
                }
                is TripLeg.Stop -> StopCard(state, leg)
            }
        }
    }
}

@Composable
private fun TimelinePoint(label: String, soc: String, filled: Boolean, square: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(14.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(12.dp)
                    .clip(if (square) RoundedCornerShape(2.dp) else CircleShape)
                    .background(if (filled) Ev.Ink else Ev.Paper),
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(label, Modifier.weight(1f), fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ev.Ink)
        Text(soc, fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Ev.Ink)
    }
}

@Composable
private fun TimelineLine(text: String) {
    Row(Modifier.height(IntrinsicSize.Min)) {
        Box(Modifier.width(14.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
            Box(Modifier.width(2.dp).fillMaxHeight().background(Ev.Ink))
        }
        Spacer(Modifier.width(14.dp))
        Text(text, Modifier.padding(vertical = 12.dp), fontFamily = Body, fontSize = 13.sp, color = Ev.Muted)
    }
}

@Composable
private fun StopCard(state: AppState, s: TripLeg.Stop) {
    val fmt = state.fmt
    Row(Modifier.height(IntrinsicSize.Min)) {
        Box(Modifier.width(14.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
            Box(Modifier.width(2.dp).fillMaxHeight().background(Ev.Ink))
            Box(
                Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Ev.Volt)
                    .border(2.dp, Ev.Ink, CircleShape),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(
            Modifier
                .weight(1f)
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Ev.Mist)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Fast charge at ${fmt.distance(s.atKm)}",
                    Modifier.weight(1f), fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ev.Ink,
                )
                BigText(fmt.money(s.cost), 22.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SmallTag("${fmt.percent(s.fromSoc)} → ${fmt.percent(s.toSoc)}", dark = true)
                SmallTag(Formatter.duration(s.minutes))
                SmallTag(fmt.kwh(s.kwh))
            }
        }
    }
}

@Composable
private fun SmallTag(text: String, dark: Boolean = false) {
    Text(
        text,
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (dark) Ev.Ink else Ev.Paper)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        fontFamily = Body, fontWeight = if (dark) FontWeight.Bold else FontWeight.Medium, fontSize = 12.sp,
        color = if (dark) Ev.Paper else Ev.Ink,
    )
}

@Composable
private fun Summary(state: AppState, plan: TripPlan) {
    val fmt = state.fmt
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(Ev.Ink)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryStat("Trip cost", fmt.money(plan.totalCost), Ev.Volt, Modifier.weight(1f))
            SummaryStat(fmt.per100Label(), fmt.per100(plan.costPer100Km), Ev.Paper, Modifier.weight(1f))
            SummaryStat("Charging", Formatter.duration(plan.chargingMinutes), Ev.Paper, Modifier.weight(1f))
        }
        Text(
            "Energy ${fmt.kwh(plan.totalEnergyKwh)} · home ${fmt.money(plan.homeEnergyCost)} + fast charging ${fmt.money(plan.chargingCost)}",
            color = Ev.MutedOnDark, fontFamily = Body, fontSize = 13.sp,
        )
        val save = plan.savingsVsPetrol
        Text(
            if (save >= 0) "${fmt.money(save)} cheaper than a petrol car (${Formatter.number(state.prefs.petrolLPer100, 1)} L/100 km at ${fmt.rate(state.petrolPrice)}/L)"
            else "${fmt.money(-save)} more than a petrol car (${Formatter.number(state.prefs.petrolLPer100, 1)} L/100 km at ${fmt.rate(state.petrolPrice)}/L)",
            color = Ev.Paper, fontFamily = Body, fontWeight = FontWeight.Medium, fontSize = 13.sp,
        )
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun SummaryStat(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = Ev.MutedOnDark, fontFamily = Body, fontSize = 12.sp)
        BigText(value, 28.sp, color)
    }
}
