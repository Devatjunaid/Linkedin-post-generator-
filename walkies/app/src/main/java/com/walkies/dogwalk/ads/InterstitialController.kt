package com.walkies.dogwalk.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Interstitials shown at natural breaks after a major action (saving a walk, sharing the vet
 * report, adding a reminder) and never more than once every [MIN_INTERVAL_MS]. They are never
 * shown on app launch or on exit, which AdMob policy does not allow.
 */
class InterstitialController(
    private val context: Context,
    private val adsReady: () -> Boolean,
) {
    private val handler = Handler(Looper.getMainLooper())
    private var ad: InterstitialAd? = null
    private var loading = false
    private var showing = false
    private var lastShownAt = 0L
    private var retryDelayMs = FIRST_RETRY_MS

    fun preload() {
        if (loading || ad != null || !adsReady()) return
        loading = true
        InterstitialAd.load(
            context,
            AdUnits.interstitial,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    ad = interstitialAd
                    loading = false
                    retryDelayMs = FIRST_RETRY_MS
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    ad = null
                    loading = false
                    handler.postDelayed({ preload() }, retryDelayMs)
                    retryDelayMs = (retryDelayMs * 2).coerceAtMost(MAX_RETRY_MS)
                }
            },
        )
    }

    /**
     * Shows an interstitial if one is ready and the frequency cap allows it, then runs
     * [onContinue] once the ad is closed. Without an ad, [onContinue] runs straight away.
     */
    fun showIfAllowed(activity: Activity, onContinue: () -> Unit) {
        val current = ad
        val now = SystemClock.elapsedRealtime()
        val capped = lastShownAt != 0L && now - lastShownAt < MIN_INTERVAL_MS
        if (current == null || showing || capped || activity.isFinishing) {
            if (current == null) preload()
            onContinue()
            return
        }
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                lastShownAt = SystemClock.elapsedRealtime()
            }

            override fun onAdDismissedFullScreenContent() {
                finish(onContinue)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                finish(onContinue)
            }
        }
        showing = true
        ad = null
        current.show(activity)
    }

    private fun finish(onContinue: () -> Unit) {
        showing = false
        preload()
        onContinue()
    }

    private companion object {
        const val MIN_INTERVAL_MS = 30_000L
        const val FIRST_RETRY_MS = 30_000L
        const val MAX_RETRY_MS = 5 * 60_000L
    }
}
