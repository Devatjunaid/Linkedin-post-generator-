package com.glowdash.warningidentifier.ui.home

import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.glowdash.warningidentifier.ads.AdsManager
import com.glowdash.warningidentifier.ads.NativeAdCard
import com.glowdash.warningidentifier.ai.BitmapUtils
import com.glowdash.warningidentifier.ai.LightClassifier
import com.glowdash.warningidentifier.ai.ScanSession
import com.glowdash.warningidentifier.data.HistoryEntry
import com.glowdash.warningidentifier.data.HistoryStore
import com.glowdash.warningidentifier.data.Severity
import com.glowdash.warningidentifier.ui.components.AnalyzingOverlay
import com.glowdash.warningidentifier.ui.components.FloatingNavBar
import com.glowdash.warningidentifier.ui.components.LightIcon
import com.glowdash.warningidentifier.ui.components.NavTab
import com.glowdash.warningidentifier.ui.components.PathIcon
import com.glowdash.warningidentifier.ui.components.Pressable
import com.glowdash.warningidentifier.ui.components.ScreenBackground
import com.glowdash.warningidentifier.ui.components.SeverityPill
import com.glowdash.warningidentifier.ui.components.ShineButton
import com.glowdash.warningidentifier.ui.components.UiIcons
import com.glowdash.warningidentifier.ui.components.findActivity
import com.glowdash.warningidentifier.ui.theme.WiColors
import com.glowdash.warningidentifier.ui.theme.WiType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

