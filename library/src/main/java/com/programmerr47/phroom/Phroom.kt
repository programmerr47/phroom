package com.programmerr47.phroom

import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import java.io.InputStream
import java.net.URL
import java.util.concurrent.Executors

class Phroom {
    private val executor = Executors.newFixedThreadPool(5)
    private val uiHandler = Handler(Looper.getMainLooper())

    fun load(url: String, target: ImageView) {
        //TODO replace that with more nice approach
        executor.execute {
            val stream = URL(url).content as InputStream
            val drawable = Drawable.createFromStream(stream, null)
            uiHandler.post { target.setImageDrawable(drawable) }
        }
    }
}