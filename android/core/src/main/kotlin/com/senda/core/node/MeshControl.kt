package com.senda.core.node

import kotlinx.coroutines.flow.StateFlow

// spec 107, sección 2 · El usuario enciende o apaga la malla desde la pantalla Estado (109).
interface MeshControl {
    val enabled: StateFlow<Boolean>

    fun setEnabled(enabled: Boolean)
}
