package com.senda.permissions

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.senda.BuildConfig
import com.senda.R
import com.senda.ui.components.ButtonStyle
import com.senda.ui.components.CardStyle
import com.senda.ui.components.MessageBanner
import com.senda.ui.components.ScreenHeader
import com.senda.ui.components.SendaButton
import com.senda.ui.components.SendaCard
import com.senda.ui.components.SendaIcon
import com.senda.ui.components.SendaIcons
import com.senda.ui.components.SendaTextButton
import com.senda.ui.components.SmallActionButton
import com.senda.ui.theme.Dimens
import com.senda.ui.theme.Senda
import com.senda.ui.theme.SendaText
import com.senda.ui.theme.UiTone

// spec 100, secciones 5.3 y 12 · Pantalla de permisos (Figma, marco "P1 · Permisos"). Sin estado: la dibuja un
// PermissionsUiState y avisa de lo que toca el usuario. Lo que Figma no dibuja (ubicación del teléfono, ubicación en
// segundo plano, batería, paso de Samsung, "negado", banner) usa el mismo estilo.

@Composable
fun PermissionsScreen(
    state: PermissionsUiState,
    onRowAction: (PermissionRowUi) -> Unit,
    onSamsungOpenSettings: () -> Unit,
    onSamsungDone: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Senda.colors
    Column(modifier = modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
        ScreenHeader(
            title = stringResource(R.string.permissions_title),
            subtitle = stringResource(R.string.permissions_subtitle),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = Dimens.screenPadding, end = Dimens.screenPadding, top = 4.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            if (state.showLostPermissionBanner) {
                MessageBanner(text = stringResource(R.string.common_lost_permission), tone = UiTone.ERROR)
            }
            Intro()
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.rows.forEach { PermissionRow(it, onClick = { onRowAction(it) }) }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.permissions_recommended_title),
                    style = SendaText.label,
                    color = colors.textPrimary,
                )
                state.recommended.forEach { PermissionRow(it, onClick = { onRowAction(it) }) }
                if (state.showSamsungStep) SamsungStep(onOpenSettings = onSamsungOpenSettings, onDone = onSamsungDone)
            }
        }
        Continuation(canContinue = state.canContinue, onContinue = onContinue)
    }
}

/** Figma, "Introducción": bloque de acento suave con el ícono de red. */
@Composable
private fun Intro() {
    val colors = Senda.colors
    SendaCard(style = CardStyle.Tinted) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            SendaIcon(SendaIcons.Network, size = 28.dp, tint = colors.accent)
            Text(
                text = stringResource(R.string.permissions_intro),
                style = SendaText.subtitle,
                color = colors.accentStrong,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Figma, "Permiso": nombre y símbolo, explicación, estado a la izquierda y el botón a la derecha. */
@Composable
internal fun PermissionRow(row: PermissionRowUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Senda.colors
    val text = row.kind.text()
    val locked = !row.actionEnabled
    val tone = when {
        locked -> UiTone.WARNING
        row.state == RowState.GRANTED -> UiTone.SUCCESS
        row.state == RowState.MISSING -> UiTone.WARNING
        else -> UiTone.ERROR
    }
    SendaCard(modifier = modifier, padding = 14.dp, verticalSpacing = 0.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    SendaIcon(text.icon, size = 19.dp, tint = colors.textPrimary)
                    Text(stringResource(text.title), style = SendaText.sectionTitle, color = colors.textPrimary)
                }
                Text(stringResource(text.body), style = SendaText.caption, color = colors.textSecondary)
                Text(
                    text = stringResource(if (locked) R.string.permissions_background_locked else row.statusText()),
                    style = SendaText.captionSemiBold,
                    color = colors.tone(tone),
                )
            }
            SmallActionButton(
                text = stringResource(row.actionLabel()),
                onClick = onClick,
                filled = row.state != RowState.GRANTED,
                enabled = row.actionEnabled,
            )
        }
    }
}

