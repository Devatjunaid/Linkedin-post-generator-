package com.superdevlabs.coldplunge

import com.superdevlabs.coldplunge.ads.BannerRetryPolicy
import com.superdevlabs.coldplunge.ads.FrequencyCap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdPoliciesTest {

    @Test
    fun bannerRetriesImmediatelyOnceThenEvery15Seconds() {
        val p = BannerRetryPolicy()
        assertEquals(0L, p.onFailed())      // 1st request failed -> 2nd goes out now
        assertEquals(15_000L, p.onFailed()) // 2nd failed -> wait 15 s
        assertEquals(15_000L, p.onFailed()) // keeps waiting 15 s between requests
        p.onLoaded()
        assertEquals(0L, p.onFailed())      // counter resets after a successful load
    }

    @Test
    fun interstitialCapIsOnePer30Seconds() {
        var now = 1_000L
        val cap = FrequencyCap(30_000L) { now }
        assertTrue(cap.canShow())
        cap.markShown()
        now += 29_999
        assertFalse(cap.canShow())
        assertEquals(1L, cap.msUntilAllowed())
        now += 1
        assertTrue(cap.canShow())
    }
}
