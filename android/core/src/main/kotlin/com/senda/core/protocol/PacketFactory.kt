package com.senda.core.protocol

// spec 101, sección 6 · Crea paquetes nuevos en el origen, con ttl = config.ttlMax y relayTag = relayTagOf(deviceId).
// Devuelve errores en lugar de lanzar excepciones. El resultado pasa por las mismas validaciones que encode.
object PacketFactory {

    private const val PEOPLE_MAX = 255

    /** `people > 255` se guarda como 255 ("255 o más"); `people < 1` es error. `automatic` siempre es false. */
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
    ): CreateResult {
        if (people < 1) return CreateResult.Err(FieldError("people"))
        val alert = Alert(
            deviceId = deviceId,
            counter = counter,
            emergencyType = type,
            automatic = false,
            vulnerable = vulnerable,
            people = minOf(people, PEOPLE_MAX),
            latE7 = latE7,
            lonE7 = lonE7,
            createdAtMs = roundToSeconds(nowMs),
        )
        return finish(alert, deviceId, config)
    }

    /** Movimiento brusco: atrapado o grave, automática, 1 persona y sin vulnerables (000 y 001, sección 3.1). */
    fun newAutomaticAlert(
        deviceId: Long,
        counter: Int,
        latE7: Int,
        lonE7: Int,
        nowMs: Long,
        config: ProtocolConfig = ProtocolConfig.DEFAULT,
    ): CreateResult {
        val alert = Alert(
            deviceId = deviceId,
            counter = counter,
            emergencyType = EmergencyType.TRAPPED_OR_SEVERE,
            automatic = true,
            vulnerable = Vulnerable(),
            people = 1,
            latE7 = latE7,
            lonE7 = lonE7,
            createdAtMs = roundToSeconds(nowMs),
        )
        return finish(alert, deviceId, config)
    }

    fun newOnTheWay(
        volunteerDeviceId: Long,
        alert: AlertKey,
        nowMs: Long,
        config: ProtocolConfig = ProtocolConfig.DEFAULT,
    ): CreateResult =
        finish(OnTheWay(alert, volunteerDeviceId, roundToSeconds(nowMs)), volunteerDeviceId, config)

    /** Solo el origen cancela o marca a salvo: si [deviceId] no es el de la alerta, el error es `alertDeviceId`. */
    fun newStatus(
        deviceId: Long,
        alert: AlertKey,
        status: StatusCode,
        nowMs: Long,
        config: ProtocolConfig = ProtocolConfig.DEFAULT,
    ): CreateResult {
        if (alert.deviceId != deviceId) return CreateResult.Err(FieldError("alertDeviceId"))
        return finish(Status(alert, status, roundToSeconds(nowMs)), deviceId, config)
    }

    /** floorDiv y no `/`: con una hora negativa el resultado sigue siendo múltiplo de 1000 y encode la rechaza. */
    private fun roundToSeconds(nowMs: Long): Long = Math.floorDiv(nowMs, 1000L) * 1000L

    private fun finish(packet: Packet, emitterDeviceId: Long, config: ProtocolConfig): CreateResult {
        val wire = WirePacket(packet, ttl = config.ttlMax, relayTag = relayTagOf(emitterDeviceId))
        return PacketCodec.validate(wire, config)?.let { CreateResult.Err(it) } ?: CreateResult.Ok(wire)
    }
}

sealed interface CreateResult {
    data class Ok(val wire: WirePacket) : CreateResult

    data class Err(val error: FieldError) : CreateResult
}
