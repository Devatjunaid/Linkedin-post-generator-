package com.verba.subtitles.speech

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File
import java.nio.ByteOrder

class NoAudioTrackException : Exception("This video has no sound track, so there is no speech to subtitle.")

/**
 * Decodes the sound track of a video file to 16 kHz mono 16-bit PCM using the device's
 * hardware/software decoders (MediaCodec). Works for any container/codec Android can play.
 */
class AudioPcmExtractor(private val file: File) {

    /**
     * Streams PCM in chunks of [chunkSamples]. [onChunk] receives the buffer, the number of
     * valid samples and overall progress (0..1). Returns early when [isCancelled] is true.
     */
    fun decode(
        targetRate: Int = 16_000,
        chunkSamples: Int = 4_000,
        isCancelled: () -> Boolean,
        onChunk: (ShortArray, Int, Float) -> Unit,
    ) {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(file.absolutePath)
            val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: throw NoAudioTrackException()
            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: throw NoAudioTrackException()
            val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION) else 0L

            var channels = format.getIntegerOr(MediaFormat.KEY_CHANNEL_COUNT, 1).coerceAtLeast(1)
            var sampleRate = format.getIntegerOr(MediaFormat.KEY_SAMPLE_RATE, 44_100)
            var floatPcm = false
            var resampler = PcmResampler(sampleRate, targetRate)

            val decoder = MediaCodec.createDecoderByType(mime)
            codec = decoder
            decoder.configure(format, null, null, 0)
            decoder.start()

            val out = ShortArray(chunkSamples)
            var outCount = 0
            var progress = 0f
            var mono = FloatArray(4096)
            val emit: (Float) -> Unit = { sample ->
                out[outCount++] = (sample.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
                if (outCount == chunkSamples) {
                    onChunk(out, outCount, progress)
                    outCount = 0
                }
            }

            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            while (!outputDone) {
                if (isCancelled()) return
                if (!inputDone) {
                    val inIndex = decoder.dequeueInputBuffer(TIMEOUT_US)
                    if (inIndex >= 0) {
                        val buffer = decoder.getInputBuffer(inIndex)
                        val size = if (buffer != null) extractor.readSampleData(buffer, 0) else -1
                        if (size < 0) {
                            decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            decoder.queueInputBuffer(inIndex, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outIndex = decoder.dequeueOutputBuffer(info, TIMEOUT_US)
                when {
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val newFormat = decoder.outputFormat
                        channels = newFormat.getIntegerOr(MediaFormat.KEY_CHANNEL_COUNT, channels).coerceAtLeast(1)
                        val newRate = newFormat.getIntegerOr(MediaFormat.KEY_SAMPLE_RATE, sampleRate)
                        floatPcm = newFormat.getIntegerOr(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT) ==
                            AudioFormat.ENCODING_PCM_FLOAT
                        if (newRate != sampleRate) {
                            sampleRate = newRate
                            resampler = PcmResampler(sampleRate, targetRate)
                        }
                    }
                    outIndex >= 0 -> {
                        val buffer = decoder.getOutputBuffer(outIndex)
                        if (buffer != null && info.size > 0) {
                            buffer.position(info.offset)
                            buffer.limit(info.offset + info.size)
                            val ordered = buffer.slice().order(ByteOrder.nativeOrder())
                            val frames = if (floatPcm) info.size / 4 / channels else info.size / 2 / channels
                            if (mono.size < frames) mono = FloatArray(frames)
                            if (floatPcm) {
                                val fb = ordered.asFloatBuffer()
                                for (f in 0 until frames) {
                                    var sum = 0f
                                    for (c in 0 until channels) sum += fb.get(f * channels + c)
                                    mono[f] = sum / channels
                                }
                            } else {
                                val sb = ordered.asShortBuffer()
                                for (f in 0 until frames) {
                                    var sum = 0
                                    for (c in 0 until channels) sum += sb.get(f * channels + c).toInt()
                                    mono[f] = sum.toFloat() / channels / 32768f
                                }
                            }
                            if (durationUs > 0) progress = (info.presentationTimeUs.toFloat() / durationUs).coerceIn(0f, 1f)
                            resampler.process(mono, frames, emit)
                        }
                        decoder.releaseOutputBuffer(outIndex, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                    }
                }
            }
            if (outCount > 0) onChunk(out, outCount, 1f)
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            extractor.release()
        }
    }

    private fun MediaFormat.getIntegerOr(key: String, fallback: Int): Int =
        if (containsKey(key)) getInteger(key) else fallback

    private companion object {
        const val TIMEOUT_US = 10_000L
    }
}
