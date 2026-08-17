package gooner.demo.tranning_rxandroid.editor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import gooner.demo.tranning_rxandroid.R
import gooner.demo.tranning_rxandroid.editor.model.PhotoFilter

/** The horizontal strip of filter previews shown under the photo. */
class FilterAdapter(
    private val onFilterClicked: (PhotoFilter) -> Unit
) : RecyclerView.Adapter<FilterAdapter.FilterViewHolder>() {

    private val thumbnails = ArrayList<FilterThumbnail>()
    private var selectedFilter = PhotoFilter.ORIGINAL

    fun clear() {
        thumbnails.clear()
        notifyDataSetChanged()
    }

    fun addThumbnail(thumbnail: FilterThumbnail) {
        thumbnails.add(thumbnail)
        notifyItemInserted(thumbnails.size - 1)
    }

    fun setSelectedFilter(filter: PhotoFilter) {
        if (selectedFilter == filter) {
            return
        }
        selectedFilter = filter
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_editor_filter, parent, false)
        return FilterViewHolder(view)
    }

    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        val thumbnail = thumbnails[position]
        holder.preview.setImageBitmap(thumbnail.preview)
        holder.label.setText(thumbnail.filter.labelRes)
        holder.itemView.isSelected = thumbnail.filter == selectedFilter
        holder.itemView.setOnClickListener { onFilterClicked(thumbnail.filter) }
    }

    override fun getItemCount(): Int = thumbnails.size

    class FilterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val preview: ImageView = itemView.findViewById(R.id.editor_filter_preview)
        val label: TextView = itemView.findViewById(R.id.editor_filter_label)
    }
}
