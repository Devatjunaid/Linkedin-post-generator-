package com.superdevlabs.coldplunge.ui.protocols

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
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
import com.superdevlabs.coldplunge.ads.NativeAdCard
import com.superdevlabs.coldplunge.domain.Protocol
import com.superdevlabs.coldplunge.domain.ProtocolPlanner
import com.superdevlabs.coldplunge.domain.Units
import com.superdevlabs.coldplunge.ui.components.CircleIconButton
import com.superdevlabs.coldplunge.ui.components.GlassCard
import com.superdevlabs.coldplunge.ui.components.KIcons
import com.superdevlabs.coldplunge.ui.components.PrimaryButton
import com.superdevlabs.coldplunge.ui.components.SecondaryButton
import com.superdevlabs.coldplunge.ui.components.SectionLabel
import com.superdevlabs.coldplunge.ui.components.ThermalRibbon
import com.superdevlabs.coldplunge.ui.theme.K
import com.superdevlabs.coldplunge.ui.theme.KType
import com.google.android.gms.ads.nativead.NativeAd

@Composable
fun ProtocolsScreen(
    custom: List<Protocol>,
    favoriteId: Long,
    fahrenheit: Boolean,
    nativeAd: NativeAd?,
    onStart: (Protocol) -> Unit,
    onEdit: (Protocol?) -> Unit,
    onDelete: (Protocol) -> Unit,
    onFeature: (Protocol) -> Unit,
) {
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    var pendingDelete by remember { mutableStateOf<Protocol?>(null) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = top + 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Protocols", style = KType.Title)
                    Text("Rounds of heat → cold, timed for you", style = KType.Small)
                }
                CircleIconButton(KIcons.Plus, "New protocol", { onEdit(null) }, background = K.Ice.copy(alpha = 0.16f), tint = K.Ice)
            }
        }

        if (custom.isNotEmpty()) {
            item { SectionLabel("Your protocols") }
            items(custom, key = { "c${it.id}" }) { p ->
                ProtocolCard(
                    protocol = p,
                    featured = p.id == favoriteId,
                    fahrenheit = fahrenheit,
                    onStart = { onStart(p) },
                    onEdit = { onEdit(p) },
                    onDelete = { pendingDelete = p },
                    onFeature = { onFeature(p) },
                )
            }
        }

        item { SectionLabel("Built-in") }
        items(ProtocolPlanner.presets.take(2), key = { "p${it.id}" }) { p ->
            ProtocolCard(p, p.id == favoriteId, fahrenheit, { onStart(p) }, { onEdit(p) }, null, { onFeature(p) })
        }
        if (nativeAd != null) item { NativeAdCard(nativeAd) }
        items(ProtocolPlanner.presets.drop(2), key = { "p${it.id}" }) { p ->
            ProtocolCard(p, p.id == favoriteId, fahrenheit, { onStart(p) }, { onEdit(p) }, null, { onFeature(p) })
        }
    }

    pendingDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = K.Surface2,
            title = { Text("Delete “${p.name}”?", style = KType.Heading) },
            text = { Text("Your logged sessions are kept.", style = KType.Body) },
            confirmButton = {
                TextButton(onClick = { onDelete(p); pendingDelete = null }) { Text("Delete", style = KType.BodyStrong.copy(color = K.EmberDeep)) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel", style = KType.BodyStrong) } },
        )
    }
}

@Composable
private fun ProtocolCard(
    protocol: Protocol,
    featured: Boolean,
    fahrenheit: Boolean,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: (() -> Unit)?,
    onFeature: () -> Unit,
) {
    val phases = remember(protocol) { ProtocolPlanner.phases(protocol) }
    val totals = remember(phases) { ProtocolPlanner.totals(phases) }
    GlassCard(Modifier.animateContentSize(), border = if (featured) K.Ice.copy(alpha = 0.45f) else K.Line) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(protocol.name, style = KType.Heading)
                Text(
                    "${protocol.rounds} × (${Units.duration(protocol.heatSec)} heat → ${Units.clock(protocol.coldSec)} cold)" +
                        " · ${Units.temp(protocol.heatTempC, fahrenheit)} / ${Units.temp(protocol.coldTempC, fahrenheit)}",
                    style = KType.Small,
                )
            }
            CircleIconButton(
                KIcons.Star,
                if (featured) "Featured on Today" else "Feature on Today",
                onFeature,
                size = 38.dp,
                tint = if (featured) K.Gold else K.Faint,
            )
        }
        ThermalRibbon(phases, height = 40.dp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total ${Units.duration(totals.totalSec)}", style = KType.Small.copy(color = K.Text))
            Text("Heat ${Units.duration(totals.heatSec)} · Cold ${Units.duration(totals.coldSec)}", style = KType.Small)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton("Start", onClick = onStart, icon = KIcons.Play, modifier = Modifier.weight(1f))
            SecondaryButton(if (protocol.builtIn) "Customize" else "Edit", onClick = onEdit, icon = KIcons.Edit)
            if (onDelete != null) CircleIconButton(KIcons.Trash, "Delete ${protocol.name}", onDelete, size = 50.dp, tint = K.EmberDeep)
        }
    }
}
