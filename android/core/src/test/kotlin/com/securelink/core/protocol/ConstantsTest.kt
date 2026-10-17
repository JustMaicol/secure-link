package com.securelink.core.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

// spec 101 · Criterio 9: el byte mágico solo aparece en ProtocolConfig (regla 17).
// Las demás constantes del protocolo (TTL, ventanas, tamaños) se revisan en el PR.
class ConstantsTest {

    private val coreMain = File(checkNotNull(System.getProperty("androidRootDir")) { "Falta androidRootDir" })
        .resolve("core/src/main")

    @Test
    fun `magic byte appears only in ProtocolConfig`() {
        val sources = coreMain.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        assertTrue("No se encontraron fuentes en ${coreMain.path}", sources.isNotEmpty())
        assertTrue("ProtocolConfig.kt tiene que existir", sources.any { it.name == "ProtocolConfig.kt" })

        val offenders = sources
            .filter { it.name != "ProtocolConfig.kt" }
            .flatMap { file -> magicByteLines(file.readText()).map { "${file.relativeTo(coreMain)}:$it" } }

        assertEquals("Byte mágico fuera de ProtocolConfig", emptyList<String>(), offenders)
    }

    @Test
    fun `ProtocolConfig is where the magic byte lives`() {
        val config = coreMain.walkTopDown().first { it.name == "ProtocolConfig.kt" }

        assertTrue(magicByteLines(config.readText()).isNotEmpty())
    }

    @Test
    fun `detector catches 0xA7, 0xa7 and 167 but not look-alikes`() {
        val source = """
            val a = 0xA7
            val b = 0xa7
            val c = 167
            val d = 0xA70
            val e = 1_760_600_167
            val f = 0x6D2B79F5
            val g = 4_294_967_295
        """.trimIndent()

        assertEquals(listOf(1, 2, 3), magicByteLines(source))
    }

    private companion object {
        val MAGIC = Regex("""(?<![\w.])(0[xX][aA]7|167)(?![\w.])""")

        /** Números de línea (desde 1) donde aparece el byte mágico como literal. */
        fun magicByteLines(source: String): List<Int> =
            source.lines().mapIndexedNotNull { i, line -> if (MAGIC.containsMatchIn(line)) i + 1 else null }
    }
}
