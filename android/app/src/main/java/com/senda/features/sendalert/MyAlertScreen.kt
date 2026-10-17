package com.senda.features.sendalert

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.senda.R
import com.senda.ui.components.ButtonStyle
import com.senda.ui.components.CardStyle
import com.senda.ui.components.ConfirmDialog
import com.senda.ui.components.EmergencyBadge
import com.senda.ui.components.InfoRow
import com.senda.ui.components.ScreenScaffold
import com.senda.ui.components.SendaButton
import com.senda.ui.components.SendaCard
import com.senda.ui.components.SendaIcon
import com.senda.ui.components.SendaIcons
import com.senda.ui.components.SendaTextButton
import com.senda.ui.format.ConnectivityText
import com.senda.ui.format.classifyElapsed
import com.senda.ui.format.elapsedText
import com.senda.ui.format.emergencyLevelText
import com.senda.ui.format.peopleAndVulnerableText
import com.senda.ui.theme.Dimens
import com.senda.ui.theme.Senda
import com.senda.ui.theme.SendaText

/** Qué confirma el diálogo abierto (es del dibujo: el negocio solo recibe el resultado). */
private enum class Confirm { SAFE, CANCEL }

/**
 * E2 · Mi alerta (Figma). Sin estado de negocio: dibuja [state] y avisa de cada acción. "Estoy a salvo" y "Cancelar
 * alerta" son opcionales (spec 000): sin callback no se dibujan. Cada una pide confirmación porque no se deshace.
 */
@Composable
fun MyAlertScreen(
    state: MyAlertUiState,
    connectivity: ConnectivityText,
    modifier: Modifier = Modifier,
    onSafe: (() -> Unit)? = null,
    onCancelAlert: (() -> Unit)? = null,
    initialConfirm: Boolean = false,
) {
    var confirm by rememberSaveable { mutableStateOf<Confirm?>(if (initialConfirm) Confirm.SAFE else null) }
    ScreenScaffold(
        title = stringResource(R.string.sendalert_mine_title),
        subtitle = stringResource(R.string.sendalert_mine_subtitle, state.createdAtText, state.reference),
        modifier = modifier,
        connectivity = connectivity,
        contentPadding = PaddingValues(horizontal = Dimens.screenPadding, vertical = 18.dp),
    ) {
        ProgressCard(state)
        VolunteersCard(state.volunteers)
        SummaryCard(state)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (onSafe != null) {
                SendaButton(
                    text = stringResource(R.string.sendalert_mine_safe),
                    onClick = { confirm = Confirm.SAFE },
                    style = ButtonStyle.Success,
                    icon = SendaIcons.ShieldCheck,
                )
            }
            if (onCancelAlert != null) {
                SendaTextButton(
                    text = stringResource(R.string.sendalert_mine_cancel),
                    onClick = { confirm = Confirm.CANCEL },
                    fullWidth = true,
                )
            }
        }
    }
    when (confirm) {
        Confirm.SAFE -> ConfirmDialog(
            title = stringResource(R.string.sendalert_mine_safe),
            body = stringResource(R.string.sendalert_mine_confirm_safe),
            confirmLabel = stringResource(R.string.common_confirm),
            dismissLabel = stringResource(R.string.common_back),
            onConfirm = { confirm = null; onSafe?.invoke() },
            onDismiss = { confirm = null },
        )
        Confirm.CANCEL -> ConfirmDialog(
            title = stringResource(R.string.sendalert_mine_cancel),
            body = stringResource(R.string.sendalert_mine_confirm_cancel),
            confirmLabel = stringResource(R.string.sendalert_mine_cancel),
            dismissLabel = stringResource(R.string.common_back),
            onConfirm = { confirm = null; onCancelAlert?.invoke() },
            onDismiss = { confirm = null },
            confirmStyle = ButtonStyle.Warning,
        )
        null -> Unit
    }
}

// ---------------------------------------------------------------- Progreso

/** Cómo se dibuja el círculo de un paso. */
private enum class StepMark { DONE, BROADCAST, CURRENT, PENDING }

