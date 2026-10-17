package com.senda.features

import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.senda.R
import com.senda.features.nearby.NearbyAlertUi
import com.senda.features.nearby.NearbyDetailScreen
import com.senda.features.nearby.NearbyListScreen
import com.senda.features.nearby.NearbyListUiState
import com.senda.features.nearby.SampleNearbyAlerts
import com.senda.features.status.StatusContent
import com.senda.features.status.StatusDebugUiState
import com.senda.features.status.StatusUiState
import com.senda.ui.format.ConnectivityText
import com.senda.ui.theme.SendaTheme
import com.senda.ui.theme.UiTone
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// spec 109 · Las pantallas C1, C2 y S1 dibujan su UiState (a mano, sin teléfono ni ViewModel) y avisan cada acción.
@RunWith(AndroidJUnit4::class)
class NearbyAndStatusScreensTest {

    @get:Rule
    val rule = createComposeRule()

    private val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
    private fun text(id: Int) = resources.getString(id)

    private val connectivity = ConnectivityText("Malla activa", UiTone.SUCCESS, "Sin internet")
    private val first = SampleNearbyAlerts[0]

    // ---------------- C1 · Lista ----------------

    private fun showList(state: NearbyListUiState, onOpen: (NearbyAlertUi) -> Unit = {}) {
        rule.setContent { SendaTheme(darkTheme = false) { NearbyListScreen(state, connectivity, onOpenAlert = onOpen) } }
    }

    @Test
    fun theListShowsTheCountTheOrderAndEachAlert() {
        showList(NearbyListUiState(SampleNearbyAlerts, updatedAgeMs = 12_000, demoData = true))

        rule.onNodeWithText("3 alertas").assertExists()
        rule.onNodeWithText(text(R.string.nearby_sort)).assertExists()
        rule.onNodeWithText("Nivel naranja").assertExists()
        rule.onNodeWithText("Nivel amarillo").assertExists()
        rule.onNodeWithText("Nivel rojo").assertExists()
        rule.onNodeWithText("320 m").assertExists()
        rule.onNodeWithText("aprox. · NE").assertExists()
        rule.onNodeWithText("3 personas · 1 vulnerable").assertExists()
        rule.onNodeWithText("2 en camino").assertExists()
        rule.onNodeWithText("hace 2 min").assertExists()
    }

    @Test
    fun oneAlertIsSingular() {
        showList(NearbyListUiState(listOf(first)))

        rule.onNodeWithText("1 alerta").assertExists()
    }

    @Test
    fun demoDataSaysSoAndRealDataSaysWhenItWasUpdated() {
        showList(NearbyListUiState(SampleNearbyAlerts, updatedAgeMs = 12_000, demoData = true))
        rule.onNodeWithText("Datos ilustrativos · actualización hace 12 s").assertExists()
    }

    @Test
    fun realDataDoesNotSayItIsIllustrative() {
        showList(NearbyListUiState(SampleNearbyAlerts, updatedAgeMs = 12_000, demoData = false))

        rule.onNodeWithText("Actualización hace 12 s").assertExists()
        rule.onAllNodesWithText("Datos ilustrativos", substring = true).assertCountEquals(0)
    }

    @Test
    fun tappingAnAlertReportsThatAlert() {
        val opened = mutableListOf<String>()
        showList(NearbyListUiState(SampleNearbyAlerts)) { opened += it.id }

        rule.onNodeWithText("-33.50020, -70.64620").performScrollTo().performClick()

        assertEquals(listOf(SampleNearbyAlerts[1].id), opened)
    }

    @Test
    fun anEmptyListExplainsItself() {
        showList(NearbyListUiState())

        rule.onNodeWithText(text(R.string.nearby_empty_title)).assertExists()
        // Sin alertas no hay conteo ni orden que mostrar.
        rule.onAllNodesWithText(text(R.string.nearby_sort)).assertCountEquals(0)
    }

    @Test
    fun withoutDistanceOrBearingTheListShowsADash() {
        showList(NearbyListUiState(listOf(first.copy(distanceM = null, bearingDeg = null))))

        rule.onNodeWithText("aprox. · —").assertExists()
        rule.onAllNodesWithText("320 m").assertCountEquals(0)
    }

    // ---------------- C2 · Detalle ----------------

    private fun showDetail(
        alert: NearbyAlertUi = first,
        onBack: () -> Unit = {},
        onOnMyWay: () -> Unit = {},
        onOpenMaps: () -> Unit = {},
    ) {
        rule.setContent {
            SendaTheme(darkTheme = false) {
                NearbyDetailScreen(alert, connectivity, onBack = onBack, onOnMyWay = onOnMyWay, onOpenMaps = onOpenMaps)
            }
        }
    }

    @Test
    fun theCompassSpeaksDistanceBearingAndTheNotARouteWarning() {
        showDetail()

        rule.onNodeWithContentDescription("320 m", substring = true)
            .assertContentDescriptionContains("Noreste · 45°", substring = true)
            .assertContentDescriptionContains("no es una ruta", substring = true)
    }

    @Test
    fun theDetailShowsTheAlertData() {
        showDetail()

        rule.onNodeWithText("NX-204 · recibida hace 2 min").assertExists()
        rule.onNodeWithText("Nivel naranja").assertExists()
        rule.onNodeWithText("2 en camino").assertExists()
        rule.onNodeWithText("-33.49715, -70.64950").assertExists()
        rule.onNodeWithText("3 · 1 vulnerable").assertExists()
        rule.onNodeWithText("Ubicación de hace 2 min · precisión ±35 m. La persona puede haberse desplazado.").assertExists()
    }

