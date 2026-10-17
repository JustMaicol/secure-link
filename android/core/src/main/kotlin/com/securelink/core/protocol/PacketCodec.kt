package com.securelink.core.protocol

import com.securelink.core.common.DEVICE_ID_MAX

// spec 101, sección 5 · Formato de la spec 001, secciones 2, 3 y 5. Funciones puras (regla 5).
// Los campos multibyte son big-endian. El bit reservado de `info` se escribe en 0 y se ignora al leer.
object PacketCodec {

    // Layout de la 001, sección 3: hechos del formato, no configuración (el byte mágico y la versión sí vienen de config).
    private const val TYPE_ALERT = 1
    private const val TYPE_ON_THE_WAY = 2
    private const val TYPE_STATUS = 3

    private const val ALERT_SIZE = 27
    private const val ON_THE_WAY_SIZE = 23
    private const val STATUS_SIZE = 18

    private const val COUNTER_MAX = 65_535
    private const val PEOPLE_MAX = 255
    private const val LAT_E7_MAX = 900_000_000
    private const val LON_E7_MAX = 1_800_000_000
    private const val UINT16_MAX = 65_535

    /** uint32 en segundos, expresado en ms. */
    private const val CREATED_AT_MS_MAX = 4_294_967_295_000L

    /** Valida en orden de offset y devuelve el primer campo inválido. Nunca trunca. */
    fun encode(wire: WirePacket, config: ProtocolConfig = ProtocolConfig.DEFAULT): EncodeResult {
        validate(wire, config)?.let { return EncodeResult.Err(it) }
        val payload = when (val packet = wire.packet) {
            is Alert -> encodeAlert(packet, wire, config)
            is OnTheWay -> encodeOnTheWay(packet, wire, config)
            is Status -> encodeStatus(packet, wire, config)
        }
        return EncodeResult.Ok(payload)
    }

    /** Errores en el orden de la 001, sección 5. Nunca lanza, con entradas de cualquier tamaño. */
    fun decode(payload: ByteArray, config: ProtocolConfig = ProtocolConfig.DEFAULT): DecodeResult {
        if (payload.size < 2 || payload[0].toInt() and 0xFF != config.magic) {
            return DecodeResult.Err(DecodeError.NotOurPacket)
        }
        val version = (payload[1].toInt() and 0xFF) ushr 4
        if (version != config.protocolVersion) return DecodeResult.Err(DecodeError.UnsupportedVersion(version))

        val type = payload[1].toInt() and 0x0F
        val expectedSize = when (type) {
            TYPE_ALERT -> ALERT_SIZE
            TYPE_ON_THE_WAY -> ON_THE_WAY_SIZE
            TYPE_STATUS -> STATUS_SIZE
            else -> return DecodeResult.Err(DecodeError.UnknownType(type))
        }
        if (payload.size != expectedSize) {
            return DecodeResult.Err(DecodeError.InvalidLength(expectedSize, payload.size))
        }
        return when (type) {
            TYPE_ALERT -> decodeAlert(payload, config)
            TYPE_ON_THE_WAY -> decodeOnTheWay(payload, config)
            else -> decodeStatus(payload, config)
        }
    }

    /** Validación compartida con PacketFactory: mismos rangos que encode, en orden de offset. */
    internal fun validate(wire: WirePacket, config: ProtocolConfig): FieldError? {
        val ttlOk = wire.ttl in 1..config.ttlMax
        val relayOk = wire.relayTag in 0..UINT16_MAX
        return when (val packet = wire.packet) {
            is Alert -> firstInvalid(
                "deviceId" to isDeviceId(packet.deviceId),
                "counter" to (packet.counter in 1..COUNTER_MAX),
                "people" to (packet.people in 1..PEOPLE_MAX),
                "latE7" to (packet.latE7 in -LAT_E7_MAX..LAT_E7_MAX),
                "lonE7" to (packet.lonE7 in -LON_E7_MAX..LON_E7_MAX),
                "createdAtS" to isCreatedAt(packet.createdAtMs),
                "ttl" to ttlOk,
                "relayTag" to relayOk,
            )
            is OnTheWay -> firstInvalid(
                "alertDeviceId" to isDeviceId(packet.alert.deviceId),
                "alertCounter" to (packet.alert.counter in 1..COUNTER_MAX),
                "volunteerDeviceId" to isDeviceId(packet.volunteerDeviceId),
                "createdAtS" to isCreatedAt(packet.createdAtMs),
                "ttl" to ttlOk,
                "relayTag" to relayOk,
            )
            is Status -> firstInvalid(
                "alertDeviceId" to isDeviceId(packet.alert.deviceId),
                "alertCounter" to (packet.alert.counter in 1..COUNTER_MAX),
                "createdAtS" to isCreatedAt(packet.createdAtMs),
                "ttl" to ttlOk,
                "relayTag" to relayOk,
            )
        }
    }

