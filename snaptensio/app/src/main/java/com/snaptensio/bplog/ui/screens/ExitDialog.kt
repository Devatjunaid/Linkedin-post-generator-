package com.snaptensio.bplog.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.snaptensio.bplog.ui.components.AnimatedLogo

/**
 * Shown when the user presses Back on Home. Deliberately ad-free: AdMob policy does not allow
 * ads that appear on app exit, and an ad next to "Exit" invites accidental clicks.
 */
@Composable
fun ExitDialog(todayCount: Int, onStay: () -> Unit, onExit: () -> Unit) {
    val pop = remember { Animatable(0.85f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
    Dialog(onDismissRequest = onStay) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value },
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AnimatedLogo(size = 84.dp, intro = false)
                Text("Leaving already?", style = MaterialTheme.typography.headlineSmall)
                Text(
                    when (todayCount) {
                        0 -> "You haven't logged a reading today. A quick scan takes 10 seconds."
                        1 -> "You logged 1 reading today. Nice work, see you at your next check."
                        else -> "You logged $todayCount readings today. Nice work, see you at your next check."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onExit, modifier = Modifier.weight(1f).height(50.dp)) { Text("Exit") }
                    Button(onClick = onStay, modifier = Modifier.weight(1f).height(50.dp)) { Text("Stay") }
                }
            }
        }
    }
}
