package com.voltora.evcalculator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltora.evcalculator.AppState
import com.voltora.evcalculator.ads.NativeAdCard
import com.voltora.evcalculator.domain.ChargeLocation
import com.voltora.evcalculator.domain.DrivingStyle
import com.voltora.evcalculator.domain.EvMath
import com.voltora.evcalculator.domain.Formatter
import com.voltora.evcalculator.ui.components.AnimatedLogo
import com.voltora.evcalculator.ui.components.BigText
import com.voltora.evcalculator.ui.components.MistCard
import com.voltora.evcalculator.ui.components.RateTile
import com.voltora.evcalculator.ui.components.SectionLabel
import com.voltora.evcalculator.ui.components.TopBlock
import com.voltora.evcalculator.ui.components.VoltSlider
import com.voltora.evcalculator.ui.theme.Body
import com.voltora.evcalculator.ui.theme.Ev
import java.util.Calendar
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    state: AppState,
    onSocChange: (Float) -> Unit,
    onPickCountry: () -> Unit,
    onOpenVehicle: () -> Unit,
    onOpenCharge: () -> Unit,
    onOpenTrip: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val model = state.model
    val fmt = state.fmt
    var soc by remember { mutableFloatStateOf(state.prefs.currentSoc) }
    LaunchedEffect(state.prefs.currentSoc) { soc = state.prefs.currentSoc }

    val range = EvMath.rangeKm(model, soc.toDouble())
    val target = 0.8
    val tonight = EvMath.charge(
        model, soc.toDouble(), target, ChargeLocation.HOME, state.prefs.homeChargerKw, state.homeRate,
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(Ev.Paper)
            .verticalScroll(rememberScrollState()),
    ) {
        TopBlock {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(greeting(), color = Ev.MutedOnDark, fontFamily = Body, fontSize = 14.sp)
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(role = Role.Button, onClick = onPickCountry)
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${state.country.flag}  ${state.country.name}", color = Ev.Paper, fontFamily = Body, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Change country", tint = Ev.Paper)
                    }
                }
                AnimatedLogo(size = 40.dp)
                Spacer(Modifier.width(6.dp))
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(Ev.Ink2),
                ) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = Ev.Paper)
                }
            }

            Column(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClick = onOpenVehicle),
            ) {
                SectionLabel(model.brand, color = Ev.MutedOnDark)
                BigText(model.name.uppercase(), 36.sp, Ev.Paper)
            }

            Row(verticalAlignment = Alignment.Bottom) {
                BigText("${(soc * 100).roundToInt()}%", 100.sp, Ev.Volt, Modifier.weight(1f), FontWeight.ExtraBold)
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(bottom = 8.dp)) {
                    Text(fmt.distance(range), color = Ev.Paper, fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text(
                        "${Formatter.number(model.batteryKwh * soc, 1)} of ${Formatter.number(model.batteryKwh, 1)} kWh",
                        color = Ev.MutedOnDark, fontFamily = Body, fontSize = 13.sp,
                    )
                }
            }
            Column {
                VoltSlider(
                    value = soc,
                    onValueChange = { soc = it },
                    onValueChangeFinished = { onSocChange(soc) },
                    onDark = true,
                )
                Text("Drag to set your current charge", color = Ev.MutedOnDark, fontFamily = Body, fontSize = 12.sp)
            }
        }

        Column(
            Modifier.padding(horizontal = 18.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Tonight at home")
                MistCard(onClick = onOpenCharge) {
                    if (soc >= target - 0.005) {
                        Text("You're above 80%", fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Ev.Ink)
                        Text("No charge needed for daily driving.", fontFamily = Body, fontSize = 13.sp, color = Ev.Muted)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Charge to 80%", fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Ev.Ink)
                                Text(
                                    "${fmt.kwh(tonight.energyBilledKwh)} · ${fmt.power(tonight.effectivePowerKw)} · ${Formatter.duration(tonight.minutes)}",
                                    fontFamily = Body, fontSize = 13.sp, color = Ev.Muted,
                                )
                            }
                            BigText(fmt.money(tonight.cost), 32.sp)
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("${state.country.name} · per kWh", Modifier.weight(1f))
                    Text(
                        if (state.hasCustomRates) "Custom · Edit" else "Edit",
                        fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Ev.Ink,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onOpenSettings).padding(6.dp),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RateTile("Home", fmt.rate(state.homeRate), true, Modifier.weight(1f))
                    RateTile("Public AC", fmt.rate(state.acRate), false, Modifier.weight(1f))
                    RateTile("DC fast", fmt.rate(state.dcRate), false, Modifier.weight(1f))
                }
            }

            NativeAdCard()

            CostPer100Card(state)
        }

        // Bottom block: trip CTA
        Row(
            Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 18.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Ev.Volt)
                .clickable(role = Role.Button, onClick = onOpenTrip)
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionLabel("Road trip?", color = Ev.Ink)
                BigText("PLAN A TRIP", 30.sp)
                Text(
                    "How much to charge, where to stop and what it costs",
                    fontFamily = Body, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Ev.Ink,
                )
            }
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(Ev.Ink),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Ev.Paper)
            }
        }
    }
}

@Composable
private fun CostPer100Card(state: AppState) {
    val fmt = state.fmt
    val kwh100 = EvMath.kwhPer100(state.model, DrivingStyle.MIXED)
    val home = kwh100 / EvMath.AC_EFFICIENCY * state.homeRate
    val dc = kwh100 / EvMath.DC_EFFICIENCY * state.dcRate
    val petrol = state.prefs.petrolLPer100 * state.petrolPrice
    val max = maxOf(home, dc, petrol).coerceAtLeast(0.0001)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("Cost ${fmt.per100Label().lowercase()} · mixed driving")
        MistCard {
            Text(
                "${state.model.name} uses about ${fmt.consumption(kwh100)}",
                fontFamily = Body, fontSize = 13.sp, color = Ev.Muted,
            )
            Spacer(Modifier.height(4.dp))
            CostBar("EV · home", fmt.per100(home), (home / max).toFloat(), Ev.Ink)
            CostBar("EV · DC fast", fmt.per100(dc), (dc / max).toFloat(), Ev.Muted)
            CostBar("Petrol car", fmt.per100(petrol), (petrol / max).toFloat(), Ev.Warn)
        }
    }
}

@Composable
private fun CostBar(label: String, value: String, fraction: Float, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row {
            Text(label, Modifier.weight(1f), fontFamily = Body, fontSize = 13.sp, color = Ev.Ink)
            Text(value, fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Ev.Ink)
        }
        Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Ev.Stroke)) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(5.dp))
                    .background(color),
            )
        }
    }
}

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Good morning"
    in 12..17 -> "Good afternoon"
    else -> "Good evening"
}
