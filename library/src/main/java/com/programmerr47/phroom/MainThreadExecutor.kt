package com.programmerr47.phroom

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor

internal class MainThreadExecutor : Executor {
    private val uiHandler = Handler(Looper.getMainLooper())

    override fun execute(command: Runnable) {
        uiHandler.post(command)
    }
}