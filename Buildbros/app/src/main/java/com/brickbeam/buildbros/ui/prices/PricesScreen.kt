package com.brickbeam.buildbros.ui.prices

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brickbeam.buildbros.ads.NativeAdSlot
import com.brickbeam.buildbros.calc.Category
import com.brickbeam.buildbros.calc.PriceKey
import com.brickbeam.buildbros.data.AppSettings
import com.brickbeam.buildbros.ui.LocalAppEnv
import com.brickbeam.buildbros.ui.Routes
import com.brickbeam.buildbros.ui.calc.parseNum
import com.brickbeam.buildbros.ui.components.Badge
import com.brickbeam.buildbros.ui.components.BmCard
import com.brickbeam.buildbros.ui.components.InfoNote
import com.brickbeam.buildbros.ui.components.InputField
import com.brickbeam.buildbros.ui.components.OutlineButton
import com.brickbeam.buildbros.ui.components.ScreenHeader
import com.brickbeam.buildbros.ui.components.SectionTitle
import com.brickbeam.buildbros.ui.theme.BmIcons
import com.brickbeam.buildbros.ui.theme.BmType
import com.brickbeam.buildbros.ui.theme.LocalPalette
import com.brickbeam.buildbros.ui.theme.MaterialColors
import java.util.Locale

@Composable
fun PricesScreen() {
    val env = LocalAppEnv.current
    val p = LocalPalette.current
    val settings by env.vm.settings.collectAsStateWithLifecycle()
    val prices by env.vm.prices.collectAsStateWithLifecycle()
    val custom by env.vm.customizedPrices.collectAsStateWithLifecycle()
    val s = settings ?: AppSettings()
    val region = s.region
    val market = region.market
    var confirmReset by remember { mutableStateOf(false) }
    val keys = PriceKey.entries.filter { it.appliesTo(market) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { ScreenHeader("Prices", "${region.name} · ${region.currency} · ${if (region.taxIncludedInPrices) "incl. VAT" else "before sales tax"}") }
        item {
            InfoNote(
                "These are sample starting prices so the app works out of the box. Type in what your supplier charges; every calculator and estimate updates instantly. " +
                    "Prices are saved per currency (${region.currency}).",
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlineButton("Change region", { env.go(Routes.REGION) }, Modifier.weight(1f), BmIcons.Place)
                OutlineButton("Reset", { confirmReset = true }, Modifier.weight(1f), BmIcons.Refresh)
            }
        }
        Category.entries.filter { c -> keys.any { it.category == c } }.forEachIndexed { idx, cat ->
            item(key = "cat_${cat.name}") { SectionTitle(cat.label) }
            items(keys.filter { it.category == cat }, key = { it.name }) { k ->
                PriceRow(k, prices[k] ?: 0.0, k in custom, k.unit(market), region.currency, region.zone.name) { v -> env.vm.setPrice(k, v) }
            }
            if (idx == 1) item(key = "ad") { NativeAdSlot("prices") }
        }
        item { Text("Tip: prices for bulk sand and gravel are per ${if (market.name == "US") "short ton (2,000 lb)" else "tonne (1,000 kg)"}.", style = BmType.tiny, color = p.mute) }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset ${region.currency} prices?") },
            text = { Text("All prices you typed for ${region.currency} go back to the sample values.") },
            confirmButton = { TextButton(onClick = { env.vm.resetPrices(); confirmReset = false }) { Text("Reset", color = p.danger) } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun PriceRow(
    key: PriceKey,
    value: Double,
    customized: Boolean,
    unit: String,
    currency: String,
    zone: String,
    onCommit: (Double) -> Unit,
) {
    val p = LocalPalette.current
    // Local text so typing "1." or "" isn't overwritten mid-edit; re-syncs when the stored value
    // changes from elsewhere (e.g. Reset).
    var text by remember(key, zone) { mutableStateOf(fmtPrice(value)) }
    LaunchedEffect(value) { if (parseNum(text) != value) text = fmtPrice(value) }
    val parsed = parseNum(text)?.takeIf { it >= 0 }
    BmCard(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(5.dp, 34.dp).clip(RoundedCornerShape(3.dp)).background(MaterialColors.of(key.category)))
            Column(Modifier.weight(1f)) {
                Text(key.label, style = BmType.section, color = p.ink)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("per $unit", style = BmType.tiny, color = p.mute)
                    if (customized) Badge("yours") else Text("sample", style = BmType.tiny, color = p.mute)
                }
            }
            InputField(
                currency, text,
                { t ->
                    text = t
                    parseNum(t)?.takeIf { it >= 0 }?.let(onCommit)
                },
                Modifier.width(128.dp),
                isError = parsed == null,
            )
        }
    }
}

private fun fmtPrice(v: Double) = String.format(Locale.US, "%.2f", v).trimEnd('0').trimEnd('.')