    @Test
    fun withoutDistanceOrBearingTheCompassSaysItIsUnknown() {
        showDetail(first.copy(distanceM = null, bearingDeg = null))

        rule.onNodeWithContentDescription("—, —.", substring = true).assertExists()
    }

    @Test
    fun theDetailActionsAreReported() {
        var back = 0
        var onMyWay = 0
        var maps = 0
        showDetail(onBack = { back++ }, onOnMyWay = { onMyWay++ }, onOpenMaps = { maps++ })

        rule.onNodeWithText(text(R.string.nearby_detail_back)).performClick()
        rule.onNodeWithText(text(R.string.nearby_detail_on_my_way)).performScrollTo().performClick()
        rule.onNodeWithText(text(R.string.nearby_detail_open_maps)).performScrollTo().performClick()

        assertEquals(listOf(1, 1, 1), listOf(back, onMyWay, maps))
    }

    // ---------------- S1 · Estado ----------------

    private val status = StatusUiState(
        meshActive = true, announcementsActive = 12, neighbors = 4, internetOnline = false, pendingUploads = 1,
        lastUploadClock = "09:12", lastUploadAgeMs = 29 * 60_000L, deviceId = "NX-PXL-7A2F", relayTag = "R7-K2",
        motionEnabled = true, motionCountdownSeconds = 15,
        debug = StatusDebugUiState(blockedTags = listOf("TEST-04"), motionReadingG = 0.18f),
    )

    private fun showStatus(
        state: StatusUiState = status,
        onCopy: () -> Unit = {},
        onMotion: (Boolean) -> Unit = {},
        onPermissions: () -> Unit = {},
        onRemoveTag: (String) -> Unit = {},
        onAddTag: () -> Unit = {},
        onSimulate: () -> Unit = {},
    ) {
        rule.setContent {
            SendaTheme(darkTheme = false) {
                StatusContent(
                    state = state, connectivity = connectivity, onCopyId = onCopy, onMotionEnabledChange = onMotion,
                    onReviewPermissions = onPermissions, onRemoveTag = onRemoveTag, onAddTag = onAddTag, onSimulateShake = onSimulate,
                )
            }
        }
    }

    @Test
    fun statusShowsTheMeshServerAndPhoneData() {
        showStatus()

        rule.onNodeWithText(text(R.string.status_mesh_active)).assertExists()
        rule.onNodeWithText("12").assertExists()
        rule.onNodeWithText(text(R.string.status_metric_announcements)).assertExists()
        rule.onNodeWithText("4").assertExists()
        rule.onNodeWithText("1 alerta pendiente").assertExists()
        rule.onNodeWithText("09:12 · hace 29 min").assertExists()
        rule.onNodeWithText("NX-PXL-7A2F").assertExists()
        rule.onNodeWithText("R7-K2").assertExists()
        rule.onNodeWithText("Cuenta regresiva de 15 s antes de enviar").assertExists()
    }

    @Test
    fun aStoppedMeshSaysSo() {
        showStatus(status.copy(meshActive = false))

        rule.onNodeWithText(text(R.string.status_mesh_stopped)).assertExists()
        rule.onAllNodesWithText(text(R.string.status_mesh_active)).assertCountEquals(0)
    }

    @Test
    fun withInternetTheServerSaysSo() {
        showStatus(status.copy(internetOnline = true, pendingUploads = 0, lastUploadClock = null, lastUploadAgeMs = null))

        rule.onNodeWithText("0 alertas pendientes").assertExists()
        rule.onNodeWithText(text(R.string.format_unknown)).assertExists()
        rule.onAllNodesWithText(text(R.string.connectivity_internet_on)).assertCountEquals(1)
    }

    @Test
    fun copyingTheIdIsReported() {
        var copied = 0
        showStatus(onCopy = { copied++ })

        rule.onNodeWithContentDescription(text(R.string.common_copy)).performScrollTo().performClick()

        assertEquals(1, copied)
    }

    @Test
    fun theMotionSwitchReportsTheNewValue() {
        val values = mutableListOf<Boolean>()
        showStatus(onMotion = { values += it })

        rule.onNode(isToggleable()).performScrollTo().performClick()

        assertEquals(listOf(false), values)
    }

    @Test
    fun reviewingPermissionsIsReported() {
        var reviewed = 0
        showStatus(onPermissions = { reviewed++ })

        rule.onNodeWithText(text(R.string.status_settings_permissions)).performScrollTo().performClick()

        assertEquals(1, reviewed)
    }

    @Test
    fun theSimulationToolsAppearOnlyWithDebugData() {
        showStatus()
        rule.onNodeWithText(text(R.string.common_simulation_badge)).performScrollTo().assertExists()
        rule.onNodeWithText("TEST-04").assertExists()
        rule.onNodeWithText("0,18 g").assertExists()
    }

    @Test
    fun withoutDebugDataThereIsNoSimulationCard() {
        showStatus(status.copy(debug = null))

        rule.onAllNodesWithText(text(R.string.common_simulation_badge)).assertCountEquals(0)
        rule.onAllNodesWithText(text(R.string.status_debug_simulate_shake)).assertCountEquals(0)
    }

    @Test
    fun theSimulationActionsAreReported() {
        val removed = mutableListOf<String>()
        var added = 0
        var simulated = 0
        showStatus(onRemoveTag = { removed += it }, onAddTag = { added++ }, onSimulate = { simulated++ })

        rule.onNodeWithContentDescription("Quitar TEST-04").performScrollTo().performClick()
        rule.onNodeWithText(text(R.string.status_debug_filter_add)).performScrollTo().performClick()
        rule.onNodeWithText(text(R.string.status_debug_simulate_shake)).performScrollTo().performClick()

        assertEquals(listOf("TEST-04"), removed)
        assertEquals(1, added)
        assertEquals(1, simulated)
    }
}
