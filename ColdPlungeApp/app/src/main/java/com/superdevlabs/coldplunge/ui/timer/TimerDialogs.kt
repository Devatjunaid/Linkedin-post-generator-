package com.superdevlabs.coldplunge.ui.timer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.superdevlabs.coldplunge.domain.Moods
import com.superdevlabs.coldplunge.domain.Units
import com.superdevlabs.coldplunge.ui.components.Chip
import com.superdevlabs.coldplunge.ui.components.PrimaryButton
import com.superdevlabs.coldplunge.ui.components.SecondaryButton
import com.superdevlabs.coldplunge.ui.components.SectionLabel
import com.superdevlabs.coldplunge.ui.components.StatTile
import com.superdevlabs.coldplunge.ui.components.Stepper
import com.superdevlabs.coldplunge.ui.components.ThermalButton
import com.superdevlabs.coldplunge.ui.theme.K
import com.superdevlabs.coldplunge.ui.theme.KType
import kotlin.math.cos
import kotlin.math.sin

@Composable
private fun DialogCard(onDismiss: () -> Unit, dismissible: Boolean = true, content: @Composable () -> Unit) {
    Dialog(
        onDismissRequest = { if (dismissible) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = dismissible, dismissOnBackPress = dismissible),
    ) {
        val enter = remember { Animatable(0f) }
        LaunchedEffect(Unit) { enter.animateTo(1f, spring(dampingRatio = 0.65f, stiffness = 300f)) }
        Box(
            Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = enter.value.coerceIn(0f, 1f)
                    scaleX = 0.9f + 0.1f * enter.value
                    scaleY = 0.9f + 0.1f * enter.value
                }
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF16202B), K.InkDeep)))
                .border(1.dp, K.Line, RoundedCornerShape(28.dp)),
        ) { content() }
    }
}

@Composable
fun EndSessionDialog(hasProgress: Boolean, onKeepGoing: () -> Unit, onSave: () -> Unit, onDiscard: () -> Unit) {
    DialogCard(onDismiss = onKeepGoing) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("End this session?", style = KType.Title.copy(fontSize = 22.sp))
            Text(
                if (hasProgress) "You can log the time you've done so far, or discard it."
                else "Nothing has been timed yet.",
                style = KType.Body.copy(color = K.Muted),
            )
            PrimaryButton("Keep going", onClick = onKeepGoing, modifier = Modifier.fillMaxWidth())
            if (hasProgress) SecondaryButton("End & log progress", onClick = onSave, modifier = Modifier.fillMaxWidth())
            SecondaryButton("Discard session", onClick = onDiscard, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** Celebration + logging step: a burst of ice and ember particles, totals, mood, temperatures and a note. */
@Composable
fun SessionSummaryDialog(
    totalSec: Int,
    coldSec: Int,
    heatSec: Int,
    rounds: Int,
    initialColdTemp: Float?,
    initialHeatTemp: Float?,
    fahrenheit: Boolean,
    onSave: (mood: Int?, note: String, coldTempC: Float?, heatTempC: Float?) -> Unit,
    onDiscard: () -> Unit,
) {
    var mood by remember { mutableStateOf<Int?>(null) }
    var note by remember { mutableStateOf("") }
    var coldTemp by remember { mutableStateOf(initialColdTemp) }
    var heatTemp by remember { mutableStateOf(initialHeatTemp) }
    val burst = remember { Animatable(0f) }
    LaunchedEffect(Unit) { burst.animateTo(1f, tween(1400, easing = FastOutSlowInEasing)) }

    DialogCard(onDismiss = {}, dismissible = false) {
        Box {
            Canvas(Modifier.fillMaxWidth().height(180.dp)) {
                val c = Offset(size.width / 2f, size.height * 0.55f)
                val p = burst.value
                repeat(36) { i ->
                    val angle = i * (360f / 36f) * (Math.PI / 180f).toFloat()
                    val dist = size.width * 0.45f * p * (0.6f + (i % 5) * 0.1f)
                    val pos = c + Offset(cos(angle), sin(angle)) * dist
                    drawCircle(if (i % 2 == 0) K.Ice else K.Ember, 3f + (i % 3), pos, alpha = (1f - p).coerceIn(0f, 1f))
                }
            }
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Session complete", style = KType.Title, textAlign = TextAlign.Start)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("Total", Units.clock(totalSec), Modifier.weight(1f))
                    if (coldSec > 0) StatTile("Cold", Units.clock(coldSec), Modifier.weight(1f), valueColor = K.Ice)
                    if (heatSec > 0) StatTile("Heat", Units.clock(heatSec), Modifier.weight(1f), valueColor = K.Ember)
                }
                if (rounds > 1) Text("$rounds rounds completed", style = KType.Small)

                SectionLabel("How do you feel?")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Moods.labels.forEachIndexed { i, label ->
                        Chip(label, selected = mood == i + 1, onClick = { mood = if (mood == i + 1) null else i + 1 }, accent = if (i >= 3) K.Ice else K.Text)
                    }
                }

                if (coldSec > 0 && coldTemp != null) {
                    TempRow("Water temperature", coldTemp!!, fahrenheit, 0.5f, 0f, 25f) { coldTemp = it }
                }
                if (heatSec > 0 && heatTemp != null) {
                    TempRow("Sauna temperature", heatTemp!!, fahrenheit, 5f, 40f, 120f) { heatTemp = it }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= 280) note = it },
                    label = { Text("Note (optional)") },
                    placeholder = { Text("e.g. felt easier on round 3") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = KType.Body,
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
                ThermalButton("Save to log", onClick = { onSave(mood, note, coldTemp, heatTemp) }, modifier = Modifier.fillMaxWidth(), icon = null)
                SecondaryButton("Discard", onClick = onDiscard, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun TempRow(label: String, value: Float, fahrenheit: Boolean, step: Float, min: Float, max: Float, onChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            SectionLabel(label)
            Text(Units.temp(value, fahrenheit), style = KType.Mono)
        }
        Stepper(
            value = "",
            onDecrease = { onChange((value - step).coerceAtLeast(min)) },
            onIncrease = { onChange((value + step).coerceAtMost(max)) },
            decreaseLabel = "Lower $label",
            increaseLabel = "Raise $label",
            valueWidth = 8.dp,
        )
    }
}
