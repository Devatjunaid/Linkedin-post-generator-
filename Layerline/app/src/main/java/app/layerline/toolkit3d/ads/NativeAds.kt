package app.layerline.toolkit3d.ads

import android.app.Application
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.layerline.toolkit3d.R
import app.layerline.toolkit3d.ui.components.SectionLabel
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * One cached native ad per placement ("bench", "stock", …), so moving between screens does not
 * fire a new request each time. Ads are refreshed after ~50 minutes (they expire after an hour)
 * and a failed placement waits a minute before trying again.
 */
class NativeAdStore(private val app: Application, private val ready: StateFlow<Boolean>) {

    private class Slot {
        val ad = MutableStateFlow<NativeAd?>(null)
        var loadedAt = 0L
        var failedAt = 0L
        var loading = false
    }

    private val slots = mutableMapOf<String, Slot>()

    fun observe(placement: String): StateFlow<NativeAd?> {
        val slot = slots.getOrPut(placement) { Slot() }
        refreshIfNeeded(slot)
        return slot.ad
    }

    private fun refreshIfNeeded(slot: Slot) {
        if (!ready.value || slot.loading) return
        val now = SystemClock.elapsedRealtime()
        if (slot.ad.value != null && now - slot.loadedAt < 50 * 60_000L) return
        if (slot.failedAt != 0L && now - slot.failedAt < 60_000L) return

        slot.loading = true
        AdLoader.Builder(app, AdUnits.native)
            .forNativeAd { ad ->
                val old = slot.ad.value
                slot.ad.value = ad
                slot.loadedAt = SystemClock.elapsedRealtime()
                slot.loading = false
                old?.destroy()
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    slot.loading = false
                    slot.failedAt = SystemClock.elapsedRealtime()
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
}

/**
 * A native ad that fills a free gap between content blocks. If no ad is available the slot
 * collapses completely, so screens never show an empty box.
 */
@Composable
fun NativeAdSlot(ads: AdsManager, placement: String, modifier: Modifier = Modifier) {
    val ready by ads.ready.collectAsStateWithLifecycle()
    val flow = remember(placement, ready) { ads.natives.observe(placement) }
    val ad by flow.collectAsStateWithLifecycle()
    val current = ad ?: return

    Column(modifier.fillMaxWidth()) {
        SectionLabel("; SPONSORED")
        AndroidView(
            factory = { ctx -> NativeAdViews.create(ctx) },
            update = { view -> NativeAdViews.bind(view, current) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Programmatic native ad layout styled like the rest of the app (dark panel, lime call to action). */
internal object NativeAdViews {
    private const val PANEL = 0xFF181B1E.toInt()
    private const val LINE = 0xFF2C3136.toInt()
    private const val INK = 0xFFECEEE8.toInt()
    private const val SOFT = 0xFFC9CEC6.toInt()
    private const val MUTED = 0xFF9AA19A.toInt()
    private const val LIME = 0xFFC8F03C.toInt()
    private const val GRAPHITE = 0xFF0F1113.toInt()

    fun create(context: Context): NativeAdView {
        val density = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        val mono = ResourcesCompat.getFont(context, R.font.plex_mono_semibold)
        val display = ResourcesCompat.getFont(context, R.font.chakra_petch_semibold)
        val wrap = ViewGroup.LayoutParams.WRAP_CONTENT
        val match = ViewGroup.LayoutParams.MATCH_PARENT

        val adView = NativeAdView(context)
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(14))
            background = GradientDrawable().apply {
                setColor(PANEL)
                setStroke(dp(1), LINE)
                cornerRadius = dp(4).toFloat()
            }
        }

        val top = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val badge = TextView(context).apply {
            text = context.getString(R.string.ad_badge)
            typeface = mono
            textSize = 10f
            setTextColor(GRAPHITE)
            setBackgroundColor(LIME)
            setPadding(dp(6), dp(1), dp(6), dp(1))
        }
        val advertiser = TextView(context).apply {
            typeface = mono
            textSize = 11f
            setTextColor(MUTED)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setPadding(dp(8), 0, dp(28), 0)
        }
        top.addView(badge, LinearLayout.LayoutParams(wrap, wrap))
        top.addView(advertiser, LinearLayout.LayoutParams(0, wrap, 1f))

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val icon = ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
        val texts = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val headline = TextView(context).apply {
            typeface = display
            textSize = 15f
            setTextColor(INK)
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
        }
        val body = TextView(context).apply {
            textSize = 12f
            setTextColor(SOFT)
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
        }
        texts.addView(headline)
        texts.addView(body)
        row.addView(icon, LinearLayout.LayoutParams(dp(44), dp(44)).apply { marginEnd = dp(10) })
        row.addView(texts, LinearLayout.LayoutParams(0, wrap, 1f))

        val media = MediaView(context)
        val cta = TextView(context).apply {
            typeface = display
            textSize = 14f
            isAllCaps = true
            gravity = Gravity.CENTER
            setTextColor(GRAPHITE)
            setBackgroundColor(LIME)
        }

        root.addView(top, LinearLayout.LayoutParams(match, wrap))
        root.addView(row, LinearLayout.LayoutParams(match, wrap).apply { topMargin = dp(10) })
        root.addView(media, LinearLayout.LayoutParams(match, dp(168)).apply { topMargin = dp(10) })
        root.addView(cta, LinearLayout.LayoutParams(match, dp(46)).apply { topMargin = dp(12) })
        adView.addView(root, ViewGroup.LayoutParams(match, wrap))

        adView.headlineView = headline
        adView.bodyView = body
        adView.advertiserView = advertiser
        adView.iconView = icon
        adView.mediaView = media
        adView.callToActionView = cta
        return adView
    }

    fun bind(view: NativeAdView, ad: NativeAd) {
        if (view.tag === ad) return
        view.tag = ad

        (view.headlineView as TextView).text = ad.headline
        (view.bodyView as TextView).apply {
            text = ad.body.orEmpty()
            visibility = if (ad.body.isNullOrBlank()) View.GONE else View.VISIBLE
        }
        (view.advertiserView as TextView).text = ad.advertiser ?: ad.store.orEmpty()
        (view.iconView as ImageView).apply {
            val drawable = ad.icon?.drawable
            setImageDrawable(drawable)
            visibility = if (drawable == null) View.GONE else View.VISIBLE
        }
        (view.callToActionView as TextView).apply {
            text = ad.callToAction.orEmpty()
            visibility = if (ad.callToAction.isNullOrBlank()) View.GONE else View.VISIBLE
        }
        view.mediaView?.apply {
            val content = ad.mediaContent
            if (content != null) setMediaContent(content)
            visibility = if (content == null) View.GONE else View.VISIBLE
        }
        view.setNativeAd(ad)
    }
}
