package com.segregataur.util

import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ByteBufferPool(initialSize: Int, private val bufferCapacity: Int) {

    private val poolLock = MutableList(initialSize) { false }
    private val pool = MutableList(initialSize) { allocatePool(bufferCapacity) }

    fun get(): Pair<ByteBuffer, Int> {
        poolLock.forEachIndexed { index, inUse ->
            if (!inUse) {
                poolLock[index] = true
                return Pair(pool[index], index)
            }
        }
        debugLog("Adding another buffer to the pool")
        poolLock.add(false)
        pool.add(allocatePool(bufferCapacity))
        return Pair(pool.last(), pool.size)
    }

    fun free(index: Int) {
        pool[index].clear()
        poolLock[index] = false
    }

    private fun allocatePool(bufferCapacity: Int): ByteBuffer {
        return ByteBuffer
                .allocateDirect(bufferCapacity)
                .order(ByteOrder.nativeOrder())
    }

    private fun debugLog(message: String) {
        Log.d("BYTE_BUFFER_POOL_LOG", message)
    }
}