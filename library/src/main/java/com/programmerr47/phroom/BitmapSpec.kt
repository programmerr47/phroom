package com.programmerr47.phroom

data class BitmapSpec(
    val url: String,
    val tWidth: Int,
    val tHeight: Int
) {
    val area = tWidth * tHeight
}
