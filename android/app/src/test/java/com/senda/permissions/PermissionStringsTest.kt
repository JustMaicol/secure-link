package com.senda.permissions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

// spec 100, criterio 12 · Cada mensaje de la sección 12 existe en strings.xml con su id y su texto, y
// strings.xml no trae textos de permisos que la spec no liste.
class PermissionStringsTest {

    // Las pruebas de :app corren con la carpeta del módulo (android/app) como directorio de trabajo.
    private val module = File(System.getProperty("user.dir"))
    private val spec = File(module, "../../specs/android/100-android-base/spec.md")
    private val strings = File(module, "src/main/res/values/strings.xml")

    private val specMessages: Map<String, String> by lazy {
        val text = spec.readText()
        // Solo la tabla "Mensajes": la de "Variables visibles" también empieza sus filas con un nombre entre comillas.
        val section = text.substring(
            text.indexOf("### Mensajes", text.indexOf("## 12. Contenido visible de la pantalla de permisos")),
            text.indexOf("## Criterios de aceptación"),
        )
        section.lines()
            .filter { Regex("""^\| `([a-z_]+)` \|""").containsMatchIn(it) }
            // | id | cuándo | dónde | tono | texto | acción |
            .map { row -> row.split("|").map { it.trim() } }
            .associate { cells -> cells[1].trim('`') to cells[5] }
    }

    private val resourceStrings: Map<String, String> by lazy {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(strings)
        val nodes = document.getElementsByTagName("string")
        (0 until nodes.length).associate { i ->
            val node = nodes.item(i)
            node.attributes.getNamedItem("name").nodeValue to node.textContent
        }
    }

    @Test
    fun `the spec lists the messages of the screen`() {
        assertTrue("la spec trae ${specMessages.size} mensajes", specMessages.size >= 40)
        assertTrue("permissions_title" in specMessages && "common_lost_permission" in specMessages)
    }

    @Test
    fun `every message of the spec exists in strings xml`() {
        val missing = specMessages.keys - resourceStrings.keys

        assertEquals("ids de la spec que faltan en strings.xml", emptySet<String>(), missing)
    }

    @Test
    fun `every message has the text the spec gives`() {
        val different = specMessages.filter { (id, text) -> resourceStrings[id] != text }

        assertEquals("textos distintos entre la spec y strings.xml", emptyMap<String, String>(), different)
    }

    @Test
    fun `strings xml has no permission text the spec does not list`() {
        val own = resourceStrings.keys.filter { it.startsWith("permissions_") || it == "common_lost_permission" }.toSet()

        assertEquals("textos de permisos que la spec no lista", emptySet<String>(), own - specMessages.keys)
    }
}
