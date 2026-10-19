package com.devatjunaid.kelvin.ui.exit

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.devatjunaid.kelvin.ui.components.AmbientBackground
import com.devatjunaid.kelvin.ui.components.AnimatedLogo
import com.devatjunaid.kelvin.ui.components.PrimaryButton
import com.devatjunaid.kelvin.ui.components.SecondaryButton
import com.devatjunaid.kelvin.ui.components.StatTile
import com.devatjunaid.kelvin.ui.theme.K
import com.devatjunaid.kelvin.ui.theme.KType
import kotlinx.coroutines.launch

/**
 * Shown when Back is pressed on the Today tab. Kept ad-free on purpose: AdMob policy forbids
 * ads that appear when the user is trying to leave the app.
 */
@Composable
fun ExitDialog(
    streak: Int,
    weekSessions: Int,
    onStay: () -> Unit,
    onExit: () -> Unit,
) {
    Dialog(onDismissRequest = onStay, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val enter = remember { Animatable(0f) }
        val logoSpin = remember { Animatable(-90f) }
        LaunchedEffect(Unit) {
            launch { enter.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 260f)) }
            logoSpin.animateTo(0f, tween(900, easing = FastOutSlowInEasing))
        }
        Box(
            Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    val e = enter.value
                    alpha = e.coerceIn(0f, 1f)
                    scaleX = 0.85f + 0.15f * e
                    scaleY = 0.85f + 0.15f * e
                    translationY = (1f - e) * 120f
                }
                .clip(RoundedCornerShape(32.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF16202B), K.InkDeep)))
                .border(1.dp, Brush.linearGradient(listOf(K.Ice.copy(alpha = 0.5f), K.Ember.copy(alpha = 0.5f))), RoundedCornerShape(32.dp)),
        ) {
            AmbientBackground(Modifier.matchParentSize(), warmth = 0.5f)
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                AnimatedLogo(size = 120.dp, intro = true, introDurationMs = 1100, modifier = Modifier.graphicsLayer { rotationZ = logoSpin.value })
                Text("Leaving already?", style = KType.Title, textAlign = TextAlign.Center)
                Text(
                    if (streak > 0) "You're on a $streak-day streak. Come back tomorrow to keep it going."
                    else "Your next session is one tap away whenever you're ready.",
                    style = KType.Body.copy(color = K.Muted),
                    textAlign = TextAlign.Center,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Day streak", "$streak", Modifier.weight(1f), valueColor = K.Ice)
                    StatTile("This week", "$weekSessions sessions", Modifier.weight(1f), valueColor = K.Ember)
                }
                PrimaryButton("Stay", onClick = onStay, modifier = Modifier.fillMaxWidth())
                SecondaryButton("Exit app", onClick = onExit, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
