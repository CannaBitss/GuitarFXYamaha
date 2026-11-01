package com.example.audio.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/** Classic short-delay, feedback flanger with linear interpolation and a sinusoidal LFO. */
class FlangerDsp(private val sampleRate: Float) {
    @Volatile var rateHz: Float = 0.35f
    @Volatile var depth: Float = 0.7f
    @Volatile var mix: Float = 0.35f
    @Volatile var feedback: Float = 0.2f

    private val delayBuffer = FloatArray((sampleRate * 0.015f).toInt().coerceAtLeast(8))
    private var writeIndex = 0
    private var phase = 0.0f
    private var feedbackSample = 0.0f

    fun process(input: FloatArray, size: Int) {
        val maxDelay = delayBuffer.size
        val phaseIncrement = (2.0 * PI * rateHz.coerceIn(0.05f, 5.0f) / sampleRate).toFloat()
        val depthSamples = sampleRate * 0.0045f * depth.coerceIn(0f, 1f)
        val baseDelay = sampleRate * 0.0045f
        val wetMix = mix.coerceIn(0f, 1f)
        val feedbackAmount = feedback.coerceIn(0f, 0.75f)

        for (i in 0 until size) {
            val dry = input[i]
            delayBuffer[writeIndex] = dry + feedbackSample * feedbackAmount

            phase += phaseIncrement
            if (phase >= (2.0 * PI).toFloat()) phase -= (2.0 * PI).toFloat()

            val delaySamples = (baseDelay + (0.5f + 0.5f * sin(phase)) * depthSamples)
                .coerceIn(1f, (maxDelay - 2).toFloat())
            var readPosition = writeIndex - delaySamples
            if (readPosition < 0f) readPosition += maxDelay
            val index0 = readPosition.toInt()
            val index1 = (index0 + 1) % maxDelay
            val fraction = readPosition - index0
            val delayed = delayBuffer[index0] * (1f - fraction) + delayBuffer[index1] * fraction
            feedbackSample = delayed
            input[i] = dry * (1f - wetMix * 0.5f) + delayed * wetMix * 0.5f
            writeIndex = (writeIndex + 1) % maxDelay
        }
    }

    fun reset() {
        delayBuffer.fill(0f)
        writeIndex = 0
        phase = 0f
        feedbackSample = 0f
    }
}

/** Resonant band-pass wah filter. Position is a manual expression-pedal value (0..1). */
class WahDsp(private val sampleRate: Float) {
    @Volatile var position: Float = 0.5f
    @Volatile var q: Float = 3.5f
    @Volatile var mix: Float = 1.0f

    private var b0 = 0f
    private var b1 = 0f
    private var b2 = 0f
    private var a1 = 0f
    private var a2 = 0f
    private var z1 = 0f
    private var z2 = 0f
    private var lastPosition = -1f
    private var lastQ = -1f

    private fun updateCoefficients() {
        val p = position.coerceIn(0f, 1f)
        val quality = q.coerceIn(0.7f, 10f)
        if (p == lastPosition && quality == lastQ) return

        val frequency = 350f * 6f.pow(p) // approximately 350 Hz .. 2100 Hz
        val omega = (2.0 * PI * frequency / sampleRate).toFloat()
        val alpha = sin(omega) / (2f * quality)
        val cosine = cos(omega)
        val a0 = 1f + alpha

        // Constant-skirt-gain band-pass biquad.
        b0 = alpha / a0
        b1 = 0f
        b2 = -alpha / a0
        a1 = -2f * cosine / a0
        a2 = (1f - alpha) / a0
        lastPosition = p
        lastQ = quality
    }

    fun process(input: FloatArray, size: Int) {
        updateCoefficients()
        val wet = mix.coerceIn(0f, 1f)
        for (i in 0 until size) {
            val dry = input[i]
            val filtered = b0 * dry + z1
            z1 = b1 * dry - a1 * filtered + z2
            z2 = b2 * dry - a2 * filtered
            input[i] = dry * (1f - wet) + filtered * (wet * 5.5f)
        }
    }

    fun reset() {
        z1 = 0f
        z2 = 0f
        lastPosition = -1f
    }
}
