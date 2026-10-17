package com.senda.features

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.senda.R
import com.senda.ui.components.CardStyle
import com.senda.ui.components.ScreenScaffold
import com.senda.ui.components.SendaCard
import com.senda.ui.format.ConnectivityText
import com.senda.ui.theme.Senda
import com.senda.ui.theme.SendaText

/**
 * Pantalla de una pestaña mientras su spec no la reemplaza (spec 100, sección 6): encabezado y franja de
 * conectividad reales, y un rótulo "Pendiente: spec NNN" en lugar del contenido.
 */
@Composable
fun ProvisionalScreen(
    title: String,
    subtitle: String,
    spec: String,
    connectivity: ConnectivityText,
    modifier: Modifier = Modifier,
) {
    ScreenScaffold(title = title, subtitle = subtitle, connectivity = connectivity, modifier = modifier) {
        SendaCard(style = CardStyle.Tinted) {
            Text(
                text = stringResource(R.string.provisional_pending, spec),
                style = SendaText.labelSemiBold,
                color = Senda.colors.textSecondary,
            )
        }
    }
}
