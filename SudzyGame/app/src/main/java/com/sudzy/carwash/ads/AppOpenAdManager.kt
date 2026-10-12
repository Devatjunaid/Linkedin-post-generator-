package com.sudzy.carwash.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.sudzy.carwash.MainActivity

/**
 * App open ads, shown ONLY when the player returns to the game from the background.
 *
 * - Never on a cold start / first launch: [returningFromBackground] only becomes true after
 *   the app has actually been sent to the background during this process.
 * - Never when coming back from one of our own ads, or from a link we opened (review, share...).
 * - Never on top of another full screen ad.
 */
class AppOpenAdManager(private val app: Application, private val ads: AdsManager) : Application.ActivityLifecycleCallbacks {
    private var ad: AppOpenAd? = null
    private var loading = false
    private var loadedAt = 0L
    private var started = 0
    private var returningFromBackground = false
    private var skipNext = false

    init { app.registerActivityLifecycleCallbacks(this) }

    /** Call before sending the player outside the app on purpose. */
    fun skipNextForeground() { skipNext = true }

    private fun available() = ad != null && System.currentTimeMillis() - loadedAt < AdsConfig.APP_OPEN_EXPIRY_MS

    fun load() {
        if (!ads.ready || loading || available()) return
        loading = true
        AppOpenAd.load(app, AdsConfig.APP_OPEN, ads.request(), object : AppOpenAd.AppOpenAdLoadCallback() {
            override fun onAdLoaded(loaded: AppOpenAd) { ad = loaded; loadedAt = System.currentTimeMillis(); loading = false }
            override fun onAdFailedToLoad(error: LoadAdError) { Log.w(AdsManager.TAG, "App open failed: ${error.message}"); loading = false }
        })
    }

    private fun onForeground(activity: Activity) {
        if (skipNext) { skipNext = false; load(); return }
        if (ads.showingFullScreen || activity !is MainActivity) return
        val current = ad
        if (current == null || !available()) { ad = null; load(); return }
        ad = null
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() { ads.showingFullScreen = true }
            override fun onAdDismissedFullScreenContent() { ads.showingFullScreen = false; load() }
            override fun onAdFailedToShowFullScreenContent(error: AdError) { ads.showingFullScreen = false; load() }
            override fun onAdClicked() { skipNext = true }
        }
        current.show(activity)
    }

    override fun onActivityStarted(activity: Activity) {
        started++
        if (started == 1 && returningFromBackground) onForeground(activity)
    }

    override fun onActivityStopped(activity: Activity) {
        started = (started - 1).coerceAtLeast(0)
        if (started == 0 && !activity.isChangingConfigurations) returningFromBackground = true
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
