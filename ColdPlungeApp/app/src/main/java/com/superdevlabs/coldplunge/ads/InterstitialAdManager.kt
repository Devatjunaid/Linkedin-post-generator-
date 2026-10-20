package com.superdevlabs.coldplunge.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.superdevlabs.coldplunge.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Interstitials are shown only at natural breaks after a major action (starting a session,
 * saving a session or protocol, exporting the log) and never more than once every 30 seconds.
 * They are never shown on app launch, on exit, or while a timer is running.
 */
class InterstitialAdManager(
    private val app: Context,
    private val consent: AdsConsentManager,
) {
    private val cap = FrequencyCap(intervalMs = 30_000L) { SystemClock.elapsedRealtime() }
    private val handler = Handler(Looper.getMainLooper())
    private var ad: InterstitialAd? = null
    private var loading = false
    private var showing = false
    private var failures = 0

    fun preload() {
        if (!consent.adsReady.value || ad != null || loading) return
        loading = true
        InterstitialAd.load(
            app,
            BuildConfig.ADMOB_INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    ad = interstitialAd
                    loading = false
                    failures = 0
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    ad = null
                    loading = false
                    failures++
                    // Back off: 30 s, 60 s, 90 s ... capped at 5 minutes.
                    val delayMs = (30_000L * failures).coerceAtMost(300_000L)
                    handler.postDelayed({ preload() }, delayMs)
                }
            },
        )
    }

    /**
     * Shows an interstitial when one is ready and the 30-second cap allows it.
     * [onContinue] is always invoked exactly once — right away, or after the ad closes.
     */
    fun showThen(activity: Activity, onContinue: () -> Unit) {
        val current = ad
        if (current == null || showing || !cap.canShow()) {
            preload()
            onContinue()
            return
        }
        var continued = false
        val proceed = {
            if (!continued) {
                continued = true
                onContinue()
            }
        }
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                cap.markShown()
            }

            override fun onAdDismissedFullScreenContent() {
                showing = false
                cap.markShown() // the 30 s window starts when the user is back in the app
                preload()
                proceed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                showing = false
                preload()
                proceed()
            }
        }
        ad = null
        showing = true
        current.show(activity)
    }
}
