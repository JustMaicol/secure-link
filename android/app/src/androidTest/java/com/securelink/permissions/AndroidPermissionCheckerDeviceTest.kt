package com.securelink.permissions

import android.Manifest
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// spec 100, sección 5.2 · AndroidPermissionChecker contra el sistema real (necesita un teléfono por ADB).
@RunWith(AndroidJUnit4::class)
class AndroidPermissionCheckerDeviceTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext
    private val checker = AndroidPermissionChecker(context)

    private val runtimePermissions = listOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS,
    )

    @Before
    fun resetHistory() {
        context.getSharedPreferences(SharedPreferencesPermissionHistory.PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
    }

    @After
    fun cleanHistory() = resetHistory()

    @Test
    fun answersEveryRequirementWithoutThrowing() {
        // Con o sin permisos concedidos: leer el estado no debe lanzar (p. ej. SecurityException de Bluetooth).
        assertEquals(Requirement.entries.toSet(), checker.check().keys)
    }

    @Test
    fun grantedRuntimePermissionsAreReported() {
        val ui = instrumentation.uiAutomation
        // Solo se concede: revocar mataría el proceso de la propia prueba. El desinstalar al final lo deja limpio.
        runtimePermissions.forEach { ui.grantRuntimePermission(context.packageName, it) }

        val state = checker.check()

        assertTrue(state.getValue(Requirement.BLUETOOTH))
        assertTrue(state.getValue(Requirement.LOCATION))
        assertTrue(state.getValue(Requirement.NOTIFICATIONS))
    }

    @Test
    fun systemSwitchesAreConsistentWithTheirOwnApis() {
        val state = checker.check()
        val platform = AndroidPlatformState(context)

        assertEquals(platform.bluetoothEnabled(), state.getValue(Requirement.BLUETOOTH_ON))
        assertEquals(platform.locationEnabled(), state.getValue(Requirement.LOCATION_ON))
        assertEquals(platform.ignoringBatteryOptimizations(), state.getValue(Requirement.BATTERY_UNRESTRICTED))
    }

    @Test
    fun historyPersistsInSharedPreferences() {
        assertFalse(SharedPreferencesPermissionHistory(context).wasRequested(Requirement.LOCATION))

        SharedPreferencesPermissionHistory(context).markRequested(Requirement.LOCATION)

        val second = SharedPreferencesPermissionHistory(context)
        assertTrue(second.wasRequested(Requirement.LOCATION))
        assertFalse(second.wasRequested(Requirement.BLUETOOTH))
    }
}
