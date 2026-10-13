package com.snaptensio.bplog.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.snaptensio.bplog.data.AppSettings
import com.snaptensio.bplog.data.Guideline
import com.snaptensio.bplog.domain.DAY_MS
import com.snaptensio.bplog.domain.Severity
import com.snaptensio.bplog.domain.computeStats
import com.snaptensio.bplog.ui.MainViewModel
import com.snaptensio.bplog.ui.components.CategoryChip
import com.snaptensio.bplog.ui.components.ChartMarker
import com.snaptensio.bplog.ui.components.EmptyState
import com.snaptensio.bplog.ui.components.ScreenHeader
import com.snaptensio.bplog.ui.components.SectionCard
import com.snaptensio.bplog.ui.components.Segmented
import com.snaptensio.bplog.ui.components.SeverityBarRow
import com.snaptensio.bplog.ui.components.StatTile
import com.snaptensio.bplog.ui.components.TrendChart
import com.snaptensio.bplog.domain.BpCategory
import com.snaptensio.bplog.domain.BpClassifier
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val periods = listOf(7, 30, 90, 365)

fun severityLabel(s: Severity, g: Guideline): String = when (s) {
    Severity.LOW -> "Low"
    Severity.NORMAL -> "Normal"
    Severity.ELEVATED -> if (g == Guideline.AHA) "Elevated" else "High normal"
    Severity.STAGE1 -> if (g == Guideline.AHA) "Stage 1" else "Grade 1"
    Severity.STAGE2 -> if (g == Guideline.AHA) "Stage 2" else "Grade 2–3"
    Severity.CRISIS -> "Very high"
}

@Composable
fun TrendsScreen(vm: MainViewModel, onInsights: () -> Unit, onReport: () -> Unit) {
    val readings by vm.readings.collectAsStateWithLifecycle()
    val profile by vm.activeProfile.collectAsStateWithLifecycle()
    val meds by vm.medications.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val s = settings ?: AppSettings()
    var days by rememberSaveable { mutableIntStateOf(30) }
    val p = profile

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Trends") {
            FilledTonalButton(onClick = onInsights) {
                Icon(Icons.Filled.Lightbulb, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Insights")
            }
        }
        if (p == null || readings.isEmpty()) {
            EmptyState("No trends yet", "After a few readings you'll see your averages, ranges and patterns here.")
            return@Column
        }
        val stats = remember(readings, p, s.guideline, days) { computeStats(readings, p, s.guideline, days) }
        val inPeriod = remember(readings, days) { readings.filter { it.timestamp >= System.currentTimeMillis() - days * DAY_MS } }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Segmented(periods, days, { if (it == 365) "1Y" else "${it}D" }, { days = it }, Modifier.fillMaxWidth())
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("$days-day average", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            stats.average?.let { "${it.sys}/${it.dia}" } ?: "—",
                            style = MaterialTheme.typography.headlineLarge,
                        )
                    }
                    stats.average?.let { CategoryChip(BpClassifier.classify(it.sys, it.dia, s.guideline)) }
                }
                stats.changeVsPrevious?.let { d ->
                    val better = d <= 0
                    CategoryChip(
                        BpCategory(
                            (if (d > 0) "+$d" else "−${-d}") + " systolic vs previous $days days",
                            if (better) Severity.NORMAL else Severity.STAGE1,
                            "",
                        ),
                    )
                }
                TrendChart(
                    readings = inPeriod,
                    targetSys = p.targetSystolic,
                    targetDia = p.targetDiastolic,
                    days = days,
                    markers = meds.map { ChartMarker(it.startDate, "Started ${it.name}") },
                )
            }
            if (stats.distribution.isNotEmpty()) {
                SectionCard {
                    Text("Time in each range", style = MaterialTheme.typography.titleMedium)
                    SeverityBarRow(stats.distribution) { severityLabel(it, s.guideline) }
                }
            }
            SectionCard {
                Row {
                    StatTile("Morning", stats.morning?.toString() ?: "—", Modifier.weight(1f))
                    StatTile("Evening", stats.evening?.toString() ?: "—", Modifier.weight(1f))
                    StatTile("Avg pulse", stats.average?.pulse?.toString() ?: "—", Modifier.weight(1f))
                }
                Row {
                    val df = SimpleDateFormat("d MMM", Locale.getDefault())
                    StatTile("Highest", stats.max?.let { "${it.systolic}/${it.diastolic}" } ?: "—", Modifier.weight(1f), stats.max?.let { df.format(Date(it.timestamp)) })
                    StatTile("Lowest", stats.min?.let { "${it.systolic}/${it.diastolic}" } ?: "—", Modifier.weight(1f), stats.min?.let { df.format(Date(it.timestamp)) })
                    StatTile("In target", "${stats.inTargetPercent}%", Modifier.weight(1f), "below ${p.targetSystolic}/${p.targetDiastolic}")
                }
            }
            FilledTonalButton(onClick = onReport, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Create report for your doctor")
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
