package com.securelink.core.protocol

// spec 101 · Contrato: las firmas ya están; los cuerpos llegan en C3 (spec 001, secciones 3 y 5).
object PacketCodec {
    /** Valida en orden de offset y devuelve el primer campo inválido. */
    fun encode(wire: WirePacket, config: ProtocolConfig = ProtocolConfig.DEFAULT): EncodeResult =
        TODO("spec 101: C3")

    /** Errores en el orden de la 001, sección 5. Nunca lanza, con entradas de cualquier tamaño. */
    fun decode(payload: ByteArray, config: ProtocolConfig = ProtocolConfig.DEFAULT): DecodeResult =
        TODO("spec 101: C3")
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
