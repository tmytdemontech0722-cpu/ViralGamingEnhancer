package com.viral.gamingenhancer.data

enum class OutputAspect(val label: String, val width: Int, val height: Int) {
    ORIGINAL("Original", 0, 0),
    REELS("9:16 Reels", 1080, 1920),
    SQUARE("1:1", 1080, 1080),
    PORTRAIT("4:5", 1080, 1350)
}

/** Every field here is read by EffectChain / VideoExportManager, so every control really does something. */
data class EditorSettings(
    val saturation: Float = 0f,   // -1..1
    val contrast: Float = 0f,     // -1..1
    val brightness: Float = 0f,   // -1..1
    val temperature: Float = 0f,  // -1 cool .. +1 warm
    val tint: Float = 0f,         // -1 green .. +1 magenta
    val aspect: OutputAspect = OutputAspect.ORIGINAL,
    val targetFps: Int = 0,       // 0 = keep original. 30/60 only ever REDUCE the rate.
    val bitrateMbps: Int = 12,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L
)
