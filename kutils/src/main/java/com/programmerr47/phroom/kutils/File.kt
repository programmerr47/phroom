package com.programmerr47.phroom.kutils

import java.io.File

fun File.createSafe() = safeBoolean { createNewFile() }
fun File.deleteSafe() = safeBoolean { delete() }

private inline fun safeBoolean(operation: () -> Boolean) =
    runCatching(operation).fold({ it }, { false })
