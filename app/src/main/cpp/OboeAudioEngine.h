#pragma once

#include <memory>
#include <vector>
#include <atomic>
#include <string>
#include "OverdriveEffect.h"
#include "DelayEffect.h"

// Forward declaration or include oboe if available in NDK
// #include <oboe/Oboe.h>

/**
 * Architecture of the Ultra-Low Latency Guitar Audio Engine using Oboe.
 * Implements Full-Duplex Input/Output audio streams with zero-allocation
 * real-time callbacks and lock-free thread safety.
 */
class OboeAudioEngine {
public:
    OboeAudioEngine();
    ~OboeAudioEngine();

    bool start();
    void stop();
    bool isRunning() const { return mIsRunning.load(); }

    // Real-time audio processing callback (called on real-time audio thread)
    void processAudio(float* inBuffer, float* outBuffer, int numFrames);

    // Thread-safe effect parameter control from JNI
    void setOverdriveDrive(float val) { mOverdrive.setDrive(val); }
    void setOverdriveTone(float val) { mOverdrive.setTone(val); }
    void setOverdriveLevel(float val) { mOverdrive.setLevel(val); }
    void setOverdriveBypass(bool bypass) { mOverdrive.setEnabled(!bypass); }

    void setDelayTime(float ms) { mDelay.setTimeMs(ms); }
    void setDelayFeedback(float fb) { mDelay.setFeedback(fb); }
    void setDelayMix(float mix) { mDelay.setMix(mix); }
    void setDelayBypass(bool bypass) { mDelay.setEnabled(!bypass); }

    void setMasterVolume(float vol) { mMasterVolume.store(vol, std::memory_order_relaxed); }
    float getMasterVolume() const { return mMasterVolume.load(std::memory_order_relaxed); }

    // Real-time telemetry for UI oscilloscope and VU meters
    float getLastInputRms() const { return mInputRms.load(std::memory_order_relaxed); }
    float getLastOutputRms() const { return mOutputRms.load(std::memory_order_relaxed); }

private:
    float mSampleRate{48000.0f};
    int mBurstFrames{192};
    std::atomic<bool> mIsRunning{false};
    std::atomic<float> mMasterVolume{1.0f};

    std::atomic<float> mInputRms{0.0f};
    std::atomic<float> mOutputRms{0.0f};

    // DSP chain components
    OverdriveEffect mOverdrive;
    DelayEffect mDelay;
};
