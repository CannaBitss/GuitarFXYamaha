package com.example.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import android.os.Build
import android.os.Process
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.audio.dsp.*
import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.max

data class AudioEngineStats(
    val isRunning: Boolean = false,
    val sampleRate: Int = 48000,
    val bufferSizeFrames: Int = 256,
    val measuredLatencyMs: Float = 5.3f,
    val inputLevelRms: Float = 0.0f,
    val outputLevelRms: Float = 0.0f,
    val isClipping: Boolean = false,
    val activeDeviceName: String = "Default Audio",
    val statusMessage: String = "Engine Ready"
)

class AudioEngine(private val context: Context) {
    private val deviceInfoHelper = AudioDeviceInfoHelper(context)
    private val isRunningFlag = AtomicBoolean(false)
    private var audioThread: Thread? = null

    private val _engineStats = MutableStateFlow(AudioEngineStats())
    val engineStats = _engineStats.asStateFlow()

    private val _waveformFlow = MutableStateFlow(FloatArray(64))
    val waveformFlow = _waveformFlow.asStateFlow()

    private val _tunerResultFlow = MutableStateFlow<TunerResult?>(null)
    val tunerResultFlow = _tunerResultFlow.asStateFlow()

    var isTunerActive: Boolean = false
    var isMuted: Boolean = false
    var masterVolume: Float = 1.4f

    // DSP Effects
    private val sampleRate = 48000f
    private val outputLimiter = PeakLimiter(sampleRate)
    private val noiseGate = NoiseGate(sampleRate)
    private val overdrive = OverdriveDsp(sampleRate)
    private val distortion = DistortionDsp(sampleRate)
    private val booster = BoosterDsp(sampleRate)
    private val fuzz = FuzzDsp()
    private val ampSim = AmpSimulatorDsp(sampleRate)
    private val cabSim = CabinetSimulatorDsp(sampleRate)
    private val chorus = ChorusDsp(sampleRate)
    private val delay = DelayDsp(sampleRate)
    private val revXReverb = YamahaRevXReverbDsp(sampleRate)

    private val pitchDetector = PitchDetectorYin(sampleRate, 2048)
    val virtualGuitar = VirtualGuitarRiff(sampleRate)
    val irigConditioner = IrigConditioner(sampleRate)

    @Volatile
    private var activeEffects: List<EffectSetting> = PresetRepository.createDefaultChain()

    fun updatePreset(preset: GuitarPreset) {
        try {
            activeEffects = preset.effects
            applyParamsToDsp(preset.effects)
        } catch (e: Exception) {
            Log.e("AudioEngine", "Error updating preset", e)
        }
    }

    fun updateEffectParam(effectType: EffectType, paramName: String, value: Float) {
        try {
            val updated = activeEffects.map { fx ->
                if (fx.type == effectType) {
                    val newParams = fx.params.toMutableMap()
                    newParams[paramName] = value
                    fx.copy(params = newParams)
                } else fx
            }
            activeEffects = updated
            applyParamsToDsp(updated)
        } catch (e: Exception) {
            Log.e("AudioEngine", "Error updating effect param", e)
        }
    }

    fun toggleEffectBypass(effectType: EffectType) {
        val updated = activeEffects.map { fx ->
            if (fx.type == effectType) {
                fx.copy(isEnabled = !fx.isEnabled)
            } else fx
        }
        activeEffects = updated
    }

