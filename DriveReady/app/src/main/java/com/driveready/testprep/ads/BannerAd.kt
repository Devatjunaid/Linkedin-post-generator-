package com.driveready.testprep.ads

import android.util.Log
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.driveready.testprep.BuildConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import kotlinx.coroutines.delay

private const val TAG = "BannerAd"

/**
 * Anchored adaptive banner with a request cap: the first two attempts are immediate; once two
 * attempts in a row have failed, every further request waits 15 seconds ([AdPolicy]).
 * Takes no space until an ad has loaded.
 */
@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val ready by AdsManager.ready.collectAsStateWithLifecycle()
    if (!ready) return

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val context = LocalContext.current
        val widthDp = maxWidth.value.toInt().coerceAtLeast(320)
        val adSize = remember(widthDp) { AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, widthDp) }

        var loaded by remember { mutableStateOf(false) }
        var failures by remember { mutableIntStateOf(0) }
        // Bumped to trigger a (re)load request.
        var requestToken by remember { mutableIntStateOf(0) }

        val adView = remember(adSize) {
            AdView(context).apply {
                adUnitId = BuildConfig.BANNER_AD_UNIT_ID
                setAdSize(adSize)
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        loaded = true
                        failures = 0
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.d(TAG, "Banner failed (${failures + 1}): ${error.code} ${error.message}")
                        loaded = false
                        failures += 1
                        requestToken += 1
                    }
                }
            }
        }

        LaunchedEffect(adView, requestToken) {
            val wait = if (requestToken == 0) 0L else AdPolicy.bannerRetryDelayMs(failures)
            if (wait > 0) delay(wait)
            adView.loadAd(AdRequest.Builder().build())
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

        // Wrap-content height: the AdView measures 0 until an ad arrives, so no empty gap.
        AndroidView(
            factory = { adView },
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .semantics { contentDescription = if (loaded) "Advertisement" else "" }
        )
    }
}
