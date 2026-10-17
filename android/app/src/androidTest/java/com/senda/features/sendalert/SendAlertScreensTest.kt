package com.senda.features.sendalert

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.down
import androidx.compose.ui.test.up
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.senda.R
import com.senda.core.protocol.EmergencyType
import com.senda.core.protocol.Vulnerable
import com.senda.ui.format.ConnectivityText
import com.senda.ui.theme.SendaTheme
import com.senda.ui.theme.UiTone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// spec 108, criterios 4 a 6 · Las pantallas E1, E2 y E3 dibujan cada estado de su UiState (a mano, sin teléfono ni
// ViewModel) y avisan de cada acción.
@RunWith(AndroidJUnit4::class)
class SendAlertScreensTest {

    @get:Rule
    val rule = createComposeRule()

    private val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
    private fun text(id: Int) = resources.getString(id)

    private val connectivity = ConnectivityText("Malla activa", UiTone.SUCCESS, "Sin internet")
    private val fix = LocationUi(LocationStatus.FIX, coordinates = "-33.50000, -70.65000", accuracyM = 35, ageMs = 20_000)

    // ---------------- E1 · Formulario ----------------

    private fun showForm(
        state: SendAlertFormUiState,
        onSelectType: (EmergencyType) -> Unit = {},
        onDecrement: () -> Unit = {},
        onIncrement: () -> Unit = {},
        onVulnerable: (Vulnerable) -> Unit = {},
        onUpdate: () -> Unit = {},
        onEnable: () -> Unit = {},
        onHoldStart: () -> Unit = {},
        onHoldEnd: () -> Unit = {},
    ) {
        rule.setContent {
            SendaTheme(darkTheme = false) {
                SendAlertFormScreen(
                    state = state, connectivity = connectivity, onSelectType = onSelectType,
                    onDecrementPeople = onDecrement, onIncrementPeople = onIncrement, onVulnerableChange = onVulnerable,
                    onUpdateLocation = onUpdate, onEnableLocation = onEnable, onHoldStart = onHoldStart, onHoldEnd = onHoldEnd,
                )
            }
        }
    }

    private val ready = SendAlertFormUiState(selectedType = EmergencyType.MEDICAL, people = 3, location = fix)

    @Test
    fun formShowsTheThreeLevelsAndReportsTheOneTapped() {
        val chosen = mutableListOf<EmergencyType>()
        showForm(ready.copy(selectedType = null), onSelectType = { chosen += it })

        rule.onNodeWithText(text(R.string.emergency_option_yellow)).assertExists()
        rule.onNodeWithText(text(R.string.emergency_option_orange)).assertExists()
        rule.onNodeWithText(text(R.string.emergency_option_red)).performClick()

        assertEquals(listOf(EmergencyType.TRAPPED_OR_SEVERE), chosen)
    }

    @Test
    fun formStepperReportsBothDirectionsAndStopsAtTheOne() {
        var minus = 0
        var plus = 0
        showForm(ready.copy(people = 1), onDecrement = { minus++ }, onIncrement = { plus++ })

        rule.onNodeWithContentDescription(text(R.string.common_decrease)).assertIsNotEnabled()
        rule.onNodeWithContentDescription(text(R.string.common_increase)).performClick()

        assertEquals(0, minus)
        assertEquals(1, plus)
    }

    @Test
    fun formStepperStopsAtTheCap() {
        showForm(ready.copy(people = MAX_PEOPLE))

        rule.onNodeWithContentDescription(text(R.string.common_increase)).assertIsNotEnabled()
        rule.onNodeWithContentDescription(text(R.string.common_decrease)).assertIsEnabled()
    }

    @Test
    fun formSummarizesTheVulnerablePeople() {
        showForm(ready.copy(vulnerable = Vulnerable(children = true)))

        rule.onNodeWithText("1 de las 3 personas").assertExists()
    }

    @Test
    fun formInvitesToSaySoWhenNoOneIsMarked() {
        showForm(ready)

        rule.onNodeWithText(text(R.string.sendalert_vulnerable_hint)).assertExists()
    }

    @Test
    fun theVulnerableRowOpensTheSheetAndEachFlagIsReported() {
        val changes = mutableListOf<Vulnerable>()
        showForm(ready, onVulnerable = { changes += it })

        rule.onNodeWithText(text(R.string.sendalert_vulnerable_title)).performScrollTo().performClick()
        rule.onNodeWithText(text(R.string.sendalert_vulnerable_sheet_title)).assertExists()
        rule.onNodeWithText(text(R.string.sendalert_vulnerable_elderly)).performClick()

        assertEquals(listOf(Vulnerable(elderly = true)), changes)
    }

    @Test
    fun doneClosesTheSheet() {
        showForm(ready)

        rule.onNodeWithText(text(R.string.sendalert_vulnerable_title)).performScrollTo().performClick()
        rule.onNodeWithText(text(R.string.sendalert_vulnerable_done)).performClick()

        rule.onAllNodesWithText(text(R.string.sendalert_vulnerable_sheet_title)).assertCountEquals(0)
    }

