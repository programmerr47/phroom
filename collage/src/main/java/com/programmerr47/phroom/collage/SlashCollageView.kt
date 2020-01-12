package com.programmerr47.phroom.collage

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import com.programmerr47.phroom.Phroom
import com.programmerr47.phroom.kutils.area
import com.programmerr47.phroom.kutils.nextFloat
import com.programmerr47.phroom.targets.LockTargetSize
import com.programmerr47.phroom.targets.Target
import kotlin.math.max
import kotlin.math.nextDown
import kotlin.math.pow
import kotlin.properties.Delegates.observable
import kotlin.random.Random

class SlashCollageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val generator = Generator()

    private var originCollage: List<RectF> by observable(emptyList()) { _, _, new ->
        invalidateFinalCollage(new)
    }
    private var finalCollage: List<RectF> = emptyList()

    private var urls: List<String> = emptyList()
    private var collageTargets: Array<CollageTarget> = emptyArray()

    lateinit var phroom: Phroom

    var framePadding: Int by observable(0) { _, old, new ->
        if (old != new) {
            invalidateFinalCollage(originCollage)
        }
    }

    //TODO add invalidate, because right now you have to change them before Targets will be built
    var frameColor: Int = 0
    var errorFrameColor: Int = 0

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthNotUnspecified = MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED
        val heightNotUnspecified = MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.UNSPECIFIED

        if (widthNotUnspecified && heightNotUnspecified && originCollage.isEmpty() && !urls.isEmpty()) {
            originCollage = generateCollage(
                MeasureSpec.getSize(widthMeasureSpec),
                MeasureSpec.getSize(heightMeasureSpec)
            )
        }

        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onDraw(canvas: Canvas) {
        finalCollage.forEachIndexed { i, rect ->
            collageTargets[i].drawable.draw(canvas)
        }
    }

    fun generateAgain(urls: List<String>) {
        this.urls = urls
        collageTargets = Array(urls.size) {
            CollageTarget(
                frameColor,
                errorFrameColor,
                { invalidate() },
                resources
            )
        }
        originCollage = generateCollage(measuredWidth, measuredHeight)

        invalidate()

        urls.forEachIndexed { i, url ->
            phroom.load(url, collageTargets[i])
        }
    }

    private fun generateCollage(width: Int, height: Int): List<RectF> {
        if (width > 0 && height > 0) {
            return generator.generate(
                urls.size, Rect(paddingStart, paddingTop, width - paddingEnd, height - paddingEnd)
            )
        } else {
            return emptyList()
        }
    }

    private fun invalidateFinalCollage(originCollage: List<RectF>) {
        finalCollage = originCollage.map {
            RectF(it).apply {
                left += framePadding
                top += framePadding
                right -= framePadding
                bottom -= framePadding
            }
        }

        collageTargets.forEachIndexed { i, target -> target.onMeasured(finalCollage[i]) }
        invalidate()
    }

    private class Generator {
        private val rnd = Random(System.currentTimeMillis())
        private val queue = ArrayList<RectF>(20)
        private val weights = ArrayList<Float>(20)
        private val splitPoint = PointF()

        fun generate(n: Int, initial: Rect) = generate(n, RectF(initial))

        fun generate(n: Int, initial: RectF): List<RectF> {
            if (n == 0) return emptyList()

            queue.clear()
            weights.clear()

            queue.ensureCapacity(n)
            weights.ensureCapacity(n)

            queue.add(initial)
            weights.add(initial.weight)

            var sum = initial.weight
            while (queue.size < n) {
                val choice = rnd.nextFloat(sum)
                val index = findIndex(choice, weights)

                val rectToSplit = queue.removeAt(index)
                val removedWeight = weights.removeAt(index)
                findSplitPoint(rectToSplit, splitPoint)

                val newRect = if (splitPoint.x == 0f) { //means we will cut horizontally
                    cutHorizontally(rectToSplit, splitPoint.y)
                } else { //means we will cut vertically
                    cutVertically(rectToSplit, splitPoint.x)
                }

                queue.add(rectToSplit)
                queue.add(newRect)
                val rectToSplitWeight = rectToSplit.weight
                val newRectWeight = newRect.weight
                weights.add(rectToSplitWeight)
                weights.add(newRectWeight)

                sum += rectToSplitWeight + newRectWeight - removedWeight
            }

            return queue
        }

        private fun findIndex(choice: Float, weights: List<Float>): Int {
            var controlSum = 0f
            weights.forEachIndexed { i, weight ->
                controlSum += weight

                if (controlSum > choice) {
                    return i
                }
            }

            return weights.lastIndex
        }

        private fun findSplitPoint(rect: RectF, out: PointF) {
            val until = (rect.height() + rect.width()) * 2
            val choice = rnd.nextFloat(until)

            if (choice < 2 * rect.height()) { //means that choice lies on left side
                out.x = 0f
                out.y = rect.height() / 4 + choice / 4 //picking point between 1/4 and 3/4 of rect height
            } else { //means that choice lies on top side
                out.x = rect.width() / 4 + (choice - 2 * rect.height()) / 4 //picking point between 1/4 and 3/4 of rect width
                out.y = 0f
            }
        }

        //Too save space we will not returns to new objects of RectF,
        //but instead one new object and old one will be resized
        //To make it more clean, we could use object pool for that,
        //but I decided to keep this approach as more simple one
        private fun cutHorizontally(rect: RectF, yRelative: Float): RectF {
            require(yRelative < rect.height())

            val splitY = rect.top + yRelative
            val bottomRect = RectF(rect).apply { top = splitY }
            rect.bottom = splitY
            return bottomRect
        }

        private fun cutVertically(rect: RectF, xRelative: Float): RectF {
            require(xRelative < rect.width())

            val splitX = rect.left + xRelative
            val rightRect = RectF(rect).apply { left = splitX }
            rect.right = splitX
            return rightRect
        }

        //Special formula to help random build more pretty collages
        private val RectF.weight get() = area.pow(0.5f) * maxRatio.pow(2)

        private val RectF.maxRatio get() = max(width() / height(), height() / width())
    }

    private class CollageTarget(
        private val frameColor: Int,
        private val errorFrameColor: Int,
        private val invalidate: () -> Unit,
        private val resources: Resources
    ) : Target {
        var drawable: Drawable = ColorDrawable(frameColor)
            private set(value) {
                field = value
                applyMeasurement()
                invalidate()
            }

        private var sizeRect: RectF = RectF(0f, 0f, 0f, 0f)

        override val size = Size()

        override fun onNew(initial: Bitmap?) {
            if (initial != null) {
                drawable = BitmapDrawable(resources, initial)
            } else {
                drawable = ColorDrawable(frameColor)
            }
        }

        override fun onStart() {}

        override fun onSuccess(bitmap: Bitmap) {
            drawable = BitmapDrawable(resources, bitmap)
        }

        override fun onFailure(e: Throwable) {
            drawable = ColorDrawable(errorFrameColor)
        }

        fun onMeasured(rect: RectF) {
            sizeRect = rect
            applyMeasurement()
            size.onMeasured(rect)
        }

        private fun applyMeasurement() {
            drawable.setBounds(sizeRect.left.toInt(), sizeRect.top.toInt(), sizeRect.right.toInt(), sizeRect.bottom.toInt())
        }

        private class Size : LockTargetSize() {

            @Volatile
            private var sizeRect: RectF = RectF(0f, 0f, 0f, 0f)

            override val width: Int get() = await { sizeRect.width().toInt() }

            override val height: Int get() = await { sizeRect.height().toInt() }

            init {
                startWait()
            }

            override fun check() = sizeRect.width().toInt() to sizeRect.height().toInt()

            fun onMeasured(rect: RectF) {
                signal { sizeRect = rect }
            }
        }
    }
}
