package com.senda.core.protocol

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// spec 101 · Criterios 1 y 4: los tres ejemplos de la 001, sección 10, escritos en el código.
class PacketCodecTest {

    private fun encoded(wire: WirePacket): ByteArray = when (val result = PacketCodec.encode(wire)) {
        is EncodeResult.Ok -> result.payload
        is EncodeResult.Err -> throw AssertionError("encode rechazó $wire: ${result.error}")
    }

    private fun decoded(hex: String): WirePacket = when (val result = PacketCodec.decode(Hex.decode(hex))) {
        is DecodeResult.Ok -> result.wire
        is DecodeResult.Err -> throw AssertionError("decode falló: ${result.error}")
    }

    // --- ALERT ---

    @Test
    fun `alert encodes to the exact hex`() {
        assertEquals(Fixtures.ALERT_HEX, Hex.encode(encoded(Fixtures.alertWire())))
    }

    @Test
    fun `alert decodes to the same fields and the same key`() {
        val wire = decoded(Fixtures.ALERT_HEX)

        assertEquals(Fixtures.alertWire(), wire)
        assertEquals("a1b2c3d4e5f6-0001", wire.packet.key.canonical())
        assertEquals(8, wire.ttl)
        assertEquals(0xE5F6, wire.relayTag)
    }

    // --- ON_THE_WAY ---

    @Test
    fun `on the way encodes to the exact hex`() {
        assertEquals(Fixtures.ON_THE_WAY_HEX, Hex.encode(encoded(Fixtures.onTheWayWire())))
    }

    @Test
    fun `on the way decodes to the same fields and the same key`() {
        val wire = decoded(Fixtures.ON_THE_WAY_HEX)

        assertEquals(Fixtures.onTheWayWire(), wire)
        assertEquals("a1b2c3d4e5f6-0001/0c0c0c0c0c0c", wire.packet.key.canonical())
    }

    // --- STATUS ---

    @Test
    fun `status encodes to the exact hex`() {
        assertEquals(Fixtures.STATUS_HEX, Hex.encode(encoded(Fixtures.statusWire())))
    }

    @Test
    fun `status decodes to the same fields and the same key`() {
        val wire = decoded(Fixtures.STATUS_HEX)

        assertEquals(Fixtures.statusWire(), wire)
        assertEquals("a1b2c3d4e5f6-0001#2", wire.packet.key.canonical())
    }

    // --- Tamaños ---

    @Test
    fun `payload sizes are 27, 23 and 18 bytes`() {
        assertEquals(27, encoded(Fixtures.alertWire()).size)
        assertEquals(23, encoded(Fixtures.onTheWayWire()).size)
        assertEquals(18, encoded(Fixtures.statusWire()).size)
    }

    // --- Criterio 4: el bit reservado se ignora y se codifica en 0 ---

    @Test
    fun `reserved bit set to 1 decodes without error and encodes back as 0`() {
        val withReservedBit = Hex.decode(Fixtures.ALERT_HEX).also { it[10] = (it[10].toInt() or 0x80).toByte() }

        val wire = (PacketCodec.decode(withReservedBit) as DecodeResult.Ok).wire

        assertEquals(Fixtures.alertWire(), wire)
        assertArrayEquals(Hex.decode(Fixtures.ALERT_HEX), encoded(wire))
        assertTrue(encoded(wire)[10].toInt() and 0x80 == 0)
    }

    // --- Bits del byte info ---

    @Test
    fun `info byte carries type, automatic and the four vulnerable bits`() {
        val all = Fixtures.alert().copy(
            emergencyType = EmergencyType.MEDICAL,
            automatic = true,
            vulnerable = Vulnerable(children = true, elderly = true, injured = true, reducedMobility = true),
        )

        // tipo 1 | automática (bit 2) | niños, mayores, heridos y movilidad (bits 3-6)
        assertEquals(0b0111_1101, encoded(WirePacket(all, 8, 0))[10].toInt())
    }

    // --- Orden de errores de decode (001, sección 5) ---

    @Test
    fun `not our packet wins over everything else`() {
        assertEquals(DecodeError.NotOurPacket, errorOf(ByteArray(0)))
        assertEquals(DecodeError.NotOurPacket, errorOf(byteArrayOf(0xA7.toByte())))
        assertEquals(DecodeError.NotOurPacket, errorOf(Hex.decode("a811")))
    }

    @Test
    fun `version is checked before type and length`() {
        assertEquals(DecodeError.UnsupportedVersion(2), errorOf(Hex.decode("a721")))
        assertEquals(DecodeError.UnsupportedVersion(0), errorOf(Hex.decode("a701")))
    }

    @Test
    fun `type is checked before length`() {
        assertEquals(DecodeError.UnknownType(0), errorOf(Hex.decode("a710")))
        assertEquals(DecodeError.UnknownType(4), errorOf(Hex.decode("a714")))
        assertEquals(DecodeError.UnknownType(15), errorOf(Hex.decode("a71f")))
    }

    @Test
    fun `length is checked before the fields`() {
        assertEquals(DecodeError.InvalidLength(27, 26), errorOf(Hex.decode(Fixtures.ALERT_HEX.dropLast(2))))
        assertEquals(DecodeError.InvalidLength(27, 28), errorOf(Hex.decode(Fixtures.ALERT_HEX + "00")))
        assertEquals(DecodeError.InvalidLength(23, 2), errorOf(Hex.decode("a712")))
        assertEquals(DecodeError.InvalidLength(18, 17), errorOf(Hex.decode(Fixtures.STATUS_HEX.dropLast(2))))
    }

    @Test
    fun `invalid fields report the first one by offset`() {
        // counter (offset 8) y people (offset 11) inválidos a la vez: gana counter.
        val bytes = Hex.decode(Fixtures.ALERT_HEX).also {
            it[8] = 0
            it[9] = 0
            it[11] = 0
        }
        assertEquals(DecodeError.InvalidField("counter"), errorOf(bytes))
    }

    @Test
    fun `invalid field names for on the way and status use the alert prefix`() {
        val onTheWay = Hex.decode(Fixtures.ON_THE_WAY_HEX).also { it[8] = 0; it[9] = 0 }
        assertEquals(DecodeError.InvalidField("alertCounter"), errorOf(onTheWay))

        val status = Hex.decode(Fixtures.STATUS_HEX).also { it[10] = 3 }
        assertEquals(DecodeError.InvalidField("status"), errorOf(status))

        val ttlZero = Hex.decode(Fixtures.STATUS_HEX).also { it[15] = 0 }
        assertEquals(DecodeError.InvalidField("ttl"), errorOf(ttlZero))
    }

    @Test
    fun `ttl above ttlMax is rejected and follows the config`() {
        val nine = Hex.decode(Fixtures.ALERT_HEX).also { it[24] = 9 }
        assertEquals(DecodeError.InvalidField("ttl"), errorOf(nine))

        val relaxed = ProtocolConfig(ttlMax = 9)
        assertTrue(PacketCodec.decode(nine, relaxed) is DecodeResult.Ok)
    }

    private fun errorOf(bytes: ByteArray): DecodeError = when (val result = PacketCodec.decode(bytes)) {
        is DecodeResult.Err -> result.error
        is DecodeResult.Ok -> throw AssertionError("se esperaba un error y decodificó ${result.wire}")
    }
}
