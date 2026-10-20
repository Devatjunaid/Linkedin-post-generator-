package com.superdevlabs.autosubtitles.speech

import com.superdevlabs.autosubtitles.core.model.Word
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import kotlin.math.roundToLong

interface TranscriptionListener {
    fun onProgress(progress: Float)
    /** Text recognised so far: confirmed words plus the current, still-changing guess. */
    fun onTranscript(confirmed: String, partial: String)
}

/**
 * Offline speech-to-text with word-level timestamps, powered by Vosk (Kaldi) running
 * on-device. The loaded model is cached so consecutive projects start instantly.
 */
class Transcriber {

    private val modelLock = Mutex()
    private var loadedPath: String? = null
    private var loadedModel: Model? = null

    init {
        runCatching { LibVosk.setLogLevel(LogLevel.WARNINGS) }
    }

    suspend fun transcribe(video: File, modelDir: File, listener: TranscriptionListener): List<Word> =
        withContext(Dispatchers.Default) {
            val model = model(modelDir)
            val words = mutableListOf<Word>()
            val confirmed = StringBuilder()
            Recognizer(model, SAMPLE_RATE.toFloat()).use { recognizer ->
                recognizer.setWords(true)
                AudioPcmExtractor(video).decode(
                    targetRate = SAMPLE_RATE,
                    isCancelled = { !isActive },
                ) { buffer, count, progress ->
                    if (recognizer.acceptWaveForm(buffer, count)) {
                        val chunk = parseWords(recognizer.result)
                        words += chunk
                        chunk.forEach { confirmed.append(it.text).append(' ') }
                        listener.onTranscript(confirmed.toString(), "")
                    } else {
                        listener.onTranscript(confirmed.toString(), parsePartial(recognizer.partialResult))
                    }
                    listener.onProgress(progress)
                }
                if (isActive) {
                    words += parseWords(recognizer.finalResult)
                    listener.onTranscript(words.joinToString(" ") { it.text }, "")
                    listener.onProgress(1f)
                }
            }
            words.sortedBy { it.startMs }
        }

    private suspend fun model(dir: File): Model = modelLock.withLock {
        val path = dir.absolutePath
        val cached = loadedModel
        if (cached != null && loadedPath == path) return@withLock cached
        cached?.close()
        loadedModel = null
        val model = withContext(Dispatchers.IO) { Model(path) }
        loadedModel = model
        loadedPath = path
        model
    }

    /** Frees the native model (called when a model is deleted). */
    suspend fun release() = modelLock.withLock {
        loadedModel?.close()
        loadedModel = null
        loadedPath = null
    }

    companion object {
        const val SAMPLE_RATE = 16_000

        /** Parses Vosk's `{"result":[{"word":"hi","start":0.4,"end":0.7,"conf":1.0}],"text":"hi"}`. */
        fun parseWords(json: String?): List<Word> {
            if (json.isNullOrBlank()) return emptyList()
            val array = runCatching { JSONObject(json).optJSONArray("result") }.getOrNull() ?: return emptyList()
            return (0 until array.length()).mapNotNull { i ->
                val item = array.optJSONObject(i) ?: return@mapNotNull null
                val text = item.optString("word").trim()
                if (text.isEmpty() || text == "[unk]") return@mapNotNull null
                Word(
                    text = text,
                    startMs = (item.optDouble("start", 0.0) * 1000).roundToLong(),
                    endMs = (item.optDouble("end", 0.0) * 1000).roundToLong(),
                )
            }
        }

        fun parsePartial(json: String?): String =
            if (json.isNullOrBlank()) "" else runCatching { JSONObject(json).optString("partial") }.getOrDefault("")
    }
}
