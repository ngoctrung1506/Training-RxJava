package gooner.demo.tranning_rxandroid.editor.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import gooner.demo.tranning_rxandroid.editor.FilterPreview
import gooner.demo.tranning_rxandroid.editor.model.PhotoFilter

/** The horizontal strip of filter previews, filled in as the previews are computed. */
@Composable
fun FilterStrip(
    previews: List<FilterPreview>,
    selectedFilter: PhotoFilter,
    onFilterSelected: (PhotoFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items = previews, key = { it.filter.name }) { preview ->
            FilterItem(
                preview = preview,
                selected = preview.filter == selectedFilter,
                onClick = { onFilterSelected(preview.filter) }
            )
        }
    }
}

@Composable
private fun FilterItem(
    preview: FilterPreview,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(6.dp)
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Image(
            bitmap = preview.preview.asImageBitmap(),
            contentDescription = stringResource(preview.filter.labelRes),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .clip(shape)
                .border(if (selected) 2.dp else 1.dp, borderColor, shape)
        )
        Text(
            text = stringResource(preview.filter.labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
