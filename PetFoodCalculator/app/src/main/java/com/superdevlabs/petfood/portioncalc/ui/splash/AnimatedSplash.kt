package com.superdevlabs.petfood.portioncalc.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.superdevlabs.petfood.portioncalc.R
import com.superdevlabs.petfood.portioncalc.ui.Brand
import kotlinx.coroutines.delay

private const val TOTAL_MS = 1700
private val EaseOutBack: Easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

/** Progress of one element that animates between [startMs] and [endMs] of the timeline. */
private fun phase(t: Float, startMs: Int, endMs: Int, easing: Easing = FastOutSlowInEasing): Float {
    val now = t * TOTAL_MS
    val raw = ((now - startMs) / (endMs - startMs)).coerceIn(0f, 1f)
    return easing.transform(raw)
}

/**
 * Brand splash: the logo assembles once (scale, bowl lands, dog looks in, dial needle
 * swings to the measured portion, check mark confirms) and the app opens.
 * It waits for the consent step so no screen with ads appears before consent.
 */
@Composable
fun AnimatedSplash(consentDone: Boolean, onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    var animationDone by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(TOTAL_MS, easing = LinearEasing))
        delay(250)
        animationDone = true
    }
    LaunchedEffect(animationDone, consentDone) {
        if (animationDone && consentDone) onFinished()
    }
    // Never block the user for long if the consent service is unreachable.
    LaunchedEffect(animationDone) {
        if (animationDone) {
            delay(4_000)
            onFinished()
        }
    }

    val t = progress.value
    val disc = phase(t, 0, 450)
    val scale = phase(t, 100, 600)
    val bowl = phase(t, 350, 850, EaseOutBack)
    val dog = phase(t, 550, 1050)
    val needle = phase(t, 850, 1350, EaseOutBack)
    val check = phase(t, 1200, 1550, EaseOutBack)
    val text = phase(t, 1000, 1600)

    Box(
        Modifier
            .fillMaxSize()
            .background(Brand.Navy)
            .semantics { contentDescription = "Pet Food Calculator is starting" },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(
                Modifier
                    .size(240.dp)
                    .graphicsLayer {
                        alpha = disc
                        scaleX = 0.85f + 0.15f * disc
                        scaleY = 0.85f + 0.15f * disc
                    }
                    .clip(CircleShape)
                    .background(Color(0xFF26304F)),
            ) {
                Layer(R.drawable.logo_scale, alpha = scale, offsetY = (1 - scale) * 24f)
                Layer(R.drawable.logo_dog, alpha = dog, offsetY = (1 - dog) * 30f)
                Layer(R.drawable.logo_bowl, alpha = bowl.coerceAtMost(1f), offsetY = (1 - bowl) * -70f)
                Image(
                    painterResource(R.drawable.logo_needle),
                    contentDescription = null,
                    modifier = Modifier
                        .size(240.dp)
                        .graphicsLayer {
                            alpha = scale
                            rotationZ = -60f + 98f * needle
                            transformOrigin = TransformOrigin(256f / 512f, 402f / 512f)
                        },
                )
                Image(
                    painterResource(R.drawable.logo_check),
                    contentDescription = null,
                    modifier = Modifier
                        .size(240.dp)
                        .graphicsLayer {
                            alpha = check.coerceIn(0f, 1f)
                            scaleX = check
                            scaleY = check
                            transformOrigin = TransformOrigin(392f / 512f, 168f / 512f)
                        },
                )
            }
            Spacer(Modifier.height(28.dp))
            Text(
                "Pet Food Calculator",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.graphicsLayer {
                    alpha = text
                    translationY = (1 - text) * 16f
                },
            )
            Text(
                "Exact dog & cat portions by breed and weight",
                color = Color(0xFFC9D0E2),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(top = 6.dp, start = 24.dp, end = 24.dp)
                    .graphicsLayer { alpha = text },
            )
        }
    }
}

@Composable
private fun Layer(res: Int, alpha: Float, offsetY: Float) {
    Image(
        painterResource(res),
        contentDescription = null,
        modifier = Modifier
            .size(240.dp)
            .graphicsLayer {
                this.alpha = alpha.coerceIn(0f, 1f)
                translationY = offsetY * density
            },
    )
}
