package com.example.audio

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log

data class ConnectedAudioDevice(
    val id: Int,
    val productName: String,
    val type: Int,
    val typeName: String,
    val isUsb: Boolean,
    val isIrigJack: Boolean,
    val isInput: Boolean
)

data class HardwareAudioSpecs(
    val optimalSampleRate: Int,
    val framesPerBuffer: Int,
    val estimatedHardwareLatencyMs: Float,
    val isLowLatencyFeatureSupported: Boolean,
    val isProAudioFeatureSupported: Boolean
)

class AudioDeviceInfoHelper(
    private val context: Context,
    private val onDevicesChanged: (() -> Unit)? = null
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private var deviceCallback: AudioDeviceCallback? = null

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audioManager != null) {
            try {
                deviceCallback = object : AudioDeviceCallback() {
                    override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                        onDevicesChanged?.invoke()
                    }

                    override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                        onDevicesChanged?.invoke()
                    }
                }
                audioManager.registerAudioDeviceCallback(deviceCallback, null)
            } catch (e: Exception) {
                Log.w("AudioDeviceInfoHelper", "Could not register audio device callback", e)
            }
        }
    }

    fun release() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audioManager != null && deviceCallback != null) {
            try {
                audioManager.unregisterAudioDeviceCallback(deviceCallback)
            } catch (_: Exception) {}
        }
    }

    fun isIrigConnected(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audioManager != null) {
            try {
                val devices = audioManager.getDevices(AudioManager.GET_DEVICES_ALL)
                for (dev in devices) {
                    if (dev.type == AudioDeviceInfo.TYPE_WIRED_HEADSET) {
                        return true
                    }
                }
            } catch (_: Exception) {}
        }
        return false
    }

    fun getWiredHeadsetInputDevice(): AudioDeviceInfo? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audioManager != null) {
            try {
                val devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
                for (dev in devices) {
                    if (dev.type == AudioDeviceInfo.TYPE_WIRED_HEADSET) {
                        return dev
                    }
                }
            } catch (_: Exception) {}
        }
        return null
    }

    fun getWiredHeadsetOutputDevice(): AudioDeviceInfo? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audioManager != null) {
            try {
                val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                for (dev in devices) {
                    if (dev.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                        dev.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES) {
                        return dev
                    }
                }
            } catch (_: Exception) {}
        }
        return null
    }

    fun getHardwareSpecs(): HardwareAudioSpecs {
        return try {
            val sampleRateStr = audioManager?.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
            val framesStr = audioManager?.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)

            val sampleRate = sampleRateStr?.toIntOrNull()?.takeIf { it in 8000..192000 } ?: 48000
            val framesPerBuffer = framesStr?.toIntOrNull()?.takeIf { it in 16..4096 } ?: 192

            val latencyMs = (framesPerBuffer.toFloat() / sampleRate.toFloat()) * 1000.0f * 2.0f

            val pm = context.packageManager
            val hasLowLatency = pm?.hasSystemFeature("android.hardware.audio.low_latency") ?: false
            val hasProAudio = pm?.hasSystemFeature("android.hardware.audio.pro") ?: false

            HardwareAudioSpecs(
                optimalSampleRate = sampleRate,
                framesPerBuffer = framesPerBuffer,
                estimatedHardwareLatencyMs = latencyMs,
                isLowLatencyFeatureSupported = hasLowLatency,
                isProAudioFeatureSupported = hasProAudio
            )
        } catch (e: Exception) {
            Log.e("AudioDeviceInfoHelper", "Error getting hardware specs", e)
            HardwareAudioSpecs(
                optimalSampleRate = 48000,
                framesPerBuffer = 192,
                estimatedHardwareLatencyMs = 8.0f,
                isLowLatencyFeatureSupported = true,
                isProAudioFeatureSupported = false
            )
        }
    }

    fun getConnectedDevices(): List<ConnectedAudioDevice> {
        val list = mutableListOf<ConnectedAudioDevice>()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audioManager != null) {
                val devices = audioManager.getDevices(AudioManager.GET_DEVICES_ALL)
                for (dev in devices) {
                    val isUsbHeadset = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                                       dev.type == AudioDeviceInfo.TYPE_USB_HEADSET
                    val isUsb = dev.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                                isUsbHeadset ||
                                dev.type == AudioDeviceInfo.TYPE_USB_ACCESSORY
                    val isIrigJack = dev.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                                     dev.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES

                    val typeName = when {
                        dev.type == AudioDeviceInfo.TYPE_WIRED_HEADSET -> "3.5mm Earphone Jack (iRig Input/Output)"
                        dev.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "3.5mm Headphones (Output Only)"
                        dev.type == AudioDeviceInfo.TYPE_USB_DEVICE -> "USB Audio Interface"
                        isUsbHeadset -> "USB Headset / OTG Adapter"
                        dev.type == AudioDeviceInfo.TYPE_BUILTIN_MIC -> "Built-in Microphone"
                        dev.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Built-in Speaker"
                        dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth Audio"
                        else -> "Audio Device (${dev.type})"
                    }

                    val name = try {
                        if (dev.type == AudioDeviceInfo.TYPE_WIRED_HEADSET) {
                            "iRig 3.5mm Guitar Adapter"
                        } else {
                            dev.productName?.toString()?.ifBlank { typeName } ?: typeName
                        }
                    } catch (_: Exception) {
                        typeName
                    }

                    list.add(
                        ConnectedAudioDevice(
                            id = dev.id,
                            productName = name,
                            type = dev.type,
                            typeName = typeName,
                            isUsb = isUsb,
                            isIrigJack = isIrigJack,
                            isInput = dev.isSource
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("AudioDeviceInfoHelper", "Error enumerating audio devices", e)
        }

        if (list.isEmpty()) {
            list.add(
                ConnectedAudioDevice(
                    id = 0,
                    productName = "Default Device Audio",
                    type = 0,
                    typeName = "Built-in Audio",
                    isUsb = false,
                    isIrigJack = false,
                    isInput = true
                )
            )
        }
        return list
    }
}
