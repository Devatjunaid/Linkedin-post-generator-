package com.glowdash.warningidentifier.ui.result

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.glowdash.warningidentifier.ads.AdsManager
import com.glowdash.warningidentifier.ads.NativeAdCard
import com.glowdash.warningidentifier.data.HistoryStore
import com.glowdash.warningidentifier.data.LightsCatalog
import com.glowdash.warningidentifier.data.WarningLight
import com.glowdash.warningidentifier.ui.components.AppCard
import com.glowdash.warningidentifier.ui.components.LightIcon
import com.glowdash.warningidentifier.ui.components.PathIcon
import com.glowdash.warningidentifier.ui.components.Pressable
import com.glowdash.warningidentifier.ui.components.RoundIconButton
import com.glowdash.warningidentifier.ui.components.ScreenBackground
import com.glowdash.warningidentifier.ui.components.SeverityPill
import com.glowdash.warningidentifier.ui.components.ShineButton
import com.glowdash.warningidentifier.ui.components.UiIcons
import com.glowdash.warningidentifier.ui.components.findActivity
import com.glowdash.warningidentifier.ui.theme.WiColors
import com.glowdash.warningidentifier.ui.theme.WiType
import kotlinx.coroutines.delay

@Composable
fun LightDetailScreen(
    lightId: String,
    confidence: Int,
    fromScan: Boolean,
    alternatives: List<WarningLight>,
    onBack: () -> Unit,
    onOpenLight: (String) -> Unit,
) {
    val light = remember(lightId) { LightsCatalog.find(lightId) }
    if (light == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val context = LocalContext.current
    val activity = context.findActivity()

    var recorded by rememberSaveable(lightId) { mutableStateOf(false) }
    LaunchedEffect(lightId) {
        if (fromScan && !recorded) {
            HistoryStore.add(lightId, confidence)
            recorded = true
        }
    }

    val tts = rememberTts()

    ScreenBackground(topGlow = light.color.color, bottomGlow = WiColors.Violet) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(UiIcons.Back, "Back", onBack)
                Text(
                    if (fromScan) "SCAN RESULT" else "LIGHT DETAILS",
                    style = WiType.Label.copy(color = WiColors.TextSecondary, textAlign = TextAlign.Center),
                    modifier = Modifier.weight(1f),
                )
                RoundIconButton(
                    UiIcons.Speaker, "Read aloud",
                    onClick = { tts.speak(spokenText(light)) },
                    tint = WiColors.Pink,
                )
            }

            Hero(light, confidence, Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(18.dp))
            SeverityMeter(light)

            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoCard(UiIcons.Info, WiColors.Cyan, "What it means", delayMs = 100) {
                    Text(light.meaning, style = WiType.BodySmall)
                }
                InfoCard(UiIcons.Check, WiColors.Lime, "What to do", delayMs = 220) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        light.steps.forEachIndexed { i, step ->
                            Text("${i + 1}. $step", style = WiType.BodySmall)
                        }
                    }
                }

                NativeAdCard("detail")

                InfoCard(UiIcons.Car, light.severity.color, "Can I keep driving?", delayMs = 340) {
                    Text(light.canDrive, style = WiType.BodySmall)
                }
                light.flashingNote?.let { note -> FlashingCard(note) }

                if (alternatives.isNotEmpty()) {
                    Text("Not it? It could also be", style = WiType.Section, modifier = Modifier.padding(top = 8.dp))
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        alternatives.forEach { alt ->
                            Pressable(onClick = { onOpenLight(alt.id) }, shape = RoundedCornerShape(16.dp)) {
                                Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    LightIcon(alt, size = 22.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text(alt.name, style = WiType.Caption.copy(color = WiColors.TextPrimary))
                                }
                            }
                        }
                    }
                }

                Text(
                    "General guidance only — symbols vary between makes. Check your owner's manual and consult a mechanic if unsure.",
                    style = WiType.Caption.copy(color = WiColors.TextMuted),
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Spacer(Modifier.height(100.dp))
        }

        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, WiColors.Bg, WiColors.Bg)))
                .padding(horizontal = 22.dp, vertical = 14.dp),
        ) {
            ShineButton(
                text = "Find Mechanic Nearby",
                icon = UiIcons.Pin,
                onClick = { AdsManager.showInterstitialThen(activity) { openNearbyMechanics(context) } },
                modifier = Modifier.fillMaxWidth(),
                color = WiColors.Pink,
                contentColor = Color.White,
            )
        }
    }
}

