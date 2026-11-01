package com.example.audio.dsp

import kotlin.math.*

class BiquadFilter {
    private var b0 = 1.0f
    private var b1 = 0.0f
    private var b2 = 0.0f
    private var a1 = 0.0f
    private var a2 = 0.0f

    private var z1 = 0.0f
    private var z2 = 0.0f

    fun reset() {
        z1 = 0.0f
        z2 = 0.0f
    }

    fun setLowPass(sampleRate: Float, cutoffHz: Float, q: Float = 0.707f) {
        val w0 = 2.0f * Math.PI.toFloat() * cutoffHz / sampleRate
        val alpha = sin(w0) / (2.0f * q)
        val cosw0 = cos(w0)

        val a0 = 1.0f + alpha
        b0 = ((1.0f - cosw0) / 2.0f) / a0
        b1 = (1.0f - cosw0) / a0
        b2 = ((1.0f - cosw0) / 2.0f) / a0
        a1 = (-2.0f * cosw0) / a0
        a2 = (1.0f - alpha) / a0
    }

    fun setHighPass(sampleRate: Float, cutoffHz: Float, q: Float = 0.707f) {
        val w0 = 2.0f * Math.PI.toFloat() * cutoffHz / sampleRate
        val alpha = sin(w0) / (2.0f * q)
        val cosw0 = cos(w0)

        val a0 = 1.0f + alpha
        b0 = ((1.0f + cosw0) / 2.0f) / a0
        b1 = (-(1.0f + cosw0)) / a0
        b2 = ((1.0f + cosw0) / 2.0f) / a0
        a1 = (-2.0f * cosw0) / a0
        a2 = (1.0f - alpha) / a0
    }

    fun setPeaking(sampleRate: Float, centerHz: Float, gainDb: Float, q: Float = 1.0f) {
        val w0 = 2.0f * Math.PI.toFloat() * centerHz / sampleRate
        val a = 10.0f.pow(gainDb / 40.0f)
        val alpha = sin(w0) / (2.0f * q)
        val cosw0 = cos(w0)

        val a0 = 1.0f + alpha / a
        b0 = (1.0f + alpha * a) / a0
        b1 = (-2.0f * cosw0) / a0
        b2 = (1.0f - alpha * a) / a0
        a1 = (-2.0f * cosw0) / a0
        a2 = (1.0f - alpha / a) / a0
    }

    fun setLowShelf(sampleRate: Float, cutoffHz: Float, gainDb: Float) {
        val a = 10.0f.pow(gainDb / 40.0f)
        val w0 = 2.0f * Math.PI.toFloat() * cutoffHz / sampleRate
        val cosw0 = cos(w0)
        val sinw0 = sin(w0)
        val alpha = sinw0 / 2.0f * sqrt(2.0f)

        val a0 = (a + 1.0f) + (a - 1.0f) * cosw0 + 2.0f * sqrt(a) * alpha
        b0 = (a * ((a + 1.0f) - (a - 1.0f) * cosw0 + 2.0f * sqrt(a) * alpha)) / a0
        b1 = (2.0f * a * ((a - 1.0f) - (a + 1.0f) * cosw0)) / a0
        b2 = (a * ((a + 1.0f) - (a - 1.0f) * cosw0 - 2.0f * sqrt(a) * alpha)) / a0
        a1 = (-2.0f * ((a - 1.0f) + (a + 1.0f) * cosw0)) / a0
        a2 = ((a + 1.0f) + (a - 1.0f) * cosw0 - 2.0f * sqrt(a) * alpha) / a0
    }

    fun setHighShelf(sampleRate: Float, cutoffHz: Float, gainDb: Float) {
        val a = 10.0f.pow(gainDb / 40.0f)
        val w0 = 2.0f * Math.PI.toFloat() * cutoffHz / sampleRate
        val cosw0 = cos(w0)
        val sinw0 = sin(w0)
        val alpha = sinw0 / 2.0f * sqrt(2.0f)

        val a0 = (a + 1.0f) - (a - 1.0f) * cosw0 + 2.0f * sqrt(a) * alpha
        b0 = (a * ((a + 1.0f) + (a - 1.0f) * cosw0 + 2.0f * sqrt(a) * alpha)) / a0
        b1 = (-2.0f * a * ((a - 1.0f) + (a + 1.0f) * cosw0)) / a0
        b2 = (a * ((a + 1.0f) + (a - 1.0f) * cosw0 - 2.0f * sqrt(a) * alpha)) / a0
        a1 = (2.0f * ((a - 1.0f) - (a + 1.0f) * cosw0)) / a0
        a2 = ((a + 1.0f) - (a - 1.0f) * cosw0 - 2.0f * sqrt(a) * alpha) / a0
    }

