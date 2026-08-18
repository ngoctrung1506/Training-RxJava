package gooner.demo.tranning_rxandroid.editor

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import gooner.demo.tranning_rxandroid.R
import gooner.demo.tranning_rxandroid.editor.model.Adjustments
import gooner.demo.tranning_rxandroid.editor.model.EditorOverlay
import gooner.demo.tranning_rxandroid.editor.model.PhotoFilter
import gooner.demo.tranning_rxandroid.editor.model.Placement
import gooner.demo.tranning_rxandroid.editor.model.SoundTrack
import gooner.demo.tranning_rxandroid.editor.model.StickerOverlay
import gooner.demo.tranning_rxandroid.editor.model.TextOverlay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns the editing session: the picture, its colours, the overlays and the attached
 * sound. Everything slow is a coroutine on [viewModelScope], so it survives rotation
 * and is cancelled when the screen goes away for good.
 */
class PhotoEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = ImageEditorEngine(application)
    private val soundPlayer = SoundPlayer(application)

    private val _uiState = MutableStateFlow(PhotoEditorUiState())
    val uiState: StateFlow<PhotoEditorUiState> = _uiState.asStateFlow()

    private val _events = Channel<EditorEvent>(Channel.BUFFERED)
    val events: Flow<EditorEvent> = _events.receiveAsFlow()

    private var previewJob: Job? = null
    private var exportJob: Job? = null
    private var nextOverlayId = 1L

    init {
        soundPlayer.onPlaybackFinished = {
            _uiState.update { it.copy(soundPlaying = false) }
        }
    }

    override fun onCleared() {
        soundPlayer.release()
        super.onCleared()
    }

    // ---------------------------------------------------------------- picture

    fun openPhoto(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true) }
            try {
                val photo = engine.loadBitmap(uri, MAX_PHOTO_SIZE)
                _uiState.update {
                    it.copy(
                        photo = photo,
                        filter = PhotoFilter.ORIGINAL,
                        adjustments = Adjustments(),
                        overlays = emptyList(),
                        selectedOverlayId = null,
                        filterPreviews = emptyList(),
                        panel = EditorPanel.FILTERS,
                        busy = false
                    )
                }
                loadFilterPreviews(photo)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Log.w(TAG, "Unable to open the picture", error)
                _uiState.update { it.copy(busy = false) }
                _events.send(EditorEvent.Message(R.string.editor_load_failed))
            }
        }
    }

    private fun loadFilterPreviews(photo: Bitmap) {
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            try {
                engine.filterPreviews(photo, FILTER_PREVIEW_SIZE).collect { preview ->
                    _uiState.update { it.copy(filterPreviews = it.filterPreviews + preview) }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Log.w(TAG, "Unable to build the filter previews", error)
            }
        }
    }

    fun selectFilter(filter: PhotoFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun updateAdjustments(adjustments: Adjustments) {
        _uiState.update { it.copy(adjustments = adjustments) }
    }

    fun resetAdjustments() {
        _uiState.update { it.copy(adjustments = Adjustments()) }
    }

    fun showPanel(panel: EditorPanel) {
        _uiState.update { it.copy(panel = if (it.panel == panel) EditorPanel.NONE else panel) }
    }

    // --------------------------------------------------------------- overlays

    fun selectOverlayAt(x: Float, y: Float) {
        _uiState.update { state ->
            // The topmost overlay under the finger wins.
            val hit = state.overlays.lastOrNull { it.contains(x, y) }
            state.copy(selectedOverlayId = hit?.id)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedOverlayId = null) }
    }

    fun transformSelected(panX: Float, panY: Float, zoom: Float, rotation: Float) {
        _uiState.update { state ->
            val selectedId = state.selectedOverlayId ?: return@update state
            state.copy(
                overlays = state.overlays.map { overlay ->
                    if (overlay.id == selectedId) {
                        overlay.withPlacement(
                            overlay.placement.transformed(panX, panY, zoom, rotation)
                        )
                    } else {
                        overlay
                    }
                }
            )
        }
    }

    fun deleteSelected() {
        _uiState.update { state ->
            val selectedId = state.selectedOverlayId ?: return@update state
            state.copy(
                overlays = state.overlays.filterNot { it.id == selectedId },
                selectedOverlayId = null
            )
        }
    }

    fun duplicateSelected() {
        val state = _uiState.value
        val selected = state.selectedOverlay ?: return
        val placement = selected.placement.copy(
            centerX = selected.placement.centerX + selected.width * DUPLICATE_OFFSET_RATIO,
            centerY = selected.placement.centerY + selected.height * DUPLICATE_OFFSET_RATIO
        )
        val copy = when (selected) {
            is TextOverlay -> selected.copy(id = nextOverlayId++, placement = placement)
            is StickerOverlay -> selected.copy(id = nextOverlayId++, placement = placement)
        }
        _uiState.update { it.copy(overlays = it.overlays + copy, selectedOverlayId = copy.id) }
    }

    /** The text size, in bitmap pixels, matching the middle of the size slider. */
    fun referenceTextSize(): Float {
        val photo = _uiState.value.photo ?: return DEFAULT_TEXT_SIZE
        return photo.width * TEXT_SIZE_RATIO
    }

    fun addText(text: String, color: Int, textSize: Float) {
        val photo = _uiState.value.photo ?: return
        val overlay = TextOverlay(
            id = nextOverlayId++,
            text = text,
            color = color,
            textSize = textSize,
            placement = Placement(centerX = photo.width / 2f, centerY = photo.height / 2f)
        )
        // Keep a long caption inside the picture.
        val fitted = overlay.withPlacement(
            overlay.placement.copy(
                scale = Math.min(1f, photo.width * TEXT_MAX_WIDTH_RATIO / overlay.contentWidth)
            )
        )
        _uiState.update {
            it.copy(overlays = it.overlays + fitted, selectedOverlayId = fitted.id)
        }
    }

    fun updateSelectedText(text: String, color: Int, textSize: Float) {
        _uiState.update { state ->
            val selected = state.selectedOverlay
            if (selected !is TextOverlay) {
                return@update state
            }
            val updated = selected.copy(text = text, color = color, textSize = textSize)
            state.copy(overlays = state.overlays.map { if (it.id == updated.id) updated else it })
        }
    }

    fun addSticker(uri: Uri) {
        val photo = _uiState.value.photo ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true) }
            try {
                val bitmap = engine.loadBitmap(uri, MAX_STICKER_SIZE)
                val sticker = StickerOverlay(
                    id = nextOverlayId++,
                    bitmap = bitmap,
                    placement = Placement(
                        centerX = photo.width / 2f,
                        centerY = photo.height / 2f,
                        scale = photo.width * STICKER_WIDTH_RATIO / bitmap.width
                    )
                )
                _uiState.update {
                    it.copy(
                        overlays = it.overlays + sticker,
                        selectedOverlayId = sticker.id,
                        busy = false
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Log.w(TAG, "Unable to open the image", error)
                _uiState.update { it.copy(busy = false) }
                _events.send(EditorEvent.Message(R.string.editor_load_failed))
            }
        }
    }

    // ------------------------------------------------------------------ sound

    fun attachSound(uri: Uri) {
        viewModelScope.launch {
            val name = try {
                engine.displayNameOf(uri)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Log.w(TAG, "Unable to read the sound name", error)
                null
            } ?: getApplication<Application>().getString(R.string.editor_sound_default_title)

            if (soundPlayer.load(uri)) {
                _uiState.update {
                    it.copy(
                        sound = SoundTrack(uri, name),
                        soundPlaying = false,
                        panel = EditorPanel.SOUND
                    )
                }
            } else {
                _uiState.update { it.copy(sound = null, soundPlaying = false) }
                _events.send(EditorEvent.Message(R.string.editor_sound_failed))
            }
        }
    }

    fun toggleSound() {
        val playing = soundPlayer.togglePlayback()
        _uiState.update { it.copy(soundPlaying = playing) }
    }

    fun pauseSound() {
        soundPlayer.pause()
        _uiState.update { it.copy(soundPlaying = false) }
    }

    fun removeSound() {
        soundPlayer.release()
        _uiState.update { it.copy(sound = null, soundPlaying = false) }
    }

    // ----------------------------------------------------------------- export

    /** Renders the picture at full resolution, saves it, then optionally shares it. */
    fun export(share: Boolean) {
        val state = _uiState.value
        val photo = state.photo ?: return
        if (exportJob?.isActive == true) {
            return
        }
        exportJob = viewModelScope.launch {
            _uiState.update { it.copy(busy = true, selectedOverlayId = null) }
            try {
                val snapshot = EditorSnapshot(photo, state.colorMatrix(), state.overlays)
                val rendered = engine.render(snapshot)
                val uri = engine.saveToGallery(rendered, buildFileName())
                if (share) {
                    _events.send(EditorEvent.Share(uri, state.sound?.uri))
                } else {
                    _events.send(EditorEvent.Message(R.string.editor_saved))
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Log.w(TAG, "Unable to export the picture", error)
                _events.send(EditorEvent.Message(R.string.editor_save_failed))
            } finally {
                _uiState.update { it.copy(busy = false) }
            }
        }
    }

    fun onStoragePermissionDenied() {
        viewModelScope.launch {
            _events.send(EditorEvent.Message(R.string.editor_storage_permission_needed))
        }
    }

    private fun buildFileName(): String = "photo_editor_" + System.currentTimeMillis() + ".jpg"

    private companion object {
        const val TAG = "PhotoEditorViewModel"

        const val MAX_PHOTO_SIZE = 2048
        const val MAX_STICKER_SIZE = 512
        const val FILTER_PREVIEW_SIZE = 144

        const val STICKER_WIDTH_RATIO = 0.33f
        const val TEXT_SIZE_RATIO = 0.09f
        const val TEXT_MAX_WIDTH_RATIO = 0.9f
        const val DEFAULT_TEXT_SIZE = 48f
        const val DUPLICATE_OFFSET_RATIO = 0.12f
    }
}
