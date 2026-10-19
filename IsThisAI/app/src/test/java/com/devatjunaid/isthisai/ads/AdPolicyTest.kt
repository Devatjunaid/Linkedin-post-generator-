package com.devatjunaid.isthisai.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdPolicyTest {

    @Test fun banner_firstTwoCallsAreImmediate_thenWaits15Seconds() {
        // Call 1 fails -> call 2 goes out immediately.
        assertEquals(0L, AdPolicy.bannerDelayAfterFailure(1))
        // Call 2 fails -> wait 15 s before call 3, and before every call after that.
        assertEquals(15_000L, AdPolicy.bannerDelayAfterFailure(2))
        assertEquals(15_000L, AdPolicy.bannerDelayAfterFailure(3))
        assertEquals(15_000L, AdPolicy.bannerDelayAfterFailure(10))
    }

    @Test fun interstitial_atMostOnePer30Seconds() {
        val cap = FrequencyCap(AdPolicy.INTERSTITIAL_MIN_INTERVAL_MS)
        assertTrue(cap.canShow(1_000))
        cap.markShown(1_000)
        assertFalse(cap.canShow(1_001))
        assertFalse(cap.canShow(30_999))
        assertTrue(cap.canShow(31_000))
    }

    @Test fun preloadBackoff_isBounded() {
        assertEquals(15_000L, AdPolicy.preloadRetryDelay(1))
        assertEquals(30_000L, AdPolicy.preloadRetryDelay(2))
        assertEquals(60_000L, AdPolicy.preloadRetryDelay(9))
    }
}