    // --- encode ---

    private fun encodeAlert(alert: Alert, wire: WirePacket, config: ProtocolConfig): ByteArray {
        val out = header(ALERT_SIZE, TYPE_ALERT, config)
        out.putUInt(2, 6, alert.deviceId)
        out.putUInt(8, 2, alert.counter.toLong())
        out[10] = infoOf(alert).toByte()
        out[11] = alert.people.toByte()
        out.putUInt(12, 4, alert.latE7.toLong())
        out.putUInt(16, 4, alert.lonE7.toLong())
        out.putUInt(20, 4, alert.createdAtMs / 1000)
        out[24] = wire.ttl.toByte()
        out.putUInt(25, 2, wire.relayTag.toLong())
        return out
    }

    private fun encodeOnTheWay(packet: OnTheWay, wire: WirePacket, config: ProtocolConfig): ByteArray {
        val out = header(ON_THE_WAY_SIZE, TYPE_ON_THE_WAY, config)
        out.putUInt(2, 6, packet.alert.deviceId)
        out.putUInt(8, 2, packet.alert.counter.toLong())
        out.putUInt(10, 6, packet.volunteerDeviceId)
        out.putUInt(16, 4, packet.createdAtMs / 1000)
        out[20] = wire.ttl.toByte()
        out.putUInt(21, 2, wire.relayTag.toLong())
        return out
    }

    private fun encodeStatus(packet: Status, wire: WirePacket, config: ProtocolConfig): ByteArray {
        val out = header(STATUS_SIZE, TYPE_STATUS, config)
        out.putUInt(2, 6, packet.alert.deviceId)
        out.putUInt(8, 2, packet.alert.counter.toLong())
        out[10] = packet.status.code.toByte()
        out.putUInt(11, 4, packet.createdAtMs / 1000)
        out[15] = wire.ttl.toByte()
        out.putUInt(16, 2, wire.relayTag.toLong())
        return out
    }

    private fun header(size: Int, type: Int, config: ProtocolConfig): ByteArray {
        val out = ByteArray(size)
        out[0] = config.magic.toByte()
        out[1] = ((config.protocolVersion shl 4) or type).toByte()
        return out
    }

    /** Bits 0-1 tipo, 2 automática, 3-6 vulnerables; el bit 7 (reservado) queda en 0. */
    private fun infoOf(alert: Alert): Int {
        var info = alert.emergencyType.code
        if (alert.automatic) info = info or (1 shl 2)
        if (alert.vulnerable.children) info = info or (1 shl 3)
        if (alert.vulnerable.elderly) info = info or (1 shl 4)
        if (alert.vulnerable.injured) info = info or (1 shl 5)
        if (alert.vulnerable.reducedMobility) info = info or (1 shl 6)
        return info
    }

    // --- decode (campos en orden de offset: el primero inválido gana) ---

    private fun decodeAlert(p: ByteArray, config: ProtocolConfig): DecodeResult {
        val counter = p.getUInt(8, 2).toInt()
        if (counter !in 1..COUNTER_MAX) return invalid("counter")

        val info = p[10].toInt() and 0xFF
        val emergencyType = EmergencyType.entries.firstOrNull { it.code == info and 0b11 }
            ?: return invalid("emergencyType")

        val people = p[11].toInt() and 0xFF
        if (people !in 1..PEOPLE_MAX) return invalid("people")

        val latE7 = p.getUInt(12, 4).toInt()
        if (latE7 !in -LAT_E7_MAX..LAT_E7_MAX) return invalid("latE7")
        val lonE7 = p.getUInt(16, 4).toInt()
        if (lonE7 !in -LON_E7_MAX..LON_E7_MAX) return invalid("lonE7")

        val ttl = p[24].toInt() and 0xFF
        if (ttl !in 1..config.ttlMax) return invalid("ttl")

        val alert = Alert(
            deviceId = p.getUInt(2, 6),
            counter = counter,
            emergencyType = emergencyType,
            automatic = info and (1 shl 2) != 0,
            vulnerable = Vulnerable(
                children = info and (1 shl 3) != 0,
                elderly = info and (1 shl 4) != 0,
                injured = info and (1 shl 5) != 0,
                reducedMobility = info and (1 shl 6) != 0,
            ),
            people = people,
            latE7 = latE7,
            lonE7 = lonE7,
            createdAtMs = p.getUInt(20, 4) * 1000,
        )
        return DecodeResult.Ok(WirePacket(alert, ttl, p.getUInt(25, 2).toInt()))
    }

