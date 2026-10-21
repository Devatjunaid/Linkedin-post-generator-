package com.drawtocompletefun.kids.ads

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * One banner and its retry rule: the first two requests go out straight away; once two
 * requests in a row have failed, every further request waits 15 seconds.
 */
class BannerController(context: Context, unitId: String, widthDp: Int) {

    val size: AdSize = AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, widthDp)
    val view: AdView = AdView(context)

    private val handler = Handler(Looper.getMainLooper())
    private var failedInARow = 0
    private var destroyed = false
    private val retry = Runnable { load() }

    init {
        view.adUnitId = unitId
        view.setAdSize(size)
        view.adListener = object : AdListener() {
            override fun onAdLoaded() {
                failedInARow = 0
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                if (destroyed) return
                failedInARow++
                if (failedInARow < MAX_QUICK_TRIES) {
                    handler.post(retry)
                } else {
                    handler.postDelayed(retry, RETRY_DELAY_MS)
                }
            }
        }
    }

    fun load() {
        if (!destroyed) view.loadAd(AdRequest.Builder().build())
    }

    fun pause() = view.pause()

    fun resume() = view.resume()

    fun destroy() {
        destroyed = true
        handler.removeCallbacks(retry)
        view.destroy()
    }

    companion object {
        const val MAX_QUICK_TRIES = 2
        const val RETRY_DELAY_MS = 15_000L
    }
}

@Composable
fun BannerAd(ads: AdsManager, modifier: Modifier = Modifier) {
    if (!ads.ready.value) return
    val context = LocalContext.current
    BoxWithConstraints(modifier) {
        val widthDp = maxWidth.value.toInt()
        val banner = remember(widthDp) {
            BannerController(context, AdsManager.Ids.banner, widthDp).also { it.load() }
        }
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        DisposableEffect(banner, lifecycle) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> banner.pause()
                    Lifecycle.Event.ON_RESUME -> banner.resume()
                    else -> Unit
                }
            }
            lifecycle.addObserver(observer)
            onDispose {
                lifecycle.removeObserver(observer)
                banner.destroy()
            }
        }
        AndroidView(
            factory = { banner.view },
            modifier = Modifier
                .fillMaxWidth()
                .height(banner.size.height.dp),
        )
    }
}
