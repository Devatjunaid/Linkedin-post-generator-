package com.harrycodess.fueltracker.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
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
import com.harrycodess.fueltracker.ads.AdsConfig
import kotlinx.coroutines.delay

private const val TAG = "FuelTrackerAds"
private const val MAX_RETRIES = 3

/**
 * Full-width anchored adaptive banner (Google's recommended banner type).
 *
 * The slot height is the banner's own height, calculated before loading, so the
 * layout never shifts. There is no placeholder drawn over or under the ad.
 * Load failures are logged to Logcat with tag "FuelTrackerAds" and retried.
 */
@Composable
fun AdBannerSlot(canRequestAds: Boolean, modifier: Modifier = Modifier) {
    val inPreview = LocalInspectionMode.current
    val context = LocalContext.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val adSize = remember(screenWidthDp, inPreview) {
        if (inPreview) {
            AdSize.BANNER
        } else {
            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, screenWidthDp)
                .takeIf { it.height > 0 } ?: AdSize.BANNER
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .height(adSize.height.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (canRequestAds && !inPreview) {
            BannerAd(adSize)
        }
    }
}

@Composable
private fun BannerAd(adSize: AdSize) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var failures by remember { mutableIntStateOf(0) }

    val adView = remember(adSize) {
        AdView(context).apply {
            setAdSize(adSize)
            adUnitId = AdsConfig.bannerId
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.d(TAG, "Banner loaded (${adSize.width}x${adSize.height})")
                    failures = 0
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    // 0 = internal error, 1 = invalid request (check the banner ID),
                    // 2 = network error, 3 = no fill (no ad available right now).
                    Log.w(TAG, "Banner failed to load: code=${error.code} message=${error.message}")
                    failures += 1
                }
            }
            loadAd(AdRequest.Builder().build())
        }
    }

    // Retry after a failure with a growing delay (30s, 60s, 90s).
    LaunchedEffect(adView, failures) {
        if (failures in 1..MAX_RETRIES) {
            delay(30_000L * failures)
            adView.loadAd(AdRequest.Builder().build())
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
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            adView.destroy()
        }
    }

    AndroidView(
        factory = { adView },
        modifier = Modifier
            .fillMaxWidth()
            .height(adSize.height.dp),
    )
}
