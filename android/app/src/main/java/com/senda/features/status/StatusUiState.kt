package com.senda.features.status

// spec 109 · Estado que dibuja S1 (Estado de la red). Es dato puro: el ViewModel (con MeshStatus, SyncStatus,
// DeviceIdentity y los ajustes) lo arma; la pantalla solo lo dibuja. Las horas llegan ya formateadas (`formatClock`
// con la zona del teléfono); los tiempos transcurridos, en milisegundos, para escribirlos con las reglas de formato (100).

/** Herramientas de simulación de la demo. Solo en la versión de depuración (decisión del equipo, 2026-10-17). */
data class StatusDebugUiState(
    /** Etiquetas de relay bloqueadas por el filtro de depuración (`formatRelayTag`). */
    val blockedTags: List<String> = emptyList(),
    /** Última lectura del acelerómetro, en g, para el medidor de prueba. */
    val motionReadingG: Float = 0f,
)

data class StatusUiState(
    val meshActive: Boolean,
    /** Anuncios BLE activos ahora mismo. */
    val announcementsActive: Int,
    /** Teléfonos vecinos vistos recientemente. */
    val neighbors: Int,
    val internetOnline: Boolean,
    val pendingUploads: Int,
    val lastUploadClock: String?,
    val lastUploadAgeMs: Long?,
    /** Identificador del teléfono (`formatDeviceId`) y su etiqueta de relay (`formatRelayTag`). */
    val deviceId: String,
    val relayTag: String,
    val motionEnabled: Boolean,
    val motionCountdownSeconds: Int,
    /** Nulo fuera de la versión de depuración: no se dibuja la tarjeta de simulación. */
    val debug: StatusDebugUiState? = null,
)

/** Fondo de la escala del medidor de movimiento, en g (las marcas del diseño: 0 g, 1 g y 2 g). */
const val MOTION_SCALE_MAX_G = 2f
