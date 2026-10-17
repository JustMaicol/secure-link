package com.securelink.core.gossip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

// spec 103 · Criterio 2: cada gossip/*.json (specs/shared/vectors/gossip, de P2) produce exactamente su `expected`.
class GossipVectorsTest {

    private val files: List<File> = File(checkNotNull(System.getProperty("vectorsDir")) { "Falta vectorsDir" }, "gossip")
        .listFiles { f -> f.extension == "json" }
        .orEmpty()
        .sortedBy { it.name }

    @Test
    fun `the gossip vectors include the five scenarios of the 001`() {
        val names = files.map { it.nameWithoutExtension }

        assertTrue(
            "gossip/ trae $names",
            names.containsAll(
                listOf("line-a-b-c", "triangle-c-blocks-a", "ttl-exhausted", "expired-alert", "two-origins-same-time"),
            ),
        )
    }

    @Test
    fun `every scenario produces exactly its expected sequence`() {
        for (file in files) {
            val parsed = ScenarioJson.parse(file.readText())

            assertEquals(file.name, parsed.expected, ScenarioRunner.run(parsed.scenario))
        }
    }
}
