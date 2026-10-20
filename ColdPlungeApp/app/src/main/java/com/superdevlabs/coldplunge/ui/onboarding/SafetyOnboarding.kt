package com.superdevlabs.coldplunge.ui.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.superdevlabs.coldplunge.domain.Safety
import com.superdevlabs.coldplunge.ui.components.AmbientBackground
import com.superdevlabs.coldplunge.ui.components.GlassCard
import com.superdevlabs.coldplunge.ui.components.KIcons
import com.superdevlabs.coldplunge.ui.components.LogoMark
import com.superdevlabs.coldplunge.ui.components.ThermalButton
import com.superdevlabs.coldplunge.ui.theme.K
import com.superdevlabs.coldplunge.ui.theme.KType

/** First-launch safety briefing. Also reachable later from Settings › Safety guide. */
@Composable
fun SafetyOnboarding(onAccept: () -> Unit, acceptLabel: String = "I understand — let's go") {
    val intro = remember { Animatable(0f) }
    LaunchedEffect(Unit) { intro.animateTo(1f, tween(1200, easing = FastOutSlowInEasing)) }
    Box(Modifier.fillMaxSize()) {
        AmbientBackground(warmth = 0.4f)
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LogoMark(size = 64.dp)
            Text("Before your first plunge", style = KType.Title)
            Text(
                "Cold and heat exposure are powerful. This app helps you time and track sessions; it is not medical advice and does not diagnose or treat anything.",
                style = KType.Body.copy(color = K.Muted),
            )
            GlassCard {
                Safety.essentials.forEachIndexed { i, line ->
                    val local = ((intro.value * Safety.essentials.size) - i * 0.5f).coerceIn(0f, 1f)
                    Row(
                        Modifier.graphicsLayer { alpha = local; translationX = (1f - local) * 40f },
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(KIcons.Shield, contentDescription = null, tint = if (i % 2 == 0) K.Ice else K.Ember, modifier = Modifier.size(20.dp))
                        Text(line, style = KType.Body)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            ThermalButton(acceptLabel, onClick = onAccept, modifier = Modifier.fillMaxWidth(), icon = KIcons.Check)
        }
    }
}
