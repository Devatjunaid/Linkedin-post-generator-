package com.dailycash.tracker.ads

import android.app.Activity
import android.content.Context
import com.dailycash.tracker.BuildConfig
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

object AdIds {
    val banner: String get() = BuildConfig.AD_BANNER_ID
    val interstitial: String get() = BuildConfig.AD_INTERSTITIAL_ID
    val native: String get() = BuildConfig.AD_NATIVE_ID
}

/**
 * Google-certified consent (UMP) must be gathered before any ad request, as required for
 * users in the EEA, UK and Switzerland. Ads are only requested once [canRequestAds] is true.
 */
object AdsConsent {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val started = AtomicBoolean(false)

    private val _canRequestAds = MutableStateFlow(false)
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    fun gather(activity: Activity) {
        val info = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()
        info.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ ->
                    _privacyOptionsRequired.value = info.privacyOptionsRequirementStatus ==
                        ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
                    if (info.canRequestAds()) startAds(activity.applicationContext)
                }
            },
            { _ -> if (info.canRequestAds()) startAds(activity.applicationContext) },
        )
        // Consent from a previous session is enough to start right away.
        if (info.canRequestAds()) startAds(activity.applicationContext)
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { _ -> }
    }

    private fun startAds(context: Context) {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            withContext(Dispatchers.IO) { MobileAds.initialize(context) {} }
            _canRequestAds.value = true
            InterstitialAds.preload(context)
        }
    }
}
