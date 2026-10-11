package com.shelfguard.app.ads

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
 * Interstitials shown at natural breaks (opening Inventory or Reports, after saving a product).
 * Frequency-capped so they never feel spammy:
 *  - not in the first [GRACE_AFTER_LAUNCH_MS] of a session,
 *  - at most once every [COOLDOWN_MS],
 *  - never right after another full-screen ad.
 */
object InterstitialAdManager {
    private const val COOLDOWN_MS = 90_000L
    private const val GRACE_AFTER_LAUNCH_MS = 30_000L
    private const val AFTER_OTHER_AD_MS = 30_000L
    private const val MAX_RETRIES = 3

    private var ad: InterstitialAd? = null
    private var isLoading = false
    private var retries = 0
    private var lastShownAt = 0L
    private val sessionStart = SystemClock.elapsedRealtime()
    private val handler = Handler(Looper.getMainLooper())

    fun load(context: Context) {
        if (!AdsManager.ready.value || isLoading || ad != null) return
        isLoading = true
        val appContext = context.applicationContext
        InterstitialAd.load(
            appContext,
            AdIds.interstitial,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(loaded: InterstitialAd) {
                    ad = loaded
                    isLoading = false
                    retries = 0
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    if (retries < MAX_RETRIES) {
                        retries++
                        handler.postDelayed({ load(appContext) }, 15_000L * retries)
                    }
                }
            },
        )
    }

    private fun isEligible(): Boolean {
        val now = SystemClock.elapsedRealtime()
        return ad != null &&
            !FullScreenAdGate.isShowing &&
            now - sessionStart >= GRACE_AFTER_LAUNCH_MS &&
            (lastShownAt == 0L || now - lastShownAt >= COOLDOWN_MS) &&
            (FullScreenAdGate.lastDismissedAt == 0L || now - FullScreenAdGate.lastDismissedAt >= AFTER_OTHER_AD_MS)
    }

    /**
     * Shows an interstitial if one is ready and allowed, then calls [onFinished] after it closes.
     * If no ad is shown, [onFinished] runs immediately, so callers can always put their
     * navigation in it.
     */
    fun showIfEligible(activity: Activity?, onFinished: () -> Unit) {
        val current = ad
        if (activity == null || current == null || !isEligible()) {
            onFinished()
            activity?.let { load(it) }
            return
        }
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                FullScreenAdGate.markShown()
            }

            override fun onAdDismissedFullScreenContent() {
                FullScreenAdGate.markDismissed()
                lastShownAt = SystemClock.elapsedRealtime()
                ad = null
                onFinished()
                load(activity)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                FullScreenAdGate.markDismissed()
                ad = null
                onFinished()
                load(activity)
            }

            override fun onAdClicked() {
                FullScreenAdGate.skipNextAppOpen = true
            }
        }
        current.show(activity)
    }
}
