package com.driveready.testprep.ui.hazard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.driveready.testprep.ads.NativeAdCard
import com.driveready.testprep.data.content.HAZARD_WINDOW_SEC
import com.driveready.testprep.data.content.HazardClip
import com.driveready.testprep.data.content.HazardClips
import com.driveready.testprep.data.model.Country
import com.driveready.testprep.data.model.DrivingSide
import com.driveready.testprep.quiz.HazardScoring
import com.driveready.testprep.ui.components.AmberButton
import com.driveready.testprep.ui.components.AppCard
import com.driveready.testprep.ui.components.AppIcons
import com.driveready.testprep.ui.components.IconTile
import com.driveready.testprep.ui.components.Pill
import com.driveready.testprep.ui.components.PrimaryButton
import com.driveready.testprep.ui.components.RingMeter
import com.driveready.testprep.ui.components.ScreenHeader
import com.driveready.testprep.ui.components.SecondaryButton
import com.driveready.testprep.ui.components.SectionTitle
import com.driveready.testprep.ui.theme.Brand

@Composable
fun HazardHubScreen(
    country: Country,
    bestScore: Int?,
    contentPadding: PaddingValues,
    onStartTest: () -> Unit,
    onPlayClip: (Int) -> Unit
) {
    val hazard = country.hazard
    Column(
        Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
    ) {
        ScreenHeader("Hazard perception", hazard?.name ?: "Practice")
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = RoundedCornerShape(24.dp), color = Brand.Navy, onClick = onStartTest) {
                Column(Modifier.padding(20.dp)) {
                    Pill(if (hazard?.isOfficialPart == true) "OFFICIAL TEST PART" else "PRACTICE", Brand.Amber)
                    Spacer(Modifier.height(10.dp))
                    Text("Full test · ${HazardClips.size} clips", color = Color.White, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Pass at ${HazardScoring.passMark(HazardClips.size)} of ${HazardClips.size * HazardScoring.MAX_PER_CLIP}" + (bestScore?.let { " · best $it" } ?: ""),
                        color = Color(0xFFC9CBD1), style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(AppIcons.Play, contentDescription = null, tint = Brand.Amber, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Start", color = Brand.Amber, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            AppCard {
                Text("How it's scored", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(hazard?.description ?: "", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))
                ScoreBandBar(taps = emptyList(), clip = null)
                Spacer(Modifier.height(8.dp))
                Text("Tap the screen as soon as you see a hazard start to develop — something that would make you slow down, stop or change direction. Tap again as it develops, but never in a rhythm.", style = MaterialTheme.typography.bodySmall, color = Brand.Muted)
            }
            NativeAdCard()
            SectionTitle("Practise single clips")
            HazardClips.forEachIndexed { i, clip ->
                AppCard(onClick = { onPlayClip(i) }, contentPadding = PaddingValues(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconTile(AppIcons.Eye, Brand.BlueSoft)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Clip ${i + 1}: ${clip.title}", style = MaterialTheme.typography.titleSmall)
                            Text("${clip.durationSec.toInt()} s", style = MaterialTheme.typography.bodySmall, color = Brand.Muted)
                        }
                        Icon(AppIcons.Play, contentDescription = "Play clip", modifier = Modifier.size(18.dp))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

private enum class Phase { READY, PLAYING, FEEDBACK, SUMMARY }

/** Plays a list of clips in order and scores each tap against the hazard's window. */
@Composable
fun HazardPlayerScreen(
    country: Country,
    clips: List<HazardClip>,
    contentPadding: PaddingValues,
    onExit: () -> Unit,
    onFinished: (score: Int, total: Int, passed: Boolean) -> Unit
) {
    val kerbSide = if (country.drivingSide == DrivingSide.RIGHT) 1f else -1f
    var clipIndex by remember { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf(Phase.READY) }
    var time by remember { mutableFloatStateOf(0f) }
    val taps = remember { mutableStateListOf<Float>() }
    val scores = remember { mutableStateListOf<Int>() }
    val clip = clips[clipIndex]
    val renderer = remember(clip.id, kerbSide) { HazardSceneRenderer(clip, kerbSide) }
    var reported by remember { mutableStateOf(false) }

    BackHandler { onExit() }

    LaunchedEffect(phase, clipIndex) {
        if (phase != Phase.PLAYING) return@LaunchedEffect
        time = 0f
        taps.clear()
        val start = withFrameNanos { it }
        while (time < clip.durationSec) {
            val now = withFrameNanos { it }
            time = (now - start) / 1_000_000_000f
        }
        scores += HazardScoring.score(taps.toList(), clip.developAt)
        phase = Phase.FEEDBACK
    }

    when (phase) {
        Phase.PLAYING -> Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(clipIndex) { detectTapGestures { taps += time } }
                .semantics { contentDescription = "Hazard clip playing. Tap anywhere when you see a developing hazard." }
        ) {
            Canvas(Modifier.fillMaxSize()) { renderer.draw(this, time) }
            Row(
                Modifier
                    .padding(contentPadding)
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Clip ${clipIndex + 1}/${clips.size}", color = Color.White, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    taps.takeLast(8).forEach { _ -> Icon(AppIcons.Flag, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp)) }
                }
            }
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(contentPadding)
                    .padding(16.dp)
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0x55FFFFFF))
            ) {
                Box(Modifier.fillMaxWidth((time / clip.durationSec).coerceIn(0f, 1f)).height(4.dp).background(Brand.Amber))
            }
        }

        Phase.READY -> Column(
            Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            IconTile(AppIcons.Eye, Brand.BlueSoft, size = 88.dp)
            Spacer(Modifier.height(20.dp))
            Text("Clip ${clipIndex + 1}: ${clip.title}", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("Tap anywhere on the screen when you see a hazard starting to develop.", style = MaterialTheme.typography.bodyLarge, color = Brand.Muted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(28.dp))
            AmberButton("Play clip", { phase = Phase.PLAYING }, Modifier.fillMaxWidth(), icon = AppIcons.Play)
            Spacer(Modifier.height(10.dp))
            SecondaryButton("Back", onExit, Modifier.fillMaxWidth())
        }

        Phase.FEEDBACK -> {
            val s = scores.lastOrNull() ?: 0
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onExit) { Icon(AppIcons.Close, contentDescription = "Exit") }
                    Text("Clip ${clipIndex + 1} result", style = MaterialTheme.typography.titleLarge)
                }
                Surface(shape = RoundedCornerShape(24.dp), color = Brand.Navy) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        RingMeter(s / 5f, 110.dp, 10.dp, Brand.Amber, Color(0xFF2B3446)) {
                            Text("$s/5", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            when {
                                taps.size > 8 -> "Too many taps — pattern clicking scores zero."
                                s >= 4 -> "Excellent early response."
                                s >= 2 -> "Good, but you could respond earlier."
                                s == 1 -> "Just in time — look further ahead."
                                else -> "Missed the scoring window."
                            },
                            color = Color.White, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center
                        )
                    }
                }
                AppCard {
                    Text("Your taps vs. the scoring window", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(10.dp))
                    ScoreBandBar(taps.toList(), clip)
                }
                AppCard(color = Brand.AmberSoft, border = false) {
                    Text("What to learn", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(clip.lesson, style = MaterialTheme.typography.bodyMedium)
                }
                NativeAdCard()
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton("Replay", { scores.removeAt(scores.lastIndex); phase = Phase.PLAYING }, Modifier.weight(1f), icon = AppIcons.Replay)
                    PrimaryButton(
                        if (clipIndex == clips.lastIndex) "See summary" else "Next clip",
                        {
                            if (clipIndex == clips.lastIndex) phase = Phase.SUMMARY else { clipIndex++; phase = Phase.READY }
                        },
                        Modifier.weight(1f)
                    )
                }
            }
        }

        Phase.SUMMARY -> {
            val total = scores.sum()
            val max = clips.size * HazardScoring.MAX_PER_CLIP
            val pass = HazardScoring.passMark(clips.size)
            LaunchedEffect(Unit) {
                if (!reported) {
                    reported = true
                    onFinished(total, max, total >= pass)
                }
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ScreenHeader("Hazard perception result", onBack = onExit)
                Surface(shape = RoundedCornerShape(24.dp), color = Brand.Navy) {
                    Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Pill(if (total >= pass) "PASS" else "NOT YET", if (total >= pass) Brand.Amber else Brand.BadSoft, if (total >= pass) Brand.Ink else Brand.Bad)
                        Spacer(Modifier.height(12.dp))
                        RingMeter(total.toFloat() / max, 130.dp, 12.dp, Brand.Amber, Color(0xFF2B3446)) {
                            Text("$total/$max", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("Pass mark $pass", color = Color(0xFFC9CBD1), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                AppCard {
                    clips.forEachIndexed { i, c ->
                        Row(Modifier.padding(vertical = 6.dp)) {
                            Text("${i + 1}. ${c.title}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Text("${scores.getOrElse(i) { 0 }}/5", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                NativeAdCard()
                PrimaryButton("Done", onExit, Modifier.fillMaxWidth())
            }
        }
    }
}

/** Visualises the 5→1 point bands after the hazard starts developing, with tap markers. */
@Composable
private fun ScoreBandBar(taps: List<Float>, clip: HazardClip?) {
    val bandColors = listOf(Color(0xFF166534), Color(0xFF3F8F4F), Color(0xFF8BC34A), Color(0xFFF5B700), Color(0xFFFF8A65))
    Column {
        BoxWithConstraints(Modifier.fillMaxWidth().height(36.dp)) {
            val total = clip?.durationSec ?: 10f
            val start = clip?.developAt ?: 3f
            val pxPerSec = maxWidth / total
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)).background(Color(0xFFEDEBE4)))
            for (b in 0 until 5) {
                Box(
                    Modifier
                        .offset(x = pxPerSec * (start + b * HAZARD_WINDOW_SEC / 5))
                        .width(pxPerSec * (HAZARD_WINDOW_SEC / 5))
                        .fillMaxHeight()
                        .background(bandColors[b]),
                    contentAlignment = Alignment.Center
                ) { Text("${5 - b}", color = Color.White, style = MaterialTheme.typography.labelMedium) }
            }
            taps.forEach { t ->
                Box(Modifier.offset(x = pxPerSec * t).width(3.dp).fillMaxHeight().background(Brand.Ink))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            if (clip == null) "Points window after the hazard starts developing" else "Hazard started developing at ${"%.1f".format(clip.developAt)} s · ${taps.size} tap(s)",
            style = MaterialTheme.typography.bodySmall, color = Brand.Muted
        )
    }
}
