package gooner.demo.tranning_rxandroid.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import gooner.demo.tranning_rxandroid.editor.model.EditorOverlay

/**
 * The editing surface.
 *
 * It draws the photo with the current colour filter applied and every overlay on top of
 * it, and lets the user drag, pinch and rotate the selected overlay. Overlay positions
 * are kept in bitmap coordinates so that [snapshot] can be re-drawn at full resolution.
 */
class EditorCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /** Notified every time the selected overlay changes, including when it is cleared. */
    var onSelectionChanged: ((EditorOverlay?) -> Unit)? = null

    private val overlays = ArrayList<EditorOverlay>()

    private val imageMatrix = Matrix()
    private val inverseMatrix = Matrix()
    private val touchPoint = FloatArray(2)

    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val selectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
    }

    private var baseBitmap: Bitmap? = null
    private var colorMatrix = ColorMatrix()

    private var selectedOverlay: EditorOverlay? = null
    private var lastImageX = 0f
    private var lastImageY = 0f
    private var lastAngle = 0f
    private var draggingSelection = false

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val overlay = selectedOverlay ?: return false
                val scaled = overlay.scale * detector.scaleFactor
                overlay.scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scaled))
                invalidate()
                return true
            }
        }
    )

    fun setBaseBitmap(bitmap: Bitmap?) {
        baseBitmap = bitmap
        overlays.clear()
        updateSelection(null)
        requestImageMatrixUpdate()
        invalidate()
    }

    fun getBaseBitmap(): Bitmap? = baseBitmap

    fun hasPhoto(): Boolean = baseBitmap != null

    fun setColorMatrix(matrix: ColorMatrix) {
        colorMatrix = ColorMatrix(matrix)
        bitmapPaint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        invalidate()
    }

    fun addOverlay(overlay: EditorOverlay) {
        overlays.add(overlay)
        updateSelection(overlay)
        invalidate()
    }

    fun selectedOverlay(): EditorOverlay? = selectedOverlay

    fun removeSelectedOverlay() {
        val overlay = selectedOverlay ?: return
        overlays.remove(overlay)
        updateSelection(null)
        invalidate()
    }

    fun duplicateSelectedOverlay() {
        val overlay = selectedOverlay ?: return
        val copy = overlay.copyOverlay()
        copy.centerX += overlay.width * DUPLICATE_OFFSET_RATIO
        copy.centerY += overlay.height * DUPLICATE_OFFSET_RATIO
        addOverlay(copy)
    }

    fun clearSelection() {
        updateSelection(null)
        invalidate()
    }

    /** Redraws the preview after the selected overlay was edited elsewhere (text dialog). */
    fun notifyOverlayChanged() {
        invalidate()
    }

    /**
     * Freezes everything that is needed to re-render the picture on a background thread.
     * Overlays are copied so that further edits cannot corrupt an export in flight.
     */
    fun snapshot(): EditorSnapshot? {
        val bitmap = baseBitmap ?: return null
        val copies = ArrayList<EditorOverlay>(overlays.size)
        for (overlay in overlays) {
            copies.add(overlay.copyOverlay())
        }
        return EditorSnapshot(bitmap, ColorMatrix(colorMatrix), copies)
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        requestImageMatrixUpdate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bitmap = baseBitmap ?: return

        val checkpoint = canvas.save()
        canvas.concat(imageMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, bitmapPaint)
        for (overlay in overlays) {
            overlay.draw(canvas)
        }
        selectedOverlay?.let { overlay ->
            selectionPaint.strokeWidth = SELECTION_STROKE_DP * resources.displayMetrics.density /
                    currentImageScale()
            overlay.drawSelection(canvas, selectionPaint)
        }
        canvas.restoreToCount(checkpoint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (baseBitmap == null) {
            return false
        }
        scaleDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mapToImage(event.x, event.y)
                lastImageX = touchPoint[0]
                lastImageY = touchPoint[1]
                val hit = findOverlayAt(lastImageX, lastImageY)
                draggingSelection = hit != null
                updateSelection(hit)
                invalidate()
                return true
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                // The focal point jumps when a finger is added, so restart from it.
                resetGestureAnchor(event)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val overlay = selectedOverlay
                if (overlay == null || !draggingSelection) {
                    return true
                }
                if (event.pointerCount >= 2) {
                    val angle = angleBetweenPointers(event)
                    overlay.rotationDegrees = normalizeDegrees(
                        overlay.rotationDegrees + (angle - lastAngle)
                    )
                    lastAngle = angle
                }
                mapToImage(focusX(event), focusY(event))
                overlay.centerX += touchPoint[0] - lastImageX
                overlay.centerY += touchPoint[1] - lastImageY
                lastImageX = touchPoint[0]
                lastImageY = touchPoint[1]
                invalidate()
                return true
            }

            MotionEvent.ACTION_POINTER_UP -> {
                resetGestureAnchor(event)
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                draggingSelection = false
                performClick()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean = super.performClick()

    private fun requestImageMatrixUpdate() {
        val bitmap = baseBitmap
        if (bitmap == null || width == 0 || height == 0) {
            return
        }
        val source = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
        val destination = RectF(0f, 0f, width.toFloat(), height.toFloat())
        imageMatrix.setRectToRect(source, destination, Matrix.ScaleToFit.CENTER)
        if (!imageMatrix.invert(inverseMatrix)) {
            inverseMatrix.reset()
        }
    }

    private fun currentImageScale(): Float {
        val values = FloatArray(9)
        imageMatrix.getValues(values)
        val scale = values[Matrix.MSCALE_X]
        return if (scale > 0f) scale else 1f
    }

    private fun mapToImage(x: Float, y: Float) {
        touchPoint[0] = x
        touchPoint[1] = y
        inverseMatrix.mapPoints(touchPoint)
    }

    private fun findOverlayAt(x: Float, y: Float): EditorOverlay? {
        // Topmost overlay wins, so walk the list backwards.
        for (index in overlays.indices.reversed()) {
            val overlay = overlays[index]
            if (overlay.contains(x, y)) {
                return overlay
            }
        }
        return null
    }

    private fun updateSelection(overlay: EditorOverlay?) {
        if (selectedOverlay === overlay) {
            return
        }
        selectedOverlay = overlay
        onSelectionChanged?.invoke(overlay)
    }

    private fun resetGestureAnchor(event: MotionEvent) {
        mapToImage(focusX(event), focusY(event))
        lastImageX = touchPoint[0]
        lastImageY = touchPoint[1]
        if (event.pointerCount >= 2) {
            lastAngle = angleBetweenPointers(event)
        }
    }

    private fun focusX(event: MotionEvent): Float {
        var sum = 0f
        for (index in 0 until event.pointerCount) {
            sum += event.getX(index)
        }
        return sum / event.pointerCount
    }

    private fun focusY(event: MotionEvent): Float {
        var sum = 0f
        for (index in 0 until event.pointerCount) {
            sum += event.getY(index)
        }
        return sum / event.pointerCount
    }

    private fun angleBetweenPointers(event: MotionEvent): Float {
        val deltaX = (event.getX(1) - event.getX(0)).toDouble()
        val deltaY = (event.getY(1) - event.getY(0)).toDouble()
        return Math.toDegrees(Math.atan2(deltaY, deltaX)).toFloat()
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
        const val SELECTION_STROKE_DP = 1.5f
        const val DUPLICATE_OFFSET_RATIO = 0.12f
    }
}

/** An immutable description of the picture, safe to render off the main thread. */
class EditorSnapshot(
    val baseBitmap: Bitmap,
    val colorMatrix: ColorMatrix,
    val overlays: List<EditorOverlay>
)
