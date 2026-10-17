package com.senda.permissions

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// spec 100 · El ViewModel de la pantalla de permisos.
class PermissionsViewModelTest {

    private val platform = FakePlatformState()
    private val history = FakePermissionRequestHistory()
    private val flow = FakePermissionFlowStore()

    private fun viewModel() =
        PermissionsViewModel(PermissionsModel(AndroidPermissionChecker(platform), platform, history, flow))

    private fun grantBlocking() {
        platform.granted = setOf(
            Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        platform.bluetoothEnabled = true
        platform.locationEnabled = true
    }

    @Test
    fun `starts with the current state and not passed on a fresh install`() {
        val vm = viewModel()

        assertFalse(vm.state.value.canContinue)
        assertFalse(vm.passed.value)
    }

    @Test
    fun `continue does nothing while a blocking requirement is missing`() {
        val vm = viewModel()

        vm.onContinue()

        assertFalse(vm.passed.value)
        assertFalse(flow.onboardingDone)
    }

    @Test
    fun `continue passes the screen and remembers it once nothing blocking is missing`() {
        grantBlocking()
        val vm = viewModel()
        vm.refresh { false }

        vm.onContinue()

        assertTrue(vm.passed.value)
        assertTrue(flow.onboardingDone)
    }

    @Test
    fun `refresh picks up permissions granted in the settings`() {
        val vm = viewModel()
        assertFalse(vm.state.value.canContinue)

        grantBlocking()
        vm.refresh { false }

        assertTrue(vm.state.value.canContinue)
    }

    @Test
    fun `a request marks the requirement as asked`() {
        val vm = viewModel()

        vm.onRequestStarted(Requirement.LOCATION)
        vm.refresh { false }

        assertTrue(history.wasRequested(Requirement.LOCATION))
        assertEquals(
            RowState.DENIED_PERMANENTLY,
            vm.state.value.rows.first { it.kind == PermissionRowKind.LOCATION }.state,
        )
    }

    @Test
    fun `acknowledging samsung hides the step`() {
        val vm = viewModel()
        assertTrue(vm.state.value.showSamsungStep)

        vm.onSamsungAcknowledged { false }

        assertFalse(vm.state.value.showSamsungStep)
    }

    @Test
    fun `an already finished onboarding starts as passed`() {
        flow.onboardingDone = true

        assertTrue(viewModel().passed.value)
    }
}
