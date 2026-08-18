package gooner.demo.tranning_rxandroid.editor.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import gooner.demo.tranning_rxandroid.R
import gooner.demo.tranning_rxandroid.editor.model.Adjustments

/** Brightness, contrast, saturation and warmth, applied on top of the chosen filter. */
@Composable
fun AdjustPanel(
    adjustments: Adjustments,
    onAdjustmentsChanged: (Adjustments) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        AdjustSlider(
            labelRes = R.string.editor_adjust_brightness,
            value = adjustments.brightness,
            range = Adjustments.BRIGHTNESS_RANGE,
            onValueChange = { onAdjustmentsChanged(adjustments.copy(brightness = it)) }
        )
        AdjustSlider(
            labelRes = R.string.editor_adjust_contrast,
            value = adjustments.contrast,
            range = Adjustments.CONTRAST_RANGE,
            onValueChange = { onAdjustmentsChanged(adjustments.copy(contrast = it)) }
        )
        AdjustSlider(
            labelRes = R.string.editor_adjust_saturation,
            value = adjustments.saturation,
            range = Adjustments.SATURATION_RANGE,
            onValueChange = { onAdjustmentsChanged(adjustments.copy(saturation = it)) }
        )
        AdjustSlider(
            labelRes = R.string.editor_adjust_warmth,
            value = adjustments.warmth,
            range = Adjustments.WARMTH_RANGE,
            onValueChange = { onAdjustmentsChanged(adjustments.copy(warmth = it)) }
        )
        TextButton(
            onClick = onReset,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(text = stringResource(R.string.editor_adjust_reset))
        }
    }
}

@Composable
private fun AdjustSlider(
    labelRes: Int,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Text(
        text = stringResource(labelRes),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = range,
        modifier = Modifier.fillMaxWidth()
    )
}
