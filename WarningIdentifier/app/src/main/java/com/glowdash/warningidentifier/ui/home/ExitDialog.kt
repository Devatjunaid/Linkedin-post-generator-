package com.glowdash.warningidentifier.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.glowdash.warningidentifier.ads.NativeAdCard
import com.glowdash.warningidentifier.ui.components.PathIcon
import com.glowdash.warningidentifier.ui.components.Pressable
import com.glowdash.warningidentifier.ui.components.UiIcons
import com.glowdash.warningidentifier.ui.theme.WiColors
import com.glowdash.warningidentifier.ui.theme.WiType

/** "Leaving already?" pop-up shown when back is pressed on Home. */
@Composable
fun ExitDialog(onStay: () -> Unit, onExit: () -> Unit) {
    Dialog(
        onDismissRequest = onStay,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val pop = remember { Animatable(0.85f) }
        LaunchedEffect(Unit) {
            pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 300f))
        }
        val t = rememberInfiniteTransition(label = "exit")
        val glow by t.animateFloat(0.5f, 1f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "glow")

        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .graphicsLayer { scaleX = pop.value; scaleY = pop.value }
                .clip(RoundedCornerShape(28.dp))
                .background(WiColors.Surface)
                .border(1.dp, WiColors.Stroke, RoundedCornerShape(28.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(64.dp)
                    .graphicsLayer { alpha = 0.6f + 0.4f * glow }
                    .clip(RoundedCornerShape(20.dp))
                    .background(WiColors.Pink.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                PathIcon(UiIcons.Power, WiColors.Pink, size = 30.dp, strokeWidth = 2.2f)
            }
            Spacer(Modifier.height(14.dp))
            Text("Leaving already?", style = WiType.Title)
            Spacer(Modifier.height(4.dp))
            Text(
                "Drive safe! Come back anytime a light pops up.",
                style = WiType.BodySmall.copy(textAlign = TextAlign.Center),
            )

            NativeAdCard("exit", Modifier.padding(top = 16.dp))

            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Pressable(
                    onClick = onExit,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    background = WiColors.SurfaceHigh,
                ) {
                    Text("Exit", style = WiType.Button.copy(color = WiColors.TextPrimary), modifier = Modifier.align(Alignment.Center))
                }
                Pressable(
                    onClick = onStay,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    background = WiColors.Cyan,
                    border = null,
                ) {
                    Text("Stay", style = WiType.Button.copy(color = WiColors.OnCyan), modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}
