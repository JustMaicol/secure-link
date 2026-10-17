package com.securelink.core.gossip

import com.securelink.core.gossip.GossipEffect.Event
import com.securelink.core.gossip.GossipEffect.StartAnnounce
import com.securelink.core.gossip.GossipEffect.StopAnnounce
import com.securelink.core.gossip.GossipEffect.Store
import com.securelink.core.gossip.GossipEventType.ANNOUNCE
import com.securelink.core.gossip.GossipEventType.BLOCKED
import com.securelink.core.gossip.GossipEventType.DUPLICATE
import com.securelink.core.gossip.GossipEventType.NO_FORWARD_EXPIRED
import com.securelink.core.gossip.GossipEventType.NO_FORWARD_TTL
import com.securelink.core.gossip.GossipEventType.RECEIVED
import com.securelink.core.protocol.AlertKey
import com.securelink.core.protocol.Fixtures
import com.securelink.core.protocol.Mulberry32
import com.securelink.core.protocol.ProtocolConfig
import com.securelink.core.protocol.WirePacket
import com.securelink.core.protocol.relayTagOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// spec 103, sección 4 · Cada paso por separado, con tiempos fijos (sin reloj del sistema).
class EngineRulesTest {

    private val now = 1_760_600_000_000L // = createdAt de la alerta de referencia
    private val seedB = 185273099 // primer valor: 2943612290, que mod 2001 es 1220

    private fun alert(counter: Int = 1, ttl: Int = 8, relayTag: Int = 0xE5F6, createdAtMs: Long = now): WirePacket {
        val base = Fixtures.alert()
        return WirePacket(base.copy(counter = counter, createdAtMs = createdAtMs), ttl, relayTag)
    }

    private fun engine(
        config: ProtocolConfig = ProtocolConfig.DEFAULT,
        initiallySeen: List<AlertKey> = emptyList(),
        blocked: Set<Int> = emptySet(),
        rng: Mulberry32 = Mulberry32(seedB),
    ) = GossipEngine(Fixtures.DEVICE_B, rng, config, initiallySeen, blocked)

    private val key = Fixtures.alert().key

    // ---------------- 4.1 originate ----------------

    @Test
    fun `originate announces without delay for the origin window and does not store`() {
        val wire = alert()
        val effects = engine().originate(wire, now)

        assertEquals(
            listOf(
                StartAnnounce(wire, now + 60_000),
                Event(ANNOUNCE, key, ttl = 8),
            ),
            effects,
        )
    }

    @Test
    fun `originate twice gives DUPLICATE the second time and opens no second window`() {
        val engine = engine()
        engine.originate(alert(), now)

        assertEquals(listOf(Event(DUPLICATE, key)), engine.originate(alert(), now + 10))
        assertEquals(1, engine.active().size)
        assertEquals(now + 60_000, engine.nextWakeupMs())
    }

    @Test
    fun `originate does not consume the generator`() {
        val rng = Mulberry32(seedB)
        engine(rng = rng).originate(alert(), now)

        assertEquals(Mulberry32(seedB).next(), rng.next())
    }

    // ---------------- 4.2 onReceived, paso por paso ----------------

    @Test
    fun `step 1 - a blocked relayTag is dropped and the key is not marked as seen`() {
        val engine = engine(blocked = setOf(0xE5F6))

        assertEquals(listOf(Event(BLOCKED, key, relayTag = 0xE5F6)), engine.onReceived(alert(relayTag = 0xE5F6), now))
        // Criterio 3: el mismo paquete por otro relay se recibe.
        assertEquals(
            listOf(Store(alert(relayTag = 0x1111)), Event(RECEIVED, key, ttl = 8)),
            engine.onReceived(alert(relayTag = 0x1111), now + 5),
        )
    }

    @Test
    fun `step 2 - a seen key is a duplicate and carries the relayTag`() {
        val engine = engine()
        engine.onReceived(alert(relayTag = 0xE5F6), now)

        assertEquals(
            listOf(Event(DUPLICATE, key, relayTag = 0x0C0C)),
            engine.onReceived(alert(ttl = 3, relayTag = 0x0C0C), now + 1),
        )
    }

