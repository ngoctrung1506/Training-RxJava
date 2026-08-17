package gooner.demo.tranning_rxandroid.editor

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import gooner.demo.tranning_rxandroid.R
import gooner.demo.tranning_rxandroid.editor.model.TextOverlay

/**
 * The "Add text" / "Edit text" dialog: the caption itself, its colour and its size.
 * The colour swatches are built in code so the palette lives in a single place.
 */
object TextOverlayDialog {

    private val PALETTE = intArrayOf(
        Color.WHITE,
        Color.BLACK,
        Color.parseColor("#F44336"),
        Color.parseColor("#FF9800"),
        Color.parseColor("#FFEB3B"),
        Color.parseColor("#4CAF50"),
        Color.parseColor("#03A9F4"),
        Color.parseColor("#3F51B5"),
        Color.parseColor("#E91E63")
    )

    private const val MIN_SIZE_RATIO = 0.4f
    private const val SIZE_RANGE_RATIO = 1.6f

    /**
     * @param referenceTextSize the text size, in bitmap pixels, matching the middle of the
     * slider. It depends on the photo resolution so the caption always looks the same.
     * @param onConfirmed called with the edited text, colour and size when the user accepts.
     */
    fun show(
        activity: Activity,
        existing: TextOverlay?,
        referenceTextSize: Float,
        onConfirmed: (String, Int, Float) -> Unit
    ) {
        val content = activity.layoutInflater.inflate(R.layout.dialog_editor_text, null)
        val input = content.findViewById<EditText>(R.id.editor_text_input)
        val sizeBar = content.findViewById<SeekBar>(R.id.editor_text_size)
        val swatchRow = content.findViewById<LinearLayout>(R.id.editor_text_colors)

        val selectedColor = intArrayOf(existing?.color ?: Color.WHITE)

        existing?.let {
            input.setText(it.text)
            input.setSelection(it.text.length)
        }
        sizeBar.max = 100
        sizeBar.progress = progressFor(existing?.textSize, referenceTextSize)

        buildSwatches(activity, swatchRow, selectedColor)

        AlertDialog.Builder(activity)
            .setTitle(
                if (existing == null) R.string.editor_text_add_title
                else R.string.editor_text_edit_title
            )
            .setView(content)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val text = input.text.toString().trim()
                if (!TextUtils.isEmpty(text)) {
                    onConfirmed(
                        text,
                        selectedColor[0],
                        textSizeFor(sizeBar.progress, referenceTextSize)
                    )
                }
            }
            .show()
    }

    private fun buildSwatches(
        activity: Activity,
        row: LinearLayout,
        selectedColor: IntArray
    ) {
        val density = activity.resources.displayMetrics.density
        val size = (36 * density).toInt()
        val margin = (6 * density).toInt()
        val swatches = ArrayList<View>(PALETTE.size)

        for (color in PALETTE) {
            val swatch = View(activity)
            val params = LinearLayout.LayoutParams(size, size)
            params.setMargins(margin, margin, margin, margin)
            swatch.layoutParams = params
            swatch.tag = color
            swatch.setOnClickListener {
                selectedColor[0] = color
                refreshSwatches(swatches, selectedColor[0], density)
            }
            swatches.add(swatch)
            row.addView(swatch)
        }

        refreshSwatches(swatches, selectedColor[0], density)
    }

    private fun refreshSwatches(swatches: List<View>, selectedColor: Int, density: Float) {
        for (swatch in swatches) {
            val color = swatch.tag as Int
            swatch.background = swatchDrawable(color, color == selectedColor, density)
        }
    }

    private fun swatchDrawable(color: Int, selected: Boolean, density: Float): GradientDrawable {
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.OVAL
        drawable.setColor(color)
        val strokeWidth = if (selected) (3 * density).toInt() else (1 * density).toInt()
        val strokeColor = if (selected) Color.parseColor("#FFD54F") else Color.parseColor("#66FFFFFF")
        drawable.setStroke(strokeWidth, strokeColor)
        return drawable
    }

    private fun progressFor(textSize: Float?, referenceTextSize: Float): Int {
        if (textSize == null || referenceTextSize <= 0f) {
            return 50
        }
        val ratio = textSize / referenceTextSize
        val progress = ((ratio - MIN_SIZE_RATIO) / SIZE_RANGE_RATIO * 100f).toInt()
        return Math.max(0, Math.min(100, progress))
    }

    private fun textSizeFor(progress: Int, referenceTextSize: Float): Float {
        return referenceTextSize * (MIN_SIZE_RATIO + progress / 100f * SIZE_RANGE_RATIO)
    }
}
