package com.senda.ui.format

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.senda.core.node.MeshHealth
import com.senda.core.protocol.EmergencyType
import com.senda.ui.theme.UiTone
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

// spec 100, sección 11 · Los textos de cada regla de formato, con plurales en español, desde strings.xml.
@RunWith(AndroidJUnit4::class)
class FormatTextTest {

    private val r = InstrumentationRegistry.getInstrumentation().targetContext.resources

    @Test
    fun distance() {
        assertEquals("850 m", r.distanceText(classifyDistance(850.0)))
        assertEquals("1,2 km", r.distanceText(classifyDistance(1200.0)))
        assertEquals("1,0 km", r.distanceText(classifyDistance(999.0)))
        assertEquals("99,9 km", r.distanceText(classifyDistance(99_900.0)))
        assertEquals("100 km", r.distanceText(classifyDistance(99_950.0)))
        assertEquals("123 km", r.distanceText(classifyDistance(123_000.0)))
        assertEquals("—", r.distanceText(classifyDistance(null)))
    }

    @Test
    fun elapsed() {
        assertEquals("ahora", r.elapsedText(classifyElapsed(500)))
        assertEquals("hace 1 s", r.elapsedText(classifyElapsed(1_000)))
        assertEquals("hace 20 s", r.elapsedText(classifyElapsed(20_000)))
        assertEquals("hace 5 min", r.elapsedText(classifyElapsed(5 * 60_000L)))
        assertEquals("hace 1 h", r.elapsedText(classifyElapsed(3_600_000L)))
        assertEquals("hace 3 h", r.elapsedText(classifyElapsed(3 * 3_600_000L)))
        assertEquals("hace 2 d", r.elapsedText(classifyElapsed(2 * 86_400_000L)))
    }

    @Test
    fun people() {
        assertEquals("1 persona", r.peopleText(classifyPeople(1)))
        assertEquals("3 personas", r.peopleText(classifyPeople(3)))
        assertEquals("255 o más personas", r.peopleText(classifyPeople(255)))
    }

    @Test
    fun peopleAndVulnerable() {
        assertEquals("3 personas · 1 vulnerable", r.peopleAndVulnerableText(3, 1))
        assertEquals("2 personas · sin vulnerables", r.peopleAndVulnerableText(2, 0))
        assertEquals("5 personas · 2 vulnerables", r.peopleAndVulnerableText(5, 2))
        assertEquals("1 persona · sin vulnerables", r.peopleAndVulnerableText(1, 0))
    }

    @Test
    fun cardinalPoints() {
        assertEquals(
            listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO"),
            CardinalPoint.entries.map { r.cardinalShortText(it) },
        )
        assertEquals(
            listOf("Norte", "Noreste", "Este", "Sureste", "Sur", "Suroeste", "Oeste", "Noroeste"),
            CardinalPoint.entries.map { r.cardinalNameText(it) },
        )
    }

    @Test
    fun emergencyLevels() {
        assertEquals("Nivel amarillo", r.emergencyLevelText(EmergencyType.FOOD_WATER))
        assertEquals("Nivel naranja", r.emergencyLevelText(EmergencyType.MEDICAL))
        assertEquals("Nivel rojo", r.emergencyLevelText(EmergencyType.TRAPPED_OR_SEVERE))
        assertEquals("Amarillo", r.emergencyOptionText(EmergencyType.FOOD_WATER))
        assertEquals("Naranja", r.emergencyOptionText(EmergencyType.MEDICAL))
        assertEquals("Rojo", r.emergencyOptionText(EmergencyType.TRAPPED_OR_SEVERE))
    }

    @Test
    fun connectivityCoversEveryMeshHealthWithItsTone() {
        val running = r.connectivityText(MeshHealth.RUNNING, internetOnline = false)
        assertEquals(ConnectivityText("Malla activa", UiTone.SUCCESS, "Sin internet"), running)

        val off = r.connectivityText(MeshHealth.BLUETOOTH_OFF, internetOnline = true)
        assertEquals(ConnectivityText("Bluetooth apagado", UiTone.WARNING, "Con internet"), off)

        // Ningún estado de la malla se queda sin texto.
        for (health in MeshHealth.entries) {
            assertEquals(health.name, false, r.connectivityText(health, false).mesh.isBlank())
        }
    }
}
