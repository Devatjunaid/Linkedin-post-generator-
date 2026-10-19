package com.devatjunaid.isthisai.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The sample images were produced with the open-source c2pa-python SDK (signed with throwaway
 * test certificates), Pillow and piexif, so they carry the same structures real tools write.
 */
class AnalyzerTest {

    private fun scan(name: String): ScanReport {
        val bytes = javaClass.getResourceAsStream("/samples/$name")!!.use { it.readBytes() }
        return Analyzer.analyze(bytes, name, null, now = 0L)
    }

    private fun ScanReport.rawValue(section: String, key: String): String? =
        raw.firstOrNull { it.title.startsWith(section) }?.entries?.firstOrNull { it.first == key }?.second

    @Test fun c2paJpegEs256_isAiGenerated_andSignatureAndHashPass() {
        val r = scan("c2pa_ai_es256.jpg")
        assertEquals(Verdict.AI_GENERATED, r.verdict)
        assertEquals(Strength.SIGNED, r.strength)
        assertEquals("Signature matches the certificate", r.rawValue("Content Credentials", "Signature"))
        assertEquals("Image pixels and data match the signed hash", r.rawValue("Content Credentials", "Image hash"))
        assertEquals("Example Generator Inc", r.rawValue("Content Credentials", "Signed by"))
        assertEquals("Example Image Model 3", r.aiTool)
        assertTrue(r.warnings.isEmpty())
    }

    @Test fun c2paPngPs256_isAiGenerated_signed() {
        val r = scan("c2pa_ai_ps256.png")
        assertEquals(Verdict.AI_GENERATED, r.verdict)
        assertEquals(Strength.SIGNED, r.strength)
        assertEquals("PS256", r.rawValue("Content Credentials", "Algorithm"))
    }

    @Test fun c2paWebp_isAiGenerated_signed() {
        val r = scan("c2pa_ai.webp")
        assertEquals(Verdict.AI_GENERATED, r.verdict)
        assertEquals(Strength.SIGNED, r.strength)
    }

    @Test fun c2paCameraEd25519_isCamera_signed() {
        val r = scan("c2pa_camera_ed25519.jpg")
        assertEquals(Verdict.CAMERA, r.verdict)
        assertEquals(Strength.SIGNED, r.strength)
    }

    @Test fun c2paEditedWithAi_isAiEdited() {
        val r = scan("c2pa_ai_edited.jpg")
        assertEquals(Verdict.AI_EDITED, r.verdict)
        assertTrue(r.trail.any { it.title == "Edited with generative AI" })
    }

    @Test fun tamperedPixels_areDetected() {
        val r = scan("c2pa_tampered.jpg")
        assertEquals("Image bytes changed after signing", r.rawValue("Content Credentials", "Image hash"))
        assertTrue(r.warnings.any { it.contains("changed after") })
        assertEquals(Strength.EMBEDDED, r.strength)
    }

    @Test fun forgedClaim_failsSignatureCheck() {
        val r = scan("c2pa_forged_claim.jpg")
        assertEquals("Signature does not match: the label may have been altered", r.rawValue("Content Credentials", "Signature"))
        assertTrue(r.warnings.any { it.contains("tampered") })
        assertEquals(Strength.EMBEDDED, r.strength)
    }

    @Test fun a1111Png_isAiGenerated_withPrompt() {
        val r = scan("sd_a1111.png")
        assertEquals(Verdict.AI_GENERATED, r.verdict)
        assertEquals("Stable Diffusion WebUI", r.aiTool)
        assertEquals("a cat astronaut, highly detailed", r.aiPrompt)
    }

    @Test fun comfyPng_isAiGenerated() {
        val r = scan("comfy.png")
        assertEquals(Verdict.AI_GENERATED, r.verdict)
        assertEquals("ComfyUI", r.aiTool)
        assertEquals("neon city at night", r.aiPrompt)
    }

    @Test fun xmpDigitalSourceType_isAiGenerated() {
        val r = scan("xmp_ai.jpg")
        assertEquals(Verdict.AI_GENERATED, r.verdict)
        assertEquals(Strength.EMBEDDED, r.strength)
    }

    @Test fun cameraExif_isCameraHint_andGpsHidden() {
        val r = scan("camera_exif.jpg")
        assertEquals(Verdict.CAMERA, r.verdict)
        assertEquals(Strength.HINT, r.strength)
        assertEquals("Canon", r.rawValue("EXIF", "Make"))
        assertEquals("1/250s", r.rawValue("EXIF", "ExposureTime"))
        assertEquals("f/2.8", r.rawValue("EXIF", "FNumber"))
        assertEquals("Present (not shown)", r.rawValue("EXIF", "GPS location"))
    }

    @Test fun plainJpeg_hasNoLabel() {
        val r = scan("plain.jpg")
        assertEquals(Verdict.NO_LABEL, r.verdict)
        assertEquals(64, r.width)
        assertEquals(48, r.height)
    }

    @Test fun garbageInput_doesNotCrash() {
        val r = Analyzer.analyze(ByteArray(300) { (it * 37).toByte() }, "x.bin", null)
        assertEquals(Verdict.NO_LABEL, r.verdict)
        val truncated = javaClass.getResourceAsStream("/samples/c2pa_ai_es256.jpg")!!.use { it.readBytes() }
        for (cut in listOf(10, 200, 1000, truncated.size / 2)) {
            Analyzer.analyze(truncated.copyOf(cut), "cut.jpg", null)
        }
    }

    @Test fun reportJson_roundTrips() {
        val r = scan("c2pa_ai_edited.jpg")
        val back = ReportJson.decode(ReportJson.encode(r))
        assertEquals(r, back)
        assertNotNull(ReportJson.encode(r, pretty = true))
    }

    @Test fun toolNameMatching_respectsWordBoundaries() {
        assertEquals("Midjourney", KnownTools.match("Midjourney v7"))
        assertEquals(null, KnownTools.match("Fireflyer camera app"))
        assertEquals(null, KnownTools.match("Photoshop 26.0"))
    }
}
