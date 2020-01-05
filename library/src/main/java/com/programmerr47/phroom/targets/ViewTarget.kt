package com.programmerr47.phroom.targets

import android.graphics.Bitmap
import android.widget.ImageView
import com.programmerr47.phroom.TaskConfig

internal class ViewTarget(
    private val imageView: ImageView,
    private val configBuilder: TaskConfig
) : Target {

    override fun onNew() = imageView.setImageDrawable(configBuilder.loadingPlaceholder)

    override fun onStart() {}

    override fun onSuccess(bitmap: Bitmap) = imageView.setImageBitmap(bitmap)

    override fun onFailure(e: Throwable) =
        imageView.setImageDrawable(configBuilder.errorPlaceholder)
}
