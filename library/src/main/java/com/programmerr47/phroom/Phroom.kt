package com.programmerr47.phroom

import android.graphics.drawable.Drawable
import android.util.Log
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

    fun load(url: String, target: ImageView, config: TaskConfigBuilder.() -> Unit = {}) {
        cancelWork(target)
        target.setImageDrawable(null)  //TODO add progress drawable

        val task = UrlTask(url, TaskConfigBuilder(target.context).apply(config), target)
        submitted[target] = task
        task.start(executor, cbExecutor)
    }

    private fun cancelWork(target: ImageView) {
        submitted.remove(target)?.end()
    }
}

private class UrlTask(
        private val url: String,
        private val config: TaskConfig,
        target: ImageView
) {
    val weakTarget = WeakReference(target)
    var inner: Future<*>? = null

    fun start(executor: ExecutorService, callbackEx: Executor) {
        inner?.cancel(true)
        weakTarget.get()?.setImageDrawable(config.loadingPlaceholder)
        inner = executor.submit {
            val drawable = runCatching { Drawable.createFromStream(URL(url).content as InputStream, null) }.getOrNull()

            callbackEx.execute {
                Log.v("FUCK", "${url}: $drawable, ${config.errorPlaceholder}")
                weakTarget.get()?.setImageDrawable(drawable ?: config.errorPlaceholder)
            }
        }
    }

    fun end() {
        weakTarget.clear()
        inner?.cancel(true)
    }
}
