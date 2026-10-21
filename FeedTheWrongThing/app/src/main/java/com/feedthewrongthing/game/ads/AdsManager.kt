package com.feedthewrongthing.game.ads

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Google Mobile Ads, configured for a child-directed (Designed for Families) app:
 * child-directed + under-age-of-consent tags, G-rated ads only, no ads on launch.
 *
 * Banner: if the first two requests fail, later retries wait 15 seconds each.
 * Interstitial: at most one every 30 seconds (shared with rewarded ads).
 * Rewarded: only ever shown after the child taps an opt-in "Watch" button.
 */
class AdsManager(private val activity: Activity, private val container: FrameLayout, private val listener: Listener) {

    interface Listener {
        fun onFullScreenAdShowing(showing: Boolean)
        fun onBannerHeightChanged(px: Int)
    }

    private val main = Handler(Looper.getMainLooper())
    private val initStarted = AtomicBoolean(false)
    private var ready = false
    private var destroyed = false

    private var adView: AdView? = null
    private var bannerVisible = false
    private var bannerLoaded = false
    private var bannerLoading = false
    private var bannerFailures = 0
    private var bannerHeight = 0

    private var interstitial: InterstitialAd? = null
    private var interstitialLoading = false
    private var rewarded: RewardedAd? = null
    private var rewardedLoading = false
    private var interstitialRetry = 0
    private var rewardedRetry = 0

    /** Counts from app start so no full-screen ad can appear in the first 30 seconds. */
    private var lastFullScreen = SystemClock.elapsedRealtime()
    private var showingFullScreen = false

    fun init() {
        if (!initStarted.compareAndSet(false, true)) return
        val config = RequestConfiguration.Builder()
            .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
            .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
            .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
            .build()
        MobileAds.setRequestConfiguration(config)
        Thread({
            MobileAds.initialize(activity.applicationContext) {
                main.post {
                    if (destroyed) return@post
                    ready = true
                    setupBanner()
                    loadInterstitial()
                    loadRewarded()
                }
            }
        }, "ads-init").start()
    }

    private fun request(): AdRequest = AdRequest.Builder().build()

    // ------------------------------------------------------------------ banner

    private fun setupBanner() {
        if (adView != null) return
        val view = AdView(activity)
        view.setAdUnitId(BANNER_ID)
        val size = adaptiveSize()
        view.setAdSize(size)
        bannerHeight = size.getHeightInPixels(activity)
        view.adListener = object : AdListener() {
            override fun onAdLoaded() {
                bannerLoading = false
                bannerLoaded = true
                bannerFailures = 0
                applyBannerVisibility()
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                bannerLoading = false
                bannerFailures++
                // First two attempts retry right away; after that wait 15 seconds between requests.
                val delay = if (bannerFailures < 2) 0L else BANNER_RETRY_MS
                main.postDelayed({ loadBanner() }, delay)
            }
        }
        val lp = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
        container.addView(view, lp)
        view.visibility = View.GONE
        adView = view
        loadBanner()
    }

    private fun loadBanner() {
        val v = adView ?: return
        if (destroyed || bannerLoading || bannerLoaded) return
        bannerLoading = true
        v.loadAd(request())
    }

