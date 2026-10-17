package com.securelink.core.node

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// spec 107 · AutoAlertController para pantallas y pruebas. El estado cambia con emit o con cancel.
class FakeAutoAlertController(initial: CountdownState = CountdownState.Idle) : AutoAlertController {
    private val mutableState = MutableStateFlow(initial)
    override val state: StateFlow<CountdownState> = mutableState.asStateFlow()

    var cancelCalls: Int = 0
        private set

    fun emit(state: CountdownState) {
        mutableState.value = state
    }

    /** Igual que el real: solo cancela mientras cuenta o espera la ubicación. */
    override fun cancel() {
        cancelCalls++
        val current = mutableState.value
        if (current is CountdownState.Counting || current is CountdownState.WaitingLocation) {
            mutableState.value = CountdownState.Cancelled
        }
    }
}
