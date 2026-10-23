package com.devatjunaid.examclock.ads

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devatjunaid.examclock.LocalAppContainer
import com.devatjunaid.examclock.domain.AdRetryPolicy
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import kotlinx.coroutines.delay

/**
 * Anchored adaptive banner.
 *
 * Request cap: if the first two load calls both fail, every further call waits 15 seconds.
 * Once an ad has loaded, the SDK's own refresh takes over and no manual retries are made.
 * The banner takes no space until an ad is actually showing.
 */
@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val ads = LocalAppContainer.current.ads
    val ready by ads.ready.collectAsStateWithLifecycle()
    if (!ready) return

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val context = LocalContext.current
        val widthDp = maxWidth.value.toInt().coerceAtLeast(320)
        val adSize = remember(widthDp) { AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, widthDp) }
        val policy = remember { AdRetryPolicy(immediateAttempts = 2, backoffMillis = 15_000L) }
        var loaded by remember { mutableStateOf(false) }
        var requestNumber by remember { mutableIntStateOf(0) }
        var nextDelay by remember { mutableLongStateOf(0L) }

        val adView = remember(adSize) {
            AdView(context).apply {
                adUnitId = ads.bannerUnitId
                setAdSize(adSize)
            }
        }

        DisposableEffect(adView) {
            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    policy.onSuccess()
                    loaded = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    if (!loaded) {
                        nextDelay = policy.onFailure()
                        requestNumber++
                    }
                }
            }
            onDispose { adView.destroy() }
        }

        LaunchedEffect(adView, requestNumber) {
            if (requestNumber > 0) delay(nextDelay)
            adView.loadAd(AdRequest.Builder().build())
        }

        val lifecycleOwner = LocalLifecycleOwner.current
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

        AndroidView(
            factory = { adView },
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .height(if (loaded) adSize.height.dp else 0.dp),
        )
    }
}
