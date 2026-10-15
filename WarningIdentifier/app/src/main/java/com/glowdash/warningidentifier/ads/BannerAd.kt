package com.glowdash.warningidentifier.ads

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.glowdash.warningidentifier.BuildConfig
import com.glowdash.warningidentifier.ui.theme.WiColors
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * One anchored adaptive banner shared by the whole app, docked at the bottom.
 *
 * Why it feels light:
 * - A single AdView lives at the root, so it is NOT reloaded on every screen change.
 * - It takes zero space until an ad has actually loaded, then slides in smoothly
 *   (no empty grey box, no layout jump while loading).
 * - It hides itself (and pauses) on the splash and camera screens.
 * - It blends into the app with the same dark surface and a hairline divider.
 * - Standard anchored banner only: no collapsible / expanding formats.
 */
@Composable
fun AnchoredBanner(visible: Boolean, modifier: Modifier = Modifier) {
    val ready by AdsManager.ready.collectAsState()
    if (!ready) return

    val context = LocalContext.current
    val widthDp = LocalConfiguration.current.screenWidthDp
    val adSize = remember(widthDp) {
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
    }
    var loaded by remember { mutableStateOf(false) }

    val adView = remember(adSize) {
        AdView(context).apply {
            adUnitId = BuildConfig.ADMOB_BANNER_ID
            setAdSize(adSize)
            adListener = object : AdListener() {
                override fun onAdLoaded() { loaded = true }
                override fun onAdFailedToLoad(error: LoadAdError) { loaded = false }
            }
            loadAd(AdRequest.Builder().build())
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(adView, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> adView.resume()
                Lifecycle.Event.ON_PAUSE -> adView.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            adView.destroy()
        }
    }
    LaunchedEffect(visible) { if (visible) adView.resume() else adView.pause() }

    val show = visible && loaded
    val height by animateDpAsState(
        targetValue = if (show) adSize.height.dp + 1.dp else 0.dp,
        animationSpec = tween(durationMillis = 380),
        label = "bannerHeight",
    )

    Column(
        modifier
            .fillMaxWidth()
            .height(height)
            .clipToBounds()
            .background(WiColors.Bg),
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(WiColors.Stroke))
        AndroidView(
            factory = { adView },
            modifier = Modifier.fillMaxWidth().height(adSize.height.dp),
        )
    }
}
