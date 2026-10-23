package com.driveready.testprep.ui.practical

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.driveready.testprep.ads.NativeAdCard
import com.driveready.testprep.data.content.Maneuver
import com.driveready.testprep.data.content.Maneuvers
import com.driveready.testprep.data.model.Country
import com.driveready.testprep.data.model.DrivingSide
import com.driveready.testprep.ui.components.AppCard
import com.driveready.testprep.ui.components.AppIcons
import com.driveready.testprep.ui.components.IconTile
import com.driveready.testprep.ui.components.LinearMeter
import com.driveready.testprep.ui.components.ManeuverDiagram
import com.driveready.testprep.ui.components.Pill
import com.driveready.testprep.ui.components.PrimaryButton
import com.driveready.testprep.ui.components.ScreenHeader
import com.driveready.testprep.ui.components.SecondaryButton
import com.driveready.testprep.ui.components.SectionTitle
import com.driveready.testprep.ui.theme.Brand

@Composable
fun PracticalScreen(
    country: Country,
    confident: Set<String>,
    contentPadding: PaddingValues,
    onOpenManeuver: (String) -> Unit,
    onTestDay: () -> Unit
) {
    val p = country.practical
    val maneuvers = p.maneuverIds.mapNotNull { Maneuvers[it] }
    Column(
        Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
    ) {
        ScreenHeader("Practical test", p.name)
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatBox(p.durationMinutes?.let { "~$it min" } ?: "Varies", "test length", Modifier.weight(1f))
                StatBox("${maneuvers.size}", "maneuvers to learn", Modifier.weight(1f))
            }
            AppCard(color = Brand.Navy, border = false) {
                Text("How you pass", color = Brand.Amber, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Text(p.passRule, color = Color.White, style = MaterialTheme.typography.bodyMedium)
            }

            SectionTitle("What happens, in order")
            AppCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                p.steps.forEachIndexed { i, step ->
                    Row(Modifier.padding(vertical = 10.dp)) {
                        Box(
                            Modifier.size(28.dp).clip(CircleShape).background(if (i % 2 == 0) Brand.Ink else Brand.Blue),
                            contentAlignment = Alignment.Center
                        ) { Text("${i + 1}", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium) }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(step.title, style = MaterialTheme.typography.titleSmall)
                            Text(step.detail, style = MaterialTheme.typography.bodySmall, color = Brand.Muted)
                        }
                    }
                }
            }

            SectionTitle("Technique guides")
            maneuvers.forEachIndexed { i, m ->
                val done = "${country.id}:${m.id}" in confident
                AppCard(onClick = { onOpenManeuver(m.id) }, contentPadding = PaddingValues(14.dp), color = if (done) Brand.GoodSoft else Brand.Card) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconTile(if (m.isTwoWheeler) AppIcons.Flag else AppIcons.Wheel, if (done) Color.White else Brand.AmberSoft)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(m.name, style = MaterialTheme.typography.titleSmall)
                            Text(if (done) "Confident" else "${m.steps.size} steps", style = MaterialTheme.typography.bodySmall, color = if (done) Brand.Good else Brand.Muted)
                        }
                        Icon(AppIcons.ChevronRight, contentDescription = null)
                    }
                }
                if (i == 2) NativeAdCard()
            }

            SectionTitle("Most common faults")
            AppCard {
                p.commonFaults.forEach { f ->
                    Row(Modifier.padding(vertical = 4.dp)) {
                        Icon(AppIcons.Warning, contentDescription = null, tint = Brand.Bad, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(f, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            SecondaryButton("Test day checklist", onTestDay, Modifier.fillMaxWidth(), icon = AppIcons.Doc)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun StatBox(value: String, label: String, modifier: Modifier) {
    AppCard(modifier = modifier) {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.bodySmall, color = Brand.Muted)
    }
}

@Composable
fun ManeuverScreen(
    country: Country,
    maneuver: Maneuver,
    confident: Boolean,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onToggleConfident: () -> Unit
) {
    var step by rememberSaveable(maneuver.id) { mutableIntStateOf(0) }
    var replay by rememberSaveable { mutableIntStateOf(0) }
    val mirror = country.drivingSide == DrivingSide.LEFT
    Column(
        Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
    ) {
        ScreenHeader(maneuver.name, maneuver.summary, onBack = onBack, actions = {
            IconButton(onClick = { replay++ }) { Icon(AppIcons.Replay, contentDescription = "Replay animation") }
        })
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ManeuverDiagram(maneuver, step, mirror, replay)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                maneuver.steps.indices.forEach { i ->
                    Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(50)).background(if (i <= step) Brand.Ink else Color(0xFFDCDAD2)))
                }
            }
            val s = maneuver.steps[step]
            AppCard {
                Text("STEP ${step + 1} OF ${maneuver.steps.size}", style = MaterialTheme.typography.labelMedium, color = Brand.Blue)
                Spacer(Modifier.height(6.dp))
                Text(s.title, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text(s.detail, style = MaterialTheme.typography.bodyLarge, color = Color(0xFF3F4249))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("Previous", { if (step > 0) step-- }, Modifier.weight(1f))
                PrimaryButton(
                    if (step == maneuver.steps.lastIndex) "Start again" else "Next step",
                    { if (step == maneuver.steps.lastIndex) { step = 0; replay++ } else step++ },
                    Modifier.weight(1.4f)
                )
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Brand.AmberSoft).padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(AppIcons.Lightbulb, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(
                    "Reference points vary by car. Confirm yours with your instructor. Diagram shown for ${if (mirror) "left" else "right"}-hand traffic.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            AppCard {
                Text("Examiners mark you down for", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                maneuver.faults.forEach { f ->
                    Row(Modifier.padding(vertical = 3.dp)) {
                        Icon(AppIcons.Warning, contentDescription = null, tint = Brand.Bad, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(f, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            NativeAdCard()
            Surface(onClick = onToggleConfident, shape = RoundedCornerShape(16.dp), color = if (confident) Brand.GoodSoft else Brand.Ink) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(AppIcons.Check, contentDescription = null, tint = if (confident) Brand.Good else Brand.Amber)
                    Spacer(Modifier.width(10.dp))
                    Text(if (confident) "Marked as confident" else "I can do this confidently", color = if (confident) Brand.Good else Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
fun TestDayScreen(
    country: Country,
    done: Set<String>,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onToggle: (String) -> Unit,
    onWarmUp: () -> Unit
) {
    val items = country.documents + listOf(
        "Sleep well and eat a light meal",
        "Arrive at least 10 minutes early",
        "Glasses or contact lenses if you need them",
        "Do a 5-minute oral warm-up"
    )
    val doneCount = items.count { "${country.id}:$it" in done }
    Column(
        Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
    ) {
        ScreenHeader("Test day", country.authority, onBack = onBack)
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppCard(color = Brand.Navy, border = false) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$doneCount/${items.size}", color = Brand.Amber, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.width(14.dp))
                    Text("items ready. Tick them off the night before so the morning is calm.", color = Color(0xFFD4D6DB), style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(10.dp))
                LinearMeter(doneCount.toFloat() / items.size, color = Brand.Amber, track = Color(0xFF2B3446))
            }
            items.forEach { item ->
                val checked = "${country.id}:$item" in done
                Surface(onClick = { onToggle(item) }, shape = RoundedCornerShape(16.dp), color = Brand.Card, border = androidx.compose.foundation.BorderStroke(1.dp, Brand.Line)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)).background(if (checked) Brand.Ink else Color(0xFFEDEBE4)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (checked) Icon(AppIcons.Check, contentDescription = null, tint = Brand.Amber, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(item, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    }
                }
            }
            AppCard(color = Brand.BlueSoft, border = false) {
                Text("Nerves? Box breathing", style = MaterialTheme.typography.titleSmall)
                Text("Breathe in for 4, hold for 4, out for 4, hold for 4. One minute in the waiting room steadies your hands.", style = MaterialTheme.typography.bodySmall)
            }
            country.tips.forEach { tip -> Pill(tip, Brand.AmberSoft) }
            PrimaryButton("Warm up: oral questions", onWarmUp, Modifier.fillMaxWidth(), icon = AppIcons.Mic)
            Spacer(Modifier.height(12.dp))
        }
    }
}
