package com.senda.core.common

// spec 100 · Reloj de pruebas: solo avanza con advanceBy o set.
class FakeClock(private var nowMs: Long) : Clock {
    override fun nowMs(): Long = nowMs

    /** Avanza el reloj; [ms] debe ser ≥ 0. */
    fun advanceBy(ms: Long) {
        require(ms >= 0) { "advanceBy necesita ms >= 0, recibió $ms" }
        nowMs += ms
    }

    fun set(ms: Long) {
        nowMs = ms
    }
}
