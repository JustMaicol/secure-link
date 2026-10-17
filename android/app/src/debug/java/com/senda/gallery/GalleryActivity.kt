package com.senda.gallery

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.senda.core.protocol.EmergencyType
import com.senda.core.protocol.Vulnerable
import com.senda.features.nearby.NearbyDetailScreen
import com.senda.features.nearby.NearbyListScreen
import com.senda.features.nearby.NearbyListUiState
import com.senda.features.nearby.NearbyScreen
import com.senda.features.nearby.SampleNearbyAlerts
import com.senda.features.sendalert.AutoAlertCountdown
import com.senda.features.sendalert.AutoAlertCountdownUiState
import com.senda.features.sendalert.LocationStatus
import com.senda.features.sendalert.LocationUi
import com.senda.features.sendalert.MyAlertScreen
import com.senda.features.sendalert.MyAlertUiState
import com.senda.features.sendalert.SendAlertFormScreen
import com.senda.features.sendalert.SendAlertFormUiState
import com.senda.features.status.StatusContent
import com.senda.features.status.StatusDebugUiState
import com.senda.features.status.StatusUiState
import com.senda.ui.components.BottomNavItem
import com.senda.ui.components.ButtonStyle
import com.senda.ui.components.CardStyle
import com.senda.ui.components.ConfirmDialog
import com.senda.ui.components.ConnectivityStrip
import com.senda.ui.components.DemoBadge
import com.senda.ui.components.EmergencyBadge
import com.senda.ui.components.EmptyState
import com.senda.ui.components.InfoRow
import com.senda.ui.components.MessageBanner
import com.senda.ui.components.QuantityStepper
import com.senda.ui.components.ScreenScaffold
import com.senda.ui.components.SendaBottomBar
import com.senda.ui.components.SendaButton
import com.senda.ui.components.SendaCard
import com.senda.ui.components.SendaCheckbox
import com.senda.ui.components.SendaIcon
import com.senda.ui.components.SendaIcons
import com.senda.ui.components.SendaSwitch
import com.senda.ui.components.SendaTextButton
import com.senda.ui.components.SimulationBadge
import com.senda.ui.components.StatusChip
import com.senda.ui.components.SmallActionButton
import com.senda.ui.format.ConnectivityText
import com.senda.ui.theme.Senda
import com.senda.ui.theme.SendaText
import com.senda.ui.theme.SendaTheme
import com.senda.ui.theme.UiTone

/**
 * Galería de depuración: tema, íconos y componentes del diseño, para compararlos con Figma en el teléfono.
 * `adb shell am start -n com.senda/.gallery.GalleryActivity --ez dark true` fuerza el tema oscuro.
 */
class GalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val forcedDark: Boolean? = if (intent.hasExtra(EXTRA_DARK)) intent.getBooleanExtra(EXTRA_DARK, false) else null
        val frame: String? = intent.getStringExtra(EXTRA_FRAME)
        setContent {
            // Arranca con el tema del teléfono (o el extra "dark") y el interruptor de la pantalla lo cambia.
            val systemDark = isSystemInDarkTheme()
            var dark by rememberSaveable { mutableStateOf(forcedDark ?: systemDark) }
            LaunchedEffect(dark) { applySystemBars(dark) }
            SendaTheme(darkTheme = dark) {
                if (frame != null) Frame(frame) else GalleryScreen(dark = dark, onDarkChange = { dark = it })
            }
        }
    }

    /** Los íconos de la barra de estado y de navegación siguen al tema de la galería, no al del teléfono. */
    private fun applySystemBars(dark: Boolean) {
        val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    companion object {
        const val EXTRA_DARK = "dark"

        /** `--es frame e1|e1-sheet|e1-empty|e2|e2-confirm|e3|c1|c1-empty|c2|c2-unknown|s1|s1-stopped`: una pantalla completa con datos de prueba (ver Frames.kt). */
        const val EXTRA_FRAME = "frame"
    }
}

