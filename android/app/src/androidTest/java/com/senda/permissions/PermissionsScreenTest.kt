package com.senda.permissions

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.senda.R
import com.senda.ui.theme.SendaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// spec 100, criterios 7 y 12 · La pantalla de permisos dibuja cada estado de fila (PermissionsUiState a mano, sin teléfono).
@RunWith(AndroidJUnit4::class)
class PermissionsScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
    private fun text(id: Int) = resources.getString(id)

    private val allGranted = Requirement.entries.associateWith { true }

    private fun show(
        check: Map<Requirement, Boolean> = allGranted,
        states: Map<Requirement, RowState> = emptyMap(),
        samsungAcknowledged: Boolean = true,
        onboardingDone: Boolean = false,
        onRowAction: (PermissionRowUi) -> Unit = {},
        onSamsungDone: () -> Unit = {},
        onContinue: () -> Unit = {},
    ) {
        val state = buildPermissionsState(check, states, samsungAcknowledged, onboardingDone)
        rule.setContent {
            SendaTheme(darkTheme = false) {
                PermissionsScreen(
                    state = state,
                    onRowAction = onRowAction,
                    onSamsungOpenSettings = {},
                    onSamsungDone = onSamsungDone,
                    onContinue = onContinue,
                )
            }
        }
    }

    // ---------------- Estados de fila ----------------

    @Test
    fun grantedRowsShowTheirStatusAndReview() {
        show()

        rule.onNodeWithText(text(R.string.permissions_state_active)).assertExists()
        rule.onNodeWithText(text(R.string.permissions_state_granted_masculine)).assertExists()
        // "✓ Permitida" es de dos filas: Ubicación y Ubicación en segundo plano.
        rule.onAllNodesWithText(text(R.string.permissions_state_granted_feminine)).assertCountEquals(2)
        rule.onNodeWithText(text(R.string.permissions_state_granted_feminine_plural)).assertExists()
        rule.onNodeWithText(text(R.string.permissions_state_battery_ok)).assertExists()
        // 4 filas de Figma + ubicación en segundo plano + batería, todas con "Revisar".
        rule.onAllNodesWithText(text(R.string.permissions_action_review)).assertCountEquals(6)
        rule.onAllNodesWithText(text(R.string.permissions_state_pending)).assertCountEquals(0)
    }

    @Test
    fun missingRowShowsPendingAndAllow() {
        show(check = allGranted + (Requirement.NOTIFICATIONS to false), states = mapOf(Requirement.NOTIFICATIONS to RowState.MISSING))

        rule.onNodeWithText(text(R.string.permissions_state_pending)).assertExists()
        rule.onAllNodesWithText(text(R.string.permissions_action_allow)).assertCountEquals(1)
        rule.onAllNodesWithText(text(R.string.permissions_action_review)).assertCountEquals(5)
    }

    @Test
    fun permanentlyDeniedRowShowsTheDeniedMessageAndSettings() {
        show(
            check = allGranted + (Requirement.LOCATION to false),
            states = mapOf(Requirement.LOCATION to RowState.DENIED_PERMANENTLY),
        )

        rule.onNodeWithText(text(R.string.permissions_state_denied)).assertExists()
        rule.onNodeWithText(text(R.string.permissions_action_settings)).assertExists()
    }

    @Test
    fun systemLocationRowAppearsOnlyWhileTheSwitchIsOff() {
        show(check = allGranted + (Requirement.LOCATION_ON to false))

        rule.onNodeWithText(text(R.string.permissions_location_services_title)).assertExists()
        rule.onNodeWithText(text(R.string.permissions_action_activate)).assertExists()
    }

    @Test
    fun systemLocationRowIsAbsentWhenTheSwitchIsOn() {
        show()

        rule.onAllNodesWithText(text(R.string.permissions_location_services_title)).assertCountEquals(0)
    }

    @Test
    fun bluetoothOffOffersToTurnItOn() {
        show(check = allGranted + (Requirement.BLUETOOTH_ON to false))

        rule.onNodeWithText(text(R.string.permissions_action_enable)).assertExists()
    }

    @Test
    fun backgroundLocationIsLockedUntilLocationIsGranted() {
        show(check = allGranted + (Requirement.LOCATION to false) + (Requirement.BACKGROUND_LOCATION to false))

        rule.onNodeWithText(text(R.string.permissions_background_locked)).assertExists()
        // Hay dos "Permitir" (ubicación y ubicación en segundo plano): solo el de la ubicación está habilitado.
        rule.onAllNodesWithText(text(R.string.permissions_action_allow)).assertCountEquals(2)
        rule.onAllNodes(hasText(text(R.string.permissions_action_allow)) and isEnabled()).assertCountEquals(1)
    }

    // ---------------- Continuar ----------------

    @Test
    fun continueIsDisabledWithAHintWhileABlockingRequirementIsMissing() {
        show(check = allGranted + (Requirement.BLUETOOTH to false))

        rule.onNodeWithText(text(R.string.permissions_continue)).assertIsNotEnabled()
        rule.onNodeWithText(text(R.string.permissions_missing_hint)).assertExists()
        rule.onAllNodesWithText(text(R.string.permissions_continue_hint)).assertCountEquals(0)
    }

    @Test
    fun continueIsEnabledAndFiresWhenNothingBlockingIsMissing() {
        var continued = false
        show(
            check = allGranted + (Requirement.NOTIFICATIONS to false), // las notificaciones no bloquean
            onContinue = { continued = true },
        )

        rule.onNodeWithText(text(R.string.permissions_continue_hint)).assertExists()
        rule.onNodeWithText(text(R.string.permissions_continue)).assertIsEnabled().performClick()

        assertTrue(continued)
    }

    // ---------------- Acciones ----------------

    @Test
    fun tappingAnActionButtonReportsItsRow() {
        val pressed = mutableListOf<PermissionRowKind>()
        show(
            check = allGranted + (Requirement.BLUETOOTH to false),
            states = mapOf(Requirement.BLUETOOTH to RowState.MISSING),
            onRowAction = { pressed += it.kind },
        )

        rule.onNodeWithText(text(R.string.permissions_action_allow)).performScrollTo().performClick()

        assertEquals(listOf(PermissionRowKind.NEARBY), pressed)
    }

    // ---------------- Paso de Samsung y aviso ----------------

    @Test
    fun samsungStepShowsUntilAcknowledged() {
        var acknowledged = false
        show(samsungAcknowledged = false, onSamsungDone = { acknowledged = true })

        rule.onNodeWithText(text(R.string.permissions_samsung_title)).assertExists()
        rule.onNodeWithText(text(R.string.permissions_samsung_done)).performScrollTo().performClick()

        assertTrue(acknowledged)
    }

    @Test
    fun samsungStepIsHiddenOnceAcknowledged() {
        show(samsungAcknowledged = true)

        rule.onAllNodesWithText(text(R.string.permissions_samsung_title)).assertCountEquals(0)
    }

    @Test
    fun lostPermissionBannerShowsOnlyAfterTheOnboarding() {
        show(check = allGranted + (Requirement.LOCATION to false), onboardingDone = true)
        rule.onNodeWithText(text(R.string.common_lost_permission)).assertExists()
    }

    @Test
    fun noBannerOnTheFirstRun() {
        show(check = allGranted + (Requirement.LOCATION to false), onboardingDone = false)

        rule.onAllNodesWithText(text(R.string.common_lost_permission)).assertCountEquals(0)
        assertFalse(rule.onAllNodesWithText(text(R.string.permissions_title)).fetchSemanticsNodes().isEmpty())
    }
}
