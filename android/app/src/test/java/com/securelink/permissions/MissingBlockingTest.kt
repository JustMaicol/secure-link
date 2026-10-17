package com.securelink.permissions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// spec 100 · Criterio 6.
class MissingBlockingTest {

    private val allGranted = Requirement.entries.associateWith { true }

    @Test
    fun `nothing missing when everything is granted`() {
        assertTrue(missingBlocking(allGranted).isEmpty())
    }

    @Test
    fun `returns the missing blocking requirements in enum order`() {
        val state = allGranted + mapOf(
            Requirement.LOCATION_ON to false,
            Requirement.BLUETOOTH to false,
            Requirement.NOTIFICATIONS to false,
        )

        assertEquals(
            listOf(Requirement.BLUETOOTH, Requirement.NOTIFICATIONS, Requirement.LOCATION_ON),
            missingBlocking(state),
        )
    }

    @Test
    fun `ignores BATTERY_UNRESTRICTED`() {
        val state = allGranted + (Requirement.BATTERY_UNRESTRICTED to false)

        assertTrue(missingBlocking(state).isEmpty())
    }

    @Test
    fun `everything denied returns all five blocking requirements and not the battery one`() {
        val state = Requirement.entries.associateWith { false }

        assertEquals(
            listOf(
                Requirement.BLUETOOTH,
                Requirement.LOCATION,
                Requirement.NOTIFICATIONS,
                Requirement.BLUETOOTH_ON,
                Requirement.LOCATION_ON,
            ),
            missingBlocking(state),
        )
    }

    @Test
    fun `a requirement absent from the map counts as missing`() {
        assertEquals(Requirement.entries - Requirement.BATTERY_UNRESTRICTED, missingBlocking(emptyMap()))
    }

    @Test
    fun `enum keeps the order the permissions screen relies on`() {
        assertEquals(
            listOf("BLUETOOTH", "LOCATION", "NOTIFICATIONS", "BLUETOOTH_ON", "LOCATION_ON", "BATTERY_UNRESTRICTED"),
            Requirement.entries.map { it.name },
        )
    }
}
