package com.programmerr47.phroom.targets

import android.graphics.Bitmap

//Made it internal for now, but we can easily expose it
internal interface Target {
    fun onNew()
    fun onStart()
    fun onSuccess(bitmap: Bitmap)
    fun onFailure(e: Throwable)
}