    fun reorderEffects(fromIndex: Int, toIndex: Int) {
        val list = activeEffects.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            activeEffects = list
        }
    }

    private fun applyParamsToDsp(effects: List<EffectSetting>) {
        for (fx in effects) {
            val p = fx.params
            when (fx.type) {
                EffectType.NOISE_GATE -> {
                    noiseGate.thresholdDb = p["threshold"] ?: -50.0f
                    noiseGate.releaseMs = p["release"] ?: 40.0f
                }
                EffectType.OVERDRIVE -> {
                    overdrive.drive = p["drive"] ?: 0.5f
                    overdrive.tone = p["tone"] ?: 0.5f
                    overdrive.level = p["level"] ?: 0.7f
                }
                EffectType.DISTORTION -> {
                    distortion.gain = p["gain"] ?: 0.7f
                    distortion.tone = p["tone"] ?: 0.5f
                    distortion.level = p["level"] ?: 0.7f
                }
                EffectType.BOOSTER -> {
                    booster.gain = p["gain"] ?: 2.0f
                    booster.bright = (p["bright"] ?: 1.0f) > 0.5f
                }
                EffectType.FUZZ -> {
                    fuzz.fuzz = p["fuzz"] ?: 0.8f
                    fuzz.level = p["level"] ?: 0.7f
                }
                EffectType.AMP_SIM -> {
                    ampSim.modelIndex = (p["model"] ?: 0.0f).toInt()
                    ampSim.gain = p["gain"] ?: 0.5f
                    ampSim.bass = p["bass"] ?: 0.5f
                    ampSim.mid = p["mid"] ?: 0.5f
                    ampSim.treble = p["treble"] ?: 0.5f
                    ampSim.presence = p["presence"] ?: 0.5f
                    ampSim.master = p["master"] ?: 0.8f
                }
                EffectType.CAB_SIM -> {
                    cabSim.cabIndex = (p["cab_type"] ?: 1.0f).toInt()
                    cabSim.micDistance = p["mic_distance"] ?: 0.25f
                }
                EffectType.CHORUS -> {
                    chorus.rateHz = p["rate"] ?: 1.2f
                    chorus.depth = p["depth"] ?: 0.5f
                    chorus.mix = p["mix"] ?: 0.35f
                }
                EffectType.DELAY -> {
                    delay.delayTimeMs = p["time"] ?: 350.0f
                    delay.feedback = p["feedback"] ?: 0.4f
                    delay.mix = p["mix"] ?: 0.3f
                }
                EffectType.REVERB -> {
                    revXReverb.typeIndex = (p["type"] ?: 1.0f).toInt()
                    revXReverb.decaySec = p["decay"] ?: 2.5f
                    revXReverb.mix = p["mix"] ?: 0.35f
                    revXReverb.damping = p["damping"] ?: 0.4f
                }
                else -> {}
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (isRunningFlag.get()) return
        isRunningFlag.set(true)

        val specs = deviceInfoHelper.getHardwareSpecs()
        // All DSP coefficients and delay buffers are initialized at 48 kHz.
        // Keep capture/playback at the same rate to avoid pitch and filter mismatch.
        val optimalSampleRate = sampleRate.toInt()
        val burstSize = specs.framesPerBuffer.coerceIn(64, 512)

        audioThread = Thread({
            try {
                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
            } catch (_: Exception) {}

            val hasMicPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            var recorder: AudioRecord? = null
            var track: AudioTrack? = null

            try {
                // Initialize AudioRecord safely if permission is granted
                if (hasMicPermission) {
                    val minInBuf = AudioRecord.getMinBufferSize(
                        optimalSampleRate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT
                    )
                    val inBufSize = if (minInBuf > 0) max(minInBuf, burstSize * 4) else burstSize * 4

                    val audioSources = buildList {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            add(MediaRecorder.AudioSource.VOICE_PERFORMANCE)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            add(MediaRecorder.AudioSource.UNPROCESSED)
                        }
                        add(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                        add(MediaRecorder.AudioSource.MIC)
                        add(MediaRecorder.AudioSource.DEFAULT)
                    }.distinct().toIntArray()

                    for (source in audioSources) {
                        try {
                            val candidate = AudioRecord(
                                source,
                                optimalSampleRate,
                                AudioFormat.CHANNEL_IN_MONO,
                                AudioFormat.ENCODING_PCM_16BIT,
                                inBufSize
                            )
                            if (candidate.state == AudioRecord.STATE_INITIALIZED) {
                                recorder = candidate
                                break
                            } else {
                                candidate.release()
                            }
                        } catch (e: Exception) {
                            Log.w("AudioEngine", "AudioRecord source $source failed: ${e.message}")
                        }
                    }
                }

                // Initialize AudioTrack safely
                val minOutBuf = AudioTrack.getMinBufferSize(
                    optimalSampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val outBufSize = if (minOutBuf > 0) max(minOutBuf, burstSize * 4) else burstSize * 4

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()

                val audioFormat = AudioFormat.Builder()
                    .setSampleRate(optimalSampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()

                track = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        AudioTrack.Builder()
                            .setAudioAttributes(audioAttributes)
                            .setAudioFormat(audioFormat)
                            .setBufferSizeInBytes(outBufSize)
                            .setTransferMode(AudioTrack.MODE_STREAM)
                            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                            .build()
                    } else {
                        AudioTrack(
                            audioAttributes,
                            audioFormat,
                            outBufSize,
                            AudioTrack.MODE_STREAM,
                            AudioManager.AUDIO_SESSION_ID_GENERATE
                        )
                    }
                } catch (e: Exception) {
                    Log.e("AudioEngine", "AudioTrack creation failed", e)
                    null
                }

                // Bind preferred device to 3.5mm Earphone Jack if iRig/Headset is plugged in
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val irigInput = deviceInfoHelper.getWiredHeadsetInputDevice()
                    if (irigInput != null && recorder != null) {
                        recorder.setPreferredDevice(irigInput)
                    }
                    val irigOutput = deviceInfoHelper.getWiredHeadsetOutputDevice()
                    if (irigOutput != null && track != null) {
                        track.setPreferredDevice(irigOutput)
                    }
                }

                if (recorder?.state == AudioRecord.STATE_INITIALIZED) {
                    try {
                        recorder.startRecording()
                    } catch (e: Exception) {
                        Log.e("AudioEngine", "Failed to start recording", e)
                        recorder.release()
                        recorder = null
                    }
                }

                if (track?.state == AudioTrack.STATE_INITIALIZED) {
                    try {
                        track.play()
                    } catch (e: Exception) {
                        Log.e("AudioEngine", "Failed to start playback track", e)
                        track.release()
                        track = null
                    }
                }

                val shortIn = ShortArray(burstSize)
                val floatBuf = FloatArray(burstSize)
                val virtualBuf = FloatArray(burstSize)
                val tunerAccumulator = FloatArray(2048)
                var tunerAccumIdx = 0

                val waveDisplay = FloatArray(64)
                var uiTick = 0

                val activeDevice = when {
                    deviceInfoHelper.isIrigConnected() -> "iRig 3.5mm Earphone Jack"
                    deviceInfoHelper.getConnectedDevices().any { it.isUsb } -> deviceInfoHelper.getConnectedDevices().first { it.isUsb }.productName
                    else -> "Built-in Audio"
                }

                _engineStats.value = AudioEngineStats(
                    isRunning = true,
                    sampleRate = optimalSampleRate,
                    bufferSizeFrames = burstSize,
                    measuredLatencyMs = specs.estimatedHardwareLatencyMs,
                    activeDeviceName = activeDevice,
                    statusMessage = if (recorder != null) "DSP Active ($activeDevice)" else "DSP Active (Virtual Riff Only)"
                )

                while (isRunningFlag.get()) {
                    var readSamples = 0
                    if (recorder != null && recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        readSamples = recorder.read(shortIn, 0, burstSize)
                    }

                    if (readSamples <= 0) {
                        readSamples = burstSize
                        for (i in 0 until burstSize) floatBuf[i] = 0.0f
                    } else {
                        for (i in 0 until readSamples) {
                            val sample = shortIn[i] / 32768.0f
                            floatBuf[i] = if (sample.isNaN() || sample.isInfinite()) 0.0f else sample
                        }
                    }

                    // Apply iRig 3.5mm TRRS conditioning (anti-feedback crosstalk filter & hum kill)
                    irigConditioner.process(floatBuf, readSamples)

                    // Blend with virtual guitar riff if enabled
                    if (virtualGuitar.isEnabled) {
                        virtualGuitar.generateNext(virtualBuf, readSamples)
                        for (i in 0 until readSamples) {
                            floatBuf[i] += virtualBuf[i]
                        }
                    }

                    var inRms = 0.0f
                    for (i in 0 until readSamples) {
                        val s = floatBuf[i]
                        inRms += s * s
                    }
                    inRms = if (inRms.isNaN() || inRms <= 0f) 0f else kotlin.math.sqrt(inRms / readSamples)

                    // Tuner Pitch Detection
                    if (isTunerActive) {
                        for (i in 0 until readSamples) {
                            tunerAccumulator[tunerAccumIdx++] = floatBuf[i]
                            if (tunerAccumIdx >= 2048) {
                                val result = pitchDetector.getPitch(tunerAccumulator)
                                _tunerResultFlow.value = result
                                tunerAccumIdx = 0
                            }
                        }
                    }

                    // Process DSP Effects Chain sequentially
                    val chain = activeEffects
                    for (fx in chain) {
                        if (!fx.isEnabled) continue
                        when (fx.type) {
                            EffectType.NOISE_GATE -> noiseGate.process(floatBuf, readSamples)
                            EffectType.OVERDRIVE -> overdrive.process(floatBuf, readSamples)
                            EffectType.DISTORTION -> distortion.process(floatBuf, readSamples)
                            EffectType.BOOSTER -> booster.process(floatBuf, readSamples)
                            EffectType.FUZZ -> fuzz.process(floatBuf, readSamples)
                            EffectType.AMP_SIM -> ampSim.process(floatBuf, readSamples)
                            EffectType.CAB_SIM -> cabSim.process(floatBuf, readSamples)
                            EffectType.CHORUS -> chorus.process(floatBuf, readSamples)
                            EffectType.DELAY -> delay.process(floatBuf, readSamples)
                            EffectType.REVERB -> revXReverb.process(floatBuf, readSamples)
                            else -> {}
                        }
                    }

                    // Apply output gain, then a smooth -1 dBFS peak limiter.
                    val effVol = if (isMuted) 0.0f else masterVolume.coerceIn(0.0f, 2.5f)
                    var outRms = 0.0f
                    var clipping = false

                    for (i in 0 until readSamples) {
                        val amplified = floatBuf[i] * effVol
                        if (amplified.isFinite() && kotlin.math.abs(amplified) > 0.89125f) clipping = true
                        val outVal = outputLimiter.process(amplified)
                        shortIn[i] = (outVal * 32767.0f).toInt().toShort()
                        outRms += outVal * outVal
                    }
                    outRms = if (outRms.isNaN() || outRms <= 0f) 0f else kotlin.math.sqrt(outRms / readSamples)

                    // Write to audio output
                    if (track != null && track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        track.write(shortIn, 0, readSamples)
                    } else {
                        // Sleep slightly if no physical audio track to prevent 100% CPU loop
                        Thread.sleep(10)
                    }

                    // Update UI stats periodically
                    uiTick++
                    if (uiTick % 6 == 0) {
                        val step = max(1, readSamples / 64)
                        for (k in 0 until 64) {
                            val idx = (k * step).coerceAtMost(readSamples - 1)
                            val wVal = floatBuf[idx]
                            waveDisplay[k] = if (wVal.isNaN() || wVal.isInfinite()) 0f else wVal
                        }
                        _waveformFlow.value = waveDisplay.copyOf()

                        _engineStats.value = _engineStats.value.copy(
                            inputLevelRms = inRms.coerceIn(0f, 1f),
                            outputLevelRms = outRms.coerceIn(0f, 1f),
                            isClipping = clipping
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("AudioEngine", "Audio loop exception", e)
            } finally {
                try {
                    if (recorder != null && recorder.state == AudioRecord.STATE_INITIALIZED) {
                        if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                            recorder.stop()
                        }
                        recorder.release()
                    }
                } catch (e: Exception) {
                    Log.e("AudioEngine", "Error releasing recorder", e)
                }

                try {
                    if (track != null && track.state == AudioTrack.STATE_INITIALIZED) {
                        if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                            track.stop()
                        }
                        track.release()
                    }
                } catch (e: Exception) {
                    Log.e("AudioEngine", "Error releasing track", e)
                }

                _engineStats.value = _engineStats.value.copy(
                    isRunning = false,
                    inputLevelRms = 0f,
                    outputLevelRms = 0f,
                    statusMessage = "Engine Stopped"
                )
            }
        }, "GuitarFxAudioThread")

        audioThread?.start()
    }

    fun stop() {
        isRunningFlag.set(false)
        try {
            audioThread?.interrupt()
            audioThread?.join(300)
        } catch (_: Exception) {}
        audioThread = null
        _engineStats.value = _engineStats.value.copy(isRunning = false, inputLevelRms = 0f, outputLevelRms = 0f)
    }

    fun getConnectedDevices(): List<ConnectedAudioDevice> = deviceInfoHelper.getConnectedDevices()
    fun getHardwareSpecs(): HardwareAudioSpecs = deviceInfoHelper.getHardwareSpecs()
}
