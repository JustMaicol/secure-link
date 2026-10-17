package com.securelink.permissions

// spec 100 · Requisitos que revisa la pantalla de permisos. El orden del enum es el orden del contrato;
// la pantalla puede ordenar sus filas distinto, según el diseño.
enum class Requirement {
    BLUETOOTH,
    LOCATION,
    NOTIFICATIONS,
    BLUETOOTH_ON,
    LOCATION_ON,
    BACKGROUND_LOCATION,
    BATTERY_UNRESTRICTED,
}
