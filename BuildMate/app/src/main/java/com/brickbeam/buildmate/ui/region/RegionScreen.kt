package com.brickbeam.buildmate.ui.region

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brickbeam.buildmate.calc.Region
import com.brickbeam.buildmate.calc.RegionGroup
import com.brickbeam.buildmate.calc.Regions
import com.brickbeam.buildmate.ui.LocalAppEnv
import com.brickbeam.buildmate.ui.Routes
import com.brickbeam.buildmate.ui.components.InputField
import com.brickbeam.buildmate.ui.components.ScreenHeader
import com.brickbeam.buildmate.ui.graphics.AnimatedLogo
import com.brickbeam.buildmate.ui.theme.BmIcons
import com.brickbeam.buildmate.ui.theme.BmType
import com.brickbeam.buildmate.ui.theme.LocalPalette
import java.util.Locale

/**
 * Pick a US state or European country. On first launch ([firstRun]) there is no back button
 * and choosing a region continues to Home.
 */
@Composable
fun RegionScreen(firstRun: Boolean) {
    val env = LocalAppEnv.current
    val p = LocalPalette.current
    val settings by env.vm.settings.collectAsStateWithLifecycle()
    val current = settings?.regionCode
    var query by rememberSaveable { mutableStateOf("") }
    val guess = remember { Regions.guessFromCountry(Locale.getDefault().country) }
    val q = query.trim().lowercase()
    fun matches(r: Region) = q.isEmpty() || q in r.name.lowercase() || q == r.shortCode.lowercase() || q in r.currency.lowercase()

    fun pick(r: Region) {
        env.vm.setRegion(r.code)
        if (firstRun) {
            env.nav.navigate(Routes.HOME) { popUpTo("region?first={first}") { inclusive = true } }
        } else {
            env.back()
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            if (firstRun) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AnimatedLogo(Modifier.size(56.dp), badge = p.surface2, roof = p.accent)
                    Column {
                        Text("Where do you build?", style = BmType.title, color = p.ink)
                        Text("Sets units, standard sizes, bag weights, tax and currency. Change it anytime.", style = BmType.tiny, color = p.mute)
                    }
                }
            } else {
                ScreenHeader("Region", "US states & European countries", onBack = { env.back() })
            }
        }
        item {
            InputField("Search state or country", query, { query = it }, Modifier.fillMaxWidth(), keyboardType = KeyboardType.Text)
        }
        if (guess != null && q.isEmpty()) {
            item { GroupLabel("Suggested") }
            item { RegionRow(guess, guess.code == current) { pick(guess) } }
        }
        RegionGroup.entries.forEach { group ->
            val list = Regions.ALL.filter { it.group == group && matches(it) }
            if (list.isNotEmpty()) {
                item(key = "h_${group.name}") { GroupLabel(group.label) }
                items(list, key = { it.code }) { r -> RegionRow(r, r.code == current) { pick(r) } }
            }
        }
    }
}

@Composable
private fun GroupLabel(text: String) {
    val p = LocalPalette.current
    Text(text.uppercase(), style = BmType.tiny.copy(fontWeight = FontWeight.Bold), color = p.mute, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun RegionRow(r: Region, selected: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) p.glow else p.surface)
            .border(1.dp, if (selected) p.accentText else p.line, RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.widthIn(min = 40.dp).clip(RoundedCornerShape(8.dp)).border(1.5.dp, p.ink, RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center,
        ) { Text(r.shortCode, style = BmType.figure.copy(fontSize = BmType.tiny.fontSize), color = p.ink) }
        Column(Modifier.weight(1f)) {
            Text(r.name, style = BmType.section, color = p.ink)
            Text(
                "${r.taxLabel} ${String.format(Locale.US, "%.3f", r.taxRatePct).trimEnd('0').trimEnd('.')}%" +
                    "${if (r.taxIncludedInPrices) " (in prices)" else " + local"} · ${r.currency} · ${if (r.defaultUnitSystem.name == "IMPERIAL") "Imperial" else "Metric"}",
                style = BmType.tiny, color = p.mute,
            )
        }
        if (selected) Icon(BmIcons.Check, "Selected", tint = p.accentText)
    }
}