@Composable
private fun Hero(light: WarningLight, confidence: Int, modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "halo")
    val glow by t.animateFloat(0.35f, 0.75f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "glow")
    val c = light.color.color
    Column(modifier.padding(top = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(116.dp)
                .shadow(34.dp, RoundedCornerShape(34.dp), ambientColor = c.copy(alpha = glow), spotColor = c.copy(alpha = glow))
                .clip(RoundedCornerShape(34.dp))
                .background(WiColors.Surface)
                .border(2.dp, c.copy(alpha = 0.5f + glow / 2f), RoundedCornerShape(34.dp)),
            contentAlignment = Alignment.Center,
        ) {
            LightIcon(light, size = 68.dp)
        }
        Spacer(Modifier.height(16.dp))
        Text(light.name, style = WiType.Display.copy(textAlign = TextAlign.Center))
        val sub = if (confidence >= 0) "${light.aka} · $confidence% match" else light.aka
        Text(sub, style = WiType.Caption.copy(textAlign = TextAlign.Center), modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun SeverityMeter(light: WarningLight) {
    val fill = remember { Animatable(0f) }
    LaunchedEffect(light.id) { fill.animateTo(light.severity.meter, tween(1200, easing = FastOutSlowInEasing)) }
    AppCard(Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Severity", style = WiType.CardTitle, modifier = Modifier.weight(1f))
                SeverityPill(light.severity)
            }
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(WiColors.SurfaceHigh),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fill.value)
                        .height(10.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(Brush.horizontalGradient(listOf(WiColors.Lime, WiColors.Amber, WiColors.Red))),
                )
            }
            Spacer(Modifier.height(6.dp))
            Row {
                Text("Info", style = WiType.Caption.copy(color = WiColors.TextMuted), modifier = Modifier.weight(1f))
                Text("Caution", style = WiType.Caption.copy(color = WiColors.TextMuted), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("Stop now", style = WiType.Caption.copy(color = WiColors.TextMuted), modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }
        }
    }
}

@Composable
private fun InfoCard(
    icon: String,
    tint: Color,
    title: String,
    delayMs: Long,
    content: @Composable () -> Unit,
) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delayMs)
        appear.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
    }
    AppCard(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = appear.value
                translationY = (1f - appear.value) * 16.dp.toPx()
            },
    ) {
        Row {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) { PathIcon(icon, tint, size = 18.dp, strokeWidth = 2.2f) }
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = WiType.CardTitle)
                content()
            }
        }
    }
}

@Composable
private fun FlashingCard(note: String) {
    AppCard(
        Modifier.fillMaxWidth(),
        background = WiColors.Red.copy(alpha = 0.12f),
        border = WiColors.Red.copy(alpha = 0.45f),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PathIcon(UiIcons.Alert, Color(0xFFF08A9A), size = 22.dp, strokeWidth = 2.2f)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Flashing?", style = WiType.CardTitle)
                Text(note, style = WiType.BodySmall.copy(color = Color(0xFFF4C3CC)))
            }
        }
    }
}

private fun spokenText(light: WarningLight): String = buildString {
    append(light.name).append(". ")
    append(light.meaning).append(" ")
    append("What to do: ")
    light.steps.forEach { append(it).append(" ") }
    append("Can I keep driving? ").append(light.canDrive)
}

/** Built-in Android text-to-speech: free and works offline with installed voices. */
private class Tts(context: Context) {
    private var ready = false
    private val engine = TextToSpeech(context.applicationContext) { status ->
        ready = status == TextToSpeech.SUCCESS
    }

    fun speak(text: String) {
        if (ready) engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "light")
    }

    fun shutdown() {
        engine.stop()
        engine.shutdown()
    }
}

@Composable
private fun rememberTts(): Tts {
    val context = LocalContext.current
    val tts = remember { Tts(context) }
    DisposableEffect(tts) { onDispose { tts.shutdown() } }
    return tts
}

/** Opens the user's maps app for nearby garages — no maps API key needed. */
private fun openNearbyMechanics(context: Context) {
    val geo = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode("car repair")))
    try {
        context.startActivity(geo)
    } catch (e: ActivityNotFoundException) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.openstreetmap.org/search?query=car%20repair")),
            )
        }
    }
}
