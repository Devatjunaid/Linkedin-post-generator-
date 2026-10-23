package com.devatjunaid.examclock.ui.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.devatjunaid.examclock.R
import com.devatjunaid.examclock.ui.components.LightSystemBarIcons
import com.devatjunaid.examclock.ui.components.LogoMark
import com.devatjunaid.examclock.ui.theme.Brand
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

/**
 * Branded intro: the ring fills, the tick draws itself, then the name and tagline rise in.
 * About 1.8 s in total, then [onFinished] moves on (guide for new users, home otherwise).
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    LightSystemBarIcons()
    val scale = remember { Animatable(0.6f) }
    val ring = remember { Animatable(0f) }
    val check = remember { Animatable(0f) }
    val crown = remember { Animatable(0f) }
    val text = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            async { scale.animateTo(1f, tween(650, easing = FastOutSlowInEasing)) }
            async { crown.animateTo(1f, tween(400, delayMillis = 150)) }
            async { ring.animateTo(0.75f, tween(800, delayMillis = 100, easing = FastOutSlowInEasing)) }
            async { check.animateTo(1f, tween(420, delayMillis = 780, easing = LinearOutSlowInEasing)) }
            async { text.animateTo(1f, tween(500, delayMillis = 900, easing = FastOutSlowInEasing)) }
        }
        delay(450)
        onFinished()
    }

    val appName = stringResource(R.string.app_name)
    Box(
        Modifier
            .fillMaxSize()
            .background(Brand.Ink)
            .semantics { contentDescription = appName },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(
                Modifier
                    .size(148.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                    }
                    .clip(RoundedCornerShape(40.dp))
                    .background(Brand.InkRaised),
                contentAlignment = Alignment.Center,
            ) {
                LogoMark(Modifier.size(148.dp), ringProgress = ring.value, checkProgress = check.value, crownAlpha = crown.value)
            }
            Spacer(Modifier.height(28.dp))
            Text(
                appName,
                style = MaterialTheme.typography.displaySmall,
                color = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.graphicsLayer {
                    alpha = text.value
                    translationY = (1f - text.value) * 24.dp.toPx()
                },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = Brand.OnInkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    alpha = text.value
                    translationY = (1f - text.value) * 32.dp.toPx()
                },
            )
        }
        LinearProgressIndicator(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 48.dp)
                .width(96.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = Brand.BlueLight,
            trackColor = Brand.InkLine,
        )
    }
}
