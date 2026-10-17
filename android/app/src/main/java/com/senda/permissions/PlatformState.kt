package com.senda.permissions

// spec 100, sección 5.2 · Lo que AndroidPermissionChecker necesita saber del sistema. Separado para que
// la lógica de agrupar (qué cuenta como "concedido") se pruebe en la JVM con FakePlatformState.
interface PlatformState {
    fun isGranted(permission: String): Boolean

    /** El adaptador de Bluetooth está encendido. */
    fun bluetoothEnabled(): Boolean

    /** La ubicación del sistema (el interruptor general) está activada. */
    fun locationEnabled(): Boolean

    /** La app está excluida de la optimización de batería. */
    fun ignoringBatteryOptimizations(): Boolean
}

class FakePlatformState(
    var granted: Set<String> = emptySet(),
    var bluetoothEnabled: Boolean = false,
    var locationEnabled: Boolean = false,
    var ignoringBatteryOptimizations: Boolean = false,
) : PlatformState {
    override fun isGranted(permission: String): Boolean = permission in granted

    override fun bluetoothEnabled(): Boolean = bluetoothEnabled

    override fun locationEnabled(): Boolean = locationEnabled

    override fun ignoringBatteryOptimizations(): Boolean = ignoringBatteryOptimizations
}
