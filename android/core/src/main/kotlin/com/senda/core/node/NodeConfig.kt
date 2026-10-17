package com.senda.core.node

// spec 107, sección 1 · Constantes del nodo en un solo lugar (regla 17).
// Se pasa como parámetro, no como global, para que las pruebas puedan sobrescribirlo.
data class NodeConfig(
    /** Tras recibir un paquete, el nodo se queda en modo ACTIVO este tiempo. */
    val activeHoldMs: Long = 120_000,
    /** Tiempo mínimo entre cambios de modo (Android limita los reinicios de escaneo). */
    val modeMinDwellMs: Long = 30_000,
    /** "Teléfonos cerca": relayTag distintos oídos en esta ventana. */
    val neighborWindowMs: Long = 60_000,
    /** Cuenta regresiva de la alerta automática (la 108 solo muestra remainingMs). */
    val autoCountdownMs: Long = 15_000,
    /** Reintentos al guardar la alerta automática. */
    val autoSaveRetryMax: Int = 3,
    /** El wake lock se pide siempre con tiempo límite y se renueva mientras haya anuncios. */
    val wakeLockTimeoutMs: Long = 70_000,
    /** La notificación del servicio no se actualiza más seguido que esto. */
    val serviceNotifMinUpdateMs: Long = 30_000,
    /** Solo suena la primera alerta cercana de cada ventana. */
    val alertSoundMinGapMs: Long = 60_000,
) {
    companion object {
        val DEFAULT = NodeConfig()
    }
}
