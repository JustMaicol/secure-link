package com.securelink.permissions

import android.content.Context

// spec 100, sección 5.2 · Implementación real de PermissionChecker.
//   BLUETOOTH      → los tres permisos de Bluetooth juntos
//   LOCATION       → ubicación fina y aproximada juntas
//   NOTIFICATIONS  → POST_NOTIFICATIONS
//   BLUETOOTH_ON / LOCATION_ON → ajustes del sistema encendidos
//   BATTERY_UNRESTRICTED       → exclusión de la optimización de batería
class AndroidPermissionChecker(private val platform: PlatformState) : PermissionChecker {

    constructor(context: Context) : this(AndroidPlatformState(context))

    override fun check(): Map<Requirement, Boolean> = Requirement.entries.associateWith { requirement ->
        when (requirement) {
            Requirement.BLUETOOTH,
            Requirement.LOCATION,
            Requirement.NOTIFICATIONS,
            -> runtimePermissionsOf(requirement).all(platform::isGranted)
            Requirement.BLUETOOTH_ON -> platform.bluetoothEnabled()
            Requirement.LOCATION_ON -> platform.locationEnabled()
            Requirement.BATTERY_UNRESTRICTED -> platform.ignoringBatteryOptimizations()
        }
    }
}
