package com.programmerr47.phroom.sample

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Rect
import android.graphics.drawable.Drawable
import kotlin.math.min

class CenterInsideDrawable(
    private val original: Drawable
) : Drawable() {
    override fun draw(canvas: Canvas) {
        intrinsicHeight
        original.intrinsicHeight
        original.draw(canvas)
    }

    override fun setAlpha(alpha: Int) {
        original.alpha = alpha
    }

    override fun getOpacity() = original.opacity

    override fun setColorFilter(colorFilter: ColorFilter?) {
        original.colorFilter = colorFilter
    }

    override fun getIntrinsicHeight() = original.intrinsicHeight

    override fun getIntrinsicWidth() = original.intrinsicWidth

    override fun onBoundsChange(bounds: Rect) {
        val ratio = original.intrinsicHeight.toFloat() / original.intrinsicWidth
        var originalWidth: Float = min(bounds.width(), original.intrinsicWidth).toFloat()
        val tempHeight = originalWidth * ratio

        val originalHeight: Float = if (tempHeight > bounds.height()) {
            originalWidth = bounds.height() / ratio
            bounds.height().toFloat()
        } else {
            tempHeight
        }

        val widthOffset = ((bounds.width() - originalWidth) / 2).toInt()
        val heightOffset = ((bounds.height() - originalHeight) / 2).toInt()
        original.setBounds(
            bounds.left + widthOffset,
            bounds.top + heightOffset,
            bounds.right - widthOffset,
            bounds.bottom - heightOffset
        )
    }
}