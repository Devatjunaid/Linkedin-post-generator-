package com.driveready.testprep

import com.driveready.testprep.ads.AdPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdPolicyTest {
    @Test
    fun bannerRetriesImmediatelyAfterFirstFailure() {
        assertEquals(0L, AdPolicy.bannerRetryDelayMs(consecutiveFailures = 1))
    }

    @Test
    fun bannerWaits15SecondsAfterTwoFailures() {
        assertEquals(15_000L, AdPolicy.bannerRetryDelayMs(consecutiveFailures = 2))
        assertEquals(15_000L, AdPolicy.bannerRetryDelayMs(consecutiveFailures = 5))
    }

    @Test
    fun interstitialCappedAtOnePer30Seconds() {
        assertTrue(AdPolicy.canShowInterstitial(nowMs = 1_000, lastShownMs = 0))
        assertFalse(AdPolicy.canShowInterstitial(nowMs = 40_000, lastShownMs = 20_000))
        assertTrue(AdPolicy.canShowInterstitial(nowMs = 50_000, lastShownMs = 20_000))
    }
}
