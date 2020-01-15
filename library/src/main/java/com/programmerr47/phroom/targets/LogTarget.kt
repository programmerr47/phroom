package com.programmerr47.phroom.targets

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import com.programmerr47.phroom.logInternal

internal class LogTarget(private val origin: Target) : Target by origin {
    private var initialNs = System.nanoTime()
    private var startNs = System.nanoTime()

    override fun onNew(initial: Bitmap?) {
        initialNs = System.nanoTime()
        origin.onNew(initial)
    }

    override fun onStart() {
        startNs = System.nanoTime()
        origin.onStart()
    }

    override fun onSuccess(bitmap: Bitmap) {
        logTime(Result.success(bitmap))
        origin.onSuccess(bitmap)
    }

    override fun onFailure(e: Throwable) {
        logTime(Result.failure(e))
        origin.onFailure(e)
    }

    private fun logTime(result: Result<Bitmap>) {
        val end = System.nanoTime()
        val lifeTime = (end - initialNs).toMillis()
        val loadTime = (end - startNs).toMillis()
        logInternal("[$lifeTime ms | $loadTime ms] $origin is finished with ${if (result.isSuccess) "SUCCESS" else "FAIL (reason ${result.exceptionOrNull()})"}")
    }

    private fun Long.toMillis() = this / 1_000_000
}
