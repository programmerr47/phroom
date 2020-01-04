package com.programmerr47.phroom

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor

//This class should be internal, since it actually created for the purposes of library
//But Sample module currently needs MainThreadExecutor for PagingLibrary and
//I didn't want to duplicate it. So I've made it public :(
class MainThreadExecutor : Executor {
    private val uiHandler = Handler(Looper.getMainLooper())

    override fun execute(command: Runnable) {
        uiHandler.post(command)
    }
}
