package com.senda.core.node

import kotlinx.coroutines.flow.StateFlow

// spec 107, sección 2 · Estado de la malla para las pantallas (108, 109).

enum class MeshHealth {
    STARTING,
    RUNNING,
    STOPPED,
    BLUETOOTH_OFF,
    NO_PERMISSION,
    ADVERTISE_UNSUPPORTED,
    ADVERTISE_FAILED,
    SCAN_FAILED,
}

enum class PowerMode { ACTIVE, IDLE }

data class MeshState(
    val health: MeshHealth,
    val scanning: Boolean,
    val advertising: Boolean,
    val activeAnnouncements: Int,
    val neighbors: Int,
    val lastReceivedAtMs: Long?,
    val powerMode: PowerMode,
)

interface MeshStatus {
    val state: StateFlow<MeshState>
}
