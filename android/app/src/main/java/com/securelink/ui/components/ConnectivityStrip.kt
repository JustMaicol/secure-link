package com.securelink.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.securelink.ui.theme.Dimens
import com.securelink.ui.theme.Senda
import com.securelink.ui.theme.SendaText
import com.securelink.ui.theme.UiTone

/**
 * Franja bajo el encabezado (Figma, "Conectividad"): estado de la malla Bluetooth y de internet.
 * Ejemplo del diseño: `Malla activa · Sin internet`. Los textos y el tono los decide quien la usa.
 */
@Composable
fun ConnectivityStrip(
    meshText: String,
    meshTone: UiTone,
    internetText: String,
    modifier: Modifier = Modifier,
) {
    val colors = Senda.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.connectivityHeight)
            .background(colors.surfaceMuted)
            .padding(horizontal = Dimens.screenPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SendaIcon(SendaIcons.Bluetooth, size = 15.dp, tint = colors.tone(meshTone))
        Text(meshText, style = SendaText.captionMedium, color = colors.tone(meshTone))
        Box(Modifier.size(3.dp).background(colors.textSecondary, CircleShape))
        Text(internetText, style = SendaText.caption, color = colors.textSecondary)
    }
}
