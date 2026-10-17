package com.senda.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.senda.BuildConfig
import com.senda.R
import com.senda.ui.theme.Dimens
import com.senda.ui.theme.Senda
import com.senda.ui.theme.SendaText

/**
 * Encabezado de cada pantalla (Figma, "Cabecera"): identidad a la izquierda (marca, o flecha y nombre de la
 * pestaña de origen en un detalle), rótulo DEMO a la derecha, y debajo el título y su contexto.
 * El espacio de la barra de estado lo pone la pantalla con los márgenes del sistema, no este componente.
 */
@Composable
fun ScreenHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    identityIcon: Int = SendaIcons.RadioTower,
    identityLabel: String = stringResource(R.string.brand_name),
    onIdentityClick: (() -> Unit)? = null,
    showDemoBadge: Boolean = BuildConfig.DEBUG,
) {
    val colors = Senda.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = Dimens.screenPadding, end = Dimens.screenPadding, top = 12.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = if (onIdentityClick != null) {
                    Modifier
                        .minimumInteractiveComponentSize()
                        .clickable(role = Role.Button, onClick = onIdentityClick)
                } else {
                    Modifier
                },
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SendaIcon(identityIcon, size = 20.dp, tint = colors.accent)
                Text(identityLabel, style = SendaText.body13Bold, color = colors.accent)
            }
            if (showDemoBadge) DemoBadge()
        }
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                text = title,
                style = SendaText.title,
                color = colors.textPrimary,
                modifier = Modifier.semantics { heading() },
            )
            Text(text = subtitle, style = SendaText.subtitle, color = colors.textSecondary)
        }
    }
}