    fun process(sample: Float): Float {
        val out = b0 * sample + z1
        z1 = b1 * sample - a1 * out + z2
        z2 = b2 * sample - a2 * out
        return out
    }
}

class NoiseGate(private val sampleRate: Float) {
    var thresholdDb: Float = -48.0f
    var releaseMs: Float = 120.0f
    var floorDb: Float = -18.0f

    private var envelope: Float = 0.0f
    private var gain: Float = 1.0f

    fun process(input: FloatArray, size: Int) {
        if (size <= 0) return
        val openThreshold = 10.0f.pow(thresholdDb.coerceIn(-80.0f, -20.0f) / 20.0f)
        val closeThreshold = openThreshold * 0.65f
        val floorGain = 10.0f.pow(floorDb.coerceIn(-36.0f, -6.0f) / 20.0f)
        val attackCoeff = exp(-1.0f / (sampleRate * 0.004f))
        val detectorRelease = exp(-1.0f / (sampleRate * 0.035f))
        val gainAttack = exp(-1.0f / (sampleRate * 0.006f))
        val gainRelease = exp(-1.0f / (sampleRate * (releaseMs.coerceIn(30.0f, 350.0f) / 1000.0f)))
        var isOpen = gain > floorGain + 0.05f

        for (i in 0 until size) {
            val level = abs(input[i])
            envelope = if (level > envelope) {
                attackCoeff * envelope + (1.0f - attackCoeff) * level
            } else {
                detectorRelease * envelope + (1.0f - detectorRelease) * level
            }

            if (isOpen && envelope < closeThreshold) isOpen = false
            else if (!isOpen && envelope > openThreshold) isOpen = true

            val target = if (isOpen) 1.0f else floorGain
            val coeff = if (target > gain) gainAttack else gainRelease
            gain = coeff * gain + (1.0f - coeff) * target
            input[i] *= gain
        }
    }
}

class OverdriveDsp(private val sampleRate: Float) {
    var drive: Float = 0.5f
    var tone: Float = 0.5f
    var level: Float = 0.7f

    private val preHp = BiquadFilter()
    private val postLp = BiquadFilter()

    init {
        preHp.setHighPass(sampleRate, 720.0f, 0.707f)
        updateTone()
    }

    fun updateTone() {
        val cutoff = 1200.0f + tone * 4500.0f
        postLp.setLowPass(sampleRate, cutoff, 0.707f)
    }

    fun process(input: FloatArray, size: Int) {
        updateTone()
        val gainFactor = 1.0f + drive * 28.0f
        val outLevel = level * 1.5f

        for (i in 0 until size) {
            val pre = preHp.process(input[i])
            val boosted = pre * gainFactor
            // Asymmetric soft tube diode waveshaper
            val shaped = if (boosted > 0.0f) {
                tanh(boosted)
            } else {
                // slightly more linear in negative cycle for tube asymmetry
                tanh(boosted * 0.85f) / 0.85f
            }
            val filtered = postLp.process(shaped)
            // Blend clean & overdrive (typical TS808 clean bleed)
            input[i] = (filtered * 0.85f + input[i] * 0.15f) * outLevel
        }
    }
}

class DistortionDsp(private val sampleRate: Float) {
    var gain: Float = 0.7f
    var tone: Float = 0.5f
    var level: Float = 0.7f

    private val postFilter = BiquadFilter()

    init {
        updateFilter()
    }

    fun updateFilter() {
        val cutoff = 900.0f + (1.0f - tone) * 5000.0f
        postFilter.setLowPass(sampleRate, cutoff, 0.9f)
    }

