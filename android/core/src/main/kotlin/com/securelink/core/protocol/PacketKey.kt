package com.securelink.core.protocol

// spec 101 · Claves de deduplicación: solo campos inmutables (spec 001, sección 4). Nunca ttl ni relayTag.
// Forma canónica: hexadecimal en minúscula con ceros a la izquierda. El orden es el del texto (regla 9).
sealed interface PacketKey : Comparable<PacketKey> {
    fun canonical(): String

    override fun compareTo(other: PacketKey): Int = canonical().compareTo(other.canonical())
}

/** `a1b2c3d4e5f6-0001`. También es el `alertId` de la base de datos (spec 002). */
data class AlertKey(val deviceId: Long, val counter: Int) : PacketKey {
    override fun canonical(): String = "${hex(deviceId, DEVICE_ID_DIGITS)}-${hex(counter.toLong(), COUNTER_DIGITS)}"
}

/** `a1b2c3d4e5f6-0001/0c0c0c0c0c0c` */
data class OnTheWayKey(val alert: AlertKey, val volunteerDeviceId: Long) : PacketKey {
    override fun canonical(): String = "${alert.canonical()}/${hex(volunteerDeviceId, DEVICE_ID_DIGITS)}"
}

/** `a1b2c3d4e5f6-0001#2` */
data class StatusKey(val alert: AlertKey, val status: StatusCode) : PacketKey {
    override fun canonical(): String = "${alert.canonical()}#${status.code}"
}

/** Los 16 bits bajos del deviceId de quien emite físicamente el anuncio (spec 001, sección 4). */
fun relayTagOf(deviceId: Long): Int = (deviceId and 0xFFFF).toInt()

private const val DEVICE_ID_DIGITS = 12
private const val COUNTER_DIGITS = 4

private fun hex(value: Long, digits: Int): String = value.toString(16).padStart(digits, '0')
