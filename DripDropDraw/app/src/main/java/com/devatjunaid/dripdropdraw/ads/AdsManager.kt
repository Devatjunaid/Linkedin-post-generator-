package com.devatjunaid.dripdropdraw.ads

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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

/**
 * All AdMob code lives here.
 *
 * This is a game for children, so every request is tagged as child-directed and under the age
 * of consent, and limited to G-rated ads. The advertising ID permission is removed in the
 * manifest. Full-screen ads only appear at natural breaks (between levels), never during play.
 */
object AdsManager {

    private const val BANNER_ID = "ca-app-pub-1948347222012540/3155780325"
    private const val INTERSTITIAL_ID = "ca-app-pub-1948347222012540/9291697858"
    private const val REWARDED_ID = "ca-app-pub-1948347222012540/6516440838"

    // Google's official test units. Debug builds use these so testing never touches real ads
    // (clicking your own live ads can get an AdMob account suspended).
    private const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/9214589741"
    private const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"

    private var debugBuild = false
    val bannerId get() = if (debugBuild) TEST_BANNER_ID else BANNER_ID
    private val interstitialId get() = if (debugBuild) TEST_INTERSTITIAL_ID else INTERSTITIAL_ID
    private val rewardedId get() = if (debugBuild) TEST_REWARDED_ID else REWARDED_ID

    /** At most one interstitial every 30 seconds. */
    private const val INTERSTITIAL_GAP_MS = 30_000L

    /** Banner: two quick tries, then wait this long between tries. */
    const val BANNER_RETRY_DELAY_MS = 15_000L
    private const val FULLSCREEN_RETRY_DELAY_MS = 30_000L

    private val main by lazy { Handler(Looper.getMainLooper()) }
    private lateinit var appContext: Context

    private var interstitial: InterstitialAd? = null
    private var loadingInterstitial = false
    private var lastInterstitialAt = 0L

    private var rewarded: RewardedAd? = null
    private var loadingRewarded = false

    var showingFullScreen = false
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
        debugBuild = (appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        // No interstitial in the first 30 seconds after the app opens.
        lastInterstitialAt = SystemClock.elapsedRealtime()
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
                .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
                .build(),
        )
        // The SDK recommends initialising off the main thread.
        Thread {
            MobileAds.initialize(appContext) {
                main.post {
                    loadInterstitial()
                    loadRewarded()
                }
            }
        }.start()
    }

    fun request(): AdRequest = AdRequest.Builder().build()

    // ---- interstitial -------------------------------------------------------------------

    private val ready get() = ::appContext.isInitialized

    private fun loadInterstitial() {
        if (!ready || interstitial != null || loadingInterstitial) return
        loadingInterstitial = true
        InterstitialAd.load(appContext, interstitialId, request(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                loadingInterstitial = false
                interstitial = ad
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                loadingInterstitial = false
                main.postDelayed({ loadInterstitial() }, FULLSCREEN_RETRY_DELAY_MS)
            }
        })
    }

    /**
     * Shows an interstitial at a break between levels if one is ready and the 30 second cap
     * allows it. [then] always runs afterwards (straight away when no ad is shown).
     */
    fun maybeShowInterstitial(activity: Activity?, then: () -> Unit) {
        val ad = interstitial
        val now = SystemClock.elapsedRealtime()
        if (activity == null || ad == null || showingFullScreen || now - lastInterstitialAt < INTERSTITIAL_GAP_MS) {
            if (ad == null) loadInterstitial()
            then()
            return
        }
        interstitial = null
        showingFullScreen = true
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                lastInterstitialAt = SystemClock.elapsedRealtime()
            }

            override fun onAdDismissedFullScreenContent() {
                showingFullScreen = false
                lastInterstitialAt = SystemClock.elapsedRealtime()
                loadInterstitial()
                then()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                showingFullScreen = false
                loadInterstitial()
                then()
            }
        }
        ad.show(activity)
    }

    // ---- rewarded -----------------------------------------------------------------------

    private fun loadRewarded() {
        if (!ready || rewarded != null || loadingRewarded) return
        loadingRewarded = true
        RewardedAd.load(appContext, rewardedId, request(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                loadingRewarded = false
                rewarded = ad
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                loadingRewarded = false
                main.postDelayed({ loadRewarded() }, FULLSCREEN_RETRY_DELAY_MS)
            }
        })
    }

    fun rewardedReady(): Boolean {
        if (rewarded == null) loadRewarded()
        return rewarded != null
    }

    /**
     * Shows a rewarded video the player chose to watch. [onReward] runs when the reward is
     * earned, [onClosed] when the ad goes away (or could not be shown).
     */
    fun showRewarded(activity: Activity?, onReward: () -> Unit, onClosed: () -> Unit) {
        val ad = rewarded
        if (activity == null || ad == null || showingFullScreen) {
            loadRewarded()
            onClosed()
            return
        }
        rewarded = null
        showingFullScreen = true
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                showingFullScreen = false
                // A rewarded ad also counts towards the interstitial cap.
                lastInterstitialAt = SystemClock.elapsedRealtime()
                loadRewarded()
                onClosed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                showingFullScreen = false
                loadRewarded()
                onClosed()
            }
        }
        ad.show(activity) { onReward() }
    }
}

/**
 * A banner that keeps trying: the first two requests go out straight away, after that it waits
 * [AdsManager.BANNER_RETRY_DELAY_MS] (15 s) before each new request.
 */
private class BannerController(context: Context, size: AdSize, unitId: String) {
    val view = AdView(context)
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private var failures = 0
    private var destroyed = false
    private val retry = Runnable { load() }

    init {
        view.adUnitId = unitId
        view.setAdSize(size)
        view.adListener = object : AdListener() {
            override fun onAdLoaded() {
                failures = 0
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                if (destroyed) return
                failures++
                if (failures < 2) load() else handler.postDelayed(retry, AdsManager.BANNER_RETRY_DELAY_MS)
            }
        }
    }

    fun load() {
        if (!destroyed) view.loadAd(AdsManager.request())
    }

    fun destroy() {
        destroyed = true
        handler.removeCallbacks(retry)
        view.destroy()
    }
}

/** Anchored adaptive banner. Its space is reserved up front so the game never jumps. */
@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val context = LocalContext.current
        val widthDp = maxWidth.value.toInt().coerceAtLeast(320)
        val size = remember(widthDp) {
            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
        }
        val controller = remember(size) { BannerController(context, size, AdsManager.bannerId) }
        DisposableEffect(controller) {
            controller.load()
            onDispose { controller.destroy() }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(size.height.dp),
            contentAlignment = Alignment.Center,
        ) {
            AndroidView(factory = { controller.view })
        }
    }
}
