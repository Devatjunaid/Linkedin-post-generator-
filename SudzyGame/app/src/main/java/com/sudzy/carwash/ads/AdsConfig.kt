package com.sudzy.carwash.ads

/**
 * Ad unit IDs. These are Google's official TEST IDs — they always serve test ads and are safe
 * during development. Replace them with your own IDs from the AdMob console before release
 * (and update ADMOB_APP_ID in app/build.gradle.kts).
 */
object AdsConfig {
    const val BANNER = "ca-app-pub-3940256099942544/9214589741"
    const val INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    const val APP_OPEN = "ca-app-pub-3940256099942544/9257395921"

    /** App open ads older than this are discarded (Google's recommended expiry). */
    const val APP_OPEN_EXPIRY_MS = 4 * 60 * 60 * 1000L
}
