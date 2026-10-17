package com.senda.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
 * Aviso o error con una acción opcional (spec 100, sección 10). No está en Figma: se construye en el estilo del
 * diseño (esquinas de 16, íconos de trazo, texto de 14 con el color del tono sobre su fondo suave).
 */
@Composable
fun MessageBanner(
    text: String,
    tone: UiTone,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = Senda.colors
    val content = colors.tone(tone)
    val container = when (tone) {
        UiTone.ERROR -> colors.red.container
        UiTone.WARNING, UiTone.SIMULATION -> colors.warningContainer
        UiTone.SUCCESS -> colors.successContainer
        UiTone.INFO -> colors.surfaceTint
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(container, RoundedCornerShape(Dimens.cardRadius))
            .padding(Dimens.cardPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            SendaIcon(SendaIcons.TriangleAlert, size = 20.dp, tint = content)
            Text(text, style = SendaText.labelMedium, color = content, modifier = Modifier.weight(1f))
        }
        if (actionLabel != null && onAction != null) {
            SendaTextButton(actionLabel, onClick = onAction)
        }
    }
}
