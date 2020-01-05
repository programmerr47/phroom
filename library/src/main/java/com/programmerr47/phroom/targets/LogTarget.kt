package com.programmerr47.phroom.targets

import android.graphics.Bitmap
import com.programmerr47.phroom.logInternal

internal class LogTarget(private val origin: Target) : Target {
    private var initialNs = 0L
    private var startNs = 0L

    override fun onNew() {
        initialNs = System.nanoTime()
        origin.onNew()
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
