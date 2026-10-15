package com.voltora.evcalculator.ads

import com.voltora.evcalculator.BuildConfig

/**
 * Ad unit ids. Debug builds always use Google's official TEST units so you can never
 * invalidate your AdMob account while developing. Put your real unit ids in the
 * RELEASE_* constants (and your app id in res/values/strings.xml) before publishing.
 */
object AdIds {
    private const val TEST_BANNER = "ca-app-pub-3940256099942544/9214589741"
    private const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_NATIVE = "ca-app-pub-3940256099942544/2247696110"

    // TODO: replace with your own ad unit ids from https://apps.admob.com
    private const val RELEASE_BANNER = TEST_BANNER
    private const val RELEASE_INTERSTITIAL = TEST_INTERSTITIAL
    private const val RELEASE_NATIVE = TEST_NATIVE

    val BANNER get() = if (BuildConfig.DEBUG) TEST_BANNER else RELEASE_BANNER
    val INTERSTITIAL get() = if (BuildConfig.DEBUG) TEST_INTERSTITIAL else RELEASE_INTERSTITIAL
    val NATIVE get() = if (BuildConfig.DEBUG) TEST_NATIVE else RELEASE_NATIVE
}
