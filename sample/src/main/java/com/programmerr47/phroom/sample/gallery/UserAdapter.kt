package com.programmerr47.phroom.sample.gallery

import android.view.ViewGroup
import android.widget.ImageView
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.programmerr47.phroom.Phroom
import com.programmerr47.phroom.sample.R
import com.programmerr47.phroom.kutils.views.SquareImageView

class UserAdapter(
    private val phroom: Phroom
) : PagedListAdapter<String, UserAdapter.ViewHolder>(SimpleItemCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(SquareImageView(parent.context).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
    })

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = with(holder) {
        val url = getItem(position) ?: return@with
        phroom.load(url, ivPhoto) {
            loadingPlaceholderRes = R.drawable.bg_image
            errorPlaceholderRes = R.drawable.bg_image_no
        }
    }

    class ViewHolder(
        val ivPhoto: SquareImageView
    ) : RecyclerView.ViewHolder(ivPhoto)
}
