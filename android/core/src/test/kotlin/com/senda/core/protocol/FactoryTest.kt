package com.senda.core.protocol

import com.senda.core.common.DEVICE_ID_MAX
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// spec 101 · Criterio 7: una prueba por regla de la tabla de la sección 6.
class FactoryTest {

    private val deviceA = Fixtures.DEVICE_A
    private val alertKey = AlertKey(deviceA, 1)
    private val noVulnerable = Vulnerable()

    private fun alertOf(result: CreateResult): Alert =
        (okOf(result).packet as Alert)

    private fun okOf(result: CreateResult): WirePacket = when (result) {
        is CreateResult.Ok -> result.wire
        is CreateResult.Err -> throw AssertionError("la fábrica rechazó: ${result.error}")
    }

    private fun errorOf(result: CreateResult): String = when (result) {
        is CreateResult.Err -> result.error.field
        is CreateResult.Ok -> throw AssertionError("se esperaba un error y creó ${result.wire}")
    }

    private fun newAlert(
        people: Int = 3,
        counter: Int = 1,
        deviceId: Long = deviceA,
        nowMs: Long = 1_760_600_000_000L,
        config: ProtocolConfig = ProtocolConfig.DEFAULT,
    ) = PacketFactory.newAlert(
        deviceId, counter, EmergencyType.MEDICAL, noVulnerable, people,
        latE7 = -335_000_000, lonE7 = -706_500_000, nowMs = nowMs, config = config,
    )

    // --- createdAtMs = floorDiv(nowMs, 1000) * 1000 ---

    @Test
    fun `createdAt is rounded down to whole seconds`() {
        assertEquals(1_760_600_000_000L, alertOf(newAlert(nowMs = 1_760_600_000_999L)).createdAtMs)
        assertEquals(1_760_600_001_000L, alertOf(newAlert(nowMs = 1_760_600_001_000L)).createdAtMs)
        assertEquals(1_760_600_000_000L, alertOf(newAlert(nowMs = 1_760_600_000_000L)).createdAtMs)
    }

    @Test
    fun `a negative clock is rejected instead of rounded toward zero`() {
        assertEquals("createdAtS", errorOf(newAlert(nowMs = -1L)))
    }

    // --- people ---

    @Test
    fun `people above 255 is stored as 255`() {
        assertEquals(255, alertOf(newAlert(people = 256)).people)
        assertEquals(255, alertOf(newAlert(people = 100_000)).people)
        assertEquals(255, alertOf(newAlert(people = 255)).people)
    }

    @Test
    fun `people below 1 is an error`() {
        assertEquals("people", errorOf(newAlert(people = 0)))
        assertEquals("people", errorOf(newAlert(people = -5)))
        assertEquals(1, alertOf(newAlert(people = 1)).people)
    }

    // --- automatic ---

    @Test
    fun `newAlert is never automatic`() {
        assertFalse(alertOf(newAlert()).automatic)
    }

    @Test
    fun `automatic alert is severe, one person and no vulnerable`() {
        val alert = alertOf(
            PacketFactory.newAutomaticAlert(deviceA, 4, latE7 = 100, lonE7 = -200, nowMs = 1_760_600_000_500L),
        )

        assertEquals(EmergencyType.TRAPPED_OR_SEVERE, alert.emergencyType)
        assertTrue(alert.automatic)
        assertEquals(1, alert.people)
        assertEquals(noVulnerable, alert.vulnerable)
        assertEquals(AlertKey(deviceA, 4), alert.key)
        assertEquals(1_760_600_000_000L, alert.createdAtMs)
    }

    // --- ttl y relayTag ---

    @Test
    fun `new packets start with ttlMax and the emitter's relayTag`() {
        val alert = okOf(newAlert())
        assertEquals(ProtocolConfig.DEFAULT.ttlMax, alert.ttl)
        assertEquals(0xE5F6, alert.relayTag)

        val onTheWay = okOf(PacketFactory.newOnTheWay(Fixtures.DEVICE_C, alertKey, 1_760_600_120_000L))
        assertEquals(ProtocolConfig.DEFAULT.ttlMax, onTheWay.ttl)
        assertEquals(0x0C0C, onTheWay.relayTag) // el del voluntario, no el de la alerta

        val status = okOf(PacketFactory.newStatus(deviceA, alertKey, StatusCode.SAFE, 1_760_600_600_000L))
        assertEquals(ProtocolConfig.DEFAULT.ttlMax, status.ttl)
        assertEquals(0xE5F6, status.relayTag)
    }

