package com.snaptensio.bplog.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.snaptensio.bplog.data.AppSettings
import kotlinx.coroutines.delay

private val CrisisBg = Color(0xFFFBEDEE)
private val CrisisInk = Color(0xFF2A0A0E)
private val CrisisRed = Color(0xFF7A1020)

private val symptoms = listOf("Chest pain", "Shortness of breath", "Back pain", "Numbness or weakness", "Change in vision", "Trouble speaking")

/**
 * Safety screen for readings over 180/120. No ads appear here. Guidance follows the American Heart
 * Association's advice: wait, re-measure, and call emergency services if there are symptoms.
 */
@Composable
fun CrisisScreen(
    sys: Int,
    dia: Int,
    settings: AppSettings,
    onMeasureAgain: () -> Unit,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    var selected by rememberSaveable { mutableStateOf(setOf<Int>()) }
    var timerEnd by rememberSaveable { mutableLongStateOf(0L) }
    var now by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(timerEnd) {
        while (System.currentTimeMillis() < timerEnd) {
            now = System.currentTimeMillis(); delay(500)
        }
        now = System.currentTimeMillis()
    }
    val left = ((timerEnd - now) / 1000).coerceAtLeast(0)
    val pulse = rememberInfiniteTransition(label = "alert")
    val scale by pulse.animateFloat(1f, 1.08f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "scale")

    fun dial(number: String) {
        if (number.isBlank()) return
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(number))))
    }

    Column(Modifier.fillMaxSize().background(CrisisBg)) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(56.dp).graphicsLayer { scaleX = scale; scaleY = scale }.clip(RoundedCornerShape(16.dp)).background(CrisisRed),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp)) }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Very high reading", style = MaterialTheme.typography.labelLarge, color = CrisisRed)
                    Text("$sys/$dia", style = MaterialTheme.typography.displaySmall, color = CrisisInk)
                }
            }
            Text("Sit still and measure again in 5 minutes", style = MaterialTheme.typography.headlineSmall, color = CrisisInk)
            Text(
                "One high number can come from a loose cuff, talking, or moving. If your second reading is still above 180/120, contact your doctor right away.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF4A1A20),
            )
            if (timerEnd == 0L || left == 0L) {
                OutlinedButton(
                    onClick = { if (timerEnd != 0L && left == 0L) onMeasureAgain() else timerEnd = System.currentTimeMillis() + 5 * 60_000 },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CrisisRed, containerColor = Color.White),
                ) {
                    Icon(Icons.Filled.Timer, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (timerEnd != 0L) "Measure again now" else "Start 5-minute timer")
                }
            } else {
                Text(
                    "Measure again in ${left / 60}:${(left % 60).toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.titleLarge,
                    color = CrisisRed,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).padding(16.dp),
                )
            }
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White).padding(8.dp),
            ) {
                Text("Do you have any of these right now?", style = MaterialTheme.typography.titleMedium, color = CrisisInk, modifier = Modifier.padding(8.dp))
                symptoms.forEachIndexed { i, s ->
                    val on = i in selected
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .toggleable(on, role = Role.Checkbox) { selected = if (it) selected + i else selected - i }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = on, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = CrisisRed))
                        Spacer(Modifier.width(8.dp))
                        Text(s, style = MaterialTheme.typography.bodyLarge, color = CrisisInk)
                    }
                }
            }
            if (selected.isNotEmpty()) {
                Text(
                    "With these symptoms, don't wait for a second reading. Call emergency services now.",
                    style = MaterialTheme.typography.titleMedium,
                    color = CrisisRed,
                )
            }
        }
        Column(Modifier.padding(horizontal = 22.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { dial(settings.emergencyNumber) },
                modifier = Modifier.fillMaxWidth().height(58.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CrisisRed, contentColor = Color.White),
            ) {
                Icon(Icons.Filled.Call, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Call emergency (${settings.emergencyNumber})")
            }
            if (settings.doctorNumber.isNotBlank()) {
                OutlinedButton(
                    onClick = { dial(settings.doctorNumber) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CrisisInk, containerColor = Color.White),
                ) { Text("Call my doctor") }
            }
            TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("I'm OK, back to home", color = CrisisInk) }
            Text(
                "SnapTensio does not diagnose. Set your emergency and doctor numbers in Settings.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF5C2A30),
            )
        }
    }
}
