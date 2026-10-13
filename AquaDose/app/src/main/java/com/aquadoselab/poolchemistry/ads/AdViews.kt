package com.aquadoselab.poolchemistry.ads

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.aquadoselab.poolchemistry.R
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

/** Anchored adaptive banner. Takes no space until the SDK is ready. */
@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val ready by AdsManager.ready.collectAsState()
    if (!ready) return
    val context = LocalContext.current
    val widthDp = LocalConfiguration.current.screenWidthDp
    val adSize = remember(widthDp) {
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(adSize.height.dp),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(adSize)
                    adUnitId = AdIds.BANNER
                    loadAd(AdRequest.Builder().build())
                }
            },
            onRelease = { it.destroy() }
        )
    }
}

enum class NativeStyle { SMALL, MEDIUM }

/**
 * Native ad card. Renders nothing until an ad has loaded, so it never leaves an empty gap.
 */
@Composable
fun NativeAdCard(modifier: Modifier = Modifier, style: NativeStyle = NativeStyle.SMALL) {
    val ready by AdsManager.ready.collectAsState()
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var disposed by remember { mutableStateOf(false) }

    LaunchedEffect(ready) {
        if (!ready || nativeAd != null) return@LaunchedEffect
        val loader = AdLoader.Builder(context, AdIds.NATIVE)
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
        loader.loadAd(AdRequest.Builder().build())
    }

    DisposableEffect(Unit) {
        onDispose {
            disposed = true
            nativeAd?.destroy()
            nativeAd = null
        }
    }

    val ad = nativeAd ?: return
    val layout = if (style == NativeStyle.MEDIUM) R.layout.native_ad_medium else R.layout.native_ad_small
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            LayoutInflater.from(ctx).inflate(layout, null, false) as NativeAdView
        },
        update = { view -> bindNativeAd(view, ad) }
    )
}

private fun bindNativeAd(view: NativeAdView, ad: NativeAd) {
    val headline = view.findViewById<TextView>(R.id.ad_headline)
    val body = view.findViewById<TextView>(R.id.ad_body)
    val cta = view.findViewById<Button>(R.id.ad_call_to_action)
    val icon = view.findViewById<ImageView>(R.id.ad_icon)
    val advertiser = view.findViewById<TextView>(R.id.ad_advertiser)
    val media = view.findViewById<View>(R.id.ad_media) as? MediaView

    headline.text = ad.headline
    view.headlineView = headline

    if (ad.body.isNullOrBlank()) body.visibility = View.GONE
    else { body.text = ad.body; body.visibility = View.VISIBLE }
    view.bodyView = body

    if (ad.callToAction.isNullOrBlank()) cta.visibility = View.GONE
    else { cta.text = ad.callToAction; cta.visibility = View.VISIBLE }
    view.callToActionView = cta

    val iconDrawable = ad.icon?.drawable
    if (iconDrawable == null) icon.visibility = View.GONE
    else { icon.setImageDrawable(iconDrawable); icon.visibility = View.VISIBLE }
    view.iconView = icon

    if (ad.advertiser.isNullOrBlank()) advertiser.visibility = View.GONE
    else { advertiser.text = ad.advertiser; advertiser.visibility = View.VISIBLE }
    view.advertiserView = advertiser

    if (media != null) view.mediaView = media

    view.setNativeAd(ad)
}
