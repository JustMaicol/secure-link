package com.senda.features.nearby

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.senda.core.protocol.EmergencyType
import com.senda.ui.format.ConnectivityText

// provisional: la spec 109 (ViewModels con las alertas guardadas, el GPS y la brújula) la reemplaza en la tanda 5.
// Mientras no existan esos contratos, la pestaña muestra alertas de muestra (las de Figma) y navega entre la lista y el
// detalle con estado local. "Voy en camino" y "Abrir en Google Maps" todavía no hacen nada.

/** Alertas de muestra, ordenadas por distancia: las tres del diseño de Figma. */
internal val SampleNearbyAlerts = listOf(
    NearbyAlertUi(
        id = "a1b2c3d4e5f6-0004", reference = "NX-204", type = EmergencyType.MEDICAL, receivedAgeMs = 120_000,
        coordinates = "-33.49715, -70.64950", people = 3, vulnerableCount = 1, volunteers = 2,
        distanceM = 320.0, bearingDeg = 45.0, accuracyM = 35, locationAgeMs = 120_000,
    ),
    NearbyAlertUi(
        id = "0f1e2d3c4b5a-0002", reference = "NX-198", type = EmergencyType.FOOD_WATER, receivedAgeMs = 360_000,
        coordinates = "-33.50020, -70.64620", people = 2, vulnerableCount = 0, volunteers = 1,
        distanceM = 640.0, bearingDeg = 90.0, accuracyM = 50, locationAgeMs = 360_000,
    ),
    NearbyAlertUi(
        id = "9a8b7c6d5e4f-0001", reference = "NX-187", type = EmergencyType.TRAPPED_OR_SEVERE, receivedAgeMs = 540_000,
        coordinates = "-33.50350, -70.65830", people = 5, vulnerableCount = 2, volunteers = 0,
        distanceM = 980.0, bearingDeg = 225.0, accuracyM = 40, locationAgeMs = 540_000,
    ),
)

@Composable
fun NearbyScreen(connectivity: ConnectivityText, modifier: Modifier = Modifier) {
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val state = remember { NearbyListUiState(alerts = SampleNearbyAlerts, updatedAgeMs = 12_000, demoData = true) }
    val selected = state.alerts.firstOrNull { it.id == selectedId }

    // Atrás vuelve de un detalle a la lista, no sale de la app.
    BackHandler(enabled = selected != null) { selectedId = null }

    if (selected != null) {
        NearbyDetailScreen(
            alert = selected,
            connectivity = connectivity,
            onBack = { selectedId = null },
            onOnMyWay = {},
            onOpenMaps = {},
            modifier = modifier,
        )
    } else {
        NearbyListScreen(state = state, connectivity = connectivity, onOpenAlert = { selectedId = it.id }, modifier = modifier)
    }
}
