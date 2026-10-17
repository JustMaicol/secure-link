package com.securelink.permissions

// spec 100 · BATTERY_UNRESTRICTED es recomendado, no bloquea (sección 5.2).
private val BLOCKING = Requirement.entries - Requirement.BATTERY_UNRESTRICTED

/**
 * Puro: requisitos bloqueantes que faltan, en el orden del enum.
 * Un requisito ausente del mapa cuenta como faltante (no se asume concedido).
 */
fun missingBlocking(state: Map<Requirement, Boolean>): List<Requirement> =
    BLOCKING.filter { state[it] != true }
