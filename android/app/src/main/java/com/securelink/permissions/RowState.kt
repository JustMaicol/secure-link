package com.securelink.permissions

import android.content.Context

// spec 100, secciones 5.3 y 12 · Estado de cada fila de la pantalla de permisos.
enum class RowState { GRANTED, MISSING, DENIED_PERMANENTLY }

/**
 * Pura. "Negado permanentemente" es: el permiso falta, ya se pidió alguna vez (historial guardado) y Android
 * ya no ofrece volver a pedirlo (`shouldShowRequestPermissionRationale` es falso en todo lo que falta).
 * Solo aplica a permisos en tiempo de ejecución: lo demás (Bluetooth apagado, ubicación del sistema, batería)
 * se arregla en ajustes y nunca es "negado".
 *
 * @param isGranted consulta del sistema por permiso.
 * @param requestedBefore el historial de [PermissionRequestHistory] para este requisito.
 * @param showsRationale `shouldShowRequestPermissionRationale` por permiso (necesita una Activity).
 */
fun rowStateOf(
    requirement: Requirement,
    granted: Boolean,
    requestedBefore: Boolean,
    isGranted: (String) -> Boolean,
    showsRationale: (String) -> Boolean,
): RowState {
    if (granted) return RowState.GRANTED

    val missing = runtimePermissionsOf(requirement).filterNot(isGranted)
    val permanentlyDenied = missing.isNotEmpty() && requestedBefore && missing.none(showsRationale)
    return if (permanentlyDenied) RowState.DENIED_PERMANENTLY else RowState.MISSING
}

// spec 100, sección 5.3 · Android no dice si un permiso se negó antes o nunca se pidió: ambos casos tienen
// rationale falso. Por eso se guarda aquí si ya se pidió.
interface PermissionRequestHistory {
    fun wasRequested(requirement: Requirement): Boolean

    fun markRequested(requirement: Requirement)
}

class FakePermissionRequestHistory(requested: Set<Requirement> = emptySet()) : PermissionRequestHistory {
    private val requested = requested.toMutableSet()

    override fun wasRequested(requirement: Requirement): Boolean = requirement in requested

    override fun markRequested(requirement: Requirement) {
        requested += requirement
    }
}

class SharedPreferencesPermissionHistory(context: Context) : PermissionRequestHistory {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun wasRequested(requirement: Requirement): Boolean = prefs.getBoolean(requirement.name, false)

    override fun markRequested(requirement: Requirement) {
        // commit(): se marca justo antes de abrir el diálogo, y el proceso puede morir mientras está abierto.
        check(prefs.edit().putBoolean(requirement.name, true).commit()) { "No se pudo guardar el historial de ${requirement.name}" }
    }

    companion object {
        const val PREFS_NAME = "permission_history"
    }
}