    fun process(input: FloatArray, size: Int) {
        updateFilter()
        val ampGain = 2.0f + gain * 60.0f

        for (i in 0 until size) {
            val x = input[i] * ampGain
            // Hard silicon diode clipping with op-amp slew
            val clipped = when {
                x > 1.0f -> 0.75f + 0.25f * tanh((x - 1.0f) * 2.0f)
                x < -1.0f -> -0.75f + 0.25f * tanh((x + 1.0f) * 2.0f)
                else -> x - (x * x * x) / 3.0f
            }
            input[i] = postFilter.process(clipped) * level
        }
    }
}

class BoosterDsp(private val sampleRate: Float) {
    var gain: Float = 2.0f
    var bright: Boolean = true
    private val brightFilter = BiquadFilter()

    init {
        brightFilter.setHighShelf(sampleRate, 2500.0f, 4.5f)
    }

    fun process(input: FloatArray, size: Int) {
        for (i in 0 until size) {
            var x = input[i] * gain
            if (bright) {
                x = brightFilter.process(x)
            }
            // Gentle transparent saturation
            input[i] = tanh(x * 0.9f)
        }
    }
}

class FuzzDsp {
    var fuzz: Float = 0.8f
    var level: Float = 0.7f

    fun process(input: FloatArray, size: Int) {
        val driveGain = 5.0f + fuzz * 80.0f
        for (i in 0 until size) {
            val x = input[i] * driveGain
            // Vintage Germanium square wave saturation with asymmetric bias
            val biased = x + 0.2f
            val shaped = if (biased > 0.0f) {
                1.0f - exp(-biased)
            } else {
                -1.0f + exp(biased * 0.8f)
            }
            input[i] = shaped * level * 0.7f
        }
    }
}

class AmpSimulatorDsp(private val sampleRate: Float) {
    var modelIndex: Int = 0 // 0: Clean, 1: Crunch, 2: Lead, 3: Modern, 4: Bass
    var gain: Float = 0.5f
    var bass: Float = 0.5f
    var mid: Float = 0.5f
    var treble: Float = 0.5f
    var presence: Float = 0.5f
    var master: Float = 0.8f

    private val bassFilter = BiquadFilter()
    private val midFilter = BiquadFilter()
    private val trebleFilter = BiquadFilter()
    private val presenceFilter = BiquadFilter()

    init {
        updateToneStack()
    }

    fun updateToneStack() {
        val bassDb = (bass - 0.5f) * 16.0f
        val midDb = (mid - 0.5f) * 14.0f
        val trebleDb = (treble - 0.5f) * 18.0f
        val presDb = (presence - 0.5f) * 12.0f

        bassFilter.setLowShelf(sampleRate, 120.0f, bassDb)
        midFilter.setPeaking(sampleRate, 650.0f, midDb, 1.2f)
        trebleFilter.setHighShelf(sampleRate, 3200.0f, trebleDb)
        presenceFilter.setPeaking(sampleRate, 4800.0f, presDb, 1.0f)
    }

    fun process(input: FloatArray, size: Int) {
        updateToneStack()

        val preampDrive = when (modelIndex) {
            0 -> 1.2f + gain * 3.5f    // CLEAN
            1 -> 2.5f + gain * 12.0f   // CRUNCH
            2 -> 5.0f + gain * 35.0f   // LEAD
            3 -> 8.0f + gain * 55.0f   // MODERN
            4 -> 1.5f + gain * 6.0f    // BASS
            else -> 2.0f + gain * 10.0f
        }

        for (i in 0 until size) {
            var x = input[i] * preampDrive

            // Vacuum Tube Transfer Characteristic (Yamaha THR modeling curve)
            x = when (modelIndex) {
                0 -> tanh(x) * 1.1f
                1 -> {
                    // 12AX7 class-A triode warm saturation
                    val biased = x + 0.12f
                    (tanh(biased * 1.3f) - 0.10f)
                }
                2 -> {
                    // High-gain cascading tube stages
                    var s1 = tanh(x * 1.5f)
                    var s2 = tanh(s1 * 1.8f + 0.08f)
                    s2
                }
                3 -> {
                    // Modern dual-rectifier saturation
                    val s = tanh(x * 2.2f)
                    val scooped = s - (s * s * s) * 0.25f
                    scooped
                }
                4 -> {
                    // Deep tube bass punch
                    tanh(x * 1.2f) * 1.2f
                }
                else -> tanh(x)
            }

            // Passive Tone Stack
            var eq = bassFilter.process(x)
            eq = midFilter.process(eq)
            eq = trebleFilter.process(eq)
            eq = presenceFilter.process(eq)

            // Power amp stage & Master output
            input[i] = tanh(eq * master * 1.2f)
        }
    }
}

