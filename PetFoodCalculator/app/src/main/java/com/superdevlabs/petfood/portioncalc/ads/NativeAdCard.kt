package com.superdevlabs.petfood.portioncalc.ads

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
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.superdevlabs.petfood.portioncalc.R
import com.superdevlabs.petfood.portioncalc.ui.LocalAppContainer

/**
 * A clearly labelled native ad placed in a free gap between content sections.
 * It occupies no space until an ad has loaded, and destroys its ad when it leaves composition.
 */
@Composable
fun NativeAdCard(modifier: Modifier = Modifier, showMedia: Boolean = true) {
    val ads = LocalAppContainer.current.ads
    val ready by ads.ready.collectAsStateWithLifecycle()
    if (!ready) return

    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(Unit) {
        var disposed = false
        val loader = AdLoader.Builder(context, AdUnits.native)
            .forNativeAd { ad ->
                if (disposed) ad.destroy() else {
                    nativeAd?.destroy()
                    nativeAd = ad
                }
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    // Leave the gap empty; the next screen visit asks again.
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .build(),
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
        update = { view -> bind(view, ad, showMedia) },
    )
}

private fun bind(view: NativeAdView, ad: NativeAd, showMedia: Boolean) {
    val headline = view.findViewById<TextView>(R.id.ad_headline)
    val body = view.findViewById<TextView>(R.id.ad_body)
    val advertiser = view.findViewById<TextView>(R.id.ad_advertiser)
    val icon = view.findViewById<ImageView>(R.id.ad_icon)
    val media = view.findViewById<MediaView>(R.id.ad_media)
    val cta = view.findViewById<Button>(R.id.ad_call_to_action)

    view.headlineView = headline
    headline.text = ad.headline

    view.bodyView = body
    body.text = ad.body
    body.visibility = if (ad.body.isNullOrBlank()) View.GONE else View.VISIBLE

    view.advertiserView = advertiser
    advertiser.text = ad.advertiser ?: ad.store.orEmpty()
    advertiser.visibility = if (advertiser.text.isNullOrBlank()) View.GONE else View.VISIBLE

    view.iconView = icon
    val iconDrawable = ad.icon?.drawable
    icon.setImageDrawable(iconDrawable)
    icon.visibility = if (iconDrawable == null) View.GONE else View.VISIBLE

    val mediaContent = ad.mediaContent
    if (showMedia && mediaContent != null) {
        view.mediaView = media
        media.visibility = View.VISIBLE
        media.setMediaContent(mediaContent)
    } else {
        media.visibility = View.GONE
    }

    view.callToActionView = cta
    cta.text = ad.callToAction
    cta.visibility = if (ad.callToAction.isNullOrBlank()) View.GONE else View.VISIBLE

    view.setNativeAd(ad)
}
