package com.securelink.core.protocol

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

// spec 101 · Criterio 2: packets-valid.json y packets-invalid.json (specs/shared/vectors, de P2).
class PacketVectorsTest {

    private fun load(name: String): JsonArray = Json.parseToJsonElement(
        File(checkNotNull(System.getProperty("vectorsDir")) { "Falta vectorsDir" }, name).readText(),
    ).jsonArray

    private fun JsonObject.str(key: String) = getValue(key).jsonPrimitive.content
    private fun JsonObject.long(key: String) = getValue(key).jsonPrimitive.long
    private fun JsonObject.int(key: String) = getValue(key).jsonPrimitive.int
    private fun JsonObject.bool(key: String) = getValue(key).jsonPrimitive.boolean

    /** Convierte los campos del JSON al modelo. `createdAtS` sale en segundos. */
    private fun wireOf(p: JsonObject): WirePacket {
        val packet: Packet = when (val type = p.str("type")) {
            "ALERT" -> Alert(
                deviceId = p.long("deviceId"),
                counter = p.int("counter"),
                emergencyType = EmergencyType.entries.first { it.code == p.int("emergencyType") },
                automatic = p.bool("automatic"),
                vulnerable = Vulnerable(p.bool("children"), p.bool("elderly"), p.bool("injured"), p.bool("reducedMobility")),
                people = p.int("people"),
                latE7 = p.int("latE7"),
                lonE7 = p.int("lonE7"),
                createdAtMs = p.long("createdAtS") * 1000,
            )
            "ON_THE_WAY" -> OnTheWay(
                alert = AlertKey(p.long("alertDeviceId"), p.int("alertCounter")),
                volunteerDeviceId = p.long("volunteerDeviceId"),
                createdAtMs = p.long("createdAtS") * 1000,
            )
            "STATUS" -> Status(
                alert = AlertKey(p.long("alertDeviceId"), p.int("alertCounter")),
                status = StatusCode.entries.first { it.code == p.int("status") },
                createdAtMs = p.long("createdAtS") * 1000,
            )
            else -> throw AssertionError("Tipo desconocido en el vector: $type")
        }
        return WirePacket(packet, p.int("ttl"), p.int("relayTag"))
    }

    // --- packets-valid.json ---

    @Test
    fun `valid vectors include at least the three reference packets`() {
        val names = load("packets-valid.json").map { it.jsonObject.str("name") }

        assertTrue("packets-valid.json trae $names", names.size >= 3)
        assertTrue(names.containsAll(listOf("alert-reference", "on-the-way-reference", "status-reference")))
    }

    @Test
    fun `encode gives the hex, decode gives the packet and the key matches`() {
        for (entry in load("packets-valid.json").map { it.jsonObject }) {
            val name = entry.str("name")
            val wire = wireOf(entry.getValue("packet").jsonObject)
            val hex = entry.str("hex")

            val encoded = PacketCodec.encode(wire)
            assertTrue("$name: encode rechazó el paquete", encoded is EncodeResult.Ok)
            assertEquals("$name: hex", hex, Hex.encode((encoded as EncodeResult.Ok).payload))

            val decoded = PacketCodec.decode(Hex.decode(hex))
            assertTrue("$name: decode falló: $decoded", decoded is DecodeResult.Ok)
            assertEquals("$name: campos", wire, (decoded as DecodeResult.Ok).wire)

            assertEquals("$name: clave", entry.str("key"), wire.packet.key.canonical())
        }
    }

    // --- packets-invalid.json ---

    @Test
    fun `invalid vectors cover every decode error`() {
        val errors = load("packets-invalid.json").map { it.jsonObject.str("error") }.toSet()

        assertTrue("errores en el archivo: $errors", errors.containsAll(
            listOf("NOT_OUR_PACKET", "UNSUPPORTED_VERSION", "UNKNOWN_TYPE", "INVALID_LENGTH", "INVALID_FIELD", "NONE"),
        ))
    }

    @Test
    fun `decode returns the expected error and field for every invalid vector`() {
        for (entry in load("packets-invalid.json").map { it.jsonObject }) {
            val name = entry.str("name")
            val bytes = Hex.decode(entry.str("hex"))
            val result = PacketCodec.decode(bytes)

            when (val error = entry.str("error")) {
                "NONE" -> {
                    assertTrue("$name: debía decodificar y dio $result", result is DecodeResult.Ok)
                    // El bit reservado se ignora y al re-codificar vuelve a 0.
                    val reencoded = (PacketCodec.encode((result as DecodeResult.Ok).wire) as EncodeResult.Ok).payload
                    assertArrayEquals("$name: re-codificado", Hex.decode(entry.str("reencodedHex")), reencoded)
                }
                else -> {
                    assertTrue("$name: debía fallar y dio $result", result is DecodeResult.Err)
                    val actual = (result as DecodeResult.Err).error
                    assertEquals("$name: error", error, codeOf(actual))
                    if (actual is DecodeError.InvalidField) {
                        assertEquals("$name: campo", entry.str("field"), actual.field)
                    }
                }
            }
        }
    }

    private fun codeOf(error: DecodeError): String = when (error) {
        DecodeError.NotOurPacket -> "NOT_OUR_PACKET"
        is DecodeError.UnsupportedVersion -> "UNSUPPORTED_VERSION"
        is DecodeError.UnknownType -> "UNKNOWN_TYPE"
        is DecodeError.InvalidLength -> "INVALID_LENGTH"
        is DecodeError.InvalidField -> "INVALID_FIELD"
    }
}
