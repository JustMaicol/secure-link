package com.senda.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

// spec 100 · Reglas del proyecto: criterios 2, 3 y 9 (task A5).
class ProjectRulesTest {

    private val androidRoot = File(requireProperty("androidRootDir"))

    // --- Criterio 2: :core no depende de Android ---

    @Test
    fun `core sources never import android`() {
        val sources = androidRoot.resolve("core/src").walkTopDown()
            .filter { it.isFile && it.extension in setOf("kt", "java") }
            .toList()
        assertTrue("No se encontraron fuentes en core/src", sources.isNotEmpty())

        val offenders = sources.flatMap { file ->
            findAndroidImports(file.readText()).map { "${file.relativeTo(androidRoot)}:$it" }
        }
        assertEquals("Imports de Android en :core (reglas 5 y 14)", emptyList<String>(), offenders)
    }

    @Test
    fun `android import detector catches android and androidx`() {
        val source = "package x\n${IMPORT_ANDROID}.content.Context\n${IMPORT_ANDROID}x.annotation.Keep\nimport java.io.File\n"
        assertEquals(listOf(2, 3), findAndroidImports(source))
    }

    // --- Criterio 3: versiones exactas en el catálogo (regla 12) ---

    @Test
    fun `version catalog has only exact versions`() {
        val catalog = androidRoot.resolve("gradle/libs.versions.toml")
        assertTrue("No existe ${catalog.path}", catalog.isFile)

        assertEquals("Versiones no exactas en libs.versions.toml", emptyList<String>(), findInexactVersions(catalog.readText()))
    }

    @Test
    fun `inexact version detector catches plus, latest and ranges`() {
        val toml = """
            # Un comentario con "+" y latest no cuenta
            [versions]
            ok = "2.4.21"
            bom = "2026.09.00"
            plus = "1.+"
            latest = "latest.release"
            range = "[1.0,2.0)"
            rich = { strictly = "1.0" }
            [libraries]
            a = { module = "g:a", version.ref = "ok" }
            b = { module = "g:b" }
            c = { module = "g:c", version = "3.0+" }
        """.trimIndent()

        assertEquals(
            listOf("plus = \"1.+\"", "latest = \"latest.release\"", "range = \"[1.0,2.0)\"", "rich = { strictly = \"1.0\" }", "c: 3.0+"),
            findInexactVersions(toml),
        )
    }

    // --- Criterio 9: la carpeta de vectores existe ---

    @Test
    fun `vectorsDir exists`() {
        val path = System.getProperty("vectorsDir")
        assertNotNull("Falta la propiedad de sistema vectorsDir", path)
        assertTrue("No existe la carpeta de vectores: $path (specs/shared/vectors, de P2)", File(path!!).isDirectory)
    }

    private companion object {
        // Armado por partes para que este archivo no se detecte a sí mismo.
        val IMPORT_ANDROID = "import " + "android"

        val ANDROID_IMPORT = Regex("""^\s*import\s+androidx?\.""")
        val EXACT_VERSION = Regex("""^\d+(\.\d+)*(-[0-9A-Za-z.]+)?$""")
        val ENTRY = Regex("""^([\w.-]+)\s*=\s*(.+)$""")
        val QUOTED = Regex("""^"([^"]*)"$""")
        val INLINE_VERSION = Regex("""\bversion\s*=\s*("[^"]*"|\{[^}]*\})""")

        fun requireProperty(name: String): String =
            checkNotNull(System.getProperty(name)) { "Falta la propiedad de sistema $name (core/build.gradle.kts)" }

        /** Números de línea (desde 1) con un import de android.* o androidx.*. */
        fun findAndroidImports(source: String): List<Int> =
            source.lines().mapIndexedNotNull { i, line -> if (ANDROID_IMPORT.containsMatchIn(line)) i + 1 else null }

        fun isExact(version: String): Boolean =
            EXACT_VERSION.matches(version) && !version.contains("latest", ignoreCase = true)

        /** Versiones que no son exactas: en [versions] y en `version = ...` de bibliotecas y plugins. */
        fun findInexactVersions(toml: String): List<String> {
            val problems = mutableListOf<String>()
            var section = ""
            for (raw in toml.lines()) {
                val line = raw.substringBefore('#').trim()
                if (line.isEmpty()) continue
                if (line.startsWith("[")) {
                    section = line
                    continue
                }
                val (key, value) = ENTRY.matchEntire(line)?.destructured ?: continue
                if (section == "[versions]") {
                    val version = QUOTED.matchEntire(value)?.groupValues?.get(1)
                    if (version == null || !isExact(version)) problems += line
                } else {
                    val inline = INLINE_VERSION.find(value)?.groupValues?.get(1) ?: continue
                    val version = QUOTED.matchEntire(inline)?.groupValues?.get(1)
                    if (version == null || !isExact(version)) problems += "$key: ${version ?: inline}"
                }
            }
            return problems
        }
    }
}
