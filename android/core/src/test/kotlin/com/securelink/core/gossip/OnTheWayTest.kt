package com.securelink.core.gossip

import com.securelink.core.gossip.GossipEffect.Event
import com.securelink.core.gossip.GossipEffect.StartAnnounce
import com.securelink.core.gossip.GossipEffect.Store
import com.securelink.core.gossip.GossipEventType.ANNOUNCE
import com.securelink.core.gossip.GossipEventType.DUPLICATE
import com.securelink.core.gossip.GossipEventType.RECEIVED
import com.securelink.core.protocol.Fixtures
import com.securelink.core.protocol.Mulberry32
import com.securelink.core.protocol.ProtocolConfig
import com.securelink.core.protocol.relayTagOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

// spec 103 · Un ON_THE_WAY (y un STATUS) se propaga con las mismas reglas, y su clave no choca con la de su ALERT.
class OnTheWayTest {

    private val now = 1_760_600_000_000L
    private val seedB = 185273099

    private val alert = Fixtures.alertWire()
    private val onTheWay = Fixtures.onTheWayWire()
    private val status = Fixtures.statusWire()

    @Test
    fun `the keys of an alert, its on the way and its status are all different`() {
        val keys = listOf(alert, onTheWay, status).map { it.packet.key.canonical() }

        assertEquals(3, keys.toSet().size)
        assertNotEquals(alert.packet.key, onTheWay.packet.key)
    }

    @Test
    fun `an on the way is received and forwarded even when its alert was already seen`() {
        val engine = GossipEngine(Fixtures.DEVICE_B, Mulberry32(seedB))
        engine.onReceived(alert, now)

        val effects = engine.onReceived(onTheWay, now + 100)

        assertEquals(listOf(Store(onTheWay), Event(RECEIVED, onTheWay.packet.key, ttl = 8)), effects)
        assertNotNull("debe haber un reenvío programado para el on the way", engine.nextWakeupMs())
    }

    @Test
    fun `each packet of the same alert is forwarded exactly once`() {
        val engine = GossipEngine(Fixtures.DEVICE_B, Mulberry32(seedB), ProtocolConfig(forwardDelayMaxMs = 0))
        engine.onReceived(alert, now)
        engine.onReceived(onTheWay, now)
        engine.onReceived(status, now)
        // Segundas copias: duplicados.
        assertEquals(DUPLICATE, (engine.onReceived(onTheWay, now + 1).single() as Event).type)

        val announced = engine.tick(now).filterIsInstance<StartAnnounce>().map { it.wire.packet.key.canonical() }

        assertEquals(
            listOf("a1b2c3d4e5f6-0001", "a1b2c3d4e5f6-0001#2", "a1b2c3d4e5f6-0001/0c0c0c0c0c0c"),
            announced,
        )
        assertEquals(emptyList<GossipEffect>(), engine.tick(now + 1)) // nada más que reenviar
    }

    @Test
    fun `the forwarded on the way loses one ttl and carries the relay's tag`() {
        val engine = GossipEngine(Fixtures.DEVICE_B, Mulberry32(seedB), ProtocolConfig(forwardDelayMaxMs = 0))
        engine.onReceived(onTheWay, now)

        val announce = engine.tick(now).filterIsInstance<StartAnnounce>().single()

        assertEquals(7, announce.wire.ttl)
        assertEquals(relayTagOf(Fixtures.DEVICE_B), announce.wire.relayTag)
        assertEquals(onTheWay.packet, announce.wire.packet) // el contenido inmutable no cambia
    }

    @Test
    fun `an on the way crosses the line a-b-c with the same timing as an alert`() {
        val scenario = Scenario(
            name = "on-the-way-line",
            config = ProtocolConfig.DEFAULT,
            nodes = listOf(
                ScenarioNode("A", Fixtures.DEVICE_A, seed = 3285509622L.toInt()),
                ScenarioNode("B", Fixtures.DEVICE_B, seed = seedB),
                ScenarioNode("C", Fixtures.DEVICE_C, seed = 202116108),
            ),
            links = listOf("A" to "B", "B" to "C"),
            blockedRelayTags = emptyMap(),
            nowMs = now + 120_000, // la hora del on the way (createdAt + 120 s)
            originations = listOf(ScenarioOrigination(t = 0, node = "A", hex = Fixtures.ON_THE_WAY_HEX)),
        )

        val trace = ScenarioRunner.run(scenario)

        val key = "a1b2c3d4e5f6-0001/0c0c0c0c0c0c"
        assertEquals(
            listOf(
                ScenarioEvent(0, ANNOUNCE, "A", key, ttl = 8),
                ScenarioEvent(0, RECEIVED, "B", key, ttl = 8),
                ScenarioEvent(1220, ANNOUNCE, "B", key, ttl = 7),
                ScenarioEvent(1220, DUPLICATE, "A", key),
                ScenarioEvent(1220, RECEIVED, "C", key, ttl = 7),
                ScenarioEvent(2790, ANNOUNCE, "C", key, ttl = 6),
                ScenarioEvent(2790, DUPLICATE, "B", key),
            ),
            trace,
        )
        assertTrue(trace.all { it.key == key })
    }
}
