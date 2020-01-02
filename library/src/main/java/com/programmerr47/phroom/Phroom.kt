package com.programmerr47.phroom

import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import java.io.InputStream
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.Future

class Phroom {
    private val executor = Executors.newFixedThreadPool(5)
    private val uiHandler = Handler(Looper.getMainLooper())

    private val submitted = HashMap<String, Future<*>>()

    fun load(url: String, target: ImageView) {
        submitted[url]?.cancel(true)
        val removed = submitted.remove(url)

        if (removed != null) {
            Log.v("FUCK", "remove submitted for $url")
        }
        Log.v("FUCK", "started new task for $url")

        val newTask = Task(url).also { it.target = target }
        target.setImageDrawable(null) //TODO add progress drawable

        val new = executor.submit {
            val stream = URL(url).content as InputStream
            val drawable = Drawable.createFromStream(stream, null)
            uiHandler.post {
                newTask.target?.let {
                    Log.v("FUCK", "setImageDrawable for $url")
                    it.setImageDrawable(drawable)
                }
            }
        }
        submitted[url] = new
    }
}

private class Task(
    val url: String
) : View.OnAttachStateChangeListener {

    @Volatile
    var target: ImageView? = null
        @Synchronized
        set(value) {
            field?.removeOnAttachStateChangeListener(this)
            field = value
            value?.addOnAttachStateChangeListener(this)
        }

    override fun onViewDetachedFromWindow(v: View) {
        synchronized(this) {
            if (v == target) {
                Log.v("FUCK", "destruct view for $url")
                target = null
            }
        }
    }

    override fun onViewAttachedToWindow(v: View) {}
}