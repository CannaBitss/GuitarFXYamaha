package com.example.audio.dsp

import kotlin.math.abs
import kotlin.math.exp

/**
 * Fast-attack, smooth-release peak limiter for the final PCM output stage.
 * Ceiling is expressed in linear full-scale units (0..1).
 */
class PeakLimiter(
    private val sampleRate: Float,
    var ceiling: Float = 0.89125f, // -1 dBFS
    var releaseMs: Float = 90.0f
) {
    private var gain = 1.0f
    private var cachedReleaseMs = releaseMs
    private var cachedReleaseCoeff = exp(-1.0f / (sampleRate * (releaseMs / 1000.0f)))
    private val safeCeiling get() = ceiling.coerceIn(0.1f, 0.98f)

    fun process(sample: Float): Float {
        if (!sample.isFinite()) return 0.0f

        val peak = abs(sample)
        val targetGain = if (peak > safeCeiling) safeCeiling / peak else 1.0f

        // Instant attack prevents overs; exponential release avoids audible pumping.
        gain = if (targetGain < gain) {
            targetGain
        } else {
            if (releaseMs != cachedReleaseMs) {
                cachedReleaseMs = releaseMs
                val releaseSeconds = releaseMs.coerceIn(10.0f, 500.0f) / 1000.0f
                cachedReleaseCoeff = exp(-1.0f / (sampleRate * releaseSeconds))
            }
            cachedReleaseCoeff * gain + (1.0f - cachedReleaseCoeff) * targetGain
        }

        return (sample * gain).coerceIn(-safeCeiling, safeCeiling)
    }

    fun reset() {
        gain = 1.0f
    }
}
