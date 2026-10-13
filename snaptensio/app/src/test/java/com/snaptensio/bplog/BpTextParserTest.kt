package com.snaptensio.bplog

import com.snaptensio.bplog.ocr.BpResult
import com.snaptensio.bplog.ocr.BpTextParser
import com.snaptensio.bplog.ocr.ConsensusTracker
import com.snaptensio.bplog.ocr.NumberToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BpTextParserTest {
    private fun tok(raw: String, top: Int, height: Int) = NumberToken(raw, top, top + height, 0, 100)

    @Test fun readsTypicalMonitor() {
        val r = BpTextParser.parse(listOf(tok("128", 10, 80), tok("82", 100, 70), tok("72", 190, 40)))
        assertEquals(BpResult(128, 82, 72), r)
    }

    @Test fun ignoresClockAndSmallPrint() {
        val r = BpTextParser.parse(
            listOf(tok("12:45", 0, 15), tok("M3", 0, 12), tok("135", 30, 90), tok("88", 130, 80), tok("64", 220, 45), tok("10", 280, 12)),
        )
        assertEquals(BpResult(135, 88, 64), r)
    }

    @Test fun fixesSevenSegmentConfusions() {
        assertEquals(118, BpTextParser.normalize("lI8"))
        assertEquals(80, BpTextParser.normalize("8O"))
        assertNull(BpTextParser.normalize("12:30"))
        assertNull(BpTextParser.normalize("7"))
        assertNull(BpTextParser.normalize("1234"))
    }

    @Test fun rejectsImplausiblePairs() {
        assertNull(BpTextParser.parse(listOf(tok("82", 10, 80), tok("128", 100, 80))))
        assertNull(BpTextParser.parse(listOf(tok("99", 10, 80), tok("95", 100, 80))))
    }

    @Test fun worksWithoutPulse() {
        assertEquals(BpResult(142, 91, null), BpTextParser.parse(listOf(tok("142", 10, 80), tok("91", 100, 75))))
    }

    @Test fun consensusNeedsAgreement() {
        val c = ConsensusTracker(window = 6, required = 3)
        assertNull(c.add(BpResult(128, 82, 72)))
        assertNull(c.add(BpResult(126, 82, 72)))
        assertNull(c.add(BpResult(128, 82, 71)))
        assertEquals(BpResult(128, 82, 72), c.add(BpResult(128, 82, 72)))
    }
}
