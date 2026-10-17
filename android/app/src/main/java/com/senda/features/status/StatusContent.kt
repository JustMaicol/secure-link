package com.senda.features.status

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.senda.R
import com.senda.ui.components.ButtonStyle
import com.senda.ui.components.InfoRow
import com.senda.ui.components.ScreenScaffold
import com.senda.ui.components.SendaButton
import com.senda.ui.components.SendaCard
import com.senda.ui.components.SendaIcon
import com.senda.ui.components.SendaIcons
import com.senda.ui.components.SendaSwitch
import com.senda.ui.components.SimulationBadge
import com.senda.ui.format.ConnectivityText
import com.senda.ui.format.classifyElapsed
import com.senda.ui.format.elapsedText
import com.senda.ui.theme.Dimens
import com.senda.ui.theme.Senda
import com.senda.ui.theme.SendaText
import java.util.Locale

/**
 * S1 · Estado de la red (Figma). Sin estado de negocio: dibuja [state] y avisa de cada acción. La tarjeta de simulación
 * solo se dibuja si [StatusUiState.debug] no es nulo (versión de depuración) y siempre lleva el rótulo SIMULACIÓN.
 */
@Composable
fun StatusContent(
    state: StatusUiState,
    connectivity: ConnectivityText,
    onCopyId: () -> Unit,
    onMotionEnabledChange: (Boolean) -> Unit,
    onReviewPermissions: () -> Unit,
    onRemoveTag: (String) -> Unit,
    onAddTag: () -> Unit,
    onSimulateShake: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenScaffold(
        title = stringResource(R.string.status_title),
        subtitle = stringResource(R.string.status_subtitle),
        modifier = modifier,
        connectivity = connectivity,
        contentPadding = PaddingValues(horizontal = Dimens.screenPadding, vertical = 18.dp),
    ) {
        MeshCard(state)
        ServerCard(state)
        PhoneCard(state, onCopyId)
        SettingsCard(state, onMotionEnabledChange, onReviewPermissions)
        state.debug?.let { SimulationCard(it, onRemoveTag, onAddTag, onSimulateShake) }
    }
}

// ---------------------------------------------------------------- Malla

@Composable
private fun MeshCard(state: StatusUiState) {
    val colors = Senda.colors
    SendaCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CardTitle(SendaIcons.Bluetooth, stringResource(R.string.status_mesh_title))
            Text(
                stringResource(if (state.meshActive) R.string.status_mesh_active else R.string.status_mesh_stopped),
                style = SendaText.captionBold,
                color = if (state.meshActive) colors.success else colors.warning,
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Metric(state.announcementsActive, stringResource(R.string.status_metric_announcements), Modifier.weight(1f))
            Metric(state.neighbors, stringResource(R.string.status_metric_neighbors), Modifier.weight(1f))
        }
    }
}

/** Título de tarjeta con ícono de 19 dp en acento (Figma, "Título"). */
@Composable
private fun CardTitle(icon: Int, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        SendaIcon(icon, size = 19.dp, tint = Senda.colors.accent)
        Text(text, style = SendaText.label, color = Senda.colors.textPrimary)
    }
}

/** Cifra y su rótulo en un bloque (Figma, "Métrica"). En oscuro el bloque es negro, sin diferencia con la tarjeta. */
@Composable
private fun Metric(value: Int, label: String, modifier: Modifier = Modifier) {
    val colors = Senda.colors
    Column(
        modifier = modifier
            .background(if (colors.isDark) colors.surface else colors.surfaceMuted, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(value.toString(), style = SendaText.figure, color = colors.textPrimary)
        Text(label, style = SendaText.captionPlain, color = colors.textSecondary)
    }
}

// ---------------------------------------------------------------- Servidor y teléfono

@Composable
private fun ServerCard(state: StatusUiState) {
    val colors = Senda.colors
    val resources = LocalResources.current
    SendaCard(verticalSpacing = 10.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CardTitle(if (state.internetOnline) SendaIcons.Network else SendaIcons.CloudOff, stringResource(R.string.status_server_title))
            Text(
                stringResource(if (state.internetOnline) R.string.connectivity_internet_on else R.string.connectivity_internet_off),
                style = SendaText.captionPlain,
                color = if (state.internetOnline) colors.success else colors.warning,
            )
        }
        InfoRow(
            label = stringResource(R.string.status_server_pending_label),
            value = pluralStringResource(R.plurals.status_server_pending_value, state.pendingUploads, state.pendingUploads),
            modifier = Modifier.fillMaxWidth(),
        )
        InfoRow(
            label = stringResource(R.string.status_server_last_label),
            value = if (state.lastUploadClock != null && state.lastUploadAgeMs != null) {
                stringResource(R.string.status_server_last_value, state.lastUploadClock, resources.elapsedText(classifyElapsed(state.lastUploadAgeMs)))
            } else {
                stringResource(R.string.format_unknown)
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PhoneCard(state: StatusUiState, onCopyId: () -> Unit) {
    val colors = Senda.colors
    SendaCard(verticalSpacing = 10.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.status_phone_title), style = SendaText.label, color = colors.textPrimary)
            SendaIcon(
                SendaIcons.Copy,
                size = 18.dp,
                tint = colors.accent,
                contentDescription = stringResource(R.string.common_copy),
                modifier = Modifier.clickable(role = Role.Button, onClick = onCopyId),
            )
        }
        InfoRow(stringResource(R.string.status_phone_id_label), state.deviceId, Modifier.fillMaxWidth())
        InfoRow(stringResource(R.string.status_phone_relay_label), state.relayTag, Modifier.fillMaxWidth())
    }
}

// ---------------------------------------------------------------- Ajustes

@Composable
private fun SettingsCard(state: StatusUiState, onMotionEnabledChange: (Boolean) -> Unit, onReviewPermissions: () -> Unit) {
    val colors = Senda.colors
    SendaCard {
        Text(stringResource(R.string.status_settings_title), style = SendaText.label, color = colors.textPrimary)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(stringResource(R.string.status_settings_motion_title), style = SendaText.labelMedium, color = colors.textPrimary)
                Text(
                    stringResource(R.string.status_settings_motion_body, state.motionCountdownSeconds),
                    style = SendaText.captionPlain,
                    color = colors.textSecondary,
                )
            }
            SendaSwitch(checked = state.motionEnabled, onCheckedChange = onMotionEnabledChange)
        }
        // El borde superior es parte de la fila de 40 dp (Figma, "Revisar permisos"), no un separador aparte.
        val divider = colors.border
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .drawBehind { drawLine(divider, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx()) }
                .clickable(role = Role.Button, onClick = onReviewPermissions),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.status_settings_permissions), style = SendaText.labelMedium, color = colors.textPrimary)
            SendaIcon(SendaIcons.ChevronRight, size = 18.dp, tint = colors.accent)
        }
    }
}

