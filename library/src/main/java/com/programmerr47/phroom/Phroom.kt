package com.programmerr47.phroom

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.widget.ImageView
import androidx.annotation.WorkerThread
import com.programmerr47.phroom.caching.BitmapCache
import com.programmerr47.phroom.caching.DiskCache
import com.programmerr47.phroom.caching.LruMemoryCache
import com.programmerr47.phroom.caching.MemoryCache
import com.programmerr47.phroom.targets.LogTarget
import com.programmerr47.phroom.targets.MainThreadTarget
import com.programmerr47.phroom.targets.Target
import com.programmerr47.phroom.targets.ViewTarget
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.net.URL
import java.util.WeakHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.math.min

class Phroom {
    private val executor = Executors.newFixedThreadPool(5)
    private val cbExecutor = MainThreadExecutor()
    private var diskCache: BitmapCache = DUMMY_CACHE
    private var memoryCache: MemoryCache = DUMMY_MEM_CACHE

    private val submitted = WeakHashMap<ImageView, UrlTask>()

    fun load(url: String, targetView: ImageView, config: TaskConfigBuilder.() -> Unit = {}) {
        checkCaches(targetView.context)
        cancelWork(targetView)

        val config = TaskConfigBuilder(targetView.context).apply(config)
        val target = MainThreadTarget(ViewTarget(targetView, config), cbExecutor)
        val task = UrlTask(url, diskCache, memoryCache, target)
        submitted[targetView] = task
        task.start(executor)
    }

    private fun cancelWork(target: ImageView) {
        submitted.remove(target)?.end()
    }

    private fun checkCaches(context: Context) {
        if (diskCache == DUMMY_CACHE) {
            diskCache = diskCache(context)
        }

        if (memoryCache == DUMMY_MEM_CACHE) {
            memoryCache = LruMemoryCache(calculateMemoryCacheSize(context))
        }
    }

    private fun diskCache(context: Context): DiskCache {
        val cacheDir = with(context.applicationContext) { externalCacheDir ?: cacheDir }
        val phroomDir = File(cacheDir, CACHE_DIR_NAME)
        return DiskCache(if (phroomDir.mkdir()) phroomDir else cacheDir)
    }

    companion object {
        private const val CACHE_DIR_NAME = "phroom"

        private val DUMMY_CACHE = object : BitmapCache {
            override fun get(key: String): Bitmap? = null
            override fun put(key: String, bitmap: Bitmap) = Unit
        }

        private val DUMMY_MEM_CACHE = object : MemoryCache {
            override fun get(key: BitmapSpec): Pair<Bitmap?, Boolean> = null to false
            override fun put(key: BitmapSpec, bitmap: Bitmap) = Unit
        }
    }
}

private class UrlTask(
    private val url: String,
    private val diskCache: BitmapCache,
    private val memoryCache: MemoryCache,
    target: Target
) {
    var target: Target? = LogTarget(target)
    var inner: Future<*>? = null

    fun start(executor: ExecutorService) {
        inner?.cancel(true)

        target?.let { start(it, executor) }
    }

    private fun start(target: Target, executor: ExecutorService) {
        val size = target.size.check()

        if (size != 0 to 0) {
            val (cached, dirty) = memoryCache.get(BitmapSpec(url, size.first, size.second))
            if (cached != null && !dirty) {
                target.onSuccess(cached)
            } else if (cached != null && dirty) {
                target.onNew(BitmapDrawable(cached))
                inner = executor.enqueueJob(size)
            } else {
                target.onNew(null)
                inner = executor.enqueueJob(size)
            }
        } else {
            target.onNew(null)
            inner = executor.enqueueJob(size)
        }
    }

    private fun ExecutorService.enqueueJob(size: Pair<Int, Int>) = submit {
        target?.onStart()

        if (size == 0 to 0) {
            val width = target?.size?.width ?: 0
            val height = target?.size?.height ?: 0

            val spec = BitmapSpec(url, width, height)
            val (cached, dirty) = memoryCache.get(spec)
            if (cached != null && !dirty) {
                target?.onSuccess(cached)
            } else {
                fetchUrl(spec)
            }
        } else {
            fetchUrl(BitmapSpec(url, size.first, size.second))
        }
    }

    @WorkerThread
    private fun fetchUrl(spec: BitmapSpec) {
        runCatching {
            val originalBitmap = diskCache.get(spec.url) ?: BitmapFactory.decodeStream(BufferedInputStream(URL(spec.url).content as InputStream)).also {
                diskCache.put(spec.url, it)
            }

            transform(originalBitmap, spec)
        }
            .onSuccess { memoryCache.put(spec, it) }
            .onSuccess { target?.onSuccess(it) }
            .onFailure { target?.onFailure(it) }
    }

    @WorkerThread
    private fun transform(bitmap: Bitmap, spec: BitmapSpec): Bitmap {
        val scaleFactor = scaleFactor(bitmap, spec)
        if (scaleFactor == 1f) return bitmap

        val matrix = Matrix().apply { postScale(scaleFactor, scaleFactor) }
        val transformed: Bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        bitmap.recycle()
        return transformed
    }

    @WorkerThread
    private fun scaleFactor(bitmap: Bitmap, spec: BitmapSpec): Float {
        if (bitmap.width <= spec.tWidth || bitmap.height <= spec.tHeight) return 1f

        return min(
            spec.tWidth.toFloat() / bitmap.width,
            spec.tHeight.toFloat() / bitmap.height
        )
    }

    fun end() {
        target = null
        inner?.cancel(true)
    }
}
