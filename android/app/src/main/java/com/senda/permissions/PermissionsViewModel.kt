package com.senda.permissions

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** spec 100 · Estado de la pantalla de permisos. Se vuelve a calcular al volver a la app (`refresh`). */
class PermissionsViewModel(private val model: PermissionsModel) : ViewModel() {

    private val mutableState = MutableStateFlow(model.state(showsRationale = { false }))
    val state: StateFlow<PermissionsUiState> = mutableState.asStateFlow()

    /** `true` cuando el usuario ya pasó la pantalla con "Continuar" y no falta nada bloqueante. */
    private val mutablePassed = MutableStateFlow(model.onboardingDone)
    val passed: StateFlow<Boolean> = mutablePassed.asStateFlow()

    /** Al abrir la app y en cada `onResume`: el usuario pudo cambiar permisos en los ajustes. */
    fun refresh(showsRationale: (String) -> Boolean) {
        mutableState.value = model.state(showsRationale)
    }

    fun onRequestStarted(requirement: Requirement) = model.markRequested(requirement)

    fun onSamsungAcknowledged(showsRationale: (String) -> Boolean) {
        model.acknowledgeSamsung()
        refresh(showsRationale)
    }

    /** "Continuar": solo vale si no falta ningún requisito bloqueante. */
    fun onContinue() {
        if (!state.value.canContinue) return
        model.completeOnboarding()
        mutablePassed.value = true
    }
}
