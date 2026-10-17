package com.senda.core.protocol

// spec 101 · Contenido inmutable: lo que crea el origen y nunca cambia en la malla.
// Los constructores no validan; validan encode, decode y las fábricas (devuelven errores, no excepciones).
// deviceId y volunteerDeviceId son Long con 48 bits útiles (0..DEVICE_ID_MAX, spec 100).
sealed interface Packet {
    val key: PacketKey

    /** Epoch en ms, UTC, múltiplo de 1000 (spec 001). */
    val createdAtMs: Long
}

data class Alert(
    val deviceId: Long,
    val counter: Int,
    val emergencyType: EmergencyType,
    val automatic: Boolean,
    val vulnerable: Vulnerable,
    val people: Int,
    val latE7: Int,
    val lonE7: Int,
    override val createdAtMs: Long,
) : Packet {
    override val key: AlertKey get() = AlertKey(deviceId, counter)
}

data class OnTheWay(
    val alert: AlertKey,
    val volunteerDeviceId: Long,
    override val createdAtMs: Long,
) : Packet {
    override val key: OnTheWayKey get() = OnTheWayKey(alert, volunteerDeviceId)
}

data class Status(
    val alert: AlertKey,
    val status: StatusCode,
    override val createdAtMs: Long,
) : Packet {
    override val key: StatusKey get() = StatusKey(alert, status)
}

// spec 101 · Sobre: lo que cambia en cada salto (ttl y relayTag). Nunca entra en la clave.
data class WirePacket(val packet: Packet, val ttl: Int, val relayTag: Int)
