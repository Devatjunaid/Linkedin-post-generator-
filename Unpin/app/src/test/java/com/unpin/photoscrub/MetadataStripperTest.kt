package com.unpin.photoscrub

import com.unpin.photoscrub.core.BannerRetryPolicy
import com.unpin.photoscrub.core.InterstitialCap
import com.unpin.photoscrub.core.MetadataStripper
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import javax.imageio.ImageIO

class MetadataStripperTest {

    private fun sampleImage(): BufferedImage {
        val img = BufferedImage(64, 48, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until 48) for (x in 0 until 64) img.setRGB(x, y, (x * 4 shl 16) or (y * 5 shl 8) or 0x40)
        return img
    }

    private fun encode(img: BufferedImage, format: String): ByteArray =
        ByteArrayOutputStream().also { ImageIO.write(img, format, it) }.toByteArray()

    private fun segment(marker: Int, payload: ByteArray): ByteArray {
        val len = payload.size + 2
        return byteArrayOf(0xFF.toByte(), marker.toByte(), (len shr 8).toByte(), len.toByte()) + payload
    }

    private fun pixels(bytes: ByteArray): IntArray {
        val img = ImageIO.read(ByteArrayInputStream(bytes))
        return img.getRGB(0, 0, img.width, img.height, null, 0, img.width)
    }

    private fun contains(hay: ByteArray, needle: String): Boolean =
        String(hay, Charsets.ISO_8859_1).contains(needle)

    @Test
    fun jpegLosesMetadataButKeepsPixels() {
        val clean = encode(sampleImage(), "jpg")
        val gps = segment(0xE1, "Exif\u0000\u0000MM\u0000*GPS-LAT-51.5".toByteArray(Charsets.ISO_8859_1))
        val xmp = segment(0xE1, "http://ns.adobe.com/xap/1.0/\u0000<x:xmpmeta>exif:GPSLatitude</x:xmpmeta>".toByteArray(Charsets.ISO_8859_1))
        val icc = segment(0xE2, "ICC_PROFILE\u0000\u0001\u0001fakeprofile".toByteArray(Charsets.ISO_8859_1))
        val iptc = segment(0xED, "Photoshop 3.0\u0000IPTC-CITY".toByteArray(Charsets.ISO_8859_1))
        val com = segment(0xFE, "secret comment".toByteArray())
        val trailer = "MOTION-PHOTO-VIDEO-TRAILER".toByteArray()
        // SOI + injected segments + rest of the original file (which starts with APP0 JFIF)
        val dirty = clean.copyOfRange(0, 2) + gps + xmp + icc + iptc + com + clean.copyOfRange(2, clean.size) + trailer

        val result = MetadataStripper.strip(dirty, keepIcc = true)!!
        val out = result.bytes
        assertFalse(contains(out, "GPS-LAT"))
        assertFalse(contains(out, "GPSLatitude"))
        assertFalse(contains(out, "IPTC-CITY"))
        assertFalse(contains(out, "secret comment"))
        assertFalse(contains(out, "MOTION-PHOTO"))
        assertTrue(contains(out, "ICC_PROFILE"))
        assertTrue(result.findings.exif && result.findings.xmp && result.findings.iptc && result.findings.comment)
        assertEquals(trailer.size, result.findings.trailingBytes)
        assertArrayEquals(pixels(clean), pixels(out))
        assertEquals(0xD9, out.last().toInt() and 0xFF)

        val noIcc = MetadataStripper.strip(dirty, keepIcc = false)!!.bytes
        assertFalse(contains(noIcc, "ICC_PROFILE"))
        assertArrayEquals(pixels(clean), pixels(noIcc))
    }

    @Test
    fun jpegStripIsIdempotent() {
        val clean = encode(sampleImage(), "jpg")
        val once = MetadataStripper.strip(clean, keepIcc = true)!!.bytes
        val twice = MetadataStripper.strip(once, keepIcc = true)!!.bytes
        assertArrayEquals(once, twice)
        assertArrayEquals(pixels(clean), pixels(once))
    }

    private fun pngChunk(type: String, data: ByteArray): ByteArray {
        val typeBytes = type.toByteArray(Charsets.ISO_8859_1)
        val crc = CRC32().apply { update(typeBytes); update(data) }.value
        val len = data.size
        return byteArrayOf((len ushr 24).toByte(), (len ushr 16).toByte(), (len ushr 8).toByte(), len.toByte()) +
            typeBytes + data +
            byteArrayOf((crc ushr 24).toByte(), (crc ushr 16).toByte(), (crc ushr 8).toByte(), crc.toByte())
    }

