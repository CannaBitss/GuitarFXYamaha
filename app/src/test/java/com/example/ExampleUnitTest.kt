package com.example

import com.example.audio.dsp.OverdriveDsp
import com.example.audio.dsp.PitchDetectorYin
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sin

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testOverdriveDspProcessing() {
    val overdrive = OverdriveDsp(48000f)
    val buffer = FloatArray(128) { i -> sin(2.0 * Math.PI * 440.0 * i / 48000.0).toFloat() * 0.5f }
    overdrive.process(buffer, buffer.size)

    var hasNonZero = false
    for (sample in buffer) {
      if (sample != 0.0f) hasNonZero = true
      assertTrue("Sample must not exceed soft clipping ceiling", sample in -2.0f..2.0f)
    }
    assertTrue("Overdrive must produce non-zero output", hasNonZero)
  }

  @Test
  fun testOutputLimiterKeepsSamplesBelowCeiling() {
    val limiter = com.example.audio.dsp.PeakLimiter(48000f)
    val output = FloatArray(256) { if (it < 64) 0.2f else 2.0f }
      .map { limiter.process(it) }
    assertTrue("Limiter output must remain below -1 dBFS ceiling",
      output.all { it.isFinite() && kotlin.math.abs(it) <= 0.8913f })
  }

  @Test
  fun testNoiseGateAttenuatesSilenceAndOpensForGuitarSignal() {
    val gate = com.example.audio.dsp.NoiseGate(48000f)
    val silence = FloatArray(24000) { 0.0001f }
    gate.process(silence, silence.size)
    assertTrue("Noise floor should be attenuated", kotlin.math.abs(silence.last()) < 0.00003f)

    val guitarSignal = FloatArray(24000) { 0.25f }
    gate.process(guitarSignal, guitarSignal.size)
    assertTrue("Gate should open for a guitar signal", guitarSignal.last() > 0.20f)
  }

  @Test
  fun testYinPitchDetection() {
    val sampleRate = 48000f
    val yin = PitchDetectorYin(sampleRate, 2048)
    val targetFreq = 440.0f // A4
    val buffer = FloatArray(2048) { i ->
      sin(2.0 * Math.PI * targetFreq * i / sampleRate).toFloat() * 0.5f
    }
    val result = yin.getPitch(buffer)
    assertNotNull("YIN must detect 440Hz sine wave", result)
    result?.let {
      assertEquals("A", it.noteName)
      assertEquals(4, it.octave)
      assertTrue("Detected freq must be within 2Hz of 440Hz", kotlin.math.abs(it.frequency - 440f) < 2.0f)
    }
  }

  @Test
  fun testIrigConditionerProcessing() {
    val irig = com.example.audio.dsp.IrigConditioner(48000f)
    irig.inputGain = 1.5f
    irig.antiFeedbackEnabled = true
    irig.humReductionEnabled = true

    val buffer = FloatArray(256) { i -> sin(2.0 * Math.PI * 1000.0 * i / 48000.0).toFloat() * 0.2f }
    irig.process(buffer, buffer.size)

    var hasNonZero = false
    for (s in buffer) {
      if (s != 0.0f) hasNonZero = true
      assertFalse("Sample must not be NaN", s.isNaN())
      assertFalse("Sample must not be Infinite", s.isInfinite())
    }
    assertTrue("iRig conditioned buffer should contain processed audio", hasNonZero)
  }
}

