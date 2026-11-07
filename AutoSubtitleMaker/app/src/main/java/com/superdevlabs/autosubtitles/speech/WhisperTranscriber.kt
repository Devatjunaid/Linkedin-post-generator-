package com.superdevlabs.autosubtitles.speech

import android.content.Context
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig
import com.k2fsa.sherpa.onnx.SileroVadModelConfig
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.VadModelConfig
import com.superdevlabs.autosubtitles.core.model.Word
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/** Words plus the language that was spoken (ISO 639-1, e.g. "en"). */
data class TranscriptResult(val words: List<Word>, val language: String)

/**
 * Offline Whisper speech-to-text (sherpa-onnx + ONNX Runtime, all on the phone).
 *
 * Audio is cut into speech segments by Silero voice-activity detection, so silence and music
 * are skipped (that is where Whisper tends to invent text) and every segment gets accurate
 * start/end times. With "auto" language the first few segments vote on the language, which
 * is then locked for the rest of the video: per-segment detection flips on short clips.
 */
class WhisperTranscriber(private val context: Context) {

    private val lock = Mutex()
    private var loadedKey: String? = null
    private var recognizer: OfflineRecognizer? = null
    private var recognizerConfig: OfflineRecognizerConfig? = null

    private class Pending(val startMs: Long, val samples: FloatArray, val text: String, val lang: String)

    suspend fun transcribe(
        video: File,
        modelDir: File,
        model: WhisperModel,
        language: String,
        listener: TranscriptionListener,
    ): TranscriptResult = lock.withLock {
        withContext(Dispatchers.Default) {
            val auto = language == WhisperCatalog.AUTO || language.isBlank()
            val rec = recognizer(modelDir, model, if (auto) "" else language)
            val vad = Vad(context.assets, vadConfig())
            val words = mutableListOf<Word>()
            val confirmed = StringBuilder()
            var locked: String? = if (auto) null else language
            val pending = mutableListOf<Pending>()
            var pendingMs = 0L

            fun emit(startMs: Long, samples: FloatArray, text: String) {
                val endMs = startMs + samples.size * 1000L / SAMPLE_RATE
                val clean = WhisperText.clean(text, endMs - startMs)
                if (clean.isEmpty()) return
                words += WhisperText.timedWords(clean, startMs, endMs)
                confirmed.append(clean).append(' ')
                listener.onTranscript(confirmed.toString(), "")
            }

            fun lockLanguage() {
                val chosen = WhisperText.majority(pending.map { it.lang }) ?: "en"
                locked = chosen
                setLanguage(rec, chosen)
                for (p in pending) {
                    // Re-decode segments that were heard as another language.
                    val text = if (p.lang == chosen) p.text else decode(rec, p.samples).first
                    emit(p.startMs, p.samples, text)
                }
                pending.clear()
            }

            fun handle(startSample: Long, samples: FloatArray) {
                val startMs = startSample * 1000L / SAMPLE_RATE
                val (text, lang) = decode(rec, samples)
                if (locked != null) {
                    emit(startMs, samples, text)
                    return
                }
                pending += Pending(startMs, samples, text, lang)
                pendingMs += samples.size * 1000L / SAMPLE_RATE
                listener.onTranscript(confirmed.toString(), pending.joinToString(" ") { it.text })
                if (pendingMs >= DETECT_SPEECH_MS || pending.size >= DETECT_SEGMENTS) lockLanguage()
            }

            fun drain() {
                while (!vad.empty()) {
                    val segment = vad.front()
                    vad.pop()
                    val samples = segment.samples
                    // Shorter pieces = tighter word timing (VAD can return 15 s+ of non-stop talk).
                    for ((offset, length) in WhisperText.splitPoints(samples, MAX_SEGMENT_SAMPLES)) {
                        val piece = if (offset == 0 && length == samples.size) samples else samples.copyOfRange(offset, offset + length)
                        handle(segment.start + offset, piece)
                    }
                }
            }

            try {
                val floats = FloatArray(4_000)
                AudioPcmExtractor(video).decode(targetRate = SAMPLE_RATE, isCancelled = { !isActive }) { buffer, count, progress ->
                    for (i in 0 until count) floats[i] = buffer[i] / 32768f
                    vad.acceptWaveform(if (count == floats.size) floats else floats.copyOf(count))
                    drain()
                    listener.onProgress(progress * 0.99f)
                }
                if (isActive) {
                    vad.flush()
                    drain()
                    if (locked == null) lockLanguage()
                    listener.onProgress(1f)
                }
            } finally {
                vad.release()
            }
            TranscriptResult(words.sortedBy { it.startMs }, locked ?: "en")
        }
    }

    private fun decode(rec: OfflineRecognizer, samples: FloatArray): Pair<String, String> {
        val stream = rec.createStream()
        try {
            stream.acceptWaveform(samples, SAMPLE_RATE)
            rec.decode(stream)
            val result = rec.getResult(stream)
            return result.text.trim() to normalizeLang(result.lang)
        } finally {
            stream.release()
        }
    }

    /** sherpa-onnx reports "en" (sometimes "<|en|>"). */
    private fun normalizeLang(lang: String): String = lang.trim().removePrefix("<|").removeSuffix("|>").lowercase()

    private fun setLanguage(rec: OfflineRecognizer, language: String) {
        val config = recognizerConfig ?: return
        val updated = config.copy(
            modelConfig = config.modelConfig.copy(whisper = config.modelConfig.whisper.copy(language = language)),
        )
        rec.setConfig(updated)
        recognizerConfig = updated
    }

    /** Loading a model takes a few seconds, so the recognizer is reused between projects. */
    private fun recognizer(dir: File, model: WhisperModel, language: String): OfflineRecognizer {
        val key = dir.absolutePath
        val cached = recognizer
        if (cached != null && loadedKey == key) {
            setLanguage(cached, language)
            return cached
        }
        cached?.release()
        recognizer = null
        val config = OfflineRecognizerConfig(
            featConfig = FeatureConfig(sampleRate = SAMPLE_RATE, featureDim = 80),
            modelConfig = OfflineModelConfig(
                whisper = OfflineWhisperModelConfig(
                    encoder = File(dir, model.encoder).absolutePath,
                    decoder = File(dir, model.decoder).absolutePath,
                    language = language,
                    task = "transcribe",
                ),
                tokens = File(dir, model.tokens).absolutePath,
                numThreads = threads(),
                modelType = "whisper",
            ),
        )
        val created = OfflineRecognizer(config = config)
        recognizer = created
        recognizerConfig = config
        loadedKey = key
        return created
    }

    /** Frees the native model (called when a Whisper model is deleted). */
    suspend fun release() = lock.withLock {
        recognizer?.release()
        recognizer = null
        recognizerConfig = null
        loadedKey = null
    }

    private fun vadConfig() = VadModelConfig(
        sileroVadModelConfig = SileroVadModelConfig(
            model = "silero_vad.onnx",
            threshold = 0.5f,
            minSilenceDuration = 0.3f,
            minSpeechDuration = 0.25f,
            windowSize = 512,
            maxSpeechDuration = 10f,
        ),
        sampleRate = SAMPLE_RATE,
        numThreads = 1,
    )

    private fun threads(): Int = Runtime.getRuntime().availableProcessors().coerceIn(2, 4)

    private companion object {
        const val SAMPLE_RATE = 16_000
        const val DETECT_SPEECH_MS = 8_000L
        const val DETECT_SEGMENTS = 4
        const val MAX_SEGMENT_SAMPLES = 7 * SAMPLE_RATE
    }
}