    @Test
    fun pngTextAndExifChunksRemoved() {
        val clean = encode(sampleImage(), "png")
        val ihdrEnd = 8 + 25 // signature + IHDR chunk
        val dirty = clean.copyOfRange(0, ihdrEnd) +
            pngChunk("tEXt", "Author\u0000Jane".toByteArray(Charsets.ISO_8859_1)) +
            pngChunk("eXIf", "MM\u0000*GPSDATA".toByteArray(Charsets.ISO_8859_1)) +
            pngChunk("tIME", ByteArray(7)) +
            clean.copyOfRange(ihdrEnd, clean.size) + "TRAILING".toByteArray()
        val result = MetadataStripper.strip(dirty, keepIcc = true)!!
        assertFalse(contains(result.bytes, "Jane"))
        assertFalse(contains(result.bytes, "GPSDATA"))
        assertFalse(contains(result.bytes, "TRAILING"))
        assertTrue(result.findings.exif)
        assertArrayEquals(pixels(clean), pixels(result.bytes))
    }

    private fun riffChunk(type: String, data: ByteArray): ByteArray {
        val size = data.size
        val head = type.toByteArray(Charsets.ISO_8859_1) +
            byteArrayOf(size.toByte(), (size ushr 8).toByte(), (size ushr 16).toByte(), (size ushr 24).toByte())
        return head + data + if (size % 2 == 1) byteArrayOf(0) else byteArrayOf()
    }

    @Test
    fun webpExifAndXmpChunksRemovedAndFlagsCleared() {
        val vp8x = ByteArray(10).also { it[0] = (0x08 or 0x04 or 0x20).toByte() }
        val body = "WEBP".toByteArray() +
            riffChunk("VP8X", vp8x) +
            riffChunk("ICCP", "icc".toByteArray()) +
            riffChunk("VP8 ", ByteArray(21) { 7 }) +
            riffChunk("EXIF", "GPSINSIDE".toByteArray()) +
            riffChunk("XMP ", "<xmp/>".toByteArray())
        val size = body.size
        val dirty = "RIFF".toByteArray() +
            byteArrayOf(size.toByte(), (size ushr 8).toByte(), (size ushr 16).toByte(), (size ushr 24).toByte()) + body

        val kept = MetadataStripper.strip(dirty, keepIcc = true)!!
        assertFalse(contains(kept.bytes, "GPSINSIDE"))
        assertFalse(contains(kept.bytes, "<xmp/>"))
        assertTrue(contains(kept.bytes, "ICCP"))
        assertEquals(0x20, kept.bytes[20].toInt() and 0xFF) // only the ICC flag remains
        val riffSize = (kept.bytes[4].toInt() and 0xFF) or ((kept.bytes[5].toInt() and 0xFF) shl 8)
        assertEquals(kept.bytes.size - 8, riffSize)

        val noIcc = MetadataStripper.strip(dirty, keepIcc = false)!!
        assertFalse(contains(noIcc.bytes, "ICCP"))
        assertEquals(0, noIcc.bytes[20].toInt() and 0xFF)
    }

    @Test
    fun unknownFormatsAreLeftToTheReEncoder() {
        assertNull(MetadataStripper.strip("....ftypheic....".toByteArray(), keepIcc = true))
    }

    @Test
    fun bannerRetriesTwiceThenBacksOff15Seconds() {
        val policy = BannerRetryPolicy()
        assertEquals(0L, policy.onFailure())      // 1st request failed -> 2nd goes out now
        assertEquals(15_000L, policy.onFailure()) // 2nd failed -> wait 15 s
        assertEquals(15_000L, policy.onFailure())
        policy.onSuccess()
        assertEquals(0L, policy.onFailure())
    }

    @Test
    fun interstitialCappedAtOnePer30Seconds() {
        var now = 1_000L
        val cap = InterstitialCap(clock = { now })
        assertTrue(cap.canShow())
        cap.markShown()
        now += 29_999
        assertFalse(cap.canShow())
        now += 1
        assertTrue(cap.canShow())
    }
}
