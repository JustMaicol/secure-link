package com.senda.core.node

import kotlinx.coroutines.flow.StateFlow

// spec 107, sección 2 y 5 · Cuenta regresiva de la alerta automática (la muestra la 108).
sealed interface CountdownState {
    data object Idle : CountdownState

    data class Counting(val remainingMs: Long) : CountdownState

    data object WaitingLocation : CountdownState

    data object Sent : CountdownState

    data object Cancelled : CountdownState

    /** No se pudo guardar tras autoSaveRetryMax reintentos. */
    data object Failed : CountdownState
}

interface AutoAlertController {
    val state: StateFlow<CountdownState>

    /** Cancela en Counting o WaitingLocation, sin guardar nada. */
    fun cancel()
}
