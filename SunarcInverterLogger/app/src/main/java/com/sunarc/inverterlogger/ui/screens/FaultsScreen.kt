package com.sunarc.inverterlogger.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sunarc.inverterlogger.ads.NativeAdSlot
import com.sunarc.inverterlogger.domain.FaultCategory
import com.sunarc.inverterlogger.domain.FaultInfo
import com.sunarc.inverterlogger.domain.FaultLibrary
import com.sunarc.inverterlogger.domain.InverterBrands
import com.sunarc.inverterlogger.domain.Severity
import com.sunarc.inverterlogger.ui.AppViewModel
import com.sunarc.inverterlogger.ui.Navigator
import com.sunarc.inverterlogger.ui.Route
import com.sunarc.inverterlogger.ui.components.ScreenHeader
import com.sunarc.inverterlogger.ui.components.SunCard
import com.sunarc.inverterlogger.ui.components.SunChip
import com.sunarc.inverterlogger.ui.components.TextInput
import com.sunarc.inverterlogger.ui.theme.MonoFont
import com.sunarc.inverterlogger.ui.theme.Palette

@Composable
fun FaultsScreen(vm: AppViewModel, nav: Navigator) {
    val inverters by vm.inverters.collectAsStateWithLifecycle()
    val dashboard by vm.dashboard.collectAsStateWithLifecycle()
    val myBrand = inverters.firstOrNull()?.brandId
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("mine") }
    var open by remember { mutableStateOf<String?>(null) }

    val results = FaultLibrary.search(query, if (filter == "mine") myBrand else null).let { list ->
        when (filter) {
            "mine", "all" -> list
            else -> list.filter { it.category.name == filter }
        }
    }.let { list -> if (query.isBlank() && filter == "mine") list.ifEmpty { FaultLibrary.allGeneral } else list }

    ScreenList(Modifier.fillMaxSize()) {
        item { ScreenHeader("Fault codes", subtitle = "Offline library") }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextInput("Code or message, e.g. 102", query, { query = it }, Modifier.weight(1f))
                IconButton(
                    onClick = { nav.push(Route.SCAN) },
                    modifier = Modifier.padding(start = 8.dp).size(52.dp).clip(RoundedCornerShape(16.dp)).background(Palette.Sun)
                ) { Icon(Icons.Rounded.DocumentScanner, contentDescription = "Scan an error screen", tint = Palette.OnSun) }
            }
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SunChip(if (myBrand != null) InverterBrands.byId(myBrand).name else "My inverter", filter == "mine", { filter = "mine" })
                SunChip("All", filter == "all", { filter = "all" })
                listOf(FaultCategory.GRID, FaultCategory.DC, FaultCategory.INSULATION, FaultCategory.TEMPERATURE, FaultCategory.COMMUNICATION, FaultCategory.BATTERY).forEach { c ->
                    SunChip(c.label, filter == c.name, { filter = c.name })
                }
            }
        }
        val seen = dashboard?.faultsThisMonth.orEmpty()
        if (seen.isNotEmpty()) {
            item {
                SunCard(Modifier.fillMaxWidth(), color = Palette.SunDeep.copy(alpha = 0.35f), border = Palette.Sun.copy(alpha = 0.4f)) {
                    Text("Seen on your inverter this month", fontSize = 13.sp, color = Palette.Ink, fontWeight = FontWeight.Medium)
                    seen.forEach { (code, count) ->
                        val info = FaultLibrary.decode(code, code, myBrand)
                        Text("$code · ${info.title} · ${count}×", fontFamily = MonoFont, fontSize = 12.sp, color = Palette.InkSoft)
                    }
                }
            }
        }
        if (results.isEmpty()) {
            item { Text("Nothing matched. Try the words shown on the screen, like \"isolation\" or \"grid\".", fontSize = 13.sp, color = Palette.InkSoft) }
        }
        itemsIndexed(results, key = { _, f -> f.code + (f.brandId ?: "") }) { i, f ->
            Column {
                FaultCard(f, open == f.code + f.brandId) { open = if (open == f.code + f.brandId) null else f.code + f.brandId }
                // A native ad fits the gap after the third entry.
                if (i == 2) NativeAdSlot(vm.ads, Modifier.padding(top = 12.dp))
            }
        }
        item { Text(FaultLibrary.DISCLAIMER, fontSize = 11.sp, color = Palette.Muted, modifier = Modifier.padding(top = 8.dp)) }
    }
}

@Composable
private fun FaultCard(f: FaultInfo, expanded: Boolean, onToggle: () -> Unit) {
    SunCard(Modifier.fillMaxWidth(), padding = 0.dp) {
        Row(
            Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = if (expanded) "Hide details" else "Show details", onClick = onToggle).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(width = 58.dp, height = 44.dp).clip(RoundedCornerShape(12.dp)).background(Palette.Night).border(1.dp, Palette.Line, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(f.code.take(6), fontFamily = MonoFont, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Palette.SunSoft)
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(f.title, fontSize = 14.sp, color = Palette.Ink, fontWeight = FontWeight.Medium)
                Text(listOfNotNull(f.brandId?.let { InverterBrands.byId(it).name }, f.category.label).joinToString(" · "), fontSize = 12.sp, color = Palette.Muted)
            }
            SeverityTag(f.severity)
        }
        AnimatedVisibility(expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(Modifier.padding(start = 82.dp, end = 14.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(f.meaning, fontSize = 13.sp, color = Palette.InkSoft)
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Palette.Night).border(1.dp, Palette.LineSoft, RoundedCornerShape(12.dp)).padding(10.dp)) {
                    Text("What to do: ${f.action}", fontSize = 13.sp, color = Palette.Ink)
                }
            }
        }
    }
}

@Composable
private fun SeverityTag(s: Severity) {
    val (bg, fg, border) = when (s) {
        Severity.INFO -> Triple(Palette.SkyNight, Palette.Sky, Palette.SkyDeep)
        Severity.CHECK -> Triple(Palette.Sun, Palette.OnSun, Palette.Sun)
        Severity.INSTALLER -> Triple(Palette.Night, Palette.Ink, Palette.Ink)
    }
    Text(
        if (s == Severity.INSTALLER) "Installer" else s.label,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(bg).border(1.dp, border, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
        fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = fg
    )
}
