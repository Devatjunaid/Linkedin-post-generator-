package com.devatjunaid.examclock.ads

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.load
import com.devatjunaid.examclock.LocalAppContainer
import com.devatjunaid.examclock.R
import com.devatjunaid.examclock.domain.AdRetryPolicy
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import kotlinx.coroutines.delay

/**
 * Native ad that fills free space between content sections. It renders nothing until an ad has
 * loaded, so screens never show an empty box. Clearly labelled "Ad" and uses the SDK's
 * AdChoices overlay and MediaView as required by AdMob policy.
 */
@Composable
fun NativeAdCard(modifier: Modifier = Modifier) {
    val ads = LocalAppContainer.current.ads
    val ready by ads.ready.collectAsStateWithLifecycle()
    if (!ready) return

    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    val policy = remember { AdRetryPolicy(immediateAttempts = 2, backoffMillis = 15_000L) }
    var requestNumber by remember { mutableIntStateOf(0) }
    var nextDelay by remember { mutableLongStateOf(0L) }
    val disposed = remember { booleanArrayOf(false) }

    LaunchedEffect(requestNumber) {
        if (requestNumber > 0) delay(nextDelay)
        AdLoader.Builder(context, ads.nativeUnitId)
            .forNativeAd { ad ->
                if (disposed[0]) {
                    ad.destroy()
                } else {
                    policy.onSuccess()
                    nativeAd?.destroy()
                    nativeAd = ad
                }
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    if (!disposed[0] && nativeAd == null) {
                        nextDelay = policy.onFailure()
                        requestNumber++
                    }
                }
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

    DisposableEffect(Unit) {
        onDispose {
            disposed[0] = true
            nativeAd?.destroy()
        }
    }

    val ad = nativeAd ?: return
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx -> LayoutInflater.from(ctx).inflate(R.layout.native_ad_card, null) as NativeAdView },
        update = { view -> bind(view, ad) },
    )
}

private fun bind(view: NativeAdView, ad: NativeAd) {
    val headline = view.findViewById<TextView>(R.id.ad_headline)
    val body = view.findViewById<TextView>(R.id.ad_body)
    val advertiser = view.findViewById<TextView>(R.id.ad_advertiser)
    val icon = view.findViewById<ImageView>(R.id.ad_icon)
    val media = view.findViewById<MediaView>(R.id.ad_media)
    val cta = view.findViewById<Button>(R.id.ad_call_to_action)

    view.headlineView = headline
    view.bodyView = body
    view.advertiserView = advertiser
    view.iconView = icon
    view.mediaView = media
    view.callToActionView = cta

    headline.text = ad.headline
    body.text = ad.body
    body.visibility = if (ad.body.isNullOrBlank()) View.GONE else View.VISIBLE
    advertiser.text = ad.advertiser
    advertiser.visibility = if (ad.advertiser.isNullOrBlank()) View.GONE else View.VISIBLE
    cta.text = ad.callToAction
    cta.visibility = if (ad.callToAction.isNullOrBlank()) View.INVISIBLE else View.VISIBLE

    val adIcon = ad.icon
    when {
        adIcon?.drawable != null -> {
            icon.setImageDrawable(adIcon.drawable)
            icon.visibility = View.VISIBLE
        }
        adIcon?.uri != null -> {
            // Coil handles downsampling, caching and threading for remote icons.
            icon.load(adIcon.uri)
            icon.visibility = View.VISIBLE
        }
        else -> icon.visibility = View.GONE
    }

    ad.mediaContent?.let { media.setMediaContent(it) }
    view.setNativeAd(ad)
}
