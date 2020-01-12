package com.programmerr47.phroom.caching

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.collection.LruCache
import com.programmerr47.phroom.atLeastKitkat
import com.programmerr47.phroom.kutils.createSafe
import com.programmerr47.phroom.kutils.deleteSafe
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

internal class DiskCache(
    private val cacheDir: File,
    private val maxBytesCount: Int
) : BitmapCache {
    private val _lock = ReentrantReadWriteLock()

    private val records = object : LruCache<String, Record>(maxBytesCount) {
        override fun sizeOf(key: String, value: Record) = value.size

        override fun entryRemoved(evicted: Boolean, key: String, oldValue: Record, newValue: Record?) {
            tryToRemove(key)
        }
    }

    init {
        require(cacheDir.isDirectory)
        parseJournal()
    }

    override fun get(key: String): Bitmap? = _lock.read {
        val key = key.md5()
        val cached = getFile(key)
        cached?.let { BitmapFactory.decodeStream(FileInputStream(it)) }
    }

    override fun put(key: String, bitmap: Bitmap): Unit = _lock.write {
        val key = key.md5()
        tryToRemove(key)

        val bitmapFile = File(cacheDir, key)
        if (bitmapFile.createSafe()) {
            runCatching { bitmapFile.writePng(bitmap) }
                .onSuccess { records.put(key, Record(key, with(bitmap) { if (atLeastKitkat()) allocationByteCount else byteCount })) }
                .onFailure { tryToRemove(key) }
        }

        saveJournal()
    }

    private fun tryToRemove(key: String) {
        getFile(key)?.deleteSafe()
        records.remove(key)
    }

    private fun getFile(key: String): File? {
        val possible = File(cacheDir, key)
        return possible.takeIf { it.exists() } ?: getCachedFiles().find { it.name == key }
    }

    private fun getCachedFiles() = cacheDir.listFiles() ?: emptyArray()

    private fun parseJournal() {
        getJournalFile().reader().buffered().useLines {
            it.forEach {
                val record = parseLine(it)
                records.put(record.name, record)
            }
        }
    }

    private fun parseLine(line: String): Record {
        val parts = line.split(" ")
        return Record(parts[0], parts[1].toInt())
    }

    //TODO Now we save journal on every single update, which looks like time spending. Need to optimize that
    private fun saveJournal() {
        getJournalFile().bufferedWriter().use {
            records.snapshot().forEach { (_, record) ->
                it.write(record.toFileStr())
                it.newLine()
            }
        }
    }

    private fun getJournalFile() = File(cacheDir, JOUNRAL_FILE).apply {
        if (!exists()) {
            createNewFile()
        }
    }

    private fun String.md5() =
        MessageDigest.getInstance("MD5").digest(toByteArray()).toHex()

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }

    private fun File.writePng(bitmap: Bitmap) =
        outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 0, it) }

    private class Record(
        val name: String,
        val size: Int //in bytes
    ) {
        fun toFileStr() = "$name $size"
    }

    companion object {
        private const val JOUNRAL_FILE = ".journal"
    }
}
