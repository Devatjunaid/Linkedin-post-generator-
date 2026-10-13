package com.snaptensio.bplog.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.snaptensio.bplog.ads.NativeAdCard
import com.snaptensio.bplog.data.AppSettings
import com.snaptensio.bplog.data.Arm
import com.snaptensio.bplog.data.Position
import com.snaptensio.bplog.data.Source
import com.snaptensio.bplog.data.timeList
import com.snaptensio.bplog.domain.BpClassifier
import com.snaptensio.bplog.domain.DAY_MS
import com.snaptensio.bplog.domain.computeStats
import com.snaptensio.bplog.domain.dayKey
import com.snaptensio.bplog.ui.MainViewModel
import com.snaptensio.bplog.ui.Routes
import com.snaptensio.bplog.ui.components.AnimatedCount
import com.snaptensio.bplog.ui.components.AnimatedLogo
import com.snaptensio.bplog.ui.components.CategoryBar
import com.snaptensio.bplog.ui.components.CategoryChip
import com.snaptensio.bplog.ui.components.SectionCard
import com.snaptensio.bplog.ui.components.StatTile
import com.snaptensio.bplog.ui.components.TrendChart
import com.snaptensio.bplog.ui.findActivity
import com.snaptensio.bplog.ui.theme.Brand
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(vm: MainViewModel, nav: NavHostController) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val profile by vm.activeProfile.collectAsStateWithLifecycle()
    val profiles by vm.profiles.collectAsStateWithLifecycle()
    val readings by vm.readings.collectAsStateWithLifecycle()
    val meds by vm.medications.collectAsStateWithLifecycle()
    val reminders by vm.reminders.collectAsStateWithLifecycle()
    val s = settings ?: AppSettings()
    val p = profile
    var showExit by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    BackHandler { showExit = true }
    if (showExit) {
        val today = dayKey(System.currentTimeMillis())
        ExitDialog(
            todayCount = readings.count { dayKey(it.timestamp) == today },
            onStay = { showExit = false },
            onExit = { context.findActivity()?.finish() },
        )
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Header with profile switcher
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date()),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                var open by remember { mutableStateOf(false) }
                Box {
                    TextButton(onClick = { open = true }, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                        Text(p?.name ?: "Me", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Switch person", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    DropdownMenu(open, onDismissRequest = { open = false }) {
                        profiles.forEach { pr ->
                            DropdownMenuItem(text = { Text(pr.name, fontWeight = if (pr.id == p?.id) FontWeight.ExtraBold else FontWeight.Normal) }, onClick = {
                                vm.switchProfile(pr.id); open = false
                            })
                        }
                        DropdownMenuItem(text = { Text("Manage people…") }, onClick = { open = false; nav.navigate(Routes.SETTINGS) })
                    }
                }
            }
            IconButton(onClick = { nav.navigate(Routes.SETTINGS) }) { Icon(Icons.Filled.Settings, contentDescription = "Settings") }
        }

        val latest = readings.firstOrNull()
        if (latest == null) {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    AnimatedLogo(size = 64.dp, intro = false)
                    Column(Modifier.weight(1f)) {
                        Text("No readings yet", style = MaterialTheme.typography.titleLarge)
                        Text("Tap Scan monitor, point at your BP monitor's screen, and the numbers fill in by themselves.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            val cat = BpClassifier.classify(latest.systolic, latest.diastolic, s.guideline)
            SectionCard(onClick = { nav.navigate(Routes.edit(id = latest.id)) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Latest · " + relativeTime(latest.timestamp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    CategoryChip(cat)
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom) {
                        AnimatedCount(latest.systolic, MaterialTheme.typography.displayLarge)
                        Text("/", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.outline)
                        AnimatedCount(latest.diastolic, MaterialTheme.typography.displayLarge)
                        Spacer(Modifier.width(6.dp))
                        Text("mmHg", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 10.dp))
                    }
                    latest.pulse?.let {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
                            Icon(Icons.Filled.Favorite, contentDescription = null, tint = Brand.Coral, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("$it bpm", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                CategoryBar(latest.systolic, latest.diastolic, s.guideline)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val chips = buildList {
                        add(Arm.valueOf(latest.arm).label + " arm")
                        add(Position.valueOf(latest.position).label)
                        if (latest.source == Source.SCAN.name) add("Scanned")
                        if (latest.averagedCount > 1) add("Avg of ${latest.averagedCount}")
                    }
                    chips.forEach {
                        Text(
                            it,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { nav.navigate(Routes.scan()) }, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Scan monitor")
            }
            OutlinedButton(onClick = { nav.navigate(Routes.edit()) }, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Filled.Edit, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Type it in")
            }
        }

        Surface(
            onClick = { nav.navigate(Routes.SESSION) },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Guided session", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("3 readings, 1 min apart, averaged for accuracy", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }

        if (p != null && readings.isNotEmpty()) {
            val stats = remember(readings, p, s.guideline) { computeStats(readings, p, s.guideline, 7) }
            SectionCard(onClick = { nav.navigate(Routes.TRENDS) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Last 7 days", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text("See trends", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                Row {
                    StatTile("Average", stats.average?.toString() ?: "—", Modifier.weight(1f))
                    StatTile("In target", if (stats.average == null) "—" else "${stats.inTargetPercent}%", Modifier.weight(1f))
                    StatTile("Readings", (stats.average?.count ?: 0).toString(), Modifier.weight(1f))
                }
                TrendChart(
                    readings = readings.filter { it.timestamp >= System.currentTimeMillis() - 7 * DAY_MS },
                    targetSys = p.targetSystolic,
                    targetDia = p.targetDiastolic,
                    days = 7,
                    height = 90.dp,
                    showAxis = false,
                )
            }
        }

        // Native ad sits between content cards, never next to the primary buttons.
        NativeAdCard()

        val upcoming = remember(reminders, meds) { upcomingItems(reminders.filter { it.enabled }.map { hhmm(it.hour, it.minute) to "${it.label} reading" }, meds.filter { it.active }.flatMap { m -> m.timeList().map { t -> t to "${m.name} ${m.dose}".trim() } }) }
        if (upcoming.isNotEmpty()) {
            SectionCard(onClick = { nav.navigate(Routes.MEDS) }) {
                Text("Coming up today", style = MaterialTheme.typography.titleMedium)
                upcoming.take(3).forEach { (time, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                        Spacer(Modifier.width(10.dp))
                        Text(time, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(56.dp))
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

private fun upcomingItems(vararg lists: List<Pair<String, String>>): List<Pair<String, String>> {
    val now = Calendar.getInstance()
    val nowStr = hhmm(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
    return lists.flatMap { it }.filter { it.first >= nowStr }.sortedBy { it.first }
}

fun relativeTime(ts: Long): String {
    val today = dayKey(System.currentTimeMillis())
    val yesterday = dayKey(System.currentTimeMillis() - DAY_MS)
    val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ts))
    return when (dayKey(ts)) {
        today -> "Today $time"
        yesterday -> "Yesterday $time"
        else -> SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(ts)) + " $time"
    }
}
