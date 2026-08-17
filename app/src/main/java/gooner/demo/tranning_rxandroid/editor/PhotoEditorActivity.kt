package gooner.demo.tranning_rxandroid.editor

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import gooner.demo.tranning_rxandroid.R
import gooner.demo.tranning_rxandroid.editor.model.Adjustments
import gooner.demo.tranning_rxandroid.editor.model.EditorOverlay
import gooner.demo.tranning_rxandroid.editor.model.PhotoFilter
import gooner.demo.tranning_rxandroid.editor.model.StickerOverlay
import gooner.demo.tranning_rxandroid.editor.model.TextOverlay
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable

/**
 * A small photo editor: pick a picture, recolour it with a filter or the manual
 * adjustments, drop captions and stickers on it, attach a sound, then save or share
 * the result.
 *
 * The heavy work (decoding, filtering, rendering, saving) runs through RxJava so the
 * UI thread only ever draws.
 */
class PhotoEditorActivity : AppCompatActivity() {

    private val disposables = CompositeDisposable()
    private val adjustments = Adjustments()

    private lateinit var engine: ImageEditorEngine
    private lateinit var soundPlayer: SoundPlayer
    private lateinit var filterAdapter: FilterAdapter

    private lateinit var canvasView: EditorCanvasView
    private lateinit var emptyState: View
    private lateinit var progressBar: ProgressBar
    private lateinit var selectionActions: View
    private lateinit var editTextAction: Button
    private lateinit var filtersPanel: RecyclerView
    private lateinit var adjustPanel: View
    private lateinit var soundPanel: View
    private lateinit var soundTitle: TextView
    private lateinit var soundPlayButton: Button

