package com.snaptensio.bplog.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import com.snaptensio.bplog.data.Reading
import com.snaptensio.bplog.data.Source
import com.snaptensio.bplog.domain.BpClassifier
import com.snaptensio.bplog.ui.MainViewModel
import com.snaptensio.bplog.ui.Routes
import com.snaptensio.bplog.ui.components.ScreenHeader
import com.snaptensio.bplog.ui.components.SectionCard
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val checklist = listOf(
    "Rested 5 minutes, back supported",
    "Feet flat, legs uncrossed",
    "Cuff on bare upper arm at heart level",
    "No caffeine, smoking or exercise for 30 min",
    "Empty bladder",
    "No talking during the reading",
)

private const val REST_SECONDS = 60
private const val TARGET_READINGS = 3

private data class Measured(val sys: Int, val dia: Int, val pulse: Int?)

private fun decode(s: String): List<Measured> = s.split(';').filter { it.isNotBlank() }.mapNotNull { part ->
    val v = part.split(',').map { it.toIntOrNull() }
    if (v.size < 3 || v[0] == null || v[1] == null) null else Measured(v[0]!!, v[1]!!, v[2]?.takeIf { it > 0 })
}

private fun encode(list: List<Measured>) = list.joinToString(";") { "${it.sys},${it.dia},${it.pulse ?: -1}" }

/**
 * Guided measurement following home-monitoring guidance: prepare, take up to 3 readings one minute
 * apart, then save their average (optionally ignoring the first, which is often highest).
 */
