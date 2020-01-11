package com.programmerr47.phroom.targets

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

abstract class LockTargetSize : Target.Size {
    private val _lock = ReentrantLock()
    private val _cond = _lock.newCondition()
    private var awaitForSize = false //wanted to do AtomicBoolean, but realised that we use it only inside lock section, so it is not needed

    protected fun <T> await(block: () -> T): T = _lock.withLock {
        if (awaitForSize) _cond.await()
        block()
    }

    protected fun signal(block: () -> Unit): Unit = _lock.withLock {
        block()

        if (awaitForSize) {
            awaitForSize = false
            _cond.signalAll()
        }
    }

    protected fun startWait(): Unit = _lock.withLock { awaitForSize = true }
}
