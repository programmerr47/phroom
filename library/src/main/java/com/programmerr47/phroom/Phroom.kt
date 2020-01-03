package com.programmerr47.phroom

import android.graphics.drawable.Drawable
import android.widget.ImageView
import java.io.InputStream
import java.lang.ref.WeakReference
import java.net.URL
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

class Phroom {
    private val executor = Executors.newFixedThreadPool(5)
    private val cbExecutor = MainThreadExecutor()

    private val submitted = WeakHashMap<ImageView, UrlTask>()

    fun load(url: String, target: ImageView) {
        cancelWork(target)
        target.setImageDrawable(null)  //TODO add progress drawable

        val task = UrlTask(url, target)
        submitted[target] = task
        task.start(executor, cbExecutor)
    }

    private fun cancelWork(target: ImageView) {
        submitted.remove(target)?.end()
    }
}

private class UrlTask(
        private val url: String,
        target: ImageView
) {
    val weakTarget = WeakReference(target)
    var inner: Future<*>? = null

    fun start(executor: ExecutorService, callbackEx: Executor) {
        inner?.cancel(true)
        inner = executor.submit {
            val stream = URL(url).content as InputStream
            val drawable = Drawable.createFromStream(stream, null)

            callbackEx.execute {
                weakTarget.get()?.setImageDrawable(drawable)
            }
        }
    }

    fun end() {
        weakTarget.clear()
        inner?.cancel(true)
    }
}
