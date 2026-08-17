package gooner.demo.tranning_rxandroid.editor.model

import android.graphics.ColorMatrix
import androidx.annotation.StringRes
import gooner.demo.tranning_rxandroid.R

/**
 * The ready made looks offered in the "Filter" tool. Every look is expressed as a
 * [ColorMatrix] so it can be applied both to the small preview thumbnails and to the
 * full resolution bitmap when exporting, without any third party library.
 */
enum class PhotoFilter(@StringRes val labelRes: Int) {

    ORIGINAL(R.string.filter_original),
    MONO(R.string.filter_mono),
    NOIR(R.string.filter_noir),
    SEPIA(R.string.filter_sepia),
    VIVID(R.string.filter_vivid),
    COOL(R.string.filter_cool),
    WARM(R.string.filter_warm),
    FADE(R.string.filter_fade),
    INVERT(R.string.filter_invert);

    fun colorMatrix(): ColorMatrix {
        return when (this) {
            ORIGINAL -> ColorMatrix()

            MONO -> ColorMatrix().apply { setSaturation(0f) }

            NOIR -> ColorMatrix().apply {
                setSaturation(0f)
                postConcat(contrastColorMatrix(1.45f))
                postConcat(brightnessColorMatrix(-8f))
            }

            SEPIA -> ColorMatrix().apply {
                setSaturation(0f)
                postConcat(
                    ColorMatrix(
                        floatArrayOf(
                            1.12f, 0f, 0f, 0f, 14f,
                            0f, 0.96f, 0f, 0f, 6f,
                            0f, 0f, 0.74f, 0f, 0f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                )
            }

            VIVID -> ColorMatrix().apply {
                setSaturation(1.55f)
                postConcat(contrastColorMatrix(1.15f))
            }

            COOL -> ColorMatrix().apply {
                setSaturation(1.1f)
                postConcat(warmthColorMatrix(-18f))
            }

            WARM -> ColorMatrix().apply {
                setSaturation(1.1f)
                postConcat(warmthColorMatrix(18f))
            }

            FADE -> ColorMatrix().apply {
                setSaturation(0.72f)
                postConcat(contrastColorMatrix(0.82f))
                postConcat(brightnessColorMatrix(14f))
            }

            INVERT -> ColorMatrix(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }
    }
}

/**
 * Scales every colour channel around the mid grey point, which is what "contrast" means
 * for an 8 bit image. `contrast` is a multiplier, 1f keeps the picture untouched.
 */
internal fun contrastColorMatrix(contrast: Float): ColorMatrix {
    val translate = (1f - contrast) * 127.5f
    return ColorMatrix(
        floatArrayOf(
            contrast, 0f, 0f, 0f, translate,
            0f, contrast, 0f, 0f, translate,
            0f, 0f, contrast, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        )
    )
}

/** `brightness` is an offset added to every channel, in the -255..255 range. */
internal fun brightnessColorMatrix(brightness: Float): ColorMatrix {
    return ColorMatrix(
        floatArrayOf(
            1f, 0f, 0f, 0f, brightness,
            0f, 1f, 0f, 0f, brightness,
            0f, 0f, 1f, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        )
    )
}

/**
 * Shifts the colour temperature: a positive `warmth` pushes the picture towards orange,
 * a negative one towards blue.
 */
internal fun warmthColorMatrix(warmth: Float): ColorMatrix {
    return ColorMatrix(
        floatArrayOf(
            1f, 0f, 0f, 0f, warmth,
            0f, 1f, 0f, 0f, warmth * 0.25f,
            0f, 0f, 1f, 0f, -warmth,
            0f, 0f, 0f, 1f, 0f
        )
    )
}
