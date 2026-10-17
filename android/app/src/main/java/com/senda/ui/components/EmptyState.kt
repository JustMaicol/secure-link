package com.senda.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.senda.ui.theme.Senda
import com.senda.ui.theme.SendaText

/**
 * Pantalla o lista sin contenido: ícono, título y explicación centrados (spec 100, sección 10). No está en Figma:
 * se construye con el estilo del diseño (ícono de trazo de 32 dp, título de 16 y texto de 14).
 */
@Composable
fun EmptyState(
    icon: Int,
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
) {
    val colors = Senda.colors
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SendaIcon(icon, size = 32.dp, tint = colors.textSecondary)
        Text(title, style = SendaText.cardTitle, color = colors.textPrimary, textAlign = TextAlign.Center)
        if (body != null) {
            Text(body, style = SendaText.subtitle, color = colors.textSecondary, textAlign = TextAlign.Center)
        }
    }
}
