package com.senda.permissions

/**
 * Recomendados, no bloquean el paso (spec 100, sección 5.2): el diseño deja continuar sin notificaciones, y la
 * ubicación en segundo plano y la exclusión de batería solo hacen más fiable el servicio.
 */
val RECOMMENDED_REQUIREMENTS: Set<Requirement> = setOf(
    Requirement.NOTIFICATIONS,
    Requirement.BACKGROUND_LOCATION,
    Requirement.BATTERY_UNRESTRICTED,
)

private val BLOCKING = Requirement.entries.filterNot { it in RECOMMENDED_REQUIREMENTS }

/**
 * Puro: requisitos bloqueantes que faltan, en el orden del enum.
 * Un requisito ausente del mapa cuenta como faltante (no se asume concedido).
 */
fun missingBlocking(state: Map<Requirement, Boolean>): List<Requirement> =
    BLOCKING.filter { state[it] != true }
