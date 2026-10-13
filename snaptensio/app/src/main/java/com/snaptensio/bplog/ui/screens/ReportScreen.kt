package com.snaptensio.bplog.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.snaptensio.bplog.export.Exporters
import com.snaptensio.bplog.export.ReportOptions
import com.snaptensio.bplog.ui.MainViewModel
import com.snaptensio.bplog.ui.components.ScreenHeader
import com.snaptensio.bplog.ui.components.SectionCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val ranges = listOf(14 to "2 weeks", 30 to "30 days", 90 to "3 months", -1 to "All")

@Composable
fun ReportScreen(vm: MainViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val readings by vm.readings.collectAsStateWithLifecycle()
    val meds by vm.medications.collectAsStateWithLifecycle()
    var range by rememberSaveable { mutableStateOf(30) }
    var chart by rememberSaveable { mutableStateOf(true) }
    var table by rememberSaveable { mutableStateOf(true) }
    var medsOn by rememberSaveable { mutableStateOf(true) }
    var notes by rememberSaveable { mutableStateOf(false) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val options = ReportOptions(chart, table, medsOn, notes)
    val days = range.takeIf { it > 0 }

    LaunchedEffect(range, chart, table, medsOn, notes, readings, meds) {
        val file = vm.buildPdf(days, options) ?: return@LaunchedEffect
        preview = withContext(Dispatchers.IO) { Exporters.renderFirstPage(file, 900) }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Report for your doctor", onBack = onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ranges.forEach { (d, label) -> FilterChip(selected = range == d, onClick = { range = d }, label = { Text(label) }) }
            }
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                val bmp = preview
                if (bmp == null) {
                    Box(Modifier.fillMaxWidth(0.7f).aspectRatio(595f / 842f), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else {
                    Image(
                        bmp.asImageBitmap(),
                        contentDescription = "Preview of the first page of your report",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth(0.82f).aspectRatio(bmp.width.toFloat() / bmp.height).background(Color.White).border(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    )
                }
            }
            SectionCard(padding = 6) {
                ToggleRow("Charts and averages", chart) { chart = it }
                ToggleRow("Every reading (table)", table) { table = it }
                ToggleRow("Medications", medsOn) { medsOn = it }
                ToggleRow("Notes", notes) { notes = it }
            }
            Text("The report is made on your phone. It is only shared when you choose an app to send it with.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        busy = true
                        vm.buildCsv()?.let { Exporters.share(context, it, "text/csv", "Blood pressure readings (CSV)") }
                        busy = false
                    }
                },
                enabled = !busy && readings.isNotEmpty(),
                modifier = Modifier.height(54.dp),
            ) { Text("CSV") }
            Button(
                onClick = {
                    scope.launch {
                        busy = true
                        vm.buildPdf(days, options)?.let { Exporters.share(context, it, "application/pdf", "Blood pressure report") }
                        busy = false
                    }
                },
                enabled = !busy && readings.isNotEmpty(),
                modifier = Modifier.weight(1f).height(54.dp),
            ) {
                Icon(Icons.Filled.Share, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Share PDF")
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked, onChange)
    }
}
