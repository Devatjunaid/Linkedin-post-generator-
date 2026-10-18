package com.inkformlabs.pdfsigner.ui.exit

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.inkformlabs.pdfsigner.ui.components.ButtonKind
import com.inkformlabs.pdfsigner.ui.components.InkButton
import com.inkformlabs.pdfsigner.ui.components.InkformLogo
import com.inkformlabs.pdfsigner.ui.theme.InkIcons
import com.inkformlabs.pdfsigner.ui.theme.LocalInk
import kotlinx.coroutines.launch

/** Shown when the user presses Back on the home screen. No ads are shown here. */
@Composable
fun ExitDialog(documentCount: Int, onStay: () -> Unit, onExit: () -> Unit) {
    val p = LocalInk.current
    val scale = remember { Animatable(0.82f) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { alpha.animateTo(1f, tween(220)) }
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
    }

    Dialog(onDismissRequest = onStay, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(28.dp)
                .widthIn(max = 420.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    this.alpha = alpha.value
                }
                .clip(RoundedCornerShape(30.dp))
                .background(p.surface)
                .border(1.dp, p.line, RoundedCornerShape(30.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            InkformLogo(Modifier.size(132.dp), playIntro = true, idle = true, introMillis = 1500)
            Text(
                "Leaving Inkform?",
                style = MaterialTheme.typography.headlineSmall,
                color = p.ink,
                textAlign = TextAlign.Center,
            )
            Text(
                if (documentCount > 0) {
                    "Your $documentCount document${if (documentCount == 1) "" else "s"} and signatures stay safely on this phone."
                } else {
                    "Everything you fill and sign stays safely on this phone."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = p.muted,
                textAlign = TextAlign.Center,
            )
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(p.surface2)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(InkIcons.Shield, contentDescription = null, tint = p.hi, modifier = Modifier.size(15.dp))
                Text("Nothing was uploaded", style = MaterialTheme.typography.labelMedium, color = p.ink)
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InkButton("Exit", onClick = onExit, kind = ButtonKind.Secondary, icon = InkIcons.Exit, modifier = Modifier.weight(1f))
                InkButton("Stay", onClick = onStay, kind = ButtonKind.Primary, modifier = Modifier.weight(1f))
            }
        }
    }
}
