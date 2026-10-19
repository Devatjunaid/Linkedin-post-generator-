package com.sunarc.inverterlogger.ads

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.sunarc.inverterlogger.BuildConfig
import com.sunarc.inverterlogger.R
import com.sunarc.inverterlogger.domain.AdConfig
import com.sunarc.inverterlogger.domain.BannerRetryPolicy
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Anchored adaptive banner. The first two requests go out back to back; after two failures
 * each new request waits 15 seconds. The slot takes no space until an ad has loaded.
 */
@Composable
fun BannerAdSlot(ads: AdsManager, modifier: Modifier = Modifier) {
    val ready by ads.ready.collectAsStateWithLifecycle()
    if (!ready) return
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    BoxWithConstraints(modifier.fillMaxWidth().animateContentSize()) {
        val widthDp = maxWidth.value.toInt().coerceAtLeast(320)
        var loaded by remember { mutableStateOf(false) }
        val policy = remember { BannerRetryPolicy(AdConfig.BANNER_QUICK_ATTEMPTS, AdConfig.BANNER_BACKOFF_MS) }
        val adView = remember(widthDp) {
            AdView(context).apply {
                adUnitId = BuildConfig.BANNER_AD_UNIT
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp))
            }
        }

        DisposableEffect(adView, lifecycleOwner) {
            var retryJob: Job? = null
            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    policy.onLoaded()
                    loaded = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    policy.onFailed()
                    val wait = policy.delayBeforeNextAttempt()
                    retryJob?.cancel()
                    retryJob = scope.launch {
                        delay(wait)
                        adView.loadAd(ads.request())
                    }
                }
            }
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> adView.resume()
                    Lifecycle.Event.ON_PAUSE -> adView.pause()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            adView.loadAd(ads.request())
            onDispose {
                retryJob?.cancel()
                lifecycleOwner.lifecycle.removeObserver(observer)
                adView.destroy()
            }
        }

        AndroidView(
            factory = { adView },
            modifier = if (loaded) Modifier.fillMaxWidth() else Modifier.fillMaxWidth().height(0.dp)
        )
    }
}

/**
 * Native ad shown in a free gap between sections of a screen. It is clearly marked "Ad",
 * keeps the AdChoices icon, and collapses to nothing if no ad is available.
 */
@Composable
fun NativeAdSlot(ads: AdsManager, modifier: Modifier = Modifier) {
    val ready by ads.ready.collectAsStateWithLifecycle()
    if (!ready) return
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(Unit) {
        var disposed = false
        val loader = AdLoader.Builder(context, BuildConfig.NATIVE_AD_UNIT)
            .forNativeAd { ad ->
                if (disposed) ad.destroy() else {
                    nativeAd?.destroy()
                    nativeAd = ad
                }
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) = Unit
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .build()
            )
            .build()
        loader.loadAd(ads.request())
        onDispose {
            disposed = true
            nativeAd?.destroy()
            nativeAd = null
        }
    }

    val ad = nativeAd ?: return
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            LayoutInflater.from(ctx).inflate(R.layout.native_ad_card, FrameLayout(ctx), false) as NativeAdView
        },
        update = { view -> bindNativeAd(view, ad) }
    )
}

private fun bindNativeAd(view: NativeAdView, ad: NativeAd) {
    fun View.shownIf(value: Any?) { visibility = if (value == null) View.GONE else View.VISIBLE }

    view.headlineView = view.findViewById<TextView>(R.id.ad_headline).also { it.text = ad.headline }
    view.bodyView = view.findViewById<TextView>(R.id.ad_body).also { it.text = ad.body; it.shownIf(ad.body) }
    view.callToActionView = view.findViewById<Button>(R.id.ad_cta).also { it.text = ad.callToAction; it.shownIf(ad.callToAction) }
    view.advertiserView = view.findViewById<TextView>(R.id.ad_advertiser).also { it.text = ad.advertiser; it.shownIf(ad.advertiser) }
    view.iconView = view.findViewById<ImageView>(R.id.ad_icon).also { iv ->
        val icon = ad.icon
        iv.setImageDrawable(icon?.drawable)
        iv.shownIf(icon)
    }
    view.mediaView = view.findViewById<MediaView>(R.id.ad_media).also { mv ->
        ad.mediaContent?.let { mv.setMediaContent(it) }
        mv.shownIf(ad.mediaContent)
    }
    view.setNativeAd(ad)
}
