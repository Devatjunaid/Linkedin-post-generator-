package com.voltora.evcalculator.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Consent (UMP) -> Mobile Ads SDK init -> ad loading.
 *
 * Interstitials are only shown at natural breaks (after choosing a car, after planning a
 * trip) and are frequency-capped so they never feel spammy.
 */
object AdsManager {
    private val initStarted = AtomicBoolean(false)
    private val _ready = MutableStateFlow(false)
    /** True once consent allows ad requests and the SDK is initialised. */
    val ready: StateFlow<Boolean> = _ready

    private var consentInfo: ConsentInformation? = null

    private var interstitial: InterstitialAd? = null
    private var loading = false
    private val appStart = SystemClock.elapsedRealtime()
    private var lastShown = 0L
    private var eligibleTriggers = 0

    private const val MIN_GAP_MS = 90_000L          // at most one interstitial every 90 s
    private const val GRACE_AFTER_START_MS = 30_000L // never in the first 30 s of a session
    private const val SHOW_EVERY_N_TRIGGERS = 2      // every 2nd natural break

    fun gatherConsentAndInit(activity: Activity) {
        val info = UserMessagingPlatform.getConsentInformation(activity)
        consentInfo = info
        val params = ConsentRequestParameters.Builder().build()
        info.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ ->
                    if (info.canRequestAds()) initSdk(activity.applicationContext)
                }
            },
            { _ -> if (info.canRequestAds()) initSdk(activity.applicationContext) },
        )
        // Consent obtained in a previous session: start right away.
        if (info.canRequestAds()) initSdk(activity.applicationContext)
    }

    val privacyOptionsRequired: Boolean
        get() = consentInfo?.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { }
    }

    private fun initSdk(context: Context) {
        if (!initStarted.compareAndSet(false, true)) return
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(context) {}
            withContext(Dispatchers.Main) {
                _ready.value = true
                loadInterstitial(context)
            }
        }
    }

    private fun loadInterstitial(context: Context) {
        if (!_ready.value || loading || interstitial != null) return
        loading = true
        InterstitialAd.load(
            context,
            AdIds.INTERSTITIAL,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                    loading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitial = null
                    loading = false
                }
            },
        )
    }

    /**
     * Call at a natural break. Runs [then] immediately if no ad is due, or after the ad is
     * dismissed. The app flow is never blocked by ads.
     */
    fun maybeShowInterstitial(activity: Activity?, then: () -> Unit) {
        val now = SystemClock.elapsedRealtime()
        val ad = interstitial
        eligibleTriggers++
        val due = activity != null && ad != null &&
            now - appStart > GRACE_AFTER_START_MS &&
            now - lastShown > MIN_GAP_MS &&
            eligibleTriggers % SHOW_EVERY_N_TRIGGERS == 0
        if (!due) {
            activity?.let { loadInterstitial(it.applicationContext) }
            then()
            return
        }
        val context = activity!!.applicationContext
        ad!!.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitial = null
                loadInterstitial(context)
                then()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitial = null
                loadInterstitial(context)
                then()
            }
        }
        lastShown = now
        interstitial = null
        ad.show(activity)
    }
}
