package com.drawtocompletefun.kids.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

enum class Sfx { Tap, Pop, Oops, Star1, Star2, Star3, Fanfare, Unlock, Whoosh }

enum class RewardResult { Earned, Closed, Unavailable }

/**
 * Everything the UI needs from the device: sounds, ads, store review and app exit.
 * The Android implementation lives in MainActivity / AndroidPlatform.
 */
interface GamePlatform {
    fun play(sfx: Sfx)

    fun setMusicEnabled(enabled: Boolean)

    /** Shows an interstitial if one is ready and the frequency cap allows it, then calls [onDone]. */
    fun showInterstitial(onDone: () -> Unit)

    /** Loads (if needed) and shows a rewarded ad. Always calls [onResult] exactly once. */
    fun showRewarded(onResult: (RewardResult) -> Unit)

    fun requestReview()

    fun openPrivacyPolicy()

    fun exitApp()

    @Composable
    fun Banner(modifier: Modifier)
}
