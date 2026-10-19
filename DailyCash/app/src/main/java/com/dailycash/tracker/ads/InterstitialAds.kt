package com.dailycash.tracker.ads

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
 * Interstitials are shown only after a finished action (saving an entry, opening a report),
 * never on launch or exit, and at most once every 30 seconds.
 */
object InterstitialAds {
    private const val CAP_MS = 30_000L
    private const val RETRY_MS = 30_000L

    private var ad: InterstitialAd? = null
    private var loading = false
    private var lastShownAt = 0L
    private val handler = Handler(Looper.getMainLooper())

    fun preload(context: Context) {
        if (!AdsConsent.canRequestAds.value || ad != null || loading) return
        loading = true
        val app = context.applicationContext
        InterstitialAd.load(app, AdIds.interstitial, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(loaded: InterstitialAd) {
                ad = loaded
                loading = false
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                ad = null
                loading = false
                handler.postDelayed({ preload(app) }, RETRY_MS)
            }
        })
    }

    /** Runs [then] right away, or after the ad is closed when one is due and ready. */
    fun showThen(activity: Activity?, then: () -> Unit) {
        val current = ad
        val now = SystemClock.elapsedRealtime()
        val capped = lastShownAt != 0L && now - lastShownAt < CAP_MS
        if (activity == null || current == null || capped || !AdsConsent.canRequestAds.value) {
            then()
            if (activity != null) preload(activity)
            return
        }
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                then()
                preload(activity)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                then()
                preload(activity)
            }
        }
        ad = null
        lastShownAt = now
        current.show(activity)
    }
}
