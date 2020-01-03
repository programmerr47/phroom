package com.programmerr47.phroom

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import java.lang.IllegalStateException
import kotlin.DeprecationLevel.ERROR

interface TaskConfig {
    val loadingPlaceholder: Drawable?
    val errorPlaceholder: Drawable?
}

class TaskConfigBuilder(private val context: Context) : TaskConfig {
    override var loadingPlaceholder: Drawable? = null
    override var errorPlaceholder: Drawable? = null

    var loadingPlaceholderRes: Int
        @Deprecated(NO_GETTER, level = ERROR) get() = throw IllegalStateException(NO_GETTER)
        set(@DrawableRes value) {
            loadingPlaceholder = ContextCompat.getDrawable(context, value)
        }

    var errorPlaceholderRes: Int
        @Deprecated(NO_GETTER, level = ERROR) get() = throw IllegalStateException(NO_GETTER)
        set(@DrawableRes value) {
            errorPlaceholder = ContextCompat.getDrawable(context, value)
        }

    companion object {
        private const val NO_GETTER = "No getter for that field"
    }
}