package com.securelink

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.securelink.core.common.DEVICE_ID_MAX
import com.securelink.core.node.CountdownState
import com.securelink.core.node.MeshHealth
import com.securelink.permissions.missingBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

// spec 100 · A3: la Application real crea un único AppContainer ya cableado (necesita un teléfono por ADB).
@RunWith(AndroidJUnit4::class)
class AppContainerTest {

    private val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as SecureLinkApp

    @Test
    fun applicationExposesASingleContainer() {
        assertSame(app.container, app.container)
    }

    @Test
    fun containerIsWired() {
        val container = app.container

        assertTrue(container.clock.nowMs() > 0)
        assertTrue(container.deviceIdentity.deviceId in 0L..DEVICE_ID_MAX)
        // Provisional hasta A3': el fake lo concede todo.
        assertTrue(missingBlocking(container.permissionChecker.check()).isEmpty())
    }

    @Test
    fun containerExposesTheMeshContracts() {
        val container = app.container

        assertEquals(MeshHealth.RUNNING, container.meshStatus.state.value.health)
        assertTrue(container.meshControl.enabled.value)
        assertEquals(CountdownState.Idle, container.autoAlertController.state.value)
    }
}
