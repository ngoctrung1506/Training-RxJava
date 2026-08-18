package gooner.demo.tranning_rxandroid.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import gooner.demo.tranning_rxandroid.R
import gooner.demo.tranning_rxandroid.editor.model.TextOverlay

private val PALETTE = listOf(
    Color.White,
    Color.Black,
    Color(0xFFF44336),
    Color(0xFFFF9800),
    Color(0xFFFFEB3B),
    Color(0xFF4CAF50),
    Color(0xFF03A9F4),
    Color(0xFF3F51B5),
    Color(0xFFE91E63)
)

private const val MIN_SIZE_RATIO = 0.4f
private const val SIZE_RANGE_RATIO = 1.6f

/**
 * Adds a new caption or edits the selected one: the text itself, its colour and size.
 *
 * @param referenceTextSize the text size, in bitmap pixels, matching the middle of the
 * slider. It follows the photo resolution so a caption always looks the same.
 */
@Composable
fun TextOverlayDialog(
    existing: TextOverlay?,
    referenceTextSize: Float,
    onDismiss: () -> Unit,
    onConfirm: (text: String, color: Int, textSize: Float) -> Unit
) {
    var text by remember { mutableStateOf(existing?.text.orEmpty()) }
    var color by remember { mutableIntStateOf(existing?.color ?: Color.White.toArgb()) }
    var sizeFraction by remember {
        mutableFloatStateOf(sizeFractionOf(existing?.textSize, referenceTextSize))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    if (existing == null) R.string.editor_text_add_title
                    else R.string.editor_text_edit_title
                )
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(text = stringResource(R.string.editor_text_hint)) },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.editor_text_size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
                Slider(value = sizeFraction, onValueChange = { sizeFraction = it })
                Text(
                    text = stringResource(R.string.editor_text_color),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 8.dp)
                ) {
                    PALETTE.forEach { swatch ->
                        ColorSwatch(
                            color = swatch,
                            selected = swatch.toArgb() == color,
                            onClick = { color = swatch.toArgb() }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text.trim(), color, textSizeOf(sizeFraction, referenceTextSize)) },
                enabled = text.isNotBlank()
            ) {
                Text(text = stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }
        }
    )
}

@Composable
private fun ColorSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(4.dp)
            .size(36.dp)
            .background(color, CircleShape)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                },
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}

private fun sizeFractionOf(textSize: Float?, referenceTextSize: Float): Float {
    if (textSize == null || referenceTextSize <= 0f) {
        return 0.5f
    }
    val fraction = (textSize / referenceTextSize - MIN_SIZE_RATIO) / SIZE_RANGE_RATIO
    return Math.max(0f, Math.min(1f, fraction))
}

private fun textSizeOf(fraction: Float, referenceTextSize: Float): Float =
    referenceTextSize * (MIN_SIZE_RATIO + fraction * SIZE_RANGE_RATIO)
