package com.senda.common

import com.senda.core.common.DEVICE_ID_MAX
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

// spec 100 · Criterio 5, en la JVM con FakeIdStore.
class PrefsDeviceIdentityTest {

    private class CountingGenerator(private val value: Long) : () -> Long {
        var calls = 0
            private set

        override fun invoke(): Long {
            calls++
            return value
        }
    }

    @Test
    fun `calls the generator only once`() {
        val store = FakeIdStore()
        val generator = CountingGenerator(0x1234_5678_9ABCL)
        val identity = PrefsDeviceIdentity(store, generator)

        identity.deviceId
        identity.deviceId

        assertEquals(1, generator.calls)
        assertEquals(1, store.writes)
    }

    @Test
    fun `saves 12 lowercase hex digits`() {
        val store = FakeIdStore()
        PrefsDeviceIdentity(store) { 0xABCL }.deviceId

        assertEquals("000000000abc", store.value)
    }

    @Test
    fun `a second instance on the same store returns the same id without generating`() {
        val store = FakeIdStore()
        val first = PrefsDeviceIdentity(store) { 0x1234_5678_9ABCL }.deviceId
        val generator = CountingGenerator(0x0FED_CBA9_8765L)

        val second = PrefsDeviceIdentity(store, generator).deviceId

        assertEquals(first, second)
        assertEquals(0, generator.calls)
    }

    @Test
    fun `a negative generator value stays in range`() {
        val identity = PrefsDeviceIdentity(FakeIdStore()) { -1L }

        assertEquals(DEVICE_ID_MAX, identity.deviceId)
    }

    @Test
    fun `keeps only the low 48 bits`() {
        val identity = PrefsDeviceIdentity(FakeIdStore()) { Long.MIN_VALUE or 0x0000_1234_5678_9ABCL }

        assertEquals(0x1234_5678_9ABCL, identity.deviceId)
        assertTrue(identity.deviceId in 0L..DEVICE_ID_MAX)
    }

    @Test
    fun `reads a stored value`() {
        val identity = PrefsDeviceIdentity(FakeIdStore("a1b2c3d4e5f6")) { error("no debe generar") }

        assertEquals(0xA1B2_C3D4_E5F6L, identity.deviceId)
    }

    @Test
    fun `an invalid stored value throws instead of generating a new one`() {
        val store = FakeIdStore("not-hex")
        val identity = PrefsDeviceIdentity(store) { 1L }

        assertThrows(IllegalStateException::class.java) { identity.deviceId }
        assertEquals("not-hex", store.value)
    }
}
