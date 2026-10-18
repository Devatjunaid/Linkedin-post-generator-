package com.inkformlabs.pdfsigner.ads

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.inkformlabs.pdfsigner.AppConfig
import com.inkformlabs.pdfsigner.R
import com.inkformlabs.pdfsigner.ui.LocalAppContainer
import com.inkformlabs.pdfsigner.ui.theme.InkPalette
import com.inkformlabs.pdfsigner.ui.theme.LocalInk
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Anchored adaptive banner pinned to the bottom of the screen.
 *
 * Retry policy: the first failed request is retried at once; after 2 failed requests in a row the
 * banner waits 15 seconds before requesting again (and keeps that 15 s spacing until one loads).
 * The banner takes no space until an ad has loaded, and is separated from app buttons by a divider.
 */
@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val ads = LocalAppContainer.current.ads
    val ready by ads.ready.collectAsStateWithLifecycle()
    if (!ready) return

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val palette = LocalInk.current

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val widthDp = maxWidth.value.toInt().coerceAtLeast(320)
        var loaded by remember { mutableStateOf(false) }
        val adSize = remember(widthDp) { AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, widthDp) }
        val adView = remember(widthDp) {
            AdView(context).apply {
                adUnitId = AdUnits.BANNER
                setAdSize(adSize)
            }
        }

        DisposableEffect(adView) {
            var consecutiveFailures = 0
            var retryJob: Job? = null
            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    consecutiveFailures = 0
                    loaded = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    consecutiveFailures++
                    val wait = if (consecutiveFailures < AppConfig.BANNER_FAST_ATTEMPTS) 0L else AppConfig.BANNER_RETRY_DELAY_MS
                    retryJob?.cancel()
                    retryJob = scope.launch {
                        delay(wait)
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
                retryJob?.cancel()
                lifecycleOwner.lifecycle.removeObserver(observer)
                adView.destroy()
            }
        }

        val heightDp = with(LocalDensity.current) { adSize.getHeightInPixels(context).toDp() }
        Column(Modifier.fillMaxWidth()) {
            AnimatedVisibility(visible = loaded, enter = expandVertically() + fadeIn()) {
                HorizontalDivider(color = palette.line)
            }
            AndroidView(
                factory = { adView },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (loaded) heightDp else 0.dp),
            )
        }
    }
}

/**
 * Native ad shown in natural gaps between content (lists, below forms). Takes no space until loaded.
 */
@Composable
fun NativeAdCard(modifier: Modifier = Modifier) {
    val ads = LocalAppContainer.current.ads
    val ready by ads.ready.collectAsStateWithLifecycle()
    if (!ready) return

    val context = LocalContext.current
    val palette = LocalInk.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(Unit) {
        var disposed = false
        val loader = AdLoader.Builder(context, AdUnits.NATIVE)
            .forNativeAd { ad ->
                if (disposed) {
                    ad.destroy()
                } else {
                    nativeAd?.destroy()
                    nativeAd = ad
                }
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    // Simply stay hidden; the gap closes up.
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_LANDSCAPE)
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
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(20.dp)),
        factory = { ctx ->
            LayoutInflater.from(ctx).inflate(R.layout.native_ad_card, null, false) as NativeAdView
        },
        update = { view -> bindNativeAd(view, ad, palette) },
    )
}

private fun bindNativeAd(view: NativeAdView, ad: NativeAd, palette: InkPalette) {
    val density = view.resources.displayMetrics.density
    fun rounded(color: Int, radiusDp: Float, strokeColor: Int? = null) = GradientDrawable().apply {
        cornerRadius = radiusDp * density
        setColor(color)
        if (strokeColor != null) setStroke((1 * density).toInt().coerceAtLeast(1), strokeColor)
    }

    view.findViewById<View>(R.id.ad_container).background =
        rounded(palette.surface.toArgb(), 20f, palette.line.toArgb())

    val badge = view.findViewById<TextView>(R.id.ad_badge)
    badge.background = rounded(palette.accent2.toArgb(), 6f)
    badge.setTextColor(palette.bg.toArgb())

    val headline = view.findViewById<TextView>(R.id.ad_headline)
    headline.text = ad.headline
    headline.setTextColor(palette.ink.toArgb())
    view.headlineView = headline

    val advertiser = view.findViewById<TextView>(R.id.ad_advertiser)
    advertiser.text = ad.advertiser.orEmpty()
    advertiser.setTextColor(palette.muted.toArgb())
    advertiser.visibility = if (ad.advertiser.isNullOrBlank()) View.GONE else View.VISIBLE
    view.advertiserView = advertiser

    val body = view.findViewById<TextView>(R.id.ad_body)
    body.text = ad.body.orEmpty()
    body.setTextColor(palette.muted.toArgb())
    body.visibility = if (ad.body.isNullOrBlank()) View.GONE else View.VISIBLE
    view.bodyView = body

    val icon = view.findViewById<ImageView>(R.id.ad_icon)
    val iconDrawable = ad.icon?.drawable
    icon.setImageDrawable(iconDrawable)
    icon.visibility = if (iconDrawable == null) View.GONE else View.VISIBLE
    view.iconView = icon

    val media = view.findViewById<MediaView>(R.id.ad_media)
    view.mediaView = media
    val content = ad.mediaContent
    if (content != null) {
        media.setMediaContent(content)
        media.visibility = View.VISIBLE
    } else {
        media.visibility = View.GONE
    }

    val cta = view.findViewById<Button>(R.id.ad_call_to_action)
    cta.text = ad.callToAction.orEmpty()
    cta.visibility = if (ad.callToAction.isNullOrBlank()) View.GONE else View.VISIBLE
    cta.background = rounded(palette.hi.toArgb(), 14f)
    cta.setTextColor(palette.onHi.toArgb())
    view.callToActionView = cta

    view.setNativeAd(ad)
}
