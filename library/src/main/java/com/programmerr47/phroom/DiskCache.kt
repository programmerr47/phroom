package com.programmerr47.phroom

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

internal class DiskCache(
    private val cacheDir: File
) : BitmapCache {
    private val _lock = ReentrantReadWriteLock()

    init {
        require(cacheDir.isDirectory)
    }

    override fun get(key: String): Bitmap? = _lock.read {
        val cached = getFile(key)
        cached?.let { BitmapFactory.decodeStream(FileInputStream(it)) }
    }

    override fun put(key: String, bitmap: Bitmap): Unit = _lock.write {
        tryToRemove(key)

        val bitmapFile = File(cacheDir, key.md5())
        if (bitmapFile.createSafe()) {
            runCatching { bitmapFile.writePng(bitmap) }
                .onFailure { tryToRemove(key) }
        }
    }

    private fun tryToRemove(key: String) {
        getFile(key)?.deleteSafe()
    }

    private fun getFile(key: String): File? {
        val desiredName = key.md5()
        return getCachedFiles().find { it.name == desiredName }
    }

    private fun getCachedFiles() = cacheDir.listFiles() ?: emptyArray()

    private fun String.md5() =
        MessageDigest.getInstance("MD5").digest(toByteArray()).toHex()

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }

    private fun File.createSafe() = safeBoolean { createNewFile() }
    private fun File.deleteSafe() = safeBoolean { delete() }

    private fun File.writePng(bitmap: Bitmap) =
        outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 0, it) }

    private inline fun safeBoolean(operation: () -> Boolean) =
        runCatching(operation).fold({ it }, { false })
}
