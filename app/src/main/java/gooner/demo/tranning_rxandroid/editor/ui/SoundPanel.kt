package gooner.demo.tranning_rxandroid.editor.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import gooner.demo.tranning_rxandroid.R
import gooner.demo.tranning_rxandroid.editor.model.SoundTrack

/** The sound attached to the picture, with its preview controls. */
@Composable
fun SoundPanel(
    sound: SoundTrack?,
    playing: Boolean,
    onPickSound: () -> Unit,
    onTogglePlayback: () -> Unit,
    onRemoveSound: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = sound?.title ?: stringResource(R.string.editor_sound_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onPickSound) {
            Text(text = stringResource(R.string.editor_sound_pick))
        }
        TextButton(onClick = onTogglePlayback, enabled = sound != null) {
            Text(
                text = stringResource(
                    if (playing) R.string.editor_sound_pause else R.string.editor_sound_play
                )
            )
        }
        TextButton(onClick = onRemoveSound, enabled = sound != null) {
            Text(text = stringResource(R.string.editor_sound_remove))
        }
    }
}
