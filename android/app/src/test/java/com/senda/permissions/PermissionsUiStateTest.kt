package com.senda.permissions

import com.senda.permissions.PermissionAction.ENABLE_BLUETOOTH
import com.senda.permissions.PermissionAction.OPEN_APP_SETTINGS
import com.senda.permissions.PermissionAction.OPEN_BATTERY_SETTINGS
import com.senda.permissions.PermissionAction.OPEN_BLUETOOTH_SETTINGS
import com.senda.permissions.PermissionAction.OPEN_LOCATION_SETTINGS
import com.senda.permissions.PermissionAction.REQUEST
import com.senda.permissions.PermissionAction.REQUEST_BATTERY_EXCLUSION
import com.senda.permissions.PermissionRowKind.BACKGROUND_LOCATION
import com.senda.permissions.PermissionRowKind.BATTERY
import com.senda.permissions.PermissionRowKind.BLUETOOTH
import com.senda.permissions.PermissionRowKind.LOCATION
import com.senda.permissions.PermissionRowKind.LOCATION_SERVICES
import com.senda.permissions.PermissionRowKind.NEARBY
import com.senda.permissions.PermissionRowKind.NOTIFICATIONS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// spec 100, sección 12 · Qué filas, estados y acciones muestra la pantalla de permisos.
class PermissionsUiStateTest {

    private val allGranted = Requirement.entries.associateWith { true }

    private fun build(
        check: Map<Requirement, Boolean> = allGranted,
        states: Map<Requirement, RowState> = emptyMap(),
        samsungAcknowledged: Boolean = false,
        onboardingDone: Boolean = false,
    ) = buildPermissionsState(check, states, samsungAcknowledged, onboardingDone)

    private fun PermissionsUiState.row(kind: PermissionRowKind) = (rows + recommended).first { it.kind == kind }

    // ---------------- Filas ----------------

    @Test
    fun `the four rows of the Figma design come first, in its order`() {
        val state = build()

        assertEquals(listOf(BLUETOOTH, NEARBY, LOCATION, NOTIFICATIONS), state.rows.map { it.kind })
        assertTrue(state.rows.all { it.kind.inFigma })
    }

    @Test
    fun `the recommended section has background location and battery, and neither is in Figma`() {
        val state = build()

        assertEquals(listOf(BACKGROUND_LOCATION, BATTERY), state.recommended.map { it.kind })
        assertTrue(state.recommended.none { it.kind.inFigma })
    }

    @Test
    fun `the system location row appears right after location only while the switch is off`() {
        val off = build(check = allGranted + (Requirement.LOCATION_ON to false))
        assertEquals(listOf(BLUETOOTH, NEARBY, LOCATION, LOCATION_SERVICES, NOTIFICATIONS), off.rows.map { it.kind })
        assertFalse(LOCATION_SERVICES.inFigma)

        val on = build()
        assertFalse(on.rows.any { it.kind == LOCATION_SERVICES })
    }

    @Test
    fun `every row maps to its own requirement`() {
        assertEquals(Requirement.BLUETOOTH_ON, BLUETOOTH.requirement)
        assertEquals(Requirement.BLUETOOTH, NEARBY.requirement)
        assertEquals(Requirement.LOCATION, LOCATION.requirement)
        assertEquals(Requirement.LOCATION_ON, LOCATION_SERVICES.requirement)
        assertEquals(Requirement.NOTIFICATIONS, NOTIFICATIONS.requirement)
        assertEquals(Requirement.BACKGROUND_LOCATION, BACKGROUND_LOCATION.requirement)
        assertEquals(Requirement.BATTERY_UNRESTRICTED, BATTERY.requirement)
        assertEquals(PermissionRowKind.entries.map { it.requirement }.toSet(), Requirement.entries.toSet())
    }

    // ---------------- Estados ----------------

    @Test
    fun `state comes from rowStates and falls back to the check`() {
        val state = build(
            check = allGranted + (Requirement.NOTIFICATIONS to false) + (Requirement.LOCATION to false),
            states = mapOf(Requirement.LOCATION to RowState.DENIED_PERMANENTLY),
        )

        assertEquals(RowState.DENIED_PERMANENTLY, state.row(LOCATION).state)
        assertEquals(RowState.MISSING, state.row(NOTIFICATIONS).state) // sin dato en rowStates: lo da el check
        assertEquals(RowState.GRANTED, state.row(NEARBY).state)
    }

    // ---------------- Continuar ----------------

