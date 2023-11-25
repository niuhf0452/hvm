package com.github.niuhf0452.hvm

import java.util.concurrent.atomic.AtomicLong

interface IdGen {
    fun nextId(): Long

    companion object {
        private const val TIME_EPOCH = 1700385319003L
        private const val TIME_BITS = 41
        private const val SEQ_BITS = 12
        private const val WORKER_BITS = 10
        private const val TIME_SHIFT = SEQ_BITS + WORKER_BITS
        private const val SEQ_UNIT = 1L shl WORKER_BITS
        private const val TIME_MASK = ((1L shl TIME_BITS) - 1) shl TIME_SHIFT

        fun create(now: () -> Long, workerId: Int): IdGen {
            return Gen(now, workerId)
        }
    }

    private class Gen(
        private val now: () -> Long,
        private val workerId: Int = 0
    ) : IdGen {
        private val lastId = AtomicLong(0)

        override fun nextId(): Long {
            val minId = ((now() - TIME_EPOCH) shl TIME_SHIFT) or workerId.toLong()
            while (true) {
                val last = lastId.get()
                val id = if (minId > last) {
                    minId
                } else {
                    val value = last + SEQ_UNIT
                    if (value and TIME_MASK != last and TIME_MASK) {
                        throw IllegalStateException("Id generating overflow")
                    }
                    value
                }
                if (lastId.compareAndSet(last, id)) {
                    return id
                }
            }
        }
    }
}