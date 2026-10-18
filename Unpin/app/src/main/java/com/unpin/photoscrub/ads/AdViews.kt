package com.unpin.photoscrub.ads

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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
import com.unpin.photoscrub.BuildConfig
import com.unpin.photoscrub.R
import com.unpin.photoscrub.UnpinApp
import com.unpin.photoscrub.core.BannerRetryPolicy
import com.unpin.photoscrub.ui.theme.Paper

/**
 * Anchored adaptive banner. Request cap: two attempts back to back, then one
 * request every 15 seconds until an ad fills. Paused with the screen.
 */
@Composable
fun BannerAdBar(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val ads = (context.applicationContext as UnpinApp).ads
    val ready by ads.ready.collectAsStateWithLifecycle()
    if (!ready) return

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val widthDp = maxWidth.value.toInt().coerceAtLeast(320)
        val adSize = remember(widthDp) { AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp) }
        val adView = remember(adSize) {
            AdView(context).apply {
                adUnitId = BuildConfig.AD_BANNER_ID
                setAdSize(adSize)
            }
        }
        val lifecycleOwner = LocalLifecycleOwner.current

        DisposableEffect(adView, lifecycleOwner) {
            val handler = Handler(Looper.getMainLooper())
            val policy = BannerRetryPolicy(immediateAttempts = 2, backoffMs = 15_000L)
            var waitingForResume = false
            val request = object : Runnable {
                override fun run() {
                    if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        adView.loadAd(AdRequest.Builder().build())
                    } else {
                        waitingForResume = true
                    }
                }
            }
            adView.adListener = object : AdListener() {
                override fun onAdLoaded() = policy.onSuccess()

                override fun onAdFailedToLoad(error: LoadAdError) {
                    handler.removeCallbacks(request)
                    handler.postDelayed(request, policy.onFailure())
                }
            }
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> {
                        adView.resume()
                        if (waitingForResume) {
                            waitingForResume = false
                            handler.post(request)
                        }
                    }
                    Lifecycle.Event.ON_PAUSE -> adView.pause()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            handler.post(request)
            onDispose {
                handler.removeCallbacksAndMessages(null)
                lifecycleOwner.lifecycle.removeObserver(observer)
                adView.destroy()
            }
        }

        AndroidView(
            factory = { adView },
            modifier = Modifier
                .fillMaxWidth()
                .height(adSize.height.dp)
                .background(Paper),
        )
    }
}

/** Native ad card for free space between sections. Renders nothing until an ad fills. */
@Composable
fun NativeAdCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val ads = (context.applicationContext as UnpinApp).ads
    val ready by ads.ready.collectAsStateWithLifecycle()
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(ready) {
        var disposed = false
        if (ready) {
            AdLoader.Builder(context, BuildConfig.AD_NATIVE_ID)
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
                .loadAd(AdRequest.Builder().build())
        }
        onDispose { disposed = true }
    }
    DisposableEffect(Unit) {
        onDispose { nativeAd?.destroy() }
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
    val cta = view.findViewById<Button>(R.id.ad_cta)
    val icon = view.findViewById<ImageView>(R.id.ad_icon)
    val advertiser = view.findViewById<TextView>(R.id.ad_advertiser)
    val media = view.findViewById<MediaView>(R.id.ad_media)

    headline.text = ad.headline
    view.headlineView = headline

    body.text = ad.body
    body.visibility = if (ad.body.isNullOrBlank()) View.GONE else View.VISIBLE
    view.bodyView = body

    cta.text = ad.callToAction
    cta.visibility = if (ad.callToAction.isNullOrBlank()) View.GONE else View.VISIBLE
    view.callToActionView = cta

    val drawable = ad.icon?.drawable
    icon.setImageDrawable(drawable)
    icon.visibility = if (drawable == null) View.GONE else View.VISIBLE
    view.iconView = icon

    advertiser.text = ad.advertiser
    advertiser.visibility = if (ad.advertiser.isNullOrBlank()) View.GONE else View.VISIBLE
    view.advertiserView = advertiser

    val content = ad.mediaContent
    if (content != null) {
        media.setMediaContent(content)
        media.visibility = View.VISIBLE
    } else {
        media.visibility = View.GONE
    }
    view.mediaView = media

    view.setNativeAd(ad)
}
