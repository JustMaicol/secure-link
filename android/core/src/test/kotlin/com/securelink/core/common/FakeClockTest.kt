package com.securelink.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

// spec 100 · Criterio 4.
class FakeClockTest {

    @Test
    fun `returns the time it was created with`() {
        val clock = FakeClock(1_000L)
        assertEquals(1_000L, clock.nowMs())
    }

    @Test
    fun `does not advance on its own`() {
        val clock = FakeClock(1_000L)
        clock.nowMs()
        clock.nowMs()
        assertEquals(1_000L, clock.nowMs())
    }

    @Test
    fun `advanceBy moves the time forward`() {
        val clock = FakeClock(1_000L)
        clock.advanceBy(250L)
        assertEquals(1_250L, clock.nowMs())
    }

    @Test
    fun `advanceBy zero keeps the time`() {
        val clock = FakeClock(1_000L)
        clock.advanceBy(0L)
        assertEquals(1_000L, clock.nowMs())
    }

    @Test
    fun `set replaces the time`() {
        val clock = FakeClock(1_000L)
        clock.set(500L)
        assertEquals(500L, clock.nowMs())
    }

    @Test
    fun `advanceBy negative throws and keeps the time`() {
        val clock = FakeClock(1_000L)
        assertThrows(IllegalArgumentException::class.java) { clock.advanceBy(-1L) }
        assertEquals(1_000L, clock.nowMs())
    }
}
