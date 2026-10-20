package com.superdevlabs.coldplunge.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.superdevlabs.coldplunge.data.Settings
import com.superdevlabs.coldplunge.domain.PhaseType
import com.superdevlabs.coldplunge.domain.Protocol
import com.superdevlabs.coldplunge.domain.ProtocolPlanner
import com.superdevlabs.coldplunge.domain.Safety
import com.superdevlabs.coldplunge.domain.Units
import com.superdevlabs.coldplunge.ui.components.Chip
import com.superdevlabs.coldplunge.ui.components.GlassCard
import com.superdevlabs.coldplunge.ui.components.NoteLine
import com.superdevlabs.coldplunge.ui.components.RibbonLegend
import com.superdevlabs.coldplunge.ui.components.SecondaryButton
import com.superdevlabs.coldplunge.ui.components.SectionLabel
import com.superdevlabs.coldplunge.ui.components.Stepper
import com.superdevlabs.coldplunge.ui.components.ThermalButton
import com.superdevlabs.coldplunge.ui.components.ThermalRibbon
import com.superdevlabs.coldplunge.ui.theme.K
import com.superdevlabs.coldplunge.ui.theme.KType

private enum class QuickMode(val label: String) { Cold("Cold"), Heat("Sauna"), Contrast("Contrast") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickStartSheet(
    initial: PhaseType?,
    settings: Settings,
    featured: Protocol,
    onDismiss: () -> Unit,
    onStartSingle: (type: PhaseType, seconds: Int, tempC: Float) -> Unit,
    onStartProtocol: (Protocol) -> Unit,
    onBrowseProtocols: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var mode by remember {
        mutableStateOf(
            when (initial) {
                PhaseType.COLD -> QuickMode.Cold
                PhaseType.HEAT -> QuickMode.Heat
                else -> QuickMode.Contrast
            },
        )
    }
    var coldSec by remember { mutableIntStateOf(settings.lastColdSec) }
    var coldTemp by remember { mutableFloatStateOf(settings.lastColdTempC) }
    var heatSec by remember { mutableIntStateOf(settings.lastHeatSec) }
    var heatTemp by remember { mutableFloatStateOf(settings.lastHeatTempC) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = K.Surface,
        contentColor = K.Text,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Quick start", style = KType.Title)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickMode.entries.forEach { m ->
                    Chip(
                        m.label,
                        selected = m == mode,
                        onClick = { mode = m },
                        accent = when (m) {
                            QuickMode.Cold -> K.Ice
                            QuickMode.Heat -> K.Ember
                            QuickMode.Contrast -> K.Text
                        },
                    )
                }
            }

            AnimatedContent(
                targetState = mode,
                transitionSpec = {
                    val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                    (slideInHorizontally { it * dir / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it * dir / 3 } + fadeOut())
                },
                label = "quickMode",
            ) { m ->
                when (m) {
                    QuickMode.Cold -> SingleSetup(
                        accentCold = true,
                        seconds = coldSec,
                        tempC = coldTemp,
                        fahrenheit = settings.fahrenheit,
                        presets = listOf(60, 120, 180, 300),
                        stepSec = 15,
                        minSec = 15,
                        maxSec = 20 * 60,
                        tempStep = 0.5f,
                        minTemp = 0f,
                        maxTemp = 20f,
                        notes = Safety.coldNotes(coldTemp, coldSec),
                        onSeconds = { coldSec = it },
                        onTemp = { coldTemp = it },
                        onStart = { onStartSingle(PhaseType.COLD, coldSec, coldTemp) },
                    )
                    QuickMode.Heat -> SingleSetup(
                        accentCold = false,
                        seconds = heatSec,
                        tempC = heatTemp,
                        fahrenheit = settings.fahrenheit,
                        presets = listOf(10 * 60, 15 * 60, 20 * 60),
                        stepSec = 60,
                        minSec = 3 * 60,
                        maxSec = 45 * 60,
                        tempStep = 5f,
                        minTemp = 40f,
                        maxTemp = 110f,
                        notes = Safety.heatNotes(heatTemp, heatSec),
                        onSeconds = { heatSec = it },
                        onTemp = { heatTemp = it },
                        onStart = { onStartSingle(PhaseType.HEAT, heatSec, heatTemp) },
                    )
                    QuickMode.Contrast -> {
                        val phases = remember(featured) { ProtocolPlanner.phases(featured) }
                        val totals = remember(phases) { ProtocolPlanner.totals(phases) }
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            GlassCard {
                                SectionLabel("Featured protocol")
                                Text(featured.name, style = KType.Heading)
                                ThermalRibbon(phases, height = 46.dp)
                                RibbonLegend(Units.duration(totals.heatSec), Units.duration(totals.coldSec), Units.duration(totals.restSec))
                            }
                            ThermalButton("Start ${featured.name}", onClick = { onStartProtocol(featured) }, modifier = Modifier.fillMaxWidth())
                            SecondaryButton("Choose another protocol", onClick = onBrowseProtocols, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SingleSetup(
    accentCold: Boolean,
    seconds: Int,
    tempC: Float,
    fahrenheit: Boolean,
    presets: List<Int>,
    stepSec: Int,
    minSec: Int,
    maxSec: Int,
    tempStep: Float,
    minTemp: Float,
    maxTemp: Float,
    notes: List<String>,
    onSeconds: (Int) -> Unit,
    onTemp: (Float) -> Unit,
    onStart: () -> Unit,
) {
    val accent = if (accentCold) K.Ice else K.Ember
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard {
            SectionLabel("Duration")
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(Units.clock(seconds), style = KType.MonoHuge.copy(fontSize = 44.sp, color = accent))
                Stepper(
                    value = "",
                    onDecrease = { onSeconds((seconds - stepSec).coerceAtLeast(minSec)) },
                    onIncrease = { onSeconds((seconds + stepSec).coerceAtMost(maxSec)) },
                    decreaseLabel = "Shorter",
                    increaseLabel = "Longer",
                    valueWidth = 8.dp,
                )
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { p -> Chip(Units.clock(p), selected = seconds == p, onClick = { onSeconds(p) }, accent = accent) }
            }
        }
        GlassCard {
            SectionLabel(if (accentCold) "Water temperature" else "Sauna temperature")
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(Units.temp(tempC, fahrenheit), style = KType.Mono.copy(fontSize = 28.sp))
                Stepper(
                    value = "",
                    onDecrease = { onTemp((tempC - tempStep).coerceAtLeast(minTemp)) },
                    onIncrease = { onTemp((tempC + tempStep).coerceAtMost(maxTemp)) },
                    decreaseLabel = "Colder",
                    increaseLabel = "Warmer",
                    valueWidth = 8.dp,
                )
            }
            Text("Logged with the session so you can track progress.", style = KType.Small)
        }
        notes.forEach { NoteLine(it) }
        ThermalButton(if (accentCold) "Start plunge" else "Start sauna", onClick = onStart, modifier = Modifier.fillMaxWidth())
    }
}
