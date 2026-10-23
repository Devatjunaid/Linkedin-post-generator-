package com.driveready.testprep.ads

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.load
import com.driveready.testprep.R
import com.driveready.testprep.ui.theme.Brand
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

enum class NativeAdStyle { COMPACT, MEDIA }

/**
 * Native ad placed in a free slot between content blocks. Renders nothing until an ad is
 * available from [NativeAdManager], so it never leaves an empty gap.
 */
@Composable
fun NativeAdCard(modifier: Modifier = Modifier, style: NativeAdStyle = NativeAdStyle.COMPACT) {
    val ready by AdsManager.ready.collectAsStateWithLifecycle()
    if (!ready) return
    val context = LocalContext.current
    val available by NativeAdManager.available.collectAsStateWithLifecycle()
    var ad by remember { mutableStateOf<NativeAd?>(null) }

    LaunchedEffect(available) {
        if (ad == null && available > 0) ad = NativeAdManager.take(context)
    }
    DisposableEffect(Unit) {
        onDispose { ad?.destroy() }
    }

    val current = ad ?: return
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx -> buildNativeAdView(ctx, style) },
        update = { view -> bind(view, current) }
    )
}

private fun Context.dp(value: Int): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()

private fun roundRect(color: Int, radius: Float, strokeColor: Int? = null, strokeWidth: Int = 0) =
    GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
        if (strokeColor != null) setStroke(strokeWidth, strokeColor)
    }

private fun buildNativeAdView(context: Context, style: NativeAdStyle): NativeAdView {
    val ink = Brand.Ink.toArgb()
    val muted = Brand.Muted.toArgb()
    val adView = NativeAdView(context)
    val root = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(context.dp(14), context.dp(12), context.dp(14), context.dp(14))
        background = roundRect(Color.WHITE, context.dp(18).toFloat(), Brand.Line.toArgb(), context.dp(1))
    }

    val header = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    val icon = ImageView(context).apply {
        layoutParams = LinearLayout.LayoutParams(context.dp(44), context.dp(44)).apply { marginEnd = context.dp(12) }
        scaleType = ImageView.ScaleType.CENTER_CROP
        background = roundRect(Brand.Surface.toArgb(), context.dp(10).toFloat())
        clipToOutline = true
    }
    val titles = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    }
    val headline = TextView(context).apply {
        setTextColor(ink)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        typeface = Typeface.DEFAULT_BOLD
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
    }
    val subRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    val badge = TextView(context).apply {
        text = context.getString(R.string.ad_label)
        setTextColor(Brand.Ink.toArgb())
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
        typeface = Typeface.DEFAULT_BOLD
        setPadding(context.dp(6), context.dp(1), context.dp(6), context.dp(1))
        background = roundRect(Brand.Amber.toArgb(), context.dp(4).toFloat())
    }
    val advertiser = TextView(context).apply {
        setTextColor(muted)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        setPadding(context.dp(6), 0, 0, 0)
    }
    subRow.addView(badge)
    subRow.addView(advertiser)
    titles.addView(headline)
    titles.addView(subRow)
    header.addView(icon)
    header.addView(titles)
    root.addView(header)

    val body = TextView(context).apply {
        setTextColor(muted)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
        setPadding(0, context.dp(8), 0, 0)
    }
    root.addView(body)

    if (style == NativeAdStyle.MEDIA) {
        val media = MediaView(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, context.dp(150)).apply {
                topMargin = context.dp(10)
            }
        }
        root.addView(media)
        adView.mediaView = media
    }

    val cta = Button(context).apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, context.dp(46)).apply {
            topMargin = context.dp(10)
        }
        isAllCaps = false
        setTextColor(Color.WHITE)
        typeface = Typeface.DEFAULT_BOLD
        background = roundRect(ink, context.dp(12).toFloat())
        stateListAnimator = null
    }
    root.addView(cta)

    adView.addView(root)
    adView.headlineView = headline
    adView.bodyView = body
    adView.iconView = icon
    adView.advertiserView = advertiser
    adView.callToActionView = cta
    return adView
}

private fun bind(adView: NativeAdView, ad: NativeAd) {
    (adView.headlineView as TextView).text = ad.headline

    (adView.bodyView as TextView).apply {
        text = ad.body
        visibility = if (ad.body.isNullOrBlank()) View.GONE else View.VISIBLE
    }
    (adView.advertiserView as TextView).text = ad.advertiser ?: ad.store ?: ""
    (adView.callToActionView as Button).apply {
        text = ad.callToAction
        visibility = if (ad.callToAction.isNullOrBlank()) View.GONE else View.VISIBLE
    }

    val iconView = adView.iconView as ImageView
    val icon = ad.icon
    when {
        icon?.drawable != null -> {
            iconView.setImageDrawable(icon.drawable)
            iconView.visibility = View.VISIBLE
        }
        icon?.uri != null -> {
            // Never decode network images by hand — Coil handles threading, sampling and caching.
            iconView.load(icon.uri)
            iconView.visibility = View.VISIBLE
        }
        else -> iconView.visibility = View.GONE
    }

    ad.mediaContent?.let { content -> adView.mediaView?.setMediaContent(content) }
    adView.setNativeAd(ad)
}