    private fun adaptiveSize(): AdSize {
        val metrics: DisplayMetrics = activity.resources.displayMetrics
        val widthPx = container.width.takeIf { it > 0 } ?: metrics.widthPixels
        val widthDp = (widthPx / metrics.density).toInt().coerceAtMost(728)
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, widthDp)
    }

    fun setBannerVisible(visible: Boolean) {
        bannerVisible = visible
        applyBannerVisibility()
    }

    private fun applyBannerVisibility() {
        val v = adView ?: return
        val show = bannerVisible && bannerLoaded && !showingFullScreen
        v.visibility = if (show) View.VISIBLE else View.GONE
        listener.onBannerHeightChanged(if (show) bannerHeight else 0)
    }

    // ------------------------------------------------------------------ interstitial

    private fun loadInterstitial() {
        if (!ready || destroyed || interstitial != null || interstitialLoading) return
        interstitialLoading = true
        InterstitialAd.load(activity, INTERSTITIAL_ID, request(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitialLoading = false
                interstitialRetry = 0
                interstitial = ad
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                interstitialLoading = false
                interstitial = null
                interstitialRetry++
                main.postDelayed({ loadInterstitial() }, retryDelay(interstitialRetry))
            }
        })
    }

    private fun capOk(): Boolean = SystemClock.elapsedRealtime() - lastFullScreen >= INTERSTITIAL_CAP_MS

    /** Shows an interstitial if one is ready and the 30-second cap allows it, then runs [then]. */
    fun showInterstitial(then: () -> Unit) {
        val ad = interstitial
        if (ad == null || !capOk() || showingFullScreen || destroyed) {
            then()
            loadInterstitial()
            return
        }
        interstitial = null
        var done = false
        val finish = {
            if (!done) {
                done = true
                fullScreen(false)
                then()
                loadInterstitial()
            }
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                lastFullScreen = SystemClock.elapsedRealtime()
            }

            override fun onAdDismissedFullScreenContent() = finish()

            override fun onAdFailedToShowFullScreenContent(error: AdError) = finish()
        }
        fullScreen(true)
        lastFullScreen = SystemClock.elapsedRealtime()
        ad.show(activity)
    }

    // ------------------------------------------------------------------ rewarded

    private fun loadRewarded() {
        if (!ready || destroyed || rewarded != null || rewardedLoading) return
        rewardedLoading = true
        RewardedAd.load(activity, REWARDED_ID, request(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                rewardedLoading = false
                rewardedRetry = 0
                rewarded = ad
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                rewardedLoading = false
                rewarded = null
                rewardedRetry++
                main.postDelayed({ loadRewarded() }, retryDelay(rewardedRetry))
            }
        })
    }

    fun isRewardedReady(): Boolean = rewarded != null && !showingFullScreen

    fun showRewarded(onDone: (earned: Boolean) -> Unit) {
        val ad = rewarded
        if (ad == null || destroyed) {
            onDone(false)
            loadRewarded()
            return
        }
        rewarded = null
        var earned = false
        var done = false
        val finish = {
            if (!done) {
                done = true
                fullScreen(false)
                onDone(earned)
                loadRewarded()
            }
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                lastFullScreen = SystemClock.elapsedRealtime()
            }

            override fun onAdDismissedFullScreenContent() = finish()

            override fun onAdFailedToShowFullScreenContent(error: AdError) = finish()
        }
        fullScreen(true)
        lastFullScreen = SystemClock.elapsedRealtime()
        ad.show(activity) { _ -> earned = true }
    }

    private fun fullScreen(on: Boolean) {
        showingFullScreen = on
        applyBannerVisibility()
        listener.onFullScreenAdShowing(on)
    }

    private fun retryDelay(attempt: Int): Long = (15_000L * attempt).coerceAtMost(120_000L)

    // ------------------------------------------------------------------ lifecycle

    fun onPause() { adView?.pause() }

    fun onResume() { adView?.resume() }

    fun onDestroy() {
        destroyed = true
        main.removeCallbacksAndMessages(null)
        adView?.destroy()
        adView = null
        interstitial = null
        rewarded = null
    }

    companion object {
        const val APP_ID = "ca-app-pub-1948347222012540~6281556983"
        const val BANNER_ID = "ca-app-pub-1948347222012540/2102638829"
        const val INTERSTITIAL_ID = "ca-app-pub-1948347222012540/9789557152"
        const val REWARDED_ID = "ca-app-pub-1948347222012540/7500726159"
        const val BANNER_RETRY_MS = 15_000L
        const val INTERSTITIAL_CAP_MS = 30_000L
    }
}
