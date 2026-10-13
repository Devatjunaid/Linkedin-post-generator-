package com.odomate.app.ads

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.odomate.app.BuildConfig
import com.odomate.app.R
import com.odomate.app.ui.LocalAppContainer
import com.odomate.app.ui.theme.AppTheme

/** Anchored adaptive banner sized to the screen width. Renders nothing until ads are allowed. */
@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val ads = LocalAppContainer.current.ads
    val ready by ads.ready.collectAsStateWithLifecycle()
    if (!ready) return
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val context = LocalContext.current
        val widthDp = maxWidth.value.toInt().coerceAtLeast(320)
        val adSize = remember(widthDp) { AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp) }
        val adView = remember(widthDp) {
            AdView(context).apply {
                setAdSize(adSize)
                adUnitId = BuildConfig.BANNER_AD_ID
                loadAd(AdRequest.Builder().build())
            }
        }
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(adView, lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> adView.pause()
                    Lifecycle.Event.ON_RESUME -> adView.resume()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                adView.destroy()
            }
        }
        AndroidView(
            factory = { adView },
            modifier = Modifier.fillMaxWidth().height(adSize.height.dp),
        )
    }
}

enum class NativeAdSize { SMALL, MEDIUM }

/**
 * Native ad drawn in the app's own card style. Collapses to nothing until an ad has loaded,
 * so it never leaves an empty gap.
 */
@Composable
fun NativeAdCard(modifier: Modifier = Modifier, size: NativeAdSize = NativeAdSize.SMALL) {
    val ads = LocalAppContainer.current.ads
    val ready by ads.ready.collectAsStateWithLifecycle()
    if (!ready) return
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(Unit) {
        var disposed = false
        val loader = AdLoader.Builder(context, BuildConfig.NATIVE_AD_ID)
            .forNativeAd { ad ->
                if (disposed) ad.destroy() else {
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

    val ad = nativeAd ?: return
    val palette = AppTheme.colors
    val textColor = palette.text.toArgb()
    val mutedColor = palette.muted.toArgb()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.outline),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                val layout = if (size == NativeAdSize.MEDIUM) R.layout.ad_native_medium else R.layout.ad_native_small
                LayoutInflater.from(ctx).inflate(layout, null, false) as NativeAdView
            },
            update = { view -> bindNativeAd(view, ad, textColor, mutedColor) },
        )
    }
}

private fun bindNativeAd(view: NativeAdView, ad: NativeAd, textColor: Int, mutedColor: Int) {
    val headline = view.findViewById<TextView>(R.id.ad_headline)
    headline.text = ad.headline
    headline.setTextColor(textColor)
    view.headlineView = headline

    view.findViewById<TextView>(R.id.ad_body)?.let { body ->
        body.text = ad.body
        body.setTextColor(mutedColor)
        body.visibility = if (ad.body.isNullOrBlank()) View.GONE else View.VISIBLE
        view.bodyView = body
    }

    view.findViewById<Button>(R.id.ad_call_to_action)?.let { cta ->
        cta.text = ad.callToAction ?: "Open"
        cta.visibility = if (ad.callToAction.isNullOrBlank()) View.GONE else View.VISIBLE
        view.callToActionView = cta
    }

    view.findViewById<ImageView>(R.id.ad_icon)?.let { icon ->
        val drawable = ad.icon?.drawable
        icon.setImageDrawable(drawable)
        icon.visibility = if (drawable == null) View.GONE else View.VISIBLE
        view.iconView = icon
    }

    view.findViewById<TextView>(R.id.ad_advertiser)?.let { adv ->
        adv.text = ad.advertiser
        adv.setTextColor(mutedColor)
        adv.visibility = if (ad.advertiser.isNullOrBlank()) View.GONE else View.VISIBLE
        view.advertiserView = adv
    }

    view.findViewById<RatingBar>(R.id.ad_stars)?.let { stars ->
        val rating = ad.starRating
        if (rating == null || rating <= 0.0) {
            stars.visibility = View.GONE
        } else {
            stars.rating = rating.toFloat()
            stars.visibility = View.VISIBLE
        }
        view.starRatingView = stars
    }

    view.findViewById<MediaView>(R.id.ad_media)?.let { media ->
        media.setImageScaleType(ImageView.ScaleType.CENTER_CROP)
        view.mediaView = media
    }

    view.setNativeAd(ad)
}
