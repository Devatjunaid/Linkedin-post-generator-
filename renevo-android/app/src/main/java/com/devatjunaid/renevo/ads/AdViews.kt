package com.devatjunaid.renevo.ads

import android.graphics.drawable.GradientDrawable
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.devatjunaid.renevo.BuildConfig
import com.devatjunaid.renevo.R
import com.devatjunaid.renevo.ui.theme.RenevoTheme
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

/** Anchored adaptive banner. Sits below the navigation bar, never over content. */
@Composable
fun BannerAd(ads: AdsManager, modifier: Modifier = Modifier) {
    val ready by ads.ready.collectAsState()
    if (!ready) return
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val widthDp = maxWidth.value.toInt().coerceAtLeast(320)
        val adSize = remember(widthDp) {
            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
        }
        val adView = remember(adSize) {
            AdView(context).apply {
                adUnitId = BuildConfig.ADMOB_BANNER_ID
                setAdSize(adSize)
                loadAd(ads.newRequest())
            }
        }
        DisposableEffect(adView, lifecycle) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> adView.pause()
                    Lifecycle.Event.ON_RESUME -> adView.resume()
                    else -> Unit
                }
            }
            lifecycle.addObserver(observer)
            onDispose {
                lifecycle.removeObserver(observer)
                adView.destroy()
            }
        }
        AndroidView(
            factory = { adView },
            modifier = Modifier.fillMaxWidth().height(adSize.height.dp),
        )
    }
}

enum class NativeStyle { SMALL, MEDIUM }

/**
 * Native ad card. Takes no space until an ad has loaded, carries a clear "Ad" badge and a
 * distinct outlined style so it can't be mistaken for a subscription.
 */
@Composable
fun NativeAdCard(ads: AdsManager, style: NativeStyle, modifier: Modifier = Modifier) {
    val ready by ads.ready.collectAsState()
    if (!ready) return
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(style) {
        var disposed = false
        val loader = AdLoader.Builder(context, BuildConfig.ADMOB_NATIVE_ID)
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
                    .build(),
            )
            .build()
        loader.loadAd(ads.newRequest())
        onDispose {
            disposed = true
            nativeAd?.destroy()
            nativeAd = null
        }
    }

    val ad = nativeAd ?: return
    val colors = RenevoTheme.colors
    val density = LocalDensity.current
    val surface = colors.surface.toArgb()
    val outline = colors.outline.toArgb()
    val text = colors.onSurface.toArgb()
    val muted = colors.muted.toArgb()
    val accent = colors.accent.toArgb()
    val badgeBg = colors.warnContainer.toArgb()
    val badgeFg = colors.warnText.toArgb()
    val radius = with(density) { 18.dp.toPx() }
    val small = with(density) { 6.dp.toPx() }
    val stroke = with(density) { 1.dp.roundToPx() }

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            val layout = if (style == NativeStyle.SMALL) R.layout.native_ad_small else R.layout.native_ad_medium
            LayoutInflater.from(ctx).inflate(layout, null) as NativeAdView
        },
        update = { view ->
            view.findViewById<View>(R.id.ad_container).background = GradientDrawable().apply {
                cornerRadius = radius
                setColor(surface)
                setStroke(stroke * 2, outline)
            }
            view.findViewById<TextView>(R.id.ad_badge).apply {
                setTextColor(badgeFg)
                background = GradientDrawable().apply { cornerRadius = small; setColor(badgeBg) }
            }
            val headline = view.findViewById<TextView>(R.id.ad_headline)
            headline.text = ad.headline
            headline.setTextColor(text)
            view.headlineView = headline

            val body = view.findViewById<TextView>(R.id.ad_body)
            body.setTextColor(muted)
            if (ad.body.isNullOrBlank()) body.visibility = View.GONE else {
                body.visibility = View.VISIBLE
                body.text = ad.body
            }
            view.bodyView = body

            val advertiser = view.findViewById<TextView>(R.id.ad_advertiser)
            advertiser.setTextColor(muted)
            advertiser.text = ad.advertiser ?: ad.store ?: ""
            view.advertiserView = advertiser

            val icon = view.findViewById<ImageView>(R.id.ad_app_icon)
            val drawable = ad.icon?.drawable
            if (drawable == null) icon.visibility = View.GONE else {
                icon.visibility = View.VISIBLE
                icon.setImageDrawable(drawable)
            }
            view.iconView = icon

            val cta = view.findViewById<Button>(R.id.ad_call_to_action)
            if (ad.callToAction.isNullOrBlank()) cta.visibility = View.GONE else {
                cta.visibility = View.VISIBLE
                cta.text = ad.callToAction
            }
            cta.setTextColor(android.graphics.Color.WHITE)
            cta.background = GradientDrawable().apply { cornerRadius = radius; setColor(accent) }
            view.callToActionView = cta

            (view.findViewById<View>(R.id.ad_media) as? MediaView)?.let { media ->
                view.mediaView = media
            }
            view.setNativeAd(ad)
        },
    )
}
