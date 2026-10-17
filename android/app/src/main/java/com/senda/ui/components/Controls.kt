package com.senda.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.senda.R
import com.senda.ui.theme.Senda
import com.senda.ui.theme.SendaText

/**
 * Interruptor de 44 × 26 dp (Figma, "Interruptor activado"). Apagado: pista del color del borde.
 * PROVISIONAL el aspecto apagado: el diseño solo dibuja el interruptor encendido (en claro y en oscuro).
 */
@Composable
fun SendaSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = Senda.colors
    val thumbOffset by animateDpAsState(targetValue = if (checked) 21.dp else 3.dp, label = "switchThumb")
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .toggleable(
                value = checked,
                enabled = enabled && onCheckedChange != null,
                role = Role.Switch,
                onValueChange = { onCheckedChange?.invoke(it) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Encendido (Figma): claro = pista de acento y botón blanco; oscuro = pista gris con borde y botón claro.
        val trackShape = RoundedCornerShape(13.dp)
        val track = Modifier
            .size(width = 44.dp, height = 26.dp)
            .background(if (checked) colors.switchTrackOn else colors.border, trackShape)
            .let { if (checked && colors.isDark) it.border(BorderStroke(1.dp, colors.actionBorder), trackShape) else it }
        Box(modifier = track) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(thumbOffset.roundToPx(), 3.dp.roundToPx()) }
                    .size(20.dp)
                    .background(if (checked) colors.onActionContainer else colors.textSecondary, CircleShape),
            )
        }
    }
}

/** Casilla de 22 dp con esquinas de 5 dp (Figma, "Casilla seleccionada"). PROVISIONAL la casilla vacía. */
@Composable
fun SendaCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = Senda.colors
    val shape = RoundedCornerShape(5.dp)
    // En claro la casilla marcada es de acento; en oscuro, como el botón principal (fondo y borde del diseño).
    val checkedContainer = if (colors.isDark) colors.actionContainer else colors.accent
    val checkedBorder = if (colors.isDark) colors.actionBorder else colors.accent
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .toggleable(
                value = checked,
                enabled = enabled && onCheckedChange != null,
                role = Role.Checkbox,
                onValueChange = { onCheckedChange?.invoke(it) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(if (checked) checkedContainer else colors.surface, shape)
                .border(BorderStroke(1.5.dp, if (checked) checkedBorder else colors.textSecondary), shape),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) SendaIcon(SendaIcons.Check, size = 16.dp, tint = colors.onActionContainer)
        }
    }
}

/** Contador de personas (Figma, "Control de cantidad"): menos, número en 24 sp y más. */
@Composable
fun QuantityStepper(
    value: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
    canDecrement: Boolean = true,
    canIncrement: Boolean = true,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SquareIconButton(
            icon = SendaIcons.Minus,
            contentDescription = stringResource(R.string.common_decrease),
            onClick = onDecrement,
            enabled = canDecrement,
        )
        Text(
            text = value.toString(),
            style = SendaText.counter,
            color = Senda.colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = CounterMinWidth),
        )
        SquareIconButton(
            icon = SendaIcons.Plus,
            contentDescription = stringResource(R.string.common_increase),
            onClick = onIncrement,
            enabled = canIncrement,
        )
    }
}

/** Ancho mínimo del número, para que 9 → 10 no mueva los botones. */
private val CounterMinWidth = 28.dp

/** Par etiqueta-valor en una línea (Figma, "Dato"): etiqueta de 110 dp y valor a su lado. */
@Composable
fun InfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = Senda.colors
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = SendaText.body13, color = colors.textSecondary, modifier = Modifier.width(110.dp))
        Text(value, style = SendaText.labelMedium, color = colors.textPrimary, modifier = Modifier.weight(1f))
    }
}
