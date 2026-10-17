package com.securelink.core.node

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// spec 107 · MeshStatus para pantallas y pruebas: el estado cambia solo con set.
class FakeMeshStatus(initial: MeshState = RUNNING) : MeshStatus {
    private val mutableState = MutableStateFlow(initial)
    override val state: StateFlow<MeshState> = mutableState.asStateFlow()

    fun set(state: MeshState) {
        mutableState.value = state
    }

    companion object {
        /** Malla sana y en reposo, sin vecinos. */
        val RUNNING = MeshState(
            health = MeshHealth.RUNNING,
            scanning = true,
            advertising = false,
            activeAnnouncements = 0,
            neighbors = 0,
            lastReceivedAtMs = null,
            powerMode = PowerMode.IDLE,
        )
    }
}
