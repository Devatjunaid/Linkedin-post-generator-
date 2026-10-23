package com.driveready.testprep.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
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

/**
 * Owns consent (UMP) and Mobile Ads SDK initialisation. Every ad surface waits on [ready]
 * so no request is made before consent has been gathered.
 */
object AdsManager {
    private const val TAG = "AdsManager"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val initCalled = AtomicBoolean(false)
    private val _ready = MutableStateFlow(false)

    /** True once consent allows ad requests and the SDK has been initialised. */
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private lateinit var consentInformation: ConsentInformation

    /**
     * Gathers consent (showing the UMP form if required) and initialises the SDK.
     * [onFinished] is always called exactly once, so the splash screen never hangs.
     */
    fun gatherConsentAndInit(activity: Activity, onFinished: () -> Unit) {
        consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()
        val finished = AtomicBoolean(false)
        val finishOnce = { if (!finished.getAndSet(true)) onFinished() }

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) Log.w(TAG, "Consent form: ${formError.errorCode} ${formError.message}")
                    if (consentInformation.canRequestAds()) initializeSdk(activity.applicationContext)
                    finishOnce()
                }
            },
            { requestError ->
                Log.w(TAG, "Consent update failed: ${requestError.errorCode} ${requestError.message}")
                if (consentInformation.canRequestAds()) initializeSdk(activity.applicationContext)
                finishOnce()
            }
        )

        // Consent from a previous session lets ads start loading immediately.
        if (consentInformation.canRequestAds()) initializeSdk(activity.applicationContext)
    }

    val isPrivacyOptionsRequired: Boolean
        get() = ::consentInformation.isInitialized &&
            consentInformation.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            if (error != null) Log.w(TAG, "Privacy options: ${error.message}")
        }
    }

    private fun initializeSdk(context: Context) {
        if (initCalled.getAndSet(true)) return
        // Learner drivers can be teenagers: keep ads suitable for teens.
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_T)
                .build()
        )
        scope.launch {
            withContext(Dispatchers.IO) { MobileAds.initialize(context) {} }
            _ready.value = true
            InterstitialAdManager.preload(context)
            NativeAdManager.preload(context)
        }
    }
}
