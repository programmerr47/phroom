package com.programmerr47.phroom.targets

import android.graphics.Bitmap
import android.graphics.drawable.Drawable

//Made it internal for now, but we can easily expose it
internal interface Target {
    val size: Size

    fun onNew(initial: Bitmap?)
    fun onStart()
    fun onSuccess(bitmap: Bitmap)
    fun onFailure(e: Throwable)

    /**
     * The implementation is not guarante to immediately return [width] or [height].
     * In fact they will block thread until actual view will be measured.
     *
     * Thus, use [width] and [height] only in background thread.
     *
     * If you want to get size right here and right now, use [check] method,
     * but it is not guarantee to return actual size. For example, if we is not measured yet,
     * this method will return [0, 0]
     */
    interface Size {
        val width: Int
        val height: Int

        fun check(): Pair<Int, Int>
    }
}
