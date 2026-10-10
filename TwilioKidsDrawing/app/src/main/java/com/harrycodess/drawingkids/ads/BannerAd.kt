package com.harrycodess.drawingkids.ads

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/** Anchored adaptive banner that sizes itself to the screen width. */
@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    if (LocalInspectionMode.current) return
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val widthDp = maxWidth.value.toInt().coerceAtLeast(320)
        val context = LocalContext.current
        key(widthDp) {
            val adSize = remember { AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp) }
            val adView = remember {
                AdView(context).apply {
                    setAdSize(adSize)
                    adUnitId = AdUnits.BANNER
                    loadAd(Ads.request())
                }
            }
            DisposableEffect(adView) { onDispose { adView.destroy() } }
            AndroidView(
                factory = { adView },
                modifier = Modifier.fillMaxWidth().height(adSize.height.dp),
            )
        }
    }
}