    @Test
    fun aFixShowsCoordinatesPrecisionAndAge() {
        showForm(ready)

        rule.onNodeWithText("-33.50000, -70.65000").assertExists()
        rule.onNodeWithText("Precisión ±35 m · actualizada hace 20 s").assertExists()
        rule.onNodeWithText(text(R.string.sendalert_location_update)).assertExists()
    }

    @Test
    fun aRecentLastKnownLocationExplainsWhatWillBeUsed() {
        showForm(ready.copy(location = LocationUi(LocationStatus.LAST_KNOWN, "-33.50000, -70.65000", accuracyM = 45, ageMs = 180_000)))

        rule.onNodeWithText("Se usará tu última ubicación (±45 m, hace 3 min).").assertExists()
    }

    @Test
    fun aStaleLastKnownLocationWarnsItMayBeInexact() {
        showForm(
            ready.copy(location = LocationUi(LocationStatus.LAST_KNOWN, "-33.50000, -70.65000", accuracyM = 45, ageMs = 40 * 60_000L, isStale = true)),
        )

        rule.onNodeWithText("Tu última ubicación es de hace 40 min. Si te moviste, puede no ser exacta.").assertExists()
    }

    @Test
    fun searchingSaysSo() {
        showForm(ready.copy(location = LocationUi(LocationStatus.SEARCHING)))

        rule.onNodeWithText(text(R.string.sendalert_location_searching)).assertExists()
    }

    @Test
    fun aDisabledLocationOffersToTurnItOn() {
        var enabled = 0
        showForm(ready.copy(location = LocationUi(LocationStatus.DISABLED)), onEnable = { enabled++ })

        rule.onNodeWithText(text(R.string.sendalert_location_disabled)).assertExists()
        rule.onNodeWithText(text(R.string.sendalert_location_activate)).performScrollTo().performClick()

        assertEquals(1, enabled)
    }

    @Test
    fun noLocationAtAllTellsThePersonToGoOutside() {
        showForm(ready.copy(location = LocationUi(LocationStatus.UNAVAILABLE)))

        rule.onNodeWithText(text(R.string.sendalert_location_unavailable)).assertExists()
    }

    @Test
    fun theFooterShowsWhenEverythingIsReady() {
        showForm(ready)

        rule.onNodeWithText(text(R.string.sendalert_footer)).assertExists()
    }

    @Test
    fun withoutATypeTheButtonIsOffAndTheHintAsksForOne() {
        showForm(ready.copy(selectedType = null))

        rule.onNode(hasText(text(R.string.sendalert_hold_title)) and isEnabled().not()).assertExists()
        rule.onNodeWithText(text(R.string.sendalert_cannot_send_type)).assertExists()
    }

    @Test
    fun withoutLocationTheHintSaysItIsMissing() {
        showForm(ready.copy(location = LocationUi(LocationStatus.SEARCHING)))

        rule.onNodeWithText(text(R.string.sendalert_cannot_send_location)).assertExists()
    }

    @Test
    fun releasingEarlyIsExplained() {
        showForm(ready.copy(releasedEarly = true))

        rule.onNodeWithText(text(R.string.sendalert_hold_release_early)).assertExists()
    }

    @Test
    fun holdingTheButtonReportsPressAndRelease() {
        val events = mutableListOf<String>()
        showForm(ready, onHoldStart = { events += "start" }, onHoldEnd = { events += "end" })

        rule.onNodeWithText(text(R.string.sendalert_hold_title)).performTouchInput { down(center) }
        assertEquals(listOf("start"), events)
        rule.onNodeWithText(text(R.string.sendalert_hold_title)).performTouchInput { up() }

        assertEquals(listOf("start", "end"), events)
    }

    @Test
    fun aDisabledButtonIgnoresThePress() {
        val events = mutableListOf<String>()
        showForm(ready.copy(selectedType = null), onHoldStart = { events += "start" }, onHoldEnd = { events += "end" })

        rule.onNodeWithText(text(R.string.sendalert_hold_title)).performTouchInput { down(center); up() }

        assertTrue(events.isEmpty())
    }

    // ---------------- E2 · Mi alerta ----------------

    private val mine = MyAlertUiState(
        type = EmergencyType.MEDICAL, people = 3, vulnerableCount = 1, createdAtText = "09:39", reference = "NX-204",
        savedAtText = "09:39", echoCount = 2, lastEchoAgeMs = 12_000, uploadedAtText = null, volunteers = 2,
        coordinates = "-33.50000, -70.65000", accuracyM = 35, locationAgeMs = 120_000,
    )

    private fun showMine(state: MyAlertUiState, onSafe: (() -> Unit)? = null, onCancel: (() -> Unit)? = null) {
        rule.setContent {
            SendaTheme(darkTheme = false) {
                MyAlertScreen(state = state, connectivity = connectivity, onSafe = onSafe, onCancelAlert = onCancel)
            }
        }
    }

