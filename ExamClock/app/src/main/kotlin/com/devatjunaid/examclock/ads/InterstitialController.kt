package com.devatjunaid.examclock.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.devatjunaid.examclock.domain.AdRetryPolicy
import com.devatjunaid.examclock.domain.FrequencyCap
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Preloads one interstitial and shows it after major actions (saving an exam, generating a plan,
 * finishing a focus session) — never on app start and never in the middle of a task.
 *
 * Frequency cap: at most one interstitial per 30 seconds, counted from when the previous one was
 * closed. If no ad is ready or the cap blocks it, [showIfAllowed] simply continues the action.
 */
class InterstitialController(
    private val context: Context,
    private val adUnitId: String,
    private val canRequestAds: () -> Boolean,
) {
    private val handler = Handler(Looper.getMainLooper())
    private val cap = FrequencyCap(intervalMillis = 30_000L) { SystemClock.elapsedRealtime() }
    private val retry = AdRetryPolicy(immediateAttempts = 2, backoffMillis = 15_000L)
    private var ad: InterstitialAd? = null
    private var loading = false
    private var showing = false

    fun preload() {
        if (!canRequestAds() || loading || ad != null) return
        loading = true
        InterstitialAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    loading = false
                    retry.onSuccess()
                    ad = interstitialAd
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loading = false
                    handler.postDelayed({ preload() }, retry.onFailure())
                }
            },
        )
    }

    /** Shows the ad if one is loaded and the cap allows it, then runs [onContinue] exactly once. */
    fun showIfAllowed(activity: Activity?, onContinue: () -> Unit) {
        val loaded = ad
        if (activity == null || loaded == null || showing || !cap.canShow() || activity.isFinishing) {
            onContinue()
            preload()
            return
        }
        var continued = false
        fun continueOnce() {
            if (!continued) {
                continued = true
                onContinue()
            }
        }
        loaded.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                cap.markShown()
            }

            override fun onAdDismissedFullScreenContent() {
                showing = false
                cap.markShown()
                continueOnce()
                preload()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                showing = false
                continueOnce()
                preload()
            }
        }
        ad = null
        showing = true
        loaded.show(activity)
    }
}