    @Test
    fun `can continue when nothing blocking is missing, even without the recommended ones`() {
        val onlyRecommendedMissing = allGranted +
            (Requirement.NOTIFICATIONS to false) + (Requirement.BACKGROUND_LOCATION to false) +
            (Requirement.BATTERY_UNRESTRICTED to false)

        assertTrue(build(check = onlyRecommendedMissing).canContinue)
    }

    @Test
    fun `cannot continue while a blocking requirement is missing`() {
        for (requirement in listOf(Requirement.BLUETOOTH, Requirement.LOCATION, Requirement.BLUETOOTH_ON, Requirement.LOCATION_ON)) {
            assertFalse("$requirement", build(check = allGranted + (requirement to false)).canContinue)
        }
    }

    // ---------------- Acciones ----------------

    @Test
    fun `a missing runtime permission is requested, a granted or denied one opens the app settings`() {
        for (kind in listOf(NEARBY, LOCATION, NOTIFICATIONS)) {
            val requirement = kind.requirement
            val missing = build(check = allGranted + (requirement to false), states = mapOf(requirement to RowState.MISSING))
            assertEquals("$kind pendiente", REQUEST, missing.row(kind).action)

            val denied = build(check = allGranted + (requirement to false), states = mapOf(requirement to RowState.DENIED_PERMANENTLY))
            assertEquals("$kind negado", OPEN_APP_SETTINGS, denied.row(kind).action)

            assertEquals("$kind concedido", OPEN_APP_SETTINGS, build().row(kind).action)
        }
    }

    @Test
    fun `bluetooth off is turned on from the app only when the bluetooth permission is granted`() {
        val withPermission = build(check = allGranted + (Requirement.BLUETOOTH_ON to false))
        assertEquals(ENABLE_BLUETOOTH, withPermission.row(BLUETOOTH).action)

        val withoutPermission = build(check = allGranted + (Requirement.BLUETOOTH_ON to false) + (Requirement.BLUETOOTH to false))
        assertEquals(OPEN_BLUETOOTH_SETTINGS, withoutPermission.row(BLUETOOTH).action)

        assertEquals(OPEN_BLUETOOTH_SETTINGS, build().row(BLUETOOTH).action) // activo: "Revisar" abre los ajustes
    }

    @Test
    fun `location off and battery have their own actions`() {
        val off = build(check = allGranted + (Requirement.LOCATION_ON to false))
        assertEquals(OPEN_LOCATION_SETTINGS, off.row(LOCATION_SERVICES).action)

        val batteryMissing = build(check = allGranted + (Requirement.BATTERY_UNRESTRICTED to false))
        assertEquals(REQUEST_BATTERY_EXCLUSION, batteryMissing.row(BATTERY).action)
        assertEquals(OPEN_BATTERY_SETTINGS, build().row(BATTERY).action)
    }

    @Test
    fun `background location can only be requested once location is granted`() {
        val withoutLocation = build(
            check = allGranted + (Requirement.LOCATION to false) + (Requirement.BACKGROUND_LOCATION to false),
        )
        assertFalse(withoutLocation.row(BACKGROUND_LOCATION).actionEnabled)

        val withLocation = build(check = allGranted + (Requirement.BACKGROUND_LOCATION to false))
        assertTrue(withLocation.row(BACKGROUND_LOCATION).actionEnabled)
        assertEquals(REQUEST, withLocation.row(BACKGROUND_LOCATION).action)
    }

    @Test
    fun `every other row has its action enabled`() {
        val state = build(check = Requirement.entries.associateWith { false })

        assertTrue((state.rows + state.recommended).filter { it.kind != BACKGROUND_LOCATION }.all { it.actionEnabled })
    }

    // ---------------- Paso de Samsung y banner ----------------

    @Test
    fun `the samsung step shows until it is acknowledged`() {
        assertTrue(build(samsungAcknowledged = false).showSamsungStep)
        assertFalse(build(samsungAcknowledged = true).showSamsungStep)
    }

    @Test
    fun `the lost permission banner needs a finished onboarding and a missing blocking requirement`() {
        val lost = allGranted + (Requirement.BLUETOOTH to false)

        assertTrue(build(check = lost, onboardingDone = true).showLostPermissionBanner)
        assertFalse(build(check = lost, onboardingDone = false).showLostPermissionBanner) // primera vez: no se perdió nada
        assertFalse(build(check = allGranted, onboardingDone = true).showLostPermissionBanner)
        assertFalse(
            build(check = allGranted + (Requirement.NOTIFICATIONS to false), onboardingDone = true).showLostPermissionBanner,
        )
    }
}
