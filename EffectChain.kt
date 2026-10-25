@file:OptIn(UnstableApi::class)

package com.viral.gamingenhancer.effects

import androidx.media3.common.Effect
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Brightness
import androidx.media3.effect.Contrast
import androidx.media3.effect.FrameDropEffect
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.Presentation
import androidx.media3.effect.RgbAdjustment
import com.viral.gamingenhancer.data.EditorSettings
import com.viral.gamingenhancer.data.OutputAspect

/**
 * Turns EditorSettings into Media3 video effects.
 * The SAME colour list is used for the live preview and for the exported file.
 */
object EffectChain {

    fun colorEffects(s: EditorSettings): List<Effect> {
        val l = mutableListOf<Effect>()
        if (s.brightness != 0f) l += Brightness(s.brightness.coerceIn(-1f, 1f))
        if (s.contrast != 0f) l += Contrast(s.contrast.coerceIn(-1f, 1f))
        if (s.saturation != 0f) {
            l += HslAdjustment.Builder()
                .adjustSaturation((s.saturation * 100f).coerceIn(-100f, 100f))
                .build()
        }
        if (s.temperature != 0f || s.tint != 0f) {
            l += RgbAdjustment.Builder()
                .setRedScale(1f + 0.12f * s.temperature)
                .setGreenScale(1f - 0.10f * s.tint)
                .setBlueScale(1f - 0.12f * s.temperature)
                .build()
        }
        return l
    }

    /** Export = colour + optional frame dropping + optional crop/scale. */
    fun exportEffects(s: EditorSettings, sourceFps: Float): List<Effect> {
        val l = colorEffects(s).toMutableList()
        // Frame-rate: Media3 can only DROP frames. It cannot invent new ones.
        if (s.targetFps > 0 && sourceFps > s.targetFps + 1f) {
            l += FrameDropEffect.createDefaultFrameDropEffect(s.targetFps.toFloat())
        }
        if (s.aspect != OutputAspect.ORIGINAL) {
            l += Presentation.createForWidthAndHeight(
                s.aspect.width, s.aspect.height, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP
            )
        }
        return l
    }
}
