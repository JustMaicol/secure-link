package com.senda.ui.format

// spec 100, sección 11 · Rumbo cardinal: 8 sectores de 45° centrados en cada punto.
// Las siglas son las de la spec, en español: S/O = sur/oeste (`SO` = suroeste, `NO` = noroeste).
enum class CardinalPoint { N, NE, E, SE, S, SO, O, NO }

private const val SECTOR_DEGREES = 45.0

/**
 * Los límites pertenecen al sector siguiente en sentido horario: 22,5° ya es `NE` y 337,5° ya es `N`.
 * Acepta cualquier ángulo (negativo o mayor que 360°); un valor no numérico da null.
 */
fun cardinalOf(degrees: Double?): CardinalPoint? {
    if (degrees == null || degrees.isNaN() || degrees.isInfinite()) return null
    val normalized = ((degrees % 360.0) + 360.0) % 360.0
    val sector = Math.floor((normalized + SECTOR_DEGREES / 2) / SECTOR_DEGREES).toInt() % CardinalPoint.entries.size
    return CardinalPoint.entries[sector]
}
