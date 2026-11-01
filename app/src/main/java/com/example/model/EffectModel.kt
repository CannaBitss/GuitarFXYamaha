package com.example.model

enum class EffectCategory {
    UTILITY,
    DRIVE,
    AMP,
    CABINET,
    MODULATION,
    DELAY,
    REVERB
}

enum class EffectType(val displayName: String, val category: EffectCategory) {
    NOISE_GATE("Noise Gate", EffectCategory.UTILITY),
    OVERDRIVE("TS Screamer", EffectCategory.DRIVE),
    DISTORTION("Metal Shred", EffectCategory.DRIVE),
    BOOSTER("Clean Boost", EffectCategory.DRIVE),
    FUZZ("Vintage Fuzz", EffectCategory.DRIVE),
    AMP_SIM("Yamaha Amp Model", EffectCategory.AMP),
    CAB_SIM("Cabinet IR", EffectCategory.CABINET),
    CHORUS("SPX Chorus", EffectCategory.MODULATION),
    FLANGER("Jet Flanger", EffectCategory.MODULATION),
    WAH("Cry Baby Wah", EffectCategory.MODULATION),
    PHASER("Phase 90", EffectCategory.MODULATION),
    TREMOLO("Opto Tremolo", EffectCategory.MODULATION),
    ROTARY("Rotary Speaker", EffectCategory.MODULATION),
    DELAY("Tape Echo Delay", EffectCategory.DELAY),
    REVERB("Yamaha REV-X Reverb", EffectCategory.REVERB)
}

enum class AmpModel(val displayName: String, val description: String) {
    CLEAN("Boutique Clean", "Warm, glassy tube clean with high headroom"),
    CRUNCH("Class-A Crunch", "British chime with creamy harmonic breakup"),
    LEAD("THR Lead", "Sustained melodic high-gain lead tone"),
    MODERN("Modern Metal", "Tight low-end, scooped mids for heavy riffs"),
    BASS("Vintage Bass", "Deep, punchy tube amp for bass & down-tuned guitar")
}

enum class CabType(val displayName: String, val speakerSize: String) {
    CAB_1X12("1x12 American Open-Back", "12\" Jensen"),
    CAB_2X12("2x12 British Celestion", "12\" Greenback"),
    CAB_4X12_V30("4x12 Vintage 30", "12\" Celestion V30"),
    CAB_4X12_RECTO("4x12 Oversized Metal", "12\" High-Power"),
    CAB_1X15_BASS("1x15 Bass Enclosure", "15\" Heavy Cone")
}

enum class ReverbType(val displayName: String) {
    ROOM("Acoustic Room"),
    HALL("Concert Hall"),
    PLATE("Studio Plate")
}

data class EffectSetting(
    val id: String,
    val type: EffectType,
    val isEnabled: Boolean = true,
    val params: Map<String, Float> = emptyMap()
)

data class GuitarPreset(
    val id: String,
    val name: String,
    val description: String,
    val isFactory: Boolean = false,
    val effects: List<EffectSetting> = emptyList()
)
