package com.programmerr47.phroom

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.widget.ImageView
import androidx.annotation.WorkerThread
import com.programmerr47.phroom.caching.BitmapCache
import com.programmerr47.phroom.caching.DiskCache
import com.programmerr47.phroom.caching.LruMemoryCache
import com.programmerr47.phroom.caching.MemoryCache
import com.programmerr47.phroom.caching.ThumbnailsCache
import com.programmerr47.phroom.targets.LogTarget
import com.programmerr47.phroom.targets.MainThreadTarget
import com.programmerr47.phroom.targets.Target
import com.programmerr47.phroom.targets.ViewTarget
import java.io.File
import java.io.InputStream
import java.net.URL
import java.util.WeakHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class Phroom(appContext: Context) {
    private val executor = Executors.newFixedThreadPool(5)
    private val cbExecutor = MainThreadExecutor()
    private var diskCache: BitmapCache = DUMMY_CACHE
    private var memoryCache: MemoryCache = DUMMY_MEM_CACHE
    private var thumbCache: BitmapCache = DUMMY_CACHE

    private val submitted = WeakHashMap<Target, UrlTask>()

    init {
        checkCaches(appContext.applicationContext)
    }

    fun load(url: String, targetView: ImageView, config: TaskConfigBuilder.() -> Unit = {}) =
        load(url, ViewTarget(targetView, TaskConfigBuilder(targetView.context).apply(config)))

    fun load(url: String, target: Target) {
        cancelWork(target)

        val cvTarget = MainThreadTarget(target, cbExecutor)
        val task = UrlTask(url, diskCache, memoryCache, thumbCache, cvTarget)
        submitted[target] = task
        task.start(executor)
    }

    private fun cancelWork(target: Target) {
        submitted.remove(target)?.end()
    }

    private fun checkCaches(context: Context) {
        if (diskCache == DUMMY_CACHE) {
            diskCache = diskCache(context)
        }

        if (memoryCache == DUMMY_MEM_CACHE) {
            memoryCache = LruMemoryCache(calculateMemoryCacheSize(context))
        }
        
        if (thumbCache == DUMMY_CACHE) {
            //For now we will use same memory amount for thumbnails as for memory cache
            //but we can add a global config, to regulate range [0, 1] of additional
            //memory cache size multiplier for thumbnails
            thumbCache = ThumbnailsCache(calculateMemoryCacheSize(context))
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
    private val thumbCache: BitmapCache,
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
                target.onNew(cached)
                inner = executor.enqueueJob(size)
            } else {
                target.onNew(thumbCache.get(url))
                inner = executor.enqueueJob(size)
            }
        } else {
            target.onNew(thumbCache.get(url))
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
            diskCache.get(spec.url) ?: BitmapFactory.decodeStream(URL(spec.url).content as InputStream).also {
                diskCache.put(spec.url, it)
            }
        }
            .onSuccess {
                val thumbnail = it.thumbnail()
                val transformed = transform(it, spec)
                memoryCache.put(spec, transformed)
                thumbCache.put(spec.url, thumbnail)
                target?.onSuccess(transformed)
            }
            .onFailure { target?.onFailure(it) }
    }

    @WorkerThread
    private fun transform(bitmap: Bitmap, spec: BitmapSpec): Bitmap {
        val scaleFactor = scaleFactor(bitmap, spec)
        if (scaleFactor == 1f) return bitmap

        val transformed = bitmap.scaledCopy(scaleFactor)
        bitmap.recycle()
        return transformed
    }

    @WorkerThread
    //pick a scale factor with preserving the aspect ratio
    private fun scaleFactor(bitmap: Bitmap, spec: BitmapSpec): Float {
        if (bitmap.width <= spec.tWidth || bitmap.height <= spec.tHeight) return 1f

        return min(1f, max(
            spec.tWidth.toFloat() / bitmap.width,
            spec.tHeight.toFloat() / bitmap.height
        ))
    }

    @WorkerThread
    private fun Bitmap.thumbnail(): Bitmap {
        //target area is 40 pixels which equals to 6x6 bitmap or 5x8 etc
        val scaleFactor = sqrt(height.toFloat() * 40 / width) / height
        return scaledCopy(scaleFactor).apply { blur() }
    }

    @WorkerThread
    private fun Bitmap.scaledCopy(scaleFactor: Float): Bitmap {
        val matrix = Matrix().apply { postScale(scaleFactor, scaleFactor) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    @WorkerThread
    private fun Bitmap.blur() {
        val pix = IntArray(width * height)
        val newPix = IntArray(width * height)
        getPixels(pix, 0, width, 0, 0, width, height)

        val rgb = IntArray(3)
        var rgbSum = IntArray(3)
        val radius = 1 //let's say 1, but explicitly define variable for easy adjusting that
        for (y in 0 until height) {
            for (x in 0 until width) {
                rgbSum.fill(0)
                calculateSum(pix, x, y, radius, rgb, rgbSum)

                val pixel = pix[y * width + x]
                //first part is alpha channel, we want to preserve it
                newPix[y * width + x] = (0xff000000.toInt() and pixel) or (rgbSum[0] shl 16) or (rgbSum[1] shl 8) or rgbSum[2]
            }
        }
        setPixels(newPix, 0, width, 0, 0, width, height)
    }

    //This is place we can optimize for sure. Because some part of sums we recalculate all over
    //again. I've maid naive algorithm, just to see results first
    //TODO optimize blur calculation
    //TODO also think about little bit change algorithm for blur on edges
    private fun Bitmap.calculateSum(pix: IntArray, x: Int, y: Int, radius: Int, rgb: IntArray, rgbSum: IntArray) {
        var count = 0
        for (blurY in y - radius..y + radius) {
            for (blurX in x - radius..x + radius) {
                if (blurX in 0 until width && blurY in 0 until height) {
                    val pixel = pix[blurY * width + blurX]
                    pixel.spreadOutColor(rgb)
                    rgbSum.sumEach(rgb)
                    count++
                }
            }
        }

        rgbSum.divEach(count)
    }

    private fun IntArray.sumEach(other: IntArray) {
        require(size == other.size)
        for (i in 0 until size) {
            this[i] += other[i]
        }
    }

    private fun IntArray.divEach(count: Int) {
        for (i in 0 until size) {
            this[i] /= count
        }
    }

    private fun Int.spreadOutColor(rgb: IntArray) {
        rgb[0] = (this and 0xff0000) shr 16
        rgb[1] = (this and 0x00ff00) shr 8
        rgb[2] = (this and 0x0000ff)
    }

    fun end() {
        target = null
        inner?.cancel(true)
    }
}
