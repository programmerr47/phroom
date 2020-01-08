package com.programmerr47.phroom.caching

import android.graphics.Bitmap
import androidx.collection.LruCache
import com.programmerr47.phroom.BitmapSpec
import com.programmerr47.phroom.atLeastKitkat
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

internal interface MemoryCache {
    fun get(key: BitmapSpec): Pair<Bitmap?, Boolean>
    fun put(key: BitmapSpec, bitmap: Bitmap)
}

internal class LruMemoryCache(
    private val maxBytesCount: Int
) : MemoryCache {
    private val lru = object : LruCache<BitmapSpec, Bitmap>(maxBytesCount) {
        override fun sizeOf(key: BitmapSpec, value: Bitmap): Int =
            with(value) { if (atLeastKitkat()) allocationByteCount else byteCount }

        override fun entryRemoved(evicted: Boolean, key: BitmapSpec, oldValue: Bitmap, newValue: Bitmap?) {
            super.entryRemoved(evicted, key, oldValue, newValue)
            specMap[key.url]?.remove(key)

            if (originMap[key.url] == key) {
                originMap.remove(key.url)
            }
        }
    }

    //current specs, presented in lru, but grouped by same url, for more fast search of similar specs
    private val specMap = HashMap<String, MutableList<BitmapSpec>>()

    //map indicates precedence of original bitmaps in cache (thus, this is totally controlled
    //by lru cache). Since we have a some algorithm of choosing alternative bitmap for desired
    //bitmap spec, we want can have a possibility to request a bitmap for really big view.
    //But the bitmap can be small itself, and instead of return that small bitmap with mark it
    //as dirty (to make a call to server to get more big bitmap), we need to have an ability to
    //return a small bitmap and tell, that this is original one and we can not do anything with
    //that, you will not receive more bit bitmap, so use this one for sure.
    //Clearly, if we had no algorithm, for picking up 'similar' bitmap, things were much-much
    //easier :)
    private val originMap = HashMap<String, BitmapSpec>()

    private val _lock = ReentrantReadWriteLock()

    override fun get(key: BitmapSpec): Pair<Bitmap?, Boolean> = _lock.read {
        val cached = lru[key]
        if (cached != null) {
            cached to false
        } else {
            getSimilar(key)
        }
    }

    //Possibly we don't need such complex logic. Currently we saving bitmaps with preserving
    //aspect ration (without applying any transformation except scaling). If in future we will
    //have transformations, that affects cropping things, this method can become more more complex
    //And there is no 100% possibility, that it will not be removed. But hope I will handle that
    //properly, too :)
    private fun getSimilar(key: BitmapSpec): Pair<Bitmap?, Boolean> {
        val allSpecs = specMap[key.url] ?: emptyList<BitmapSpec>()

        var maxLowerBound: BitmapSpec? = null
        allSpecs.forEach {
            if (it.tHeight > key.tHeight && it.tWidth > key.tWidth) {

                //to properly compare and understand which more spec is more appropriate,
                //we need to take bitmaps, that are cached by this spec, but since it is
                //just a prototype we will make some assumptions, without getting bitmap
                //that is not so good. But I more and more think that whole 'getSimilar'
                //thing is overkill right now
                return lru[it] to false
            } else {
                if (it.url in originMap) {
                    return lru[it] to false
                }

                //as describe above, computing areas is not such a good solution to find
                //"closest" bitmap to desired one. Instead we need to analyze desired sizes,
                //taking into account the actual dimensions of bitmaps and their aspect ration
                //but this is too much for just a demonstration app. Moreover, as I said
                //even this "getSimilar" method is overkill, apparently :(
                val mlb = maxLowerBound
                if (mlb == null || mlb.area < it.area) {
                    maxLowerBound = it
                }
            }
        }

        return maxLowerBound?.let { lru[it] to true } ?: null to false
    }

    //Possible it is much clear to have an explicit parameter isOriginal, that indicates,
    //whether this bitmap is original one and, thus, should be placed placed in special originalMap
    //But we assume that is we want to fit the desired specs that no dimensions of result map
    //must by less then desired. Otherwise it means, that we have an original bitmap.
    //Because we only can downscale the bitmaps, not upscale them
    override fun put(key: BitmapSpec, bitmap: Bitmap): Unit = _lock.write {
        lru.put(key, bitmap)
        specMap.getOrPut(key.url) { mutableListOf() }.add(key)

        if (bitmap.width < key.tWidth || bitmap.height < key.tHeight) {
            originMap[key.url] = key
        }
    }
}
