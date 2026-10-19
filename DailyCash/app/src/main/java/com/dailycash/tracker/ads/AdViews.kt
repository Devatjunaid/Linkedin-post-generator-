package com.dailycash.tracker.ads

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import com.dailycash.tracker.R
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

/**
 * Anchored adaptive banner with a request cap: the first two attempts go out back to back,
 * after that every new attempt waits 15 seconds, so a failing network or empty inventory
 * never floods AdMob with requests.
 */
@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val canRequest by AdsConsent.canRequestAds.collectAsStateWithLifecycle()
    if (!canRequest) return
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val context = LocalContext.current
        val widthDp = maxWidth.value.toInt().coerceAtLeast(320)
        val adSize = remember(widthDp) { AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp) }
        val adView = remember(widthDp) {
            AdView(context).apply {
                adUnitId = AdIds.banner
                setAdSize(adSize)
            }
        }
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(adView, lifecycleOwner) {
            val loader = CappedBannerLoader(adView)
            adView.adListener = loader
            loader.request()
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
                loader.cancel()
                adView.destroy()
            }
        }
        key(adView) {
            AndroidView(
                factory = { adView },
                modifier = Modifier.fillMaxWidth().height(adSize.height.dp),
            )
        }
    }
}

private class CappedBannerLoader(private val adView: AdView) : AdListener() {
    private val handler = Handler(Looper.getMainLooper())
    private var failedCalls = 0
    private var cancelled = false
    private val retry = Runnable { request() }

    fun request() {
        if (cancelled) return
        adView.loadAd(AdRequest.Builder().build())
    }

    fun cancel() {
        cancelled = true
        handler.removeCallbacks(retry)
    }

    override fun onAdLoaded() {
        failedCalls = 0
    }

    override fun onAdFailedToLoad(error: LoadAdError) {
        if (cancelled) return
        failedCalls++
        if (failedCalls < IMMEDIATE_ATTEMPTS) {
            request()
        } else {
            handler.removeCallbacks(retry)
            handler.postDelayed(retry, WAIT_MS)
        }
    }

    private companion object {
        const val IMMEDIATE_ATTEMPTS = 2
        const val WAIT_MS = 15_000L
    }
}

/** A native ad shown in natural gaps between sections. Takes no space until an ad is ready. */
@Composable
fun NativeAdCard(modifier: Modifier = Modifier) {
    val canRequest by AdsConsent.canRequestAds.collectAsStateWithLifecycle()
    if (!canRequest) return
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(Unit) {
        var disposed = false
        val loader = AdLoader.Builder(context, AdIds.native)
            .forNativeAd { ad ->
                if (disposed) {
                    ad.destroy()
                } else {
                    nativeAd?.destroy()
                    nativeAd = ad
                }
            }
            .withAdListener(object : AdListener() {})
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .build()
            )
            .build()
        loader.loadAd(AdRequest.Builder().build())
        onDispose {
            disposed = true
            nativeAd?.destroy()
            nativeAd = null
        }
    }

    val ad = nativeAd ?: return
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx -> LayoutInflater.from(ctx).inflate(R.layout.native_ad_card, null) as NativeAdView },
        update = { view -> bindNativeAd(view, ad) },
    )
}

private fun bindNativeAd(view: NativeAdView, ad: NativeAd) {
    val headline = view.findViewById<TextView>(R.id.ad_headline)
    val body = view.findViewById<TextView>(R.id.ad_body)
    val advertiser = view.findViewById<TextView>(R.id.ad_advertiser)
    val icon = view.findViewById<ImageView>(R.id.ad_icon)
    val cta = view.findViewById<Button>(R.id.ad_call_to_action)
    val media = view.findViewById<MediaView>(R.id.ad_media)

    headline.text = ad.headline
    view.headlineView = headline

    body.text = ad.body.orEmpty()
    body.visibility = if (ad.body.isNullOrBlank()) android.view.View.GONE else android.view.View.VISIBLE
    view.bodyView = body

    advertiser.text = ad.advertiser.orEmpty()
    view.advertiserView = advertiser

    val iconDrawable = ad.icon?.drawable
    icon.setImageDrawable(iconDrawable)
    icon.visibility = if (iconDrawable == null) android.view.View.GONE else android.view.View.VISIBLE
    view.iconView = icon

    cta.text = ad.callToAction.orEmpty()
    cta.visibility = if (ad.callToAction.isNullOrBlank()) android.view.View.INVISIBLE else android.view.View.VISIBLE
    view.callToActionView = cta

    media.setMediaContent(ad.mediaContent)
    view.mediaView = media

    view.setNativeAd(ad)
}
