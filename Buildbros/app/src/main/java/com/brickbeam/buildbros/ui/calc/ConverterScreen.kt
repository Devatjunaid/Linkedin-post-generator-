package com.brickbeam.buildbros.ui.calc

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.brickbeam.buildbros.ads.NativeAdSlot
import com.brickbeam.buildbros.calc.ConvCategory
import com.brickbeam.buildbros.ui.LocalAppEnv
import com.brickbeam.buildbros.ui.components.BmCard
import com.brickbeam.buildbros.ui.components.Chip
import com.brickbeam.buildbros.ui.components.IconSquareButton
import com.brickbeam.buildbros.ui.components.InputField
import com.brickbeam.buildbros.ui.components.PickerField
import com.brickbeam.buildbros.ui.components.Reveal
import com.brickbeam.buildbros.ui.components.ScreenHeader
import com.brickbeam.buildbros.ui.graphics.RulerArt
import com.brickbeam.buildbros.ui.theme.BmIcons
import com.brickbeam.buildbros.ui.theme.BmType
import com.brickbeam.buildbros.ui.theme.LocalPalette
import java.util.Locale

@Composable
fun ConverterScreen() {
    val env = LocalAppEnv.current
    val p = LocalPalette.current
    var cat by rememberSaveable { mutableStateOf(ConvCategory.LENGTH) }
    var fromIdx by rememberSaveable(cat) { mutableStateOf(if (cat == ConvCategory.LENGTH) 4 else 1) }
    var toIdx by rememberSaveable(cat) { mutableStateOf(if (cat == ConvCategory.LENGTH) 2 else 0) }
    var value by rememberSaveable { mutableStateOf("1") }
    val from = cat.units.getOrElse(fromIdx) { cat.units[0] }
    val to = cat.units.getOrElse(toIdx) { cat.units[0] }
    val v = parseNum(value)
    val out = v?.let { cat.convert(it, from, to) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader("Unit converter", "Imperial ⇄ metric, site units included", onBack = { env.back() })
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ConvCategory.entries.forEach { c -> Chip(c.label, c == cat, { cat = c }) }
        }
        Reveal(0) {
            BmCard(Modifier.fillMaxWidth()) {
                RulerArt(Modifier.fillMaxWidth().height(70.dp), p.ink, p.accentText)
                InputField("Value", value, { value = it }, Modifier.fillMaxWidth(), suffix = from.symbol, isError = v == null)
                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PickerField("From", from, cat.units, { "${it.symbol} · ${it.name}" }, { fromIdx = cat.units.indexOf(it) }, Modifier.weight(1f))
                    IconSquareButton(BmIcons.Swap, "Swap units", { val t = fromIdx; fromIdx = toIdx; toIdx = t })
                    PickerField("To", to, cat.units, { "${it.symbol} · ${it.name}" }, { toIdx = cat.units.indexOf(it) }, Modifier.weight(1f))
                }
                Text(
                    out?.let { "${format(it)} ${to.symbol}" } ?: "—",
                    style = BmType.bigFigure.copy(fontSize = BmType.bigFigure.fontSize * 0.8f), color = p.ink,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
        if (v != null) {
            BmCard(Modifier.fillMaxWidth()) {
                Text("${format(v)} ${from.symbol} equals", style = BmType.label, color = p.mute)
                cat.units.filter { it != from }.forEach { u ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                        Text(u.name, style = BmType.body, color = p.ink, modifier = Modifier.weight(1f))
                        Text("${format(cat.convert(v, from, u))} ${u.symbol}", style = BmType.figure, color = p.ink)
                    }
                }
            }
        }
        NativeAdSlot("converter")
    }
}

private fun format(v: Double): String {
    val abs = kotlin.math.abs(v)
    val pattern = when {
        abs == 0.0 -> "%.0f"
        abs >= 1000 -> "%,.1f"
        abs >= 1 -> "%,.4f"
        else -> "%.6f"
    }
    val s = String.format(Locale.US, pattern, v)
    return if (s.contains('.')) s.trimEnd('0').trimEnd('.') else s
}
