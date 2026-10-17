package com.senda.features.sendalert

// spec 108, sección 1 · Constantes de la pestaña Enviar alerta (regla 17).

/** Tiempo de mantener presionado para enviar. */
const val SEND_HOLD_MS = 1_500L

/** A partir de aquí (5 min), la ubicación usada se marca como antigua. */
const val LOCATION_STALE_WARN_MS = 300_000L

/** Duración de la cuenta regresiva de la alerta automática (la lleva la 107; aquí solo dibuja el anillo). */
const val AUTO_COUNTDOWN_MS = 15_000L
