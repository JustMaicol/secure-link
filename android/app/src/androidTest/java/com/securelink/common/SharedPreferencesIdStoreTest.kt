package com.securelink.common

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// spec 100 · A4.5: PrefsDeviceIdentity con SharedPreferences reales (necesita un teléfono por ADB).
@RunWith(AndroidJUnit4::class)
class SharedPreferencesIdStoreTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    @After
    fun clearPrefs() {
        context.getSharedPreferences(SharedPreferencesIdStore.PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun deviceIdSurvivesANewInstance() {
        var calls = 0
        val first = PrefsDeviceIdentity(SharedPreferencesIdStore(context)) { calls++; 0x1234_5678_9ABCL }.deviceId
        val second = PrefsDeviceIdentity(SharedPreferencesIdStore(context)) { calls++; 0L }.deviceId

        assertEquals(0x1234_5678_9ABCL, first)
        assertEquals(first, second)
        assertEquals(1, calls)
        assertEquals("123456789abc", SharedPreferencesIdStore(context).read())
    }
}
