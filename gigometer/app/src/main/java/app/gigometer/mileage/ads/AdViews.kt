package app.gigometer.mileage.ads

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.gigometer.mileage.BuildConfig
import app.gigometer.mileage.R
import app.gigometer.mileage.ui.theme.LocalPalette
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

/** Anchored adaptive banner shown at the bottom of every main screen. */
@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val ready by Ads.ready.collectAsState()
    if (!ready) return
    val widthDp = LocalConfiguration.current.screenWidthDp
    val context = LocalContext.current
    val adSize = remember(widthDp) { AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp) }
    val adView = remember(adSize) {
        AdView(context).apply {
            setAdSize(adSize)
            adUnitId = BuildConfig.BANNER_AD_UNIT
            loadAd(AdRequest.Builder().build())
        }
    }
    DisposableEffect(adView) { onDispose { adView.destroy() } }
    Box(modifier.fillMaxWidth().height(adSize.height.dp)) {
        AndroidView(factory = { adView }, modifier = Modifier.fillMaxWidth().height(adSize.height.dp))
    }
}

/**
 * Native ad styled like the app's own cards (clearly labelled "Ad").
 * Takes no space at all until an ad has actually loaded.
 */
@Composable
fun NativeAdCard(modifier: Modifier = Modifier) {
    val ready by Ads.ready.collectAsState()
    if (!ready) return
    val context = LocalContext.current
    var ad by remember { mutableStateOf<NativeAd?>(null) }
    LaunchedEffect(Unit) {
        AdLoader.Builder(context, BuildConfig.NATIVE_AD_UNIT)
            .forNativeAd { loaded -> ad?.destroy(); ad = loaded }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) { ad = null }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT).build())
            .build()
            .loadAd(AdRequest.Builder().build())
    }
    DisposableEffect(Unit) { onDispose { ad?.destroy() } }
    val loaded = ad ?: return
    val palette = LocalPalette.current
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clip(RoundedCornerShape(18.dp))
            .background(palette.card),
        factory = { ctx -> LayoutInflater.from(ctx).inflate(R.layout.native_ad_card, null) as NativeAdView },
        update = { view -> bind(view, loaded, palette.dark) },
    )
}

private fun bind(view: NativeAdView, ad: NativeAd, dark: Boolean) {
    val headline = view.findViewById<TextView>(R.id.ad_headline)
    val body = view.findViewById<TextView>(R.id.ad_body)
    val cta = view.findViewById<Button>(R.id.ad_cta)
    val icon = view.findViewById<ImageView>(R.id.ad_icon)
    val advertiser = view.findViewById<TextView>(R.id.ad_advertiser)
    val media = view.findViewById<MediaView>(R.id.ad_media)

    val textColor = if (dark) 0xFFEFF3F0.toInt() else 0xFF14211B.toInt()
    val mutedColor = if (dark) 0xFFB9C6BE.toInt() else 0xFF55615A.toInt()
    headline.setTextColor(textColor)
    body.setTextColor(mutedColor)
    advertiser.setTextColor(mutedColor)

    view.headlineView = headline
    view.bodyView = body
    view.callToActionView = cta
    view.iconView = icon
    view.advertiserView = advertiser
    view.mediaView = media

    headline.text = ad.headline
    body.text = ad.body
    body.visibility = if (ad.body.isNullOrBlank()) View.GONE else View.VISIBLE
    cta.text = ad.callToAction ?: "Open"
    cta.visibility = if (ad.callToAction == null) View.GONE else View.VISIBLE
    advertiser.text = ad.advertiser
    advertiser.visibility = if (ad.advertiser.isNullOrBlank()) View.GONE else View.VISIBLE
    val iconDrawable = ad.icon?.drawable
    icon.setImageDrawable(iconDrawable)
    icon.visibility = if (iconDrawable == null) View.GONE else View.VISIBLE
    val content = ad.mediaContent
    if (content != null) {
        media.mediaContent = content
        media.visibility = View.VISIBLE
    } else {
        media.visibility = View.GONE
    }
    view.setNativeAd(ad)
}
