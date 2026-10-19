package com.verba.subtitles.ads

import android.view.LayoutInflater
import android.view.View
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
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.verba.subtitles.R

/**
 * Native ad that fills free space between content (home feed, project grid, waiting screens).
 * Takes no space at all until an ad has actually loaded, so layouts never show empty boxes.
 */
@Composable
fun NativeAdCard(ads: AdsManager, modifier: Modifier = Modifier) {
    val ready by ads.ready.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(ready) {
        var disposed = false
        if (ready) {
            val loader = AdLoader.Builder(context, AdUnits.native)
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
            loader.loadAd(AdRequest.Builder().build())
        }
        onDispose {
            disposed = true
            nativeAd?.destroy()
            nativeAd = null
        }
    }

    val ad = nativeAd ?: return
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx -> LayoutInflater.from(ctx).inflate(R.layout.ad_native_card, null) as NativeAdView },
        update = { view -> bind(view, ad) },
    )
}

private fun bind(view: NativeAdView, ad: NativeAd) {
    val headline = view.findViewById<TextView>(R.id.ad_headline)
    val body = view.findViewById<TextView>(R.id.ad_body)
    val cta = view.findViewById<TextView>(R.id.ad_call_to_action)
    val icon = view.findViewById<ImageView>(R.id.ad_icon)
    val advertiser = view.findViewById<TextView>(R.id.ad_advertiser)
    val media = view.findViewById<MediaView>(R.id.ad_media)

    headline.text = ad.headline
    view.headlineView = headline

    body.text = ad.body.orEmpty()
    body.visibility = if (ad.body.isNullOrBlank()) View.GONE else View.VISIBLE
    view.bodyView = body

    cta.text = ad.callToAction.orEmpty()
    cta.visibility = if (ad.callToAction.isNullOrBlank()) View.GONE else View.VISIBLE
    view.callToActionView = cta

    val iconDrawable = ad.icon?.drawable
    icon.setImageDrawable(iconDrawable)
    icon.visibility = if (iconDrawable == null) View.GONE else View.VISIBLE
    view.iconView = icon

    advertiser.text = ad.advertiser.orEmpty()
    view.advertiserView = advertiser

    view.mediaView = media
    view.setNativeAd(ad)
}
