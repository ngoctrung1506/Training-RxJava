package gooner.demo.tranning_rxandroid.editor.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import gooner.demo.tranning_rxandroid.R
import gooner.demo.tranning_rxandroid.editor.EditorEvent
import gooner.demo.tranning_rxandroid.editor.EditorPanel
import gooner.demo.tranning_rxandroid.editor.PhotoEditorUiState
import gooner.demo.tranning_rxandroid.editor.PhotoEditorViewModel
import gooner.demo.tranning_rxandroid.editor.model.Adjustments
import gooner.demo.tranning_rxandroid.editor.model.PhotoFilter
import gooner.demo.tranning_rxandroid.editor.model.TextOverlay
import kotlinx.coroutines.launch

/** Which caption the text dialog is editing, if it is open at all. */
private enum class TextDialogTarget { NEW, SELECTED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditorScreen(viewModel: PhotoEditorViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var textDialogTarget by remember { mutableStateOf<TextDialogTarget?>(null) }
    var pendingShare by remember { mutableStateOf(false) }

    val showMessage: (Int) -> Unit = { textRes ->
        scope.launch { snackbarHostState.showSnackbar(context.getString(textRes)) }
    }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.openPhoto(it) }
    }
    val pickSticker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.addSticker(it) }
    }
    val pickSound = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.attachSound(it) }
    }
    val requestStoragePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.export(pendingShare)
        } else {
            viewModel.onStoragePermissionDenied()
        }
    }

    // A device without any file picker would otherwise crash on launch().
    val launchPicker: (ActivityResultLauncher<String>, String) -> Unit = { launcher, mimeType ->
        try {
            launcher.launch(mimeType)
        } catch (error: ActivityNotFoundException) {
            showMessage(R.string.editor_no_picker)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is EditorEvent.Message -> showMessage(event.textRes)
                is EditorEvent.Share -> context.startActivity(
                    Intent.createChooser(
                        shareIntent(event.image, event.sound),
                        context.getString(R.string.editor_share)
                    )
                )
            }
        }
    }

    // Exporting needs the storage permission on Android 9 and older only.
    val export: (Boolean) -> Unit = { share ->
        when {
            state.photo == null -> showMessage(R.string.editor_pick_photo_first)
            hasStoragePermission(context) -> viewModel.export(share)
            else -> {
                pendingShare = share
                requestStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    val withPhoto: (() -> Unit) -> Unit = { action ->
        if (state.photo == null) showMessage(R.string.editor_pick_photo_first) else action()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.editor_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    TextButton(onClick = { export(false) }) {
                        Text(text = stringResource(R.string.editor_save))
                    }
                    TextButton(onClick = { export(true) }) {
                        Text(text = stringResource(R.string.editor_share))
                    }
                }
            )
        },
        bottomBar = {
            Column {
                ToolPanel(
                    state = state,
                    onFilterSelected = viewModel::selectFilter,
                    onAdjustmentsChanged = viewModel::updateAdjustments,
                    onResetAdjustments = viewModel::resetAdjustments,
                    onPickSound = { launchPicker(pickSound, AUDIO_MIME) },
                    onTogglePlayback = viewModel::toggleSound,
                    onRemoveSound = viewModel::removeSound
                )
                EditorToolbar(
                    onPickPhoto = { launchPicker(pickPhoto, IMAGE_MIME) },
                    onShowFilters = { withPhoto { viewModel.showPanel(EditorPanel.FILTERS) } },
                    onShowAdjust = { withPhoto { viewModel.showPanel(EditorPanel.ADJUST) } },
                    onAddText = { withPhoto { textDialogTarget = TextDialogTarget.NEW } },
                    onAddSticker = { withPhoto { launchPicker(pickSticker, IMAGE_MIME) } },
                    onShowSound = { viewModel.showPanel(EditorPanel.SOUND) }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            val photo = state.photo
            if (photo == null) {
                EmptyState(
                    onPickPhoto = { launchPicker(pickPhoto, IMAGE_MIME) },
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                EditorCanvas(
                    photo = photo,
                    colorMatrix = remember(state.filter, state.adjustments) { state.colorMatrix() },
                    overlays = state.overlays,
                    selectedOverlayId = state.selectedOverlayId,
                    onSelectAt = viewModel::selectOverlayAt,
                    onTransform = viewModel::transformSelected,
                    modifier = Modifier.fillMaxSize()
                )
            }

            state.selectedOverlay?.let { selected ->
                SelectionActions(
                    editable = selected is TextOverlay,
                    onEdit = { textDialogTarget = TextDialogTarget.SELECTED },
                    onDuplicate = viewModel::duplicateSelected,
                    onDelete = viewModel::deleteSelected,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }

            if (state.busy) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }

    textDialogTarget?.let { target ->
        val existing = if (target == TextDialogTarget.SELECTED) {
            state.selectedOverlay as? TextOverlay
        } else {
            null
        }
        TextOverlayDialog(
            existing = existing,
            referenceTextSize = viewModel.referenceTextSize(),
            onDismiss = { textDialogTarget = null },
            onConfirm = { text, color, textSize ->
                if (existing == null) {
                    viewModel.addText(text, color, textSize)
                } else {
                    viewModel.updateSelectedText(text, color, textSize)
                }
                textDialogTarget = null
            }
        )
    }
}

@Composable
private fun ToolPanel(
    state: PhotoEditorUiState,
    onFilterSelected: (PhotoFilter) -> Unit,
    onAdjustmentsChanged: (Adjustments) -> Unit,
    onResetAdjustments: () -> Unit,
    onPickSound: () -> Unit,
    onTogglePlayback: () -> Unit,
    onRemoveSound: () -> Unit
) {
    if (state.panel == EditorPanel.NONE) {
        return
    }
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        when (state.panel) {
            EditorPanel.FILTERS -> FilterStrip(
                previews = state.filterPreviews,
                selectedFilter = state.filter,
                onFilterSelected = onFilterSelected
            )

            EditorPanel.ADJUST -> AdjustPanel(
                adjustments = state.adjustments,
                onAdjustmentsChanged = onAdjustmentsChanged,
                onReset = onResetAdjustments
            )

            EditorPanel.SOUND -> SoundPanel(
                sound = state.sound,
                playing = state.soundPlaying,
                onPickSound = onPickSound,
                onTogglePlayback = onTogglePlayback,
                onRemoveSound = onRemoveSound
            )

            EditorPanel.NONE -> Unit
        }
    }
}

@Composable
private fun EditorToolbar(
    onPickPhoto: () -> Unit,
    onShowFilters: () -> Unit,
    onShowAdjust: () -> Unit,
    onAddText: () -> Unit,
    onAddSticker: () -> Unit,
    onShowSound: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(onClick = onPickPhoto) {
                Text(text = stringResource(R.string.editor_tool_photo))
            }
            TextButton(onClick = onShowFilters) {
                Text(text = stringResource(R.string.editor_tool_filter))
            }
            TextButton(onClick = onShowAdjust) {
                Text(text = stringResource(R.string.editor_tool_adjust))
            }
            TextButton(onClick = onAddText) {
                Text(text = stringResource(R.string.editor_tool_text))
            }
            TextButton(onClick = onAddSticker) {
                Text(text = stringResource(R.string.editor_tool_sticker))
            }
            TextButton(onClick = onShowSound) {
                Text(text = stringResource(R.string.editor_tool_sound))
            }
        }
    }
}

@Composable
private fun SelectionActions(
    editable: Boolean,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (editable) {
                TextButton(onClick = onEdit) {
                    Text(text = stringResource(R.string.editor_action_edit))
                }
            }
            TextButton(onClick = onDuplicate) {
                Text(text = stringResource(R.string.editor_action_duplicate))
            }
            TextButton(onClick = onDelete) {
                Text(text = stringResource(R.string.editor_action_delete))
            }
        }
    }
}

@Composable
private fun EmptyState(onPickPhoto: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(32.dp)
    ) {
        Text(
            text = stringResource(R.string.editor_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onPickPhoto,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(text = stringResource(R.string.editor_tool_photo))
        }
    }
}

private fun shareIntent(image: Uri, sound: Uri?): Intent {
    val intent = if (sound == null) {
        Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, image)
        }
    } else {
        // The picture and the attached sound travel together.
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(image, sound))
        }
    }
    return intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
}

private fun hasStoragePermission(context: Context): Boolean {
    // From Android 10 on, MediaStore writes into the app's own gallery album without
    // any permission at all.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        return true
    }
    return ContextCompat.checkSelfPermission(
        context, Manifest.permission.WRITE_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED
}

private const val IMAGE_MIME = "image/*"
private const val AUDIO_MIME = "audio/*"
