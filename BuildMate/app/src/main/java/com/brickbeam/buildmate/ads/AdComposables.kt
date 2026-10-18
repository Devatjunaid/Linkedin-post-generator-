package com.brickbeam.buildmate.ads

import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brickbeam.buildmate.R
import com.brickbeam.buildmate.appContainer
import com.brickbeam.buildmate.ui.theme.LocalPalette
import com.brickbeam.buildmate.ui.theme.Palette
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

/** Immediate retries allowed before the banner switches to waiting [BANNER_RETRY_DELAY_MS]. */
private const val BANNER_IMMEDIATE_ATTEMPTS = 2
private const val BANNER_RETRY_DELAY_MS = 15_000L

/**
 * Anchored adaptive banner pinned to the bottom of the app.
 *
 * Request policy: the first 2 requests run back-to-back; if both fail, every further request
 * waits 15 seconds. A successful load resets the counter. Nothing is requested before consent.
 */
@Composable
fun BannerAdBar(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val ads = context.appContainer.ads
    val ready by ads.ready.collectAsStateWithLifecycle()
    if (!ready) return

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val widthDp = maxWidth.value.toInt().coerceAtLeast(320)
        var loaded by remember { mutableStateOf(false) }
        val handler = remember { Handler(Looper.getMainLooper()) }
        @Suppress("DEPRECATION")
        val adSize = remember(widthDp) { AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp) }
        val adView = remember(widthDp) {
            AdView(context).apply {
                adUnitId = AdUnits.BANNER
                setAdSize(adSize)
            }
        }

        DisposableEffect(adView) {
            var failures = 0
            val retry = Runnable { adView.loadAd(AdRequest.Builder().build()) }
            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    failures = 0
                    loaded = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    failures++
                    if (failures < BANNER_IMMEDIATE_ATTEMPTS) {
                        adView.loadAd(AdRequest.Builder().build())
                    } else {
                        handler.removeCallbacks(retry)
                        handler.postDelayed(retry, BANNER_RETRY_DELAY_MS)
                    }
                }
            }
            adView.loadAd(AdRequest.Builder().build())
            onDispose {
                handler.removeCallbacks(retry)
                adView.adListener = object : AdListener() {}
                adView.destroy()
            }
        }

        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner, adView) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> adView.pause()
                    Lifecycle.Event.ON_RESUME -> adView.resume()
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
                .height(if (loaded) adSize.height.dp else 1.dp),
        )
    }
}

/**
 * A native ad card that only takes up space once an ad has actually loaded. It carries the
 * "Ad" badge and the SDK's AdChoices icon, and matches the active theme so it reads as a
 * clearly-labelled sponsored card rather than app content.
 */
@Composable
fun NativeAdSlot(slot: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val ads = context.appContainer.ads
    val ready by ads.ready.collectAsStateWithLifecycle()
    var ad by remember(slot) { mutableStateOf<NativeAd?>(null) }
    LaunchedEffect(ready, slot) {
        if (ready) ad = ads.nativeFor(slot) { ad = it }
    }
    val palette = LocalPalette.current
    val current = ad ?: return
    AndroidView(
        factory = { ctx ->
            LayoutInflater.from(ctx).inflate(R.layout.native_ad_card, null) as NativeAdView
        },
        update = { view -> bindNativeAd(view, current, palette) },
        modifier = modifier.fillMaxWidth(),
    )
}

private fun bindNativeAd(view: NativeAdView, ad: NativeAd, p: Palette) {
    val container = view.findViewById<View>(R.id.ad_container)
    (container.background?.mutate() as? GradientDrawable)?.apply {
        setColor(p.surface.toArgb())
        setStroke(2, p.line.toArgb())
    }
    val headline = view.findViewById<TextView>(R.id.ad_headline)
    val body = view.findViewById<TextView>(R.id.ad_body)
    val advertiser = view.findViewById<TextView>(R.id.ad_advertiser)
    val icon = view.findViewById<ImageView>(R.id.ad_icon)
    val media = view.findViewById<MediaView>(R.id.ad_media)
    val cta = view.findViewById<Button>(R.id.ad_cta)

    headline.setTextColor(p.ink.toArgb())
    body.setTextColor(p.mute.toArgb())
    advertiser.setTextColor(p.mute.toArgb())
    (cta.background?.mutate() as? GradientDrawable)?.setColor(p.accent.toArgb())
    cta.setTextColor(p.onAccent.toArgb())

    view.headlineView = headline
    view.bodyView = body
    view.advertiserView = advertiser
    view.iconView = icon
    view.callToActionView = cta
    view.mediaView = media

    headline.text = ad.headline
    body.text = ad.body
    body.visibility = if (ad.body.isNullOrBlank()) View.GONE else View.VISIBLE
    advertiser.text = ad.advertiser
    advertiser.visibility = if (ad.advertiser.isNullOrBlank()) View.GONE else View.VISIBLE
    val iconDrawable = ad.icon?.drawable
    icon.setImageDrawable(iconDrawable)
    icon.visibility = if (iconDrawable == null) View.GONE else View.VISIBLE
    cta.text = ad.callToAction
    cta.visibility = if (ad.callToAction.isNullOrBlank()) View.GONE else View.VISIBLE
    ad.mediaContent?.let { media.mediaContent = it }

    view.setNativeAd(ad)
}
