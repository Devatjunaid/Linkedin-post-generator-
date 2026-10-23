package com.walkies.dogwalk.ads

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.walkies.dogwalk.R

/**
 * Native ad for the free space between sections. A labelled placeholder holds the space while
 * the ad loads; if no ad can be filled the slot disappears.
 */
@Composable
fun NativeAdCard(adsManager: AdsManager, placement: String, modifier: Modifier = Modifier) {
    val ready by adsManager.adsReady.collectAsStateWithLifecycle()
    if (!ready) return
    val cache = adsManager.nativeAds
    LaunchedEffect(placement) { cache.request(placement) }
    val ad = cache.adFor(placement)
    when {
        ad != null -> AndroidView(
            modifier = modifier.fillMaxWidth(),
            factory = { context ->
                LayoutInflater.from(context).inflate(R.layout.native_ad_card, null) as NativeAdView
            },
            update = { view -> view.bind(ad) },
        )
        !cache.hasFailed(placement) -> Box(
            modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp))
                .padding(16.dp),
            contentAlignment = Alignment.TopStart,
        ) {
            Text(
                stringResource(R.string.ad_loading),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun NativeAdView.bind(ad: NativeAd) {
    if (getTag(R.id.ad_headline) === ad) return
    setTag(R.id.ad_headline, ad)

    val headline = findViewById<TextView>(R.id.ad_headline)
    val body = findViewById<TextView>(R.id.ad_body)
    val advertiser = findViewById<TextView>(R.id.ad_advertiser)
    val icon = findViewById<ImageView>(R.id.ad_icon)
    val stars = findViewById<RatingBar>(R.id.ad_stars)
    val cta = findViewById<Button>(R.id.ad_call_to_action)
    val media = findViewById<MediaView>(R.id.ad_media)

    headlineView = headline
    bodyView = body
    advertiserView = advertiser
    iconView = icon
    starRatingView = stars
    callToActionView = cta
    mediaView = media

    headline.text = ad.headline
    ad.mediaContent?.let { media.setMediaContent(it) }
    media.visibility = if (ad.mediaContent != null) View.VISIBLE else View.GONE

    body.text = ad.body
    body.visibility = if (ad.body.isNullOrBlank()) View.GONE else View.VISIBLE

    advertiser.text = ad.advertiser
    advertiser.visibility = if (ad.advertiser.isNullOrBlank()) View.INVISIBLE else View.VISIBLE

    val adIcon = ad.icon
    if (adIcon == null) {
        icon.visibility = View.GONE
    } else {
        icon.setImageDrawable(adIcon.drawable)
        icon.visibility = View.VISIBLE
    }

    val rating = ad.starRating
    if (rating == null || rating <= 0.0) {
        stars.visibility = View.GONE
    } else {
        stars.rating = rating.toFloat()
        stars.visibility = View.VISIBLE
    }

    cta.text = ad.callToAction
    cta.visibility = if (ad.callToAction.isNullOrBlank()) View.INVISIBLE else View.VISIBLE

    setNativeAd(ad)
}
