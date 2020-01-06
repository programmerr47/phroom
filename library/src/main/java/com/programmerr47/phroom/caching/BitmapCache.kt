package com.programmerr47.phroom.caching

import android.graphics.Bitmap

internal interface BitmapCache {
    fun get(key: String): Bitmap?
    fun put(key: String, bitmap: Bitmap)
}
