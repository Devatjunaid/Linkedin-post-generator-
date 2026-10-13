package com.snaptensio.bplog.ads

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.snaptensio.bplog.BuildConfig
import com.snaptensio.bplog.R
import com.snaptensio.bplog.appContainer

/** Anchored adaptive banner. Reserves its exact height so content never jumps under the user's finger. */
@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val ads = context.appContainer.ads
    if (!ads.canRequestAds) return
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val widthDp = maxWidth.value.toInt().coerceAtLeast(320)
        val size = remember(widthDp) { AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp) }
        val adView = remember(widthDp) {
            AdView(context).apply {
                setAdSize(size)
                adUnitId = BuildConfig.AD_BANNER_ID
                loadAd(ads.request())
            }
        }
        DisposableEffect(adView) { onDispose { adView.destroy() } }
        AndroidView(factory = { adView }, modifier = Modifier.fillMaxWidth().height(size.height.dp))
    }
}

/**
 * Small native ad card, labelled "Ad", styled as a distinct card. Renders nothing until an ad has
 * loaded, so empty placeholders never sit in the layout.
 */
@Composable
fun NativeAdCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val ads = context.appContainer.ads
    if (!ads.canRequestAds) return
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    DisposableEffect(Unit) {
        var disposed = false
        val loader = AdLoader.Builder(context, BuildConfig.AD_NATIVE_ID)
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
        loader.loadAd(ads.request())
        onDispose {
            disposed = true
            nativeAd?.destroy()
            nativeAd = null
        }
    }
    val ad = nativeAd ?: return
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx -> LayoutInflater.from(ctx).inflate(R.layout.native_ad_small, null) as NativeAdView },
        update = { view -> bindNativeAd(view, ad) },
    )
}

private fun bindNativeAd(view: NativeAdView, ad: NativeAd) {
    val headline = view.findViewById<TextView>(R.id.ad_headline)
    val body = view.findViewById<TextView>(R.id.ad_body)
    val cta = view.findViewById<Button>(R.id.ad_call_to_action)
    val icon = view.findViewById<ImageView>(R.id.ad_icon)
    val advertiser = view.findViewById<TextView>(R.id.ad_advertiser)

    headline.text = ad.headline
    view.headlineView = headline

    body.text = ad.body
    body.visibility = if (ad.body.isNullOrBlank()) View.GONE else View.VISIBLE
    view.bodyView = body

    cta.text = ad.callToAction
    cta.visibility = if (ad.callToAction.isNullOrBlank()) View.GONE else View.VISIBLE
    view.callToActionView = cta

    val img = ad.icon?.drawable
    icon.setImageDrawable(img)
    icon.visibility = if (img == null) View.GONE else View.VISIBLE
    view.iconView = icon

    advertiser.text = ad.advertiser
    advertiser.visibility = if (ad.advertiser.isNullOrBlank()) View.GONE else View.VISIBLE
    view.advertiserView = advertiser

    view.setNativeAd(ad)
}
