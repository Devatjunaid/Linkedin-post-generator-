package com.devatjunaid.balconysolar.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.devatjunaid.balconysolar.domain.Fmt
import com.devatjunaid.balconysolar.domain.Results
import com.devatjunaid.balconysolar.domain.SetupState
import com.devatjunaid.balconysolar.engine.FinanceInputs
import com.devatjunaid.balconysolar.ui.components.AppIcons
import com.devatjunaid.balconysolar.ui.components.CardLabel
import com.devatjunaid.balconysolar.ui.components.GhostButton
import com.devatjunaid.balconysolar.ui.components.NumberEdit
import com.devatjunaid.balconysolar.ui.components.NumberEditDialog
import com.devatjunaid.balconysolar.ui.components.PrimaryButton
import com.devatjunaid.balconysolar.ui.components.ScreenHeader
import com.devatjunaid.balconysolar.ui.components.SectionCard
import com.devatjunaid.balconysolar.ui.components.StatTile
import com.devatjunaid.balconysolar.ui.components.ValueRow
import com.devatjunaid.balconysolar.ui.graphics.CumulativeChart
import com.devatjunaid.balconysolar.ui.theme.Palette
import com.devatjunaid.balconysolar.ui.theme.Type
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PaybackScreen(
    state: SetupState,
    results: Results?,
    onState: (SetupState) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    nativeAd: @Composable () -> Unit,
) {
    var edit by remember { mutableStateOf<NumberEdit?>(null) }
    val f = state.finance
    val cur = f.currencyCode
    fun set(n: FinanceInputs) = onState(state.copy(finance = n))
    ScreenColumn {
        ScreenHeader(step = 5, title = "Payback", onBack = onBack)
        if (results == null) {
            Loading()
            return@ScreenColumn
        }
        val r = results.finance

        // Hero card.
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Palette.Amber)) {
            Canvas(Modifier.size(150.dp).align(Alignment.TopEnd).offset(x = 36.dp, y = (-40).dp)) {
                val c = center
                drawCircle(Palette.AmberSoft, size.minDimension * 0.2f, c)
                for (i in 0 until 8) {
                    val a = i * Math.PI / 4
                    val r0 = size.minDimension * 0.28f
                    val r1 = size.minDimension * 0.42f
                    drawLine(
                        Palette.AmberSoft, Offset(c.x + r0 * cos(a).toFloat(), c.y + r0 * sin(a).toFloat()),
                        Offset(c.x + r1 * cos(a).toFloat(), c.y + r1 * sin(a).toFloat()), 12f, StrokeCap.Round,
                    )
                }
            }
            Column(Modifier.padding(18.dp)) {
                Text("Pays for itself in", style = Type.labelStrong, color = Palette.OnAmber)
                if (r.paybackYears != null) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(Fmt.number(r.paybackYears, 1), style = Type.hero, color = Palette.OnAmber)
                        Text(" years", style = Type.title, color = Palette.OnAmber, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    Text(
                        "${Fmt.years(r.paybackYears)} · break-even around ${Fmt.paybackDate(r.paybackYears)} · saves ${Fmt.money(r.firstYearSavings, cur)} in year one",
                        style = Type.label.copy(fontSize = Type.labelStrong.fontSize), color = Palette.OnAmber,
                    )
                } else {
                    Text("Not within ${f.lifetimeYears} years", style = Type.title, color = Palette.OnAmber, modifier = Modifier.padding(vertical = 8.dp))
                    Text("Savings of ${Fmt.money(r.firstYearSavings, cur)} a year don't cover the cost. Check price, direction and shading.", style = Type.label, color = Palette.OnAmber)
                }
            }
        }

        SectionCard(padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
            CardLabel("Cumulative savings, ${f.lifetimeYears} years", trailing = "${if (r.netGain >= 0) "+" else ""}${Fmt.money(r.netGain, cur)} net")
            Spacer(Modifier.height(6.dp))
            CumulativeChart(
                r.cumulative, r.paybackYears, { compactMoney(it, cur) }, Modifier.fillMaxWidth().height(170.dp),
                description = "Savings over ${f.lifetimeYears} years reach ${Fmt.money(r.lifetimeSavings, cur)}",
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Return over ${f.lifetimeYears} yrs", "${Fmt.number(r.roiMultiple, 1)}× cost", Modifier.weight(1f))
            StatTile("CO₂ avoided", "${Fmt.number(r.co2KgPerYear)} kg/yr", Modifier.weight(1f), Palette.Mint)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Your solar kWh costs", Fmt.unitPrice(r.lcoe, cur), Modifier.weight(1f))
            StatTile(if (f.discountRatePct > 0) "Net present value" else "Lifetime savings", Fmt.money(if (f.discountRatePct > 0) r.npv else r.lifetimeSavings, cur), Modifier.weight(1f))
        }

        nativeAd()

        SectionCard(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
            ValueRow("Kit price incl. delivery", Fmt.money(f.systemCost, cur), onClick = {
                edit = NumberEdit("System cost", Fmt.currencySymbol(cur), f.systemCost, 0.0, 1e9, 0, "Everything you pay: panels, inverter, mount, cable, installation.") { set(f.copy(systemCost = it)) }
            })
            ValueRow("Electricity price", "${Fmt.unitPrice(f.pricePerKwh, cur)} /kWh", hint = "Typical for ${state.country.name}; use your bill", onClick = {
                edit = NumberEdit("Electricity price", "${Fmt.currencySymbol(cur)}/kWh", f.pricePerKwh, 0.0, 1e6, if (f.pricePerKwh >= 10) 2 else 3, "Working price incl. taxes from your bill (not the standing charge).") { set(f.copy(pricePerKwh = it)) }
            })
            ValueRow("Price change per year", "${Fmt.number(f.priceChangePct, 1)} %", onClick = {
                edit = NumberEdit("Electricity price change", "% / yr", f.priceChangePct, -10.0, 20.0, 1) { set(f.copy(priceChangePct = it)) }
            })
            ValueRow("Feed-in payment", if (f.feedInPerKwh > 0) "${Fmt.unitPrice(f.feedInPerKwh, cur)} /kWh" else "None", onClick = {
                edit = NumberEdit("Feed-in tariff", "${Fmt.currencySymbol(cur)}/kWh", f.feedInPerKwh, 0.0, 1e6, 3, "Most plug-in systems are not paid for exported power.") { set(f.copy(feedInPerKwh = it)) }
            })
            ValueRow("Panel ageing per year", "${Fmt.number(f.degradationPct, 1)} %", onClick = {
                edit = NumberEdit("Degradation", "% / yr", f.degradationPct, 0.0, 3.0, 2, "Datasheet warranty is usually 0.4–0.55 % per year.") { set(f.copy(degradationPct = it)) }
            })
            ValueRow("Lifetime", "${f.lifetimeYears} years", onClick = {
                edit = NumberEdit("System lifetime", "years", f.lifetimeYears.toDouble(), 1.0, 40.0, 0) { set(f.copy(lifetimeYears = it.toInt())) }
            })
            ValueRow("Discount rate", "${Fmt.number(f.discountRatePct, 1)} %", hint = "0 % = simple payback", onClick = {
                edit = NumberEdit("Discount rate", "%", f.discountRatePct, 0.0, 20.0, 1, "Return you could earn elsewhere; used for net present value and solar kWh cost.") { set(f.copy(discountRatePct = it)) }
            })
            ValueRow("Grid CO₂ intensity", "${Fmt.number(f.co2GramsPerKwh)} g/kWh", showDivider = false, onClick = {
                edit = NumberEdit("Grid emissions", "g CO₂/kWh", f.co2GramsPerKwh, 0.0, 1500.0, 0) { set(f.copy(co2GramsPerKwh = it)) }
            })
        }

        GhostButton("Share report", onClick = onShare, icon = AppIcons.Share, modifier = Modifier.fillMaxWidth(), color = Palette.Ink)
        Text(
            "Estimates for planning only, based on long-term average weather. Check local rules before connecting a plug-in system.",
            style = Type.label, color = Palette.Faint,
        )
        PrimaryButton("Save this setup", onClick = onSave, icon = AppIcons.Save)
    }
    edit?.let { NumberEditDialog(it) { edit = null } }
}

private fun compactMoney(v: Double, cur: String): String {
    val a = kotlin.math.abs(v)
    return when {
        a >= 1_000_000 -> "${Fmt.number(v / 1_000_000, 1)}M"
        a >= 10_000 -> "${Fmt.number(v / 1000)}k"
        a >= 1000 -> "${Fmt.number(v / 1000, 1)}k"
        else -> Fmt.number(v)
    } + " " + Fmt.currencySymbol(cur)
}