// ---------------------------------------------------------------- Simulación (solo depuración)

@Composable
private fun SimulationCard(
    debug: StatusDebugUiState,
    onRemoveTag: (String) -> Unit,
    onAddTag: () -> Unit,
    onSimulateShake: () -> Unit,
) {
    val colors = Senda.colors
    val shape = RoundedCornerShape(Dimens.cardRadius)
    val dashed = colors.dashedBorder
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.warningContainer)
            .drawBehind {
                drawRoundRect(
                    color = dashed,
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
                )
            }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SimulationBadge()
            Text(stringResource(R.string.status_debug_scope), style = SendaText.captionPlain, color = colors.textSecondary)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.status_debug_filter_title), style = SendaText.labelSemiBold, color = colors.textPrimary)
            TagEditor(debug.blockedTags, onRemoveTag, onAddTag)
        }
        MotionMeter(debug.motionReadingG)
        SendaButton(
            text = stringResource(R.string.status_debug_simulate_shake),
            onClick = onSimulateShake,
            style = ButtonStyle.Secondary,
            icon = SendaIcons.Activity,
        )
        Text(stringResource(R.string.status_debug_simulate_hint), style = SendaText.small, color = colors.textSecondary)
    }
}

/** Editor del filtro (Figma, "Editor de filtro"): etiquetas bloqueadas con su aspa y "+ Añadir" a la derecha. */
@Composable
private fun TagEditor(tags: List<String>, onRemove: (String) -> Unit, onAdd: () -> Unit) {
    val colors = Senda.colors
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(colors.surface, shape)
            .border(BorderStroke(Dimens.borderWidth, colors.border), shape)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            tags.forEach { tag ->
                Row(
                    modifier = Modifier
                        .background(if (colors.isDark) colors.warningContainer else colors.surfaceMuted, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(tag, style = SendaText.captionSemiBold, color = colors.textPrimary)
                    SendaIcon(
                        SendaIcons.X,
                        size = 13.dp,
                        tint = colors.textSecondary,
                        contentDescription = stringResource(R.string.status_debug_filter_remove, tag),
                        modifier = Modifier.clickable(role = Role.Button) { onRemove(tag) },
                    )
                }
            }
        }
        Text(
            stringResource(R.string.status_debug_filter_add),
            style = SendaText.captionSemiBold,
            color = colors.accent,
            modifier = Modifier.minimumInteractiveComponentSize().clickable(role = Role.Button, onClick = onAdd),
        )
    }
}

/** Medidor de movimiento (Figma): lectura en g, barra de 8 dp sobre una escala de 0 a 2 g y sus marcas. */
@Composable
private fun MotionMeter(readingG: Float) {
    val colors = Senda.colors
    val fraction = (readingG / MOTION_SCALE_MAX_G).coerceIn(0f, 1f)
    val barShape = RoundedCornerShape(4.dp)
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.status_debug_motion_label), style = SendaText.body13, color = colors.textPrimary)
            // Con coma decimal, como el resto de cifras de la app, sea cual sea el idioma del teléfono.
            Text("%.2f g".format(Locale.forLanguageTag("es"), readingG), style = SendaText.body13Bold, color = colors.accent)
        }
        Box(Modifier.fillMaxWidth().height(8.dp).background(colors.border, barShape)) {
            if (fraction > 0f) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .height(8.dp)
                        .background(if (colors.isDark) colors.actionContainer else colors.accent, barShape)
                        .let { if (colors.isDark) it.border(BorderStroke(1.dp, colors.actionBorder), barShape) else it },
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.status_debug_motion_scale_0), style = SendaText.tiny, color = colors.textSecondary)
            Text(stringResource(R.string.status_debug_motion_scale_1), style = SendaText.tiny, color = colors.textSecondary)
            Text(stringResource(R.string.status_debug_motion_scale_2), style = SendaText.tiny, color = colors.textSecondary)
        }
    }
}
