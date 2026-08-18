package gooner.demo.tranning_rxandroid.editor

import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.net.Uri
import gooner.demo.tranning_rxandroid.editor.model.Adjustments
import gooner.demo.tranning_rxandroid.editor.model.EditorOverlay
import gooner.demo.tranning_rxandroid.editor.model.PhotoFilter
import gooner.demo.tranning_rxandroid.editor.model.SoundTrack

/** Which tool panel is open under the picture. */
enum class EditorPanel { NONE, FILTERS, ADJUST, SOUND }

/** Everything the editor screen draws, in one immutable value. */
data class PhotoEditorUiState(
    val photo: Bitmap? = null,
    val filter: PhotoFilter = PhotoFilter.ORIGINAL,
    val adjustments: Adjustments = Adjustments(),
    val overlays: List<EditorOverlay> = emptyList(),
    val selectedOverlayId: Long? = null,
    val filterPreviews: List<FilterPreview> = emptyList(),
    val panel: EditorPanel = EditorPanel.NONE,
    val sound: SoundTrack? = null,
    val soundPlaying: Boolean = false,
    val busy: Boolean = false
) {

    val selectedOverlay: EditorOverlay?
        get() = overlays.firstOrNull { it.id == selectedOverlayId }

    /** The filter and the manual adjustments, combined into the matrix used everywhere. */
    fun colorMatrix(): ColorMatrix {
        val matrix = ColorMatrix(filter.colorMatrix())
        matrix.postConcat(adjustments.toColorMatrix())
        return matrix
    }
}

/** One shot things the screen has to act on, as opposed to state it draws. */
sealed interface EditorEvent {

    /** Show a short message. */
    data class Message(val textRes: Int) : EditorEvent

    /** The picture was saved and is ready to be handed to another app. */
    data class Share(val image: Uri, val sound: Uri?) : EditorEvent
}
