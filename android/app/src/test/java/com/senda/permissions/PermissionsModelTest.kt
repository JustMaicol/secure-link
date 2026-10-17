package com.senda.permissions

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// spec 100, sección 5.3 · El modelo junta permisos, historial y flujo guardado, con fakes (sin teléfono).
class PermissionsModelTest {

    private val platform = FakePlatformState()
    private val history = FakePermissionRequestHistory()
    private val flow = FakePermissionFlowStore()
    private val model = PermissionsModel(AndroidPermissionChecker(platform), platform, history, flow)

    private val nearbyPermissions = setOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.BLUETOOTH_CONNECT,
    )
    private val locationPermissions = setOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

    private fun PermissionsUiState.row(kind: PermissionRowKind) = (rows + recommended).first { it.kind == kind }

    private fun grantEverything() {
        platform.granted = nearbyPermissions + locationPermissions +
            setOf(Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        platform.bluetoothEnabled = true
        platform.locationEnabled = true
        platform.ignoringBatteryOptimizations = true
    }

    @Test
    fun `a fresh install shows everything as missing and blocks the continue button`() {
        val state = model.state(showsRationale = { false })

        assertTrue((state.rows + state.recommended).all { it.state == RowState.MISSING })
        assertFalse(state.canContinue)
        assertFalse(state.showLostPermissionBanner)
        assertTrue(state.showSamsungStep)
    }

    @Test
    fun `everything granted lets the user continue`() {
        grantEverything()

        val state = model.state(showsRationale = { false })

        assertTrue((state.rows + state.recommended).all { it.state == RowState.GRANTED })
        assertTrue(state.canContinue)
    }

    @Test
    fun `never asked is missing, even though Android reports no rationale`() {
        val state = model.state(showsRationale = { false })

        assertEquals(RowState.MISSING, state.row(PermissionRowKind.NEARBY).state)
    }

    @Test
    fun `asked before with no rationale left is denied permanently`() {
        model.markRequested(Requirement.BLUETOOTH)

        val state = model.state(showsRationale = { false })

        assertEquals(RowState.DENIED_PERMANENTLY, state.row(PermissionRowKind.NEARBY).state)
        assertEquals(PermissionAction.OPEN_APP_SETTINGS, state.row(PermissionRowKind.NEARBY).action)
        // Solo el requisito que se pidió.
        assertEquals(RowState.MISSING, state.row(PermissionRowKind.LOCATION).state)
    }

    @Test
    fun `asked before but Android still offers the dialog is missing`() {
        model.markRequested(Requirement.LOCATION)

        val state = model.state(showsRationale = { it in locationPermissions })

        assertEquals(RowState.MISSING, state.row(PermissionRowKind.LOCATION).state)
        assertEquals(PermissionAction.REQUEST, state.row(PermissionRowKind.LOCATION).action)
    }

    @Test
    fun `a permission granted after being asked is granted`() {
        model.markRequested(Requirement.BLUETOOTH)
        platform.granted = nearbyPermissions

        assertEquals(RowState.GRANTED, model.state { false }.row(PermissionRowKind.NEARBY).state)
    }

    @Test
    fun `the samsung step goes away once acknowledged and stays away`() {
        model.acknowledgeSamsung()

        assertFalse(model.state { false }.showSamsungStep)
        assertTrue(flow.samsungAcknowledged)
    }

    @Test
    fun `after the onboarding a lost permission shows the banner`() {
        grantEverything()
        model.completeOnboarding()
        assertFalse(model.state { false }.showLostPermissionBanner)

        platform.granted = platform.granted - Manifest.permission.ACCESS_FINE_LOCATION

        assertTrue(model.state { false }.showLostPermissionBanner)
    }

    @Test
    fun `the onboarding flag is exposed`() {
        assertFalse(model.onboardingDone)
        model.completeOnboarding()
        assertTrue(model.onboardingDone)
    }
}