@Composable
private fun GalleryScreen(dark: Boolean, onDarkChange: (Boolean) -> Unit) {
    ScreenScaffold(
        title = "Galería",
        subtitle = "Tema, íconos y componentes del diseño",
        connectivity = ConnectivityText("Malla activa", UiTone.SUCCESS, "Sin internet"),
        verticalSpacing = 24.dp,
    ) {
        SendaCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Tema oscuro", style = SendaText.label, color = Senda.colors.textPrimary)
                    Text("Cambia entre el tema claro y el oscuro del diseño", style = SendaText.caption, color = Senda.colors.textSecondary)
                }
                SendaSwitch(checked = dark, onCheckedChange = onDarkChange)
            }
        }
        Section("Paleta") { Palette() }
        Section("Tipografía") { Typography() }
        Section("Íconos") { Icons() }
        Section("Conectividad") { ConnectivityStrips() }
        Section("Insignias, avisos y estados vacíos") { Feedback() }
        Section("Tarjetas y niveles") { CardsAndLevels() }
        Section("Botones") { Buttons() }
        Section("Controles") { Controls() }
        Section("Barra inferior") { BottomBar() }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title.uppercase(), style = SendaText.captionBold, color = Senda.colors.accent)
        content()
    }
}

@Composable
private fun Palette() {
    val c = Senda.colors
    val swatches = listOf(
        "background" to c.background, "surface" to c.surface, "surfaceMuted" to c.surfaceMuted,
        "surfaceTint" to c.surfaceTint, "border" to c.border, "textPrimary" to c.textPrimary,
        "textSecondary" to c.textSecondary, "accent" to c.accent, "accentStrong" to c.accentStrong,
        "success" to c.success, "successContainer" to c.successContainer, "warning" to c.warning,
        "warningContainer" to c.warningContainer, "actionContainer" to c.actionContainer,
        "onActionContainer" to c.onActionContainer, "actionBorder" to c.actionBorder,
        "warningAction" to c.warningAction, "scrim" to c.scrim,
        "yellow.content" to c.yellow.content, "yellow.container" to c.yellow.container,
        "orange.content" to c.orange.content, "orange.container" to c.orange.container,
        "red.content" to c.red.content, "red.container" to c.red.container,
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        swatches.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                pair.forEach { (name, color) ->
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(28.dp)
                                .background(color, RoundedCornerShape(6.dp))
                                .border(1.dp, c.border, RoundedCornerShape(6.dp)),
                        )
                        Text(name, style = SendaText.small, color = c.textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun Typography() {
    val styles: List<Pair<String, TextStyle>> = listOf(
        "title 29 B" to SendaText.title, "headline 25 B" to SendaText.headline,
        "countdown 48 SB" to SendaText.countdown, "figureLarge 34 B" to SendaText.figureLarge,
        "figure 25 B" to SendaText.figure, "counter 24 B" to SendaText.counter,
        "distance 20 B" to SendaText.distance, "cardTitle 16 B" to SendaText.cardTitle,
        "button 16 SB" to SendaText.button, "sectionTitle 15 B" to SendaText.sectionTitle,
        "bodyLarge 15" to SendaText.bodyLarge, "subtitle 14" to SendaText.subtitle,
        "label 14 B" to SendaText.label, "labelSemiBold 14 SB" to SendaText.labelSemiBold,
        "labelMedium 14 M" to SendaText.labelMedium, "body13" to SendaText.body13,
        "body13Medium" to SendaText.body13Medium, "body13SemiBold" to SendaText.body13SemiBold,
        "body13Bold" to SendaText.body13Bold, "caption 12" to SendaText.caption,
        "captionMedium" to SendaText.captionMedium, "captionSemiBold" to SendaText.captionSemiBold,
        "captionBold" to SendaText.captionBold, "small 11" to SendaText.small,
        "smallBold" to SendaText.smallBold, "tiny 10" to SendaText.tiny, "tinyBold" to SendaText.tinyBold,
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        styles.forEach { (name, style) -> Text("Aa Áé 0123 · $name", style = style, color = Senda.colors.textPrimary) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Icons() {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SendaIcons.all.forEach { (name, icon) ->
            Column(Modifier.width(72.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                SendaIcon(icon, size = 24.dp, tint = Senda.colors.textPrimary)
                Text(name, style = SendaText.tiny, color = Senda.colors.textSecondary)
            }
        }
    }
}

@Composable
private fun ConnectivityStrips() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ConnectivityStrip("Malla activa", UiTone.SUCCESS, "Sin internet")
        ConnectivityStrip("Bluetooth apagado", UiTone.WARNING, "Con internet")
        ConnectivityStrip("Faltan permisos", UiTone.ERROR, "Sin internet")
    }
}

@Composable
private fun CardsAndLevels() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EmergencyBadge(EmergencyType.MEDICAL, "Nivel naranja")
            EmergencyBadge(EmergencyType.FOOD_WATER, "Nivel amarillo")
            EmergencyBadge(EmergencyType.TRAPPED_OR_SEVERE, "Nivel rojo")
        }
        SendaCard { Text("Tarjeta Default", style = SendaText.label); InfoRow("Personas", "3 · 1 vulnerable") }
        SendaCard(style = CardStyle.Muted) { Text("Tarjeta Muted", style = SendaText.label) }
        SendaCard(style = CardStyle.Tinted) { Text("Tarjeta Tinted", style = SendaText.label) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            DemoBadge()
            SimulationBadge()
        }
    }
}

@Composable
private fun Buttons() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SendaButton("Continuar", onClick = {}, icon = SendaIcons.ArrowRight)
        SendaButton("Abrir en Google Maps", onClick = {}, style = ButtonStyle.Secondary, icon = SendaIcons.Map)
        SendaButton("Estoy a salvo", onClick = {}, style = ButtonStyle.Success, icon = SendaIcons.ShieldCheck)
        SendaButton("Cancelar alerta automática", onClick = {}, style = ButtonStyle.Warning, icon = SendaIcons.X)
        SendaButton("Deshabilitado", onClick = {}, enabled = false)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            SmallActionButton("Revisar", onClick = {})
            SmallActionButton("Permitir", onClick = {}, filled = true)
            SendaTextButton("Cancelar alerta", onClick = {})
        }
    }
}

