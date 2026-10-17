package com.senda.permissions

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
    // Se pide aparte: Android 11 o superior no la mezcla con la ubicación normal.
    Requirement.BACKGROUND_LOCATION to listOf(
        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
    ),
)

/**
 * Los permisos en tiempo de ejecución de un requisito, o vacío si no es de ese tipo (`BLUETOOTH_ON`,
 * `LOCATION_ON` y `BATTERY_UNRESTRICTED` son ajustes del sistema, no permisos).
 */
fun runtimePermissionsOf(requirement: Requirement): List<String> = RUNTIME_PERMISSIONS[requirement].orEmpty()

/**
 * Requisito que debe estar concedido antes de poder pedir [requirement], o null si no depende de otro.
 * Android solo acepta pedir la ubicación en segundo plano cuando la ubicación normal ya está concedida.
 */
fun prerequisiteOf(requirement: Requirement): Requirement? =
    if (requirement == Requirement.BACKGROUND_LOCATION) Requirement.LOCATION else null
