package com.senda.features.sendalert

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.senda.R
import com.senda.core.protocol.EmergencyType
import com.senda.core.protocol.Vulnerable
import com.senda.ui.components.CardStyle
import com.senda.ui.components.QuantityStepper
import com.senda.ui.components.ScreenScaffold
import com.senda.ui.components.SendaCard
import com.senda.ui.components.SendaCheckbox
import com.senda.ui.components.SendaIcon
import com.senda.ui.components.SendaIcons
import com.senda.ui.format.ConnectivityText
import com.senda.ui.format.classifyElapsed
import com.senda.ui.format.elapsedText
import com.senda.ui.format.emergencyOptionText
import com.senda.ui.theme.Dimens
import com.senda.ui.theme.Senda
import com.senda.ui.theme.SendaText
import com.senda.ui.theme.UiTone

private val LEVELS = listOf(EmergencyType.FOOD_WATER, EmergencyType.MEDICAL, EmergencyType.TRAPPED_OR_SEVERE)

/**
 * E1 · Formulario de alerta (Figma). Sin estado propio: dibuja [state] y avisa de cada acción. Lo único que recuerda
 * es si la hoja de vulnerables está abierta, que es del dibujo y no del negocio.
 */
@Composable
fun SendAlertFormScreen(
    state: SendAlertFormUiState,
    connectivity: ConnectivityText,
    onSelectType: (EmergencyType) -> Unit,
    onDecrementPeople: () -> Unit,
    onIncrementPeople: () -> Unit,
    onVulnerableChange: (Vulnerable) -> Unit,
    onUpdateLocation: () -> Unit,
    onEnableLocation: () -> Unit,
    onHoldStart: () -> Unit,
    onHoldEnd: () -> Unit,
    modifier: Modifier = Modifier,
    initialSheetOpen: Boolean = false,
) {
    var sheetOpen by rememberSaveable { mutableStateOf(initialSheetOpen) }
    ScreenScaffold(
        title = stringResource(R.string.sendalert_title),
        subtitle = stringResource(R.string.sendalert_subtitle),
        modifier = modifier,
        connectivity = connectivity,
        contentPadding = PaddingValues(horizontal = Dimens.screenPadding, vertical = 20.dp),
        verticalSpacing = 18.dp,
        bottomBar = {
            HoldToSendButton(
                progress = state.holdProgress,
                onHoldStart = onHoldStart,
                onHoldEnd = onHoldEnd,
                enabled = state.canSend,
            )
            HintText(state.hint)
        },
    ) {
        LevelSelector(state.selectedType, onSelectType)
        PeopleCard(
            people = state.people,
            vulnerableCount = state.vulnerableCount,
            onDecrement = onDecrementPeople,
            onIncrement = onIncrementPeople,
            onOpenVulnerable = { sheetOpen = true },
        )
        LocationCard(state.location, onUpdateLocation, onEnableLocation)
    }
    if (sheetOpen) {
        VulnerableSheet(vulnerable = state.vulnerable, onChange = onVulnerableChange, onDone = { sheetOpen = false })
    }
}

/** Texto bajo el botón: el del diseño si todo está listo, o la razón por la que no se puede enviar. */
@Composable
private fun HintText(hint: SendHint?) {
    val colors = Senda.colors
    val (text, color) = when (hint) {
        null -> stringResource(R.string.sendalert_footer) to colors.textSecondary
        SendHint.SENDING -> stringResource(R.string.sendalert_sending) to colors.tone(UiTone.INFO)
        SendHint.RELEASED_EARLY -> stringResource(R.string.sendalert_hold_release_early) to colors.tone(UiTone.INFO)
        SendHint.NEEDS_TYPE -> stringResource(R.string.sendalert_cannot_send_type) to colors.tone(UiTone.INFO)
        SendHint.NEEDS_LOCATION -> stringResource(R.string.sendalert_cannot_send_location) to colors.tone(UiTone.WARNING)
    }
    Text(text, style = SendaText.caption, color = color, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
}

// ---------------------------------------------------------------- Nivel de emergencia

@Composable
private fun LevelSelector(selected: EmergencyType?, onSelect: (EmergencyType) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.sendalert_level_label), style = SendaText.label, color = Senda.colors.textPrimary)
        Row(modifier = Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LEVELS.forEach { type ->
                LevelOption(type, selected = type == selected, onClick = { onSelect(type) }, modifier = Modifier.weight(1f))
            }
        }
    }
}

/**
 * Una opción de nivel (Figma, "Opción de nivel": 78 dp). El nivel va en color **y** en texto, y el seleccionado suma
 * borde de 2 dp, fondo del nivel y una marca; nunca se distingue solo por color.
 */
@Composable
private fun LevelOption(type: EmergencyType, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Senda.colors
    val palette = colors.palette(type)
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .height(78.dp)
            .clip(shape)
            .background(if (selected) palette.container else colors.surface)
            .border(BorderStroke(if (selected) 2.dp else 1.dp, if (selected) palette.selectedBorder else colors.border), shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterVertically),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(palette.content, CircleShape))
            if (selected) SendaIcon(SendaIcons.Check, size = 15.dp, tint = palette.content)
        }
        Text(LocalResources.current.emergencyOptionText(type), style = SendaText.labelSemiBold, color = palette.content)
    }
}

