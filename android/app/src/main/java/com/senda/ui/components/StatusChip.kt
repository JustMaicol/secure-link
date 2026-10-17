package com.senda.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.senda.ui.theme.Dimens
import com.senda.ui.theme.Senda
import com.senda.ui.theme.SendaText
import com.senda.ui.theme.UiTone

/**
 * Insignia de estado con ícono opcional (spec 100, sección 10). No está en Figma: se construye con la forma de la
 * insignia de nivel (esquinas de 7, relleno de 9 × 5, texto de 12 en negrita) y los colores del tono.
 * El estado nunca va solo en color: siempre lleva texto.
 */
@Composable
fun StatusChip(
    text: String,
    tone: UiTone,
    modifier: Modifier = Modifier,
    icon: Int? = null,
) {
    val colors = Senda.colors
    val content = colors.tone(tone)
    Row(
        modifier = modifier
            .background(colors.toneContainer(tone), RoundedCornerShape(Dimens.badgeRadius))
            .padding(horizontal = 9.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) SendaIcon(icon, size = 14.dp, tint = content)
        Text(text, style = SendaText.captionBold, color = content)
    }
}