    @Test
    fun `step 2 - a key in initiallySeen is a duplicate and is never forwarded (criterion 4)`() {
        val engine = engine(initiallySeen = listOf(key))

        assertEquals(listOf(Event(DUPLICATE, key, relayTag = 0xE5F6)), engine.onReceived(alert(), now))
        assertNull(engine.nextWakeupMs())
        assertEquals(emptyList<WirePacket>(), engine.active())
    }

    @Test
    fun `step 3 - the first copy is stored before the RECEIVED event`() {
        val wire = alert()

        assertEquals(
            listOf(Store(wire), Event(RECEIVED, key, ttl = 8)),
            engine().onReceived(wire, now).take(2),
        )
    }

    @Test
    fun `step 4 - ttl of 1 or less is not forwarded and the generator is untouched`() {
        for (ttl in listOf(1, 0)) {
            val rng = Mulberry32(seedB)
            val engine = engine(rng = rng)

            assertEquals(
                listOf(Store(alert(ttl = ttl)), Event(RECEIVED, key, ttl = ttl), Event(NO_FORWARD_TTL, key)),
                engine.onReceived(alert(ttl = ttl), now),
            )
            assertNull(engine.nextWakeupMs())
            assertEquals(Mulberry32(seedB).next(), rng.next())
        }
    }

    @Test
    fun `step 5 - an expired packet is stored but not forwarded`() {
        val old = alert(createdAtMs = now - 86_400_000L - 1000L) // 24 h + 1 s

        val engine = engine()
        assertEquals(
            listOf(Store(old), Event(RECEIVED, key, ttl = 8), Event(NO_FORWARD_EXPIRED, key)),
            engine.onReceived(old, now),
        )
        assertNull(engine.nextWakeupMs())
    }

    @Test
    fun `step 5 - exactly 24 h old still forwards and a future time is not rejected`() {
        val exactly = engine()
        exactly.onReceived(alert(createdAtMs = now - 86_400_000L), now)
        assertEquals(now + 1220, exactly.nextWakeupMs())

        val future = engine()
        val effects = future.onReceived(alert(createdAtMs = now + 3_600_000L), now)
        assertEquals(listOf<Class<*>>(Store::class.java, Event::class.java), effects.map { it::class.java })
        assertEquals(now + 1220, future.nextWakeupMs())
    }

    @Test
    fun `step 6 - the forward is scheduled with the generator delay, ttl minus 1 and the own relayTag`() {
        val engine = engine()
        val effects = engine.onReceived(alert(), now)

        // El paso 6 no emite efectos: solo Store y RECEIVED.
        assertEquals(2, effects.size)
        // 2943612290 mod 2001 = 1220 (001, sección 10).
        assertEquals(now + 1220, engine.nextWakeupMs())

        assertEquals(emptyList<GossipEffect>(), engine.tick(now + 1219))
        val announce = engine.tick(now + 1220)
        val forwarded = alert(ttl = 7, relayTag = relayTagOf(Fixtures.DEVICE_B))
        assertEquals(
            listOf(StartAnnounce(forwarded, now + 1220 + 60_000), Event(ANNOUNCE, key, ttl = 7)),
            announce,
        )
    }

    @Test
    fun `the generator is consumed only at step 6`() {
        // Motor 1: recibe antes tres paquetes que NO llegan al paso 6.
        val noisy = engine(blocked = setOf(0x2222))
        noisy.onReceived(alert(relayTag = 0x2222), now) // BLOCKED
        noisy.onReceived(alert(counter = 2, ttl = 1), now) // NO_FORWARD_TTL
        noisy.onReceived(alert(counter = 3, createdAtMs = 0), now) // NO_FORWARD_EXPIRED
        noisy.onReceived(alert(counter = 3, createdAtMs = 0), now) // DUPLICATE
        noisy.onReceived(alert(counter = 4), now) // paso 6: el primer valor del generador

        // Motor 2: solo recibe el paquete del paso 6.
        val clean = engine()
        clean.onReceived(alert(counter = 4), now)

        // Si los anteriores hubieran consumido valores, los retardos (y los despertares) serían distintos.
        assertEquals(clean.nextWakeupMs(), noisy.nextWakeupMs())
        assertEquals(now + 1220, noisy.nextWakeupMs())
    }

    // ---------------- 4.3 tick ----------------

