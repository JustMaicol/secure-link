package com.senda.permissions

// spec 100, sección 12 · Lo que dibuja la pantalla de permisos. El mapeo desde los requisitos es puro: sin Android,
// sin textos (los pone la pantalla desde strings.xml) y probado en la JVM.

/** Qué fila es. Cada una corresponde a un [Requirement], pero el diseño las nombra distinto. */
enum class PermissionRowKind(val requirement: Requirement, val inFigma: Boolean) {
    /** Figma: "Bluetooth" (el adaptador está encendido). */
    BLUETOOTH(Requirement.BLUETOOTH_ON, inFigma = true),

    /** Figma: "Dispositivos cercanos" (los tres permisos de Bluetooth). */
    NEARBY(Requirement.BLUETOOTH, inFigma = true),

    /** Figma: "Ubicación". */
    LOCATION(Requirement.LOCATION, inFigma = true),

    /** No está en Figma: el interruptor de ubicación del sistema. Solo aparece si está apagado. */
    LOCATION_SERVICES(Requirement.LOCATION_ON, inFigma = false),

    /** Figma: "Notificaciones". */
    NOTIFICATIONS(Requirement.NOTIFICATIONS, inFigma = true),

    /** No está en Figma: recomendada. */
    BACKGROUND_LOCATION(Requirement.BACKGROUND_LOCATION, inFigma = false),

    /** No está en Figma: recomendada. */
    BATTERY(Requirement.BATTERY_UNRESTRICTED, inFigma = false),
}

/** Lo que hace el botón de una fila al tocarlo. */
enum class PermissionAction {
    /** Diálogo del sistema con los permisos del requisito. */
    REQUEST,
    OPEN_APP_SETTINGS,
    ENABLE_BLUETOOTH,
    OPEN_BLUETOOTH_SETTINGS,
    OPEN_LOCATION_SETTINGS,
    REQUEST_BATTERY_EXCLUSION,
    OPEN_BATTERY_SETTINGS,
}

data class PermissionRowUi(
    val kind: PermissionRowKind,
    val state: RowState,
    val action: PermissionAction,
    /** Falso cuando falta algo previo: la ubicación en segundo plano exige la ubicación normal. */
    val actionEnabled: Boolean,
)

data class PermissionsUiState(
    /** Las filas del diseño, en su orden, más la ubicación del sistema si está apagada. */
    val rows: List<PermissionRowUi>,
    /** Sección "Recomendado": no bloquean el paso. */
    val recommended: List<PermissionRowUi>,
    /** Mostrar el paso informativo de Samsung (hasta que el usuario toque "Ya lo hice"). */
    val showSamsungStep: Boolean,
    /** `missingBlocking(...)` vacío: "Continuar" habilitado. */
    val canContinue: Boolean,
    /** Ya había pasado la pantalla y ahora falta un requisito bloqueante: se perdió un permiso. */
    val showLostPermissionBanner: Boolean,
)

/**
 * @param check estado de cada requisito (`PermissionChecker.check()`).
 * @param rowStates `GRANTED`, `MISSING` o `DENIED_PERMANENTLY` por requisito (`rowStateOf`).
 */
fun buildPermissionsState(
    check: Map<Requirement, Boolean>,
    rowStates: Map<Requirement, RowState>,
    samsungAcknowledged: Boolean,
    onboardingDone: Boolean,
): PermissionsUiState {
    fun row(kind: PermissionRowKind): PermissionRowUi {
        val requirement = kind.requirement
        val state = rowStates[requirement] ?: if (check[requirement] == true) RowState.GRANTED else RowState.MISSING
        val prerequisite = prerequisiteOf(requirement)
        return PermissionRowUi(
            kind = kind,
            state = state,
            action = actionFor(requirement, state, check),
            actionEnabled = prerequisite == null || check[prerequisite] == true,
        )
    }

    val locationServicesOn = check[Requirement.LOCATION_ON] == true
    val rows = buildList {
        add(row(PermissionRowKind.BLUETOOTH))
        add(row(PermissionRowKind.NEARBY))
        add(row(PermissionRowKind.LOCATION))
        if (!locationServicesOn) add(row(PermissionRowKind.LOCATION_SERVICES))
        add(row(PermissionRowKind.NOTIFICATIONS))
    }
    val canContinue = missingBlocking(check).isEmpty()
    return PermissionsUiState(
        rows = rows,
        recommended = listOf(row(PermissionRowKind.BACKGROUND_LOCATION), row(PermissionRowKind.BATTERY)),
        showSamsungStep = !samsungAcknowledged,
        canContinue = canContinue,
        showLostPermissionBanner = onboardingDone && !canContinue,
    )
}

private fun actionFor(requirement: Requirement, state: RowState, check: Map<Requirement, Boolean>): PermissionAction =
    when (requirement) {
        // Encender el adaptador desde la app exige el permiso de Bluetooth; sin él se manda a los ajustes.
        Requirement.BLUETOOTH_ON -> when {
            state == RowState.GRANTED -> PermissionAction.OPEN_BLUETOOTH_SETTINGS
            check[Requirement.BLUETOOTH] == true -> PermissionAction.ENABLE_BLUETOOTH
            else -> PermissionAction.OPEN_BLUETOOTH_SETTINGS
        }
        Requirement.LOCATION_ON -> PermissionAction.OPEN_LOCATION_SETTINGS
        Requirement.BATTERY_UNRESTRICTED ->
            if (state == RowState.GRANTED) PermissionAction.OPEN_BATTERY_SETTINGS else PermissionAction.REQUEST_BATTERY_EXCLUSION
        // Permisos en tiempo de ejecución: pedirlos si faltan; si ya están (o se negaron para siempre), a los ajustes.
        else -> if (state == RowState.MISSING) PermissionAction.REQUEST else PermissionAction.OPEN_APP_SETTINGS
    }
