package com.senda.features.sendalert

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import com.senda.ui.format.ConnectivityText

// provisional: la spec 108 (ViewModels con LocationSource, MeshStatus y OwnPacketRepository) la reemplaza en la tanda 5.
// Mientras no existan esos contratos, la pestaña guarda el formulario en memoria y "envía" a una pantalla Mi alerta con
// datos de muestra. No guarda nada, no anuncia nada y no es la lógica final: sirve para ver y probar las pantallas.

/** Ubicación de muestra, la del diseño de Figma. */
private val SAMPLE_LOCATION = LocationUi(
    status = LocationStatus.FIX,
    coordinates = "-33.50000, -70.65000",
    accuracyM = 35,
    ageMs = 20_000,
)

private const val HOLD_STEPS = 30

@Composable
fun SendAlertScreen(connectivity: ConnectivityText, modifier: Modifier = Modifier) {
    var form by remember { mutableStateOf(SendAlertFormUiState(location = SAMPLE_LOCATION)) }
    var mine by remember { mutableStateOf<MyAlertUiState?>(null) }
    var pressed by remember { mutableStateOf(false) }

    // Llena el botón mientras se mantiene presionado; soltar antes de tiempo lo vacía y lo explica.
    LaunchedEffect(pressed) {
        if (pressed) {
            form = form.copy(releasedEarly = false)
            for (step in 1..HOLD_STEPS) {
                delay(SEND_HOLD_MS / HOLD_STEPS)
                form = form.copy(holdProgress = step / HOLD_STEPS.toFloat())
            }
            val type = form.selectedType
            if (type != null) {
                mine = sampleAlert(form, type)
                form = SendAlertFormUiState(location = SAMPLE_LOCATION)
            }
            pressed = false
        } else if (form.holdProgress > 0f) {
            form = form.copy(holdProgress = 0f, releasedEarly = true)
        }
    }

    val current = mine
    if (current != null) {
        MyAlertScreen(
            state = current,
            connectivity = connectivity,
            modifier = modifier,
            onSafe = { mine = null },
            onCancelAlert = { mine = null },
        )
    } else {
        SendAlertFormScreen(
            state = form,
            connectivity = connectivity,
            onSelectType = { form = form.copy(selectedType = it, releasedEarly = false) },
            onDecrementPeople = { form = form.copy(people = (form.people - 1).coerceAtLeast(MIN_PEOPLE)) },
            onIncrementPeople = { form = form.copy(people = (form.people + 1).coerceAtMost(MAX_PEOPLE)) },
            onVulnerableChange = { form = form.copy(vulnerable = it) },
            onUpdateLocation = {},
            onEnableLocation = {},
            onHoldStart = { pressed = true },
            onHoldEnd = { pressed = false },
            modifier = modifier,
        )
    }
}

/** Datos de muestra de "Mi alerta" (los de Figma) con lo que se eligió en el formulario. */
private fun sampleAlert(form: SendAlertFormUiState, type: com.senda.core.protocol.EmergencyType) = MyAlertUiState(
    type = type,
    people = form.people,
    vulnerableCount = form.vulnerableCount,
    createdAtText = "09:39",
    reference = "NX-204",
    savedAtText = "09:39",
    echoCount = 2,
    lastEchoAgeMs = 12_000,
    uploadedAtText = null,
    volunteers = 2,
    coordinates = SAMPLE_LOCATION.coordinates.orEmpty(),
    accuracyM = 35,
    locationAgeMs = 120_000,
)
