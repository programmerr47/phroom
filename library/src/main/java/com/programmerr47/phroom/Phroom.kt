package com.programmerr47.phroom

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.ImageView
import java.io.File
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
    private var diskCache: BitmapCache = DUMMY_CACHE

    private val submitted = WeakHashMap<ImageView, UrlTask>()

    fun load(url: String, target: ImageView, config: TaskConfigBuilder.() -> Unit = {}) {
        checkDiskCache(target.context)
        cancelWork(target)

        val task = UrlTask(url, TaskConfigBuilder(target.context).apply(config), diskCache, target)
        submitted[target] = task
        task.start(executor, cbExecutor)
    }

    private fun cancelWork(target: ImageView) {
        submitted.remove(target)?.end()
    }

    private fun checkDiskCache(context: Context) {
        if (diskCache == DUMMY_CACHE) {
            diskCache = diskCache(context)
        }
    }

    private fun diskCache(context: Context): DiskCache {
        val cacheDir = context.externalCacheDir ?: context.cacheDir
        val phroomDir = File(cacheDir, CACHE_DIR_NAME)
        return DiskCache(if (phroomDir.mkdir()) phroomDir else cacheDir)
    }

    companion object {
        private const val CACHE_DIR_NAME = "phroom"

        private val DUMMY_CACHE = object : BitmapCache {
            override fun get(key: String): Bitmap? = null
            override fun put(key: String, bitmap: Bitmap) {}
        }
    }
}

private class UrlTask(
        private val url: String,
        private val config: TaskConfig,
        private val diskCache: BitmapCache,
        target: ImageView
) {
    val weakTarget = WeakReference(target)
    var inner: Future<*>? = null

    fun start(executor: ExecutorService, callbackEx: Executor) {
        inner?.cancel(true)
        weakTarget.get()?.setImageDrawable(config.loadingPlaceholder)
        inner = executor.submit {
            val start = System.nanoTime()
            val bitmap = runCatching {
                diskCache.get(url) ?: BitmapFactory.decodeStream(URL(url).content as InputStream).also {
                    diskCache.put(url, it)
                }
            }.getOrNull()

            Log.d("Phroom", "$url was loading for ${(System.nanoTime() - start) / 1_000_000} ms")

            callbackEx.execute {
                weakTarget.get()?.let {
                    if (bitmap != null) it.setImageBitmap(bitmap)
                    else it.setImageDrawable(config.errorPlaceholder)
                }
            }
        }
    }

    fun end() {
        weakTarget.clear()
        inner?.cancel(true)
    }
}
