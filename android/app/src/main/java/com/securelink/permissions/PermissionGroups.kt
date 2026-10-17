package com.securelink.permissions

import android.Manifest

// spec 100, sección 5.1 · Permisos en tiempo de ejecución que agrupa cada requisito.
// Se piden juntos: el usuario ve un solo diálogo por grupo.
private val RUNTIME_PERMISSIONS: Map<Requirement, List<String>> = mapOf(
    Requirement.BLUETOOTH to listOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.BLUETOOTH_CONNECT,
    ),
    Requirement.LOCATION to listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    ),
    Requirement.NOTIFICATIONS to listOf(
        Manifest.permission.POST_NOTIFICATIONS,
    ),
)

/**
 * Los permisos en tiempo de ejecución de un requisito, o vacío si no es de ese tipo (`BLUETOOTH_ON`,
 * `LOCATION_ON` y `BATTERY_UNRESTRICTED` son ajustes del sistema, no permisos).
 */
fun runtimePermissionsOf(requirement: Requirement): List<String> = RUNTIME_PERMISSIONS[requirement].orEmpty()
