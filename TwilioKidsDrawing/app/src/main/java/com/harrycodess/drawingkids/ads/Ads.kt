package com.harrycodess.drawingkids.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.harrycodess.drawingkids.MainActivity
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Ad unit ids. These are Google's public TEST ids, which always serve test ads.
 * Replace them with your own ids from the AdMob console before publishing
 * (and the App ID in app/build.gradle.kts).
 */
object AdUnits {
    const val BANNER = "ca-app-pub-3940256099942544/9214589741"
    const val INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    const val APP_OPEN = "ca-app-pub-3940256099942544/9257395921"
}

object Ads {
    private val started = AtomicBoolean(false)

    @Volatile
    var ready = false
        private set

    lateinit var appOpen: AppOpenAdManager
        private set

    /** Child-directed setup required for apps in Google Play's Families program. */
    fun start(app: Application) {
        if (!started.compareAndSet(false, true)) return
        appOpen = AppOpenAdManager(app)
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
                .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
                .build(),
        )
        Thread {
            MobileAds.initialize(app) {
                Handler(Looper.getMainLooper()).post {
                    ready = true
                    appOpen.load()
                    Interstitials.load(app)
                }
            }
        }.start()
    }

    fun request(): AdRequest = AdRequest.Builder().build()
}

object Interstitials {
    private var ad: InterstitialAd? = null
    private var loading = false

    var isShowing = false
        private set

    fun load(context: Context) {
        if (!Ads.ready || ad != null || loading) return
        loading = true
        InterstitialAd.load(
            context.applicationContext, AdUnits.INTERSTITIAL, Ads.request(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(loaded: InterstitialAd) {
                    ad = loaded
                    loading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    ad = null
                    loading = false
                }
            },
        )
    }

    /** Shows an interstitial if one is ready, then runs [next]. Without an ad, [next] runs right away. */
    fun show(activity: Activity, next: () -> Unit) {
        if (isShowing) return
        val current = ad
        if (current == null) {
            next()
            load(activity)
            return
        }
        ad = null
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                isShowing = false
                next()
                load(activity)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                isShowing = false
                next()
                load(activity)
            }
        }
        isShowing = true
        current.show(activity)
    }
}

/**
 * App open ads: one after the splash on a cold start, and one each time the app comes back
 * from the background.
 */
class AppOpenAdManager(private val app: Application) :
    DefaultLifecycleObserver, Application.ActivityLifecycleCallbacks {

    private var ad: AppOpenAd? = null
    private var loading = false
    private var loadedAt = 0L
    private var showing = false
    private var activity: Activity? = null

    /** Off until the splash screen has had its turn, so the cold-start ad isn't shown twice. */
    var resumeAdsEnabled = false

    init {
        app.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    val isAvailable: Boolean
        get() = ad != null && System.currentTimeMillis() - loadedAt < 4 * 60 * 60 * 1000L

    fun load() {
        if (!Ads.ready || loading || isAvailable) return
        loading = true
        AppOpenAd.load(
            app, AdUnits.APP_OPEN, Ads.request(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(loaded: AppOpenAd) {
                    ad = loaded
                    loadedAt = System.currentTimeMillis()
                    loading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loading = false
                }
            },
        )
    }

    fun showIfAvailable(activity: Activity, onDone: () -> Unit) {
        val current = ad
        if (showing || Interstitials.isShowing || current == null || !isAvailable) {
            onDone()
            load()
            return
        }
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                ad = null
                showing = false
                onDone()
                load()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                ad = null
                showing = false
                onDone()
                load()
            }
        }
        showing = true
        current.show(activity)
    }

    override fun onStart(owner: LifecycleOwner) {
        if (!resumeAdsEnabled) return
        activity?.let { showIfAvailable(it) {} }
    }

    override fun onActivityStarted(activity: Activity) {
        if (activity is MainActivity) this.activity = activity
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (this.activity === activity) this.activity = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}
