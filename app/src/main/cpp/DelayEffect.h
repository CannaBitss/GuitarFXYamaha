#pragma once
#include <vector>
#include <atomic>
#include <algorithm>

/**
 * Real-time Tape Delay with analog damping filter and tap tempo synchronization
 */
class DelayEffect {
public:
    DelayEffect(float sampleRate) : mSampleRate(sampleRate) {
        mMaxDelaySamples = static_cast<int>(sampleRate * 2.0f); // 2 seconds max
        mBuffer.resize(mMaxDelaySamples, 0.0f);
        mWriteIdx = 0;
    }

    void setTimeMs(float ms) { mDelayTimeMs.store(std::clamp(ms, 10.0f, 2000.0f), std::memory_order_relaxed); }
    void setFeedback(float fb) { mFeedback.store(std::clamp(fb, 0.0f, 0.95f), std::memory_order_relaxed); }
    void setMix(float m) { mMix.store(std::clamp(m, 0.0f, 1.0f), std::memory_order_relaxed); }
    void setEnabled(bool e) { mEnabled.store(e, std::memory_order_relaxed); }

    inline void process(float* buffer, int numFrames) {
        if (!mEnabled.load(std::memory_order_relaxed)) return;

        const float timeMs = mDelayTimeMs.load(std::memory_order_relaxed);
        const float feedback = mFeedback.load(std::memory_order_relaxed);
        const float mix = mMix.load(std::memory_order_relaxed);

        const int delaySamples = std::clamp(static_cast<int>(mSampleRate * (timeMs / 1000.0f)), 1, mMaxDelaySamples - 1);

        for (int i = 0; i < numFrames; ++i) {
            float in = buffer[i];
            int readIdx = mWriteIdx - delaySamples;
            if (readIdx < 0) readIdx += mMaxDelaySamples;

            float delayed = mBuffer[readIdx];
            // Analog tape high-frequency tape head loss damping
            mDampZ += 0.25f * (delayed - mDampZ);
            float dampedFeedback = mDampZ;

            mBuffer[mWriteIdx] = in + dampedFeedback * feedback;
            buffer[i] = in + delayed * mix;

            mWriteIdx = (mWriteIdx + 1) % mMaxDelaySamples;
        }
    }

private:
    float mSampleRate;
    int mMaxDelaySamples;
    std::vector<float> mBuffer;
    int mWriteIdx;
    float mDampZ{0.0f};

    std::atomic<float> mDelayTimeMs{350.0f};
    std::atomic<float> mFeedback{0.4f};
    std::atomic<float> mMix{0.3f};
    std::atomic<bool> mEnabled{true};
};
