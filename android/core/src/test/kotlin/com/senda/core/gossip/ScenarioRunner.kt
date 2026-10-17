package com.senda.core.gossip

import com.senda.core.protocol.DecodeResult
import com.senda.core.protocol.Hex
import com.senda.core.protocol.Mulberry32
import com.senda.core.protocol.PacketCodec
import com.senda.core.protocol.ProtocolConfig
import com.senda.core.protocol.WirePacket
import java.util.PriorityQueue

// spec 103, sección 5 · Ejecuta varios motores conectados con el algoritmo de la 001, sección 6.7.
// Android (aquí) y el simulador (300) lo implementan igual, para producir la misma secuencia `expected`.

data class ScenarioNode(val id: String, val deviceId: Long, val seed: Int)

/** `originate.hex` de un evento del escenario: el teléfono [node] origina ese paquete en el instante [t]. */
data class ScenarioOrigination(val t: Long, val node: String, val hex: String)

data class Scenario(
    val name: String,
    val config: ProtocolConfig,
    val nodes: List<ScenarioNode>,
    val links: List<Pair<String, String>>,
    val blockedRelayTags: Map<String, Set<Int>>,
    val nowMs: Long,
    val originations: List<ScenarioOrigination>,
)

/**
 * Un evento de la traza. Como en los vectores, `ttl` solo viaja en ANNOUNCE y RECEIVED y `relayTag` solo en
 * BLOCKED (el relayTag de DUPLICATE no forma parte de los vectores, 001 sección 6.7).
 */
data class ScenarioEvent(
    val t: Long,
    val type: GossipEventType,
    val node: String,
    val key: String,
    val ttl: Int? = null,
    val relayTag: Int? = null,
)

object ScenarioRunner {

    /** Entrada de la cola: `kind 0` = acción del teléfono (originar o despertar), `kind 1` = entrega. */
    private class Entry(
        val t: Long,
        val kind: Int,
        val node: String,
        val key: String,
        val wire: WirePacket?,
        val seq: Long,
    )

    private val order = Comparator<Entry> { a, b ->
        val byT = a.t.compareTo(b.t)
        if (byT != 0) return@Comparator byT
        val byKind = a.kind.compareTo(b.kind)
        if (byKind != 0) return@Comparator byKind
        val byNode = a.node.compareTo(b.node)
        if (byNode != 0) return@Comparator byNode
        val byKey = a.key.compareTo(b.key) // la clave vacía ("") va antes que cualquier otra
        if (byKey != 0) byKey else a.seq.compareTo(b.seq)
    }

    fun run(scenario: Scenario): List<ScenarioEvent> {
        val engines = scenario.nodes.associate { node ->
            node.id to GossipEngine(
                deviceId = node.deviceId,
                rng = Mulberry32(node.seed),
                config = scenario.config,
                blockedRelayTags = scenario.blockedRelayTags[node.id].orEmpty(),
            )
        }
        val neighbors: Map<String, List<String>> = scenario.nodes.associate { node ->
            node.id to scenario.links.mapNotNull { (a, b) ->
                when (node.id) {
                    a -> b
                    b -> a
                    else -> null
                }
            }
        }

        val queue = PriorityQueue(order)
        val wakeups = HashSet<Pair<String, Long>>() // despertares ya en la cola: (teléfono, t)
        var seq = 0L
        val trace = mutableListOf<ScenarioEvent>()

        // 2. Cada originate entra como kind 0, con la clave de su paquete.
        for (origination in scenario.originations) {
            val wire = (PacketCodec.decode(Hex.decode(origination.hex)) as? DecodeResult.Ok)?.wire
                ?: throw AssertionError("${scenario.name}: el hex de originate no decodifica: ${origination.hex}")
            queue += Entry(origination.t, 0, origination.node, wire.packet.key.canonical(), wire, seq++)
        }

        while (queue.isNotEmpty()) {
            val entry = queue.poll() // 3. siempre la menor por (t, kind, node, key)
            val engine = engines.getValue(entry.node)
            val nowMs = scenario.nowMs + entry.t

            // 4. Según la entrada.
            val effects = when {
                entry.kind == 1 -> engine.onReceived(checkNotNull(entry.wire), nowMs)
                entry.wire != null -> engine.originate(entry.wire, nowMs)
                else -> {
                    wakeups.remove(entry.node to entry.t)
                    engine.tick(nowMs)
                }
            }

            for (effect in effects) {
                when (effect) {
                    // 5. Cada StartAnnounce se entrega a cada vecino en el mismo t, sin pérdida (6.6).
                    is GossipEffect.StartAnnounce -> neighbors.getValue(entry.node).forEach { neighbor ->
                        queue += Entry(entry.t, 1, neighbor, effect.wire.packet.key.canonical(), effect.wire, seq++)
                    }
                    // 7. La traza son los eventos, en el orden en que se producen.
                    is GossipEffect.Event -> trace += ScenarioEvent(
                        t = entry.t,
                        type = effect.type,
                        node = entry.node,
                        key = effect.key.canonical(),
                        ttl = effect.ttl.takeIf { effect.type == GossipEventType.ANNOUNCE || effect.type == GossipEventType.RECEIVED },
                        relayTag = effect.relayTag.takeIf { effect.type == GossipEventType.BLOCKED },
                    )
                    is GossipEffect.Store, is GossipEffect.StopAnnounce -> Unit
                }
            }

            // 6. Si el teléfono tiene algo pendiente y no hay ya un despertar suyo en ese tiempo, se agrega uno.
            engine.nextWakeupMs()?.let { wakeMs ->
                val wakeT = wakeMs - scenario.nowMs
                check(wakeT >= entry.t) { "${scenario.name}: despertar en el pasado ($wakeT < ${entry.t}) en ${entry.node}" }
                if (wakeups.add(entry.node to wakeT)) queue += Entry(wakeT, 0, entry.node, "", null, seq++)
            }
        }
        return trace
    }
}
