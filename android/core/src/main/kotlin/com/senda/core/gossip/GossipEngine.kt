package com.senda.core.gossip

import com.senda.core.protocol.Mulberry32
import com.senda.core.protocol.PacketKey
import com.senda.core.protocol.ProtocolConfig
import com.senda.core.protocol.WirePacket
import com.senda.core.protocol.relayTagOf
import java.util.TreeMap

/**
 * spec 103 · Reglas de reenvío de la 001, sección 6, como una clase pura: recibe entradas y devuelve efectos.
 * Nunca lee el reloj (regla 6): `nowMs` llega siempre como parámetro. No es seguro para varios hilos:
 * la 107 lo usa desde una sola corrutina. [rng] es exclusivo de este motor (001, sección 6.4).
 */
class GossipEngine(
    private val deviceId: Long,
    private val rng: Mulberry32,
    private val config: ProtocolConfig = ProtocolConfig.DEFAULT,
    initiallySeen: Collection<PacketKey> = emptyList(),
    blockedRelayTags: Set<Int> = emptySet(),
) {
    private class Pending(val dueMs: Long, val wire: WirePacket)

    private class Window(val wire: WirePacket, val untilMs: Long)

    private val seen: MutableSet<PacketKey> = HashSet(initiallySeen)
    private val pending: MutableList<Pending> = ArrayList()

    // Ordenado por clave (orden de su forma canónica): así tick y active son estables (regla 9).
    private val windows: TreeMap<PacketKey, Window> = TreeMap()
    private var blocked: Set<Int> = blockedRelayTags.toSet()

    /** Paquete propio, ya guardado por quien lo creó (108, 109). No emite `Store` ni consume `rng`. */
    fun originate(wire: WirePacket, nowMs: Long): List<GossipEffect> {
        val key = wire.packet.key
        if (!seen.add(key)) return listOf(GossipEffect.Event(GossipEventType.DUPLICATE, key))

        val untilMs = nowMs + config.originAnnounceWindowMs
        windows[key] = Window(wire, untilMs)
        return listOf(
            GossipEffect.StartAnnounce(wire, untilMs),
            GossipEffect.Event(GossipEventType.ANNOUNCE, key, ttl = wire.ttl),
        )
    }

    /** Paquete decodificado sin error por la 104. Los 6 pasos de la 001, sección 6.2, en orden. */
    fun onReceived(wire: WirePacket, nowMs: Long): List<GossipEffect> {
        val key = wire.packet.key

        // 1. Filtro de depuración. No marca la clave como vista: puede llegar después por otro relay.
        if (wire.relayTag in blocked) {
            return listOf(GossipEffect.Event(GossipEventType.BLOCKED, key, relayTag = wire.relayTag))
        }
        // 2. Duplicado. El relayTag le sirve a la 107 para detectar el eco de un paquete propio.
        if (!seen.add(key)) {
            return listOf(GossipEffect.Event(GossipEventType.DUPLICATE, key, relayTag = wire.relayTag))
        }

        // 3. Guardar y marcar como visto (ya marcado arriba).
        val effects = mutableListOf(
            GossipEffect.Store(wire),
            GossipEffect.Event(GossipEventType.RECEIVED, key, ttl = wire.ttl),
        )
        // 4. TTL agotado.
        if (wire.ttl <= 1) {
            effects += GossipEffect.Event(GossipEventType.NO_FORWARD_TTL, key)
            return effects
        }
        // 5. Caducado: se guarda y se sube igual, pero no se reenvía. Una hora futura no se rechaza.
        if (nowMs - wire.packet.createdAtMs > config.maxAgeMs) {
            effects += GossipEffect.Event(GossipEventType.NO_FORWARD_EXPIRED, key)
            return effects
        }
        // 6. Programar el reenvío. Es el único lugar donde se consume `rng`.
        val delayMs = rng.next() % (config.forwardDelayMaxMs + 1)
        pending += Pending(
            dueMs = nowMs + delayMs,
            wire = wire.copy(ttl = wire.ttl - 1, relayTag = relayTagOf(deviceId)),
        )
        return effects
    }

    /** Cierra las ventanas vencidas y luego inicia los reenvíos vencidos. Llamado tarde, anuncia en `nowMs`. */
    fun tick(nowMs: Long): List<GossipEffect> {
        val effects = mutableListOf<GossipEffect>()

        val expired = windows.filterValues { it.untilMs <= nowMs }.keys.toList() // ya viene en orden de clave
        for (key in expired) {
            windows.remove(key)
            effects += GossipEffect.StopAnnounce(key)
        }

        val due = pending
            .filter { it.dueMs <= nowMs }
            .sortedWith(compareBy<Pending> { it.dueMs }.thenBy { it.wire.packet.key })
        pending.removeAll(due.toSet())
        for (item in due) {
            val key = item.wire.packet.key
            val untilMs = nowMs + config.announceWindowMs
            windows[key] = Window(item.wire, untilMs)
            effects += GossipEffect.StartAnnounce(item.wire, untilMs)
            effects += GossipEffect.Event(GossipEventType.ANNOUNCE, key, ttl = item.wire.ttl)
        }
        return effects
    }

    /** Próximo instante en que [tick] tiene algo que hacer, o null si nada. */
    fun nextWakeupMs(): Long? {
        val nextDue = pending.minOfOrNull { it.dueMs }
        val nextClose = windows.values.minOfOrNull { it.untilMs }
        return listOfNotNull(nextDue, nextClose).minOrNull()
    }

    fun setBlockedRelayTags(tags: Set<Int>) {
        blocked = tags.toSet()
    }

    /** Anuncios con ventana abierta, ordenados por clave (para la 104 y la pantalla de estado). */
    fun active(): List<WirePacket> = windows.values.map { it.wire }
}
