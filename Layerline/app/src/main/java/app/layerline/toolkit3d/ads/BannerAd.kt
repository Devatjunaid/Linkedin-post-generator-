package app.layerline.toolkit3d.ads

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.layerline.toolkit3d.ui.theme.LL
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * Anchored adaptive banner pinned to the bottom of the app.
 *
 * Load rule (see [BannerRetryPolicy]): if the first request fails a second one goes out right
 * away; once two calls in a row have failed, each further request waits 15 seconds. Requests
 * are held while the app is in the background. The slot keeps its height while loading so
 * content never jumps under the user's finger.
 */
@Composable
fun BannerAdSlot(ads: AdsManager, modifier: Modifier = Modifier) {
    val ready by ads.ready.collectAsStateWithLifecycle()
    if (!ready) return

    val context = LocalContext.current
    val widthDp = LocalConfiguration.current.screenWidthDp
    val adSize = remember(widthDp) { AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var loaded by remember { mutableStateOf(false) }

    val adView = remember(adSize) {
        AdView(context).apply {
            adUnitId = AdUnits.banner
            setAdSize(adSize)
        }
    }

    DisposableEffect(adView, lifecycleOwner) {
        val policy = BannerRetryPolicy()
        val handler = Handler(Looper.getMainLooper())
        var resumed = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        var pendingWhilePaused = false

        val request = object : Runnable {
            override fun run() {
                if (resumed) adView.loadAd(AdRequest.Builder().build()) else pendingWhilePaused = true
            }
        }

        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                policy.onLoaded()
                loaded = true
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                val wait = policy.onFailed()
                handler.removeCallbacks(request)
                handler.postDelayed(request, wait)
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    resumed = true
                    adView.resume()
                    if (pendingWhilePaused) {
                        pendingWhilePaused = false
                        request.run()
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    resumed = false
                    adView.pause()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        request.run()

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            handler.removeCallbacks(request)
            adView.destroy()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(adSize.height.dp)
            .background(LL.Nav),
        contentAlignment = Alignment.Center,
    ) {
        key(adView) {
            AndroidView(
                factory = { adView },
                modifier = Modifier.fillMaxWidth(),
                update = { it.visibility = if (loaded) android.view.View.VISIBLE else android.view.View.INVISIBLE },
            )
        }
    }
}