@Composable
private fun SamsungStep(onOpenSettings: () -> Unit, onDone: () -> Unit) {
    val colors = Senda.colors
    SendaCard(padding = 14.dp, verticalSpacing = 10.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SendaIcon(SendaIcons.Smartphone, size = 19.dp, tint = colors.textPrimary)
            Text(stringResource(R.string.permissions_samsung_title), style = SendaText.sectionTitle, color = colors.textPrimary)
        }
        Text(stringResource(R.string.permissions_samsung_body), style = SendaText.caption, color = colors.textSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            SmallActionButton(stringResource(R.string.permissions_samsung_open), onClick = onOpenSettings)
            SendaTextButton(stringResource(R.string.permissions_samsung_done), onClick = onDone)
        }
    }
}

/** Figma, "Continuación": aviso, botón Continuar y la nota de demostración, fijos al pie. */
@Composable
private fun Continuation(canContinue: Boolean, onContinue: () -> Unit) {
    val colors = Senda.colors
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = Dimens.screenPadding, end = Dimens.screenPadding, top = 4.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (canContinue) {
            Text(stringResource(R.string.permissions_continue_hint), style = SendaText.caption, color = colors.textSecondary)
        } else {
            Text(stringResource(R.string.permissions_missing_hint), style = SendaText.captionSemiBold, color = colors.warning)
        }
        SendaButton(
            text = stringResource(R.string.permissions_continue),
            onClick = onContinue,
            style = ButtonStyle.Primary,
            icon = SendaIcons.ArrowRight,
            enabled = canContinue,
        )
        if (BuildConfig.DEBUG) {
            Text(
                text = stringResource(R.string.permissions_demo_notice),
                style = SendaText.small,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
}

// ---------------- Textos por fila ----------------

private class RowText(@StringRes val title: Int, @StringRes val body: Int, val icon: Int)

private fun PermissionRowKind.text(): RowText = when (this) {
    PermissionRowKind.BLUETOOTH -> RowText(R.string.permissions_bluetooth_title, R.string.permissions_bluetooth_body, SendaIcons.Bluetooth)
    PermissionRowKind.NEARBY -> RowText(R.string.permissions_nearby_title, R.string.permissions_nearby_body, SendaIcons.Smartphone)
    PermissionRowKind.LOCATION -> RowText(R.string.permissions_location_title, R.string.permissions_location_body, SendaIcons.MapPin)
    PermissionRowKind.LOCATION_SERVICES ->
        RowText(R.string.permissions_location_services_title, R.string.permissions_location_services_body, SendaIcons.MapPin)
    PermissionRowKind.NOTIFICATIONS ->
        RowText(R.string.permissions_notifications_title, R.string.permissions_notifications_body, SendaIcons.Bell)
    PermissionRowKind.BACKGROUND_LOCATION ->
        RowText(R.string.permissions_background_title, R.string.permissions_background_body, SendaIcons.Navigation)
    PermissionRowKind.BATTERY -> RowText(R.string.permissions_battery_title, R.string.permissions_battery_body, SendaIcons.BatteryFull)
}

@StringRes
private fun PermissionRowUi.statusText(): Int = when (state) {
    RowState.MISSING -> R.string.permissions_state_pending
    RowState.DENIED_PERMANENTLY -> R.string.permissions_state_denied
    RowState.GRANTED -> when (kind) {
        PermissionRowKind.BLUETOOTH, PermissionRowKind.LOCATION_SERVICES -> R.string.permissions_state_active
        PermissionRowKind.NEARBY -> R.string.permissions_state_granted_masculine
        PermissionRowKind.LOCATION, PermissionRowKind.BACKGROUND_LOCATION -> R.string.permissions_state_granted_feminine
        PermissionRowKind.NOTIFICATIONS -> R.string.permissions_state_granted_feminine_plural
        PermissionRowKind.BATTERY -> R.string.permissions_state_battery_ok
    }
}

/** Figma: `Revisar` cuando ya está bien y `Permitir` cuando falta; el resto de etiquetas no están en el diseño. */
@StringRes
private fun PermissionRowUi.actionLabel(): Int = when {
    state == RowState.GRANTED -> R.string.permissions_action_review
    state == RowState.DENIED_PERMANENTLY -> R.string.permissions_action_settings
    action == PermissionAction.ENABLE_BLUETOOTH || action == PermissionAction.OPEN_BLUETOOTH_SETTINGS -> R.string.permissions_action_enable
    action == PermissionAction.OPEN_LOCATION_SETTINGS -> R.string.permissions_action_activate
    else -> R.string.permissions_action_allow
}
