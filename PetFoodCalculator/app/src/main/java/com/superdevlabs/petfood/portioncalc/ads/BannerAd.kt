package com.superdevlabs.petfood.portioncalc.ads

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.superdevlabs.petfood.portioncalc.domain.BannerRetryPolicy
import com.superdevlabs.petfood.portioncalc.ui.LocalAppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private class BannerController(private val adView: AdView, private val scope: CoroutineScope) {
    private val policy = BannerRetryPolicy()
    private var retryJob: Job? = null
    var onLoadedChanged: (Boolean) -> Unit = {}

    init {
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                policy.onLoaded()
                onLoadedChanged(true)
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                val wait = policy.onFailed()
                Log.d("Ads", "Banner failed (${policy.consecutiveFailures}): ${error.message}; retry in ${wait}ms")
                retryJob?.cancel()
                retryJob = scope.launch {
                    delay(wait)
                    request()
                }
            }
        }
    }

    fun request() {
        adView.loadAd(AdRequest.Builder().build())
    }

    fun dispose() {
        retryJob?.cancel()
        adView.destroy()
    }
}

/** Anchored adaptive banner. Takes no space until an ad has actually loaded. */
@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val ads = LocalAppContainer.current.ads
    val ready by ads.ready.collectAsStateWithLifecycle()
    if (!ready) return

    val context = LocalContext.current
    val density = LocalDensity.current
    val widthDp = with(density) { LocalWindowInfo.current.containerSize.width.toDp() }.value.toInt()
    val adSize = remember(widthDp) { AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, widthDp) }
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(false) }

    val adView = remember(adSize) {
        AdView(context).apply {
            adUnitId = AdUnits.banner
            setAdSize(adSize)
        }
    }
    val controller = remember(adView) { BannerController(adView, scope) }
    controller.onLoadedChanged = { loaded = it }

    DisposableEffect(controller) {
        controller.request()
        onDispose { controller.dispose() }
    }
    LifecycleResumeEffect(adView) {
        adView.resume()
        onPauseOrDispose { adView.pause() }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (loaded) Modifier.height(adSize.height.dp) else Modifier.height(0.dp))
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(factory = { adView })
    }
}
