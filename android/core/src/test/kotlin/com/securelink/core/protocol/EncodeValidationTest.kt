package com.securelink.core.protocol

import com.securelink.core.common.DEVICE_ID_MAX
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// spec 101, sección 5 · Cada rechazo de encode, y el primer campo inválido por offset.
class EncodeValidationTest {

    private val alert = Fixtures.alert()
    private val onTheWay = Fixtures.onTheWayWire().packet as OnTheWay
    private val status = Fixtures.statusWire().packet as Status

    private fun wire(packet: Packet, ttl: Int = 8, relayTag: Int = 0) = WirePacket(packet, ttl, relayTag)

    private fun rejectedField(wire: WirePacket): String? = when (val result = PacketCodec.encode(wire)) {
        is EncodeResult.Err -> result.error.field
        is EncodeResult.Ok -> null
    }

    // --- ALERT ---

    @Test
    fun `alert rejects each field out of range`() {
        assertEquals("deviceId", rejectedField(wire(alert.copy(deviceId = -1))))
        assertEquals("deviceId", rejectedField(wire(alert.copy(deviceId = DEVICE_ID_MAX + 1))))
        assertEquals("counter", rejectedField(wire(alert.copy(counter = 0))))
        assertEquals("counter", rejectedField(wire(alert.copy(counter = 65_536))))
        assertEquals("people", rejectedField(wire(alert.copy(people = 0))))
        assertEquals("people", rejectedField(wire(alert.copy(people = 256))))
        assertEquals("latE7", rejectedField(wire(alert.copy(latE7 = -900_000_001))))
        assertEquals("latE7", rejectedField(wire(alert.copy(latE7 = 900_000_001))))
        assertEquals("lonE7", rejectedField(wire(alert.copy(lonE7 = -1_800_000_001))))
        assertEquals("lonE7", rejectedField(wire(alert.copy(lonE7 = 1_800_000_001))))
        assertEquals("createdAtS", rejectedField(wire(alert.copy(createdAtMs = -1000))))
        assertEquals("createdAtS", rejectedField(wire(alert.copy(createdAtMs = 1_760_600_000_001L))))
        assertEquals("createdAtS", rejectedField(wire(alert.copy(createdAtMs = 4_294_967_296_000L))))
        assertEquals("ttl", rejectedField(wire(alert, ttl = 0)))
        assertEquals("ttl", rejectedField(wire(alert, ttl = 9)))
        assertEquals("relayTag", rejectedField(wire(alert, relayTag = -1)))
        assertEquals("relayTag", rejectedField(wire(alert, relayTag = 65_536)))
    }

    @Test
    fun `alert accepts every limit`() {
        assertEquals(null, rejectedField(wire(alert.copy(deviceId = 0, counter = 1, people = 1))))
        assertEquals(
            null,
            rejectedField(
                wire(
                    alert.copy(
                        deviceId = DEVICE_ID_MAX, counter = 65_535, people = 255,
                        latE7 = 900_000_000, lonE7 = -1_800_000_000, createdAtMs = 4_294_967_295_000L,
                    ),
                    ttl = 1,
                    relayTag = 65_535,
                ),
            ),
        )
    }

    @Test
    fun `alert reports the first invalid field by offset`() {
        val everythingWrong = alert.copy(
            deviceId = -1, counter = 0, people = 0, latE7 = 1_000_000_000,
            lonE7 = 1_900_000_000, createdAtMs = 1,
        )

        assertEquals("deviceId", rejectedField(wire(everythingWrong, ttl = 0, relayTag = -1)))
        assertEquals("counter", rejectedField(wire(everythingWrong.copy(deviceId = 1), ttl = 0, relayTag = -1)))
        assertEquals("people", rejectedField(wire(everythingWrong.copy(deviceId = 1, counter = 1), ttl = 0, relayTag = -1)))
        assertEquals(
            "latE7",
            rejectedField(wire(everythingWrong.copy(deviceId = 1, counter = 1, people = 1), ttl = 0, relayTag = -1)),
        )
        assertEquals(
            "lonE7",
            rejectedField(wire(everythingWrong.copy(deviceId = 1, counter = 1, people = 1, latE7 = 0), ttl = 0, relayTag = -1)),
        )
        assertEquals(
            "createdAtS",
            rejectedField(wire(everythingWrong.copy(deviceId = 1, counter = 1, people = 1, latE7 = 0, lonE7 = 0), ttl = 0, relayTag = -1)),
        )
        assertEquals("ttl", rejectedField(wire(alert, ttl = 0, relayTag = -1)))
    }

    // --- ON_THE_WAY ---

    @Test
    fun `on the way rejects each field out of range`() {
        assertEquals("alertDeviceId", rejectedField(wire(onTheWay.copy(alert = AlertKey(-1, 1)))))
        assertEquals("alertDeviceId", rejectedField(wire(onTheWay.copy(alert = AlertKey(DEVICE_ID_MAX + 1, 1)))))
        assertEquals("alertCounter", rejectedField(wire(onTheWay.copy(alert = AlertKey(1, 0)))))
        assertEquals("alertCounter", rejectedField(wire(onTheWay.copy(alert = AlertKey(1, 65_536)))))
        assertEquals("volunteerDeviceId", rejectedField(wire(onTheWay.copy(volunteerDeviceId = -1))))
        assertEquals("volunteerDeviceId", rejectedField(wire(onTheWay.copy(volunteerDeviceId = DEVICE_ID_MAX + 1))))
        assertEquals("createdAtS", rejectedField(wire(onTheWay.copy(createdAtMs = 500))))
        assertEquals("ttl", rejectedField(wire(onTheWay, ttl = 9)))
        assertEquals("relayTag", rejectedField(wire(onTheWay, relayTag = 65_536)))
    }

    @Test
    fun `on the way reports the first invalid field by offset`() {
        val wrong = onTheWay.copy(alert = AlertKey(1, 0), volunteerDeviceId = -1, createdAtMs = 1)

        assertEquals("alertCounter", rejectedField(wire(wrong, ttl = 0)))
        assertEquals("volunteerDeviceId", rejectedField(wire(wrong.copy(alert = AlertKey(1, 1)), ttl = 0)))
        assertEquals("createdAtS", rejectedField(wire(wrong.copy(alert = AlertKey(1, 1), volunteerDeviceId = 1), ttl = 0)))
    }

    // --- STATUS ---

    @Test
    fun `status rejects each field out of range`() {
        assertEquals("alertDeviceId", rejectedField(wire(status.copy(alert = AlertKey(-1, 1)))))
        assertEquals("alertCounter", rejectedField(wire(status.copy(alert = AlertKey(1, 0)))))
        assertEquals("createdAtS", rejectedField(wire(status.copy(createdAtMs = -1000))))
        assertEquals("ttl", rejectedField(wire(status, ttl = 0)))
        assertEquals("relayTag", rejectedField(wire(status, relayTag = -1)))
    }

    // --- Configuración ---

    @Test
    fun `ttl limit follows the config`() {
        assertEquals("ttl", rejectedField(wire(alert, ttl = 9)))
        assertTrue(PacketCodec.encode(wire(alert, ttl = 9), ProtocolConfig(ttlMax = 9)) is EncodeResult.Ok)
    }

    @Test
    fun `a rejected packet is never truncated into an Ok`() {
        val result = PacketCodec.encode(wire(alert.copy(people = 300)))

        assertTrue(result is EncodeResult.Err)
    }
}
