package com.example.audio

/**
 * Native interface to the C++/Oboe audio engine.
 * When built with Android NDK and CMake, calls directly into native methods.
 */
object NativeGuitarEngine {
    private var isLibraryLoaded = false

    init {
        try {
            System.loadLibrary("guitar_fx_engine")
            isLibraryLoaded = true
        } catch (_: UnsatisfiedLinkError) {
            // Falls back smoothly to pure Kotlin low-latency engine
            isLibraryLoaded = false
        }
    }

    fun isNativeSupported(): Boolean = isLibraryLoaded

    external fun nativeStart(): Boolean
    external fun nativeStop()
    external fun nativeSetOverdriveParams(drive: Float, tone: Float, level: Float, bypass: Boolean)
    external fun nativeSetDelayParams(timeMs: Float, feedback: Float, mix: Float, bypass: Boolean)
    external fun nativeSetMasterVolume(vol: Float)
    external fun nativeGetInputRms(): Float
    external fun nativeGetOutputRms(): Float
}
