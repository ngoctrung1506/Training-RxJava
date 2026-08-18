package gooner.demo.tranning_rxandroid.editor.ui

import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import gooner.demo.tranning_rxandroid.editor.model.EditorOverlay

/**
 * The editing surface: the photo with its colour filter, the overlays on top, and the
 * gestures that move, scale and rotate the selected overlay.
 *
 * The overlays are drawn in bitmap coordinates through the native canvas, so the very
 * same code produces this preview and the exported full resolution picture.
 */
@Composable
fun EditorCanvas(
    photo: Bitmap,
    colorMatrix: ColorMatrix,
    overlays: List<EditorOverlay>,
    selectedOverlayId: Long?,
    onSelectAt: (Float, Float) -> Unit,
    onTransform: (panX: Float, panY: Float, zoom: Float, rotation: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val photoPaint = remember(colorMatrix) {
        Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
    }
    val framePaint = remember {
        Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            color = android.graphics.Color.WHITE
        }
    }
    val currentOnSelectAt by rememberUpdatedState(onSelectAt)
    val currentOnTransform by rememberUpdatedState(onTransform)
    val strokeWidth = with(LocalDensity.current) { 1.5.dp.toPx() }

    Canvas(
        modifier = modifier.pointerInput(photo) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val fit = PhotoFit.of(photo, size.width.toFloat(), size.height.toFloat())
                currentOnSelectAt(fit.toImageX(down.position.x), fit.toImageY(down.position.y))

                while (true) {
                    val event = awaitPointerEvent()
                    val pressed = event.changes.filter { it.pressed }
                    if (pressed.isEmpty()) {
                        break
                    }
                    val pan = centroidOf(pressed, current = true) -
                            centroidOf(pressed, current = false)
                    val zoom = zoomOf(pressed)
                    val rotation = rotationOf(pressed)
                    if (pan != Offset.Zero || zoom != 1f || rotation != 0f) {
                        currentOnTransform(pan.x / fit.scale, pan.y / fit.scale, zoom, rotation)
                        pressed.forEach { change ->
                            if (change.positionChanged()) {
                                change.consume()
                            }
                        }
                    }
                }
            }
        }
    ) {
        val fit = PhotoFit.of(photo, size.width, size.height)
        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            val checkpoint = nativeCanvas.save()
            nativeCanvas.translate(fit.offsetX, fit.offsetY)
            nativeCanvas.scale(fit.scale, fit.scale)
            nativeCanvas.drawBitmap(photo, 0f, 0f, photoPaint)
            overlays.forEach { overlay -> overlay.draw(nativeCanvas) }
            overlays.firstOrNull { it.id == selectedOverlayId }?.let { selected ->
                // The frame is drawn in bitmap space, so undo the zoom to keep it thin.
                framePaint.strokeWidth = strokeWidth / fit.scale
                selected.drawSelection(nativeCanvas, framePaint)
            }
            nativeCanvas.restoreToCount(checkpoint)
        }
    }
}

/** How the photo is laid out inside the canvas: centred, scaled to fit. */
private data class PhotoFit(val scale: Float, val offsetX: Float, val offsetY: Float) {

    fun toImageX(x: Float): Float = (x - offsetX) / scale

    fun toImageY(y: Float): Float = (y - offsetY) / scale

    companion object {
        fun of(photo: Bitmap, width: Float, height: Float): PhotoFit {
            if (photo.width <= 0 || photo.height <= 0 || width <= 0f || height <= 0f) {
                return PhotoFit(1f, 0f, 0f)
            }
            val scale = Math.min(width / photo.width, height / photo.height)
            return PhotoFit(
                scale = scale,
                offsetX = (width - photo.width * scale) / 2f,
                offsetY = (height - photo.height * scale) / 2f
            )
        }
    }
}

private fun centroidOf(changes: List<PointerInputChange>, current: Boolean): Offset {
    var sum = Offset.Zero
    for (change in changes) {
        sum += if (current) change.position else change.previousPosition
    }
    return sum / changes.size.toFloat()
}

/** How much the fingers spread apart since the previous event, 1f when they did not. */
private fun zoomOf(changes: List<PointerInputChange>): Float {
    if (changes.size < 2) {
        return 1f
    }
    val currentSpread = spreadOf(changes, centroidOf(changes, current = true), current = true)
    val previousSpread = spreadOf(changes, centroidOf(changes, current = false), current = false)
    if (previousSpread <= 0f) {
        return 1f
    }
    return currentSpread / previousSpread
}

private fun spreadOf(
    changes: List<PointerInputChange>,
    centroid: Offset,
    current: Boolean
): Float {
    var sum = 0f
    for (change in changes) {
        val position = if (current) change.position else change.previousPosition
        sum += (position - centroid).getDistance()
    }
    return sum / changes.size
}

/** How much the two fingers turned since the previous event, in degrees. */
private fun rotationOf(changes: List<PointerInputChange>): Float {
    if (changes.size < 2) {
        return 0f
    }
    val currentAngle = angleOf(changes[1].position - changes[0].position)
    val previousAngle = angleOf(changes[1].previousPosition - changes[0].previousPosition)
    var delta = currentAngle - previousAngle
    while (delta > 180f) {
        delta -= 360f
    }
    while (delta < -180f) {
        delta += 360f
    }
    return delta
}

private fun angleOf(vector: Offset): Float =
    Math.toDegrees(Math.atan2(vector.y.toDouble(), vector.x.toDouble())).toFloat()
