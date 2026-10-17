package com.senda.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.senda.R
import com.senda.core.protocol.EmergencyType
import com.senda.ui.theme.Dimens
import com.senda.ui.theme.Senda
import com.senda.ui.theme.SendaText

/** Rótulo "DEMO" del encabezado. Solo en la versión de depuración (decisión del equipo, 2026-10-17). */
@Composable
fun DemoBadge(modifier: Modifier = Modifier) {
    val colors = Senda.colors
    Box(
        modifier = modifier
            .background(colors.warningContainer, RoundedCornerShape(Dimens.demoBadgeRadius))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(stringResource(R.string.common_demo_badge), style = SendaText.tinyBold, color = colors.warning)
    }
}

/** Rótulo "SIMULACIÓN": marca todo lo simulado (regla 30). */
@Composable
fun SimulationBadge(modifier: Modifier = Modifier) {
    val colors = Senda.colors
    Box(
        modifier = modifier
            .background(colors.warningAction, RoundedCornerShape(Dimens.demoBadgeRadius))
            .padding(horizontal = 9.dp, vertical = 5.dp),
    ) {
        Text(stringResource(R.string.common_simulation_badge), style = SendaText.smallBold, color = colors.onWarningAction)
    }
}

/** Nivel de emergencia: ícono y texto con el color del nivel. El nivel nunca va solo en color. */
@Composable
fun EmergencyBadge(type: EmergencyType, text: String, modifier: Modifier = Modifier) {
    val palette = Senda.colors.palette(type)
    Row(
        modifier = modifier
            .background(palette.container, RoundedCornerShape(Dimens.badgeRadius))
            .padding(horizontal = 9.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SendaIcon(SendaIcons.TriangleAlert, size = 14.dp, tint = palette.content)
        Text(text, style = SendaText.captionBold, color = palette.content)
    }
}
