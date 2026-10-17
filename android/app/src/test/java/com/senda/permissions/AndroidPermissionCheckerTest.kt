package com.senda.permissions

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// spec 100, sección 5.2 · Qué cuenta como "concedido" para cada requisito, sin teléfono (FakePlatformState).
class AndroidPermissionCheckerTest {

    private val bluetooth = setOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.BLUETOOTH_CONNECT,
    )
    private val location = setOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    private val notifications = setOf(Manifest.permission.POST_NOTIFICATIONS)
    private val backgroundLocation = setOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

    private fun check(platform: FakePlatformState) = AndroidPermissionChecker(platform).check()

    @Test
    fun `answers every requirement`() {
        assertEquals(Requirement.entries.toSet(), check(FakePlatformState()).keys)
    }

    @Test
    fun `nothing granted and nothing on means every requirement is missing`() {
        val state = check(FakePlatformState())

        assertTrue(state.values.none { it })
        assertEquals(
            listOf(Requirement.BLUETOOTH, Requirement.LOCATION, Requirement.BLUETOOTH_ON, Requirement.LOCATION_ON),
            missingBlocking(state),
        )
    }

    @Test
    fun `everything granted and on means nothing is missing`() {
        val state = check(
            FakePlatformState(
                granted = bluetooth + location + notifications + backgroundLocation,
                bluetoothEnabled = true,
                locationEnabled = true,
                ignoringBatteryOptimizations = true,
            ),
        )

        assertTrue(state.values.all { it })
        assertTrue(missingBlocking(state).isEmpty())
    }

    @Test
    fun `bluetooth needs the three permissions together`() {
        for (missing in bluetooth) {
            val state = check(FakePlatformState(granted = bluetooth - missing))
            assertFalse("sin $missing", state.getValue(Requirement.BLUETOOTH))
        }
        assertTrue(check(FakePlatformState(granted = bluetooth)).getValue(Requirement.BLUETOOTH))
    }

    @Test
    fun `location needs fine and coarse together`() {
        assertFalse(check(FakePlatformState(granted = setOf(Manifest.permission.ACCESS_FINE_LOCATION))).getValue(Requirement.LOCATION))
        assertFalse(check(FakePlatformState(granted = setOf(Manifest.permission.ACCESS_COARSE_LOCATION))).getValue(Requirement.LOCATION))
        assertTrue(check(FakePlatformState(granted = location)).getValue(Requirement.LOCATION))
    }

    @Test
    fun `each requirement depends only on its own permissions`() {
        val onlyNotifications = check(FakePlatformState(granted = notifications))

        assertTrue(onlyNotifications.getValue(Requirement.NOTIFICATIONS))
        assertFalse(onlyNotifications.getValue(Requirement.BLUETOOTH))
        assertFalse(onlyNotifications.getValue(Requirement.LOCATION))
    }

    @Test
    fun `permissions do not turn on the system switches`() {
        val state = check(FakePlatformState(granted = bluetooth + location + notifications))

        assertFalse(state.getValue(Requirement.BLUETOOTH_ON))
        assertFalse(state.getValue(Requirement.LOCATION_ON))
        assertFalse(state.getValue(Requirement.BATTERY_UNRESTRICTED))
    }

    @Test
    fun `system switches are read from the platform`() {
        val state = check(FakePlatformState(bluetoothEnabled = true))

        assertTrue(state.getValue(Requirement.BLUETOOTH_ON))
        assertFalse(state.getValue(Requirement.LOCATION_ON))
        assertTrue(check(FakePlatformState(locationEnabled = true)).getValue(Requirement.LOCATION_ON))
        assertTrue(check(FakePlatformState(ignoringBatteryOptimizations = true)).getValue(Requirement.BATTERY_UNRESTRICTED))
    }

    @Test
    fun `the battery exclusion never blocks`() {
        val state = check(
            FakePlatformState(granted = bluetooth + location + notifications, bluetoothEnabled = true, locationEnabled = true),
        )

        assertFalse(state.getValue(Requirement.BATTERY_UNRESTRICTED))
        assertTrue(missingBlocking(state).isEmpty())
    }

    @Test
    fun `background location is its own permission and foreground location does not grant it`() {
        assertFalse(check(FakePlatformState(granted = location)).getValue(Requirement.BACKGROUND_LOCATION))
        assertFalse(check(FakePlatformState(granted = bluetooth + location + notifications)).getValue(Requirement.BACKGROUND_LOCATION))
        assertTrue(check(FakePlatformState(granted = backgroundLocation)).getValue(Requirement.BACKGROUND_LOCATION))
    }

    @Test
    fun `background location never blocks`() {
        val state = check(
            FakePlatformState(granted = bluetooth + location, bluetoothEnabled = true, locationEnabled = true),
        )

        assertFalse(state.getValue(Requirement.BACKGROUND_LOCATION))
        assertTrue(missingBlocking(state).isEmpty())
    }

    @Test
    fun `runtime permission groups match section 5-1`() {
        assertEquals(bluetooth, runtimePermissionsOf(Requirement.BLUETOOTH).toSet())
        assertEquals(location, runtimePermissionsOf(Requirement.LOCATION).toSet())
        assertEquals(notifications, runtimePermissionsOf(Requirement.NOTIFICATIONS).toSet())
        assertEquals(backgroundLocation, runtimePermissionsOf(Requirement.BACKGROUND_LOCATION).toSet())
        assertTrue(runtimePermissionsOf(Requirement.BLUETOOTH_ON).isEmpty())
        assertTrue(runtimePermissionsOf(Requirement.LOCATION_ON).isEmpty())
        assertTrue(runtimePermissionsOf(Requirement.BATTERY_UNRESTRICTED).isEmpty())
    }
}
