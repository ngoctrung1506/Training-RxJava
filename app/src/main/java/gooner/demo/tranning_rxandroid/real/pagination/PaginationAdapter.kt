package gooner.demo.tranning_rxandroid.real.pagination

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import gooner.demo.tranning_rxandroid.R

class PaginationAdapter :
    Adapter<ViewHolder?>() {
    internal var items: MutableList<String> = mutableListOf()

    internal fun addItems(items: MutableList<String>) {
        this.items.addAll(items)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ItemViewHolder.create(parent)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        (holder as ItemViewHolder).bind(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }

    private class ItemViewHolder internal constructor(itemView: View) :
        ViewHolder(itemView) {
        internal fun bind(content: String) {
            (itemView as TextView).text = content
        }

        companion object {
            internal fun create(parent: ViewGroup): ItemViewHolder {
                return ItemViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.item_pagination,
                        parent,
                        false
                    )
                )
            }
        }
    }


}