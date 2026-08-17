package gooner.demo.tranning_rxandroid.editor.model

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

/**
 * Something drawn on top of the photo: a piece of text or a pasted image.
 *
 * Every overlay lives in the coordinate space of the *source bitmap*, not of the view.
 * That way the very same drawing code produces the on screen preview and the exported
 * full resolution picture.
 */
sealed class EditorOverlay {

    var centerX: Float = 0f
    var centerY: Float = 0f
    var scale: Float = 1f
    var rotationDegrees: Float = 0f

    /** Size of the content before [scale] is applied. */
    abstract val contentWidth: Float
    abstract val contentHeight: Float

    /** Draws the content centred on (0, 0) of the current canvas. */
    protected abstract fun drawContent(canvas: Canvas)

    /** A deep enough copy to be safely rendered on a background thread. */
    abstract fun copyOverlay(): EditorOverlay

    val width: Float get() = contentWidth * scale
    val height: Float get() = contentHeight * scale

    fun draw(canvas: Canvas) {
        val checkpoint = canvas.save()
        canvas.translate(centerX, centerY)
        canvas.rotate(rotationDegrees)
        canvas.scale(scale, scale)
        drawContent(canvas)
        canvas.restoreToCount(checkpoint)
    }

    /** Draws the selection frame. Only used for the preview, never for the export. */
    fun drawSelection(canvas: Canvas, paint: Paint) {
        val checkpoint = canvas.save()
        canvas.translate(centerX, centerY)
        canvas.rotate(rotationDegrees)
        val halfWidth = width / 2f
        val halfHeight = height / 2f
        canvas.drawRect(-halfWidth, -halfHeight, halfWidth, halfHeight, paint)
        canvas.restoreToCount(checkpoint)
    }

    /** Hit testing in bitmap coordinates, taking the rotation into account. */
    fun contains(x: Float, y: Float): Boolean {
        val deltaX = x - centerX
        val deltaY = y - centerY
        val radians = Math.toRadians(-rotationDegrees.toDouble())
        val cos = Math.cos(radians).toFloat()
        val sin = Math.sin(radians).toFloat()
        val localX = deltaX * cos - deltaY * sin
        val localY = deltaX * sin + deltaY * cos
        // A small margin makes small stickers and thin text easier to grab with a finger.
        val halfWidth = width / 2f + TOUCH_MARGIN
        val halfHeight = height / 2f + TOUCH_MARGIN
        return localX >= -halfWidth && localX <= halfWidth &&
                localY >= -halfHeight && localY <= halfHeight
    }

    protected fun copyPlacementTo(target: EditorOverlay): EditorOverlay {
        target.centerX = centerX
        target.centerY = centerY
        target.scale = scale
        target.rotationDegrees = rotationDegrees
        return target
    }

    private companion object {
        const val TOUCH_MARGIN = 12f
    }
}

/** A caption typed by the user, with its own colour and size. */
class TextOverlay(
    var text: String,
    var color: Int = Color.WHITE,
    var textSize: Float = 48f,
    var shadow: Boolean = true
) : EditorOverlay() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private val lines: List<String> get() = text.split("\n")

    override val contentWidth: Float
        get() {
            syncPaint()
            var widest = 0f
            for (line in lines) {
                val lineWidth = paint.measureText(line)
                if (lineWidth > widest) {
                    widest = lineWidth
                }
            }
            return widest + textSize * 0.4f
        }

    override val contentHeight: Float
        get() {
            syncPaint()
            val metrics = paint.fontMetrics
            return (metrics.bottom - metrics.top) * lines.size
        }

    override fun drawContent(canvas: Canvas) {
        syncPaint()
        val metrics = paint.fontMetrics
        val lineHeight = metrics.bottom - metrics.top
        val allLines = lines
        var baseline = -(lineHeight * allLines.size) / 2f - metrics.top
        for (line in allLines) {
            canvas.drawText(line, 0f, baseline, paint)
            baseline += lineHeight
        }
    }

    override fun copyOverlay(): EditorOverlay {
        return copyPlacementTo(TextOverlay(text, color, textSize, shadow))
    }

    private fun syncPaint() {
        paint.textSize = textSize
        paint.color = color
        if (shadow) {
            paint.setShadowLayer(textSize * 0.12f, 0f, textSize * 0.05f, SHADOW_COLOR)
        } else {
            paint.clearShadowLayer()
        }
    }

    private companion object {
        val SHADOW_COLOR = Color.argb(160, 0, 0, 0)
    }
}

/** An image pasted on top of the photo: a sticker, a logo, a second picture. */
class StickerOverlay(val bitmap: Bitmap, var alpha: Int = 255) : EditorOverlay() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    override val contentWidth: Float get() = bitmap.width.toFloat()

    override val contentHeight: Float get() = bitmap.height.toFloat()

    override fun drawContent(canvas: Canvas) {
        paint.alpha = alpha
        canvas.drawBitmap(bitmap, -contentWidth / 2f, -contentHeight / 2f, paint)
    }

    override fun copyOverlay(): EditorOverlay {
        return copyPlacementTo(StickerOverlay(bitmap, alpha))
    }
}
