package com.programmerr47.phroom.sample

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.programmerr47.phroom.Phroom
import kotlin.math.min

class UserAdapter(
    private val phroom: Phroom
) : RecyclerView.Adapter<UserAdapter.ViewHolder>() {
    private var list: List<String> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(SquareImageView(parent.context))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = with(holder) {
        phroom.load(list[position], ivPhoto)
    }

    override fun getItemCount() = list.size

    //TODO add diff callback
    fun updateList(newList: List<String>) {
        val oldSize = list.size
        val newSize = newList.size
        list = newList

        val minSize = min(oldSize, newSize)
        if (minSize > 0) notifyItemRangeChanged(0, minSize)

        when {
            oldSize < newSize -> notifyItemRangeInserted(minSize, newSize - minSize + 1)
            oldSize > newSize -> notifyItemRangeRemoved(minSize, oldSize - minSize + 1)
        }
    }

    class ViewHolder(
        val ivPhoto: SquareImageView
    ) : RecyclerView.ViewHolder(ivPhoto)
}