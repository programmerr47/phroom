package com.programmerr47.phroom

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.ImageView
import com.programmerr47.phroom.targets.LogTarget
import com.programmerr47.phroom.targets.MainThreadTarget
import com.programmerr47.phroom.targets.Target
import com.programmerr47.phroom.targets.ViewTarget
import java.io.File
import java.io.InputStream
import java.net.URL
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

class Phroom {
    private val executor = Executors.newFixedThreadPool(5)
    private val cbExecutor = MainThreadExecutor()
    private var diskCache: BitmapCache = DUMMY_CACHE

    private val submitted = WeakHashMap<ImageView, UrlTask>()

    fun load(url: String, targetView: ImageView, config: TaskConfigBuilder.() -> Unit = {}) {
        checkDiskCache(targetView.context)
        cancelWork(targetView)

        val config = TaskConfigBuilder(targetView.context).apply(config)
        val target = MainThreadTarget(ViewTarget(targetView, config), cbExecutor)
        val task = UrlTask(url, diskCache, target)
        submitted[targetView] = task
        task.start(executor)
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
            override fun put(key: String, bitmap: Bitmap) = Unit
        }
    }
}

private class UrlTask(
        private val url: String,
        private val diskCache: BitmapCache,
        target: Target
) {
    var target: Target? = LogTarget(target)
    var inner: Future<*>? = null

    fun start(executor: ExecutorService) {
        inner?.cancel(true)
        target?.onNew()
        inner = executor.submit {
            target?.onStart()
            runCatching {
                diskCache.get(url) ?: BitmapFactory.decodeStream(URL(url).content as InputStream).also {
                    diskCache.put(url, it)
                }
            }
                .onSuccess { target?.onSuccess(it) }
                .onFailure { target?.onFailure(it) }
        }
    }

    fun end() {
        target = null
        inner?.cancel(true)
    }
}
