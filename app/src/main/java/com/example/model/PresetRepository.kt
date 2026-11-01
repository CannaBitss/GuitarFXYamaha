package com.example.model

import org.json.JSONArray
import org.json.JSONObject

object PresetRepository {

    val factoryPresets: List<GuitarPreset> = listOf(
        GuitarPreset(
            id = "preset_clean",
            name = "Boutique Clean",
            description = "Glassy chime with subtle chorus and lush REV-X hall reverb",
            isFactory = true,
            effects = listOf(
                EffectSetting("fx_ng", EffectType.NOISE_GATE, true, mapOf("threshold" to -48f, "release" to 120f)),
                EffectSetting("fx_boost", EffectType.BOOSTER, true, mapOf("gain" to 2.5f, "bright" to 1.0f)),
                EffectSetting("fx_amp", EffectType.AMP_SIM, true, mapOf("model" to 0f, "gain" to 0.35f, "bass" to 0.55f, "mid" to 0.50f, "treble" to 0.65f, "presence" to 0.60f, "master" to 0.80f)),
                EffectSetting("fx_cab", EffectType.CAB_SIM, true, mapOf("cab_type" to 0f, "mic_distance" to 0.3f)),
                EffectSetting("fx_chorus", EffectType.CHORUS, true, mapOf("rate" to 1.2f, "depth" to 0.45f, "mix" to 0.35f)),
                EffectSetting("fx_delay", EffectType.DELAY, false, mapOf("time" to 380f, "feedback" to 0.35f, "mix" to 0.25f)),
                EffectSetting("fx_reverb", EffectType.REVERB, true, mapOf("type" to 1f, "decay" to 2.8f, "mix" to 0.38f, "damping" to 0.4f))
            )
        ),
        GuitarPreset(
            id = "preset_crunch",
            name = "THR British Crunch",
            description = "Classic British stack breakup with punchy tube screamer drive",
            isFactory = true,
            effects = listOf(
                EffectSetting("fx_ng", EffectType.NOISE_GATE, true, mapOf("threshold" to -46f, "release" to 110f)),
                EffectSetting("fx_ts", EffectType.OVERDRIVE, true, mapOf("drive" to 0.55f, "tone" to 0.60f, "level" to 0.70f)),
                EffectSetting("fx_amp", EffectType.AMP_SIM, true, mapOf("model" to 1f, "gain" to 0.65f, "bass" to 0.50f, "mid" to 0.65f, "treble" to 0.60f, "presence" to 0.55f, "master" to 0.85f)),
                EffectSetting("fx_cab", EffectType.CAB_SIM, true, mapOf("cab_type" to 1f, "mic_distance" to 0.2f)),
                EffectSetting("fx_delay", EffectType.DELAY, true, mapOf("time" to 280f, "feedback" to 0.25f, "mix" to 0.20f)),
                EffectSetting("fx_reverb", EffectType.REVERB, true, mapOf("type" to 0f, "decay" to 1.5f, "mix" to 0.25f, "damping" to 0.5f))
            )
        ),
        GuitarPreset(
            id = "preset_lead",
            name = "Yamaha 80s Lead",
            description = "Sustained melodic high-gain singing solo with stereo analog delay",
            isFactory = true,
            effects = listOf(
                EffectSetting("fx_ng", EffectType.NOISE_GATE, true, mapOf("threshold" to -43f, "release" to 100f)),
                EffectSetting("fx_dist", EffectType.DISTORTION, true, mapOf("gain" to 0.65f, "tone" to 0.60f, "level" to 0.75f)),
                EffectSetting("fx_amp", EffectType.AMP_SIM, true, mapOf("model" to 2f, "gain" to 0.80f, "bass" to 0.55f, "mid" to 0.70f, "treble" to 0.65f, "presence" to 0.70f, "master" to 0.90f)),
                EffectSetting("fx_cab", EffectType.CAB_SIM, true, mapOf("cab_type" to 2f, "mic_distance" to 0.25f)),
                EffectSetting("fx_chorus", EffectType.CHORUS, false, mapOf("rate" to 0.8f, "depth" to 0.30f, "mix" to 0.25f)),
                EffectSetting("fx_delay", EffectType.DELAY, true, mapOf("time" to 420f, "feedback" to 0.45f, "mix" to 0.40f)),
                EffectSetting("fx_reverb", EffectType.REVERB, true, mapOf("type" to 1f, "decay" to 3.2f, "mix" to 0.35f, "damping" to 0.3f))
            )
        ),
        GuitarPreset(
            id = "preset_modern",
            name = "Modern Djent Chug",
            description = "Ultra-tight noise gate, aggressive scooped modern distortion",
            isFactory = true,
            effects = listOf(
                EffectSetting("fx_ng", EffectType.NOISE_GATE, true, mapOf("threshold" to -40f, "release" to 100f)),
                EffectSetting("fx_ts", EffectType.OVERDRIVE, true, mapOf("drive" to 0.20f, "tone" to 0.75f, "level" to 0.90f)),
                EffectSetting("fx_amp", EffectType.AMP_SIM, true, mapOf("model" to 3f, "gain" to 0.88f, "bass" to 0.45f, "mid" to 0.40f, "treble" to 0.75f, "presence" to 0.75f, "master" to 0.85f)),
                EffectSetting("fx_cab", EffectType.CAB_SIM, true, mapOf("cab_type" to 3f, "mic_distance" to 0.15f)),
                EffectSetting("fx_reverb", EffectType.REVERB, true, mapOf("type" to 0f, "decay" to 1.1f, "mix" to 0.15f, "damping" to 0.6f))
            )
        ),
        GuitarPreset(
            id = "preset_ambient",
            name = "Ambient REV-X Space",
            description = "Ethereal tape flutter delay cascading into shimmering plate reverb",
            isFactory = true,
            effects = listOf(
                EffectSetting("fx_ng", EffectType.NOISE_GATE, true, mapOf("threshold" to -54f, "release" to 160f)),
                EffectSetting("fx_amp", EffectType.AMP_SIM, true, mapOf("model" to 0f, "gain" to 0.25f, "bass" to 0.50f, "mid" to 0.55f, "treble" to 0.60f, "presence" to 0.50f, "master" to 0.80f)),
                EffectSetting("fx_cab", EffectType.CAB_SIM, true, mapOf("cab_type" to 0f, "mic_distance" to 0.4f)),
                EffectSetting("fx_chorus", EffectType.CHORUS, true, mapOf("rate" to 0.6f, "depth" to 0.70f, "mix" to 0.50f)),
                EffectSetting("fx_delay", EffectType.DELAY, true, mapOf("time" to 550f, "feedback" to 0.65f, "mix" to 0.55f)),
                EffectSetting("fx_reverb", EffectType.REVERB, true, mapOf("type" to 2f, "decay" to 5.0f, "mix" to 0.60f, "damping" to 0.2f))
            )
        )
    ).map { preset ->
        // Keep the new modulation pedals available in every factory preset without
        // changing the original tone until the user switches them on.
        preset.copy(
            // Put new pedals first so they are visible without horizontal scrolling.
            effects = listOf(
                EffectSetting(
                    "fx_flanger",
                    EffectType.FLANGER,
                    false,
                    mapOf("rate" to 0.35f, "depth" to 0.7f, "mix" to 0.35f, "feedback" to 0.2f)
                ),
                EffectSetting(
                    "fx_wah",
                    EffectType.WAH,
                    false,
                    mapOf("position" to 0.5f, "q" to 3.5f, "mix" to 1.0f)
                )
            ) + preset.effects.filterNot { it.type == EffectType.FLANGER || it.type == EffectType.WAH }
        )
    }

