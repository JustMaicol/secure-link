package com.senda.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

// spec 100 · FixedDeviceIdentity solo acepta 0..DEVICE_ID_MAX (48 bits, spec 001).
class FixedDeviceIdentityTest {

    @Test
    fun `DEVICE_ID_MAX is 48 bits`() {
        assertEquals((1L shl 48) - 1, DEVICE_ID_MAX)
    }

    @Test
    fun `accepts the range limits`() {
        assertEquals(0L, FixedDeviceIdentity(0L).deviceId)
        assertEquals(DEVICE_ID_MAX, FixedDeviceIdentity(DEVICE_ID_MAX).deviceId)
    }

    @Test
    fun `rejects a negative value`() {
        assertThrows(IllegalArgumentException::class.java) { FixedDeviceIdentity(-1L) }
    }

    @Test
    fun `rejects a value above 48 bits`() {
        assertThrows(IllegalArgumentException::class.java) { FixedDeviceIdentity(DEVICE_ID_MAX + 1) }
    }
}