    private var currentFilter = PhotoFilter.ORIGINAL
    private var sourceBitmap: Bitmap? = null
    private var pendingExport = EXPORT_NONE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_editor)

        engine = ImageEditorEngine(this)
        soundPlayer = SoundPlayer(this)
        soundPlayer.onPlaybackStateChanged = { playing -> updateSoundPlayButton(playing) }

        bindViews()
        setupFilterStrip()
        setupAdjustPanel()
        setupToolbar()
        updateSelectionActions(null)
        showPanel(null)
    }

    override fun onStop() {
        super.onStop()
        soundPlayer.pause()
    }

    override fun onDestroy() {
        disposables.clear()
        soundPlayer.release()
        super.onDestroy()
    }

    private fun bindViews() {
        canvasView = findViewById(R.id.editor_canvas)
        emptyState = findViewById(R.id.editor_empty_state)
        progressBar = findViewById(R.id.editor_progress)
        selectionActions = findViewById(R.id.editor_selection_actions)
        editTextAction = findViewById(R.id.editor_action_edit)
        filtersPanel = findViewById(R.id.editor_panel_filters)
        adjustPanel = findViewById(R.id.editor_panel_adjust)
        soundPanel = findViewById(R.id.editor_panel_sound)
        soundTitle = findViewById(R.id.editor_sound_title)
        soundPlayButton = findViewById(R.id.editor_sound_play)

        canvasView.onSelectionChanged = { overlay -> updateSelectionActions(overlay) }

        findViewById<View>(R.id.editor_empty_pick).setOnClickListener { pickPhoto() }
        findViewById<View>(R.id.editor_save).setOnClickListener { exportPicture(EXPORT_SAVE) }
        findViewById<View>(R.id.editor_share).setOnClickListener { exportPicture(EXPORT_SHARE) }

        editTextAction.setOnClickListener {
            val overlay = canvasView.selectedOverlay()
            if (overlay is TextOverlay) {
                showTextDialog(overlay)
            }
        }
        findViewById<View>(R.id.editor_action_duplicate).setOnClickListener {
            canvasView.duplicateSelectedOverlay()
        }
        findViewById<View>(R.id.editor_action_delete).setOnClickListener {
            canvasView.removeSelectedOverlay()
        }

        findViewById<View>(R.id.editor_sound_pick).setOnClickListener { pickSound() }
        soundPlayButton.setOnClickListener { soundPlayer.togglePlayback() }
        findViewById<View>(R.id.editor_sound_remove).setOnClickListener {
            soundPlayer.detach()
            updateSoundPanel()
        }
    }

    private fun setupFilterStrip() {
        filterAdapter = FilterAdapter { filter -> applyFilter(filter) }
        filtersPanel.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        filtersPanel.adapter = filterAdapter
    }

    private fun setupAdjustPanel() {
        bindSeekBar(R.id.editor_seek_brightness) { progress ->
            adjustments.brightness = (progress - 100).toFloat()
        }
        bindSeekBar(R.id.editor_seek_contrast) { progress ->
            adjustments.contrast = 0.5f + progress / 100f
        }
        bindSeekBar(R.id.editor_seek_saturation) { progress ->
            adjustments.saturation = progress / 100f
        }
        bindSeekBar(R.id.editor_seek_warmth) { progress ->
            adjustments.warmth = (progress - 50).toFloat()
        }
        findViewById<View>(R.id.editor_reset_adjust).setOnClickListener { resetAdjustments() }
        resetAdjustments()
    }

    private fun setupToolbar() {
        findViewById<View>(R.id.editor_tool_photo).setOnClickListener { pickPhoto() }
        findViewById<View>(R.id.editor_tool_filter).setOnClickListener {
            if (requirePhoto()) {
                showPanel(filtersPanel)
            }
        }
        findViewById<View>(R.id.editor_tool_adjust).setOnClickListener {
            if (requirePhoto()) {
                showPanel(adjustPanel)
            }
        }
        findViewById<View>(R.id.editor_tool_text).setOnClickListener {
            if (requirePhoto()) {
                showPanel(null)
                showTextDialog(null)
            }
        }
        findViewById<View>(R.id.editor_tool_sticker).setOnClickListener {
            if (requirePhoto()) {
                showPanel(null)
                pickSticker()
            }
        }
        findViewById<View>(R.id.editor_tool_sound).setOnClickListener {
            showPanel(soundPanel)
            updateSoundPanel()
        }
    }

    private fun bindSeekBar(seekBarId: Int, onProgress: (Int) -> Unit) {
        findViewById<SeekBar>(seekBarId).setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        onProgress(progress)
                        applyColorMatrix()
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

                override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
            }
        )
    }

    private fun resetAdjustments() {
        adjustments.reset()
        findViewById<SeekBar>(R.id.editor_seek_brightness).progress = 100
        findViewById<SeekBar>(R.id.editor_seek_contrast).progress = 50
        findViewById<SeekBar>(R.id.editor_seek_saturation).progress = 100
        findViewById<SeekBar>(R.id.editor_seek_warmth).progress = 50
        applyColorMatrix()
    }

    private fun applyFilter(filter: PhotoFilter) {
        currentFilter = filter
        filterAdapter.setSelectedFilter(filter)
        applyColorMatrix()
    }

    private fun applyColorMatrix() {
        val matrix = ColorMatrix(currentFilter.colorMatrix())
        matrix.postConcat(adjustments.toColorMatrix())
        canvasView.setColorMatrix(matrix)
    }

    // ---------------------------------------------------------------- picking

    private fun pickPhoto() {
        startPicker("image/*", REQUEST_PICK_PHOTO, R.string.editor_pick_photo)
    }

    private fun pickSticker() {
        startPicker("image/*", REQUEST_PICK_STICKER, R.string.editor_pick_sticker)
    }

    private fun pickSound() {
        startPicker("audio/*", REQUEST_PICK_SOUND, R.string.editor_pick_sound)
    }

    private fun startPicker(mimeType: String, requestCode: Int, titleRes: Int) {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = mimeType
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        val chooser = Intent.createChooser(intent, getString(titleRes))
        if (chooser.resolveActivity(packageManager) == null) {
            toast(R.string.editor_no_picker)
            return
        }
        startActivityForResult(chooser, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) {
            return
        }
        val uri = data?.data ?: return
        when (requestCode) {
            REQUEST_PICK_PHOTO -> loadPhoto(uri)
            REQUEST_PICK_STICKER -> loadSticker(uri)
            REQUEST_PICK_SOUND -> loadSound(uri)
        }
    }

    private fun loadPhoto(uri: Uri) {
        showProgress(true)
        disposables.add(
            engine.loadBitmap(uri, MAX_PHOTO_SIZE)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { bitmap ->
                        showProgress(false)
                        sourceBitmap = bitmap
                        canvasView.setBaseBitmap(bitmap)
                        emptyState.visibility = View.GONE
                        applyFilter(PhotoFilter.ORIGINAL)
                        resetAdjustments()
                        loadFilterThumbnails(bitmap)
                        showPanel(filtersPanel)
                    },
                    { error -> onLoadFailed(error) }
                )
        )
    }

    private fun loadFilterThumbnails(bitmap: Bitmap) {
        filterAdapter.clear()
        disposables.add(
            engine.filterThumbnails(bitmap, FILTER_THUMBNAIL_SIZE)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { thumbnail -> filterAdapter.addThumbnail(thumbnail) },
                    { error -> Log.w(TAG, "Unable to build the filter previews", error) }
                )
        )
    }

    private fun loadSticker(uri: Uri) {
        val photo = sourceBitmap ?: return
        showProgress(true)
        disposables.add(
            engine.loadBitmap(uri, MAX_STICKER_SIZE)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { bitmap ->
                        showProgress(false)
                        val sticker = StickerOverlay(bitmap)
                        sticker.centerX = photo.width / 2f
                        sticker.centerY = photo.height / 2f
                        sticker.scale = photo.width * STICKER_WIDTH_RATIO / sticker.contentWidth
                        canvasView.addOverlay(sticker)
                    },
                    { error -> onLoadFailed(error) }
                )
        )
    }

    private fun loadSound(uri: Uri) {
        disposables.add(
            engine.displayNameOf(uri)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { name -> attachSound(uri, name) },
                    { error ->
                        Log.w(TAG, "Unable to read the sound name", error)
                        attachSound(uri, getString(R.string.editor_sound_default_title))
                    }
                )
        )
    }

    private fun attachSound(uri: Uri, title: String) {
        if (!soundPlayer.attach(uri, title)) {
            toast(R.string.editor_sound_failed)
        }
        showPanel(soundPanel)
        updateSoundPanel()
    }

    private fun onLoadFailed(error: Throwable) {
        showProgress(false)
        Log.w(TAG, "Unable to load the selected file", error)
        toast(R.string.editor_load_failed)
    }

    // ------------------------------------------------------------------ text

    private fun showTextDialog(existing: TextOverlay?) {
        val photo = sourceBitmap ?: return
        val referenceTextSize = photo.width * TEXT_SIZE_RATIO
        TextOverlayDialog.show(this, existing, referenceTextSize) { text, color, textSize ->
            if (existing == null) {
                val overlay = TextOverlay(text, color, textSize)
                overlay.centerX = photo.width / 2f
                overlay.centerY = photo.height / 2f
                overlay.scale = Math.min(1f, photo.width * TEXT_MAX_WIDTH_RATIO / overlay.contentWidth)
                canvasView.addOverlay(overlay)
            } else {
                existing.text = text
                existing.color = color
                existing.textSize = textSize
                canvasView.notifyOverlayChanged()
            }
        }
    }

    // ---------------------------------------------------------------- export

    private fun exportPicture(action: Int) {
        if (!requirePhoto()) {
            return
        }
        if (!hasStoragePermission()) {
            pendingExport = action
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_WRITE_STORAGE
            )
            return
        }

        canvasView.clearSelection()
        val snapshot = canvasView.snapshot() ?: return
        showProgress(true)
        disposables.add(
            engine.render(snapshot)
                .flatMap { rendered -> engine.saveToGallery(rendered, buildFileName()) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { uri ->
                        showProgress(false)
                        if (action == EXPORT_SHARE) {
                            shareResult(uri)
                        } else {
                            toast(R.string.editor_saved)
                        }
                    },
                    { error ->
                        showProgress(false)
                        Log.w(TAG, "Unable to export the picture", error)
                        toast(R.string.editor_save_failed)
                    }
                )
        )
    }

    private fun shareResult(imageUri: Uri) {
        val track = soundPlayer.track
        val intent: Intent
        if (track == null) {
            intent = Intent(Intent.ACTION_SEND)
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_STREAM, imageUri)
        } else {
            // The picture and the attached sound travel together.
            intent = Intent(Intent.ACTION_SEND_MULTIPLE)
            intent.type = "*/*"
            val streams = ArrayList<Uri>(2)
            streams.add(imageUri)
            streams.add(track.uri)
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, streams)
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(intent, getString(R.string.editor_share)))
    }

    private fun buildFileName(): String {
        return "photo_editor_" + System.currentTimeMillis() + ".jpg"
    }

    private fun hasStoragePermission(): Boolean {
        // From Android 10 on, MediaStore writes into the app's own gallery album without
        // any permission at all.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true
        }
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_WRITE_STORAGE) {
            return
        }
        val action = pendingExport
        pendingExport = EXPORT_NONE
        val granted = grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
        if (granted && action != EXPORT_NONE) {
            exportPicture(action)
        } else if (!granted) {
            toast(R.string.editor_storage_permission_needed)
        }
    }

    // ------------------------------------------------------------------- ui

    private fun showPanel(panel: View?) {
        filtersPanel.visibility = if (panel === filtersPanel) View.VISIBLE else View.GONE
        adjustPanel.visibility = if (panel === adjustPanel) View.VISIBLE else View.GONE
        soundPanel.visibility = if (panel === soundPanel) View.VISIBLE else View.GONE
    }

    private fun updateSelectionActions(overlay: EditorOverlay?) {
        selectionActions.visibility = if (overlay == null) View.GONE else View.VISIBLE
        editTextAction.visibility = if (overlay is TextOverlay) View.VISIBLE else View.GONE
    }

    private fun updateSoundPanel() {
        val track = soundPlayer.track
        soundTitle.text = track?.title ?: getString(R.string.editor_sound_empty)
        soundPlayButton.isEnabled = track != null
        updateSoundPlayButton(soundPlayer.isPlaying)
    }

    private fun updateSoundPlayButton(playing: Boolean) {
        soundPlayButton.setText(
            if (playing) R.string.editor_sound_pause else R.string.editor_sound_play
        )
    }

    private fun showProgress(visible: Boolean) {
        progressBar.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun requirePhoto(): Boolean {
        if (canvasView.hasPhoto()) {
            return true
        }
        toast(R.string.editor_pick_photo_first)
        return false
    }

    private fun toast(messageRes: Int) {
        Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()
    }

    private companion object {
        const val TAG = "PhotoEditorActivity"

        const val REQUEST_PICK_PHOTO = 101
        const val REQUEST_PICK_STICKER = 102
        const val REQUEST_PICK_SOUND = 103
        const val REQUEST_WRITE_STORAGE = 201

        const val EXPORT_NONE = 0
        const val EXPORT_SAVE = 1
        const val EXPORT_SHARE = 2

        const val MAX_PHOTO_SIZE = 2048
        const val MAX_STICKER_SIZE = 512
        const val FILTER_THUMBNAIL_SIZE = 144

        const val STICKER_WIDTH_RATIO = 0.33f
        const val TEXT_SIZE_RATIO = 0.09f
        const val TEXT_MAX_WIDTH_RATIO = 0.9f
    }
}
