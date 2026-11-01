#include "OboeAudioEngine.h"
#include <cmath>
#include <cstring>
#include <android/log.h>

#define TAG "GuitarFX_NativeEngine"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

OboeAudioEngine::OboeAudioEngine()
    : mSampleRate(48000.0f),
      mOverdrive(48000.0f),
      mDelay(48000.0f) {
}

OboeAudioEngine::~OboeAudioEngine() {
    stop();
}

bool OboeAudioEngine::start() {
    if (mIsRunning.load()) return true;

    LOGD("Starting Ultra-Low Latency Oboe Full-Duplex Engine at %f Hz", mSampleRate);

    /*
     * In an active Oboe NDK project, configure the audio streams as follows:
     *
     * 1. OUTPUT STREAM BUILDER:
     * oboe::AudioStreamBuilder outBuilder;
     * outBuilder.setDirection(oboe::Direction::Output)
     *          ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
     *          ->setSharingMode(oboe::SharingMode::Exclusive)
     *          ->setFormat(oboe::AudioFormat::Float)
     *          ->setChannelCount(oboe::ChannelCount::Mono)
     *          ->setSampleRate(optimalSampleRate)
     *          ->setCallback(this);
     *
     * 2. INPUT STREAM BUILDER (Guitar Input / USB Audio Interface):
     * oboe::AudioStreamBuilder inBuilder;
     * inBuilder.setDirection(oboe::Direction::Input)
     *         ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
     *         ->setSharingMode(oboe::SharingMode::Exclusive)
     *         ->setFormat(oboe::AudioFormat::Float)
     *         ->setChannelCount(oboe::ChannelCount::Mono)
     *         ->setInputPreset(oboe::InputPreset::VoiceRecognition); // bypass Android AGC & EQ
     */

    mIsRunning.store(true);
    return true;
}

void OboeAudioEngine::stop() {
    if (!mIsRunning.load()) return;
    mIsRunning.store(false);
    LOGD("Oboe Audio Engine stopped");
}

/**
 * Real-time audio rendering loop.
 * CRITICAL RULE: NO memory allocations (malloc/new), NO blocking I/O, NO mutex locks.
 * All parameter transfers use std::atomic or lock-free SPSC circular buffers.
 */
void OboeAudioEngine::processAudio(float* inBuffer, float* outBuffer, int numFrames) {
    if (!mIsRunning.load(std::memory_order_relaxed)) {
        std::memset(outBuffer, 0, numFrames * sizeof(float));
        return;
    }

    // 1. Calculate input RMS for visualizer / noise gate
    float inEnergy = 0.0f;
    for (int i = 0; i < numFrames; ++i) {
        float s = inBuffer[i];
        outBuffer[i] = s;
        inEnergy += s * s;
    }
    mInputRms.store(std::sqrt(inEnergy / numFrames), std::memory_order_relaxed);

    // 2. Sequential modular DSP processing:
    mOverdrive.process(outBuffer, numFrames);
    mDelay.process(outBuffer, numFrames);

    // 3. Apply master gain and soft-clip ceiling protection
    const float master = mMasterVolume.load(std::memory_order_relaxed);
    float outEnergy = 0.0f;
    for (int i = 0; i < numFrames; ++i) {
        float sample = outBuffer[i] * master;
        // Fast soft-limiter to prevent digital harsh clipping
        if (sample > 1.0f) sample = 1.0f;
        else if (sample < -1.0f) sample = -1.0f;
        outBuffer[i] = sample;
        outEnergy += sample * sample;
    }
    mOutputRms.store(std::sqrt(outEnergy / numFrames), std::memory_order_relaxed);
}
