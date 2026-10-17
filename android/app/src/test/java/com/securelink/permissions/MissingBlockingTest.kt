package com.securelink.permissions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// spec 100 · Criterio 6.
class MissingBlockingTest {

    private val allGranted = Requirement.entries.associateWith { true }

    private val blocking = listOf(
        Requirement.BLUETOOTH,
        Requirement.LOCATION,
        Requirement.BLUETOOTH_ON,
        Requirement.LOCATION_ON,
    )

    @Test
    fun `nothing missing when everything is granted`() {
        assertTrue(missingBlocking(allGranted).isEmpty())
    }

    @Test
    fun `returns the missing blocking requirements in enum order`() {
        val state = allGranted + mapOf(
            Requirement.LOCATION_ON to false,
            Requirement.BLUETOOTH to false,
            Requirement.BLUETOOTH_ON to false,
        )

        assertEquals(
            listOf(Requirement.BLUETOOTH, Requirement.BLUETOOTH_ON, Requirement.LOCATION_ON),
            missingBlocking(state),
        )
    }

    @Test
    fun `ignores the recommended requirements`() {
        for (requirement in RECOMMENDED_REQUIREMENTS) {
            val state = allGranted + (requirement to false)

            assertTrue("$requirement no debe bloquear", missingBlocking(state).isEmpty())
        }
    }

    @Test
    fun `notifications are recommended, not blocking (Figma lets the user continue without them)`() {
        val state = allGranted + (Requirement.NOTIFICATIONS to false)

        assertTrue(missingBlocking(state).isEmpty())
        assertTrue(Requirement.NOTIFICATIONS in RECOMMENDED_REQUIREMENTS)
    }

    @Test
    fun `the recommended set is exactly notifications, background location and battery`() {
        assertEquals(
            setOf(Requirement.NOTIFICATIONS, Requirement.BACKGROUND_LOCATION, Requirement.BATTERY_UNRESTRICTED),
            RECOMMENDED_REQUIREMENTS,
        )
    }

    @Test
    fun `everything denied returns exactly the four blocking requirements`() {
        val state = Requirement.entries.associateWith { false }

        assertEquals(blocking, missingBlocking(state))
    }

    @Test
    fun `a requirement absent from the map counts as missing`() {
        assertEquals(blocking, missingBlocking(emptyMap()))
    }

    @Test
    fun `enum keeps the order the contract declares`() {
        assertEquals(
            listOf(
                "BLUETOOTH", "LOCATION", "NOTIFICATIONS", "BLUETOOTH_ON", "LOCATION_ON",
                "BACKGROUND_LOCATION", "BATTERY_UNRESTRICTED",
            ),
            Requirement.entries.map { it.name },
        )
    }

    @Test
    fun `only background location depends on another requirement, and it is the foreground location`() {
        assertEquals(Requirement.LOCATION, prerequisiteOf(Requirement.BACKGROUND_LOCATION))
        for (requirement in Requirement.entries - Requirement.BACKGROUND_LOCATION) {
            assertNull("$requirement no depende de otro", prerequisiteOf(requirement))
        }
        assertFalse(prerequisiteOf(Requirement.LOCATION) == Requirement.BACKGROUND_LOCATION)
    }
}
