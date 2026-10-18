package app.layerline.toolkit3d

import app.layerline.toolkit3d.ads.BannerRetryPolicy
import app.layerline.toolkit3d.ads.FrequencyCap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdPolicyTest {

    @Test
    fun bannerRetriesOnceImmediatelyThenEvery15Seconds() {
        val p = BannerRetryPolicy()
        assertEquals(0L, p.onFailed())      // call 1 failed -> call 2 right away
        assertEquals(15_000L, p.onFailed()) // call 2 failed -> wait 15 s
        assertEquals(15_000L, p.onFailed())
        p.onLoaded()
        assertEquals(0L, p.onFailed())
    }

    @Test
    fun interstitialCappedAtOnePer30Seconds() {
        val cap = FrequencyCap(30_000L, startedAt = 0L)
        assertFalse(cap.canShow(10_000L))
        assertTrue(cap.canShow(30_000L))
        cap.onClosed(40_000L)
        assertFalse(cap.canShow(69_999L))
        assertTrue(cap.canShow(70_000L))
        assertEquals(5_000L, FrequencyCap(30_000L, 0L).msUntilAllowed(25_000L))
    }
}