    @Test
    fun `tick closes the origin window at its end and not before`() {
        val engine = engine()
        engine.originate(alert(), now)

        assertEquals(emptyList<GossipEffect>(), engine.tick(now + 59_999))
        assertEquals(listOf(StopAnnounce(key)), engine.tick(now + 60_000))
        assertEquals(emptyList<WirePacket>(), engine.active())
        assertNull(engine.nextWakeupMs())
    }

    @Test
    fun `tick called late starts the announcement at the received now (criterion 6)`() {
        val engine = engine()
        engine.onReceived(alert(), now) // vence en now + 1220

        val late = now + 5_000
        val effects = engine.tick(late)

        assertEquals(
            listOf(
                StartAnnounce(alert(ttl = 7, relayTag = relayTagOf(Fixtures.DEVICE_B)), late + 60_000),
                Event(ANNOUNCE, key, ttl = 7),
            ),
            effects,
        )
    }

    @Test
    fun `tick closes windows before it starts forwards`() {
        val engine = engine()
        engine.originate(alert(counter = 1), now)
        engine.onReceived(alert(counter = 2), now)

        val effects = engine.tick(now + 60_000) // cierra la ventana de counter 1 y inicia el reenvío de counter 2

        assertEquals(
            listOf(
                StopAnnounce(AlertKey(Fixtures.DEVICE_A, 1)),
                StartAnnounce(alert(counter = 2, ttl = 7, relayTag = relayTagOf(Fixtures.DEVICE_B)), now + 120_000),
                Event(ANNOUNCE, AlertKey(Fixtures.DEVICE_A, 2), ttl = 7),
            ),
            effects,
        )
    }

    @Test
    fun `due forwards start ordered by due time and then by key`() {
        // forwardDelayMaxMs = 0 → todos vencen en `now`: el desempate es la clave, no el orden de llegada.
        val engine = engine(config = ProtocolConfig(forwardDelayMaxMs = 0))
        engine.onReceived(alert(counter = 2), now)
        engine.onReceived(alert(counter = 1), now)

        val announced = engine.tick(now).filterIsInstance<Event>().map { it.key }

        assertEquals(listOf(AlertKey(Fixtures.DEVICE_A, 1), AlertKey(Fixtures.DEVICE_A, 2)), announced)
    }

    // ---------------- 4.4 nextWakeupMs y active ----------------

    @Test
    fun `nextWakeupMs is the minimum of pending forwards and window closes`() {
        val engine = engine()
        assertNull(engine.nextWakeupMs())

        engine.originate(alert(counter = 1), now) // cierra en now + 60000
        assertEquals(now + 60_000, engine.nextWakeupMs())

        engine.onReceived(alert(counter = 2), now) // reenvío en now + 1220
        assertEquals(now + 1220, engine.nextWakeupMs())

        engine.tick(now + 1220) // abre otra ventana hasta now + 61220
        assertEquals(now + 60_000, engine.nextWakeupMs())
    }

    @Test
    fun `active lists the open announcements ordered by key`() {
        val engine = engine()
        engine.originate(alert(counter = 2), now)
        engine.originate(alert(counter = 1), now)

        assertEquals(listOf(alert(counter = 1), alert(counter = 2)), engine.active())
    }

    // ---------------- setBlockedRelayTags ----------------

    @Test
    fun `setBlockedRelayTags changes the filter while running`() {
        val engine = engine()
        engine.setBlockedRelayTags(setOf(0xE5F6))
        assertEquals(listOf(Event(BLOCKED, key, relayTag = 0xE5F6)), engine.onReceived(alert(), now))

        engine.setBlockedRelayTags(emptySet())
        assertEquals(RECEIVED, (engine.onReceived(alert(), now + 1)[1] as Event).type)
    }

    // ---------------- Criterio 5: determinismo ----------------

    @Test
    fun `two engines with the same seed and inputs produce the same effects`() {
        fun run(): List<GossipEffect> {
            val e = GossipEngine(Fixtures.DEVICE_B, Mulberry32(seedB))
            val out = mutableListOf<GossipEffect>()
            out += e.onReceived(alert(counter = 1), now)
            out += e.onReceived(alert(counter = 2, ttl = 5), now + 10)
            out += e.originate(alert(counter = 3), now + 20)
            out += e.tick(now + 3_000)
            out += e.tick(now + 200_000)
            return out
        }

        assertEquals(run(), run())
    }
}
