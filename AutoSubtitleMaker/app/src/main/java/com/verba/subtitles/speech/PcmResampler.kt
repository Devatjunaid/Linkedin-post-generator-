package com.verba.subtitles.speech

import kotlin.math.PI
import kotlin.math.exp

/**
 * Streaming mono resampler (linear interpolation with a one-pole anti-alias filter) that
 * turns any decoder sample rate into the 16 kHz the speech model expects. Keeps its state
 * between calls so audio can be fed in arbitrary chunks.
 */
class PcmResampler(private val inRate: Int, private val outRate: Int) {

    private val step = inRate.toDouble() / outRate
    private val filterAlpha: Float =
        if (inRate > outRate) (1.0 - exp(-2.0 * PI * (outRate * 0.45) / inRate)).toFloat() else 1f
    private var filtered = 0f
    private var position = 0.0
    private var previous = 0f
    private var primed = false

    /** Resamples [count] samples of [input] (values in -1..1) and passes each output sample to [emit]. */
    fun process(input: FloatArray, count: Int, emit: (Float) -> Unit) {
        if (count <= 0) return
        if (inRate == outRate) {
            for (i in 0 until count) emit(input[i])
            return
        }
        // Low-pass in place before decimation.
        for (i in 0 until count) {
            filtered += filterAlpha * (input[i] - filtered)
            input[i] = filtered
        }
        if (!primed) {
            previous = input[0]
            primed = true
        }
        var p = position
        // p indexes the input; -1 refers to the last sample of the previous chunk.
        while (p < count - 1) {
            val i = kotlin.math.floor(p).toInt()
            val frac = (p - i).toFloat()
            val a = if (i < 0) previous else input[i]
            val b = input[i + 1]
            emit(a + (b - a) * frac)
            p += step
        }
        position = p - count
        previous = input[count - 1]
    }
}
