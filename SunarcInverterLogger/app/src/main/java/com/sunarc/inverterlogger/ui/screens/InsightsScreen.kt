package com.sunarc.inverterlogger.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.WbShade
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sunarc.inverterlogger.ads.NativeAdSlot
import com.sunarc.inverterlogger.ui.AppViewModel
import com.sunarc.inverterlogger.ui.Navigator
import com.sunarc.inverterlogger.ui.Route
import com.sunarc.inverterlogger.ui.components.Fmt
import com.sunarc.inverterlogger.ui.components.HealthRing
import com.sunarc.inverterlogger.ui.components.PaybackTrack
import com.sunarc.inverterlogger.ui.components.ScreenHeader
import com.sunarc.inverterlogger.ui.components.SplitBar
import com.sunarc.inverterlogger.ui.components.StatTile
import com.sunarc.inverterlogger.ui.components.SunCard
import com.sunarc.inverterlogger.ui.theme.DisplayFont
import com.sunarc.inverterlogger.ui.theme.MonoFont
import com.sunarc.inverterlogger.ui.theme.Palette
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun InsightsScreen(vm: AppViewModel, nav: Navigator) {
    val dashboard by vm.dashboard.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val d = dashboard ?: return
    val p = profile ?: return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var making by remember { mutableStateOf(false) }

    ScreenList(Modifier.fillMaxSize()) {
        item { ScreenHeader("Insights") }
        item {
            SunCard(Modifier.fillMaxWidth(), color = Palette.Panel) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HealthRing(d.healthPercent)
                    Column(Modifier.padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(d.healthTitle, fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = Palette.Ink)
                        Text(d.healthNote, fontSize = 13.sp, color = Palette.InkSoft)
                        if (d.healthPercent != null && d.healthPercent < 85) {
                            TextButton(onClick = { nav.tab(Route.FAULTS) }, contentPadding = PaddingValues(0.dp)) {
                                Text("Open the fault checklist", fontSize = 13.sp, color = Palette.Sun, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
        item {
            val monthName = d.month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
            SunCard(Modifier.fillMaxWidth(), onClick = if (!p.pricesSet) ({ nav.push(Route.SETTINGS) }) else null) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$monthName money", fontSize = 13.sp, color = Palette.Ink, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Text(d.monthMoney?.let { Fmt.money(p.currency, it.total) } ?: "Set prices", fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = if (d.monthMoney != null) Palette.Ink else Palette.Sun)
                }
                val m = d.monthMoney
                if (m != null && d.monthStats.totalKwh > 0) {
                    Spacer(Modifier.height(12.dp))
                    SplitBar((m.selfUsedKwh / d.monthStats.totalKwh).toFloat())
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MoneyPart(Palette.Sun, "Used at home", "${Fmt.kwh(m.selfUsedKwh)} kWh × ${Fmt.money(p.currency, p.buyPrice)}", Modifier.weight(1f))
                        MoneyPart(Palette.Sky, p.effectiveTariffModel.label, "${Fmt.kwh(m.exportedKwh)} kWh × ${Fmt.money(p.currency, if (p.effectiveTariffModel.name == "NET_METERING") p.buyPrice else p.exportPrice)}", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(p.effectiveTariffModel.description, fontSize = 11.sp, color = Palette.Muted)
                } else if (!p.pricesSet) {
                    Text("Add your electricity price and export rate to see what your panels earn.", fontSize = 13.sp, color = Palette.InkSoft)
                }
            }
        }
        item {
            SunCard(Modifier.fillMaxWidth(), onClick = if (p.systemCost == null) ({ nav.push(Route.SETTINGS) }) else null) {
                Row {
                    Text("Payback", fontSize = 13.sp, color = Palette.Ink, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Text(
                        when {
                            p.systemCost == null -> "Add system cost"
                            d.savedSoFar == null -> "Add prices"
                            else -> "${Fmt.money(p.currency, d.savedSoFar)} of ${Fmt.money(p.currency, p.systemCost)}"
                        },
                        fontSize = 12.sp, color = if (p.systemCost == null || d.savedSoFar == null) Palette.Sun else Palette.Muted
                    )
                }
                if (d.paybackFraction != null) {
                    Spacer(Modifier.height(8.dp))
                    PaybackTrack(d.paybackFraction)
                    Text(
                        when {
                            d.paybackFraction >= 1f -> "Paid for itself. Everything from here is profit."
                            d.paybackDate != null -> "Break-even around ${d.paybackDate.format(DateTimeFormatter.ofPattern("MMM yyyy"))} at your recent rate"
                            else -> "Scan for a few more days to estimate the break-even date"
                        },
                        fontFamily = MonoFont, fontSize = 11.sp, color = Palette.Muted
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val co2 = d.co2LifetimeKg
                StatTile("CO₂ avoided", co2?.let { if (it >= 1000) Fmt.number(it / 1000, 1) else Fmt.number(it, 0) } ?: "–", if ((co2 ?: 0.0) >= 1000) "t" else "kg", Modifier.weight(1f), Icons.Rounded.Park, Palette.Sky)
                StatTile("Self-use", "${(p.selfUseShare * 100).toInt()}%", null, Modifier.weight(1f), footnote = "set in settings")
                StatTile("Yearly loss", d.degradationPerYear?.let { Fmt.number(it, 1) + "%" } ?: "–", null, Modifier.weight(1f), footnote = if (d.degradationPerYear == null) "needs 2 years" else null)
            }
        }
        item { NativeAdSlot(vm.ads) }
        item {
            SunCard(Modifier.fillMaxWidth(), onClick = if (making) null else ({
                making = true
                scope.launch {
                    val uri = runCatching { vm.report() }.getOrNull()
                    making = false
                    if (uri == null) context.toast("Could not create the report") else
                        vm.ads.interstitials.showAfterAction(context.findActivity()) {
                            context.share(vm.exports.shareIntent(uri, "application/pdf", "Share report"))
                        }
                }
                Unit
            })) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Description, contentDescription = null, tint = Palette.Sun)
                    Text(
                        if (making) "Creating report…" else "${d.month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} report (PDF) for your records or installer",
                        fontSize = 14.sp, color = Palette.Ink, modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                    )
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = Palette.Muted)
                }
            }
        }
        item { Text("Get more from your panels", fontSize = 13.sp, color = Palette.Muted, modifier = Modifier.padding(top = 6.dp)) }
        item { Tip(Icons.Rounded.CleaningServices, "Clean when output drops", "Dust, pollen and bird droppings can cost several percent. Rinse with plain water from the ground; never climb on the roof.") }
        item { Tip(Icons.Rounded.WbShade, "Watch for new shade", "A growing tree or a new building can shade one panel and pull down a whole string. Compare sunny days month by month.") }
        item { Tip(Icons.Rounded.Description, "Use more of your own power", "Run the dishwasher, washing machine or EV charging around midday to raise self-use, which is worth more than exporting.") }
    }
}

@Composable
private fun MoneyPart(color: Color, title: String, body: String, modifier: Modifier) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(color))
            Spacer(Modifier.width(6.dp))
            Text(title, fontSize = 12.sp, color = Palette.InkSoft)
        }
        Text(body, fontFamily = MonoFont, fontSize = 12.sp, color = Palette.Ink)
    }
}

@Composable
private fun Tip(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    SunCard(Modifier.fillMaxWidth(), padding = 14.dp) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = Palette.Sky, modifier = Modifier.size(20.dp))
            Column(Modifier.padding(start = 12.dp)) {
                Text(title, fontSize = 14.sp, color = Palette.Ink, fontWeight = FontWeight.Medium)
                Text(body, fontSize = 13.sp, color = Palette.InkSoft)
            }
        }
    }
}
