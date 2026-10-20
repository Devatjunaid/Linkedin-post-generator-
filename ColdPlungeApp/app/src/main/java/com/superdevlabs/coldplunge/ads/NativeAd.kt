package com.superdevlabs.coldplunge.ads

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.superdevlabs.coldplunge.BuildConfig
import com.superdevlabs.coldplunge.R
import com.superdevlabs.coldplunge.graph
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

/**
 * Loads one native ad for the lifetime of the calling screen and destroys it when the screen leaves.
 * Returns null until an ad has loaded, so callers render nothing (no empty gaps) on failure.
 */
@Composable
fun rememberNativeAd(): NativeAd? {
    val context = LocalContext.current
    val adsReady by context.graph.consent.adsReady.collectAsStateWithLifecycle()
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(adsReady) {
        var disposed = false
        if (adsReady) {
            AdLoader.Builder(context, BuildConfig.ADMOB_NATIVE_ID)
                .forNativeAd { ad ->
                    if (disposed) {
                        ad.destroy()
                    } else {
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
                        .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_LANDSCAPE)
                        .build(),
                )
                .build()
                .loadAd(AdRequest.Builder().build())
        }
        onDispose {
            disposed = true
            nativeAd?.destroy()
            nativeAd = null
        }
    }
    return nativeAd
}

/** Clearly labelled ("Ad") native ad card styled to sit between the app's own cards. */
@Composable
fun NativeAdCard(ad: NativeAd, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            (LayoutInflater.from(ctx).inflate(R.layout.native_ad_card, null) as NativeAdView).apply {
                headlineView = findViewById(R.id.ad_headline)
                bodyView = findViewById(R.id.ad_body)
                callToActionView = findViewById(R.id.ad_call_to_action)
                iconView = findViewById(R.id.ad_icon)
                advertiserView = findViewById(R.id.ad_advertiser)
                mediaView = findViewById<MediaView>(R.id.ad_media)
            }
        },
        update = { view -> bind(view, ad) },
    )
}

private fun bind(view: NativeAdView, ad: NativeAd) {
    (view.headlineView as TextView).text = ad.headline

    (view.bodyView as TextView).apply {
        text = ad.body.orEmpty()
        visibility = if (ad.body.isNullOrBlank()) View.GONE else View.VISIBLE
    }
    (view.callToActionView as Button).apply {
        text = ad.callToAction.orEmpty()
        visibility = if (ad.callToAction.isNullOrBlank()) View.GONE else View.VISIBLE
    }
    (view.iconView as ImageView).apply {
        val drawable = ad.icon?.drawable
        setImageDrawable(drawable)
        visibility = if (drawable == null) View.GONE else View.VISIBLE
    }
    (view.advertiserView as TextView).apply {
        text = ad.advertiser.orEmpty()
        visibility = if (ad.advertiser.isNullOrBlank()) View.GONE else View.VISIBLE
    }
    view.mediaView?.apply {
        mediaContent = ad.mediaContent
        visibility = if (ad.mediaContent == null) View.GONE else View.VISIBLE
    }
    view.setNativeAd(ad)
}
