#include <jni.h>
#include "OboeAudioEngine.h"

static OboeAudioEngine* sEngine = nullptr;

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_example_audio_NativeGuitarEngine_nativeStart(JNIEnv *env, jobject thiz) {
    if (!sEngine) {
        sEngine = new OboeAudioEngine();
    }
    return sEngine->start() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_example_audio_NativeGuitarEngine_nativeStop(JNIEnv *env, jobject thiz) {
    if (sEngine) {
        sEngine->stop();
    }
}

JNIEXPORT void JNICALL
Java_com_example_audio_NativeGuitarEngine_nativeSetOverdriveParams(
        JNIEnv *env, jobject thiz, jfloat drive, jfloat tone, jfloat level, jboolean bypass) {
    if (sEngine) {
        sEngine->setOverdriveDrive(drive);
        sEngine->setOverdriveTone(tone);
        sEngine->setOverdriveLevel(level);
        sEngine->setOverdriveBypass(bypass);
    }
}

JNIEXPORT void JNICALL
Java_com_example_audio_NativeGuitarEngine_nativeSetDelayParams(
        JNIEnv *env, jobject thiz, jfloat timeMs, jfloat feedback, jfloat mix, jboolean bypass) {
    if (sEngine) {
        sEngine->setDelayTime(timeMs);
        sEngine->setDelayFeedback(feedback);
        sEngine->setDelayMix(mix);
        sEngine->setDelayBypass(bypass);
    }
}

JNIEXPORT void JNICALL
Java_com_example_audio_NativeGuitarEngine_nativeSetMasterVolume(
        JNIEnv *env, jobject thiz, jfloat vol) {
    if (sEngine) {
        sEngine->setMasterVolume(vol);
    }
}

JNIEXPORT jfloat JNICALL
Java_com_example_audio_NativeGuitarEngine_nativeGetInputRms(JNIEnv *env, jobject thiz) {
    return sEngine ? sEngine->getLastInputRms() : 0.0f;
}

JNIEXPORT jfloat JNICALL
Java_com_example_audio_NativeGuitarEngine_nativeGetOutputRms(JNIEnv *env, jobject thiz) {
    return sEngine ? sEngine->getLastOutputRms() : 0.0f;
}

} // extern "C"
