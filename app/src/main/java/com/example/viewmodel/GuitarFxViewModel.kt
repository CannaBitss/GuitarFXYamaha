package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioEngine
import com.example.audio.ConnectedAudioDevice
import com.example.audio.HardwareAudioSpecs
import com.example.audio.dsp.RiffStyle
import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GuitarFxViewModel(application: Application) : AndroidViewModel(application) {
    val audioEngine = AudioEngine(application)

    private val prefs = application.getSharedPreferences("guitar_fx_presets", Context.MODE_PRIVATE)

    private val _presetsList = MutableStateFlow<List<GuitarPreset>>(emptyList())
    val presetsList = _presetsList.asStateFlow()

    private val _currentPreset = MutableStateFlow(PresetRepository.factoryPresets[0])
    val currentPreset = _currentPreset.asStateFlow()

    private val _effectsChain = MutableStateFlow<List<EffectSetting>>(emptyList())
    val effectsChain = _effectsChain.asStateFlow()

    private val _isTunerOpen = MutableStateFlow(false)
    val isTunerOpen = _isTunerOpen.asStateFlow()

    private val _isVirtualRiffEnabled = MutableStateFlow(false)
    val isVirtualRiffEnabled = _isVirtualRiffEnabled.asStateFlow()

    private val _selectedRiffStyle = MutableStateFlow(RiffStyle.ROCK_RIFF)
    val selectedRiffStyle = _selectedRiffStyle.asStateFlow()

    private val _showUsbSheet = MutableStateFlow(false)
    val showUsbSheet = _showUsbSheet.asStateFlow()

    private val _connectedDevices = MutableStateFlow<List<ConnectedAudioDevice>>(emptyList())
    val connectedDevices = _connectedDevices.asStateFlow()

    private val _hardwareSpecs = MutableStateFlow<HardwareAudioSpecs?>(null)
    val hardwareSpecs = _hardwareSpecs.asStateFlow()

    private val _masterVolume = MutableStateFlow(1.0f)
    val masterVolume = _masterVolume.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted = _isMuted.asStateFlow()

    // iRig 3.5mm Earphone Jack state
    private val _isIrigConnected = MutableStateFlow(false)
    val isIrigConnected = _isIrigConnected.asStateFlow()

    private val _isIrigModeEnabled = MutableStateFlow(true)
    val isIrigModeEnabled = _isIrigModeEnabled.asStateFlow()

    private val _irigInputGain = MutableStateFlow(1.4f)
    val irigInputGain = _irigInputGain.asStateFlow()

    private val _irigAntiFeedback = MutableStateFlow(true)
    val irigAntiFeedback = _irigAntiFeedback.asStateFlow()

    private val _irigHumReduction = MutableStateFlow(true)
    val irigHumReduction = _irigHumReduction.asStateFlow()

    // Tap tempo helper
    private var lastTapTimeMs = 0L

    init {
        loadPresets()
        selectPreset(PresetRepository.factoryPresets[0])
        refreshAudioDevices()
    }

    fun startEngine() {
        audioEngine.start()
    }

    fun stopEngine() {
        audioEngine.stop()
    }

    fun toggleEnginePower() {
        if (audioEngine.engineStats.value.isRunning) {
            audioEngine.stop()
        } else {
            audioEngine.start()
        }
    }

    fun refreshAudioDevices() {
        val devs = audioEngine.getConnectedDevices()
        _connectedDevices.value = devs
        _hardwareSpecs.value = audioEngine.getHardwareSpecs()
        _isIrigConnected.value = devs.any { it.isIrigJack && it.isInput }
    }

    fun setIrigModeEnabled(enabled: Boolean) {
        _isIrigModeEnabled.value = enabled
        audioEngine.irigConditioner.isEnabled = enabled
    }

    fun setIrigInputGain(gain: Float) {
        _irigInputGain.value = gain
        audioEngine.irigConditioner.inputGain = gain
    }

    fun toggleIrigAntiFeedback() {
        val next = !_irigAntiFeedback.value
        _irigAntiFeedback.value = next
        audioEngine.irigConditioner.antiFeedbackEnabled = next
    }

    fun toggleIrigHumReduction() {
        val next = !_irigHumReduction.value
        _irigHumReduction.value = next
        audioEngine.irigConditioner.humReductionEnabled = next
    }

    private fun loadPresets() {
        val list = mutableListOf<GuitarPreset>()
        list.addAll(PresetRepository.factoryPresets)

        val customKeys = prefs.getStringSet("custom_preset_ids", emptySet()) ?: emptySet()
        for (id in customKeys) {
            val json = prefs.getString("preset_$id", null)
            if (json != null) {
                PresetRepository.jsonToPreset(json)?.let { list.add(it) }
            }
        }
        _presetsList.value = list
    }

    fun selectPreset(preset: GuitarPreset) {
        _currentPreset.value = preset
        _effectsChain.value = preset.effects
        audioEngine.updatePreset(preset)
    }

    fun saveCurrentAsNewPreset(name: String, description: String) {
        val newId = "user_${System.currentTimeMillis()}"
        val newPreset = GuitarPreset(
            id = newId,
            name = name,
            description = description,
            isFactory = false,
            effects = _effectsChain.value
        )
        val json = PresetRepository.presetToJson(newPreset)
        val customKeys = (prefs.getStringSet("custom_preset_ids", emptySet()) ?: emptySet()).toMutableSet()
        customKeys.add(newId)

        prefs.edit()
            .putStringSet("custom_preset_ids", customKeys)
            .putString("preset_$newId", json)
            .apply()

        loadPresets()
        selectPreset(newPreset)
    }

    fun deletePreset(presetId: String) {
        val customKeys = (prefs.getStringSet("custom_preset_ids", emptySet()) ?: emptySet()).toMutableSet()
        customKeys.remove(presetId)
        prefs.edit()
            .putStringSet("custom_preset_ids", customKeys)
            .remove("preset_$presetId")
            .apply()

        loadPresets()
        if (_currentPreset.value.id == presetId) {
            selectPreset(PresetRepository.factoryPresets[0])
        }
    }

    fun toggleEffectBypass(effectType: EffectType) {
        audioEngine.toggleEffectBypass(effectType)
        _effectsChain.value = _effectsChain.value.map { fx ->
            if (fx.type == effectType) fx.copy(isEnabled = !fx.isEnabled) else fx
        }
    }

    fun updateParam(effectType: EffectType, paramName: String, value: Float) {
        audioEngine.updateEffectParam(effectType, paramName, value)
        _effectsChain.value = _effectsChain.value.map { fx ->
            if (fx.type == effectType) {
                val updated = fx.params.toMutableMap()
                updated[paramName] = value
                fx.copy(params = updated)
            } else fx
        }
    }

    fun moveEffect(from: Int, to: Int) {
        if (from in _effectsChain.value.indices && to in _effectsChain.value.indices) {
            val list = _effectsChain.value.toMutableList()
            val item = list.removeAt(from)
            list.add(to, item)
            _effectsChain.value = list
            audioEngine.reorderEffects(from, to)
        }
    }

    fun toggleTuner() {
        val next = !_isTunerOpen.value
        _isTunerOpen.value = next
        audioEngine.isTunerActive = next
    }

    fun toggleVirtualRiff() {
        val next = !_isVirtualRiffEnabled.value
        _isVirtualRiffEnabled.value = next
        audioEngine.virtualGuitar.isEnabled = next
    }

    fun setRiffStyle(style: RiffStyle) {
        _selectedRiffStyle.value = style
        audioEngine.virtualGuitar.style = style
    }

    fun setMasterVolume(vol: Float) {
        _masterVolume.value = vol
        audioEngine.masterVolume = vol
    }

    fun toggleMute() {
        val next = !_isMuted.value
        _isMuted.value = next
        audioEngine.isMuted = next
    }

    fun setShowUsbSheet(show: Boolean) {
        if (show) refreshAudioDevices()
        _showUsbSheet.value = show
    }

    fun tapTempo() {
        val now = System.currentTimeMillis()
        if (lastTapTimeMs > 0) {
            val interval = now - lastTapTimeMs
            if (interval in 150..2000) {
                updateParam(EffectType.DELAY, "time", interval.toFloat())
            }
        }
        lastTapTimeMs = now
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.stop()
    }
}
