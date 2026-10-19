package com.brickbeam.buildbros.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.brickbeam.buildbros.ui.components.AccentButton
import com.brickbeam.buildbros.ui.components.BmCard
import com.brickbeam.buildbros.ui.components.OutlineButton
import com.brickbeam.buildbros.ui.graphics.AnimatedLogo
import com.brickbeam.buildbros.ui.graphics.BrickArt
import com.brickbeam.buildbros.ui.graphics.PaintArt
import com.brickbeam.buildbros.ui.graphics.SteelArt
import com.brickbeam.buildbros.ui.graphics.TileArt
import com.brickbeam.buildbros.ui.theme.BmIcons
import com.brickbeam.buildbros.ui.theme.BmType
import com.brickbeam.buildbros.ui.theme.LocalMotion
import com.brickbeam.buildbros.ui.theme.LocalPalette

/**
 * Shown when Back is pressed on Home. Contains no ads (Google Play / AdMob discourage ads on
 * app exit). The logo rebuilds itself while the material icons keep animating.
 */
@Composable
fun ExitDialog(onStay: () -> Unit, onExit: () -> Unit, onRate: () -> Unit) {
    val p = LocalPalette.current
    val motion = LocalMotion.current
    val pop = remember { Animatable(if (motion) 0.6f else 1f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) }
    Dialog(onDismissRequest = onStay) {
        Box(Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value; alpha = ((pop.value - 0.6f) / 0.4f).coerceIn(0f, 1f) }) {
            BmCard(Modifier.widthIn(max = 360.dp), radius = 28.dp, padding = PaddingValues(22.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    AnimatedLogo(Modifier.size(104.dp), badge = p.surface2, roof = p.accent, loop = motion, animate = motion)
                    Text("Leaving the site?", style = BmType.title, color = p.ink, textAlign = TextAlign.Center)
                    Text(
                        "Your projects and prices are saved on this device. Pick up where you left off any time.",
                        style = BmType.label, color = p.mute, textAlign = TextAlign.Center,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                        BrickArt(Modifier.size(48.dp, 34.dp))
                        SteelArt(Modifier.size(48.dp, 34.dp))
                        TileArt(Modifier.size(48.dp, 34.dp))
                        PaintArt(Modifier.size(48.dp, 34.dp), p.mute)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlineButton("Exit", onExit, Modifier.weight(1f), BmIcons.Exit)
                        AccentButton("Keep building", onStay, Modifier.weight(1.4f))
                    }
                    TextButton(onClick = onRate, modifier = Modifier.height(44.dp)) {
                        Text("Enjoying Buildbros? Rate it on Google Play", style = BmType.tiny, color = p.accentText)
                    }
                }
            }
        }
    }
}
