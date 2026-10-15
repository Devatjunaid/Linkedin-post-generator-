package com.glowdash.warningidentifier.ads

import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.glowdash.warningidentifier.R
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

enum class NativeStyle { MEDIUM, SMALL }

/**
 * A native ad styled like the app's own cards. It occupies no space until an
 * ad is available, so screens never show an empty placeholder.
 */
@Composable
fun NativeAdCard(
    placement: String,
    modifier: Modifier = Modifier,
    style: NativeStyle = NativeStyle.MEDIUM,
) {
    val ready by AdsManager.ready.collectAsState()
    val context = LocalContext.current
    var ad by remember(placement) { mutableStateOf(AdsManager.cachedNative(placement)) }

    LaunchedEffect(ready, placement) {
        if (ready && ad == null) AdsManager.loadNative(context, placement) { ad = it }
    }

    AnimatedVisibility(
        visible = ad != null,
        enter = fadeIn() + expandVertically(),
        modifier = modifier,
    ) {
        ad?.let { current ->
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { ctx ->
                    val layout = if (style == NativeStyle.MEDIUM) R.layout.native_ad_medium else R.layout.native_ad_small
                    LayoutInflater.from(ctx).inflate(layout, null) as NativeAdView
                },
                update = { view -> view.bind(current) },
            )
        }
    }
}

private fun NativeAdView.bind(ad: NativeAd) {
    val headline = findViewById<TextView>(R.id.ad_headline)
    headline.text = ad.headline
    headlineView = headline

    val body = findViewById<TextView>(R.id.ad_body)
    if (ad.body.isNullOrBlank()) {
        body.visibility = View.GONE
    } else {
        body.visibility = View.VISIBLE
        body.text = ad.body
    }
    bodyView = body

    val cta = findViewById<TextView>(R.id.ad_call_to_action)
    if (ad.callToAction.isNullOrBlank()) {
        cta.visibility = View.GONE
    } else {
        cta.visibility = View.VISIBLE
        cta.text = ad.callToAction
    }
    callToActionView = cta

    val icon = findViewById<ImageView>(R.id.ad_icon)
    val drawable = ad.icon?.drawable
    if (drawable == null) {
        icon.visibility = View.GONE
    } else {
        icon.visibility = View.VISIBLE
        icon.setImageDrawable(drawable)
    }
    iconView = icon

    findViewById<TextView>(R.id.ad_advertiser)?.let { advertiser ->
        advertiser.text = ad.advertiser.orEmpty()
        advertiserView = advertiser
    }

    val media: MediaView? = findViewById(R.id.ad_media)
    if (media != null) {
        val content = ad.mediaContent
        if (content == null) {
            media.visibility = View.GONE
        } else {
            media.visibility = View.VISIBLE
            media.setMediaContent(content)
        }
        mediaView = media
    }

    setNativeAd(ad)
}
