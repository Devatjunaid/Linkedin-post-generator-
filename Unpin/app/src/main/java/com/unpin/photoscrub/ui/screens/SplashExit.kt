package com.unpin.photoscrub.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.unpin.photoscrub.ui.components.AnimatedWordmark
import com.unpin.photoscrub.ui.components.FlyAwayGraphic
import com.unpin.photoscrub.ui.components.LogoMark
import com.unpin.photoscrub.ui.components.OutlineButton
import com.unpin.photoscrub.ui.components.PrimaryButton
import com.unpin.photoscrub.ui.components.UnpinIcons
import com.unpin.photoscrub.ui.theme.Card
import com.unpin.photoscrub.ui.theme.Ink
import com.unpin.photoscrub.ui.theme.InkLine
import com.unpin.photoscrub.ui.theme.InkSoft
import com.unpin.photoscrub.ui.theme.Muted
import com.unpin.photoscrub.ui.theme.MutedOnInk
import com.unpin.photoscrub.ui.theme.Paper
import com.unpin.photoscrub.ui.theme.Signal
import com.unpin.photoscrub.ui.theme.SignalSoft
import com.unpin.photoscrub.ui.theme.SignalTint
import com.unpin.photoscrub.ui.theme.SignalDeep
import com.unpin.photoscrub.ui.theme.UnpinText
import kotlinx.coroutines.delay

private const val TAGLINE = "PHOTO LOCATION & METADATA REMOVER"

@Composable
fun SplashScreen(consentDone: Boolean, onFinished: () -> Unit) {
    val build = remember { Animatable(0f) }
    val word = remember { Animatable(0f) }
    val extras = remember { Animatable(0f) }
    var animDone by remember { mutableStateOf(false) }
    val transition = rememberInfiniteTransition(label = "splash")
    val pulse by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart), label = "pulse")
    val drift by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart), label = "drift")

    LaunchedEffect(Unit) {
        build.animateTo(1f, tween(2100, easing = LinearEasing))
        animDone = true
    }
    LaunchedEffect(Unit) {
        delay(1100)
        word.animateTo(1f, tween(1000))
    }
    LaunchedEffect(Unit) {
        delay(1650)
        extras.animateTo(1f, tween(900))
    }
    // Leave once the animation has played and consent is settled (never wait more than 5 s).
    LaunchedEffect(animDone, consentDone) {
        if (!animDone) return@LaunchedEffect
        delay(if (consentDone) 350 else 5000)
        onFinished()
    }

    Box(Modifier.fillMaxSize().background(Ink)) {
        // drifting blueprint grid + sonar rings
        Canvas(Modifier.fillMaxSize()) {
            val step = 36.dp.toPx()
            val off = drift * step
            var x = -step + off
            while (x < size.width + step) { drawLine(InkSoft, Offset(x, 0f), Offset(x, size.height), 1f); x += step }
            var y = -step + off
            while (y < size.height + step) { drawLine(InkSoft, Offset(0f, y), Offset(size.width, y), 1f); y += step }
            val c = Offset(size.width / 2f, size.height * 0.40f)
            for (k in 0 until 3) {
                val p = (pulse + k / 3f) % 1f
                drawCircle(Signal, 90.dp.toPx() + p * size.width * 0.55f, c, alpha = 0.16f * (1f - p) * build.value, style = Stroke(1.5f))
            }
        }

        Column(
            Modifier.fillMaxWidth().padding(top = 0.dp).align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(0.dp))
            LogoMark(Modifier.size(184.dp), progress = build.value, pulse = if (animDone) pulse else 0f)
            Spacer(Modifier.height(26.dp))
            AnimatedWordmark(word.value, UnpinText.hero.copy(fontSize = 56.sp, lineHeight = 58.sp, letterSpacing = (-2).sp), Paper)
            Spacer(Modifier.height(10.dp))
            val chars = (TAGLINE.length * extras.value).toInt()
            Text(
                TAGLINE.take(chars) + if (chars in 1 until TAGLINE.length) "▌" else "",
                style = UnpinText.label,
                color = SignalSoft,
                textAlign = TextAlign.Center,
                modifier = Modifier.height(16.dp),
            )
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 36.dp)
                .graphicsLayer { alpha = extras.value; translationY = (1f - extras.value) * 20.dp.toPx() },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SplashChip(UnpinIcons.Lock, "On-device")
                SplashChip(UnpinIcons.Photos, "Lossless")
                SplashChip(UnpinIcons.Shield, "Verified")
            }
            Spacer(Modifier.height(20.dp))
            Box(Modifier.width(140.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(InkLine)) {
                Box(
                    Modifier.fillMaxWidth(build.value).height(3.dp).clip(RoundedCornerShape(2.dp)).background(Signal)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                if (animDone && !consentDone) "Checking privacy choices…" else "Preparing your privacy shield…",
                style = UnpinText.small, color = MutedOnInk,
            )
        }
    }
}

@Composable
private fun SplashChip(icon: ImageVector, text: String) {
    Row(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, InkLine, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Signal, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = UnpinText.small.copy(fontSize = 12.sp), color = Paper)
    }
}

/** Shown when Back is pressed on Home. No ads here. */
@Composable
fun ExitDialog(cleaned: Int, locations: Int, onStay: () -> Unit, onExit: () -> Unit) {
    Dialog(onDismissRequest = onStay, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val appear = remember { Animatable(0f) }
        LaunchedEffect(Unit) { appear.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 380f)) }
        Column(
            Modifier
                .padding(20.dp)
                .widthIn(max = 420.dp)
                .graphicsLayer {
                    val s = 0.85f + 0.15f * appear.value
                    scaleX = s; scaleY = s; alpha = appear.value.coerceIn(0f, 1f)
                }
                .clip(RoundedCornerShape(30.dp))
                .background(Card)
                .padding(bottom = 18.dp),
        ) {
            Box(Modifier.fillMaxWidth().height(160.dp).background(Paper)) {
                FlyAwayGraphic(Modifier.fillMaxSize().padding(top = 8.dp))
            }
            Column(Modifier.padding(horizontal = 22.dp)) {
                Spacer(Modifier.height(18.dp))
                Text("Leaving already?", style = UnpinText.title.copy(fontSize = 26.sp), color = Ink)
                Spacer(Modifier.height(6.dp))
                Text(
                    if (cleaned > 0) {
                        "You've cleaned $cleaned photo${if (cleaned == 1) "" else "s"} and erased $locations location${if (locations == 1) "" else "s"}. None of it ever left your phone."
                    } else {
                        "Nothing was uploaded and your originals are untouched. Come back any time a photo needs unpinning."
                    },
                    style = UnpinText.body, color = Muted,
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(SignalTint).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(UnpinIcons.Share, null, tint = SignalDeep, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Tip: share photos to Unpin from any app — they're cleaned on the way out.",
                        style = UnpinText.small, color = SignalDeep,
                    )
                }
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlineButton("Exit", onExit, Modifier.weight(1f))
                    PrimaryButton("Stay", onStay, Modifier.weight(1f), container = Ink, content = Paper)
                }
            }
        }
    }
}
