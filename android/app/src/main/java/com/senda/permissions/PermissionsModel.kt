package com.senda.permissions

/**
 * Junta lo que la pantalla de permisos necesita saber (spec 100, sección 5.3): el estado de cada requisito, el
 * historial de negaciones y los dos datos guardados del flujo. Sin Android: se prueba en la JVM con fakes.
 */
class PermissionsModel(
    private val checker: PermissionChecker,
    private val platform: PlatformState,
    private val history: PermissionRequestHistory,
    private val flow: PermissionFlowStore,
) {
    /**
     * @param showsRationale `shouldShowRequestPermissionRationale` por permiso; necesita una Activity, por eso lo
     * pasa quien llama.
     */
    fun state(showsRationale: (String) -> Boolean): PermissionsUiState {
        val check = checker.check()
        val rowStates = Requirement.entries.associateWith { requirement ->
            rowStateOf(
                requirement = requirement,
                granted = check[requirement] == true,
                requestedBefore = history.wasRequested(requirement),
                isGranted = platform::isGranted,
                showsRationale = showsRationale,
            )
        }
        return buildPermissionsState(check, rowStates, flow.samsungAcknowledged, flow.onboardingDone)
    }

    /** Justo antes de abrir el diálogo del sistema: Android no distingue "nunca pedido" de "negado para siempre". */
    fun markRequested(requirement: Requirement) = history.markRequested(requirement)

    fun acknowledgeSamsung() {
        flow.samsungAcknowledged = true
    }

    /** El usuario pasó la pantalla con "Continuar". */
    fun completeOnboarding() {
        flow.onboardingDone = true
    }

    val onboardingDone: Boolean get() = flow.onboardingDone
}
