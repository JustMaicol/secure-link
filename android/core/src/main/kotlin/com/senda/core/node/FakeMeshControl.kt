package com.senda.core.node

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// spec 107 · MeshControl para pantallas y pruebas. La malla está encendida por defecto (como meshEnabled, 102).
class FakeMeshControl(initialEnabled: Boolean = true) : MeshControl {
    private val mutableEnabled = MutableStateFlow(initialEnabled)
    override val enabled: StateFlow<Boolean> = mutableEnabled.asStateFlow()

    /** Cuántas veces se llamó a setEnabled, para verificar interacciones. */
    var setEnabledCalls: Int = 0
        private set

    override fun setEnabled(enabled: Boolean) {
        setEnabledCalls++
        mutableEnabled.value = enabled
    }
}
