package com.securelink.core.gossip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

// spec 103, criterio 8 · El motor es puro: sin reloj del sistema, sin azar sin semilla y sin Android.
class GossipRulesTest {

    private val gossipMain = File(checkNotNull(System.getProperty("androidRootDir")) { "Falta androidRootDir" })
        .resolve("core/src/main/kotlin/com/securelink/core/gossip")

    @Test
    fun `gossip sources never use System, Random or android`() {
        val sources = gossipMain.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        assertTrue("No se encontraron fuentes en ${gossipMain.path}", sources.isNotEmpty())

        val offenders = sources.flatMap { file ->
            forbiddenLines(file.readText()).map { "${file.relativeTo(gossipMain)}:$it" }
        }

        assertEquals("Referencias prohibidas en gossip/ (reglas 5, 6 y 7)", emptyList<String>(), offenders)
    }

    @Test
    fun `detector catches System, Random and android but not look-alikes`() {
        val source = """
            val a = System.currentTimeMillis()
            val b = Random(1)
            val c = kotlin.random.Random(2)
            import ${"android"}.util.Log
            val d = Mulberry32(3)
            val e = MySystem.value
            val f = rng.next()
        """.trimIndent()

        assertEquals(listOf(1, 2, 3, 4), forbiddenLines(source))
    }

    private companion object {
        val FORBIDDEN = Regex("""(?<![\w.])System\.|(?<![\w])Random\(|^\s*import\s+android""")

        fun forbiddenLines(source: String): List<Int> =
            source.lines().mapIndexedNotNull { i, line -> if (FORBIDDEN.containsMatchIn(line)) i + 1 else null }
    }
}
