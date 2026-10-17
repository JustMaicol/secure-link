package com.senda.navigation

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.senda.R
import com.senda.core.node.FakeMeshStatus
import com.senda.permissions.AndroidPermissionChecker
import com.senda.permissions.FakePermissionFlowStore
import com.senda.permissions.FakePermissionRequestHistory
import com.senda.permissions.FakePlatformState
import com.senda.permissions.PermissionsModel
import com.senda.permissions.PermissionsViewModel
import com.senda.ui.theme.SendaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// spec 100, criterio 7 · Con permisos falsos, la raíz muestra la pantalla de permisos si falta un requisito
// bloqueante y la navegación si no falta ninguno.
@RunWith(AndroidJUnit4::class)
class SendaRootTest {

    @get:Rule
    val rule = createComposeRule()

    private val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
    private fun text(id: Int) = resources.getString(id)

    private val everything = setOf(
        Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.ACCESS_BACKGROUND_LOCATION,
    )

    private fun platform(granted: Set<String> = everything) = FakePlatformState(
        granted = granted,
        bluetoothEnabled = true,
        locationEnabled = true,
        ignoringBatteryOptimizations = true,
    )

    private fun show(platform: FakePlatformState, flow: FakePermissionFlowStore = FakePermissionFlowStore(onboardingDone = true)) {
        val model = PermissionsModel(AndroidPermissionChecker(platform), platform, FakePermissionRequestHistory(), flow)
        val viewModel = PermissionsViewModel(model)
        val meshState = FakeMeshStatus().state.value
        rule.setContent {
            SendaTheme(darkTheme = false) {
                SendaRoot(
                    permissions = viewModel,
                    meshState = meshState,
                    showsRationale = { false },
                )
            }
        }
    }

    @Test
    fun showsThePermissionsScreenWhenABlockingRequirementIsMissing() {
        show(platform(granted = everything - Manifest.permission.ACCESS_FINE_LOCATION - Manifest.permission.ACCESS_COARSE_LOCATION))

        rule.onNodeWithText(text(R.string.permissions_title)).assertIsDisplayed()
        rule.onAllNodesWithText(text(R.string.sendalert_title)).assertCountEquals(0)
    }

    @Test
    fun showsTheNavigationWhenNothingBlockingIsMissingAndTheOnboardingIsDone() {
        show(platform())

        rule.onNodeWithText(text(R.string.sendalert_title)).assertIsDisplayed()
        rule.onAllNodesWithText(text(R.string.permissions_title)).assertCountEquals(0)
    }

    @Test
    fun notificationsAreNotBlocking() {
        show(platform(granted = everything - Manifest.permission.POST_NOTIFICATIONS))

        rule.onNodeWithText(text(R.string.sendalert_title)).assertIsDisplayed()
    }

    @Test
    fun theFirstRunShowsThePermissionsScreenEvenWithEverythingGrantedUntilContinue() {
        show(platform(), FakePermissionFlowStore(onboardingDone = false))

        rule.onNodeWithText(text(R.string.permissions_title)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.permissions_continue)).performClick()

        rule.onNodeWithText(text(R.string.sendalert_title)).assertIsDisplayed()
        rule.onAllNodesWithText(text(R.string.permissions_title)).assertCountEquals(0)
    }

    @Test
    fun aLostPermissionBringsTheScreenBackWithTheBanner() {
        show(platform(granted = everything - Manifest.permission.BLUETOOTH_SCAN))

        rule.onNodeWithText(text(R.string.permissions_title)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.common_lost_permission)).assertIsDisplayed()
    }
}
