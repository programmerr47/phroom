package com.programmerr47.phroom.sample

import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import java.io.InputStream
import java.net.URL
import java.util.concurrent.Executors
import kotlin.math.min

class UserAdapter : RecyclerView.Adapter<UserAdapter.ViewHolder>() {
    private val executor = Executors.newFixedThreadPool(5)
    private val uiHandler = Handler(Looper.getMainLooper())

    private var list: List<String> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(SquareImageView(parent.context))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = with(holder) {
        //TODO replace that with more nice approach
        executor.execute {
            val stream = URL(list[position]).content as InputStream
            val drawable = Drawable.createFromStream(stream, null)
            uiHandler.post { ivPhoto.setImageDrawable(drawable) }
        }
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