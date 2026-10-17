package com.senda.features.sendalert

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.senda.R
import com.senda.ui.components.SendaIcon
import com.senda.ui.components.SendaIcons
import com.senda.ui.theme.Dimens
import com.senda.ui.theme.Senda
import com.senda.ui.theme.SendaText

/**
 * Botón de mantener presionado para enviar (Figma E1, "Mantener pulsado": 64 dp, huella y dos líneas de texto).
 * No decide nada: avisa cuando se empieza y se termina de presionar, y dibuja el [progress] (0 a 1) que le da quien
 * lleva el tiempo (el ViewModel, con el `Clock`). El llenado es el único añadido al diseño.
 */
@Composable
fun HoldToSendButton(
    progress: Float,
    onHoldStart: () -> Unit,
    onHoldEnd: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = Senda.colors
    val shape = RoundedCornerShape(Dimens.buttonRadius)
    val start by rememberUpdatedState(onHoldStart)
    val end by rememberUpdatedState(onHoldEnd)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(shape)
            .background(colors.actionContainer)
            .border(BorderStroke(Dimens.borderWidth, colors.actionBorder), shape)
            // Un solo elemento para TalkBack, y desactivado de verdad (no solo menos opaco) cuando no se puede enviar.
            .semantics(mergeDescendants = true) {
                role = Role.Button
                if (!enabled) disabled()
            }
            .pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures(onPress = {
                        start()
                        try {
                            tryAwaitRelease()
                        } finally {
                            end()
                        }
                    })
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // Llenado de izquierda a derecha, con el color del texto a poca opacidad para que se lea sobre cualquier fondo.
        if (progress > 0f) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .background(colors.onActionContainer.copy(alpha = 0.22f)),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            SendaIcon(SendaIcons.Fingerprint, size = 26.dp, tint = colors.onActionContainer)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.sendalert_hold_title), style = SendaText.cardTitle, color = colors.onActionContainer)
                Text(stringResource(R.string.sendalert_hold_hint), style = SendaText.caption, color = colors.onActionContainer)
            }
        }
    }
}
