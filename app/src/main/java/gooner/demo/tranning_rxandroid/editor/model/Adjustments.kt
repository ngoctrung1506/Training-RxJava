package gooner.demo.tranning_rxandroid.editor.model

import android.graphics.ColorMatrix

/**
 * The manual colour controls of the "Adjust" tool. They are applied on top of the
 * selected [PhotoFilter].
 */
data class Adjustments(
    /** -100f..100f, added to every channel. */
    var brightness: Float = DEFAULT_BRIGHTNESS,
    /** 0.5f..2f, multiplier around mid grey. */
    var contrast: Float = DEFAULT_CONTRAST,
    /** 0f..2f, 0f is fully grey, 1f keeps the original colours. */
    var saturation: Float = DEFAULT_SATURATION,
    /** -50f..50f, positive is warmer (orange), negative is cooler (blue). */
    var warmth: Float = DEFAULT_WARMTH
) {

    fun reset() {
        brightness = DEFAULT_BRIGHTNESS
        contrast = DEFAULT_CONTRAST
        saturation = DEFAULT_SATURATION
        warmth = DEFAULT_WARMTH
    }

    fun toColorMatrix(): ColorMatrix {
        val matrix = ColorMatrix()
        matrix.setSaturation(saturation)
        if (contrast != DEFAULT_CONTRAST) {
            matrix.postConcat(contrastColorMatrix(contrast))
        }
        if (brightness != DEFAULT_BRIGHTNESS) {
            matrix.postConcat(brightnessColorMatrix(brightness))
        }
        if (warmth != DEFAULT_WARMTH) {
            matrix.postConcat(warmthColorMatrix(warmth))
        }
        return matrix
    }

    companion object {
        const val DEFAULT_BRIGHTNESS = 0f
        const val DEFAULT_CONTRAST = 1f
        const val DEFAULT_SATURATION = 1f
        const val DEFAULT_WARMTH = 0f
    }
}
