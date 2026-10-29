package com.drawtocompletefun.kids.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drawtocompletefun.kids.ui.AppState
import com.drawtocompletefun.kids.ui.GateAction
import com.drawtocompletefun.kids.ui.Overlay
import com.drawtocompletefun.kids.ui.RewardResult
import com.drawtocompletefun.kids.ui.Sfx
import com.drawtocompletefun.kids.ui.components.ButtonRowLabel
import com.drawtocompletefun.kids.ui.components.Card
import com.drawtocompletefun.kids.ui.components.ChunkyButton
import com.drawtocompletefun.kids.ui.components.GameIcon
import com.drawtocompletefun.kids.ui.components.Ico
import com.drawtocompletefun.kids.ui.components.KText
import com.drawtocompletefun.kids.ui.components.Pip
import com.drawtocompletefun.kids.ui.theme.Palette
import kotlinx.coroutines.delay
import kotlin.random.Random

/** Dimmed backdrop with a centred card. Tapping outside does nothing, so kids don't close it by accident. */
@Composable
private fun DialogFrame(content: @Composable ColumnScope.() -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Palette.Scrim)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Card(
            Modifier
                .padding(24.dp)
                .widthIn(max = 420.dp)
                .fillMaxWidth(),
            shadow = Palette.PurpleShadow,
            radius = 30.dp,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun WideButton(
    text: String,
    ico: Ico?,
    color: Color,
    shadow: Color,
    textColor: Color = Palette.Ink,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onClick: () -> Unit,
) {
    ChunkyButton(
        onClick = onClick,
        color = color,
        shadow = shadow,
        modifier = modifier.height(58.dp),
        shape = RoundedCornerShape(20.dp),
        depth = 5.dp,
        label = text,
    ) {
        if (ico != null) {
            ButtonRowLabel(ico, text, textColor, 19.sp)
        } else {
            KText(text, size = 19.sp, color = textColor, weight = FontWeight.Black)
        }
    }
}

// ---------------------------------------------------------------- Exit

@Composable
fun ExitDialog(state: AppState) {
    val platform = state.platform
    DialogFrame {
        Pip(Modifier.size(40.dp, 92.dp), happy = false)
        KText("Leaving already?", size = 26.sp, weight = FontWeight.Black, align = TextAlign.Center)
        KText(
            "Your stars are saved. Pip will miss you!\nDo you like the game? Tell us with a rating.",
            size = 15.sp,
            color = Palette.InkSoft,
            align = TextAlign.Center,
        )
        WideButton("Rate us", Ico.Heart, Palette.Yellow, Palette.YellowShadow) {
            platform.play(Sfx.Tap)
            // Grown-ups only: the review sheet leaves the game.
            state.overlay = Overlay.ParentGate(GateAction.Rate)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WideButton("Cancel", null, Palette.Lavender, Palette.SkyShadow, modifier = Modifier.weight(1f)) {
                platform.play(Sfx.Tap)
                state.overlay = null
            }
            WideButton("Exit", null, Palette.Coral, Color(0xFFD9573A), modifier = Modifier.weight(1f)) {
                state.overlay = null
                platform.exitApp()
            }
        }
    }
}

// ---------------------------------------------------------------- Settings

@Composable
fun SettingsDialog(state: AppState) {
    val progress = state.progress
    val platform = state.platform
    DialogFrame {
        KText("Settings", size = 26.sp, weight = FontWeight.Black)
        ToggleRow(if (progress.soundOn) Ico.SoundOn else Ico.SoundOff, "Sounds", progress.soundOn) {
            progress.updateSound(!progress.soundOn)
            platform.play(Sfx.Tap)
        }
        ToggleRow(if (progress.musicOn) Ico.MusicOn else Ico.MusicOff, "Music", progress.musicOn) {
            progress.updateMusic(!progress.musicOn)
            platform.play(Sfx.Tap)
        }
        WideButton("Privacy (grown-ups)", Ico.Shield, Palette.Lavender, Palette.SkyShadow) {
            platform.play(Sfx.Tap)
            state.overlay = Overlay.ParentGate(GateAction.Privacy)
        }
        WideButton("Done", Ico.Check, Palette.Green, Palette.GreenShadow, textColor = Palette.GreenInk) {
            platform.play(Sfx.Tap)
            state.overlay = null
        }
    }
}

@Composable
private fun ToggleRow(ico: Ico, label: String, on: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.Lavender)
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GameIcon(ico, Palette.Ink, 26.dp)
        KText(label, size = 18.sp, modifier = Modifier
            .weight(1f)
            .padding(start = 12.dp))
        // Simple switch drawn with two circles.
        Box(
            Modifier
                .size(60.dp, 34.dp)
                .clip(RoundedCornerShape(50))
                .background(if (on) Palette.Green else Palette.Locked),
            contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .padding(4.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color.White),
            )
        }
        KText(if (on) "On" else "Off", size = 14.sp, modifier = Modifier.padding(start = 8.dp))
    }
}

