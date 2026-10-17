package com.securelink.ui.format

// spec 100, sección 11 · Distancia. La clasificación es pura; el texto sale de strings.xml (con plurales).
//   < 1000 m: redondeo a 10 m · de 1 a 100 km: un decimal con coma · ≥ 100 km: entero · sin dato: —
sealed interface DistanceLabel {
    /** Sin dato (o un valor inválido): se muestra "—". */
    data object Unknown : DistanceLabel

    /** Múltiplo de 10, de 0 a 990: `850 m`. */
    data class Meters(val meters: Int) : DistanceLabel

    /** Décimas de kilómetro, de 10 a 999: 12 = `1,2 km`, 999 = `99,9 km`. */
    data class Kilometers(val tenths: Int) : DistanceLabel

    /** 100 km o más, entero: `123 km`. */
    data class WholeKilometers(val km: Int) : DistanceLabel
}

/**
 * Se clasifica el valor ya redondeado, para que ninguna cifra salte de unidad al redondear:
 * - 995 a 999 m dan `1,0 km` (redondeados a 10 m serían `1000 m`).
 * - 99,95 km o más dan `100 km` (redondeado a un decimal serían `100,0 km`).
 * [DECISIÓN] La spec solo dice "< 1000 m" y "≥ 100 km"; estos dos bordes los fija esta función.
 */
fun classifyDistance(meters: Double?): DistanceLabel {
    if (meters == null || meters.isNaN() || meters.isInfinite() || meters < 0) return DistanceLabel.Unknown

    val roundedTo10 = Math.floor(meters / 10 + 0.5).toLong() * 10
    if (roundedTo10 < 1000) return DistanceLabel.Meters(roundedTo10.toInt())

    val tenths = Math.floor(meters / 100 + 0.5).toLong()
    if (tenths < 1000) return DistanceLabel.Kilometers(tenths.toInt())

    return DistanceLabel.WholeKilometers(Math.floor(meters / 1000 + 0.5).toInt())
}
