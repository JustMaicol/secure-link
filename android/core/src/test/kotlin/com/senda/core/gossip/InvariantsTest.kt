package com.senda.core.gossip

import com.senda.core.gossip.GossipEventType.ANNOUNCE
import com.senda.core.gossip.GossipEventType.BLOCKED
import com.senda.core.gossip.GossipEventType.RECEIVED
import com.senda.core.protocol.EncodeResult
import com.senda.core.protocol.Fixtures
import com.senda.core.protocol.Hex
import com.senda.core.protocol.Mulberry32
import com.senda.core.protocol.PacketCodec
import com.senda.core.protocol.ProtocolConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// spec 103, criterio 7 · 200 topologías aleatorias generadas con Mulberry32(7).
class InvariantsTest {

    private class Topology(
        val kind: Int,
        val nodeCount: Int,
        val scenario: Scenario,
        val originId: String,
    )

    private val rng = Mulberry32(7)

    private fun below(n: Int): Int = (rng.next() % n).toInt()

    private fun hexWithTtl(ttl: Int): String =
        Hex.encode((PacketCodec.encode(Fixtures.alertWire().copy(ttl = ttl), ProtocolConfig.DEFAULT) as EncodeResult.Ok).payload)

    /**
     * kind 0: conexo, sin bloqueos, ttl = n-1 (el mínimo que garantiza entrega);
     * kind 1: conexo, sin bloqueos, ttl 8; kind 2: aleatorio (puede no ser conexo); kind 3: aleatorio con bloqueos.
     */
    private fun topology(index: Int): Topology {
        val kind = index % 4
        val n = 2 + below(8) // 2..9 teléfonos
        val ids = List(n) { "N$it" }

        val edges = linkedSetOf<Pair<Int, Int>>()
        if (kind <= 1) {
            for (i in 1 until n) edges += below(i) to i // árbol de expansión: conexo por construcción
        }
        for (a in 0 until n) for (b in a + 1 until n) {
            val chance = if (kind <= 1) 4 else 3
            if (below(chance) == 0) edges += a to b
        }

        val blocked = mutableMapOf<String, Set<Int>>()
        if (kind == 3) {
            for (i in 0 until n) if (below(3) == 0) {
                // Tags de otros teléfonos (su relayTag es i + 1) o el del origen de la alerta de referencia.
                blocked[ids[i]] = setOf(1 + below(n), 0xE5F6).take(1 + below(2)).toSet()
            }
        }

        val originIndex = below(n)
        val ttl = if (kind == 0) n - 1 else 8
        val scenario = Scenario(
            name = "topology-$index",
            config = ProtocolConfig.DEFAULT,
            nodes = ids.mapIndexed { i, id -> ScenarioNode(id, deviceId = (i + 1).toLong(), seed = rng.next().toInt()) },
            links = edges.map { (a, b) -> ids[a] to ids[b] },
            blockedRelayTags = blocked,
            nowMs = 1_760_600_000_000L,
            originations = listOf(ScenarioOrigination(t = 0, node = ids[originIndex], hex = hexWithTtl(ttl))),
        )
        return Topology(kind, n, scenario, ids[originIndex])
    }

    private fun component(topology: Topology): Set<String> {
        val adjacency = topology.scenario.nodes.associate { it.id to mutableSetOf<String>() }
        topology.scenario.links.forEach { (a, b) ->
            adjacency.getValue(a) += b
            adjacency.getValue(b) += a
        }
        val reached = linkedSetOf(topology.originId)
        val frontier = ArrayDeque(listOf(topology.originId))
        while (frontier.isNotEmpty()) {
            adjacency.getValue(frontier.removeFirst()).forEach { if (reached.add(it)) frontier.addLast(it) }
        }
        return reached
    }

    private val topologies: List<Topology> = List(200) { topology(it) }

    @Test
    fun `there are 200 topologies and the generator covers every kind`() {
        assertEquals(200, topologies.size)
        assertEquals(setOf(0, 1, 2, 3), topologies.map { it.kind }.toSet())
    }

    @Test
    fun `no phone receives the same key twice and none announces it twice`() {
        for (topology in topologies) {
            val trace = ScenarioRunner.run(topology.scenario)

            val received = trace.filter { it.type == RECEIVED }.groupingBy { it.node to it.key }.eachCount()
            assertTrue("${topology.scenario.name}: RECEIVED repetido $received", received.values.all { it == 1 })

            val announced = trace.filter { it.type == ANNOUNCE }.groupingBy { it.node to it.key }.eachCount()
            assertTrue("${topology.scenario.name}: ANNOUNCE repetido $announced", announced.values.all { it == 1 })
        }
    }

    @Test
    fun `the origin never receives its own packet and nobody outside its component does`() {
        for (topology in topologies) {
            val receivers = ScenarioRunner.run(topology.scenario).filter { it.type == RECEIVED }.map { it.node }.toSet()

            assertTrue("${topology.scenario.name}: el origen recibió su propio paquete", topology.originId !in receivers)
            assertTrue(
                "${topology.scenario.name}: recibió fuera de su componente: ${receivers - component(topology)}",
                component(topology).containsAll(receivers),
            )
        }
    }

    @Test
    fun `in a connected graph without blocks everyone receives when ttl is at least phones minus 1`() {
        val guaranteed = topologies.filter { it.kind <= 1 }
        assertTrue(guaranteed.size >= 100)

        for (topology in guaranteed) {
            val receivers = ScenarioRunner.run(topology.scenario).filter { it.type == RECEIVED }.map { it.node }.toSet()
            val everyoneElse = topology.scenario.nodes.map { it.id }.toSet() - topology.originId

            assertEquals("${topology.scenario.name} (n = ${topology.nodeCount})", everyoneElse, receivers)
        }
    }

    @Test
    fun `the generator really produces disconnected graphs and blocked packets`() {
        // Sin esto, las pruebas de arriba podrían pasar sin ejercitar los casos difíciles.
        assertTrue(topologies.any { component(it).size < it.nodeCount })
        assertTrue(topologies.any { topology -> ScenarioRunner.run(topology.scenario).any { it.type == BLOCKED } })
    }

    @Test
    fun `running a topology twice gives the same sequence`() {
        for (topology in topologies) {
            assertEquals(topology.scenario.name, ScenarioRunner.run(topology.scenario), ScenarioRunner.run(topology.scenario))
        }
    }
}