@Composable
private fun Controls() {
    var switchOn by remember { mutableStateOf(true) }
    var checked by remember { mutableStateOf(true) }
    var people by remember { mutableIntStateOf(3) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            SendaSwitch(checked = switchOn, onCheckedChange = { switchOn = it })
            SendaSwitch(checked = !switchOn, onCheckedChange = { switchOn = !it })
            SendaCheckbox(checked = checked, onCheckedChange = { checked = it })
            SendaCheckbox(checked = !checked, onCheckedChange = { checked = !it })
        }
        QuantityStepper(
            value = people,
            onDecrement = { people-- },
            onIncrement = { people++ },
            canDecrement = people > 1,
            canIncrement = people < 255,
        )
    }
}

@Composable
private fun BottomBar() {
    var selected by remember { mutableIntStateOf(0) }
    SendaBottomBar(
        items = listOf(
            BottomNavItem("Enviar alerta", SendaIcons.RadioTower),
            BottomNavItem("Cercanas", SendaIcons.Radar),
            BottomNavItem("Estado", SendaIcons.Activity),
        ),
        selectedIndex = selected,
        onSelect = { selected = it },
    )
}

@Composable
private fun Feedback() {
    var dialog by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip("Información", UiTone.INFO, icon = SendaIcons.Radio)
            StatusChip("Listo", UiTone.SUCCESS, icon = SendaIcons.Check)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip("Atención", UiTone.WARNING, icon = SendaIcons.TriangleAlert)
            StatusChip("Error", UiTone.ERROR, icon = SendaIcons.X)
            StatusChip("Simulación", UiTone.SIMULATION)
        }
        MessageBanner("Información de una acción en curso.", UiTone.INFO)
        MessageBanner("Algo necesita tu atención.", UiTone.WARNING, actionLabel = "Ir a Ajustes", onAction = {})
        MessageBanner("No se pudo completar la acción.", UiTone.ERROR)
        SendaCard { EmptyState(SendaIcons.Radar, "Nada por aquí", body = "Cuando haya alertas cercanas aparecerán en esta lista.") }
        SendaButton("Abrir diálogo de confirmación", onClick = { dialog = true }, style = ButtonStyle.Secondary)
    }
    if (dialog) {
        ConfirmDialog(
            title = "Cancelar alerta",
            body = "¿Cancelar la alerta? Úsalo si la enviaste por error. No se puede deshacer.",
            confirmLabel = "Cancelar alerta",
            dismissLabel = "Volver",
            onConfirm = { dialog = false },
            onDismiss = { dialog = false },
            confirmStyle = ButtonStyle.Warning,
        )
    }
}

