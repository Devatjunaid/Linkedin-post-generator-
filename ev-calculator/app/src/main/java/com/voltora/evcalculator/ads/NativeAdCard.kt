package com.voltora.evcalculator.ads

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.voltora.evcalculator.R

/**
 * A native ad styled like the app's own cards (mist background, lime "Ad" badge, black CTA),
 * so it sits naturally in free space. Nothing is shown until an ad has loaded.
 *
 * @param showMedia true for the larger variant with a MediaView (exit dialog, trip results).
 */
@Composable
fun NativeAdCard(modifier: Modifier = Modifier, showMedia: Boolean = false) {
    val ready by AdsManager.ready.collectAsState()
    if (!ready) return

    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(showMedia) {
        var disposed = false
        val loader = AdLoader.Builder(context, AdIds.NATIVE)
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

    AnimatedVisibility(visible = nativeAd != null, enter = expandVertically() + fadeIn(), modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                LayoutInflater.from(ctx).inflate(R.layout.ad_native_card, FrameLayout(ctx), false) as NativeAdView
            },
            update = { view -> nativeAd?.let { bind(view, it, showMedia) } },
        )
    }
}

private fun bind(view: NativeAdView, ad: NativeAd, showMedia: Boolean) {
    val headline = view.findViewById<TextView>(R.id.ad_headline)
    val body = view.findViewById<TextView>(R.id.ad_body)
    val icon = view.findViewById<ImageView>(R.id.ad_icon)
    val cta = view.findViewById<Button>(R.id.ad_cta)
    val media = view.findViewById<MediaView>(R.id.ad_media)

    headline.text = ad.headline
    view.headlineView = headline

    body.text = ad.body
    body.visibility = if (ad.body.isNullOrBlank()) View.GONE else View.VISIBLE
    view.bodyView = body

    val iconDrawable = ad.icon?.drawable
    icon.setImageDrawable(iconDrawable)
    icon.visibility = if (iconDrawable == null) View.GONE else View.VISIBLE
    view.iconView = icon

    cta.text = ad.callToAction ?: "Open"
    cta.visibility = if (ad.callToAction.isNullOrBlank()) View.GONE else View.VISIBLE
    view.callToActionView = cta

    if (showMedia && ad.mediaContent != null) {
        media.visibility = View.VISIBLE
        media.mediaContent = ad.mediaContent
        view.mediaView = media
    } else {
        media.visibility = View.GONE
    }

    view.setNativeAd(ad)
}
