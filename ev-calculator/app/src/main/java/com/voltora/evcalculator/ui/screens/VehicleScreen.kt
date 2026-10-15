package com.voltora.evcalculator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import com.voltora.evcalculator.AppState
import com.voltora.evcalculator.data.EvDatabase
import com.voltora.evcalculator.data.EvModel
import com.voltora.evcalculator.domain.Formatter
import com.voltora.evcalculator.ui.components.BigText
import com.voltora.evcalculator.ui.components.Pill
import com.voltora.evcalculator.ui.components.SectionLabel
import com.voltora.evcalculator.ui.components.TopBlock
import com.voltora.evcalculator.ui.components.VoltButton
import com.voltora.evcalculator.ui.components.darkFieldColors
import com.voltora.evcalculator.ui.theme.Body
import com.voltora.evcalculator.ui.theme.Ev

@Composable
fun VehicleScreen(
    state: AppState,
    onSelect: (EvModel) -> Unit,
    onContinue: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var brand by rememberSaveable { mutableStateOf<String?>(state.model.brand) }
    val list = remember(query, brand) {
        val q = query.trim()
        EvDatabase.all.filter { m ->
            (q.isEmpty() || m.fullName.contains(q, ignoreCase = true)) &&
                (q.isNotEmpty() || brand == null || m.brand == brand)
        }
    }

    Column(Modifier.fillMaxSize().background(Ev.Paper)) {
        TopBlock {
            BigText("CHOOSE YOUR EV", 44.sp, Ev.Paper, weight = FontWeight.ExtraBold)
            Text(
                "${EvDatabase.all.size} models · ${EvDatabase.brands.size} brands · usable battery & real charging data",
                color = Ev.MutedOnDark, fontFamily = Body, fontSize = 13.sp,
            )
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Search brand or model") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                shape = RoundedCornerShape(14.dp),
                colors = darkFieldColors(),
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Pill("All", brand == null && query.isBlank(), true, { brand = null; query = "" }) }
                items(EvDatabase.brands) { b ->
                    Pill(b, brand == b && query.isBlank(), true, { brand = b; query = "" })
                }
            }
        }

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                SectionLabel(
                    if (query.isNotBlank()) "${list.size} results" else "${list.size} ${brand ?: "EV"} models",
                    Modifier.padding(start = 4.dp, bottom = 4.dp),
                )
            }
            items(list, key = { it.id }) { m ->
                ModelRow(m, selected = m.id == state.model.id, fmt = state.fmt, onClick = { onSelect(m) })
            }
        }

        // Selected summary attached above the nav bar
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Ev.Ink)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                SectionLabel("Selected", color = Ev.MutedOnDark)
                Text(state.model.fullName, color = Ev.Paper, fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                Text(
                    "${Formatter.number(state.model.batteryKwh, 1)} kWh · AC ${state.fmt.power(state.model.maxAcKw)} · DC ${state.fmt.power(state.model.maxDcKw)}",
                    color = Ev.MutedOnDark, fontFamily = Body, fontSize = 12.sp, maxLines = 1,
                )
            }
            Spacer(Modifier.width(12.dp))
            VoltButton("Continue", onContinue)
        }
    }
}

@Composable
private fun ModelRow(m: EvModel, selected: Boolean, fmt: Formatter, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) Ev.Paper else Ev.Mist)
            .border(1.5.dp, if (selected) Ev.Ink else Ev.Mist, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(m.fullName, fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ev.Ink)
            Text(
                "${Formatter.number(m.batteryKwh, 1)} kWh · ${fmt.distance(m.rangeKm.toDouble())} ${m.cycle.label}",
                fontFamily = Body, fontSize = 13.sp, color = Ev.Muted,
            )
            Text(
                "DC ${fmt.power(m.maxDcKw)} · 10–80% in ${m.dc10to80Min} min",
                fontFamily = Body, fontSize = 12.sp, color = Ev.Muted,
            )
        }
        if (selected) {
            Box(Modifier.size(30.dp).clip(CircleShape).background(Ev.Ink), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Check, contentDescription = "Selected", tint = Ev.Volt, modifier = Modifier.size(18.dp))
            }
        }
    }
}
