package com.example.audio.dsp

import kotlin.math.*

data class TunerResult(
    val frequency: Float,
    val noteName: String,
    val octave: Int,
    val targetFreq: Float,
    val centsDeviation: Float, // -50 to +50
    val isLocked: Boolean,     // within +/- 3 cents
    val signalLevel: Float
)

class PitchDetectorYin(
    private val sampleRate: Float,
    private val bufferSize: Int = 2048
) {
    private val yinBuffer = FloatArray(bufferSize / 2)
    private val threshold = 0.15f

    private val noteNames = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    fun getPitch(audioBuffer: FloatArray): TunerResult? {
        val halfSize = bufferSize / 2
        var energy = 0.0f
        for (i in 0 until bufferSize.coerceAtMost(audioBuffer.size)) {
            energy += audioBuffer[i] * audioBuffer[i]
        }
        val rms = sqrt(energy / bufferSize)
        if (rms < 0.012f) { // Silence or very weak signal
            return null
        }

        // Step 1: Difference Function
        for (tau in 0 until halfSize) {
            yinBuffer[tau] = 0.0f
        }
        for (tau in 1 until halfSize) {
            for (i in 0 until halfSize) {
                val delta = audioBuffer[i] - audioBuffer[i + tau]
                yinBuffer[tau] += delta * delta
            }
        }

        // Step 2: Cumulative Mean Normalized Difference
        yinBuffer[0] = 1.0f
        var runningSum = 0.0f
        for (tau in 1 until halfSize) {
            runningSum += yinBuffer[tau]
            if (runningSum > 0.0f) {
                yinBuffer[tau] *= tau / runningSum
            } else {
                yinBuffer[tau] = 1.0f
            }
        }

        // Step 3: Absolute Threshold & local minimum
        var tauEstimate = -1
        var tau = 2
        while (tau < halfSize) {
            if (yinBuffer[tau] < threshold) {
                while (tau + 1 < halfSize && yinBuffer[tau + 1] < yinBuffer[tau]) {
                    tau++
                }
                tauEstimate = tau
                break
            }
            tau++
        }

        if (tauEstimate == -1) {
            // Find global minimum if no point fell below threshold
            var minVal = 1000.0f
            for (tau in 2 until halfSize) {
                if (yinBuffer[tau] < minVal) {
                    minVal = yinBuffer[tau]
                    tauEstimate = tau
                }
            }
            if (minVal > 0.45f) return null // Too noisy or unpitched
        }

        // Step 4: Parabolic Interpolation
        val betterTau: Float = if (tauEstimate > 0 && tauEstimate < halfSize - 1) {
            val s0 = yinBuffer[tauEstimate - 1]
            val s1 = yinBuffer[tauEstimate]
            val s2 = yinBuffer[tauEstimate + 1]
            val bottom = 2.0f * (2.0f * s1 - s2 - s0)
            if (abs(bottom) > 1e-6f) {
                tauEstimate + (s2 - s0) / bottom
            } else {
                tauEstimate.toFloat()
            }
        } else {
            tauEstimate.toFloat()
        }

        if (betterTau <= 0.0f) return null
        val detectedFreq = sampleRate / betterTau

        // Guitar fundamental range: ~70 Hz (drop D / low E) to ~1200 Hz
        if (detectedFreq < 60.0f || detectedFreq > 1400.0f) return null

        // Step 5: Frequency to Musical Note & Cents
        val midiNumber = 69.0f + 12.0f * (ln(detectedFreq / 440.0f) / ln(2.0f))
        val roundedMidi = round(midiNumber).toInt()
        val cents = (midiNumber - roundedMidi) * 100.0f

        val noteIndex = (roundedMidi % 12 + 12) % 12
        val octave = (roundedMidi / 12) - 1
        val targetFreq = 440.0f * 2.0f.pow((roundedMidi - 69.0f) / 12.0f)

        return TunerResult(
            frequency = detectedFreq,
            noteName = noteNames[noteIndex],
            octave = octave,
            targetFreq = targetFreq,
            centsDeviation = cents.coerceIn(-50.0f, 50.0f),
            isLocked = abs(cents) <= 3.5f,
            signalLevel = rms
        )
    }
}