@Composable
fun SessionScreen(
    vm: MainViewModel,
    backStackEntry: NavBackStackEntry,
    onClose: () -> Unit,
    onScan: () -> Unit,
    onSaved: (Int, Int) -> Unit,
) {
    var checked by rememberSaveable { mutableStateOf(setOf<Int>()) }
    var started by rememberSaveable { mutableStateOf(false) }
    var data by rememberSaveable { mutableStateOf("") }
    var restUntil by rememberSaveable { mutableLongStateOf(0L) }
    var dropFirst by rememberSaveable { mutableStateOf(false) }
    var sys by rememberSaveable { mutableStateOf("") }
    var dia by rememberSaveable { mutableStateOf("") }
    var pulse by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val readings = decode(data)

    // A scan launched from this screen hands its numbers back through the saved state handle.
    val handle = backStackEntry.savedStateHandle
    LaunchedEffect(Unit) {
        handle.get<String>(Routes.SCAN_RESULT)?.let { r ->
            val v = r.split(',').map { it.toIntOrNull() }
            sys = v.getOrNull(0)?.toString() ?: ""
            dia = v.getOrNull(1)?.toString() ?: ""
            pulse = v.getOrNull(2)?.takeIf { it > 0 }?.toString() ?: ""
            handle.remove<String>(Routes.SCAN_RESULT)
        }
    }

    var now by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(restUntil) {
        while (System.currentTimeMillis() < restUntil) {
            now = System.currentTimeMillis()
            delay(250)
        }
        now = System.currentTimeMillis()
    }
    val remaining = ((restUntil - now) / 1000f).coerceAtLeast(0f)
    val resting = remaining > 0f

    fun addReading() {
        val s = sys.toIntOrNull(); val d = dia.toIntOrNull(); val p = pulse.toIntOrNull()
        val problem = BpClassifier.validate(s, d, p)
        if (problem != null) { error = problem; return }
        error = null
        val list = readings + Measured(s!!, d!!, p)
        data = encode(list)
        sys = ""; dia = ""; pulse = ""
        if (list.size < TARGET_READINGS) restUntil = System.currentTimeMillis() + REST_SECONDS * 1000L
    }

    fun save() {
        val used = if (dropFirst && readings.size >= 3) readings.drop(1) else readings
        if (used.isEmpty()) return
        val avgSys = used.map { it.sys }.average().roundToInt()
        val avgDia = used.map { it.dia }.average().roundToInt()
        val pulses = used.mapNotNull { it.pulse }
        vm.saveReading(
            Reading(
                profileId = 0,
                timestamp = System.currentTimeMillis(),
                systolic = avgSys,
                diastolic = avgDia,
                pulse = if (pulses.isEmpty()) null else pulses.average().roundToInt(),
                source = Source.SESSION.name,
                averagedCount = used.size,
                note = "Readings: " + readings.joinToString(", ") { "${it.sys}/${it.dia}" },
            ),
        ) { onSaved(avgSys, avgDia) }
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        ScreenHeader("Guided session", onBack = onClose)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (!started) {
                Text("Before you measure", style = MaterialTheme.typography.titleLarge)
                Text("These steps make home readings match what your doctor would measure.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SectionCard(padding = 6) {
                    checklist.forEachIndexed { i, item ->
                        val on = i in checked
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .toggleable(on, role = Role.Checkbox) { checked = if (it) checked + i else checked - i }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = on, onCheckedChange = null)
                            Spacer(Modifier.width(10.dp))
                            Text(item, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            } else {
                // Progress list
                SectionCard {
                    repeat(TARGET_READINGS) { i ->
                        val r = readings.getOrNull(i)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(30.dp).clip(CircleShape).background(
                                    if (r != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                ),
                                contentAlignment = Alignment.Center,
                            ) { Text("${i + 1}", style = MaterialTheme.typography.labelLarge) }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                if (r != null) "${r.sys}/${r.dia}" else if (i == readings.size) "Next" else "—",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.weight(1f),
                                color = if (r != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            r?.pulse?.let { Text("$it bpm", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }

                if (resting) {
                    RestTimer(remaining)
                } else if (readings.size < TARGET_READINGS) {
                    Text("Reading ${readings.size + 1} of $TARGET_READINGS", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NumberField("SYS", sys, { sys = it }, Modifier.weight(1f))
                        NumberField("DIA", dia, { dia = it }, Modifier.weight(1f))
                        NumberField("Pulse", pulse, { pulse = it }, Modifier.weight(1f), last = true)
                    }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onScan, modifier = Modifier.weight(1f).height(52.dp)) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Scan")
                        }
                        Button(onClick = ::addReading, modifier = Modifier.weight(1f).height(52.dp)) { Text("Add") }
                    }
                }

                if (readings.isNotEmpty()) {
                    val used = if (dropFirst && readings.size >= 3) readings.drop(1) else readings
                    SectionCard {
                        Text("Average so far", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "${used.map { it.sys }.average().roundToInt()}/${used.map { it.dia }.average().roundToInt()}",
                            style = MaterialTheme.typography.headlineLarge,
                        )
                        if (readings.size >= 3) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Ignore the first reading (often the highest)", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                Switch(dropFirst, { dropFirst = it })
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!started) {
                OutlinedButton(onClick = { started = true }, modifier = Modifier.weight(1f).height(54.dp)) { Text("Skip checklist") }
                Button(onClick = { started = true }, enabled = checked.size == checklist.size, modifier = Modifier.weight(1f).height(54.dp)) { Text("Start") }
            } else {
                if (resting) {
                    OutlinedButton(onClick = { restUntil = 0L }, modifier = Modifier.weight(1f).height(54.dp)) { Text("Skip wait") }
                }
                Button(
                    onClick = ::save,
                    enabled = readings.isNotEmpty(),
                    modifier = Modifier.weight(1f).height(54.dp),
                ) { Text(if (readings.size >= TARGET_READINGS) "Save average" else "Finish with ${readings.size}") }
            }
        }
    }
}

@Composable
private fun RestTimer(remaining: Float) {
    val fraction by animateFloatAsState(remaining / REST_SECONDS, tween(250), label = "rest")
    val track = MaterialTheme.colorScheme.surfaceVariant
    val bar = MaterialTheme.colorScheme.primary
    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(200.dp)) {
            val stroke = Stroke(14.dp.toPx(), cap = StrokeCap.Round)
            drawArc(track, 0f, 360f, false, style = stroke)
            drawArc(bar, -90f, 360f * fraction, false, style = stroke)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Rest, then measure in", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val secs = kotlin.math.ceil(remaining).toInt()
            Text("${secs / 60}:${(secs % 60).toString().padStart(2, '0')}", style = MaterialTheme.typography.displayMedium)
            Text("Stay seated, stay quiet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
