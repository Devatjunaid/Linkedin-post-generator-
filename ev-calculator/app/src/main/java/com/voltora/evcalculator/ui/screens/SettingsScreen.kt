package com.voltora.evcalculator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltora.evcalculator.AppState
import com.voltora.evcalculator.data.DistanceUnit
import com.voltora.evcalculator.domain.ChargeLocation
import com.voltora.evcalculator.domain.Formatter
import com.voltora.evcalculator.ui.components.BigText
import com.voltora.evcalculator.ui.components.MistCard
import com.voltora.evcalculator.ui.components.Pill
import com.voltora.evcalculator.ui.components.SectionLabel
import com.voltora.evcalculator.ui.components.TopBlock
import com.voltora.evcalculator.ui.components.VoltButton
import com.voltora.evcalculator.ui.components.VoltSlider
import com.voltora.evcalculator.ui.components.lightFieldColors
import com.voltora.evcalculator.ui.theme.Body
import com.voltora.evcalculator.ui.theme.Ev
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    state: AppState,
    onBack: () -> Unit,
    onPickCountry: () -> Unit,
    onUnit: (DistanceUnit) -> Unit,
    onHomeKw: (Double) -> Unit,
    onPetrolL: (Double) -> Unit,
    onSaveRates: (home: Double?, ac: Double?, dc: Double?, petrol: Double?) -> Unit,
    privacyOptionsRequired: Boolean,
    onPrivacyOptions: () -> Unit,
) {
    val c = state.country
    var home by remember { mutableStateOf("") }
    var ac by remember { mutableStateOf("") }
    var dc by remember { mutableStateOf("") }
    var petrol by remember { mutableStateOf("") }
    LaunchedEffect(state.prefs.homeRate, state.prefs.acRate, state.prefs.dcRate, state.prefs.petrolPrice, c.code) {
        home = state.homeRate.toPlain(); ac = state.acRate.toPlain(); dc = state.dcRate.toPlain(); petrol = state.petrolPrice.toPlain()
    }
    var petrolL by remember { mutableFloatStateOf(state.prefs.petrolLPer100.toFloat()) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Ev.Paper)
            .verticalScroll(rememberScrollState()),
    ) {
        TopBlock {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.clip(RoundedCornerShape(50)).background(Ev.Ink2)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Ev.Paper)
                }
            }
            BigText("SETTINGS", 44.sp, Ev.Paper, weight = FontWeight.ExtraBold)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Ev.Ink2)
                    .clickable(onClick = onPickCountry)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(c.flag, fontSize = 24.sp)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(c.name, color = Ev.Paper, fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Currency ${c.currencyCode} · prices per kWh", color = Ev.MutedOnDark, fontFamily = Body, fontSize = 12.sp)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Change country", tint = Ev.Paper)
            }
        }

        Column(Modifier.padding(horizontal = 20.dp, vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Your prices (${c.currencyCode})")
                Text(
                    "Defaults are typical 2025 prices for ${c.name}. Enter your own tariff for exact results.",
                    fontFamily = Body, fontSize = 13.sp, color = Ev.Muted,
                )
                RateField("Home electricity / kWh", home) { home = it }
                RateField("Public AC / kWh", ac) { ac = it }
                RateField("DC fast / kWh", dc) { dc = it }
                RateField("Petrol / litre", petrol) { petrol = it }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    VoltButton("Reset", { onSaveRates(null, null, null, null) }, Modifier.weight(1f), dark = true)
                    VoltButton(
                        "Save",
                        {
                            fun parse(s: String, def: Double) = s.replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0 }
                                ?.takeIf { it != def }
                            onSaveRates(parse(home, c.home), parse(ac, c.publicAc), parse(dc, c.dcFast), parse(petrol, c.petrol))
                        },
                        Modifier.weight(1f),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Home charger")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChargeLocation.HOME.powerOptions.forEach { kw ->
                        Pill(state.fmt.power(kw), kw == state.prefs.homeChargerKw, false, { onHomeKw(kw) })
                    }
                }
                Text(
                    "2.3 kW ≈ a regular socket · 7.4 kW ≈ single-phase wallbox · 11/22 kW ≈ three-phase wallbox",
                    fontFamily = Body, fontSize = 12.sp, color = Ev.Muted,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Distance unit")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("Auto (${if (c.useMiles) "mi" else "km"})", state.prefs.distanceUnit == DistanceUnit.AUTO, false, { onUnit(DistanceUnit.AUTO) })
                    Pill("Kilometres", state.prefs.distanceUnit == DistanceUnit.KM, false, { onUnit(DistanceUnit.KM) })
                    Pill("Miles", state.prefs.distanceUnit == DistanceUnit.MILES, false, { onUnit(DistanceUnit.MILES) })
                }
            }

            Column {
                Row {
                    SectionLabel("Petrol car to compare", Modifier.weight(1f))
                    Text("${Formatter.number(petrolL.toDouble(), 1)} L/100 km", fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ev.Ink)
                }
                VoltSlider(
                    petrolL,
                    { petrolL = (it * 2).roundToInt() / 2f },
                    valueRange = 4f..14f,
                    onValueChangeFinished = { onPetrolL(petrolL.toDouble()) },
                )
            }

            MistCard {
                SectionLabel("How we calculate")
                Text(
                    "• Real-world consumption from official WLTP / EPA / CLTC / MIDC ranges, adjusted for each test cycle and your driving style.\n" +
                        "• AC charging counts ~10% wall-to-battery loss; DC counts ~5%.\n" +
                        "• DC times follow a typical charge curve scaled to each car's official 10–80% time.\n" +
                        "• Prices are national averages; your tariff, network and time of day can differ.",
                    fontFamily = Body, fontSize = 13.sp, color = Ev.Ink,
                )
            }

            if (privacyOptionsRequired) {
                VoltButton("Privacy & ad choices", onPrivacyOptions, Modifier.fillMaxWidth(), dark = true)
            }
        }
    }
}

@Composable
private fun RateField(label: String, value: String, onChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = { v -> onChange(v.filter { it.isDigit() || it == '.' || it == ',' }.take(9)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(14.dp),
        colors = lightFieldColors(),
    )
}

private fun Double.toPlain(): String = Formatter.number(this, 3)
