package com.viral.gamingenhancer.data

data class Preset(
    val name: String,
    val note: String,
    val apply: (EditorSettings) -> EditorSettings
)

/** Presets are plain parameter bundles. They keep the user's trim points. */
object PresetManager {
    val all: List<Preset> = listOf(
        Preset("Viral 120 FPS", "Punchy colour. No frame interpolation (not available yet).") {
            it.copy(saturation = 0.25f, contrast = 0.20f, brightness = 0.02f)
        },
        Preset("Free Fire MAX Pro", "Rich colour, clear detail.") {
            it.copy(saturation = 0.20f, contrast = 0.12f, brightness = 0.03f)
        },
        Preset("Cinematic Teal & Orange", "Warm grade + film contrast. Global approximation, not true split-toning.") {
            it.copy(saturation = 0.10f, contrast = 0.22f, temperature = 0.25f, tint = -0.10f)
        },
        Preset("Red Glow", "Red-leaning grade. Glow/bloom is NOT implemented yet.") {
            it.copy(saturation = 0.15f, contrast = 0.15f, temperature = 0.15f, tint = 0.25f)
        },
        Preset("Clean Competitive", "Minimal processing, readable gameplay.") {
            it.copy(saturation = 0.08f, contrast = 0.06f)
        },
        Preset("Instagram Reels Viral", "9:16 crop, vibrant grade, 12 Mbps.") {
            it.copy(saturation = 0.30f, contrast = 0.20f, brightness = 0.02f,
                aspect = OutputAspect.REELS, bitrateMbps = 12)
        }
    )
}
