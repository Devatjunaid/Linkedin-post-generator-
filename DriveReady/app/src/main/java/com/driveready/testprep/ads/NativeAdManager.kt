package com.driveready.testprep.ads

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.driveready.testprep.BuildConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Keeps a small pool of preloaded native ads. Screens take one when they have a free slot
 * between content blocks; the slot stays collapsed (no empty box) until an ad is available.
 */
object NativeAdManager {
    private const val TAG = "NativeAds"
    private const val POOL_SIZE = 2
    private const val RETRY_DELAY_MS = 30_000L

    private val pool = ArrayDeque<NativeAd>()
    private val _available = MutableStateFlow(0)
    val available: StateFlow<Int> = _available.asStateFlow()

    private var loading = false
    private val handler = Handler(Looper.getMainLooper())

    fun preload(context: Context) {
        if (!AdsManager.ready.value || loading || pool.size >= POOL_SIZE) return
        val appContext = context.applicationContext
        loading = true
        val loader = AdLoader.Builder(appContext, BuildConfig.NATIVE_AD_UNIT_ID)
            .forNativeAd { ad ->
                pool.addLast(ad)
                _available.value = pool.size
            }
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setVideoOptions(VideoOptions.Builder().setStartMuted(true).build())
                    .build()
            )
            .withAdListener(object : AdListener() {
                override fun onAdLoaded() {
                    loading = false
                    if (pool.size < POOL_SIZE) preload(appContext)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.d(TAG, "Load failed: ${error.code} ${error.message}")
                    loading = false
                    handler.postDelayed({ preload(appContext) }, RETRY_DELAY_MS)
                }
            })
            .build()
        loader.loadAd(AdRequest.Builder().build())
    }

    /** Takes a ready ad from the pool (caller owns it and must destroy it) and refills the pool. */
    fun take(context: Context): NativeAd? {
        val ad = pool.removeFirstOrNull()
        _available.value = pool.size
        preload(context)
        return ad
    }
}
