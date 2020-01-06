package com.programmerr47.phroom

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.Log

private const val SCREEN_COUNT_FOR_CACHE = 3
private const val BYTES_PER_PIXEL = 4 //In ARGB_8888 there are 4 bytes per pixel

internal fun logInternal(msg: String) = Log.d("Phroom", msg)

internal fun atLeastKitkat() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

/**
 * We assume that for memory cache we need to take [SCREEN_COUNT_FOR_CACHE] amount
 * of screens. Each screen contains width * height pixels. Then we convert that to
 * bytes by multiply it to number of bytes per pixel. Well, this is a tricky part,
 * since the number of bytes can be different for different configurations. So
 * we should rely on that and maybe make it more flexible, but for now we will
 * use [Bitmap.Config.ARGB_8888] as a pivot point, since this value is default
 * for Bitmap decoding according to Android Documentations.
 *
 * @see [Bitmap.Config]
 * @see 'https://developer.android.com/reference/android/graphics/BitmapFactory.Options.html#inPreferredConfig'
 */
internal fun calculateMemoryCacheSize(context: Context): Int {
    Bitmap.Config.ARGB_8888
    val dm = context.applicationContext.resources.displayMetrics
    return SCREEN_COUNT_FOR_CACHE * dm.widthPixels * dm.heightPixels * BYTES_PER_PIXEL
}
