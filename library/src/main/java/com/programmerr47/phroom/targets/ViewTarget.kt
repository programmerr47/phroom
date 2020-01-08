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

    override fun onNew(initial: Drawable?) = imageView.setImageDrawable(initial ?: configBuilder.loadingPlaceholder)

    override fun onStart() {}

    override fun onSuccess(bitmap: Bitmap) = imageView.setImageBitmap(bitmap)

    override fun onFailure(e: Throwable) =
        imageView.setImageDrawable(configBuilder.errorPlaceholder)

    private class ViewSize(
        private val view: View
    ) : Target.Size {

        private val _lock = ReentrantLock()
        private val _cond = _lock.newCondition()
        private var awaitForSize = false //wanted to do AtomicBoolean, but realised that we use it only inside lock section, so it is not needed

        @Volatile
        private var size: Pair<Int, Int> = view.size()

        override val width: Int
            get() = _lock.withLock {
                if (awaitForSize) _cond.await()
                size.first
            }

        override val height: Int
            get() = _lock.withLock {
                if (awaitForSize) _cond.await()
                size.second
            }

        init {
            if (view.measuredWidth == 0 && view.measuredHeight == 0) {
                awaitForSize = true
                view.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        view.viewTreeObserver.removeOnPreDrawListener(this)
                        _lock.withLock {
                            size = view.size()

                            if (awaitForSize) {
                                awaitForSize = false
                                _cond.signalAll()
                            }
                        }
                        return true
                    }
                })
            }
        }

        override fun check() = size

        private fun View.size() = measuredWidth to measuredHeight
    }
}
