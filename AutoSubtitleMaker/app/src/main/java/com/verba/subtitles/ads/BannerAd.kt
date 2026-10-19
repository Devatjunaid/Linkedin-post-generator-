package com.verba.subtitles.ads

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Banner request policy:
 *  - request 1 fails → request 2 is sent immediately;
 *  - request 2 fails → wait 15 seconds, then request again;
 *  - every further failure waits 15 seconds again, up to [MAX_REQUESTS] per screen visit
 *    so a device without fill doesn't spam AdMob.
 * A successful load resets the counter.
 */
class BannerRequestController(
    private val adView: AdView,
    private val scope: CoroutineScope,
) {
    private var failures = 0
    private var requests = 0
    private var retryJob: Job? = null

    init {
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                failures = 0
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                failures++
                scheduleRetry()
            }
        }
    }

    fun start() {
        if (requests == 0) request()
    }

    fun stop() {
        retryJob?.cancel()
    }

    private fun request() {
        requests++
        adView.loadAd(AdRequest.Builder().build())
    }

    private fun scheduleRetry() {
        if (requests >= MAX_REQUESTS) return
        retryJob?.cancel()
        retryJob = scope.launch {
            if (failures >= IMMEDIATE_ATTEMPTS) delay(RETRY_DELAY_MS)
            request()
        }
    }

    companion object {
        const val IMMEDIATE_ATTEMPTS = 2
        const val RETRY_DELAY_MS = 15_000L
        const val MAX_REQUESTS = 10
    }
}

/** Anchored adaptive banner. Renders nothing until consent allows ads. */
@Composable
fun BannerAd(ads: AdsManager, modifier: Modifier = Modifier) {
    val ready by ads.ready.collectAsStateWithLifecycle()
    if (!ready) return
    BoxWithConstraints(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val context = LocalContext.current
        val widthDp = maxWidth.value.toInt().coerceAtLeast(320)
        val adSize = remember(widthDp) { bannerSize(context, widthDp) }
        val scope = rememberCoroutineScope()
        val adView = remember(adSize) {
            AdView(context).apply {
                adUnitId = AdUnits.banner
                setAdSize(adSize)
            }
        }
        val controller = remember(adView) { BannerRequestController(adView, scope) }
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
            controller.start()
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                controller.stop()
                adView.destroy()
            }
        }

        Box(Modifier.fillMaxWidth().height(adSize.height.dp), contentAlignment = Alignment.Center) {
            AndroidView(factory = { adView })
        }
    }
}

@Suppress("DEPRECATION")
private fun bannerSize(context: Context, widthDp: Int): AdSize =
    AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
