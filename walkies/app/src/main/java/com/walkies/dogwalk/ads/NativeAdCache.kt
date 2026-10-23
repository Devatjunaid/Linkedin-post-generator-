package com.walkies.dogwalk.ads

import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.mutableStateMapOf
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions

/**
 * Keeps one native ad per placement so switching tabs does not fire a new request each time.
 * An ad is replaced after [MAX_AGE_MS], well inside the one-hour validity of a native ad.
 */
class NativeAdCache(
    private val context: Context,
    private val adsReady: () -> Boolean,
) {
    private class Entry(val ad: NativeAd, val loadedAt: Long)

    private val ads = mutableStateMapOf<String, Entry>()
    private val failed = mutableStateMapOf<String, Boolean>()
    private val loading = mutableSetOf<String>()

    fun adFor(placement: String): NativeAd? = ads[placement]?.ad

    fun hasFailed(placement: String): Boolean = failed[placement] == true

    fun request(placement: String) {
        if (!adsReady() || placement in loading) return
        val cached = ads[placement]
        if (cached != null && SystemClock.elapsedRealtime() - cached.loadedAt < MAX_AGE_MS) return
        loading += placement
        val loader = AdLoader.Builder(context, AdUnits.native)
            .forNativeAd { nativeAd ->
                loading -= placement
                failed.remove(placement)
                ads.put(placement, Entry(nativeAd, SystemClock.elapsedRealtime()))?.ad?.destroy()
            }
            .withAdListener(
                object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        loading -= placement
                        if (ads[placement] == null) failed[placement] = true
                    }
                },
            )
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_LANDSCAPE)
                    .build(),
            )
            .build()
        loader.loadAd(AdRequest.Builder().build())
    }

    companion object {
        const val HOME = "home"
        const val LOG = "log"
        const val REMINDERS = "reminders"
        const val WALK = "walk"
        private const val MAX_AGE_MS = 30 * 60_000L
    }
}
