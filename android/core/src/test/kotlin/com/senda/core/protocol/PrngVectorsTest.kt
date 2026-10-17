package com.senda.core.protocol

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

// spec 101 · Criterio 2: prng.json (specs/shared/vectors, de P2).
class PrngVectorsTest {

    private val vectors: JsonArray = Json.parseToJsonElement(
        File(checkNotNull(System.getProperty("vectorsDir")) { "Falta vectorsDir" }, "prng.json").readText(),
    ).jsonArray

    @Test
    fun `prng vectors cover at least the four reference seeds`() {
        assertTrue("prng.json trae ${vectors.size} semillas", vectors.size >= 4)
    }

    @Test
    fun `every seed reproduces its outputs`() {
        for (entry in vectors) {
            val seed = entry.jsonObject.getValue("seed").jsonPrimitive.long
            val expected = entry.jsonObject.getValue("outputs").jsonArray.map { it.jsonPrimitive.long }
            // Las semillas van como uint32 en el JSON: toLong().toInt() recupera los 32 bits.
            val rng = Mulberry32(seed.toInt())

            assertEquals("semilla $seed", expected, List(expected.size) { rng.next() })
        }
    }
}
