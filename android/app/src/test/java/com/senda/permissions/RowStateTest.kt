package com.senda.permissions

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// spec 100, secciones 5.3 y 12 · GRANTED, MISSING y DENIED_PERMANENTLY.
class RowStateTest {

    private val scan = Manifest.permission.BLUETOOTH_SCAN
    private val advertise = Manifest.permission.BLUETOOTH_ADVERTISE
    private val connect = Manifest.permission.BLUETOOTH_CONNECT

    private fun state(
        requirement: Requirement = Requirement.BLUETOOTH,
        granted: Boolean = false,
        requestedBefore: Boolean = false,
        grantedPermissions: Set<String> = emptySet(),
        rationale: Set<String> = emptySet(),
    ) = rowStateOf(requirement, granted, requestedBefore, { it in grantedPermissions }, { it in rationale })

    @Test
    fun `granted wins over everything else`() {
        assertEquals(RowState.GRANTED, state(granted = true))
        assertEquals(RowState.GRANTED, state(granted = true, requestedBefore = true))
    }

    @Test
    fun `never asked and no rationale is missing, not denied`() {
        // Antes de pedir por primera vez Android también dice rationale = falso: sin historial no es "negado".
        assertEquals(RowState.MISSING, state(requestedBefore = false))
    }

    @Test
    fun `asked before and Android still offers to ask again is missing`() {
        assertEquals(RowState.MISSING, state(requestedBefore = true, rationale = setOf(scan, advertise, connect)))
    }

    @Test
    fun `asked before and Android no longer offers to ask is denied permanently`() {
        assertEquals(RowState.DENIED_PERMANENTLY, state(requestedBefore = true, rationale = emptySet()))
    }

    @Test
    fun `a group is denied permanently only when none of what is missing can be asked again`() {
        // Falta scan (sin rationale) y advertise (con rationale): todavía se puede pedir advertise.
        val stillAskable = state(requestedBefore = true, grantedPermissions = setOf(connect), rationale = setOf(advertise))
        assertEquals(RowState.MISSING, stillAskable)

        // Lo concedido no cuenta: solo falta scan y ya no se puede pedir.
        val onlyScanLeft = state(requestedBefore = true, grantedPermissions = setOf(advertise, connect), rationale = emptySet())
        assertEquals(RowState.DENIED_PERMANENTLY, onlyScanLeft)
    }

    @Test
    fun `system switches and battery are never denied`() {
        for (requirement in listOf(Requirement.BLUETOOTH_ON, Requirement.LOCATION_ON, Requirement.BATTERY_UNRESTRICTED)) {
            assertEquals(requirement.name, RowState.MISSING, state(requirement, requestedBefore = true))
        }
    }

    @Test
    fun `history starts empty, remembers by requirement and keeps them apart`() {
        val history = FakePermissionRequestHistory()
        assertFalse(history.wasRequested(Requirement.BLUETOOTH))

        history.markRequested(Requirement.BLUETOOTH)

        assertTrue(history.wasRequested(Requirement.BLUETOOTH))
        assertFalse(history.wasRequested(Requirement.LOCATION))
        assertTrue(FakePermissionRequestHistory(setOf(Requirement.LOCATION)).wasRequested(Requirement.LOCATION))
    }
}
