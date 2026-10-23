package com.driveready.testprep.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.driveready.testprep.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Preloads one interstitial and shows it on major actions (start mock test, finish a test,
 * open hazard perception, open a maneuver guide, change country), capped at one ad per 30 s.
 */
object InterstitialAdManager {
    private const val TAG = "InterstitialAds"
    private const val RELOAD_BACKOFF_MS = 20_000L

    private var ad: InterstitialAd? = null
    private var isLoading = false
    private var isShowing = false
    private var lastShownAt = 0L
    private val handler = Handler(Looper.getMainLooper())

    fun preload(context: Context) {
        if (!AdsManager.ready.value || isLoading || ad != null) return
        val appContext = context.applicationContext
        isLoading = true
        InterstitialAd.load(
            appContext,
            BuildConfig.INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(loaded: InterstitialAd) {
                    ad = loaded
                    isLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.d(TAG, "Load failed: ${error.code} ${error.message}")
                    ad = null
                    isLoading = false
                    handler.postDelayed({ preload(appContext) }, RELOAD_BACKOFF_MS)
                }
            }
        )
    }

    /**
     * Runs [onContinue] after an interstitial is dismissed, or immediately if no ad is ready or
     * the 30-second cap hasn't elapsed. The user's action is never blocked by an ad.
     */
    fun showThen(activity: Activity, onContinue: () -> Unit) {
        val now = SystemClock.elapsedRealtime()
        val current = ad
        if (current == null || isShowing || !AdPolicy.canShowInterstitial(now, lastShownAt)) {
            if (current == null) preload(activity)
            onContinue()
            return
        }
        isShowing = true
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                lastShownAt = SystemClock.elapsedRealtime()
            }

            override fun onAdDismissedFullScreenContent() {
                ad = null
                isShowing = false
                onContinue()
                preload(activity)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.d(TAG, "Show failed: ${error.message}")
                ad = null
                isShowing = false
                onContinue()
                preload(activity)
            }
        }
        current.show(activity)
    }
}
