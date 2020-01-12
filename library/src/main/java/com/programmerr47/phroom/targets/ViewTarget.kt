package com.programmerr47.phroom.targets

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewTreeObserver
import android.widget.ImageView
import com.programmerr47.phroom.TaskConfig
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class ViewTarget(
    private val imageView: ImageView,
    private val configBuilder: TaskConfig
) : Target {
    override val size: Target.Size = ViewSize(imageView)

    override fun onNew(initial: Bitmap?) {
        initial?.let { imageView.setImageBitmap(initial) } ?: imageView.setImageDrawable(configBuilder.loadingPlaceholder)
    }

    override fun onStart() {}

    override fun onSuccess(bitmap: Bitmap) = imageView.setImageBitmap(bitmap)

    override fun onFailure(e: Throwable) =
        imageView.setImageDrawable(configBuilder.errorPlaceholder)

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is ViewTarget) return false

        return imageView == other.imageView
    }

    override fun hashCode() = imageView.hashCode()

    private class ViewSize(
        private val view: View
    ) : LockTargetSize() {

        @Volatile
        private var size: Pair<Int, Int> = view.size()

        override val width: Int get() = await { size.first }

        override val height: Int get() = await { size.second }

        init {
            if (view.measuredWidth == 0 && view.measuredHeight == 0) {
                startWait()
                view.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        view.viewTreeObserver.removeOnPreDrawListener(this)
                        signal { size = view.size() }
                        return true
                    }
                })
            }
        }

        override fun check() = size

        private fun View.size() = measuredWidth to measuredHeight
    }
}
