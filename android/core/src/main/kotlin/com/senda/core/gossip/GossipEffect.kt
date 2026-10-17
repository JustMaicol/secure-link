package com.senda.core.gossip

import com.senda.core.protocol.PacketKey
import com.senda.core.protocol.WirePacket

// spec 103, sección 2 · Lo que el motor le pide a quien lo usa (107 en el teléfono, el ejecutor en las pruebas).
sealed interface GossipEffect {
    /** Guardar antes que cualquier otra cosa (regla 21). */
    data class Store(val wire: WirePacket) : GossipEffect

    data class StartAnnounce(val wire: WirePacket, val untilMs: Long) : GossipEffect

    data class StopAnnounce(val key: PacketKey) : GossipEffect

    /** [ttl] solo en ANNOUNCE y RECEIVED; [relayTag] en BLOCKED y DUPLICATE (en los vectores, solo BLOCKED). */
    data class Event(
        val type: GossipEventType,
        val key: PacketKey,
        val ttl: Int? = null,
        val relayTag: Int? = null,
    ) : GossipEffect
}

enum class GossipEventType { ANNOUNCE, RECEIVED, DUPLICATE, BLOCKED, NO_FORWARD_TTL, NO_FORWARD_EXPIRED }
