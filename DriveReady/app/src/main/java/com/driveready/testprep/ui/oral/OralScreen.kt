package com.driveready.testprep.ui.oral

import android.speech.tts.TextToSpeech
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.driveready.testprep.ads.NativeAdCard
import com.driveready.testprep.data.model.Country
import com.driveready.testprep.ui.components.AppCard
import com.driveready.testprep.ui.components.AppIcons
import com.driveready.testprep.ui.components.Pill
import com.driveready.testprep.ui.components.PrimaryButton
import com.driveready.testprep.ui.components.ScreenHeader
import com.driveready.testprep.ui.components.SecondaryButton
import com.driveready.testprep.ui.theme.Brand
import java.util.Locale

/**
 * Oral-test coach: the examiner's question is read aloud (on-device text-to-speech, no
 * permissions needed), the learner answers out loud, then compares with the model answer.
 */
@Composable
fun OralScreen(
    country: Country,
    confident: Set<String>,
    contentPadding: PaddingValues,
    onToggleConfident: (Int) -> Unit
) {
    val oral = country.oral
    val tags = remember(country.id) { oral.questions.map { it.tag }.distinct() }
    var tag by rememberSaveable(country.id) { mutableStateOf<String?>(null) }
    val indices = oral.questions.indices.filter { tag == null || oral.questions[it].tag == tag }
    var pos by rememberSaveable(country.id, tag) { mutableIntStateOf(0) }
    var revealed by rememberSaveable(country.id, tag, pos) { mutableStateOf(false) }
    val qIndex = indices.getOrNull(pos.coerceIn(0, (indices.size - 1).coerceAtLeast(0)))

    val context = LocalContext.current
    var ttsReady by remember { mutableStateOf(false) }
    val tts: TextToSpeech = remember {
        val holder = arrayOfNulls<TextToSpeech>(1)
        TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                holder[0]?.language = Locale.getDefault()
                ttsReady = true
            }
        }.also { holder[0] = it }
    }
    DisposableEffect(Unit) { onDispose { tts.stop(); tts.shutdown() } }

    Column(
        Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
    ) {
        ScreenHeader("Oral test", oral.name)
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(oral.description, style = MaterialTheme.typography.bodyMedium, color = Brand.Muted)

            if (tags.size > 1) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = tag == null, onClick = { tag = null }, label = { Text("All ${oral.questions.size}") })
                    tags.forEach { t ->
                        FilterChip(selected = tag == t, onClick = { tag = t }, label = { Text(t) })
                    }
                }
            }

            if (qIndex != null) {
                val q = oral.questions[qIndex]
                val key = "${country.id}:$qIndex"
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Pill(q.tag, Brand.BlueSoft, Brand.Blue)
                        Spacer(Modifier.weight(1f))
                        Text("${pos + 1} of ${indices.size}", style = MaterialTheme.typography.labelMedium, color = Brand.Muted)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(q.question, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SecondaryButton(
                            "Hear examiner",
                            { if (ttsReady) tts.speak(q.question, TextToSpeech.QUEUE_FLUSH, null, "q$qIndex") },
                            Modifier.weight(1f),
                            icon = AppIcons.Speaker
                        )
                        PrimaryButton(if (revealed) "Hide answer" else "Model answer", { revealed = !revealed }, Modifier.weight(1f), icon = AppIcons.Eye)
                    }
                    AnimatedVisibility(revealed) {
                        Column(
                            Modifier
                                .padding(top = 14.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Brand.Surface)
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            q.answer.forEach { line ->
                                Row {
                                    Icon(AppIcons.Check, contentDescription = null, tint = Brand.Good, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(line, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        onClick = { onToggleConfident(qIndex) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (key in confident) Brand.GoodSoft else Color(0xFFEDEBE4)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(AppIcons.Check, contentDescription = null, tint = if (key in confident) Brand.Good else Brand.Muted)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                if (key in confident) "I can answer this confidently" else "Mark as confident",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (key in confident) Brand.Good else Brand.Ink
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton("Previous", { if (pos > 0) pos-- }, Modifier.weight(1f))
                    PrimaryButton("Next question", { if (pos < indices.lastIndex) pos++ else pos = 0 }, Modifier.weight(1f))
                }
            }

            NativeAdCard()

            AppCard(color = Brand.Navy, border = false) {
                Text("Speaking technique", color = Brand.Amber, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                oral.techniques.forEachIndexed { i, tip ->
                    Row(Modifier.padding(vertical = 4.dp)) {
                        Text("${i + 1}", color = Brand.Amber, fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp))
                        Text(tip, color = Color(0xFFE4E6EA), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
