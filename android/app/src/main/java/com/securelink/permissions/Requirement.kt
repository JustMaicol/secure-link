package com.securelink.permissions

// spec 100 · Requisitos que revisa la pantalla de permisos. El orden del enum es el orden de la pantalla.
enum class Requirement {
    BLUETOOTH,
    LOCATION,
    NOTIFICATIONS,
    BLUETOOTH_ON,
    LOCATION_ON,
    BATTERY_UNRESTRICTED,
}
