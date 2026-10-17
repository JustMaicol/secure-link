package com.senda.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.senda.ui.format.ConnectivityText
import com.senda.ui.theme.Dimens
import com.senda.ui.theme.Senda

/**
 * Estructura de cada pestaña (Figma): márgenes de la barra de estado, encabezado, franja de conectividad y el
 * contenido, que se desplaza si no cabe, y una zona fija opcional debajo ([bottomBar]). La barra de pestañas no va
 * aquí: es de la navegación (SendaApp).
 */
@Composable
fun ScreenScaffold(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    connectivity: ConnectivityText? = null,
    identityIcon: Int = SendaIcons.RadioTower,
    identityLabel: String? = null,
    onIdentityClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = Dimens.screenPadding, vertical = 18.dp),
    verticalSpacing: Dp = 14.dp,
    bottomBar: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Senda.colors.background)
            .statusBarsPadding(),
    ) {
        if (identityLabel != null) {
            ScreenHeader(title, subtitle, identityIcon = identityIcon, identityLabel = identityLabel, onIdentityClick = onIdentityClick)
        } else {
            ScreenHeader(title, subtitle, identityIcon = identityIcon, onIdentityClick = onIdentityClick)
        }
        if (connectivity != null) {
            ConnectivityStrip(connectivity.mesh, connectivity.meshTone, connectivity.internet)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
            content = content,
        )
        // Zona fija bajo el desplazamiento (el botón de enviar del formulario): siempre a la vista.
        if (bottomBar != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = Dimens.screenPadding, end = Dimens.screenPadding, top = 8.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = bottomBar,
            )
        }
    }
}
