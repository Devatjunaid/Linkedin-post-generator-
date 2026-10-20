package com.superdevlabs.coldplunge.ui.protocols

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.superdevlabs.coldplunge.domain.Protocol
import com.superdevlabs.coldplunge.domain.ProtocolPlanner
import com.superdevlabs.coldplunge.domain.Safety
import com.superdevlabs.coldplunge.domain.Units
import com.superdevlabs.coldplunge.ui.components.AmbientBackground
import com.superdevlabs.coldplunge.ui.components.CircleIconButton
import com.superdevlabs.coldplunge.ui.components.GlassCard
import com.superdevlabs.coldplunge.ui.components.IconBadge
import com.superdevlabs.coldplunge.ui.components.KIcons
import com.superdevlabs.coldplunge.ui.components.NoteLine
import com.superdevlabs.coldplunge.ui.components.PrimaryButton
import com.superdevlabs.coldplunge.ui.components.RibbonLegend
import com.superdevlabs.coldplunge.ui.components.SecondaryButton
import com.superdevlabs.coldplunge.ui.components.SectionLabel
import com.superdevlabs.coldplunge.ui.components.Stepper
import com.superdevlabs.coldplunge.ui.components.ThermalButton
import com.superdevlabs.coldplunge.ui.components.ThermalRibbon
import com.superdevlabs.coldplunge.ui.components.ToggleRow
import com.superdevlabs.coldplunge.ui.theme.K
import com.superdevlabs.coldplunge.ui.theme.KType

@Composable
fun ProtocolEditorScreen(
    initial: Protocol?,
    fahrenheit: Boolean,
    onBack: () -> Unit,
    onSave: (Protocol) -> Unit,
    onStart: (Protocol) -> Unit,
) {
    val base = remember(initial) {
        when {
            initial == null -> ProtocolPlanner.presets.first().copy(id = 0L, name = "My protocol", builtIn = false)
            initial.builtIn -> initial.copy(id = 0L, name = "My ${initial.name}", builtIn = false)
            else -> initial
        }
    }
    var name by remember { mutableStateOf(base.name) }
    var heatSec by remember { mutableStateOf(base.heatSec) }
    var coldSec by remember { mutableStateOf(base.coldSec) }
    var restSec by remember { mutableStateOf(base.restSec) }
    var rounds by remember { mutableStateOf(base.rounds) }
    var endOnCold by remember { mutableStateOf(base.endOnCold) }
    var heatTemp by remember { mutableStateOf(base.heatTempC) }
    var coldTemp by remember { mutableStateOf(base.coldTempC) }

    val current = Protocol(
        id = base.id,
        name = name.trim().ifEmpty { "My protocol" },
        heatSec = heatSec,
        coldSec = coldSec,
        restSec = restSec,
        rounds = rounds,
        endOnCold = endOnCold,
        heatTempC = heatTemp,
        coldTempC = coldTemp,
    )
    val phases = remember(current) { ProtocolPlanner.phases(current) }
    val totals = remember(phases) { ProtocolPlanner.totals(phases) }
    val notes = (Safety.coldNotes(coldTemp, coldSec) + Safety.heatNotes(heatTemp, heatSec)).distinct()

    Box(Modifier.fillMaxSize()) {
        AmbientBackground(warmth = 0.5f)
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleIconButton(KIcons.Back, "Back", onBack)
                Text(
                    if (base.id > 0) "Edit protocol" else "Protocol builder",
                    style = KType.Label,
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                )
            }
            Text("Design your\ncontrast cycle", style = KType.Title)

            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 40) name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = KType.BodyStrong,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = K.Ice,
                    unfocusedBorderColor = Color(0x33FFFFFF),
                    focusedLabelColor = K.Ice,
                    unfocusedLabelColor = K.Muted,
                    cursorColor = K.Ice,
                    focusedTextColor = K.Text,
                    unfocusedTextColor = K.Text,
                ),
            )

            GlassCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SectionLabel("Thermal ribbon")
                    Text(Units.duration(totals.totalSec), style = KType.Mono.copy(fontSize = KType.Small.fontSize))
                }
                ThermalRibbon(phases, height = 54.dp)
                RibbonLegend(Units.duration(totals.heatSec), Units.duration(totals.coldSec), Units.duration(totals.restSec))
            }

            GlassCard(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                EditorRow(KIcons.Flame, K.Ember, "Sauna", "Each heat block", Units.duration(heatSec),
                    { heatSec = (heatSec - 60).coerceAtLeast(3 * 60) }, { heatSec = (heatSec + 60).coerceAtMost(45 * 60) })
                HorizontalDivider(color = K.Line)
                EditorRow(KIcons.Thermometer, K.Ember, "Sauna temp", "Dry sauna 70–100 °C is typical", Units.temp(heatTemp, fahrenheit),
                    { heatTemp = (heatTemp - 5f).coerceAtLeast(40f) }, { heatTemp = (heatTemp + 5f).coerceAtMost(110f) })
                HorizontalDivider(color = K.Line)
                EditorRow(KIcons.Snowflake, K.Ice, "Cold plunge", "Each cold block", Units.clock(coldSec),
                    { coldSec = (coldSec - 15).coerceAtLeast(15) }, { coldSec = (coldSec + 15).coerceAtMost(15 * 60) })
                HorizontalDivider(color = K.Line)
                EditorRow(KIcons.Thermometer, K.Ice, "Water temp", "3–15 °C is typical", Units.temp(coldTemp, fahrenheit),
                    { coldTemp = (coldTemp - 0.5f).coerceAtLeast(0f) }, { coldTemp = (coldTemp + 0.5f).coerceAtMost(25f) })
                HorizontalDivider(color = K.Line)
                EditorRow(KIcons.Breath, Color(0xFFB6C2CE), "Rest", "Between rounds, in air", Units.duration(restSec),
                    { restSec = (restSec - 60).coerceAtLeast(0) }, { restSec = (restSec + 60).coerceAtMost(20 * 60) })
                HorizontalDivider(color = K.Line)
                EditorRow(KIcons.Thermal, K.Violet, "Rounds", "Full heat → cold cycles", "×$rounds",
                    { rounds = (rounds - 1).coerceAtLeast(1) }, { rounds = (rounds + 1).coerceAtMost(8) })
            }

            GlassCard(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                ToggleRow(
                    title = "Finish on cold",
                    subtitle = if (endOnCold) "Let your body rewarm naturally afterwards" else "Adds a final rest and heat block",
                    checked = endOnCold,
                    onCheckedChange = { endOnCold = it },
                )
            }

            notes.forEach { NoteLine(it) }

            ThermalButton("Start · ${Units.duration(totals.totalSec)}", onClick = { onStart(current) }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton("Save protocol", onClick = { onSave(current) }, icon = KIcons.Check, modifier = Modifier.weight(1f))
                SecondaryButton("Cancel", onClick = onBack)
            }
        }
    }
}

@Composable
private fun EditorRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        IconBadge(icon, tint, size = 36.dp)
        Column(Modifier.weight(1f)) {
            Text(title, style = KType.BodyStrong)
            Text(subtitle, style = KType.Small, maxLines = 2)
        }
        Stepper(value, onDecrease, onIncrease, "Decrease $title", "Increase $title", valueWidth = 64.dp)
    }
}
