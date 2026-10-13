package com.snaptensio.bplog.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.snaptensio.bplog.ads.NativeAdCard
import com.snaptensio.bplog.data.AppSettings
import com.snaptensio.bplog.domain.BpClassifier
import com.snaptensio.bplog.domain.InsightTone
import com.snaptensio.bplog.domain.computeInsights
import com.snaptensio.bplog.ui.MainViewModel
import com.snaptensio.bplog.ui.components.EmptyState
import com.snaptensio.bplog.ui.components.ScreenHeader
import com.snaptensio.bplog.ui.components.SectionCard
import com.snaptensio.bplog.ui.components.readableColor

@Composable
fun InsightsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val readings by vm.readings.collectAsStateWithLifecycle()
    val meds by vm.medications.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val s = settings ?: AppSettings()
    val insights = remember(readings, meds) { computeInsights(readings, meds) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Insights", onBack = onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Patterns in your last 30 days, worked out on your phone with simple maths. No AI service, nothing uploaded.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (insights.isEmpty()) {
                EmptyState("Not enough readings yet", "Log at least 3 readings this month and insights will start to appear.")
            }
            insights.forEachIndexed { i, ins ->
                val (bg, fg) = when (ins.tone) {
                    InsightTone.GOOD -> Color(0xFFE7F4EC) to Color(0xFF14633F)
                    InsightTone.INFO -> Color(0xFFE8EEFC) to Color(0xFF1638A8)
                    InsightTone.WATCH -> Color(0xFFFDF0D5) to Color(0xFF7A4D06)
                }
                SectionCard {
                    Row(verticalAlignment = Alignment.Top) {
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(bg), contentAlignment = Alignment.Center) {
                            Text(ins.badge, style = MaterialTheme.typography.labelLarge, color = fg)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(ins.title, style = MaterialTheme.typography.titleMedium)
                            Text(ins.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (i == 1) NativeAdCard()
            }
            SectionCard {
                Text("What the ranges mean (${s.guideline.label})", style = MaterialTheme.typography.titleMedium)
                BpClassifier.legend(s.guideline).forEach { (label, sev, range) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Text(label, style = MaterialTheme.typography.titleSmall, color = sev.readableColor(), modifier = Modifier.weight(1f))
                        Text(range, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Text("Values are systolic/diastolic in mmHg. Change the guideline in Settings.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "Insights are observations, not diagnoses. Talk to your doctor before changing any treatment.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}
