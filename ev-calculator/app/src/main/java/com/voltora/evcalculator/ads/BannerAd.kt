package com.voltora.evcalculator.ads

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.voltora.evcalculator.ui.theme.Ev

/**
 * Anchored adaptive banner. It stays collapsed (0 dp) until an ad has actually loaded, then
 * slides in under the navigation bar on the same dark background, so it never leaves an
 * empty box or pushes content around.
 */
@Composable
fun BannerAd(modifier: Modifier = Modifier) {
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
            setAdSize(adSize)
            adUnitId = AdIds.BANNER
            adListener = object : AdListener() {
                override fun onAdLoaded() { loaded = true }
                override fun onAdFailedToLoad(error: LoadAdError) { loaded = false }
            }
        }
    }
    LaunchedEffect(adView) { adView.loadAd(AdRequest.Builder().build()) }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(adView, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> adView.resume()
                Lifecycle.Event.ON_PAUSE -> adView.pause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            adView.destroy()
        }
    }

    // The view must stay attached to load, so it is always composed; only its height animates.
    val height by animateDpAsState(if (loaded) adSize.height.dp else 1.dp, label = "banner")
    Box(
        modifier
            .fillMaxWidth()
            .background(Ev.Ink)
            .padding(top = if (loaded) 4.dp else 0.dp)
            .height(height)
            .clipToBounds(),
        contentAlignment = Alignment.TopCenter,
    ) {
        // Measured at full ad height even while collapsed, so the SDK always has room to render.
        AndroidView(factory = { adView }, modifier = Modifier.requiredHeight(adSize.height.dp))
    }
}