class CabinetSimulatorDsp(private val sampleRate: Float) {
    var cabIndex: Int = 1 // 0: 1x12, 1: 2x12, 2: 4x12 V30, 3: 4x12 Recto, 4: 1x15 Bass
    var micDistance: Float = 0.25f

    private val lowResonance = BiquadFilter()
    private val bodyResonance = BiquadFilter()
    private val highRollOff = BiquadFilter()

    init {
        updateCab()
    }

    fun updateCab() {
        when (cabIndex) {
            0 -> { // 1x12 American
                lowResonance.setPeaking(sampleRate, 95.0f, 3.0f, 1.5f)
                bodyResonance.setPeaking(sampleRate, 2400.0f, -2.0f, 1.0f)
                highRollOff.setLowPass(sampleRate, 5200.0f, 0.707f)
            }
            1 -> { // 2x12 Celestion Greenback
                lowResonance.setPeaking(sampleRate, 85.0f, 4.0f, 1.8f)
                bodyResonance.setPeaking(sampleRate, 1800.0f, 3.5f, 1.4f)
                highRollOff.setLowPass(sampleRate, 4800.0f, 0.85f)
            }
            2 -> { // 4x12 Vintage 30
                lowResonance.setPeaking(sampleRate, 75.0f, 5.0f, 2.0f)
                bodyResonance.setPeaking(sampleRate, 2600.0f, 5.0f, 1.6f)
                highRollOff.setLowPass(sampleRate, 5500.0f, 0.9f)
            }
            3 -> { // 4x12 Recto Heavy
                lowResonance.setPeaking(sampleRate, 65.0f, 6.5f, 2.2f)
                bodyResonance.setPeaking(sampleRate, 3100.0f, 4.0f, 1.5f)
                highRollOff.setLowPass(sampleRate, 6000.0f, 0.707f)
            }
            4 -> { // 1x15 Bass
                lowResonance.setPeaking(sampleRate, 50.0f, 7.0f, 1.8f)
                bodyResonance.setPeaking(sampleRate, 900.0f, -3.0f, 1.0f)
                highRollOff.setLowPass(sampleRate, 3500.0f, 0.707f)
            }
        }
    }

    fun process(input: FloatArray, size: Int) {
        updateCab()
        val airDamp = 1.0f - micDistance * 0.3f

        for (i in 0 until size) {
            var x = lowResonance.process(input[i])
            x = bodyResonance.process(x)
            x = highRollOff.process(x)
            input[i] = x * airDamp
        }
    }
}

class ChorusDsp(private val sampleRate: Float) {
    var rateHz: Float = 1.2f
    var depth: Float = 0.5f
    var mix: Float = 0.35f

    private val maxDelay = (sampleRate * 0.035f).toInt() // 35ms
    private val buffer = FloatArray(maxDelay)
    private var writeIdx = 0
    private var lfoPhase = 0.0f

    fun process(input: FloatArray, size: Int) {
        val lfoInc = 2.0f * Math.PI.toFloat() * rateHz / sampleRate
        val baseDelay = sampleRate * 0.015f // 15ms center
        val modAmp = sampleRate * 0.008f * depth

        for (i in 0 until size) {
            val dry = input[i]
            buffer[writeIdx] = dry

            lfoPhase += lfoInc
            if (lfoPhase >= 2.0f * Math.PI.toFloat()) lfoPhase -= 2.0f * Math.PI.toFloat()

            val modDelay = baseDelay + sin(lfoPhase) * modAmp
            var readIdx = writeIdx - modDelay
            while (readIdx < 0.0f) readIdx += maxDelay

            val r0 = readIdx.toInt() % maxDelay
            val r1 = (r0 + 1) % maxDelay
            val frac = readIdx - readIdx.toInt()
            val wet = buffer[r0] * (1.0f - frac) + buffer[r1] * frac

            input[i] = dry * (1.0f - mix * 0.5f) + wet * mix

            writeIdx = (writeIdx + 1) % maxDelay
        }
    }
}

