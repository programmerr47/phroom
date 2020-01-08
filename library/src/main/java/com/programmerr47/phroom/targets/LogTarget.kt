package com.programmerr47.phroom.targets

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import com.programmerr47.phroom.logInternal

internal class LogTarget(private val origin: Target) : Target by origin {
    private var initialNs = System.nanoTime()
    private var startNs = System.nanoTime()

    override fun onNew(drawable: Drawable?) {
        initialNs = System.nanoTime()
        origin.onNew(drawable)
    }

    override fun onStart() {
        startNs = System.nanoTime()
        origin.onStart()
    }

    override fun onSuccess(bitmap: Bitmap) {
        logTime(true)
        origin.onSuccess(bitmap)
    }

    override fun onFailure(e: Throwable) {
        logTime(false)
        origin.onFailure(e)
    }

    private fun logTime(success: Boolean) {
        val end = System.nanoTime()
        val lifeTime = (end - initialNs).toMillis()
        val loadTime = (end - startNs).toMillis()
        logInternal("[$lifeTime ms | $loadTime ms] $origin is finished with ${if (success) "SUCCESS" else "FAIL"}")
    }

    private fun Long.toMillis() = this / 1_000_000
}
