package com.devatjunaid.renewalert.ads

import android.app.Activity
import android.app.Application
import android.os.SystemClock
import android.util.Log
import com.devatjunaid.renewalert.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/** Where an interstitial may appear. Each is a natural pause after the user finished something. */
enum class AdPlacement {
    SUBSCRIPTION_SAVED,
    SUBSCRIPTION_CANCELLED,
    SUBSCRIPTION_DELETED,
    TRIAL_DECIDED,
    OPEN_INSIGHTS,
    OPEN_CALENDAR,
    OPEN_TRIALS,
    BACKUP_DONE,
    EXPORT_DONE,
}

/**
 * Consent (UMP) + Mobile Ads SDK + interstitial pacing.
 *
 * Policy guard rails built in:
 *  - Nothing is requested until Google's consent flow says ads may be requested.
 *  - Interstitials only at transitions listed in [AdPlacement], never on launch, back/exit or mid-task.
 *  - At most one interstitial every [COOLDOWN_MS], none in the first [LAUNCH_GRACE_MS] of a session,
 *    and at most [MAX_PER_SESSION] per session.
 */
class AdsManager(private val app: Application) {
    private val consent: ConsentInformation = UserMessagingPlatform.getConsentInformation(app)
    private val initStarted = AtomicBoolean(false)
    private val _ready = MutableStateFlow(false)

    /** True once ads may be requested and the SDK is initialised. */
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private var interstitial: InterstitialAd? = null
    private var loadingInterstitial = false
    private var lastShownAt = 0L
    private var shownThisSession = 0
    private val sessionStart = SystemClock.elapsedRealtime()

    /** True while an interstitial covers the app (so app lock doesn't treat it as leaving). */
    @Volatile var isShowingFullScreen: Boolean = false
        private set

    /** Users with app lock or an open full-screen flow can suppress interstitials. */
    @Volatile var suppressed: Boolean = false

    val privacyOptionsRequired: Boolean
        get() = consent.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /** Call once per launch from the splash. [onDone] runs whether or not consent was given. */
    var consentStarted: Boolean = false
        private set

    fun gatherConsent(activity: Activity, onDone: () -> Unit) {
        consentStarted = true
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()
        var finished = false
        val finish = {
            if (!finished) {
                finished = true
                if (consent.canRequestAds()) initialize()
                onDone()
            }
        }
        consent.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                    if (error != null) Log.w(TAG, "Consent form: ${error.message}")
                    finish()
                }
            },
            { error ->
                Log.w(TAG, "Consent info: ${error.message}")
                finish()
            },
        )
        // Consent from a previous session lets ads start loading in parallel.
        if (consent.canRequestAds()) initialize()
    }

    fun showPrivacyOptions(activity: Activity, onDone: () -> Unit) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { onDone() }
    }

    private fun initialize() {
        if (!initStarted.compareAndSet(false, true)) return
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_PG)
                .build(),
        )
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(app) {}
            withContext(Dispatchers.Main) {
                _ready.value = true
                preloadInterstitial()
            }
        }
    }

    fun newRequest(): AdRequest = AdRequest.Builder().build()

    private fun preloadInterstitial() {
        if (!_ready.value || interstitial != null || loadingInterstitial) return
        loadingInterstitial = true
        InterstitialAd.load(
            app, BuildConfig.ADMOB_INTERSTITIAL_ID, newRequest(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                    loadingInterstitial = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitial = null
                    loadingInterstitial = false
                    Log.d(TAG, "Interstitial failed: ${error.message}")
                }
            },
        )
    }

    /**
     * Shows an interstitial if pacing allows, then runs [onContinue] (after the ad closes,
     * or immediately when no ad is shown). The user's action always completes.
     */
    fun showInterstitial(activity: Activity, placement: AdPlacement, onContinue: () -> Unit = {}) {
        val now = SystemClock.elapsedRealtime()
        val ad = interstitial
        val allowed = _ready.value && !suppressed &&
            now - sessionStart >= LAUNCH_GRACE_MS &&
            now - lastShownAt >= COOLDOWN_MS &&
            shownThisSession < MAX_PER_SESSION
        if (ad == null || !allowed) {
            if (ad == null) preloadInterstitial()
            onContinue()
            return
        }
        Log.d(TAG, "Interstitial at $placement")
        isShowingFullScreen = true
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                isShowingFullScreen = true
                lastShownAt = SystemClock.elapsedRealtime()
                shownThisSession++
            }

            override fun onAdDismissedFullScreenContent() {
                isShowingFullScreen = false
                interstitial = null
                lastShownAt = SystemClock.elapsedRealtime()
                preloadInterstitial()
                onContinue()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                isShowingFullScreen = false
                interstitial = null
                preloadInterstitial()
                onContinue()
            }
        }
        ad.show(activity)
    }

    companion object {
        private const val TAG = "RenewAlertAds"
        const val COOLDOWN_MS = 90_000L
        const val LAUNCH_GRACE_MS = 45_000L
        const val MAX_PER_SESSION = 6
    }
}
