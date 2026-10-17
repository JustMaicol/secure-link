package com.senda.features.sendalert

import com.senda.core.protocol.EmergencyType
import com.senda.core.protocol.Vulnerable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// spec 108, sección 3.2 · Reglas del estado del formulario (canSend y el aviso bajo el botón) y de la cuenta regresiva.
class SendAlertUiStateTest {

    private val fix = LocationUi(LocationStatus.FIX, coordinates = "-33.50000, -70.65000", accuracyM = 35, ageMs = 20_000)

    private fun ready() = SendAlertFormUiState(selectedType = EmergencyType.MEDICAL, location = fix)

    @Test
    fun `with a type and a usable location it can send and nothing needs explaining`() {
        val state = ready()

        assertTrue(state.canSend)
        assertNull(state.hint)
    }

    @Test
    fun `without a type it cannot send and asks for one`() {
        val state = ready().copy(selectedType = null)

        assertFalse(state.canSend)
        assertEquals(SendHint.NEEDS_TYPE, state.hint)
    }

    @Test
    fun `only a fix or the last known location is usable`() {
        val usable = LocationStatus.entries.filter { LocationUi(it).usable }

        assertEquals(listOf(LocationStatus.FIX, LocationStatus.LAST_KNOWN), usable)
    }

    @Test
    fun `without a usable location it cannot send and says the location is missing`() {
        for (status in listOf(LocationStatus.SEARCHING, LocationStatus.DISABLED, LocationStatus.UNAVAILABLE)) {
            val state = ready().copy(location = LocationUi(status))

            assertFalse("$status", state.canSend)
            assertEquals("$status", SendHint.NEEDS_LOCATION, state.hint)
        }
    }

    @Test
    fun `a stale last known location can still be sent`() {
        val state = ready().copy(location = LocationUi(LocationStatus.LAST_KNOWN, ageMs = 40 * 60_000L, isStale = true))

        assertTrue(state.canSend)
    }

    @Test
    fun `while sending it cannot send again and says it is saving`() {
        val state = ready().copy(sending = true)

        assertFalse(state.canSend)
        assertEquals(SendHint.SENDING, state.hint)
    }

    @Test
    fun `releasing early is explained but does not block a new attempt`() {
        val state = ready().copy(releasedEarly = true)

        assertTrue(state.canSend)
        assertEquals(SendHint.RELEASED_EARLY, state.hint)
    }

    @Test
    fun `a missing type is explained before an early release`() {
        val state = ready().copy(selectedType = null, releasedEarly = true)

        assertEquals(SendHint.RELEASED_EARLY, state.hint)
    }

    @Test
    fun `it counts the flags that are marked`() {
        assertEquals(0, ready().vulnerableCount)
        assertEquals(1, ready().copy(vulnerable = Vulnerable(children = true)).vulnerableCount)
        assertEquals(4, ready().copy(vulnerable = Vulnerable(true, true, true, true)).vulnerableCount)
    }

    @Test
    fun `the countdown ring goes from full to empty`() {
        assertEquals(1f, AutoAlertCountdownUiState(remainingMs = AUTO_COUNTDOWN_MS).progress, 0f)
        assertEquals(0.5f, AutoAlertCountdownUiState(remainingMs = AUTO_COUNTDOWN_MS / 2).progress, 0.0001f)
        assertEquals(0f, AutoAlertCountdownUiState(remainingMs = 0).progress, 0f)
    }

    @Test
    fun `the countdown ring never leaves its range`() {
        assertEquals(1f, AutoAlertCountdownUiState(remainingMs = AUTO_COUNTDOWN_MS * 2).progress, 0f)
        assertEquals(0f, AutoAlertCountdownUiState(remainingMs = -5).progress, 0f)
        assertEquals(0f, AutoAlertCountdownUiState(remainingMs = 1_000, totalMs = 0).progress, 0f)
    }

    @Test
    fun `the configured times are the ones the spec fixes`() {
        assertEquals(1_500L, SEND_HOLD_MS)
        assertEquals(300_000L, LOCATION_STALE_WARN_MS)
        assertEquals(15_000L, AUTO_COUNTDOWN_MS)
    }
}
