package com.devatjunaid.dripdropdraw.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devatjunaid.dripdropdraw.audio.Sound
import kotlinx.coroutines.delay
import kotlin.random.Random

/** Dark backdrop with a bouncy card in the middle. Back closes it when [onDismiss] is set. */
@Composable
fun Overlay(onDismiss: (() -> Unit)?, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(enabled = true) { onDismiss?.invoke() }
    val appear = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 400f)) }
    Box(
        Modifier
            .fillMaxSize()
            .background(Palette.Scrim)
            // Swallow taps so nothing behind the dialog reacts.
            .clickable(remember { MutableInteractionSource() }, indication = null) { },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(24.dp)
                .widthIn(max = 380.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = appear.value
                    scaleY = appear.value
                }
                .background(Palette.Paper, RoundedCornerShape(28.dp))
                .border(3.dp, Palette.Ink, RoundedCornerShape(28.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
fun DialogTitle(text: String) {
    Text(text, style = textStyle(28.sp, FontWeight.Bold), textAlign = TextAlign.Center)
}

@Composable
fun DialogText(text: String) {
    Text(text, style = textStyle(17.sp, FontWeight.Medium, Palette.Muted), textAlign = TextAlign.Center)
}

/** Shown when back is pressed on the main menu: Cancel, Rate us, Exit. */
@Composable
fun ExitDialog(onCancel: () -> Unit, onRate: () -> Unit, onExit: () -> Unit) {
    Overlay(onDismiss = onCancel) {
        Mascot(Mood.SAD, Modifier.size(120.dp), fill = 0.25f)
        DialogTitle("Leaving already?")
        DialogText("The cups will miss you!")
        Spacer(Modifier.height(4.dp))
        CartoonButton(onRate, Modifier.fillMaxWidth(), color = Palette.Star) {
            GameIcon(IconKind.STAR, Modifier.size(26.dp))
            ButtonLabel("Rate us")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CartoonButton(onCancel, Modifier.weight(1f)) { ButtonLabel("Cancel") }
            CartoonButton(onExit, Modifier.weight(1f), color = Palette.Orange) { ButtonLabel("Exit") }
        }
    }
}

/**
 * A simple grown-up check before anything leaves the game (the Play Store page, info for
 * parents). Required good practice for apps made for children.
 */
@Composable
fun ParentalGate(onPass: () -> Unit, onCancel: () -> Unit) {
    val q = remember {
        val a = Random.nextInt(3, 10)
        val b = Random.nextInt(3, 10)
        val right = a * b
        val wrong = listOf(right + a, right - b, right + 1, right - 1, right + 10)
            .filter { it > 0 && it != right }
            .distinct()
            .shuffled()
            .take(2)
        Triple(a, b, (wrong + right).shuffled())
    }
    val right = q.first * q.second
    Overlay(onDismiss = onCancel) {
        GameIcon(IconKind.PARENT, Modifier.size(56.dp))
        DialogTitle("Ask a grown-up")
        DialogText("What is ${q.first} × ${q.second}?")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (opt in q.third) {
                CartoonButton({ if (opt == right) onPass() else onCancel() }, Modifier.weight(1f), color = Palette.WaterLight) {
                    ButtonLabel(opt.toString())
                }
            }
        }
        CartoonButton(onCancel, Modifier.fillMaxWidth()) { ButtonLabel("Cancel") }
    }
}

const val PRIVACY_POLICY_URL = "https://superdevlabs.blogspot.com/p/privacy-policy-drip-drop-draw-water.html"

/** Information for parents, behind the parental gate. */
@Composable
fun ParentsInfo(onClose: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    Overlay(onDismiss = onClose) {
        DialogTitle("For parents")
        DialogText(
            "Drip Drop Draw is a drawing and physics puzzle for kids. It shows ads from Google AdMob. " +
                "All ad requests are marked as made for children, so ads are not personalised and are " +
                "limited to family-friendly content. Videos are only shown when your child chooses to " +
                "open a bonus chest. The game keeps progress only on this device and has no chat, " +
                "accounts or in-app purchases.",
        )
        CartoonButton(
            onClick = {
                // No browser installed: nothing to open, so ignore the tap.
                runCatching { uriHandler.openUri(PRIVACY_POLICY_URL) }
            },
            modifier = Modifier.fillMaxWidth(),
            color = Palette.WaterLight,
        ) { ButtonLabel("Privacy policy") }
        CartoonButton(onClose, Modifier.fillMaxWidth(), color = Palette.Green) { ButtonLabel("OK") }
    }
}

/** The opt-in rewarded video offered after every 5 finished levels. */
@Composable
fun BonusChestDialog(coins: Int, onWatch: () -> Unit, onSkip: () -> Unit) {
    Overlay(onDismiss = onSkip) {
        GameIcon(IconKind.GIFT, Modifier.size(90.dp), fill = Palette.Star)
        DialogTitle("Bonus chest!")
        DialogText("Watch a short video to open it and get $coins coins.")
        CartoonButton(onWatch, Modifier.fillMaxWidth(), color = Palette.Green) {
            GameIcon(IconKind.PLAY, Modifier.size(22.dp))
            ButtonLabel("Watch video")
        }
        CartoonButton(onSkip, Modifier.fillMaxWidth()) { ButtonLabel("No thanks") }
    }
}

@Composable
fun HintDialog(cost: Int, coins: Int, onBuy: () -> Unit, onClose: () -> Unit) {
    Overlay(onDismiss = onClose) {
        GameIcon(IconKind.BULB, Modifier.size(70.dp))
        DialogTitle("Need a hint?")
        if (coins >= cost) {
            DialogText("See one way to solve this level.")
            CartoonButton(onBuy, Modifier.fillMaxWidth(), color = Palette.Green) {
                ButtonLabel("Show me")
                GameIcon(IconKind.COIN, Modifier.size(24.dp))
                ButtonLabel("$cost")
            }
            CartoonButton(onClose, Modifier.fillMaxWidth()) { ButtonLabel("Not now") }
        } else {
            DialogText("A hint costs $cost coins. Finish levels to earn more coins!")
            CartoonButton(onClose, Modifier.fillMaxWidth(), color = Palette.Green) { ButtonLabel("OK") }
        }
    }
}

private val winTitles = listOf("Splash-tastic!", "Awesome!", "Super!", "Wow!", "Great job!", "Yay!")

/** Level complete: stars pop in one by one. */
@Composable
fun WinPanel(level: Int, stars: Int, coins: Int, onReplay: () -> Unit, onNext: () -> Unit, onHome: () -> Unit) {
    val sfx = LocalSfx.current
    val title = remember { winTitles.random() }
    val starScale = remember { List(3) { Animatable(0f) } }
    LaunchedEffect(Unit) {
        delay(250)
        for (i in 0 until stars) {
            sfx?.play(Sound.STAR, rate = 1f + i * 0.12f)
            starScale[i].animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 500f))
        }
        sfx?.play(Sound.COIN)
    }
    Overlay(onDismiss = onHome) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (i in 0 until 3) {
                val big = i == 1
                Box(Modifier.size(if (big) 76.dp else 60.dp)) {
                    GameIcon(IconKind.STAR, Modifier.fillMaxSize(), fill = Palette.StarOff)
                    GameIcon(
                        IconKind.STAR,
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = starScale[i].value
                                scaleY = starScale[i].value
                            },
                    )
                }
            }
        }
        DialogTitle(title)
        DialogText("Level $level complete")
        Chip {
            GameIcon(IconKind.COIN, Modifier.size(26.dp))
            Text("+$coins", style = textStyle(18.sp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconCircleButton(IconKind.HOME, "Levels", onHome, size = 56.dp)
            IconCircleButton(IconKind.RESTART, "Play again", onReplay, size = 56.dp)
            CartoonButton(onNext, Modifier.weight(1f), color = Palette.Orange) {
                ButtonLabel("Next")
                GameIcon(IconKind.NEXT, Modifier.size(24.dp))
            }
        }
    }
}

