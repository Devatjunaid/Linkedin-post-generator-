package com.snaptensio.bplog

import com.snaptensio.bplog.data.Guideline
import com.snaptensio.bplog.domain.BpClassifier
import com.snaptensio.bplog.domain.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BpClassifierTest {
    private fun aha(s: Int, d: Int) = BpClassifier.classify(s, d, Guideline.AHA)
    private fun esc(s: Int, d: Int) = BpClassifier.classify(s, d, Guideline.ESC)

    @Test fun ahaCategories() {
        assertEquals("Normal", aha(118, 76).label)
        assertEquals("Elevated", aha(125, 78).label)
        assertEquals("Stage 1", aha(125, 82).label)
        assertEquals("Stage 1", aha(134, 70).label)
        assertEquals("Stage 2", aha(141, 70).label)
        assertEquals(Severity.CRISIS, aha(181, 100).severity)
        assertEquals(Severity.CRISIS, aha(150, 121).severity)
        assertEquals(Severity.STAGE2, aha(180, 120).severity)
        assertEquals("Low", aha(88, 58).label)
    }

    @Test fun escCategories() {
        assertEquals("Optimal", esc(115, 75).label)
        assertEquals("Normal", esc(125, 82).label)
        assertEquals("High normal", esc(135, 86).label)
        assertEquals("Grade 1", esc(145, 92).label)
        assertEquals("Grade 2", esc(165, 95).label)
        assertEquals("Grade 3", esc(180, 100).label)
    }

    @Test fun validation() {
        assertNull(BpClassifier.validate(120, 80, 70))
        assertNotNull(BpClassifier.validate(80, 120, 70))
        assertNotNull(BpClassifier.validate(120, null, 70))
        assertNotNull(BpClassifier.validate(300, 80, null))
    }
}