/** Pantallas completas con datos de prueba, para compararlas con Figma en el teléfono (spec 108, U7). */
@Composable
private fun Frame(name: String) {
    val connectivity = ConnectivityText("Malla activa", UiTone.SUCCESS, "Sin internet")
    val location = LocationUi(LocationStatus.FIX, coordinates = "-33.50000, -70.65000", accuracyM = 35, ageMs = 20_000)
    val form = SendAlertFormUiState(
        selectedType = EmergencyType.MEDICAL,
        people = 3,
        vulnerable = Vulnerable(children = true),
        location = location,
    )
    when (name) {
        "e1", "e1-sheet" -> SendAlertFormScreen(
            state = form, connectivity = connectivity, onSelectType = {}, onDecrementPeople = {}, onIncrementPeople = {},
            onVulnerableChange = {}, onUpdateLocation = {}, onEnableLocation = {}, onHoldStart = {}, onHoldEnd = {},
            initialSheetOpen = name == "e1-sheet",
        )
        "e1-empty" -> SendAlertFormScreen(
            state = SendAlertFormUiState(location = LocationUi(LocationStatus.SEARCHING)),
            connectivity = connectivity, onSelectType = {}, onDecrementPeople = {}, onIncrementPeople = {},
            onVulnerableChange = {}, onUpdateLocation = {}, onEnableLocation = {}, onHoldStart = {}, onHoldEnd = {},
        )
        "e2", "e2-confirm" -> MyAlertScreen(
            state = MyAlertUiState(
                type = EmergencyType.MEDICAL, people = 3, vulnerableCount = 1, createdAtText = "09:39", reference = "NX-204",
                savedAtText = "09:39", echoCount = 2, lastEchoAgeMs = 12_000, uploadedAtText = null, volunteers = 2,
                coordinates = "-33.50000, -70.65000", accuracyM = 35, locationAgeMs = 120_000,
            ),
            connectivity = connectivity, onSafe = {}, onCancelAlert = {}, initialConfirm = name == "e2-confirm",
        )
        "c1" -> NearbyListScreen(
            NearbyListUiState(SampleNearbyAlerts, updatedAgeMs = 12_000, demoData = true), connectivity, onOpenAlert = {},
        )
        "c1-empty" -> NearbyListScreen(NearbyListUiState(), connectivity, onOpenAlert = {})
        "c2" -> NearbyDetailScreen(SampleNearbyAlerts.first(), connectivity, onBack = {}, onOnMyWay = {}, onOpenMaps = {})
        "c2-unknown" -> NearbyDetailScreen(
            SampleNearbyAlerts.first().copy(distanceM = null, bearingDeg = null), connectivity, onBack = {}, onOnMyWay = {}, onOpenMaps = {},
        )
        "s1", "s1-stopped" -> StatusContent(
            state = StatusUiState(
                meshActive = name == "s1", announcementsActive = 12, neighbors = 4, internetOnline = false, pendingUploads = 1,
                lastUploadClock = "09:12", lastUploadAgeMs = 29 * 60_000L, deviceId = "NX-PXL-7A2F", relayTag = "R7-K2",
                motionEnabled = name == "s1", motionCountdownSeconds = 15,
                debug = StatusDebugUiState(blockedTags = listOf("TEST-04"), motionReadingG = 0.18f),
            ),
            connectivity = connectivity, onCopyId = {}, onMotionEnabledChange = {}, onReviewPermissions = {}, onRemoveTag = {},
            onAddTag = {}, onSimulateShake = {},
        )
        // La capa va sobre la pestaña en la que esté el usuario (en el diseño, Cercanas).
        "e3" -> Box {
            NearbyScreen(connectivity)
            AutoAlertCountdown(AutoAlertCountdownUiState(remainingMs = 15_000), onCancel = {})
        }
    }
}
