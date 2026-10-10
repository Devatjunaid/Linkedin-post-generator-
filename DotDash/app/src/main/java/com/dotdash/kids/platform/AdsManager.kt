package com.dotdash.kids.platform

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.dotdash.kids.game.LevelAds
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.lang.ref.WeakReference

/**
 * AdMob setup.
 *
 * These are Google's official TEST ad units, safe while developing. Before
 * publishing, replace them with your own ad unit IDs from https://apps.admob.com
 * and replace the APPLICATION_ID in AndroidManifest.xml.
 */
object AdConfig {
    const val BANNER = "ca-app-pub-3940256099942544/9214589741"
    const val INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    const val APP_OPEN = "ca-app-pub-3940256099942544/9257395921"

    /** Interstitial before every level (as requested). Raise to show it every N level opens. */
    const val INTERSTITIAL_EVERY_N_LEVELS = 1
}

/**
 * - Banner: shown under the game (see [AdBanner]).
 * - Interstitial: each time a level opens; if none is ready the level starts at once.
 * - App open: ONLY when the app returns from the background, never on first launch,
 *   and not when the app was left because an ad was open or tapped.
 */
class AdsManager(
    private val app: Application,
    private val onPause: () -> Unit,
    private val onResume: () -> Unit,
) : LevelAds, DefaultLifecycleObserver, Application.ActivityLifecycleCallbacks {

    private val main = Handler(Looper.getMainLooper())
    private var activityRef = WeakReference<Activity>(null)
    val currentActivity: Activity? get() = activityRef.get()

    private var ready = false
    private var interstitial: InterstitialAd? = null
    private var interstitialLoading = false
    private var appOpen: AppOpenAd? = null
    private var appOpenLoadedAt = 0L
    private var appOpenLoading = false
    private var fullscreen = false // an interstitial or app-open ad is on screen
    private var adClicked = false // the user left the app by tapping an ad
    private var backgrounded = false
    private var exiting = false // the player tapped Exit: the next launch is a fresh start
    private var levelOpens = 0

    fun start() {
        app.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        // A game for young children: every request is child-directed and G-rated
        // (COPPA / Google Play Families policy).
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
                .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
                .build(),
        )
        Thread {
            MobileAds.initialize(app) {
                main.post {
                    ready = true
                    loadInterstitial()
                    loadAppOpen()
                }
            }
        }.start()
    }

    private fun request() = AdRequest.Builder().build()

    // --- interstitial ----------------------------------------------------------
    private fun loadInterstitial() {
        if (!ready || interstitial != null || interstitialLoading) return
        interstitialLoading = true
        InterstitialAd.load(app, AdConfig.INTERSTITIAL, request(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitial = ad
                interstitialLoading = false
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                interstitialLoading = false
                main.postDelayed({ loadInterstitial() }, 30_000)
            }
        })
    }

    override fun onLevelOpen(then: () -> Unit) {
        levelOpens++
        if (levelOpens % AdConfig.INTERSTITIAL_EVERY_N_LEVELS.coerceAtLeast(1) != 0) return then()
        val ad = interstitial
        val activity = currentActivity
        if (ad == null || activity == null) {
            loadInterstitial()
            then()
            return
        }
        interstitial = null
        fullscreen = true
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = done()
            override fun onAdFailedToShowFullScreenContent(error: AdError) = done()
            override fun onAdClicked() {
                adClicked = true
            }

            private fun done() {
                fullscreen = false
                then()
                loadInterstitial()
            }
        }
        ad.show(activity)
    }

    // --- app open --------------------------------------------------------------
    private fun appOpenFresh() = appOpen != null && SystemClock.elapsedRealtime() - appOpenLoadedAt < 4 * 60 * 60 * 1000L

    private fun loadAppOpen() {
        if (!ready || appOpenLoading || appOpenFresh()) return
        appOpenLoading = true
        AppOpenAd.load(app, AdConfig.APP_OPEN, request(), object : AppOpenAd.AppOpenAdLoadCallback() {
            override fun onAdLoaded(ad: AppOpenAd) {
                appOpen = ad
                appOpenLoadedAt = SystemClock.elapsedRealtime()
                appOpenLoading = false
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                appOpenLoading = false
            }
        })
    }

    private fun showAppOpen() {
        val ad = appOpen
        val activity = currentActivity
        if (!appOpenFresh() || ad == null || activity == null) {
            loadAppOpen()
            return
        }
        appOpen = null
        fullscreen = true
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = done()
            override fun onAdFailedToShowFullScreenContent(error: AdError) = done()
            override fun onAdClicked() {
                adClicked = true
            }

            private fun done() {
                fullscreen = false
                loadAppOpen()
            }
        }
        ad.show(activity)
    }

    // --- whole-app foreground / background ----------------------------------------
    override fun onStop(owner: LifecycleOwner) {
        onPause()
        if (!fullscreen && !adClicked && !exiting) backgrounded = true
    }

    override fun onStart(owner: LifecycleOwner) {
        onResume()
        adClicked = false
        exiting = false
        if (backgrounded) {
            backgrounded = false
            showAppOpen()
        }
    }

    /** Called when the player exits from the exit screen: the next launch is a fresh start. */
    fun resetSession() {
        exiting = true
        backgrounded = false
    }

    fun onBannerClicked() {
        adClicked = true
    }

    // Track the visible activity so full-screen ads have something to show over.
    override fun onActivityStarted(activity: Activity) {
        if (activity::class.java.name.startsWith("com.dotdash")) activityRef = WeakReference(activity)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) {
        if (activityRef.get() === activity) activityRef = WeakReference(null)
    }
}

/** Adaptive banner that sits under the game; the game is laid out above it, so nothing is covered. */
@Composable
fun AdBanner(ads: AdsManager) {
    val context = LocalContext.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val width = maxWidth.value.toInt()
        val size = remember(width) { AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, width) }
        AndroidView(
            modifier = Modifier.fillMaxWidth().height(size.height.dp),
            factory = { ctx ->
                AdView(ctx).apply {
                    adUnitId = AdConfig.BANNER
                    setAdSize(size)
                    adListener = object : AdListener() {
                        override fun onAdClicked() = ads.onBannerClicked()
                    }
                    loadAd(AdRequest.Builder().build())
                }
            },
            onRelease = { it.destroy() },
        )
    }
}
