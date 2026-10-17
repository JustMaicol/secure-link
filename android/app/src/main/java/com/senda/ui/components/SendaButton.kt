package com.senda.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.senda.ui.theme.Dimens
import com.senda.ui.theme.Senda
import com.senda.ui.theme.SendaText

enum class ButtonStyle {
    /** Acción principal: Continuar, Voy en camino. */
    Primary,

    /** Acción secundaria con borde: Abrir en Google Maps. */
    Secondary,

    /** Confirmación positiva: Estoy a salvo. */
    Success,

    /** Cancelar la alerta automática (E3). */
    Warning,
}

private const val DISABLED_ALPHA = 0.4f

/** Botón ancho del diseño: 54 dp de alto, esquinas de 14 dp, ícono opcional de 20 dp y texto de 16 sp. */
@Composable
fun SendaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ButtonStyle = ButtonStyle.Primary,
    icon: Int? = null,
    enabled: Boolean = true,
) {
    val colors = Senda.colors
    val container: Color
    val content: Color
    val border: Color
    when (style) {
        ButtonStyle.Primary -> { container = colors.actionContainer; content = colors.onActionContainer; border = colors.actionBorder }
        ButtonStyle.Secondary -> { container = colors.surface; content = colors.accent; border = colors.border }
        ButtonStyle.Success -> { container = colors.successContainer; content = colors.success; border = colors.border }
        ButtonStyle.Warning -> { container = colors.warningAction; content = colors.onWarningAction; border = colors.warningActionBorder }
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(Dimens.buttonHeight).alpha(if (enabled) 1f else DISABLED_ALPHA),
        shape = RoundedCornerShape(Dimens.buttonRadius),
        color = container,
        contentColor = content,
        border = BorderStroke(Dimens.borderWidth, border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) SendaIcon(icon, size = 20.dp, tint = content)
            Text(text, style = SendaText.button, color = content)
        }
    }
}

/** Botón pequeño de una fila (Figma, "Acción del permiso"): `Revisar` (neutro) o `Permitir` (lleno). */
@Composable
fun SmallActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = Senda.colors
    val container = if (filled) colors.actionContainer else colors.surfaceMuted
    val content = if (filled) colors.onActionContainer else colors.textPrimary
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .defaultMinSize(minWidth = Dimens.smallButtonWidth)
            .height(Dimens.smallButtonHeight)
            .alpha(if (enabled) 1f else DISABLED_ALPHA),
        shape = RoundedCornerShape(Dimens.smallButtonRadius),
        color = container,
        contentColor = content,
        border = if (filled) BorderStroke(Dimens.borderWidth, colors.actionBorder) else null,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text, style = SendaText.body13SemiBold, color = content)
        }
    }
}

/** Acción de texto (Figma: "Cancelar alerta", "Actualizar ubicación"). */
@Composable
fun SendaTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fullWidth: Boolean = false,
) {
    val colors = Senda.colors
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .height(Dimens.textButtonHeight)
            .alpha(if (enabled) 1f else DISABLED_ALPHA),
        color = Color.Transparent,
        contentColor = colors.accent,
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text, style = SendaText.labelSemiBold, color = colors.accent)
        }
    }
}

/** Botón cuadrado de ícono, de 40 × 44 dp (menos y más del contador de personas). */
@Composable
internal fun SquareIconButton(
    icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = Senda.colors
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .size(width = 40.dp, height = Dimens.smallButtonHeight)
            .alpha(if (enabled) 1f else DISABLED_ALPHA),
        shape = RoundedCornerShape(Dimens.smallButtonRadius),
        color = colors.surfaceMuted,
        contentColor = colors.textPrimary,
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            SendaIcon(icon, size = 18.dp, tint = colors.textPrimary, contentDescription = contentDescription)
        }
    }
}
