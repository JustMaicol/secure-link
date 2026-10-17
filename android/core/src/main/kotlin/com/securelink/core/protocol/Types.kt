package com.securelink.core.protocol

// spec 101 · Tipos de emergencia (spec 001, sección 3.1). El código es el que viaja en el paquete.
enum class EmergencyType(val code: Int) {
    FOOD_WATER(0),
    MEDICAL(1),
    TRAPPED_OR_SEVERE(2),
}

// spec 101 · Estado de una alerta que su origen puede anunciar (spec 001, sección 3.3).
enum class StatusCode(val code: Int) {
    CANCELLED(1),
    SAFE(2),
}

// spec 101 · Personas vulnerables presentes, 4 bits del paquete.
data class Vulnerable(
    val children: Boolean = false,
    val elderly: Boolean = false,
    val injured: Boolean = false,
    val reducedMobility: Boolean = false,
)
