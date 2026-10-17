package com.senda.service

import org.junit.Assert.assertEquals
import org.junit.Test

// spec 107, sección 6 · P2 lee estos valores en la navegación: no pueden cambiar sin avisar.
class NotificationRoutesTest {

    @Test
    fun `extras and routes keep the names the spec publishes`() {
        assertEquals("route", NotificationRoutes.EXTRA_ROUTE)
        assertEquals("alert_key", NotificationRoutes.EXTRA_ALERT_KEY)
        assertEquals("nearby_detail", NotificationRoutes.ROUTE_NEARBY_DETAIL)
        assertEquals("my_alert", NotificationRoutes.ROUTE_MY_ALERT)
        assertEquals("countdown", NotificationRoutes.ROUTE_COUNTDOWN)
    }
}
