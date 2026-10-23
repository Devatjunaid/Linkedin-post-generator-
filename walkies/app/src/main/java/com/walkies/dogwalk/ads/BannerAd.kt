package com.walkies.dogwalk.ads

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * Anchored adaptive banner. Its height is reserved up front so content never jumps under the
 * user's finger when the ad arrives.
 */
@Composable
fun AnchoredBanner(adsManager: AdsManager, modifier: Modifier = Modifier) {
    val ready by adsManager.adsReady.collectAsStateWithLifecycle()
    if (!ready) return
    val context = LocalContext.current
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val widthDp = maxWidth.value.toInt().coerceAtLeast(MIN_WIDTH_DP)
        val adSize = remember(widthDp) { AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, widthDp) }
        val controller = remember(widthDp) { BannerController(context, adSize) }
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        DisposableEffect(controller, lifecycle) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> controller.resume()
                    Lifecycle.Event.ON_PAUSE -> controller.pause()
                    else -> Unit
                }
            }
            lifecycle.addObserver(observer)
            onDispose {
                lifecycle.removeObserver(observer)
                controller.destroy()
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(adSize.height.dp)
                .background(Color.Transparent),
        ) {
            key(controller) {
                AndroidView(factory = { controller.adView }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

private const val MIN_WIDTH_DP = 320

/**
 * Loads the banner with the requested request cap: the first two requests go out back to back;
 * if both fail, every further request waits [RETRY_DELAY_MS] (15 s). A successful load resets
 * the count. Requests stop while the screen is paused.
 */
private class BannerController(context: Context, adSize: AdSize) {
    val adView = AdView(context).apply {
        adUnitId = AdUnits.banner
        setAdSize(adSize)
    }

    private val handler = Handler(Looper.getMainLooper())
    private var failedAttempts = 0
    private var resumed = false
    private var destroyed = false
    private var loadWhenResumed = true
    private var retryPending = false
    private val retry = Runnable {
        retryPending = false
        load()
    }

    init {
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                failedAttempts = 0
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                if (destroyed) return
                failedAttempts++
                if (failedAttempts < IMMEDIATE_ATTEMPTS) load() else scheduleRetry()
            }
        }
    }

    fun resume() {
        resumed = true
        adView.resume()
        if (retryPending) {
            handler.postDelayed(retry, RETRY_DELAY_MS)
        } else if (loadWhenResumed) {
            load()
        }
    }

    fun pause() {
        resumed = false
        adView.pause()
        handler.removeCallbacks(retry)
    }

    fun destroy() {
        destroyed = true
        handler.removeCallbacks(retry)
        adView.destroy()
    }

    private fun load() {
        if (destroyed) return
        if (!resumed) {
            loadWhenResumed = true
            return
        }
        loadWhenResumed = false
        adView.loadAd(AdRequest.Builder().build())
    }

    private fun scheduleRetry() {
        retryPending = true
        handler.removeCallbacks(retry)
        if (resumed) handler.postDelayed(retry, RETRY_DELAY_MS)
    }

    private companion object {
        const val IMMEDIATE_ATTEMPTS = 2
        const val RETRY_DELAY_MS = 15_000L
    }
}
