package com.securelink.core.protocol

import com.securelink.core.common.DEVICE_ID_MAX
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// spec 101 · Criterios 3 y 6: ida y vuelta con 1000 paquetes y entradas aleatorias que nunca lanzan.
class RoundTripTest {

    /** Generador de paquetes válidos con Mulberry32: el mismo resultado en cualquier máquina. */
    private class Generator(seed: Int) {
        private val rng = Mulberry32(seed)

        fun below(n: Long): Long = rng.next() % n
        fun between(from: Long, to: Long): Long = from + below(to - from + 1)
        fun flag(): Boolean = below(2) == 1L
        fun deviceId(): Long = ((rng.next() shl 16) xor rng.next()) and DEVICE_ID_MAX

        fun alertKey() = AlertKey(deviceId(), between(1, 65_535).toInt())
        fun createdAtMs(): Long = below(4_294_967_296L) * 1000

        fun wire(): WirePacket {
            val packet: Packet = when (below(3)) {
                0L -> Alert(
                    deviceId = deviceId(),
                    counter = between(1, 65_535).toInt(),
                    emergencyType = EmergencyType.entries[below(3).toInt()],
                    automatic = flag(),
                    vulnerable = Vulnerable(flag(), flag(), flag(), flag()),
                    people = between(1, 255).toInt(),
                    latE7 = between(-900_000_000, 900_000_000).toInt(),
                    lonE7 = between(-1_800_000_000, 1_800_000_000).toInt(),
                    createdAtMs = createdAtMs(),
                )
                1L -> OnTheWay(alertKey(), deviceId(), createdAtMs())
                else -> Status(alertKey(), StatusCode.entries[below(2).toInt()], createdAtMs())
            }
            return WirePacket(packet, ttl = between(1, 8).toInt(), relayTag = between(0, 65_535).toInt())
        }

        fun bytes(size: Int): ByteArray = ByteArray(size) { rng.next().toByte() }
        fun size(max: Int): Int = below(max.toLong() + 1).toInt()
    }

    // --- Criterio 3 ---

    @Test
    fun `decode of encode returns the same packet for 1000 valid packets`() {
        val generator = Generator(seed = 1)

        repeat(1000) { i ->
            val wire = generator.wire()

            val payload = (PacketCodec.encode(wire) as EncodeResult.Ok).payload
            val back = (PacketCodec.decode(payload) as DecodeResult.Ok).wire

            assertEquals("paquete $i", wire, back)
        }
    }

    @Test
    fun `the 1000 generated packets cover the three types`() {
        val generator = Generator(seed = 1)
        val types = List(1000) { generator.wire().packet::class }.toSet()

        assertEquals(setOf(Alert::class, OnTheWay::class, Status::class), types)
    }

    // --- Criterio 6 ---

    @Test
    fun `encode and decode never throw with 10000 random inputs of 0 to 255 bytes`() {
        val generator = Generator(seed = 42)
        var decodedOk = 0

        repeat(10_000) { i ->
            val type = 1 + generator.below(3).toInt()
            val exactSize = when (type) {
                1 -> 27
                2 -> 23
                else -> 18
            }
            // Un cuarto de las entradas: largo cualquiera. Los demás: cabecera válida (y, en la mitad de esos,
            // el largo exacto) para recorrer las ramas de campos y no solo las primeras de decode.
            val size = if (i % 4 == 0 || i % 4 == 1) generator.size(255) else exactSize
            val bytes = generator.bytes(size)
            if (i % 4 != 0 && size >= 2) {
                bytes[0] = 0xA7.toByte()
                bytes[1] = (0x10 or type).toByte()
            }

            val result = PacketCodec.decode(bytes)

            if (result is DecodeResult.Ok) {
                decodedOk++
                assertTrue("encode rechazó un paquete decodificado", PacketCodec.encode(result.wire) is EncodeResult.Ok)
            }
        }
        assertTrue("el ruido con cabecera y largo válidos debería decodificar alguna vez", decodedOk > 0)
    }

    @Test
    fun `encode of decode returns the input for valid inputs without the reserved bit`() {
        val generator = Generator(seed = 99)

        repeat(500) {
            val payload = (PacketCodec.encode(generator.wire()) as EncodeResult.Ok).payload
            val wire = (PacketCodec.decode(payload) as DecodeResult.Ok).wire

            assertArrayEquals((PacketCodec.encode(wire) as EncodeResult.Ok).payload, payload)
        }
    }

    @Test
    fun `decode never throws on every length from 0 to 40 with a valid header`() {
        for (type in 1..3) {
            for (size in 0..40) {
                val bytes = ByteArray(size)
                if (size >= 1) bytes[0] = 0xA7.toByte()
                if (size >= 2) bytes[1] = (0x10 or type).toByte()

                PacketCodec.decode(bytes) // no debe lanzar
            }
        }
    }
}
