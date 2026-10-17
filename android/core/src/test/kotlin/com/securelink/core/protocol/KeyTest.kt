package com.securelink.core.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// spec 101 · Criterio 5 y formas canónicas de la spec 001, sección 4.
class KeyTest {

    private val deviceA = 0xA1B2_C3D4_E5F6L
    private val deviceC = 0x0C0C_0C0C_0C0CL

    private val alert = Alert(
        deviceId = deviceA,
        counter = 1,
        emergencyType = EmergencyType.TRAPPED_OR_SEVERE,
        automatic = false,
        vulnerable = Vulnerable(children = true, injured = true),
        people = 3,
        latE7 = -335_000_000,
        lonE7 = -706_500_000,
        createdAtMs = 1_760_600_000_000L,
    )

    // --- Formas canónicas exactas ---

    @Test
    fun `alert key canonical form`() {
        assertEquals("a1b2c3d4e5f6-0001", alert.key.canonical())
    }

    @Test
    fun `on the way key canonical form`() {
        val onTheWay = OnTheWay(alert.key, deviceC, 1_760_600_120_000L)

        assertEquals("a1b2c3d4e5f6-0001/0c0c0c0c0c0c", onTheWay.key.canonical())
    }

    @Test
    fun `status key canonical form`() {
        val status = Status(alert.key, StatusCode.SAFE, 1_760_600_600_000L)

        assertEquals("a1b2c3d4e5f6-0001#2", status.key.canonical())
        assertEquals("a1b2c3d4e5f6-0001#1", Status(alert.key, StatusCode.CANCELLED, 0L).key.canonical())
    }

    @Test
    fun `canonical form keeps leading zeros`() {
        val key = AlertKey(deviceId = 1L, counter = 1)

        assertEquals("000000000001-0001", key.canonical())
        assertEquals("000000000001-0001/000000000001", OnTheWayKey(key, 1L).canonical())
    }

    @Test
    fun `canonical form is lowercase hex and counter uses four digits`() {
        assertEquals("ffffffffffff-ffff", AlertKey(0xFFFF_FFFF_FFFFL, 65_535).canonical())
        assertEquals("000000000000-0abc", AlertKey(0L, 0xABC).canonical())
    }

    // --- Criterio 5: ttl y relayTag no cambian la clave; un campo inmutable sí ---

    @Test
    fun `changing ttl or relayTag does not change the key`() {
        val original = WirePacket(alert, ttl = 8, relayTag = relayTagOf(deviceA))
        val relayed = original.copy(ttl = 3, relayTag = relayTagOf(deviceC))

        assertEquals(original.packet.key, relayed.packet.key)
        assertEquals(original.packet.key.canonical(), relayed.packet.key.canonical())
    }

    @Test
    fun `changing a field outside the key does not change it either`() {
        assertEquals(alert.key, alert.copy(people = 9, createdAtMs = 0L, automatic = true).key)
    }

    @Test
    fun `changing an immutable field changes the key`() {
        assertNotEquals(alert.key, alert.copy(deviceId = deviceC).key)
        assertNotEquals(alert.key, alert.copy(counter = 2).key)

        val onTheWay = OnTheWay(alert.key, deviceC, 0L)
        assertNotEquals(onTheWay.key, onTheWay.copy(volunteerDeviceId = deviceA).key)
        assertNotEquals(onTheWay.key, onTheWay.copy(alert = AlertKey(deviceA, 2)).key)

        val status = Status(alert.key, StatusCode.SAFE, 0L)
        assertNotEquals(status.key, status.copy(status = StatusCode.CANCELLED).key)
        assertNotEquals(status.key, status.copy(alert = AlertKey(deviceC, 1)).key)
    }

    @Test
    fun `keys of different kinds never collide`() {
        val keys = listOf(alert.key, OnTheWayKey(alert.key, deviceC), StatusKey(alert.key, StatusCode.SAFE))

        assertEquals(keys.size, keys.map { it.canonical() }.toSet().size)
    }

    // --- relayTag ---

    @Test
    fun `relayTag is the low 16 bits of the deviceId`() {
        assertEquals(0xE5F6, relayTagOf(deviceA))
        assertEquals(0x0B0B, relayTagOf(0x0B0B_0B0B_0B0BL))
        assertEquals(0x0C0C, relayTagOf(deviceC))
        assertEquals(0xFFFF, relayTagOf(0xFFFF_FFFF_FFFFL))
    }

    // --- Orden por texto (regla 9) ---

    @Test
    fun `keys are ordered by their canonical text`() {
        val keys = listOf(
            StatusKey(AlertKey(deviceA, 1), StatusCode.SAFE),
            OnTheWayKey(AlertKey(deviceA, 1), deviceC),
            AlertKey(deviceA, 1),
            AlertKey(deviceC, 2),
            AlertKey(deviceC, 1),
        )

        // Orden ASCII: los dígitos van antes que las letras y '#' (0x23) antes que '/' (0x2F).
        assertEquals(
            listOf(
                "0c0c0c0c0c0c-0001",
                "0c0c0c0c0c0c-0002",
                "a1b2c3d4e5f6-0001",
                "a1b2c3d4e5f6-0001#2",
                "a1b2c3d4e5f6-0001/0c0c0c0c0c0c",
            ),
            keys.sorted().map { it.canonical() },
        )
    }

    @Test
    fun `equal keys compare as zero and the order is antisymmetric`() {
        val a = AlertKey(deviceA, 1)
        val b = AlertKey(deviceA, 2)

        assertEquals(0, a.compareTo(AlertKey(deviceA, 1)))
        assertTrue(a.compareTo(b) < 0)
        assertTrue(b.compareTo(a) > 0)
    }
}