@Composable
private fun ProgressCard(state: MyAlertUiState) {
    val resources = LocalResources.current
    val relayed = state.echoCount > 0
    val uploaded = state.uploadedAtText != null
    SendaCard(verticalSpacing = 0.dp) {
        Step(
            mark = StepMark.DONE,
            number = 1,
            title = stringResource(R.string.sendalert_mine_step_saved_title),
            body = stringResource(R.string.sendalert_mine_step_saved_body, state.savedAtText),
        )
        Step(
            mark = StepMark.BROADCAST,
            number = 2,
            title = stringResource(R.string.sendalert_mine_step_announcing_title),
            body = stringResource(R.string.sendalert_mine_step_announcing_body),
        )
        Step(
            mark = if (relayed) StepMark.CURRENT else StepMark.PENDING,
            number = 3,
            title = stringResource(R.string.sendalert_mine_step_relayed_title),
            body = if (relayed) {
                val ago = state.lastEchoAgeMs?.let { resources.elapsedText(classifyElapsed(it)) } ?: stringResource(R.string.format_unknown)
                pluralStringResource(R.plurals.sendalert_mine_step_relayed_body, state.echoCount, state.echoCount, ago)
            } else {
                stringResource(R.string.sendalert_mine_step_relayed_none)
            },
        )
        Step(
            mark = if (uploaded) StepMark.DONE else StepMark.PENDING,
            number = 4,
            title = stringResource(R.string.sendalert_mine_step_upload_title),
            body = if (uploaded) {
                stringResource(R.string.sendalert_mine_step_upload_done, state.uploadedAtText.orEmpty())
            } else {
                stringResource(R.string.sendalert_mine_step_upload_pending)
            },
            last = true,
            // El paso pendiente final se lee en gris; el resto, en el color del texto.
            muted = !uploaded,
        )
    }
}

/**
 * Un paso de la línea de progreso (Figma, "Paso de difusión"): círculo de 28 dp con su marca, una línea de 27 dp hasta
 * el siguiente y título y detalle al lado. Cada paso lleva su estado en texto, no solo en color.
 */
@Composable
private fun Step(mark: StepMark, number: Int, title: String, body: String, last: Boolean = false, muted: Boolean = false) {
    val colors = Senda.colors
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.width(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            StepCircle(mark, number)
            if (!last) Box(Modifier.width(2.dp).height(27.dp).background(colors.border))
        }
        Column(
            modifier = Modifier.weight(1f).padding(top = 3.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(title, style = SendaText.labelSemiBold, color = if (muted) colors.textSecondary else colors.textPrimary)
            Text(body, style = SendaText.caption, color = colors.textSecondary)
        }
    }
}

@Composable
private fun StepCircle(mark: StepMark, number: Int) {
    val colors = Senda.colors
    // Claro: hecho = fondo suave, en curso = acento lleno, pendiente = ámbar. Oscuro: monocromo; el actual lleva borde.
    val container = when (mark) {
        StepMark.DONE -> colors.surfaceMuted
        StepMark.BROADCAST -> if (colors.isDark) colors.surfaceMuted else colors.surfaceTint
        StepMark.CURRENT -> if (colors.isDark) colors.actionContainer else colors.accent
        StepMark.PENDING -> colors.warningContainer
    }
    val numberColor = when (mark) {
        StepMark.CURRENT -> if (colors.isDark) colors.onActionContainer else colors.surface
        else -> colors.warning
    }
    val modifier = Modifier
        .size(28.dp)
        .background(container, CircleShape)
        .let { if (mark == StepMark.CURRENT && colors.isDark) it.border(1.dp, colors.actionBorder, CircleShape) else it }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (mark) {
            StepMark.DONE -> SendaIcon(SendaIcons.Check, size = 16.dp, tint = colors.success)
            StepMark.BROADCAST -> SendaIcon(SendaIcons.Radio, size = 16.dp, tint = colors.accent)
            StepMark.CURRENT, StepMark.PENDING -> Text(number.toString(), style = SendaText.captionBold, color = numberColor)
        }
    }
}

// ---------------------------------------------------------------- Voluntarios y resumen

@Composable
private fun VolunteersCard(volunteers: Int) {
    val colors = Senda.colors
    SendaCard(style = CardStyle.Tinted) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(volunteers.toString(), style = SendaText.figureLarge, color = colors.accent)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.sendalert_mine_volunteers_title), style = SendaText.sectionTitle, color = colors.accentStrong)
                Text(stringResource(R.string.sendalert_mine_volunteers_hint), style = SendaText.caption, color = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun SummaryCard(state: MyAlertUiState) {
    val colors = Senda.colors
    val resources = LocalResources.current
    SendaCard(verticalSpacing = 10.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EmergencyBadge(state.type, resources.emergencyLevelText(state.type))
            Text(
                resources.peopleAndVulnerableText(state.people, state.vulnerableCount),
                style = SendaText.body13Medium,
                color = colors.textPrimary,
            )
        }
        InfoRow(label = stringResource(R.string.sendalert_mine_location_label), value = state.coordinates, modifier = Modifier.fillMaxWidth())
        Text(
            stringResource(
                R.string.sendalert_mine_location_age,
                state.accuracyM,
                resources.elapsedText(classifyElapsed(state.locationAgeMs)),
            ),
            style = SendaText.caption,
            color = colors.textSecondary,
        )
    }
}
