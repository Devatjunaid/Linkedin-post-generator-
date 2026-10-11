package com.shelfguard.app.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.shelfguard.app.MainActivity

/**
 * App-open ads, shown ONLY when the user brings the app back from the background.
 * The very first (cold) start never shows one: [wentToBackground] is false until the
 * whole app has been stopped at least once.
 */
class AppOpenAdManager(private val app: Application) :
    DefaultLifecycleObserver, Application.ActivityLifecycleCallbacks {

    private var appOpenAd: AppOpenAd? = null
    private var isLoading = false
    private var loadedAt = 0L
    private var currentActivity: Activity? = null
    private var wentToBackground = false

    init {
        instance = this
        app.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun load() {
        if (!AdsManager.ready.value || isLoading || isAdFresh()) return
        isLoading = true
        AppOpenAd.load(
            app,
            AdIds.appOpen,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    loadedAt = SystemClock.elapsedRealtime()
                    isLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                }
            },
        )
    }

    /** App-open ads expire after 4 hours. */
    private fun isAdFresh(): Boolean =
        appOpenAd != null && SystemClock.elapsedRealtime() - loadedAt < FOUR_HOURS_MS

    override fun onStop(owner: LifecycleOwner) {
        // Whole app went to the background (ProcessLifecycleOwner ignores rotation etc.).
        wentToBackground = true
    }

    override fun onStart(owner: LifecycleOwner) {
        if (!wentToBackground) return // first open of this process: never show
        wentToBackground = false
        if (FullScreenAdGate.skipNextAppOpen) {
            FullScreenAdGate.skipNextAppOpen = false
            load()
            return
        }
        val activity = currentActivity ?: return
        if (activity is MainActivity && activity.isShowingSplash) return
        show(activity)
    }

    private fun show(activity: Activity) {
        val ad = appOpenAd
        if (FullScreenAdGate.isShowing || ad == null || !isAdFresh()) {
            load()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                FullScreenAdGate.markShown()
            }

            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                FullScreenAdGate.markDismissed()
                load()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                appOpenAd = null
                FullScreenAdGate.markDismissed()
                load()
            }

            override fun onAdClicked() {
                FullScreenAdGate.skipNextAppOpen = true
            }
        }
        ad.show(activity)
    }

    override fun onActivityStarted(activity: Activity) {
        if (activity !is AdActivity) currentActivity = activity
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity === activity) currentActivity = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    companion object {
        private const val FOUR_HOURS_MS = 4 * 60 * 60 * 1000L
        var instance: AppOpenAdManager? = null
            private set
    }
}
