package com.securelink.ui.format

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// spec 100, sección 11 · Valores que no cambian con el idioma: se formatean aquí, sin strings.xml.

/** Hora `HH:mm` en la zona del teléfono. Solo al mostrar: internamente todo es UTC (regla 11). */
fun formatClock(epochMs: Long, zone: ZoneId): String =
    CLOCK_FORMAT.format(Instant.ofEpochMilli(epochMs).atZone(zone))

private val CLOCK_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)

/**
 * Cuenta atrás en segundos enteros (`15`), para la alerta automática. Se redondea hacia arriba:
 * muestra 15 al empezar y 1 hasta el último instante; nunca baja de 0.
 */
fun countdownSeconds(remainingMs: Long): Int = ceilSeconds(remainingMs)

/** Cuenta atrás `m:ss` (`0:42`), para "anunciándose". Mismo redondeo que [countdownSeconds]. */
fun countdownMinutesSeconds(remainingMs: Long): String {
    val seconds = ceilSeconds(remainingMs)
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}

private fun ceilSeconds(remainingMs: Long): Int =
    if (remainingMs <= 0) 0 else ((remainingMs + 999) / 1000).toInt()

/** `deviceId` de 48 bits como 12 hexadecimales en minúscula, en grupos de 4: `a1b2 c3d4 e5f6`. */
fun formatDeviceId(deviceId: Long): String =
    (deviceId and 0xFFFF_FFFF_FFFFL).toString(16).padStart(12, '0').chunked(4).joinToString(" ")

/**
 * Coordenadas como grados decimales con coma y 4 decimales (unos 11 m): `-33,5000, -70,6500`. Se calcula con
 * enteros (grados × 10^7, regla 11), sin pasar por flotantes. Sustituye al nombre del lugar del diseño, que sin
 * internet no existe (el paquete solo lleva latitud y longitud).
 */
fun formatCoordinates(latE7: Int, lonE7: Int): String = "${degrees(latE7)}, ${degrees(lonE7)}"

private fun degrees(e7: Int): String {
    val abs = Math.abs(e7.toLong())
    val tenThousandths = (abs + 500) / 1000 // redondeo a 4 decimales, mitad hacia arriba
    val sign = if (e7 < 0 && tenThousandths != 0L) "-" else ""
    return "$sign${tenThousandths / 10_000},${(tenThousandths % 10_000).toString().padStart(4, '0')}"
}

/** `relayTag` de 16 bits como 4 hexadecimales en minúscula: `e5f6`. */
fun formatRelayTag(relayTag: Int): String = (relayTag and 0xFFFF).toString(16).padStart(4, '0')

sealed interface PrecisionLabel {
    data object Unknown : PrecisionLabel

    /** Metros enteros: `±15 m`. */
    data class Meters(val meters: Int) : PrecisionLabel
}

fun classifyPrecision(accuracyMeters: Double?): PrecisionLabel {
    if (accuracyMeters == null || accuracyMeters.isNaN() || accuracyMeters.isInfinite() || accuracyMeters < 0) {
        return PrecisionLabel.Unknown
    }
    return PrecisionLabel.Meters(Math.floor(accuracyMeters + 0.5).toInt())
}
