package com.senda.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.senda.ui.theme.Senda
import com.senda.ui.theme.SendaText

/**
 * Diálogo de confirmación de una acción que no se puede deshacer (spec 100, sección 10). No está en Figma: toma la
 * forma de la tarjeta de la cuenta regresiva (esquinas de 24, relleno de 24, botón ancho y acción de texto debajo).
 * [confirmStyle] = [ButtonStyle.Warning] para lo destructivo.
 */
@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmStyle: ButtonStyle = ButtonStyle.Primary,
) {
    val colors = Senda.colors
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = colors.surface,
            contentColor = colors.textPrimary,
            border = if (colors.isDark) null else BorderStroke(1.dp, colors.border),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(title, style = SendaText.headline, color = colors.textPrimary, textAlign = TextAlign.Center)
                Text(body, style = SendaText.bodyLarge, color = colors.textPrimary, textAlign = TextAlign.Center)
                SendaButton(text = confirmLabel, onClick = onConfirm, style = confirmStyle)
                SendaTextButton(text = dismissLabel, onClick = onDismiss, fullWidth = true)
            }
        }
    }
}
