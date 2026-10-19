package com.devatjunaid.isthisai.ads

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devatjunaid.isthisai.BuildConfig
import com.devatjunaid.isthisai.R
import com.devatjunaid.isthisai.ui.theme.Ink
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.AdLoader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Anchored adaptive banner. Its height is reserved from the start so content never jumps
 * when the ad arrives (shifting layouts cause accidental clicks, which AdMob prohibits).
 *
 * Request rule: the first two requests go out back to back; after the second failure every
 * new request waits 15 seconds ([AdPolicy.bannerDelayAfterFailure]).
 */
@Composable
fun BannerAdBar(modifier: Modifier = Modifier) {
    val ready by AdsManager.ready.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val widthDp = LocalConfiguration.current.screenWidthDp
    @Suppress("DEPRECATION")
    val adSize = remember(widthDp) { AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp) }

    Box(
        modifier
            .fillMaxWidth()
            .height(adSize.height.dp)
            .background(Ink),
        contentAlignment = Alignment.Center,
    ) {
        if (ready) BannerAdView(adSize)
    }
}

@Composable
private fun BannerAdView(adSize: AdSize) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val adView = remember(adSize) {
        AdView(context).apply {
            adUnitId = BuildConfig.BANNER_AD_UNIT
            setAdSize(adSize)
        }
    }
    DisposableEffect(adView) {
        var failures = 0
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                failures = 0
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                failures++
                val wait = AdPolicy.bannerDelayAfterFailure(failures)
                scope.launch {
                    if (wait > 0) delay(wait)
                    adView.loadAd(AdRequest.Builder().build())
                }
            }
        }
        adView.loadAd(AdRequest.Builder().build())
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> adView.pause()
                Lifecycle.Event.ON_RESUME -> adView.resume()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            adView.destroy()
        }
    }
    AndroidView(factory = { adView })
}

enum class NativeStyle(val layout: Int) { SMALL(R.layout.native_ad_small), MEDIUM(R.layout.native_ad_medium) }

/**
 * A native ad that matches the app's look and is always marked with an "Ad" badge plus the
 * AdChoices icon. It takes no space until an ad has loaded, and retries at most twice.
 */
@Composable
fun NativeAdCard(style: NativeStyle, modifier: Modifier = Modifier) {
    val ready by AdsManager.ready.collectAsStateWithLifecycle()
    if (ready) NativeAdLoader(style, modifier)
}

@Composable
private fun NativeAdLoader(style: NativeStyle, modifier: Modifier) {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(style) {
        var disposed = false
        val handler = Handler(Looper.getMainLooper())
        var loaded: NativeAd? = null
        fun load(retry: Int) {
            AdLoader.Builder(context, BuildConfig.NATIVE_AD_UNIT)
                .forNativeAd { ad ->
                    if (disposed) {
                        ad.destroy()
                    } else {
                        loaded?.destroy()
                        loaded = ad
                        nativeAd = ad
                    }
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        if (!disposed && retry < AdPolicy.NATIVE_MAX_RETRIES) {
                            handler.postDelayed({ if (!disposed) load(retry + 1) }, AdPolicy.preloadRetryDelay(retry + 1))
                        }
                    }
                })
                .withNativeAdOptions(
                    NativeAdOptions.Builder()
                        .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                        .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_LANDSCAPE)
                        .build()
                )
                .build()
                .loadAd(AdRequest.Builder().build())
        }
        load(0)
        onDispose {
            disposed = true
            handler.removeCallbacksAndMessages(null)
            loaded?.destroy()
            nativeAd = null
        }
    }

    val ad = nativeAd
    if (ad != null) {
        AndroidView(
            modifier = modifier.fillMaxWidth(),
            factory = { ctx ->
                LayoutInflater.from(ctx).inflate(style.layout, FrameLayout(ctx), false) as NativeAdView
            },
            update = { view -> bindNativeAd(view, ad) },
        )
    }
}

private fun bindNativeAd(view: NativeAdView, ad: NativeAd) {
    if (view.tag === ad) return
    view.tag = ad
    val headline = view.findViewById<TextView>(R.id.ad_headline)
    val body = view.findViewById<TextView>(R.id.ad_body)
    val cta = view.findViewById<Button>(R.id.ad_call_to_action)
    val icon = view.findViewById<ImageView>(R.id.ad_icon)
    val advertiser = view.findViewById<TextView>(R.id.ad_advertiser)
    val media: MediaView? = view.findViewById(R.id.ad_media)

    headline.text = ad.headline
    view.headlineView = headline

    body.text = ad.body
    body.visibility = if (ad.body.isNullOrBlank()) View.GONE else View.VISIBLE
    view.bodyView = body

    cta.text = ad.callToAction
    cta.visibility = if (ad.callToAction.isNullOrBlank()) View.INVISIBLE else View.VISIBLE
    view.callToActionView = cta

    val drawable = ad.icon?.drawable
    icon.setImageDrawable(drawable)
    icon.visibility = if (drawable == null) View.GONE else View.VISIBLE
    view.iconView = icon

    advertiser.text = ad.advertiser ?: ad.store ?: ""
    view.advertiserView = advertiser

    if (media != null) {
        view.mediaView = media
    }
    view.setNativeAd(ad)
}
