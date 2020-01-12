package com.programmerr47.phroom.kutils

fun IntArray.sumEach(other: IntArray) {
    require(size == other.size)
    for (i in 0 until size) {
        this[i] += other[i]
    }
}

fun IntArray.divEach(count: Int) {
    for (i in 0 until size) {
        this[i] /= count
    }
}