    @Test
    fun myAlertShowsTheFourStepsAndTheSummary() {
        showMine(mine)

        rule.onNodeWithText("Creada a las 09:39 · referencia NX-204").assertExists()
        rule.onNodeWithText(text(R.string.sendalert_mine_step_saved_title)).assertExists()
        rule.onNodeWithText("09:39 · disponible sin conexión").assertExists()
        rule.onNodeWithText(text(R.string.sendalert_mine_step_announcing_title)).assertExists()
        rule.onNodeWithText("2 teléfonos la retransmiten · hace 12 s").assertExists()
        rule.onNodeWithText(text(R.string.sendalert_mine_step_upload_pending)).assertExists()
        rule.onNodeWithText("Nivel naranja").assertExists()
        rule.onNodeWithText("3 personas · 1 vulnerable").assertExists()
        rule.onNodeWithText("±35 m · ubicación de hace 2 min").assertExists()
    }

    @Test
    fun myAlertShowsTheVolunteerCount() {
        showMine(mine.copy(volunteers = 7))

        rule.onNodeWithText("7").assertExists()
        rule.onNodeWithText(text(R.string.sendalert_mine_volunteers_title)).assertExists()
    }

    @Test
    fun withoutEchoesTheRelayStepSaysNobodyRelaysYet() {
        showMine(mine.copy(echoCount = 0, lastEchoAgeMs = null))

        rule.onNodeWithText(text(R.string.sendalert_mine_step_relayed_none)).assertExists()
    }

    @Test
    fun anUploadedAlertShowsWhenItWasUploaded() {
        showMine(mine.copy(uploadedAtText = "09:41"))

        rule.onNodeWithText("Subida a las 09:41").assertExists()
        rule.onAllNodesWithText(text(R.string.sendalert_mine_step_upload_pending)).assertCountEquals(0)
    }

    @Test
    fun theOptionalActionsAreHiddenWithoutCallbacks() {
        showMine(mine)

        rule.onAllNodesWithText(text(R.string.sendalert_mine_safe)).assertCountEquals(0)
        rule.onAllNodesWithText(text(R.string.sendalert_mine_cancel)).assertCountEquals(0)
    }

    @Test
    fun safeAsksForConfirmationAndOnlyConfirmingReportsIt() {
        var safe = 0
        showMine(mine, onSafe = { safe++ }, onCancel = {})

        rule.onNodeWithText(text(R.string.sendalert_mine_safe)).performScrollTo().performClick()
        rule.onNodeWithText(text(R.string.sendalert_mine_confirm_safe)).assertExists()
        assertEquals(0, safe)

        rule.onNodeWithText(text(R.string.common_confirm)).performClick()

        assertEquals(1, safe)
        rule.onAllNodesWithText(text(R.string.sendalert_mine_confirm_safe)).assertCountEquals(0)
    }

    @Test
    fun goingBackFromTheConfirmationReportsNothing() {
        var safe = 0
        showMine(mine, onSafe = { safe++ }, onCancel = {})

        rule.onNodeWithText(text(R.string.sendalert_mine_safe)).performScrollTo().performClick()
        rule.onNodeWithText(text(R.string.common_back)).performClick()

        assertEquals(0, safe)
        rule.onAllNodesWithText(text(R.string.sendalert_mine_confirm_safe)).assertCountEquals(0)
    }

    @Test
    fun cancelAlsoAsksForConfirmation() {
        var cancelled = 0
        showMine(mine, onSafe = {}, onCancel = { cancelled++ })

        rule.onNodeWithText(text(R.string.sendalert_mine_cancel)).performScrollTo().performClick()
        rule.onNodeWithText(text(R.string.sendalert_mine_confirm_cancel)).assertExists()
        assertEquals(0, cancelled)

        // En el diálogo, el botón lleva el mismo texto que la acción de la pantalla: es el último dibujado.
        rule.onAllNodesWithText(text(R.string.sendalert_mine_cancel)).onLast().performClick()

        assertEquals(1, cancelled)
    }

    // ---------------- E3 · Cuenta regresiva ----------------

    private fun showCountdown(remainingMs: Long, onCancel: () -> Unit = {}) {
        rule.setContent {
            SendaTheme(darkTheme = false) {
                AutoAlertCountdown(AutoAlertCountdownUiState(remainingMs = remainingMs), onCancel = onCancel)
            }
        }
    }

    @Test
    fun theCountdownShowsTheSecondsAndItsExplanation() {
        showCountdown(remainingMs = 12_000)

        rule.onNodeWithText("12").assertExists()
        rule.onNodeWithText(text(R.string.sendalert_countdown_unit)).assertExists()
        rule.onNodeWithText(text(R.string.sendalert_countdown_origin)).assertExists()
        rule.onNodeWithText(text(R.string.sendalert_countdown_body)).assertExists()
        rule.onNodeWithText(text(R.string.sendalert_countdown_hint)).assertExists()
    }

    @Test
    fun theCountdownRoundsPartialSecondsUp() {
        showCountdown(remainingMs = 11_100)

        rule.onNodeWithText("12").assertExists()
    }

    @Test
    fun cancellingReportsItOnce() {
        var cancelled = 0
        showCountdown(remainingMs = 15_000, onCancel = { cancelled++ })

        rule.onNodeWithText(text(R.string.sendalert_countdown_cancel)).performClick()

        assertEquals(1, cancelled)
    }
}
