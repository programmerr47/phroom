package com.programmerr47.phroom.collage

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.nextDown
import kotlin.random.Random

class SlashCollageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val rnd = Random(System.currentTimeMillis())
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var collage: List<RectF> = emptyList()
    private var colors: List<Int> = emptyList()
    private var urls: List<String> = emptyList()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthNotUnspecified = MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED
        val heightNotUnspecified = MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.UNSPECIFIED

        if (widthNotUnspecified && heightNotUnspecified && collage.isEmpty()) {
            collage = generateCollage(
                MeasureSpec.getSize(widthMeasureSpec),
                MeasureSpec.getSize(heightMeasureSpec)
            )

            colors = collage.map { rnd.nextInt() }
        }

        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onDraw(canvas: Canvas) {
        collage.forEachIndexed { i, rect ->
            paint.color = colors[i]
            canvas.drawRect(rect, paint)
        }
    }

    fun generateAgain(urls: List<String>) {
        this.urls = urls
        collage = emptyList()
        colors = emptyList()
        requestLayout()
        invalidate()
    }

    private fun generateCollage(width: Int, height: Int): List<RectF> = Generator.generate(
        urls.size, Rect(paddingStart, paddingTop, width - paddingEnd, height - paddingEnd)
    )

    private object Generator {
        private val rnd = Random(System.currentTimeMillis())
        private val queue = ArrayList<RectF>(20)
        private val weights = ArrayList<Int>(20)
        private val splitPoint = PointF()

        fun generate(n: Int, initial: Rect) = generate(n, RectF(initial))

        fun generate(n: Int, initial: RectF): List<RectF> {
            queue.clear()
            weights.clear()

            queue.ensureCapacity(n)
            weights.ensureCapacity(n)

            queue.add(initial)
            weights.add(1)

            var sum = 1
            while (queue.size < n) {
                val choice = rnd.nextInt(sum)
                val index = findIndex(choice, weights)

                val rectToSplit = queue.removeAt(index)
                weights.removeAt(index)
                findSplitPoint(rectToSplit, splitPoint)

                val newRect = if (splitPoint.x == 0f) { //means we will cut horizontally
                    cutHorizontally(rectToSplit, splitPoint.y)
                } else { //means we will cut vertically
                    cutVertically(rectToSplit, splitPoint.x)
                }

                weights.indices.forEach { i ->
                    weights[i] += 1
                }

                queue.add(rectToSplit)
                queue.add(newRect)
                weights.add(1)
                weights.add(1)

                sum += weights.size
            }

            return queue
        }

        private fun findIndex(choice: Int, weights: List<Int>): Int {
            var controlSum = 0
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
            val choice = rnd.nextFloat(0f, until)

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

        //Copied from Random.nextDouble(from: Float, until: Float) method
        //and adapted to floats, because there is no appropriate method :(
        private fun Random.nextFloat(from: Float, until: Float): Float {
            require(until > from)
            val size = until - from
            val r = if (size.isInfinite() && from.isFinite() && until.isFinite()) {
                val r1 = nextFloat() * (until / 2 - from / 2)
                from + r1 + r1
            } else {
                from + nextFloat() * size
            }
            return if (r >= until) until.nextDown() else r
        }
    }
}
