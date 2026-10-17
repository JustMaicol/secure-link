package com.senda.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.senda.R
import com.senda.core.node.FakeMeshStatus
import com.senda.core.node.MeshHealth
import com.senda.ui.theme.SendaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// spec 100, criterio 8 · La navegación muestra los tres destinos con sus pantallas provisionales rotuladas.
@RunWith(AndroidJUnit4::class)
class SendaAppNavigationTest {

    @get:Rule
    val rule = createComposeRule()

    private val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
    private fun text(id: Int, vararg args: Any) = resources.getString(id, *args)

    private val running = FakeMeshStatus().state.value

    @Test
    fun startsOnSendAlertWithTheForm() {
        rule.setContent { SendaTheme(darkTheme = false) { SendaApp(running) } }

        rule.onNodeWithText(text(R.string.sendalert_title)).assertIsDisplayed()
        // La pestaña ya muestra el formulario (E1): su primer bloque es el selector de nivel.
        rule.onNodeWithText(text(R.string.sendalert_level_label)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.nav_sendalert)).assertIsSelected()
        rule.onNodeWithText(text(R.string.nav_nearby)).assertIsNotSelected()
        rule.onNodeWithText(text(R.string.nav_status)).assertIsNotSelected()
    }

    @Test
    fun theThreeDestinationsNavigateAndShowTheirLabel() {
        rule.setContent { SendaTheme(darkTheme = false) { SendaApp(running) } }

        rule.onNodeWithText(text(R.string.nav_nearby)).performClick()
        rule.onNodeWithText(text(R.string.nearby_title)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.provisional_pending, "109")).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.nav_nearby)).assertIsSelected()

        rule.onNodeWithText(text(R.string.nav_status)).performClick()
        rule.onNodeWithText(text(R.string.status_title)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.provisional_pending, "109")).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.nav_status)).assertIsSelected()

        rule.onNodeWithText(text(R.string.nav_sendalert)).performClick()
        rule.onNodeWithText(text(R.string.sendalert_title)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.nav_sendalert)).assertIsSelected()
    }

    @Test
    fun theSameScreensAppearInTheDarkTheme() {
        rule.setContent { SendaTheme(darkTheme = true) { SendaApp(running) } }

        rule.onNodeWithText(text(R.string.sendalert_title)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.nav_nearby)).performClick()
        rule.onNodeWithText(text(R.string.nearby_title)).assertIsDisplayed()
    }

    @Test
    fun theConnectivityStripFollowsTheMeshHealthAndInternet() {
        val state = FakeMeshStatus(running.copy(health = MeshHealth.BLUETOOTH_OFF)).state.value
        rule.setContent { SendaTheme(darkTheme = false) { SendaApp(state, internetOnline = true) } }

        rule.onNodeWithText(text(R.string.connectivity_mesh_bluetooth_off)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.connectivity_internet_on)).assertIsDisplayed()
    }

    @Test
    fun runningMeshWithoutInternetShowsTheFigmaStrip() {
        rule.setContent { SendaTheme(darkTheme = false) { SendaApp(running) } }

        rule.onNodeWithText(text(R.string.connectivity_mesh_running)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.connectivity_internet_off)).assertIsDisplayed()
    }
}
