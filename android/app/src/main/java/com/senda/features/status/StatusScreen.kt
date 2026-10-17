package com.senda.features.status

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.senda.BuildConfig
import com.senda.ui.format.ConnectivityText

// provisional: la spec 109 (ViewModel con MeshStatus, SyncStatus, DeviceIdentity y los ajustes) la reemplaza en la
// tanda 5. Mientras no existan esos contratos, la pestaña muestra datos de muestra (los de Figma) con estado local.
// "Revisar permisos" y "+ Añadir" todavía no hacen nada; "Simular movimiento brusco" lanza la cuenta regresiva de prueba.

private val SampleStatus = StatusUiState(
    meshActive = true,
    announcementsActive = 12,
    neighbors = 4,
    internetOnline = false,
    pendingUploads = 1,
    lastUploadClock = "09:12",
    lastUploadAgeMs = 29 * 60_000L,
    deviceId = "NX-PXL-7A2F",
    relayTag = "R7-K2",
    motionEnabled = true,
    motionCountdownSeconds = 15,
    // Las herramientas de simulación solo existen en la versión de depuración.
    debug = if (BuildConfig.DEBUG) StatusDebugUiState(blockedTags = listOf("TEST-04"), motionReadingG = 0.18f) else null,
)

@Composable
fun StatusScreen(
    connectivity: ConnectivityText,
    modifier: Modifier = Modifier,
    onSimulateShake: () -> Unit = {},
) {
    val context = LocalContext.current
    var state by remember { mutableStateOf(SampleStatus) }
    StatusContent(
        state = state,
        connectivity = connectivity,
        onCopyId = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("ID", state.deviceId))
        },
        onMotionEnabledChange = { state = state.copy(motionEnabled = it) },
        onReviewPermissions = {},
        onRemoveTag = { tag -> state = state.copy(debug = state.debug?.copy(blockedTags = state.debug!!.blockedTags - tag)) },
        onAddTag = {},
        onSimulateShake = onSimulateShake,
        modifier = modifier,
    )
}
