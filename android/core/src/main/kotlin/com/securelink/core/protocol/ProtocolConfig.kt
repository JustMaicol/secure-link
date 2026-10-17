package com.securelink.core.protocol

// spec 101 · Valores de la spec 001, sección 8. Es el único lugar del código Android donde aparecen (regla 17).
// Se pasa como parámetro, no como global, para que los escenarios de la 103 puedan sobrescribirlo.
data class ProtocolConfig(
    val companyId: Int = 0xFFFF,
    val magic: Int = 0xA7,
    val protocolVersion: Int = 1,
    val maxPayloadBytes: Int = 27,
    val ttlMax: Int = 8,
    val forwardDelayMaxMs: Long = 2_000,
    val announceWindowMs: Long = 60_000,
    val originAnnounceWindowMs: Long = 60_000,
    val maxAgeMs: Long = 86_400_000,
) {
    companion object {
        val DEFAULT = ProtocolConfig()
    }
}