    private fun decodeOnTheWay(p: ByteArray, config: ProtocolConfig): DecodeResult {
        val counter = p.getUInt(8, 2).toInt()
        if (counter !in 1..COUNTER_MAX) return invalid("alertCounter")

        val ttl = p[20].toInt() and 0xFF
        if (ttl !in 1..config.ttlMax) return invalid("ttl")

        val packet = OnTheWay(
            alert = AlertKey(p.getUInt(2, 6), counter),
            volunteerDeviceId = p.getUInt(10, 6),
            createdAtMs = p.getUInt(16, 4) * 1000,
        )
        return DecodeResult.Ok(WirePacket(packet, ttl, p.getUInt(21, 2).toInt()))
    }

    private fun decodeStatus(p: ByteArray, config: ProtocolConfig): DecodeResult {
        val counter = p.getUInt(8, 2).toInt()
        if (counter !in 1..COUNTER_MAX) return invalid("alertCounter")

        val code = p[10].toInt() and 0xFF
        val status = StatusCode.entries.firstOrNull { it.code == code } ?: return invalid("status")

        val ttl = p[15].toInt() and 0xFF
        if (ttl !in 1..config.ttlMax) return invalid("ttl")

        val packet = Status(
            alert = AlertKey(p.getUInt(2, 6), counter),
            status = status,
            createdAtMs = p.getUInt(11, 4) * 1000,
        )
        return DecodeResult.Ok(WirePacket(packet, ttl, p.getUInt(16, 2).toInt()))
    }

    // --- utilidades ---

    private fun invalid(field: String): DecodeResult = DecodeResult.Err(DecodeError.InvalidField(field))

    private fun firstInvalid(vararg checks: Pair<String, Boolean>): FieldError? =
        checks.firstOrNull { !it.second }?.let { FieldError(it.first) }

    private fun isDeviceId(value: Long): Boolean = value in 0L..DEVICE_ID_MAX

    private fun isCreatedAt(ms: Long): Boolean = ms in 0L..CREATED_AT_MS_MAX && ms % 1000 == 0L

    private fun ByteArray.putUInt(offset: Int, size: Int, value: Long) {
        for (i in 0 until size) this[offset + i] = (value ushr (8 * (size - 1 - i))).toByte()
    }

    private fun ByteArray.getUInt(offset: Int, size: Int): Long {
        var value = 0L
        for (i in 0 until size) value = (value shl 8) or (this[offset + i].toLong() and 0xFF)
        return value
    }
}

sealed interface EncodeResult {
    // No es data class: ByteArray no compara por contenido (las pruebas usan contentEquals).
    class Ok(val payload: ByteArray) : EncodeResult

    data class Err(val error: FieldError) : EncodeResult
}

sealed interface DecodeResult {
    data class Ok(val wire: WirePacket) : DecodeResult

    data class Err(val error: DecodeError) : DecodeResult
}

sealed interface DecodeError {
    data object NotOurPacket : DecodeError

    data class UnsupportedVersion(val version: Int) : DecodeError

    data class UnknownType(val code: Int) : DecodeError

    data class InvalidLength(val expected: Int, val actual: Int) : DecodeError

    data class InvalidField(val field: String) : DecodeError
}

/**
 * Campo rechazado. Los nombres son los de la 001: deviceId, counter, emergencyType, people, latE7, lonE7,
 * createdAtS, ttl, relayTag, alertDeviceId, alertCounter, volunteerDeviceId y status.
 */
data class FieldError(val field: String)
