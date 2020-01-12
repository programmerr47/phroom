package com.programmerr47.phroom.kutils

import kotlin.math.nextDown
import kotlin.random.Random

fun Random.nextFloat(until: Float) = nextFloat(0f, until)

//Copied from Random.nextDouble(from: Float, until: Float) method
//and adapted to floats, because there is no appropriate method :(
fun Random.nextFloat(from: Float, until: Float): Float {
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
