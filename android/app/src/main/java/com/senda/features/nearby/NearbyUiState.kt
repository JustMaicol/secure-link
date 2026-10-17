package com.senda.features.nearby

import com.senda.core.protocol.EmergencyType

// spec 109 · Estado que dibujan C1 (lista) y C2 (detalle). Es dato puro: el ViewModel (con las alertas guardadas, el GPS
// y la brújula) lo arma; las pantallas solo lo dibujan. Distancia y rumbo funcionan sin internet (decisión del equipo).

/**
 * Una alerta ajena cercana. Ningún dato es texto libre (regla 24): [coordinates] ya viene formateado
 * (`formatCoordinates`, 5 decimales con punto) y [reference] es una referencia corta derivada de la clave de la alerta.
 *
 * [distanceM] y [bearingDeg] son nulos mientras no haya ubicación del teléfono o rumbo: la pantalla muestra `—` y no
 * dibuja flecha. [bearingDeg] va de 0 a 360, desde el norte y en el sentido del reloj.
 */
data class NearbyAlertUi(
    val id: String,
    val reference: String,
    val type: EmergencyType,
    val receivedAgeMs: Long,
    val coordinates: String,
    val people: Int,
    val vulnerableCount: Int,
    val volunteers: Int,
    val distanceM: Double?,
    val bearingDeg: Double?,
    val accuracyM: Int,
    val locationAgeMs: Long,
)

data class NearbyListUiState(
    val alerts: List<NearbyAlertUi> = emptyList(),
    /** Tiempo desde la última actualización de la lista; nulo si todavía no hubo ninguna. */
    val updatedAgeMs: Long? = null,
    /** Datos de prueba: la pie de la lista lo dice (regla 30). */
    val demoData: Boolean = false,
)