// ---------------------------------------------------------------- Personas y vulnerables

@Composable
private fun PeopleCard(
    people: Int,
    vulnerableCount: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onOpenVulnerable: () -> Unit,
) {
    val colors = Senda.colors
    SendaCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(stringResource(R.string.sendalert_people_title), style = SendaText.label, color = colors.textPrimary)
                Text(stringResource(R.string.sendalert_people_hint), style = SendaText.caption, color = colors.textSecondary)
            }
            QuantityStepper(
                value = people,
                onDecrement = onDecrement,
                onIncrement = onIncrement,
                canDecrement = people > MIN_PEOPLE,
                canIncrement = people < MAX_PEOPLE,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.fillMaxWidth().height(Dimens.borderWidth).background(colors.border))
            VulnerableRow(people, vulnerableCount, onOpenVulnerable)
        }
    }
}

/** Fila que abre la hoja de vulnerables: casilla (marcada si hay alguna), título, resumen y flecha. */
@Composable
private fun VulnerableRow(people: Int, vulnerableCount: Int, onClick: () -> Unit) {
    val colors = Senda.colors
    Row(
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // La fila lleva el toque; la casilla solo refleja si hay alguna bandera marcada.
        SendaCheckbox(checked = vulnerableCount > 0, onCheckedChange = null, modifier = Modifier.size(22.dp).clearAndSetSemantics { })
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(stringResource(R.string.sendalert_vulnerable_title), style = SendaText.labelSemiBold, color = colors.textPrimary)
            Text(
                text = if (vulnerableCount > 0) {
                    pluralStringResource(R.plurals.sendalert_vulnerable_summary, people, vulnerableCount, people)
                } else {
                    stringResource(R.string.sendalert_vulnerable_hint)
                },
                style = SendaText.caption,
                color = colors.textSecondary,
            )
        }
        SendaIcon(SendaIcons.ChevronRight, size = 18.dp, tint = colors.accent)
    }
}

// ---------------------------------------------------------------- Ubicación

@Composable
private fun LocationCard(location: LocationUi, onUpdate: () -> Unit, onEnable: () -> Unit) {
    val colors = Senda.colors
    val resources = LocalResources.current
    SendaCard(style = CardStyle.Muted) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SendaIcon(SendaIcons.MapPin, size = 20.dp, tint = colors.accent)
            Text(stringResource(R.string.sendalert_location_title), style = SendaText.label, color = colors.textPrimary)
        }
        val age = location.ageMs?.let { resources.elapsedText(classifyElapsed(it)) } ?: stringResource(R.string.format_unknown)
        val accuracy = location.accuracyM ?: 0
        when (location.status) {
            LocationStatus.FIX -> {
                Coordinates(location.coordinates)
                Text(
                    stringResource(R.string.sendalert_location_precision, accuracy, age),
                    style = SendaText.caption,
                    color = colors.textSecondary,
                )
                LinkAction(stringResource(R.string.sendalert_location_update), onUpdate)
            }
            LocationStatus.LAST_KNOWN -> {
                if (location.isStale) {
                    Message(stringResource(R.string.sendalert_location_stale, age), UiTone.WARNING)
                } else {
                    Message(stringResource(R.string.sendalert_location_last_known, accuracy, age), UiTone.INFO)
                }
                Coordinates(location.coordinates)
                LinkAction(stringResource(R.string.sendalert_location_update), onUpdate)
            }
            LocationStatus.SEARCHING -> Message(stringResource(R.string.sendalert_location_searching), UiTone.INFO)
            LocationStatus.DISABLED -> {
                Message(stringResource(R.string.sendalert_location_disabled), UiTone.ERROR)
                LinkAction(stringResource(R.string.sendalert_location_activate), onEnable)
            }
            LocationStatus.UNAVAILABLE -> {
                Message(stringResource(R.string.sendalert_location_unavailable), UiTone.ERROR)
                LinkAction(stringResource(R.string.sendalert_location_update), onUpdate)
            }
        }
    }
}

/** Posición en coordenadas: el diseño dibuja aquí un nombre de lugar, pero el paquete solo trae coordenadas (regla 24). */
@Composable
private fun Coordinates(text: String?) {
    Text(text ?: stringResource(R.string.format_unknown), style = SendaText.labelMedium, color = Senda.colors.textPrimary)
}

@Composable
private fun Message(text: String, tone: UiTone) {
    Text(text, style = SendaText.subtitle, color = Senda.colors.tone(tone))
}

/**
 * Acción de texto de 13 sp del diseño ("Actualizar ubicación"). El relleno vertical amplía el área de toque sin
 * alargar la tarjeta más de lo que dibuja Figma (con 48 dp la tarjeta quedaba ~20 dp más alta).
 */
@Composable
private fun LinkAction(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = SendaText.body13SemiBold,
        color = Senda.colors.accent,
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick).padding(vertical = 6.dp),
    )
}
