package com.securelink.core.common

// spec 100 · El tiempo se inyecta (regla 6). La única implementación real es SystemClock, en :app.
fun interface Clock {
    /** Epoch en ms, UTC (regla 11). */
    fun nowMs(): Long
}
