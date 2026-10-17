package com.senda.features.nearby

import org.junit.Assert.assertEquals
import org.junit.Test

// spec 109 · Los grados junto al rumbo son enteros de 0 a 359: el redondeo nunca escribe 360.
class CompassDegreesTest {

    @Test
    fun `whole degrees stay as they are`() {
        assertEquals(0, displayDegrees(0.0))
        assertEquals(45, displayDegrees(45.0))
        assertEquals(359, displayDegrees(359.0))
    }

    @Test
    fun `it rounds to the nearest degree`() {
        assertEquals(45, displayDegrees(44.6))
        assertEquals(44, displayDegrees(44.4))
    }

    @Test
    fun `a bearing that rounds up to 360 is written as 0`() {
        assertEquals(0, displayDegrees(359.6))
        assertEquals(0, displayDegrees(360.0))
    }

    @Test
    fun `a bearing just below 360 that rounds down stays at 359`() {
        assertEquals(359, displayDegrees(359.4))
    }

    @Test
    fun `a negative bearing wraps into range`() {
        assertEquals(350, displayDegrees(-10.0))
    }
}