    fun createDefaultChain(): List<EffectSetting> {
        return factoryPresets[0].effects
    }

    fun presetToJson(preset: GuitarPreset): String {
        val root = JSONObject()
        root.put("id", preset.id)
        root.put("name", preset.name)
        root.put("description", preset.description)
        root.put("isFactory", preset.isFactory)
        val arr = JSONArray()
        for (fx in preset.effects) {
            val fxObj = JSONObject()
            fxObj.put("id", fx.id)
            fxObj.put("type", fx.type.name)
            fxObj.put("isEnabled", fx.isEnabled)
            val pObj = JSONObject()
            fx.params.forEach { (k, v) -> pObj.put(k, v.toDouble()) }
            fxObj.put("params", pObj)
            arr.put(fxObj)
        }
        root.put("effects", arr)
        return root.toString(2)
    }

    fun jsonToPreset(jsonStr: String): GuitarPreset? {
        return try {
            val root = JSONObject(jsonStr)
            val id = root.optString("id", "custom_${System.currentTimeMillis()}")
            val name = root.optString("name", "Imported Preset")
            val desc = root.optString("description", "")
            val isFactory = root.optBoolean("isFactory", false)
            val arr = root.optJSONArray("effects") ?: JSONArray()
            val effectsList = mutableListOf<EffectSetting>()
            for (i in 0 until arr.length()) {
                val fxObj = arr.getJSONObject(i)
                val fxId = fxObj.optString("id", "fx_$i")
                val typeName = fxObj.optString("type", EffectType.OVERDRIVE.name)
                val type = try { EffectType.valueOf(typeName) } catch (_: Exception) { EffectType.OVERDRIVE }
                val isEnabled = fxObj.optBoolean("isEnabled", true)
                val pObj = fxObj.optJSONObject("params") ?: JSONObject()
                val params = mutableMapOf<String, Float>()
                val keys = pObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    params[k] = pObj.optDouble(k, 0.0).toFloat()
                }
                effectsList.add(EffectSetting(fxId, type, isEnabled, params))
            }
            GuitarPreset(id, name, desc, isFactory, effectsList)
        } catch (_: Exception) {
            null
        }
    }
}
