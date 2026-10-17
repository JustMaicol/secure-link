package com.senda.permissions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// spec 100 · El fake devuelve el estado que se le fija.
class FakePermissionCheckerTest {

    @Test
    fun `allGranted grants every requirement`() {
        val checker = FakePermissionChecker.allGranted()

        assertEquals(Requirement.entries.toSet(), checker.check().keys)
        assertTrue(checker.check().values.all { it })
        assertTrue(missingBlocking(checker.check()).isEmpty())
    }

    @Test
    fun `check returns the state that was set`() {
        val checker = FakePermissionChecker.allGranted()
        checker.state = checker.state + (Requirement.LOCATION to false)

        assertEquals(listOf(Requirement.LOCATION), missingBlocking(checker.check()))
    }
}