class DelayDsp(private val sampleRate: Float) {
    var delayTimeMs: Float = 350.0f
    var feedback: Float = 0.4f
    var mix: Float = 0.3f

    private val maxDelaySamples = (sampleRate * 2.0f).toInt() // up to 2 seconds
    private val buffer = FloatArray(maxDelaySamples)
    private var writeIdx = 0
    private val dampingFilter = BiquadFilter()

    init {
        dampingFilter.setLowPass(sampleRate, 3800.0f, 0.707f)
    }

    fun process(input: FloatArray, size: Int) {
        val delaySamples = (sampleRate * (delayTimeMs / 1000.0f)).toInt().coerceIn(1, maxDelaySamples - 1)

        for (i in 0 until size) {
            val dry = input[i]
            var readIdx = writeIdx - delaySamples
            if (readIdx < 0) readIdx += maxDelaySamples

            val delayed = buffer[readIdx]
            val dampedFeedback = dampingFilter.process(delayed)

            buffer[writeIdx] = dry + dampedFeedback * feedback
            input[i] = dry + delayed * mix

            writeIdx = (writeIdx + 1) % maxDelaySamples
        }
    }
}

/**
 * Yamaha REV-X inspired algorithmic reverberator
 * Combines early reflections diffusion, parallel tuned comb filters,
 * and dual all-pass diffusion matrices.
 */
class YamahaRevXReverbDsp(private val sampleRate: Float) {
    var typeIndex: Int = 1 // 0: Room, 1: Hall, 2: Plate
    var decaySec: Float = 2.5f
    var mix: Float = 0.35f
    var damping: Float = 0.4f

    private val combLengths = intArrayOf(
        (sampleRate * 0.0315f).toInt(),
        (sampleRate * 0.0361f).toInt(),
        (sampleRate * 0.0411f).toInt(),
        (sampleRate * 0.0467f).toInt()
    )
    private val combBuffers = Array(4) { FloatArray(combLengths[it] + 1) }
    private val combIndices = IntArray(4)
    private val combFilterZ = FloatArray(4)

    private val allpassLength1 = (sampleRate * 0.0051f).toInt()
    private val allpassBuffer1 = FloatArray(allpassLength1 + 1)
    private var allpassIdx1 = 0

    private val allpassLength2 = (sampleRate * 0.0017f).toInt()
    private val allpassBuffer2 = FloatArray(allpassLength2 + 1)
    private var allpassIdx2 = 0

    fun process(input: FloatArray, size: Int) {
        val roomScale = when (typeIndex) {
            0 -> 0.6f  // Room
            1 -> 1.0f  // Hall
            2 -> 1.3f  // Plate
            else -> 1.0f
        }
        val feedbackGain = (0.70f + 0.22f * (decaySec / 6.0f).coerceIn(0.0f, 1.0f)) * roomScale
        val dampFactor = damping.coerceIn(0.05f, 0.85f)

        for (i in 0 until size) {
            val dry = input[i]
            var combSum = 0.0f

            for (c in 0 until 4) {
                val len = combLengths[c]
                val idx = combIndices[c]
                val out = combBuffers[c][idx]

                // Frequency damping
                val filtered = out * (1.0f - dampFactor) + combFilterZ[c] * dampFactor
                combFilterZ[c] = filtered

                combBuffers[c][idx] = dry + filtered * feedbackGain
                combIndices[c] = (idx + 1) % len
                combSum += out
            }

            // All-pass filter 1
            val apIn1 = combSum * 0.25f
            val apOutBuf1 = allpassBuffer1[allpassIdx1]
            val apOut1 = -0.7f * apIn1 + apOutBuf1
            allpassBuffer1[allpassIdx1] = apIn1 + 0.7f * apOut1
            allpassIdx1 = (allpassIdx1 + 1) % allpassLength1

            // All-pass filter 2
            val apIn2 = apOut1
            val apOutBuf2 = allpassBuffer2[allpassIdx2]
            val apOut2 = -0.6f * apIn2 + apOutBuf2
            allpassBuffer2[allpassIdx2] = apIn2 + 0.6f * apOut2
            allpassIdx2 = (allpassIdx2 + 1) % allpassLength2

            input[i] = dry * (1.0f - mix * 0.6f) + apOut2 * mix * 1.5f
        }
    }
}
