package com.programmerr47.phroom.caching

import android.graphics.Bitmap
import androidx.collection.LruCache
import com.programmerr47.phroom.atLeastKitkat
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class MemoryCache(
    private val maxBytesCount: Int
) : BitmapCache {
    private val lru = object : LruCache<String, Bitmap>(maxBytesCount) {
        override fun sizeOf(key: String, value: Bitmap): Int =
            with(value) { if (atLeastKitkat()) allocationByteCount else byteCount }
    }

    private val _lock = ReentrantReadWriteLock()

    override fun get(key: String): Bitmap? = _lock.read { lru.get(key) }

    override fun put(key: String, bitmap: Bitmap) {
        _lock.write { lru.put(key, bitmap) }
    }
}
