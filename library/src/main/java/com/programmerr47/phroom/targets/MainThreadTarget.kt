package com.programmerr47.phroom.targets

import android.graphics.Bitmap
import java.util.concurrent.Executor

internal class MainThreadTarget(
    private val origin: Target,
    private val executor: Executor
) : Target {
    override fun onNew() = executor.execute { origin.onNew() }

    override fun onStart() = executor.execute { origin.onStart() }

    override fun onSuccess(bitmap: Bitmap) = executor.execute { origin.onSuccess(bitmap) }

    override fun onFailure(e: Throwable) = executor.execute { origin.onFailure(e) }
}
