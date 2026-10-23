package com.walkies.dogwalk.ads

import com.walkies.dogwalk.BuildConfig

/** Debug builds use Google's test units; release builds use the live Walkies units. */
object AdUnits {
    val banner: String = BuildConfig.ADMOB_BANNER_ID
    val interstitial: String = BuildConfig.ADMOB_INTERSTITIAL_ID
    val native: String = BuildConfig.ADMOB_NATIVE_ID
}
