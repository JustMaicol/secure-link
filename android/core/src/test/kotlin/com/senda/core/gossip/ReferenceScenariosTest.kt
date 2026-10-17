package com.senda.core.gossip

import com.senda.core.protocol.Fixtures
import com.senda.core.protocol.ProtocolConfig
import com.senda.core.gossip.GossipEventType.ANNOUNCE
import com.senda.core.gossip.GossipEventType.BLOCKED
import com.senda.core.gossip.GossipEventType.DUPLICATE
import com.senda.core.gossip.GossipEventType.RECEIVED
import org.junit.Assert.assertEquals
import org.junit.Test

// spec 103 · Criterio 1: los dos escenarios de la 001, sección 10, escritos en el código
// (no dependen de los archivos de vectores).
class ReferenceScenariosTest {

    private val key = "a1b2c3d4e5f6-0001"

    private val nodes = listOf(
        ScenarioNode("A", Fixtures.DEVICE_A, seed = 3285509622L.toInt()), // 0xC3D4E5F6
        ScenarioNode("B", Fixtures.DEVICE_B, seed = 185273099),
        ScenarioNode("C", Fixtures.DEVICE_C, seed = 202116108),
    )

    private fun scenario(
        name: String,
        links: List<Pair<String, String>>,
        blocked: Map<String, Set<Int>> = emptyMap(),
    ) = Scenario(
        name = name,
        config = ProtocolConfig.DEFAULT,
        nodes = nodes,
        links = links,
        blockedRelayTags = blocked,
        nowMs = 1_760_600_000_000L,
        originations = listOf(ScenarioOrigination(t = 0, node = "A", hex = Fixtures.ALERT_HEX)),
    )

    private fun event(t: Long, type: GossipEventType, node: String, ttl: Int? = null, relayTag: Int? = null) =
        ScenarioEvent(t, type, node, key, ttl, relayTag)

    @Test
    fun `line-a-b-c gives exactly the table of the 001`() {
        val trace = ScenarioRunner.run(scenario("line-a-b-c", links = listOf("A" to "B", "B" to "C")))

        assertEquals(
            listOf(
                event(0, ANNOUNCE, "A", ttl = 8),
                event(0, RECEIVED, "B", ttl = 8),
                event(1220, ANNOUNCE, "B", ttl = 7),
                event(1220, DUPLICATE, "A"),
                event(1220, RECEIVED, "C", ttl = 7),
                event(2790, ANNOUNCE, "C", ttl = 6),
                event(2790, DUPLICATE, "B"),
            ),
            trace,
        )
    }

    @Test
    fun `line-a-b-c delays are 1220 ms for B and 1570 ms for C`() {
        val trace = ScenarioRunner.run(scenario("line-a-b-c", links = listOf("A" to "B", "B" to "C")))

        val announceB = trace.first { it.type == ANNOUNCE && it.node == "B" }.t
        val announceC = trace.first { it.type == ANNOUNCE && it.node == "C" }.t
        val receivedC = trace.first { it.type == RECEIVED && it.node == "C" }.t

        assertEquals(1220L, announceB) // 2943612290 mod 2001
        assertEquals(1570L, announceC - receivedC) // 1474678540 mod 2001
    }

    @Test
    fun `triangle-c-blocks-a gives exactly the table of the 001`() {
        val trace = ScenarioRunner.run(
            scenario(
                "triangle-c-blocks-a",
                links = listOf("A" to "B", "B" to "C", "A" to "C"),
                blocked = mapOf("C" to setOf(0xE5F6)), // C bloquea el relayTag de A (58870)
            ),
        )

        assertEquals(
            listOf(
                event(0, ANNOUNCE, "A", ttl = 8),
                event(0, RECEIVED, "B", ttl = 8),
                event(0, BLOCKED, "C", relayTag = 58870),
                event(1220, ANNOUNCE, "B", ttl = 7),
                event(1220, DUPLICATE, "A"),
                event(1220, RECEIVED, "C", ttl = 7),
                event(2790, ANNOUNCE, "C", ttl = 6),
                event(2790, DUPLICATE, "A"),
                event(2790, DUPLICATE, "B"),
            ),
            trace,
        )
    }

    @Test
    fun `a blocked packet is received later through another relay`() {
        // Criterio 3, a nivel de escenario: C descartó a A, pero recibe la misma clave por B.
        val trace = ScenarioRunner.run(
            scenario("triangle", links = listOf("A" to "B", "B" to "C", "A" to "C"), blocked = mapOf("C" to setOf(0xE5F6))),
        )

        assertEquals(1, trace.count { it.type == BLOCKED && it.node == "C" })
        assertEquals(1, trace.count { it.type == RECEIVED && it.node == "C" })
    }

    @Test
    fun `the same scenario run twice gives the same trace`() {
        val scenario = scenario("line-a-b-c", links = listOf("A" to "B", "B" to "C"))

        assertEquals(ScenarioRunner.run(scenario), ScenarioRunner.run(scenario))
    }
}
