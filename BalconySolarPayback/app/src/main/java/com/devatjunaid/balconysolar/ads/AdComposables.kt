package com.devatjunaid.balconysolar.ads

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devatjunaid.balconysolar.R
import com.devatjunaid.balconysolar.ui.theme.Palette
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

/** Anchored adaptive banner for the bottom of the screen; collapses to nothing until ads are allowed. */
@Composable
fun BannerAdSlot(modifier: Modifier = Modifier) {
    val ready by AdsManager.ready.collectAsStateWithLifecycle()
    if (!ready) return
    BoxWithConstraints(modifier.fillMaxWidth().background(Palette.NightDeep)) {
        val context = LocalContext.current
        val widthDp = maxWidth.value.toInt()
        val adSize = remember(widthDp) { AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp) }
        val scope = rememberCoroutineScope()
        val adView = remember(adSize) {
            AdView(context).apply {
                setAdSize(adSize)
                adUnitId = AdUnits.banner
            }
        }
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(adView, lifecycleOwner) {
            val request = { adView.loadAd(AdRequest.Builder().build()) }
            val policy = BannerRetryPolicy(scope, request)
            adView.adListener = object : AdListener() {
                override fun onAdLoaded() = policy.onLoaded()
                override fun onAdFailedToLoad(error: LoadAdError) = policy.onFailed()
            }
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> adView.resume()
                    Lifecycle.Event.ON_PAUSE -> adView.pause()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            request()
            onDispose {
                policy.cancel()
                lifecycleOwner.lifecycle.removeObserver(observer)
                adView.destroy()
            }
        }
        AndroidView(factory = { adView }, modifier = Modifier.fillMaxWidth().height(adSize.height.dp))
    }
}

/** A native ad styled as a card, shown only once an ad has loaded. */
@Composable
fun NativeAdCard(modifier: Modifier = Modifier) {
    val ready by AdsManager.ready.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var ad by remember { mutableStateOf<NativeAd?>(null) }
    val holder = remember { object { var disposed = false } }
    LaunchedEffect(ready) {
        if (!ready || ad != null) return@LaunchedEffect
        AdLoader.Builder(context, AdUnits.native)
            .forNativeAd { loaded ->
                if (holder.disposed) loaded.destroy() else ad = loaded
            }
            .withAdListener(object : AdListener() {})
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_LANDSCAPE)
                    .build(),
            )
            .build()
            .loadAd(AdRequest.Builder().build())
    }
    DisposableEffect(Unit) {
        onDispose {
            holder.disposed = true
            ad?.destroy()
        }
    }
    val native = ad ?: return
    Column(modifier.fillMaxWidth()) {
        AndroidView(
            factory = { ctx -> LayoutInflater.from(ctx).inflate(R.layout.native_ad_card, null) as NativeAdView },
            update = { view -> bind(view, native) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun bind(view: NativeAdView, ad: NativeAd) {
    val headline = view.findViewById<TextView>(R.id.ad_headline)!!
    val body = view.findViewById<TextView>(R.id.ad_body)!!
    val cta = view.findViewById<Button>(R.id.ad_call_to_action)!!
    val icon = view.findViewById<ImageView>(R.id.ad_app_icon)!!
    val advertiser = view.findViewById<TextView>(R.id.ad_advertiser)!!
    val media = view.findViewById<MediaView>(R.id.ad_media)!!

    headline.text = ad.headline
    view.headlineView = headline

    body.text = ad.body
    body.visibility = if (ad.body.isNullOrBlank()) View.GONE else View.VISIBLE
    view.bodyView = body

    cta.text = ad.callToAction
    cta.visibility = if (ad.callToAction.isNullOrBlank()) View.GONE else View.VISIBLE
    view.callToActionView = cta

    val drawable = ad.icon?.drawable
    icon.setImageDrawable(drawable)
    icon.visibility = if (drawable == null) View.GONE else View.VISIBLE
    view.iconView = icon

    advertiser.text = ad.advertiser
    advertiser.visibility = if (ad.advertiser.isNullOrBlank()) View.GONE else View.VISIBLE
    view.advertiserView = advertiser

    view.mediaView = media
    media.visibility = if (ad.mediaContent == null) View.GONE else View.VISIBLE

    view.setNativeAd(ad)
}
