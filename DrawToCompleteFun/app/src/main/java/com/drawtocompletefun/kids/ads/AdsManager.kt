package com.drawtocompletefun.kids.ads

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.mutableStateOf
import com.drawtocompletefun.kids.BuildConfig
import com.drawtocompletefun.kids.ui.RewardResult
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean

/**
 * All AdMob logic for a children's app:
 *  - every request is tagged child-directed and under age of consent, max rating G
 *  - interstitials only at "big moments" and never more than one per 30 seconds
 *  - rewarded videos are always started by the player (treasure levels)
 *  - debug builds use Google's test ad units so real ads are never clicked during testing
 */
class AdsManager(private val activity: Activity) {

    object Ids {
        val banner: String = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/9214589741" else "ca-app-pub-1948347222012540/8756958345"
        val interstitial: String = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/1033173712" else "ca-app-pub-1948347222012540/5172936551"
        val rewarded: String = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/5224354917" else "ca-app-pub-1948347222012540/2877945707"
    }

    /** True once the Mobile Ads SDK finished initialising; banners wait for it. */
    val ready = mutableStateOf(false)

    private val main = Handler(Looper.getMainLooper())
    private val initStarted = AtomicBoolean(false)
    private var destroyed = false

    private var interstitial: InterstitialAd? = null
    private var interstitialLoading = false

    private var rewarded: RewardedAd? = null
    private var rewardedLoading = false
    private var pendingReward: ((RewardResult) -> Unit)? = null
    private val rewardTimeout = Runnable { finishPendingReward(RewardResult.Unavailable) }

    /** Time of the last full screen ad. Starts at launch so no ad appears in the first 30s. */
    private var lastFullScreenAt = SystemClock.elapsedRealtime()

    // ------------------------------------------------------------------ Start-up

    /** Runs the consent flow, then initialises ads. [onDone] is always called, at most ~6s later. */
    fun start(onDone: () -> Unit) {
        val done = AtomicBoolean(false)
        val consent: ConsentInformation = UserMessagingPlatform.getConsentInformation(activity)
        fun finish() {
            if (!done.compareAndSet(false, true)) return
            if (consent.canRequestAds()) initAds()
            onDone()
        }

        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(true)
            .build()
        consent.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { finish() }
            },
            { finish() },
        )
        // Consent from an earlier session lets ads start right away.
        if (consent.canRequestAds()) initAds()
        main.postDelayed({ finish() }, 6_000)
    }

    private fun initAds() {
        if (!initStarted.compareAndSet(false, true)) return
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
                .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
                .build(),
        )
        val appContext = activity.applicationContext
        Thread {
            MobileAds.initialize(appContext) {
                main.post {
                    if (destroyed) return@post
                    ready.value = true
                    loadInterstitial()
                    loadRewarded()
                }
            }
        }.start()
    }

    fun destroy() {
        destroyed = true
        main.removeCallbacksAndMessages(null)
        pendingReward = null
    }

    private fun request(): AdRequest = AdRequest.Builder().build()

    // ------------------------------------------------------------------ Interstitial

    private fun loadInterstitial() {
        if (!ready.value || interstitial != null || interstitialLoading || destroyed) return
        interstitialLoading = true
        InterstitialAd.load(
            activity,
            Ids.interstitial,
            request(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialLoading = false
                    interstitial = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialLoading = false
                    main.postDelayed({ loadInterstitial() }, 30_000)
                }
            },
        )
    }

    fun canShowInterstitial(): Boolean =
        interstitial != null && !activity.isFinishing && !destroyed &&
            SystemClock.elapsedRealtime() - lastFullScreenAt >= INTERSTITIAL_GAP_MS

    /** Shows an interstitial when allowed (30 second cap), then always runs [onDone] once. */
    fun showInterstitial(onDone: () -> Unit) {
        val ad = interstitial
        val now = SystemClock.elapsedRealtime()
        if (ad == null || now - lastFullScreenAt < INTERSTITIAL_GAP_MS || activity.isFinishing || destroyed) {
            if (ad == null) loadInterstitial()
            onDone()
            return
        }
        interstitial = null
        val called = AtomicBoolean(false)
        fun complete() {
            if (called.compareAndSet(false, true)) onDone()
            loadInterstitial()
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                lastFullScreenAt = SystemClock.elapsedRealtime()
            }

            override fun onAdDismissedFullScreenContent() {
                lastFullScreenAt = SystemClock.elapsedRealtime()
                complete()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) = complete()
        }
        ad.show(activity)
    }

    // ------------------------------------------------------------------ Rewarded

    private fun loadRewarded() {
        if (!ready.value || rewarded != null || rewardedLoading || destroyed) return
        rewardedLoading = true
        RewardedAd.load(
            activity,
            Ids.rewarded,
            request(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedLoading = false
                    rewarded = ad
                    if (pendingReward != null) presentRewarded()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedLoading = false
                    finishPendingReward(RewardResult.Unavailable)
                }
            },
        )
    }

    /** Called when the player taps "Watch video". Waits up to 8s for a video to load. */
    fun showRewarded(onResult: (RewardResult) -> Unit) {
        if (pendingReward != null) return
        if (!ready.value || destroyed) {
            onResult(RewardResult.Unavailable)
            return
        }
        pendingReward = onResult
        if (rewarded != null) {
            presentRewarded()
        } else {
            main.postDelayed(rewardTimeout, 8_000)
            loadRewarded()
        }
    }

    private fun finishPendingReward(result: RewardResult) {
        main.removeCallbacks(rewardTimeout)
        val cb = pendingReward ?: return
        pendingReward = null
        cb(result)
    }

    private fun presentRewarded() {
        main.removeCallbacks(rewardTimeout)
        val ad = rewarded ?: return finishPendingReward(RewardResult.Unavailable)
        rewarded = null
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                lastFullScreenAt = SystemClock.elapsedRealtime()
            }

            override fun onAdDismissedFullScreenContent() {
                lastFullScreenAt = SystemClock.elapsedRealtime()
                finishPendingReward(if (earned) RewardResult.Earned else RewardResult.Closed)
                loadRewarded()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                finishPendingReward(RewardResult.Unavailable)
                loadRewarded()
            }
        }
        ad.show(activity) { earned = true }
    }

    companion object {
        const val INTERSTITIAL_GAP_MS = 30_000L
    }
}
