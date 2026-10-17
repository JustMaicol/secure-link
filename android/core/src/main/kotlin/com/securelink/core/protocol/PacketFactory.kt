package com.securelink.core.protocol

// spec 101 · Crea paquetes nuevos en el origen, con ttl = config.ttlMax y relayTag = relayTagOf(deviceId).
// Contrato: las firmas ya están; los cuerpos llegan en C4 (reglas en la spec 101, sección 6).
object PacketFactory {
    fun newAlert(
        deviceId: Long,
        counter: Int,
        type: EmergencyType,
        vulnerable: Vulnerable,
        people: Int,
        latE7: Int,
        lonE7: Int,
        nowMs: Long,
        config: ProtocolConfig = ProtocolConfig.DEFAULT,
    ): CreateResult = TODO("spec 101: C4")

    fun newAutomaticAlert(
        deviceId: Long,
        counter: Int,
        latE7: Int,
        lonE7: Int,
        nowMs: Long,
        config: ProtocolConfig = ProtocolConfig.DEFAULT,
    ): CreateResult = TODO("spec 101: C4")

    fun newOnTheWay(
        volunteerDeviceId: Long,
        alert: AlertKey,
        nowMs: Long,
        config: ProtocolConfig = ProtocolConfig.DEFAULT,
    ): CreateResult = TODO("spec 101: C4")

    /** Solo el origen de la alerta cancela o la marca a salvo. */
    fun newStatus(
        deviceId: Long,
        alert: AlertKey,
        status: StatusCode,
        nowMs: Long,
        config: ProtocolConfig = ProtocolConfig.DEFAULT,
    ): CreateResult = TODO("spec 101: C4")
}

sealed interface CreateResult {
    data class Ok(val wire: WirePacket) : CreateResult

    data class Err(val error: FieldError) : CreateResult
}
