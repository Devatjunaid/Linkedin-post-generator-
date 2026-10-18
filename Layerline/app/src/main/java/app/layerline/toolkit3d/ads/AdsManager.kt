package app.layerline.toolkit3d.ads

import android.app.Activity
import android.app.Application
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import app.layerline.toolkit3d.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/** Debug builds use Google's test units (set in app/build.gradle.kts), release builds the live ones. */
object AdUnits {
    val banner: String get() = BuildConfig.AD_BANNER_ID
    val interstitial: String get() = BuildConfig.AD_INTERSTITIAL_ID
    val native: String get() = BuildConfig.AD_NATIVE_ID
}

/**
 * Owns consent and every ad format.
 *
 * Policy notes:
 *  - Nothing is requested until the UMP consent flow says ads may be requested.
 *  - Interstitials only appear right after the user finishes a major action (a natural break),
 *    never at launch, on exit, or in the middle of a task, and at most once every 30 seconds.
 */
class AdsManager(private val app: Application) {

    private val consentInformation: ConsentInformation = UserMessagingPlatform.getConsentInformation(app)
    private val initStarted = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _ready = MutableStateFlow(false)
    /** True once consent allows ads and the SDK has been initialised. */
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    val natives = NativeAdStore(app, ready)

    private val cap = FrequencyCap(intervalMs = 30_000L, startedAt = SystemClock.elapsedRealtime())
    private var interstitial: InterstitialAd? = null
    private var interstitialLoading = false
    private var interstitialShowing = false

    // ---- Consent ------------------------------------------------------------------------------

    fun gatherConsent(activity: Activity) {
        val params = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ ->
                    refreshPrivacyState()
                    if (consentInformation.canRequestAds()) initialize()
                }
            },
            { _ ->
                refreshPrivacyState()
                if (consentInformation.canRequestAds()) initialize()
            },
        )
        // Consent given in an earlier session lets ads start loading while the update runs.
        if (consentInformation.canRequestAds()) initialize()
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { _ -> refreshPrivacyState() }
    }

    private fun refreshPrivacyState() {
        _privacyOptionsRequired.value = consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    private fun initialize() {
        if (!initStarted.compareAndSet(false, true)) return
        scope.launch(Dispatchers.IO) {
            MobileAds.initialize(app) {
                scope.launch {
                    _ready.value = true
                    loadInterstitial()
                }
            }
        }
    }

    // ---- Interstitial -------------------------------------------------------------------------

    private fun loadInterstitial() {
        if (!_ready.value || interstitial != null || interstitialLoading) return
        interstitialLoading = true
        InterstitialAd.load(
            app,
            AdUnits.interstitial,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                    interstitialLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialLoading = false
                    scope.launch {
                        delay(30_000L)
                        loadInterstitial()
                    }
                }
            },
        )
    }

    /**
     * Call after a major action has completed (quote saved, print logged, spool added…).
     * Shows an interstitial only if one is ready, the screen is in the foreground, and at least
     * 30 seconds have passed since the last one closed. Otherwise does nothing.
     */
    fun showInterstitialAtBreak(activity: Activity?) {
        if (activity == null || !_ready.value) return
        val ad = interstitial
        if (ad == null) {
            loadInterstitial()
            return
        }
        val now = SystemClock.elapsedRealtime()
        if (interstitialShowing || !cap.canShow(now)) return
        val resumed = (activity as? ComponentActivity)?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) ?: true
        if (!resumed || activity.isFinishing) return

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialShowing = false
                cap.onClosed(SystemClock.elapsedRealtime())
                loadInterstitial()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialShowing = false
                loadInterstitial()
            }
        }
        interstitial = null
        interstitialShowing = true
        ad.show(activity)
    }
}
