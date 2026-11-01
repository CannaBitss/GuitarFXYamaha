package com.example.audio.dsp

/**
 * Specialized DSP conditioning for 3.5mm TRRS Analog Guitar Interfaces (IK Multimedia iRig 1 / iRig 2 / generic guitar jack).
 *
 * Solves the 3 primary hardware issues with analog phone jack guitar interfaces:
 * 1. Electrical Crosstalk & Feedback Squeal between headphone output and mic input inside the TRRS plug.
 * 2. 50Hz/60Hz AC ground hum from guitar pickups and phone charging adapters.
 * 3. Impedance mismatch and passive pickup level attenuation.
 */
class IrigConditioner(private val sampleRate: Float) {
    var isEnabled: Boolean = true
    var inputGain: Float = 1.4f            // +3 dB default boost for passive guitar pickups
    var antiFeedbackEnabled: Boolean = true // Notch filters at typical TRRS squeal frequencies
    var humReductionEnabled: Boolean = true // Sub-bass 75Hz rumble & 50/60Hz hum cut

    private val rumbleHpf = BiquadFilter()
    private val antiFeedbackNotch1 = BiquadFilter()
    private val antiFeedbackNotch2 = BiquadFilter()
    private val highCutSquealFilter = BiquadFilter()

    init {
        // High-pass filter at 75Hz to kill ground hum & handling thumps
        rumbleHpf.setHighPass(sampleRate, 75.0f, 0.707f)

        // Notch filters at 3200Hz and 4600Hz (typical TRRS capacitive crosstalk resonance peaks)
        antiFeedbackNotch1.setPeaking(sampleRate, 3200.0f, -12.0f, 2.5f)
        antiFeedbackNotch2.setPeaking(sampleRate, 4600.0f, -10.0f, 2.0f)

        // Smooth high-cut at 6200Hz to prevent acoustic/electrical oscillation
        highCutSquealFilter.setLowPass(sampleRate, 6200.0f, 0.707f)
    }

    fun process(buffer: FloatArray, size: Int) {
        if (!isEnabled) return

        for (i in 0 until size) {
            var sample = buffer[i] * inputGain

            if (humReductionEnabled) {
                sample = rumbleHpf.process(sample)
            }

            if (antiFeedbackEnabled) {
                sample = antiFeedbackNotch1.process(sample)
                sample = antiFeedbackNotch2.process(sample)
                sample = highCutSquealFilter.process(sample)
            }

            // Sanitize against NaN / Infinity
            buffer[i] = if (sample.isNaN() || sample.isInfinite()) 0.0f else sample
        }
    }
}
