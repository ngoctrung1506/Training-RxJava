package gooner.demo.tranning_rxandroid.editor.model

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

/** Where an overlay sits on the photo, in bitmap coordinates. */
data class Placement(
    val centerX: Float = 0f,
    val centerY: Float = 0f,
    val scale: Float = 1f,
    val rotationDegrees: Float = 0f
) {

    /** Applies one step of a drag / pinch / rotate gesture. */
    fun transformed(panX: Float, panY: Float, zoom: Float, rotation: Float): Placement {
        return copy(
            centerX = centerX + panX,
            centerY = centerY + panY,
            scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale * zoom)),
            rotationDegrees = normalizeDegrees(rotationDegrees + rotation)
        )
    }

    private fun normalizeDegrees(degrees: Float): Float {
        var value = degrees % 360f
        if (value < 0f) {
            value += 360f
        }
        return value
    }

    private companion object {
        const val MIN_SCALE = 0.05f
        const val MAX_SCALE = 12f
    }
}

/**
 * Something drawn on top of the photo: a piece of text or a pasted image.
 *
 * Overlays are immutable so that Compose can tell states apart: moving one produces a
 * new instance. They live in the coordinate space of the *source bitmap*, not of the
 * screen, so the same drawing code produces the preview and the full resolution export.
 */
sealed class EditorOverlay {

    abstract val id: Long
    abstract val placement: Placement

    /** Size of the content before the placement scale is applied. */
    abstract val contentWidth: Float
    abstract val contentHeight: Float

    abstract fun withPlacement(placement: Placement): EditorOverlay

    /** Draws the content centred on (0, 0) of the current canvas. */
    protected abstract fun drawContent(canvas: Canvas)

    val width: Float get() = contentWidth * placement.scale
    val height: Float get() = contentHeight * placement.scale

    fun draw(canvas: Canvas) {
        val checkpoint = canvas.save()
        canvas.translate(placement.centerX, placement.centerY)
        canvas.rotate(placement.rotationDegrees)
        canvas.scale(placement.scale, placement.scale)
        drawContent(canvas)
        canvas.restoreToCount(checkpoint)
    }

    /** Draws the selection frame. Only used for the preview, never for the export. */
    fun drawSelection(canvas: Canvas, paint: Paint) {
        val checkpoint = canvas.save()
        canvas.translate(placement.centerX, placement.centerY)
        canvas.rotate(placement.rotationDegrees)
        val halfWidth = width / 2f
        val halfHeight = height / 2f
        canvas.drawRect(-halfWidth, -halfHeight, halfWidth, halfHeight, paint)
        canvas.restoreToCount(checkpoint)
    }

    /** Hit testing in bitmap coordinates, taking the rotation into account. */
    fun contains(x: Float, y: Float): Boolean {
        val deltaX = x - placement.centerX
        val deltaY = y - placement.centerY
        val radians = Math.toRadians(-placement.rotationDegrees.toDouble())
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

    private companion object {
        const val TOUCH_MARGIN = 12f
    }
}

/** A caption typed by the user, with its own colour and size. */
data class TextOverlay(
    override val id: Long,
    val text: String,
    val color: Int = Color.WHITE,
    val textSize: Float = 48f,
    val shadow: Boolean = true,
    override val placement: Placement = Placement()
) : EditorOverlay() {

    private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).also { textPaint ->
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = textSize
        textPaint.color = color
        if (shadow) {
            textPaint.setShadowLayer(textSize * 0.12f, 0f, textSize * 0.05f, SHADOW_COLOR)
        }
    }

    private val lines: List<String> = text.split("\n")

    override val contentWidth: Float = lines.fold(0f) { widest, line ->
        Math.max(widest, paint.measureText(line))
    } + textSize * 0.4f

    override val contentHeight: Float =
        (paint.fontMetrics.bottom - paint.fontMetrics.top) * lines.size

    override fun withPlacement(placement: Placement): EditorOverlay = copy(placement = placement)

    override fun drawContent(canvas: Canvas) {
        val metrics = paint.fontMetrics
        val lineHeight = metrics.bottom - metrics.top
        var baseline = -(lineHeight * lines.size) / 2f - metrics.top
        for (line in lines) {
            canvas.drawText(line, 0f, baseline, paint)
            baseline += lineHeight
        }
    }

    private companion object {
        val SHADOW_COLOR = Color.argb(160, 0, 0, 0)
    }
}

/** An image pasted on top of the photo: a sticker, a logo, a second picture. */
data class StickerOverlay(
    override val id: Long,
    val bitmap: Bitmap,
    val alpha: Int = 255,
    override val placement: Placement = Placement()
) : EditorOverlay() {

    private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        .also { stickerPaint -> stickerPaint.alpha = alpha }

    override val contentWidth: Float = bitmap.width.toFloat()

    override val contentHeight: Float = bitmap.height.toFloat()

    override fun withPlacement(placement: Placement): EditorOverlay = copy(placement = placement)

    override fun drawContent(canvas: Canvas) {
        canvas.drawBitmap(bitmap, -contentWidth / 2f, -contentHeight / 2f, paint)
    }
}
