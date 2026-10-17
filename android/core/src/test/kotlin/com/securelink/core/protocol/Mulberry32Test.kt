package com.securelink.core.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// spec 101 · Criterio 8: salidas de referencia de la 001, sección 7, escritas en el código.
class Mulberry32Test {

    private val references = mapOf(
        1L to listOf(2693262067L, 11749833L, 2265367787L, 4213581821L, 4159151403L),
        42L to listOf(2581720956L, 1925393290L, 3661312704L, 2876485805L, 750819978L),
        185273099L to listOf(2943612290L, 4173463580L, 1481614005L, 1144772106L, 3950213351L),
        202116108L to listOf(1474678540L, 413007807L, 3323536170L, 359873878L, 2421111283L),
    )

    @Test
    fun `reproduces the first five outputs of every reference seed`() {
        for ((seed, expected) in references) {
            val rng = Mulberry32(seed.toInt())

            assertEquals("semilla $seed", expected, List(5) { rng.next() })
        }
    }

    @Test
    fun `seeds given as uint32 convert with toLong toInt`() {
        // 0xC3D4E5F6 = 3285509622 no cabe en un Int con signo: el vector JSON lo trae como uint32.
        val fromUint32 = 3285509622L.toInt()

        assertEquals(0xC3D4E5F6.toInt(), fromUint32)
        assertTrue(fromUint32 < 0)
    }

    @Test
    fun `outputs always fit in uint32`() {
        val rng = Mulberry32(12345)

        repeat(10_000) {
            val v = rng.next()
            assertTrue("fuera de uint32: $v", v in 0L..0xFFFFFFFFL)
        }
    }

    @Test
    fun `same seed gives the same sequence and instances are independent`() {
        val a = Mulberry32(7)
        val first = List(20) { a.next() }

        val fresh = Mulberry32(7)
        assertEquals(first, List(20) { fresh.next() })

        // Avanzar una instancia no mueve a otra con la misma semilla (no hay estado global).
        val b = Mulberry32(7)
        val c = Mulberry32(7)
        b.next()
        assertEquals(first.first(), c.next())
        assertEquals(first[1], b.next())
    }

    @Test
    fun `seedOf takes the low 32 bits of the deviceId`() {
        assertEquals(0xC3D4E5F6.toInt(), seedOf(0xA1B2_C3D4_E5F6L))
        assertEquals(0x0B0B0B0B, seedOf(0x0B0B_0B0B_0B0BL))
        assertEquals(0x0C0C0C0C, seedOf(0x0C0C_0C0C_0C0CL))
        assertEquals(185273099, seedOf(0x0B0B_0B0B_0B0BL))
    }

    @Test
    fun `a seed from a deviceId reproduces the reference outputs`() {
        // Teléfono B de la 001, sección 10: deviceId 0b0b0b0b0b0b → semilla 185273099.
        val rng = Mulberry32(seedOf(0x0B0B_0B0B_0B0BL))

        assertEquals(2943612290L, rng.next())
    }
}
