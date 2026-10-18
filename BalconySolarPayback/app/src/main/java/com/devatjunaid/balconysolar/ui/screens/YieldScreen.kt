package com.devatjunaid.balconysolar.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.devatjunaid.balconysolar.domain.Fmt
import com.devatjunaid.balconysolar.domain.Results
import com.devatjunaid.balconysolar.domain.SetupState
import com.devatjunaid.balconysolar.engine.LoadProfile
import com.devatjunaid.balconysolar.ui.components.AppIcons
import com.devatjunaid.balconysolar.ui.components.CardLabel
import com.devatjunaid.balconysolar.ui.components.Chip
import com.devatjunaid.balconysolar.ui.components.NumberEdit
import com.devatjunaid.balconysolar.ui.components.NumberEditDialog
import com.devatjunaid.balconysolar.ui.components.PrimaryButton
import com.devatjunaid.balconysolar.ui.components.ScreenHeader
import com.devatjunaid.balconysolar.ui.components.SectionCard
import com.devatjunaid.balconysolar.ui.components.ValueRow
import com.devatjunaid.balconysolar.ui.graphics.MonthlyBars
import com.devatjunaid.balconysolar.ui.graphics.RingGauge
import com.devatjunaid.balconysolar.ui.theme.Palette
import com.devatjunaid.balconysolar.ui.theme.Type

