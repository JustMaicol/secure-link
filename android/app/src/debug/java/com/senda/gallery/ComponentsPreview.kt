package com.senda.gallery

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.senda.core.protocol.EmergencyType
import com.senda.ui.components.ButtonStyle
import com.senda.ui.components.CardStyle
import com.senda.ui.components.EmergencyBadge
import com.senda.ui.components.EmptyState
import com.senda.ui.components.InfoRow
import com.senda.ui.components.MessageBanner
import com.senda.ui.components.SendaButton
import com.senda.ui.components.SendaCard
import com.senda.ui.components.SendaIcons
import com.senda.ui.components.SimulationBadge
import com.senda.ui.components.StatusChip
import com.senda.ui.theme.Senda
import com.senda.ui.theme.SendaTheme
import com.senda.ui.theme.UiTone

// spec 100, criterio 13 · Vista previa de los componentes de la sección 10 con todos los tonos y variantes, en claro y
// en oscuro. Solo depuración. La galería (GalleryActivity) muestra lo mismo en el teléfono, junto con las pantallas.

@Composable
private fun ComponentsPreviewContent() {
    Column(
        modifier = Modifier.background(Senda.colors.background).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UiTone.entries.forEach { tone -> StatusChip(tone.name, tone, icon = SendaIcons.Check) }
        }
        UiTone.entries.forEach { tone -> MessageBanner("Mensaje de tono ${tone.name}.", tone, actionLabel = "Acción", onAction = {}) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EmergencyType.entries.forEach { EmergencyBadge(it, it.name) }
            SimulationBadge()
        }
        CardStyle.entries.forEach { style -> SendaCard(style = style) { InfoRow("Tarjeta", style.name) } }
        ButtonStyle.entries.forEach { style -> SendaButton(style.name, onClick = {}, style = style, icon = SendaIcons.ArrowRight) }
        SendaButton("Deshabilitado", onClick = {}, enabled = false)
        SendaCard { EmptyState(SendaIcons.Radar, "Sin contenido", body = "Explicación de por qué no hay nada.") }
    }
}

@Preview(name = "Componentes · claro", showBackground = true, heightDp = 1500)
@Composable
private fun ComponentsLightPreview() {
    SendaTheme(darkTheme = false) { ComponentsPreviewContent() }
}

@Preview(name = "Componentes · oscuro", showBackground = true, heightDp = 1500, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ComponentsDarkPreview() {
    SendaTheme(darkTheme = true) { ComponentsPreviewContent() }
}
