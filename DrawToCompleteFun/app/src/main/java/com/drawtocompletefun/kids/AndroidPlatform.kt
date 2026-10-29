package com.drawtocompletefun.kids

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import com.drawtocompletefun.kids.ads.AdsManager
import com.drawtocompletefun.kids.ads.BannerAd
import com.drawtocompletefun.kids.audio.SoundPlayer
import com.drawtocompletefun.kids.ui.GamePlatform
import com.drawtocompletefun.kids.ui.RewardResult
import com.drawtocompletefun.kids.ui.Sfx

class AndroidPlatform(
    private val activity: ComponentActivity,
    private val sounds: SoundPlayer,
    private val ads: AdsManager,
) : GamePlatform {

    override fun play(sfx: Sfx) = sounds.play(sfx)

    override fun setMusicEnabled(enabled: Boolean) = sounds.setMusic(enabled)

    override fun canShowInterstitial(): Boolean = ads.canShowInterstitial()

    override fun showInterstitial(onDone: () -> Unit) = ads.showInterstitial(onDone)

    override fun showRewarded(onResult: (RewardResult) -> Unit) = ads.showRewarded(onResult)

    /**
     * "Rate us" is a button, so it opens the store page (Google asks apps not to put the
     * in-app review sheet behind a call-to-action button). A parental gate comes first.
     */
    override fun requestReview() {
        val id = activity.packageName
        open("market://details?id=$id", fallback = "https://play.google.com/store/apps/details?id=$id")
    }

    override fun openPrivacyPolicy() = open(activity.getString(R.string.privacy_policy_url))

    override fun exitApp() = activity.finish()

    @Composable
    override fun Banner(modifier: Modifier) = BannerAd(ads, modifier)

    private fun open(url: String, fallback: String? = null) {
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (e: ActivityNotFoundException) {
            if (fallback != null) {
                try {
                    activity.startActivity(Intent(Intent.ACTION_VIEW, fallback.toUri()))
                } catch (e: ActivityNotFoundException) {
                    // No browser at all: nothing sensible to do.
                }
            }
        }
    }
}
