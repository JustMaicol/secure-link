package com.senda.permissions

// spec 100
interface PermissionChecker {
    /** Estado actual de cada requisito; se vuelve a consultar al volver a la app. */
    fun check(): Map<Requirement, Boolean>
}

// spec 100 · Para pruebas y para el AppContainer de prototipo.
class FakePermissionChecker(var state: Map<Requirement, Boolean>) : PermissionChecker {
    override fun check(): Map<Requirement, Boolean> = state

    companion object {
        /** Todo concedido, incluido el recomendado. */
        fun allGranted(): FakePermissionChecker =
            FakePermissionChecker(Requirement.entries.associateWith { true })
    }
}
