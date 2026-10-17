package com.senda.service

// spec 107, sección 6 · Extras del Intent con que una notificación abre MainActivity.
// La 107 los publica; la navegación (100-B, P2) los lee y abre la pantalla.
object NotificationRoutes {
    /** Destino: uno de los ROUTE_*. */
    const val EXTRA_ROUTE = "route"

    /** Clave canónica de la alerta (PacketKey.canonical), cuando el destino es una alerta. */
    const val EXTRA_ALERT_KEY = "alert_key"

    /** Detalle de una alerta cercana (pantalla C2). Lleva EXTRA_ALERT_KEY. */
    const val ROUTE_NEARBY_DETAIL = "nearby_detail"

    /** "Mi alerta" (pantalla E2). */
    const val ROUTE_MY_ALERT = "my_alert"

    /** Cuenta regresiva de la alerta automática (pantalla E3). */
    const val ROUTE_COUNTDOWN = "countdown"
}
