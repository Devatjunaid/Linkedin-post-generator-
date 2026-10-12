package app.gigometer.mileage.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Google UMP consent (required for users in the EEA/UK/Switzerland and US states with
 * privacy laws) followed by Mobile Ads SDK initialisation. Ads are requested only once
 * [ready] is true.
 */
object Ads {
    private const val TAG = "Ads"
    private val started = AtomicBoolean(false)
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    private lateinit var consent: ConsentInformation

    fun gatherConsentAndInit(activity: Activity) {
        consent = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().setTagForUnderAgeOfConsent(false).build()
        consent.requestConsentInfoUpdate(
            activity, params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                    if (error != null) Log.w(TAG, "consent form: ${error.message}")
                    updatePrivacyFlag()
                    if (consent.canRequestAds()) initialize(activity)
                }
            },
            { error ->
                Log.w(TAG, "consent update: ${error.message}")
                if (consent.canRequestAds()) initialize(activity)
            },
        )
        // Consent from a previous session lets us start loading ads right away.
        if (consent.canRequestAds()) initialize(activity)
    }

    private fun updatePrivacyFlag() {
        _privacyOptionsRequired.value =
            consent.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            if (error != null) Log.w(TAG, "privacy options: ${error.message}")
        }
    }

    private fun initialize(context: Context) {
        if (!started.compareAndSet(false, true)) return
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(context.applicationContext) { _ready.value = true }
        }
    }
}
