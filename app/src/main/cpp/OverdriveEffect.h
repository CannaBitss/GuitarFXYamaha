#pragma once
#include <cmath>
#include <atomic>

/**
 * High-performance Overdrive DSP modeled after classic Tube Screamer TS808
 * - High-pass pre-emphasis at 720Hz
 * - Non-linear soft-clipping waveshaping with tube asymmetry
 * - Variable active tone low-pass filter
 * - Thread-safe atomic parameter updates
 */
class OverdriveEffect {
public:
    OverdriveEffect(float sampleRate) : mSampleRate(sampleRate) {
        updatePreFilter();
        updateToneFilter();
    }

    void setDrive(float d) { mDrive.store(d, std::memory_order_relaxed); }
    void setTone(float t) { 
        mTone.store(t, std::memory_order_relaxed); 
        updateToneFilter();
    }
    void setLevel(float l) { mLevel.store(l, std::memory_order_relaxed); }
    void setEnabled(bool e) { mEnabled.store(e, std::memory_order_relaxed); }

    inline void process(float* buffer, int numFrames) {
        if (!mEnabled.load(std::memory_order_relaxed)) return;

        const float drive = mDrive.load(std::memory_order_relaxed);
        const float level = mLevel.load(std::memory_order_relaxed) * 1.5f;
        const float gain = 1.0f + drive * 28.0f;

        for (int i = 0; i < numFrames; ++i) {
            float in = buffer[i];
            // Pre-emphasis filter
            float pre = processPreHp(in);
            float boosted = pre * gain;

            // Asymmetric soft clipping transfer curve (vacuum tube diode behavior)
            float shaped = 0.0f;
            if (boosted > 0.0f) {
                shaped = std::tanh(boosted);
            } else {
                shaped = std::tanh(boosted * 0.85f) / 0.85f;
            }

            // Post low-pass tone shaping
            float filtered = processToneLp(shaped);

            // 85% wet + 15% clean direct blend (TS clean bleed characteristic)
            buffer[i] = (filtered * 0.85f + in * 0.15f) * level;
        }
    }

private:
    float mSampleRate;
    std::atomic<float> mDrive{0.5f};
    std::atomic<float> mTone{0.5f};
    std::atomic<float> mLevel{0.7f};
    std::atomic<bool> mEnabled{true};

    // Pre High-Pass filter coefficients (720Hz)
    float mPreA1{0.0f}, mPreB0{1.0f}, mPreB1{-1.0f};
    float mPreZ{0.0f};

    // Tone Low-Pass filter coefficients
    float mToneAlpha{0.1f};
    float mToneZ{0.0f};

    void updatePreFilter() {
        float fc = 720.0f;
        float w = 2.0f * M_PI * fc / mSampleRate;
        mPreA1 = -std::exp(-w);
        mPreB0 = (1.0f - mPreA1) * 0.5f;
        mPreB1 = -mPreB0;
    }

    void updateToneFilter() {
        float tone = mTone.load(std::memory_order_relaxed);
        float fc = 1200.0f + tone * 4500.0f;
        mToneAlpha = 1.0f - std::exp(-2.0f * M_PI * fc / mSampleRate);
    }

    inline float processPreHp(float in) {
        float out = mPreB0 * in + mPreZ;
        mPreZ = mPreB1 * in - mPreA1 * out;
        return out;
    }

    inline float processToneLp(float in) {
        mToneZ += mToneAlpha * (in - mToneZ);
        return mToneZ;
    }
};
