package com.superdevlabs.coldplunge.ui.log

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.superdevlabs.coldplunge.domain.SessionKind
import com.superdevlabs.coldplunge.domain.SessionRecord
import com.superdevlabs.coldplunge.domain.Units
import com.superdevlabs.coldplunge.ui.components.Chip
import com.superdevlabs.coldplunge.ui.components.PrimaryButton
import com.superdevlabs.coldplunge.ui.components.SecondaryButton
import com.superdevlabs.coldplunge.ui.components.SectionLabel
import com.superdevlabs.coldplunge.ui.components.Stepper
import com.superdevlabs.coldplunge.ui.theme.K
import com.superdevlabs.coldplunge.ui.theme.KType
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Log a session done without the timer (e.g. at a gym or spa). */
@Composable
fun ManualEntryDialog(
    fahrenheit: Boolean,
    defaultColdTemp: Float,
    defaultHeatTemp: Float,
    onDismiss: () -> Unit,
    onSave: (SessionRecord) -> Unit,
) {
    var kind by remember { mutableStateOf(SessionKind.COLD) }
    var daysAgo by remember { mutableIntStateOf(0) }
    var coldSec by remember { mutableIntStateOf(180) }
    var heatSec by remember { mutableIntStateOf(15 * 60) }
    var rounds by remember { mutableIntStateOf(1) }
    var coldTemp by remember { mutableStateOf(defaultColdTemp) }
    var heatTemp by remember { mutableStateOf(defaultHeatTemp) }
    val hasCold = kind != SessionKind.HEAT
    val hasHeat = kind != SessionKind.COLD

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF16202B), K.InkDeep)))
                .border(1.dp, K.Line, RoundedCornerShape(28.dp))
                .verticalScroll(rememberScrollState())
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Add a session", style = KType.Title)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Cold", kind == SessionKind.COLD, { kind = SessionKind.COLD }, accent = K.Ice)
                Chip("Sauna", kind == SessionKind.HEAT, { kind = SessionKind.HEAT }, accent = K.Ember)
                Chip("Contrast", kind == SessionKind.CONTRAST, { kind = SessionKind.CONTRAST })
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    SectionLabel("When")
                    Text(
                        when (daysAgo) {
                            0 -> "Today"
                            1 -> "Yesterday"
                            else -> "$daysAgo days ago"
                        },
                        style = KType.BodyStrong,
                    )
                }
                Stepper("", { daysAgo = (daysAgo + 1).coerceAtMost(60) }, { daysAgo = (daysAgo - 1).coerceAtLeast(0) }, "Earlier", "Later", valueWidth = 8.dp)
            }
            if (hasCold) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        SectionLabel(if (kind == SessionKind.CONTRAST) "Total cold" else "Cold time")
                        Text(Units.clock(coldSec), style = KType.Mono.copy(color = K.Ice))
                    }
                    Stepper("", { coldSec = (coldSec - 15).coerceAtLeast(15) }, { coldSec = (coldSec + 15).coerceAtMost(60 * 60) }, "Less cold time", "More cold time", valueWidth = 8.dp)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        SectionLabel("Water temp")
                        Text(Units.temp(coldTemp, fahrenheit), style = KType.Mono)
                    }
                    Stepper("", { coldTemp = (coldTemp - 0.5f).coerceAtLeast(0f) }, { coldTemp = (coldTemp + 0.5f).coerceAtMost(25f) }, "Colder", "Warmer", valueWidth = 8.dp)
                }
            }
            if (hasHeat) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        SectionLabel(if (kind == SessionKind.CONTRAST) "Total heat" else "Heat time")
                        Text(Units.duration(heatSec), style = KType.Mono.copy(color = K.Ember))
                    }
                    Stepper("", { heatSec = (heatSec - 60).coerceAtLeast(60) }, { heatSec = (heatSec + 60).coerceAtMost(3 * 60 * 60) }, "Less heat time", "More heat time", valueWidth = 8.dp)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        SectionLabel("Sauna temp")
                        Text(Units.temp(heatTemp, fahrenheit), style = KType.Mono)
                    }
                    Stepper("", { heatTemp = (heatTemp - 5f).coerceAtLeast(40f) }, { heatTemp = (heatTemp + 5f).coerceAtMost(120f) }, "Cooler", "Hotter", valueWidth = 8.dp)
                }
            }
            if (kind == SessionKind.CONTRAST) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        SectionLabel("Rounds")
                        Text("×$rounds", style = KType.Mono)
                    }
                    Stepper("", { rounds = (rounds - 1).coerceAtLeast(1) }, { rounds = (rounds + 1).coerceAtMost(10) }, "Fewer rounds", "More rounds", valueWidth = 8.dp)
                }
            }
            PrimaryButton(
                "Save",
                onClick = {
                    val zone = ZoneId.systemDefault()
                    val date = LocalDate.now(zone).minusDays(daysAgo.toLong())
                    val time = if (daysAgo == 0) LocalTime.now(zone) else LocalTime.of(12, 0)
                    val cold = if (hasCold) coldSec else 0
                    val heat = if (hasHeat) heatSec else 0
                    onSave(
                        SessionRecord(
                            startedAt = date.atTime(time).atZone(zone).toInstant().toEpochMilli(),
                            kind = kind,
                            title = when (kind) {
                                SessionKind.COLD -> "Cold Plunge"
                                SessionKind.HEAT -> "Sauna"
                                SessionKind.CONTRAST -> "Contrast session"
                            } + " (manual)",
                            totalSec = cold + heat,
                            coldSec = cold,
                            heatSec = heat,
                            rounds = if (kind == SessionKind.CONTRAST) rounds else 1,
                            coldTempC = if (hasCold) coldTemp else null,
                            heatTempC = if (hasHeat) heatTemp else null,
                            mood = null,
                            note = "",
                        ),
                    )
                },
                icon = com.superdevlabs.coldplunge.ui.components.KIcons.Check,
                modifier = Modifier.fillMaxWidth(),
            )
            SecondaryButton("Cancel", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}