private val failTitles = listOf("So close!", "Almost!", "Oops!", "Try again!")

@Composable
fun FailPanel(onRetry: () -> Unit, onHint: () -> Unit, onHome: () -> Unit, hintShown: Boolean) {
    val title = remember { failTitles.random() }
    Overlay(onDismiss = onHome) {
        Mascot(Mood.SAD, Modifier.size(110.dp), fill = 0.2f)
        DialogTitle(title)
        DialogText(if (hintShown) "Follow the orange dotted line!" else "The cup is still thirsty. Draw a new line!")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconCircleButton(IconKind.HOME, "Levels", onHome, size = 56.dp)
            if (!hintShown) IconCircleButton(IconKind.BULB, "Hint", onHint, color = Palette.Green, size = 56.dp)
            CartoonButton(onRetry, Modifier.weight(1f), color = Palette.Orange) {
                GameIcon(IconKind.RESTART, Modifier.size(24.dp))
                ButtonLabel("Retry")
            }
        }
    }
}

/** Shown after the very last level. */
@Composable
fun AllDoneDialog(onClose: () -> Unit) {
    Overlay(onDismiss = onClose) {
        Mascot(Mood.LAUGH, Modifier.size(130.dp), fill = 0.95f)
        DialogTitle("You did it!")
        DialogText("Every cup is happy. Try to get 3 stars everywhere — new levels are on the way!")
        CartoonButton(onClose, Modifier.fillMaxWidth(), color = Palette.Green) { ButtonLabel("Yay!") }
    }
}
