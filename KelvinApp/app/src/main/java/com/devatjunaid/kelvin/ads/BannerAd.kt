package com.devatjunaid.kelvin.ads

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devatjunaid.kelvin.BuildConfig
import com.devatjunaid.kelvin.graph
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Anchored adaptive banner. Request policy (see [BannerRetryPolicy]): the first two requests go out
 * back to back; if both fail, every further request waits 15 seconds. Takes no space until an ad loads.
 */
@Composable
fun AdaptiveBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val adsReady by context.graph.consent.adsReady.collectAsStateWithLifecycle()
    if (!adsReady) return

    val widthDp = LocalConfiguration.current.screenWidthDp
    val adSize = remember(widthDp) { AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp) }
    val adView = remember(adSize) {
        AdView(context).apply {
            adUnitId = BuildConfig.ADMOB_BANNER_ID
            setAdSize(adSize)
        }
    }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(adView) {
        val policy = BannerRetryPolicy(immediateAttempts = 2, retryDelayMs = 15_000L)
        var retryJob: Job? = null
        fun request() = adView.loadAd(AdRequest.Builder().build())

        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                policy.onLoaded()
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                val waitMs = policy.onFailed()
                retryJob?.cancel()
                retryJob = scope.launch {
                    delay(waitMs)
                    request()
                }
            }
        }
        request()
        onDispose {
            retryJob?.cancel()
            adView.destroy()
        }
    }

    DisposableEffect(lifecycleOwner, adView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> adView.resume()
                Lifecycle.Event.ON_PAUSE -> adView.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier.fillMaxWidth().animateContentSize(), contentAlignment = Alignment.Center) {
        AndroidView(factory = { adView }, modifier = Modifier.fillMaxWidth().wrapContentHeight())
    }
}