@Composable
fun YieldScreen(
    state: SetupState,
    results: Results?,
    onState: (SetupState) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    nativeAd: @Composable () -> Unit,
) {
    var edit by remember { mutableStateOf<NumberEdit?>(null) }
    var month by rememberSaveable { mutableIntStateOf(5) }
    ScreenColumn {
        ScreenHeader(step = 4, title = "Annual yield", onBack = onBack)
        if (results == null) {
            Loading()
            return@ScreenColumn
        }
        val y = results.yield
        val c = results.consumption
        Column {
            Text("Expected per year at your socket", style = Type.label.copy(fontSize = Type.labelStrong.fontSize), color = Palette.Muted)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(Fmt.number(y.annualKwh), style = Type.hero, color = Palette.Ink)
                Text(" kWh", style = Type.title, color = Palette.Ink, modifier = Modifier.padding(bottom = 8.dp))
            }
            Text(
                "${Fmt.number(y.specificYield)} kWh per kWp · ${Fmt.number(y.poaAnnualKwhM2)} kWh/m² reach the panel",
                style = Type.label.copy(fontSize = Type.labelStrong.fontSize), color = Palette.Muted,
            )
        }

        SectionCard(padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
            CardLabel("Monthly, kWh", trailing = "${Fmt.MONTHS[month]} · ${Fmt.number(y.monthlyKwh[month])} kWh", trailingColor = Palette.Amber)
            Spacer(Modifier.height(8.dp))
            MonthlyBars(
                y.monthlyKwh, month, { month = it }, Modifier.fillMaxWidth().height(150.dp),
                description = "Monthly yield: " + y.monthlyKwh.mapIndexed { i, v -> "${Fmt.MONTHS[i]} ${Fmt.number(v)}" }.joinToString(", "),
            )
        }

        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
                    RingGauge(c.selfConsumptionPct / 100, Modifier.size(84.dp))
                    Text("${Fmt.number(c.selfConsumptionPct)}%", style = Type.stat, color = Palette.Ink)
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("You use ${Fmt.kwh(c.selfUsedKwh)} yourself", style = Type.bodyMedium, color = Palette.Ink)
                    Text(
                        "${Fmt.kwh(c.exportedKwh)} flow to the grid" +
                            (if (state.finance.feedInPerKwh > 0) " and earn feed-in." else " unpaid.") +
                            " Covers ${Fmt.number(c.autarkyPct)} % of your home's use." +
                            (if (c.batteryDeliveredKwh > 0) " Battery shifts ${Fmt.kwh(c.batteryDeliveredKwh)} to the evening." else ""),
                        style = Type.label, color = Palette.Muted,
                    )
                }
            }
        }

        SectionCard(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
            CardLabel("Your household")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (p in LoadProfile.entries) {
                    Chip(p.label, state.household.profile == p, { onState(state.copy(household = state.household.copy(profile = p))) }, accent = Palette.Mint, onAccent = Palette.OnMint)
                }
            }
            ValueRow("Electricity use per year", Fmt.kwh(state.household.annualKwh), onClick = {
                edit = NumberEdit("Annual electricity use", "kWh", state.household.annualKwh, 200.0, 50_000.0, 0, "From your electricity bill.") {
                    onState(state.copy(household = state.household.copy(annualKwh = it)))
                }
            })
            ValueRow("Battery", if (state.household.batteryKwh > 0) "${Fmt.number(state.household.batteryKwh, 2)} kWh" else "None", onClick = {
                edit = NumberEdit("Battery capacity", "kWh", state.household.batteryKwh, 0.0, 20.0, 2, "Usable share assumed 90 %, 95 % efficiency each way, 800 W max.") {
                    onState(state.copy(household = state.household.copy(batteryKwh = it)))
                }
            })
            val manual = state.household.selfConsumptionOverridePct
            Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Set self-use share manually", style = Type.body, color = Palette.Ink)
                    Text(if (manual != null) "${Fmt.number(manual)} % (tap value to change)" else "Calculated from hourly profiles", style = Type.label, color = Palette.Muted)
                }
                Switch(
                    checked = manual != null,
                    onCheckedChange = { on ->
                        onState(state.copy(household = state.household.copy(selfConsumptionOverridePct = if (on) c.selfConsumptionPct.coerceIn(0.0, 100.0) else null)))
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = Palette.OnMint, checkedTrackColor = Palette.Mint),
                )
            }
            if (manual != null) {
                ValueRow("Self-use share", "${Fmt.number(manual)} %", showDivider = false, onClick = {
                    edit = NumberEdit("Share used at home", "%", manual, 0.0, 100.0, 0) {
                        onState(state.copy(household = state.household.copy(selfConsumptionOverridePct = it)))
                    }
                })
            }
        }

        nativeAd()

        SectionCard {
            CardLabel("From sky to socket")
            Spacer(Modifier.height(6.dp))
            LossRow("Sunlight on the panels at full efficiency", Fmt.kwh(y.nominalKwh), strong = false, color = Palette.Ink)
            for (l in y.losses) {
                if (kotlin.math.abs(l.lossKwh) < 0.05) continue
                val gain = l.lossKwh < 0
                LossRow(l.label, "${if (gain) "+" else "−"}${Fmt.number(kotlin.math.abs(l.lossPct), 1)} %", strong = false, color = if (gain) Palette.Mint else Palette.Coral)
            }
            LossRow("At your socket", Fmt.kwh(y.annualKwh), strong = true, color = Palette.Amber)
        }
        Text(
            "Climate: ${results.site.climate.source}. Year-to-year weather typically moves the result by ±5–8 %.",
            style = Type.label, color = Palette.Faint, textAlign = TextAlign.Start,
        )
        PrimaryButton("See payback", onClick = onNext, icon = AppIcons.Coin)
    }
    edit?.let { NumberEditDialog(it) { edit = null } }
}

@Composable
private fun LossRow(label: String, value: String, strong: Boolean, color: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth().heightIn(min = 30.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = if (strong) Type.labelStrong else Type.label.copy(fontSize = Type.labelStrong.fontSize), color = if (strong) Palette.Ink else Palette.Muted, modifier = Modifier.weight(1f))
        Text(value, style = Type.mono.copy(fontSize = Type.labelStrong.fontSize), color = color)
    }
}

@Composable
fun Loading() {
    Row(Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(color = Palette.Amber)
        Spacer(Modifier.width(14.dp))
        Text("Simulating every 15 minutes of the year…", style = Type.body, color = Palette.Muted)
    }
}
