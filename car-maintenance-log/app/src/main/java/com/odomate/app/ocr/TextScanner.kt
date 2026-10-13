package com.odomate.app.ocr

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

/** Tokens from every line and every element, so both "48 215" and "48215" are seen. */
fun Text.toTokens(): List<OcrToken> {
    val out = mutableListOf<OcrToken>()
    for (block in textBlocks) {
        for (line in block.lines) {
            line.boundingBox?.let { b -> out += OcrToken(line.text, b.left.toFloat(), b.top.toFloat(), b.right.toFloat(), b.bottom.toFloat()) }
            for (element in line.elements) {
                element.boundingBox?.let { b -> out += OcrToken(element.text, b.left.toFloat(), b.top.toFloat(), b.right.toFloat(), b.bottom.toFloat()) }
            }
        }
    }
    return out
}

/** Live camera analyzer: recognises text on each frame and reports tokens with the upright frame size. */
class OdometerAnalyzer(
    private val onResult: (tokens: List<OcrToken>, width: Int, height: Int) -> Unit,
) : ImageAnalysis.Analyzer {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    @Volatile
    var paused: Boolean = false

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        val media = image.image
        if (media == null || paused) {
            image.close()
            return
        }
        val rotation = image.imageInfo.rotationDegrees
        val width = if (rotation % 180 == 0) image.width else image.height
        val height = if (rotation % 180 == 0) image.height else image.width
        recognizer.process(InputImage.fromMediaImage(media, rotation))
            .addOnSuccessListener { text -> if (!paused) onResult(text.toTokens(), width, height) }
            .addOnCompleteListener { image.close() }
    }

    fun close() = recognizer.close()
}

/** One-shot recognition for a photo picked from the gallery. */
object PhotoScanner {
    suspend fun scan(context: Context, uri: Uri, lastReading: Int?): OdometerCandidate? {
        val input = InputImage.fromFilePath(context, uri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            val text = recognizer.process(input).await()
            OdometerParser.best(text.toTokens(), input.width, input.height, lastReading)
        } finally {
            recognizer.close()
        }
    }
}
