package com.aquadoselab.poolchemistry.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aquadoselab.poolchemistry.ads.AdsManager
import com.aquadoselab.poolchemistry.ads.NativeAdCard
import com.aquadoselab.poolchemistry.ads.NativeStyle
import com.aquadoselab.poolchemistry.ui.components.AppLogo
import com.aquadoselab.poolchemistry.ui.theme.AppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.PI
import kotlin.math.sin

private const val MIN_SPLASH_MS = 2200L
private const val MAX_CONSENT_WAIT_MS = 8000L

@Composable
fun SplashScreen(activity: Activity, onFinished: () -> Unit) {
    val finish by rememberUpdatedState(onFinished)
    var consentDone by remember { mutableStateOf(false) }
    val logoScale = remember { Animatable(0.6f) }
    val contentAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        AdsManager.start(activity) { consentDone = true }
    }
    LaunchedEffect(Unit) {
        logoScale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow))
    }
    LaunchedEffect(Unit) {
        delay(250)
        contentAlpha.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        delay(MIN_SPLASH_MS)
        withTimeoutOrNull(MAX_CONSENT_WAIT_MS) {
            while (!consentDone) delay(100)
        }
        finish()
    }

    val waves = rememberInfiniteTransition(label = "waves")
    val phase by waves.animateFloat(
        initialValue = 0f, targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "phase"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(AppColors.Navy)
    ) {
        // Animated water at the bottom of the splash
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(220.dp)
                .align(Alignment.BottomCenter)
        ) {
            fun wave(baseY: Float, amp: Float, len: Float, shift: Float, color: Color) {
                val p = Path()
                p.moveTo(0f, size.height)
                p.lineTo(0f, baseY)
                var x = 0f
                while (x <= size.width + 10f) {
                    p.lineTo(x, baseY + amp * sin((x / len) * 2 * PI.toFloat() + shift))
                    x += 8f
                }
                p.lineTo(size.width, size.height)
                p.close()
                drawPath(p, color)
            }
            wave(size.height * 0.30f, 14f, size.width / 1.3f, phase, Color(0xFF2BB7CF).copy(alpha = 0.55f))
            wave(size.height * 0.48f, 12f, size.width / 1.1f, -phase + 1f, Color(0xFF0B7A93).copy(alpha = 0.9f))
            wave(size.height * 0.70f, 9f, size.width / 0.9f, phase + 2f, Color(0xFF085E73))
            drawCircle(Color.White.copy(alpha = 0.25f), 4f, Offset(size.width * 0.2f, size.height * 0.62f))
            drawCircle(Color.White.copy(alpha = 0.2f), 3f, Offset(size.width * 0.75f, size.height * 0.8f))
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AppLogo(size = 132.dp, modifier = Modifier.scale(logoScale.value))
            Spacer(Modifier.height(28.dp))
            Column(Modifier.alpha(contentAlpha.value), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("AquaDose", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Pool chemistry calculator",
                    color = AppColors.AquaLight, fontSize = 16.sp, fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Test · Dose · Swim",
                    color = AppColors.OnNavyMuted, fontSize = 13.sp, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(36.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .width(140.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = AppColors.AquaLight,
                    trackColor = Color.White.copy(alpha = 0.15f)
                )
            }
        }
        Text(
            "Chlorine · pH · Alkalinity · Calcium · CYA · Salt",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 11.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
                .alpha(contentAlpha.value)
        )
    }
}

fun openPlayStore(context: Context) {
    val pkg = context.packageName
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")))
    } catch (e: ActivityNotFoundException) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg")))
        }
    }
}

/** Shown when the user presses back on the home screen. */
@Composable
fun ExitDialog(onStay: () -> Unit, onExit: () -> Unit, onRate: () -> Unit) {
    Dialog(onDismissRequest = onStay, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppLogo(size = 64.dp)
            Text("Leaving already?", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(
                "Remember to retest your water after adding chemicals. See you at the pool!",
                style = MaterialTheme.typography.bodyMedium, color = AppColors.Muted, textAlign = TextAlign.Center
            )
            NativeAdCard(style = NativeStyle.MEDIUM)
            TextButton(onClick = onRate) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = AppColors.WarnDot)
                Spacer(Modifier.width(6.dp))
                Text("Enjoying AquaDose? Rate us", color = AppColors.TealDark, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onExit,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Exit", color = AppColors.Ink, fontWeight = FontWeight.Bold) }
                Button(
                    onClick = onStay,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Teal)
                ) { Text("Stay", fontWeight = FontWeight.Bold) }
            }
        }
    }
}
