package com.securelink.gallery

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
import com.securelink.core.protocol.EmergencyType
import com.securelink.ui.components.BottomNavItem
import com.securelink.ui.components.ButtonStyle
import com.securelink.ui.components.CardStyle
import com.securelink.ui.components.ConnectivityStrip
import com.securelink.ui.components.DemoBadge
import com.securelink.ui.components.EmergencyBadge
import com.securelink.ui.components.InfoRow
import com.securelink.ui.components.QuantityStepper
import com.securelink.ui.components.ScreenScaffold
import com.securelink.ui.components.SendaBottomBar
import com.securelink.ui.components.SendaButton
import com.securelink.ui.components.SendaCard
import com.securelink.ui.components.SendaCheckbox
import com.securelink.ui.components.SendaIcon
import com.securelink.ui.components.SendaIcons
import com.securelink.ui.components.SendaSwitch
import com.securelink.ui.components.SendaTextButton
import com.securelink.ui.components.SimulationBadge
import com.securelink.ui.components.SmallActionButton
import com.securelink.ui.format.ConnectivityText
import com.securelink.ui.theme.Senda
import com.securelink.ui.theme.SendaText
import com.securelink.ui.theme.SendaTheme
import com.securelink.ui.theme.UiTone

/**
 * Galería de depuración: tema, íconos y componentes del diseño, para compararlos con Figma en el teléfono.
 * `adb shell am start -n com.securelink/.gallery.GalleryActivity --ez dark true` fuerza el tema oscuro.
 */
class GalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val forcedDark: Boolean? = if (intent.hasExtra(EXTRA_DARK)) intent.getBooleanExtra(EXTRA_DARK, false) else null
        setContent {
            // Arranca con el tema del teléfono (o el extra "dark") y el interruptor de la pantalla lo cambia.
            val systemDark = isSystemInDarkTheme()
            var dark by rememberSaveable { mutableStateOf(forcedDark ?: systemDark) }
            LaunchedEffect(dark) { applySystemBars(dark) }
            SendaTheme(darkTheme = dark) { GalleryScreen(dark = dark, onDarkChange = { dark = it }) }
        }
    }

    /** Los íconos de la barra de estado y de navegación siguen al tema de la galería, no al del teléfono. */
    private fun applySystemBars(dark: Boolean) {
        val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    companion object {
        const val EXTRA_DARK = "dark"
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