    @Test
    fun `ttl follows the config`() {
        val relaxed = ProtocolConfig(ttlMax = 5)

        assertEquals(5, okOf(newAlert(config = relaxed)).ttl)
    }

    // --- counter ---

    @Test
    fun `counter outside 1 to 65535 is an error`() {
        assertEquals("counter", errorOf(newAlert(counter = 0)))
        assertEquals("counter", errorOf(newAlert(counter = 65_536)))
        assertEquals("counter", errorOf(newAlert(counter = -1)))
        assertEquals(65_535, alertOf(newAlert(counter = 65_535)).counter)
    }

    // --- on the way ---

    @Test
    fun `on the way points to the alert and records the volunteer`() {
        val packet = okOf(PacketFactory.newOnTheWay(Fixtures.DEVICE_C, alertKey, 1_760_600_120_800L)).packet as OnTheWay

        assertEquals(alertKey, packet.alert)
        assertEquals(Fixtures.DEVICE_C, packet.volunteerDeviceId)
        assertEquals(1_760_600_120_000L, packet.createdAtMs)
        assertEquals("a1b2c3d4e5f6-0001/0c0c0c0c0c0c", packet.key.canonical())
    }

    // --- status ---

    @Test
    fun `status can only come from the alert's origin`() {
        assertEquals(
            "alertDeviceId",
            errorOf(PacketFactory.newStatus(Fixtures.DEVICE_C, alertKey, StatusCode.CANCELLED, 1_760_600_600_000L)),
        )

        val packet = okOf(PacketFactory.newStatus(deviceA, alertKey, StatusCode.CANCELLED, 1_760_600_600_000L)).packet as Status
        assertEquals(StatusCode.CANCELLED, packet.status)
        assertEquals("a1b2c3d4e5f6-0001#1", packet.key.canonical())
    }

    // --- Mismas validaciones que encode ---

    @Test
    fun `the factory validates like encode`() {
        assertEquals("deviceId", errorOf(newAlert(deviceId = DEVICE_ID_MAX + 1)))
        assertEquals("deviceId", errorOf(newAlert(deviceId = -1)))
        assertEquals("latE7", errorOf(PacketFactory.newAutomaticAlert(deviceA, 1, 900_000_001, 0, 1_760_600_000_000L)))
        assertEquals("lonE7", errorOf(PacketFactory.newAutomaticAlert(deviceA, 1, 0, -1_800_000_001, 1_760_600_000_000L)))
        assertEquals("alertCounter", errorOf(PacketFactory.newOnTheWay(Fixtures.DEVICE_C, AlertKey(deviceA, 0), 1_760_600_000_000L)))
        assertEquals("volunteerDeviceId", errorOf(PacketFactory.newOnTheWay(-1, alertKey, 1_760_600_000_000L)))
        assertEquals("createdAtS", errorOf(PacketFactory.newOnTheWay(Fixtures.DEVICE_C, alertKey, 4_294_967_296_000L)))
    }

    @Test
    fun `every created packet encodes and decodes back to itself`() {
        val created = listOf(
            okOf(newAlert()),
            okOf(PacketFactory.newAutomaticAlert(deviceA, 2, 10, 20, 1_760_600_000_000L)),
            okOf(PacketFactory.newOnTheWay(Fixtures.DEVICE_C, alertKey, 1_760_600_120_000L)),
            okOf(PacketFactory.newStatus(deviceA, alertKey, StatusCode.SAFE, 1_760_600_600_000L)),
        )

        for (wire in created) {
            val payload = (PacketCodec.encode(wire) as EncodeResult.Ok).payload
            assertEquals(wire, (PacketCodec.decode(payload) as DecodeResult.Ok).wire)
        }
    }

    @Test
    fun `newAlert reproduces the reference alert of the 001`() {
        val wire = okOf(
            PacketFactory.newAlert(
                deviceId = deviceA, counter = 1, type = EmergencyType.TRAPPED_OR_SEVERE,
                vulnerable = Vulnerable(children = true, injured = true), people = 3,
                latE7 = -335_000_000, lonE7 = -706_500_000, nowMs = 1_760_600_000_000L,
            ),
        )

        assertEquals(Fixtures.alertWire(), wire)
    }
}