@Composable
fun HomeScreen(
    onScan: () -> Unit,
    onScanResult: () -> Unit,
    onOpenLight: (String) -> Unit,
    onTab: (NavTab) -> Unit,
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val scope = rememberCoroutineScope()
    val history by HistoryStore.items.collectAsState()
    var showExit by rememberSaveable { mutableStateOf(false) }
    var analyzing by remember { mutableStateOf(false) }

    // Warm up the exit-dialog ad so it appears instantly when needed.
    val adsReady by AdsManager.ready.collectAsState()
    LaunchedEffect(adsReady) { if (adsReady) AdsManager.prefetchNative(context, "exit") }

    BackHandler { showExit = true }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        analyzing = true
        scope.launch {
            val analysis = withContext(Dispatchers.Default) {
                BitmapUtils.decode(context, uri)?.let { LightClassifier.get(context).analyze(it) }
            }
            analyzing = false
            if (analysis != null) {
                ScanSession.latest = analysis
                AdsManager.showInterstitialThen(activity) { onScanResult() }
            }
        }
    }

    ScreenBackground {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
        ) {
            Header()
            Spacer(Modifier.height(10.dp))
            HeroOrb(Modifier.align(Alignment.CenterHorizontally))
            Text(
                "What's that light?",
                style = WiType.Display.copy(textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            )
            Text(
                "Point your camera at the dashboard. AI identifies it instantly — fully offline.",
                style = WiType.Body.copy(textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp, start = 12.dp, end = 12.dp),
            )
            Spacer(Modifier.height(20.dp))
            ShineButton("Scan Dashboard", UiIcons.Scan, onScan, Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("From Photo", UiIcons.Gallery, WiColors.Pink, Modifier.weight(1f)) {
                    pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
                SecondaryButton("Light Library", UiIcons.Book, WiColors.Amber, Modifier.weight(1f)) {
                    onTab(NavTab.LIBRARY)
                }
            }

            NativeAdCard("home", Modifier.padding(top = 18.dp))

            Spacer(Modifier.height(22.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Recent scans", style = WiType.Section, modifier = Modifier.weight(1f))
                if (history.isNotEmpty()) {
                    Text(
                        "See all",
                        style = WiType.Caption.copy(color = WiColors.Cyan),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onTab(NavTab.HISTORY) }
                            .padding(horizontal = 6.dp, vertical = 10.dp),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            if (history.isEmpty()) {
                EmptyRecent()
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    history.take(3).forEach { entry -> RecentRow(entry) { onOpenLight(entry.light.id) } }
                }
            }
            Spacer(Modifier.height(110.dp))
        }

        FloatingNavBar(
            current = NavTab.HOME,
            onTab = onTab,
            onScan = onScan,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
        AnalyzingOverlay(analyzing, "Analyzing photo…")
    }

    if (showExit) {
        ExitDialog(
            onStay = { showExit = false },
            onExit = { activity?.finish() },
        )
    }
}

@Composable
private fun Header() {
    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "GOOD MORNING, DRIVER"
            in 12..17 -> "GOOD AFTERNOON, DRIVER"
            else -> "GOOD EVENING, DRIVER"
        }
    }
    Column(Modifier.padding(top = 20.dp)) {
        Text(greeting, style = WiType.Label.copy(color = WiColors.TextSecondary))
        Text(
            buildAnnotatedString {
                append("Warning ")
                withStyle(SpanStyle(color = WiColors.Cyan)) { append("Identifier") }
            },
            style = WiType.Title.copy(fontSize = WiType.Display.fontSize),
        )
    }
}

@Composable
private fun HeroOrb(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "hero")
    val rot by t.animateFloat(0f, 360f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "rot")
    val ripple by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearOutSlowInEasing)), label = "rip")
    val pulse by t.animateFloat(0.96f, 1.04f, infiniteRepeatable(tween(1300), RepeatMode.Reverse), label = "pulse")

    Box(modifier.size(220.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            for (i in 0..1) {
                val p = (ripple + i * 0.5f) % 1f
                drawCircle(
                    color = (if (i == 0) WiColors.Cyan else WiColors.Pink).copy(alpha = (1f - p) * 0.45f),
                    radius = r * (0.5f + 0.45f * p),
                    style = Stroke(2.dp.toPx()),
                )
            }
            rotate(rot) {
                drawCircle(
                    WiColors.StrokeStrong, radius = r - 3.dp.toPx(),
                    style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 12f))),
                )
                drawArc(
                    WiColors.Cyan, -90f, 95f, false,
                    topLeft = Offset(3.dp.toPx(), 3.dp.toPx()),
                    size = Size(size.width - 6.dp.toPx(), size.height - 6.dp.toPx()),
                    style = Stroke(3.dp.toPx(), cap = StrokeCap.Round),
                )
            }
            rotate(-rot * 0.7f) {
                val inset = 18.dp.toPx()
                val s = Size(size.width - inset * 2, size.height - inset * 2)
                drawArc(WiColors.Pink, 100f, 60f, false, Offset(inset, inset), s, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                drawArc(WiColors.Amber, 200f, 45f, false, Offset(inset, inset), s, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
            }
        }
        Box(
            Modifier
                .size(134.dp)
                .graphicsLayer { scaleX = pulse; scaleY = pulse }
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFF3FB7D6), Color(0xFF2F55C9), Color(0xFF4A2A93)))),
            contentAlignment = Alignment.Center,
        ) {
            PathIcon(UiIcons.Gauge, Color.White, size = 62.dp, strokeWidth = 1.6f)
        }
    }
}

@Composable
private fun SecondaryButton(text: String, icon: String, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    Pressable(onClick = onClick, modifier = modifier.height(50.dp), shape = RoundedCornerShape(16.dp)) {
        Row(
            Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PathIcon(icon, tint, size = 18.dp)
            Text(text, style = WiType.CardTitle.copy(fontSize = WiType.Caption.fontSize * 1.15f))
        }
    }
}

@Composable
fun RecentRow(entry: HistoryEntry, onClick: () -> Unit) {
    val light = entry.light
    Pressable(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(light.color.color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) { LightIcon(light, size = 28.dp) }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(light.name, style = WiType.CardTitle)
                val time = DateUtils.getRelativeTimeSpanString(entry.timestamp).toString()
                val match = if (entry.confidence >= 0) " · ${entry.confidence}% match" else ""
                Text(time + match, style = WiType.Caption)
            }
            SeverityPill(light.severity, blink = light.severity == Severity.STOP)
        }
    }
}

@Composable
private fun EmptyRecent() {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(WiColors.Surface)
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PathIcon(UiIcons.Car, WiColors.TextMuted, size = 26.dp)
            Spacer(Modifier.width(12.dp))
            Text("Your scans will appear here.", style = WiType.BodySmall)
        }
    }
}
