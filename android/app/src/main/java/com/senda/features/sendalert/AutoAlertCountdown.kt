package com.senda.features.sendalert

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.senda.R
import com.senda.ui.components.ButtonStyle
import com.senda.ui.components.SendaButton
import com.senda.ui.components.SendaIcon
import com.senda.ui.components.SendaIcons
import com.senda.ui.format.countdownSeconds
import com.senda.ui.theme.Senda
import com.senda.ui.theme.SendaText

/**
 * E3 · Cuenta regresiva de la alerta automática (Figma): capa a pantalla completa sobre cualquier pestaña, con el velo
 * del diseño y una tarjeta centrada. Sin estado: la cuenta la lleva la 107 (sigue con la app cerrada) y esta capa solo
 * dibuja [state] y avisa de [onCancel]. Cancelar es la acción obvia y ancha; no hay otra salida.
 */
@Composable
fun AutoAlertCountdown(
    state: AutoAlertCountdownUiState,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Senda.colors
    Box(
        modifier = modifier.fillMaxSize().background(colors.scrim),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(24.dp),
            color = colors.surface,
            contentColor = colors.textPrimary,
            border = if (colors.isDark) null else BorderStroke(1.dp, colors.border),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                    SendaIcon(SendaIcons.Activity, size = 18.dp, tint = colors.warningStrong)
                    Text(stringResource(R.string.sendalert_countdown_origin), style = SendaText.captionBold, color = colors.warningStrong)
                }
                Text(
                    stringResource(R.string.sendalert_countdown_title),
                    style = SendaText.headline,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                )
                CountdownRing(state)
                Text(
                    stringResource(R.string.sendalert_countdown_body),
                    style = SendaText.bodyLarge,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SendaButton(
                        text = stringResource(R.string.sendalert_countdown_cancel),
                        onClick = onCancel,
                        style = ButtonStyle.Warning,
                        icon = SendaIcons.X,
                    )
                    Text(
                        stringResource(R.string.sendalert_countdown_hint),
                        style = SendaText.caption,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/** Anillo de 128 dp con el tiempo que queda: pista de 7 dp y arco que se acorta en el sentido del reloj. */
@Composable
private fun CountdownRing(state: AutoAlertCountdownUiState) {
    val colors = Senda.colors
    val track = colors.surfaceMuted
    val progress = colors.countdownProgress
    Box(
        modifier = Modifier
            .size(128.dp)
            // Anuncia el cambio de segundos sin que el usuario tenga que buscarlo.
            .semantics { liveRegion = LiveRegionMode.Polite },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 7.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(track, startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke))
            if (state.progress > 0f) {
                drawArc(
                    progress,
                    startAngle = -90f,
                    sweepAngle = 360f * state.progress,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Butt),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(countdownSeconds(state.remainingMs).toString(), style = SendaText.countdown, color = colors.textPrimary)
            Text(stringResource(R.string.sendalert_countdown_unit), style = SendaText.body13, color = colors.textSecondary)
        }
    }
}
