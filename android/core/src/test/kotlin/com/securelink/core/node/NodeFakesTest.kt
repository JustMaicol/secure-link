package com.securelink.core.node

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// spec 107 · S0: los fakes permiten cambiar el estado desde las pantallas y las pruebas.
class NodeFakesTest {

    // --- FakeMeshStatus ---

    @Test
    fun `FakeMeshStatus starts healthy and set replaces the state`() {
        val status = FakeMeshStatus()
        assertEquals(MeshHealth.RUNNING, status.state.value.health)

        val busy = status.state.value.copy(neighbors = 4, powerMode = PowerMode.ACTIVE, lastReceivedAtMs = 1_000L)
        status.set(busy)

        assertEquals(busy, status.state.value)
    }

    // --- FakeMeshControl ---

    @Test
    fun `FakeMeshControl is enabled by default and follows setEnabled`() {
        val control = FakeMeshControl()
        assertTrue(control.enabled.value)

        control.setEnabled(false)
        assertFalse(control.enabled.value)
        control.setEnabled(true)
        assertTrue(control.enabled.value)
        assertEquals(2, control.setEnabledCalls)
    }

    // --- FakeAutoAlertController ---

    @Test
    fun `cancel moves Counting and WaitingLocation to Cancelled`() {
        val controller = FakeAutoAlertController(CountdownState.Counting(10_000))
        controller.cancel()
        assertEquals(CountdownState.Cancelled, controller.state.value)

        controller.emit(CountdownState.WaitingLocation)
        controller.cancel()
        assertEquals(CountdownState.Cancelled, controller.state.value)
    }

    @Test
    fun `cancel leaves the other states untouched`() {
        for (state in listOf(CountdownState.Idle, CountdownState.Sent, CountdownState.Failed)) {
            val controller = FakeAutoAlertController(state)
            controller.cancel()
            assertEquals(state, controller.state.value)
        }
    }

    @Test
    fun `emit publishes any state and cancel calls are counted`() {
        val controller = FakeAutoAlertController()
        assertEquals(CountdownState.Idle, controller.state.value)

        controller.emit(CountdownState.Counting(15_000))
        assertEquals(CountdownState.Counting(15_000), controller.state.value)

        controller.cancel()
        controller.cancel()
        assertEquals(2, controller.cancelCalls)
    }

    // --- NodeConfig (spec 107, sección 1) ---

    @Test
    fun `NodeConfig defaults match the spec table`() {
        val config = NodeConfig.DEFAULT

        assertEquals(120_000L, config.activeHoldMs)
        assertEquals(30_000L, config.modeMinDwellMs)
        assertEquals(60_000L, config.neighborWindowMs)
        assertEquals(15_000L, config.autoCountdownMs)
        assertEquals(3, config.autoSaveRetryMax)
        assertEquals(70_000L, config.wakeLockTimeoutMs)
        assertEquals(30_000L, config.serviceNotifMinUpdateMs)
        assertEquals(60_000L, config.alertSoundMinGapMs)
    }

    @Test
    fun `MeshHealth lists the eight states of the spec in order`() {
        assertEquals(
            listOf(
                "STARTING", "RUNNING", "STOPPED", "BLUETOOTH_OFF",
                "NO_PERMISSION", "ADVERTISE_UNSUPPORTED", "ADVERTISE_FAILED", "SCAN_FAILED",
            ),
            MeshHealth.entries.map { it.name },
        )
    }
}