/** Small grey "Ad" tag so children and parents can always see what is an advertisement. */
@Composable
fun AdTag(modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF5F6368))
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) { KText("Ad", size = 12.sp, color = Color.White, weight = FontWeight.Bold) }
}

// ---------------------------------------------------------------- Bonus level (rewarded ad)

@Composable
fun TreasureDialog(state: AppState, level: Int) {
    val platform = state.platform
    var loading by remember(level) { mutableStateOf(false) }
    var note by remember(level) { mutableStateOf<String?>(null) }
    DialogFrame {
        AdTag()
        KText("Level $level opens after an ad", size = 24.sp, weight = FontWeight.Black, align = TextAlign.Center)
        KText(
            note ?: "This is an advertisement, not part of the game. After the ad ends, level $level opens.",
            size = 15.sp,
            color = Palette.InkSoft,
            align = TextAlign.Center,
        )
        WideButton(
            if (loading) "Loading ad..." else "Watch ad",
            Ico.Video,
            Color(0xFFE8EAED),
            Color(0xFFBDC1C6),
        ) {
            if (loading) return@WideButton
            loading = true
            platform.play(Sfx.Tap)
            platform.showRewarded { result ->
                loading = false
                when (result) {
                    RewardResult.Earned -> {
                        platform.play(Sfx.Unlock)
                        state.treasureOpened(level)
                    }
                    RewardResult.Closed -> note = "The ad was closed early. Watch the whole ad to open level $level."
                    // No ad available right now: never block a child, just open the level.
                    RewardResult.Unavailable -> {
                        platform.play(Sfx.Unlock)
                        state.treasureOpened(level)
                    }
                }
            }
        }
        WideButton("Not now", null, Palette.Lavender, Palette.SkyShadow) {
            platform.play(Sfx.Tap)
            state.overlay = null
        }
    }
}

// ---------------------------------------------------------------- Ad break notice

/** Tells the player an ad is coming, so it is never mistaken for the next game screen. */
@Composable
fun AdBreakNotice(state: AppState, adBreak: Overlay.AdBreak) {
    var secondsLeft by remember(adBreak) { mutableIntStateOf(2) }
    LaunchedEffect(adBreak) {
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft--
        }
        state.runAdBreak(adBreak)
    }
    DialogFrame {
        AdTag()
        KText("Ad break", size = 26.sp, weight = FontWeight.Black)
        KText(
            "An advertisement will show in $secondsLeft. The game comes back right after it.",
            size = 15.sp,
            color = Palette.InkSoft,
            align = TextAlign.Center,
        )
    }
}

// ---------------------------------------------------------------- Parental gate

@Composable
fun ParentGateDialog(state: AppState, action: GateAction) {
    val platform = state.platform
    var seed by remember { mutableIntStateOf(Random.nextInt()) }
    val rnd = remember(seed) { Random(seed) }
    val a = remember(seed) { rnd.nextInt(4, 10) }
    val b = remember(seed) { rnd.nextInt(3, 10) }
    val options = remember(seed) {
        val right = a + b
        (listOf(right) + listOf(right - 2, right + 1, right + 3, right - 1).shuffled(rnd).take(2)).shuffled(rnd)
    }
    var wrong by remember { mutableStateOf(false) }

    DialogFrame {
        GameIcon(Ico.Shield, Palette.Purple, 46.dp)
        KText("Grown-ups only", size = 24.sp, weight = FontWeight.Black)
        KText(
            if (wrong) "Not quite. Please ask a grown-up." else "Please ask a grown-up to answer:",
            size = 15.sp,
            color = Palette.InkSoft,
            align = TextAlign.Center,
        )
        KText("$a + $b = ?", size = 34.sp, weight = FontWeight.Black)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            options.forEach { n ->
                IconlessTile("$n", Modifier.weight(1f)) {
                    if (n == a + b) {
                        platform.play(Sfx.Tap)
                        state.gatePassed(action)
                    } else {
                        platform.play(Sfx.Oops)
                        wrong = true
                        seed = Random.nextInt()
                    }
                }
            }
        }
        WideButton("Cancel", Ico.Close, Palette.Lavender, Palette.SkyShadow) {
            platform.play(Sfx.Tap)
            state.overlay = null
        }
    }
}

@Composable
private fun IconlessTile(text: String, modifier: Modifier, onClick: () -> Unit) {
    ChunkyButton(
        onClick = onClick,
        color = Palette.Purple,
        shadow = Palette.PurpleShadow,
        modifier = modifier.height(60.dp),
        shape = RoundedCornerShape(18.dp),
        depth = 5.dp,
        label = text,
    ) { KText(text, size = 24.sp, color = Palette.Paper, weight = FontWeight.Black) }
}
